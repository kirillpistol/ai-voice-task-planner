"""Read-only ADB connectivity check. Run inside the designated Windows VM."""
import argparse
import json
import subprocess


def parse_devices(output):
    devices = []
    for line in output.splitlines():
        fields = line.split()
        if len(fields) >= 2 and fields[1] in {"device", "offline", "unauthorized"}:
            devices.append({"serial": fields[0], "state": fields[1]})
    return devices


def probe(adb, serial):
    def run(args):
        return subprocess.run(
            [adb, *args], check=True, capture_output=True,
            text=True, timeout=15, shell=False,
        ).stdout.strip()

    devices = parse_devices(run(["devices"]))
    selected = next((item for item in devices if item["serial"] == serial), None)
    if selected is None:
        return {"status": "device_not_found"}
    if selected["state"] != "device":
        return {"status": selected["state"]}
    booted = run(["-s", serial, "shell", "getprop", "sys.boot_completed"])
    return {
        "status": "ready" if booted == "1" else "booting",
        "android_version": run([
            "-s", serial, "shell", "getprop", "ro.build.version.release"
        ]),
        "application_actions": False,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", required=True)
    args = parser.parse_args()
    if args.serial.startswith("-") or not args.serial.strip():
        parser.error("A valid explicit device serial is required")
    try:
        result = probe(args.adb, args.serial)
    except FileNotFoundError:
        result = {"status": "adb_not_found"}
    except subprocess.TimeoutExpired:
        result = {"status": "timeout"}
    except (subprocess.CalledProcessError, OSError):
        result = {"status": "adb_error"}
    print(json.dumps(result, ensure_ascii=False))
    return 0 if result["status"] == "ready" else 1


if __name__ == "__main__":
    raise SystemExit(main())
