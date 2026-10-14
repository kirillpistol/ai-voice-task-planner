"""Replace an existing Docker API while retaining its configuration and rollback.

Run on the Ubuntu Docker host after publishing the policy and building the image.
Never prints container environments, credentials or response bodies on failure.
"""
import argparse
import copy
import http.client
import json
import socket
import time
import urllib.request
from urllib.parse import quote


class DockerConnection(http.client.HTTPConnection):
    def connect(self):
        self.sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self.sock.settimeout(45)
        self.sock.connect("/var/run/docker.sock")


def docker(method, path, body=None):
    conn = DockerConnection("localhost", timeout=45)
    try:
        conn.request(method, path, None if body is None else json.dumps(body).encode(),
                     {"Content-Type": "application/json"})
        response = conn.getresponse()
        data = response.read()
        if response.status >= 400:
            raise RuntimeError("Docker HTTP " + str(response.status))
        return json.loads(data) if data else None
    finally:
        conn.close()


def replacement(old, image, policy_url, policy_version):
    if not old["State"]["Running"] or old["HostConfig"].get("AutoRemove"):
        raise ValueError("Existing API must be running and support rollback")
    config = copy.deepcopy(old["Config"])
    config["Image"] = image
    config["Hostname"] = ""
    keys = {"GENESIS_MEMORY_PRIVACY_URL", "GENESIS_MEMORY_POLICY_VERSION"}
    config["Env"] = [item for item in config.get("Env", []) if item.split("=", 1)[0] not in keys]
    config["Env"].extend(["GENESIS_MEMORY_PRIVACY_URL=" + policy_url,
                          "GENESIS_MEMORY_POLICY_VERSION=" + policy_version])
    config["HostConfig"] = copy.deepcopy(old["HostConfig"])
    config["NetworkingConfig"] = {"EndpointsConfig": {
        name: {} if name == "bridge" else {"Aliases": endpoint.get("Aliases") or []}
        for name, endpoint in old["NetworkSettings"]["Networks"].items()
    }}
    return config


def get_json(url):
    with urllib.request.urlopen(url, timeout=5) as response:
        return json.load(response)


def deploy(image, policy_url, policy_version):
    if not policy_url.startswith("https://"):
        raise ValueError("Published HTTPS policy required")
    with urllib.request.urlopen(policy_url, timeout=20) as response:
        page = response.read().decode("utf-8")
    if policy_version not in page or "</html>" not in page:
        raise ValueError("Published policy/version check failed")
    prefix = "/v" + docker("GET", "/version")["ApiVersion"]
    image_id = docker("GET", prefix + "/images/" + quote(image, safe="") + "/json")["Id"]
    old = docker("GET", prefix + "/containers/genesis-api/json")
    payload = replacement(old, image_id, policy_url, policy_version)
    old_id = old["Id"]
    restart = copy.deepcopy(old["HostConfig"]["RestartPolicy"])
    stamp = str(time.time_ns())
    backup = "genesis-api-before-memory-" + stamp
    new_id, stopped, renamed = None, False, False
    try:
        new_id = docker("POST", prefix + "/containers/create?name=genesis-api-memory-" + stamp, payload)["Id"]
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
                ready = get_json("http://127.0.0.1:8080/readyz")
                config = get_json("http://127.0.0.1:8080/api/v2/genesis/memory/config")
                if (ready.get("status") == "ready" and config.get("available") is True
                        and config.get("policy_url") == policy_url
                        and config.get("policy_version") == policy_version
                        and config.get("execution_allowed") is False):
                    break
            except Exception:
                pass
            if time.monotonic() >= deadline:
                raise RuntimeError("Readiness check failed")
            time.sleep(2)
        print("Personal memory enabled. Core execution remains closed.")
        print("Rollback container:", backup)
    except Exception:
        print("Update failed. Restoring previous API.")
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
    parser.add_argument("--image", required=True)
    parser.add_argument("--policy-url", required=True)
    parser.add_argument("--policy-version", default="memory-analysis-v1")
    args = parser.parse_args()
    try:
        deploy(args.image, args.policy_url, args.policy_version)
    except Exception as error:
        # Avoid leaking library exceptions containing credential-bearing URLs.
        raise SystemExit("Deployment stopped: " + type(error).__name__)
