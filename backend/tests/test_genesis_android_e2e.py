from app import genesis_android_e2e as e2e


def test_real_flow_contract_with_fake_transport(monkeypatch, tmp_path):
    calls = []

    def fake_adb(adb, serial, *args):
        calls.append(args)
        if args == ("get-state",):
            return "device"
        if args == ("shell", "getprop", "sys.boot_completed"):
            return "1"
        if args == ("shell", "dumpsys", "activity", "activities"):
            return "topResumedActivity=ActivityRecord{0 com.android.settings/.Settings t2}"
        return ""

    monkeypatch.setattr(e2e, "adb_call", fake_adb)
    report = e2e.run_e2e("adb", tmp_path)
    assert report["observation"]["status"] == "passed"
    assert report["comparison"][0]["result"]["verified_foreground"] is True
    assert ("shell", "input", "keyevent", "KEYCODE_HOME") in calls
    assert (tmp_path / "genesis_e2e_report.json").exists()


def test_failed_foreground_is_recorded(monkeypatch, tmp_path):
    def fake_adb(adb, serial, *args):
        if args == ("get-state",):
            return "device"
        if args == ("shell", "getprop", "sys.boot_completed"):
            return "1"
        return "topResumedActivity=ActivityRecord{0 com.android.launcher/.Launcher}"

    monkeypatch.setattr(e2e, "adb_call", fake_adb)
    monkeypatch.setattr(e2e.time, "sleep", lambda seconds: None) if hasattr(e2e, "time") else None
    report = e2e.run_e2e("adb", tmp_path)
    assert report["observation"]["status"] == "failed"
    assert report["comparison"][0]["status"] == "observed"
