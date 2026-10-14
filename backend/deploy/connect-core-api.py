"""Connect the existing API to the private test circle with status-only routing."""
import argparse
import copy
import importlib.util
import json
import os
import shutil
import subprocess
import time
from pathlib import Path
from urllib.parse import quote

spec = importlib.util.spec_from_file_location("memory_deploy", Path(__file__).with_name("update-memory-api.py"))
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
docker = base.docker
DEST = "/run/secrets/genesis-core-status"
PROBE = "import json; from app.genesis_core_status import collect_status; print(json.dumps(collect_status()))"


def prepare_secret(circle, directory, uid, gid):
    source = Path(circle) / "state" / "checker"
    config = json.loads((source / "data.json").read_text())
    if set(config["peers"]) != {"https://core-a:8443", "https://core-b:8443"}:
        raise ValueError("Unexpected test circle origins")
    directory = Path(directory)
    directory.mkdir(mode=0o700)
    for name in ("ca.crt", "data.crt", "data.key"):
        shutil.copyfile(source / name, directory / name)
    config["tls"] = {"ca": DEST + "/ca.crt", "cert": DEST + "/data.crt", "key": DEST + "/data.key"}
    (directory / "client.json").write_text(json.dumps(config))
    for file in directory.iterdir():
        file.chmod(0o400)
        os.chown(file, uid, gid)
    os.chown(directory, uid, gid)
    return directory


def probe(image, network, secret, uid, gid):
    run = subprocess.run([
        "docker", "run", "--rm", "--network", network, "--read-only", "--cap-drop", "ALL",
        "--security-opt", "no-new-privileges", "--memory", "128m", "--pids-limit", "64",
        "--user", f"{uid}:{gid}", "--mount", f"type=bind,source={secret},target={DEST},readonly",
        "--env", "GENESIS_CORE_STATUS_CONFIG=" + DEST + "/client.json", "--entrypoint", "python",
        image, "-c", PROBE
    ], capture_output=True, text=True, timeout=30)
    if run.returncode or json.loads(run.stdout).get("status") != "connected":
        raise RuntimeError("Core preflight failed; API has not been changed")


def connect(circle, image, probe_only=False):
    if os.geteuid() != 0:
        raise ValueError("Run as root on the Docker host")
    circle = Path(circle).resolve()
    project = next(line.split("=", 1)[1] for line in (circle / ".env").read_text().splitlines()
                   if line.startswith("COMPOSE_PROJECT_NAME="))
    network = project + "_private"
    prefix = "/v" + docker("GET", "/version")["ApiVersion"]
    net = docker("GET", prefix + "/networks/" + quote(network, safe=""))
    if net.get("Internal") is not True or net.get("Labels", {}).get("com.docker.compose.project") != project:
        raise ValueError("Expected the selected private circle network")
    image_id = docker("GET", prefix + "/images/" + quote(image, safe="") + "/json")["Id"]
    if probe_only:
        uid, gid = 10001, 10001
        secret = prepare_secret(circle, circle / ("status-probe-" + str(time.time_ns())), uid, gid)
        probe(image_id, network, secret, uid, gid)
        print("Real Docker API-to-core mTLS probe passed.")
        return
    old = docker("GET", prefix + "/containers/genesis-api/json")
    if not old["State"]["Running"] or old["HostConfig"].get("AutoRemove"):
        raise ValueError("Existing API must support rollback")
    def identity(option):
        value = subprocess.run(["docker", "exec", "genesis-api", "id", option],
                               check=True, capture_output=True, text=True, timeout=10)
        return int(value.stdout.strip())
    uid, gid = identity("-u"), identity("-g")
    secret = prepare_secret(circle, Path("/opt") / ("genesis-core-status-" + str(time.time_ns())), uid, gid)
    probe(image_id, network, secret, uid, gid)
    config = copy.deepcopy(old["Config"])
    config["Image"], config["Hostname"] = image_id, ""
    config["Env"] = [item for item in config.get("Env", []) if not item.startswith("GENESIS_CORE_STATUS_CONFIG=")]
    config["Env"].append("GENESIS_CORE_STATUS_CONFIG=" + DEST + "/client.json")
    host = copy.deepcopy(old["HostConfig"])
    host["Binds"] = [item for item in host.get("Binds") or [] if item.split(":")[1] != DEST]
    host["Binds"].append(str(secret) + ":" + DEST + ":ro")
    config["HostConfig"] = host
    endpoints = {name: {} if name == "bridge" else {"Aliases": endpoint.get("Aliases") or []}
                 for name, endpoint in old["NetworkSettings"]["Networks"].items()
                 if not (name.startswith("genesis-circle-") and name.endswith("_private"))}
    endpoints[network] = {}
    config["NetworkingConfig"] = {"EndpointsConfig": endpoints}
    old_id = old["Id"]
    restart = copy.deepcopy(old["HostConfig"]["RestartPolicy"])
    stamp = str(time.time_ns())
    backup = "genesis-api-before-core-status-" + stamp
    new_id, stopped, renamed = None, False, False
    try:
        new_id = docker("POST", prefix + "/containers/create?name=genesis-api-core-status-" + stamp, config)["Id"]
        docker("POST", prefix + "/containers/" + old_id + "/stop?t=20")
        stopped = True
        docker("POST", prefix + "/containers/" + old_id + "/rename?name=" + backup)
        renamed = True
        docker("POST", prefix + "/containers/" + old_id + "/update", {"RestartPolicy": {"Name": "no"}})
        docker("POST", prefix + "/containers/" + new_id + "/rename?name=genesis-api")
        docker("POST", prefix + "/containers/" + new_id + "/start")
        deadline = time.monotonic() + 60
        while True:
            try:
                if base.get_json("http://127.0.0.1:8080/readyz").get("status") == "ready":
                    status = subprocess.run(["docker", "exec", "genesis-api", "python", "-c", PROBE],
                                            capture_output=True, text=True, timeout=10)
                    if status.returncode == 0 and json.loads(status.stdout).get("status") == "connected":
                        break
            except Exception:
                pass
            if time.monotonic() >= deadline:
                raise RuntimeError("API/core readiness failed")
            time.sleep(2)
        print("API connected to both core nodes. Execution remains closed.")
        print("Rollback container:", backup)
        print(status.stdout.strip())
    except Exception:
        if new_id:
            docker("DELETE", prefix + "/containers/" + new_id + "?force=true")
        if renamed:
            docker("POST", prefix + "/containers/" + old_id + "/rename?name=genesis-api")
        docker("POST", prefix + "/containers/" + old_id + "/update", {"RestartPolicy": restart})
        if stopped:
            docker("POST", prefix + "/containers/" + old_id + "/start")
        print("Previous API restored.")
        raise


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--circle", required=True)
    parser.add_argument("--image", required=True)
    parser.add_argument("--probe-only", action="store_true")
    args = parser.parse_args()
    try:
        connect(args.circle, args.image, args.probe_only)
    except Exception as error:
        raise SystemExit("Core connection stopped: " + type(error).__name__)
