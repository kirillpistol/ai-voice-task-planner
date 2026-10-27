#!/usr/bin/env python3
"""Synthetic local storage benchmark; no banking connections or personal data."""
import argparse, json, os, statistics, tempfile, time
from pathlib import Path

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--mib", type=int, default=256)
    p.add_argument("--iterations", type=int, default=3)
    p.add_argument("--dir", type=Path, default=None, help="Target disk directory")
    p.add_argument("--output", type=Path, default=Path("storage_benchmark_result.json"))
    a = p.parse_args()
    if not (1 <= a.mib <= 10240 and 1 <= a.iterations <= 20):
        p.error("mib must be 1..10240 and iterations 1..20")
    record = b'{"id":"SYNTHETIC","payload":"' + b'X'*990 + b'"}\n'
    block = (record * (1048576 // len(record) + 1))[:1048576]
    results = []
    for _ in range(a.iterations):
        with tempfile.TemporaryDirectory(dir=a.dir) as tmp:
            path = Path(tmp) / "data.bin"
            start = time.perf_counter()
            with path.open("wb", buffering=1024*1024) as f:
                for __ in range(a.mib):
                    f.write(block)
                f.flush()
                os.fsync(f.fileno())
            elapsed = time.perf_counter() - start
            results.append({"seconds": round(elapsed, 4), "mib_per_second": round(a.mib/elapsed, 2)})
    median = statistics.median(x["mib_per_second"] for x in results)
    output = {"dataset": "synthetic", "mib_per_run": a.mib, "runs": results,
              "median_mib_per_second": round(median, 2),
              "extrapolated_hours_for_1TB": round(1e12/(median*1048576)/3600, 2),
              "warning": "Disk/cache/virtualization may skew results; extrapolation is not a measured 1TB test."}
    a.output.write_text(json.dumps(output, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(output, ensure_ascii=False, indent=2))
if __name__ == "__main__":
    main()
