# GENESIS — first synthetic write benchmark

Test date: 2026-10-27. Environment: temporary isolated execution environment (not user's Windows/Android hardware).

- Payload: synthetic, no phone numbers or personal information.
- 3 runs × 64 MiB, local temporary filesystem, buffered write + flush + fsync.
- Measured throughput: **211.12**, **1392.09**, **1774.17 MiB/s**.
- Median: **1392.09 MiB/s**.
- Observed variability is extreme. File cache, virtualized storage and small run size make 1 TB extrapolation unreliable.
- **No 1 TB write was performed.** No third-party app/API requests were performed.

## Reproduce on Windows

```powershell
python reports/testing/storage_benchmark.py --mib 256 --iterations 3 --output storage_benchmark_result.json
```

Use `--dir D:\\` (replace with the actual drive) to select the target storage. Requires Python 3; test writes and deletes a temporary file. Ensure enough free disk space. To reduce cache influence, use larger runs and repeat under stable conditions. Benchmark the actual database separately before estimating storage ingestion throughput.

## User Windows machine: measured synthetic write test

- Command: `python reports/testing/storage_benchmark.py --mib 256 --iterations 3`
- 3 × 256 MiB (768 MiB total); `flush` and `fsync` per run.
- Run 1: 0.7914 s, **323.47 MiB/s**
- Run 2: 0.7037 s, **363.78 MiB/s**
- Run 3: 1.0400 s, **246.16 MiB/s**
- Median: **323.47 MiB/s**; mean: **311.14 MiB/s**.
- Idealized 1 TB extrapolation: **0.82 h (~49 min)**, not measured.
- Machine storage type, filesystem, free capacity, sustained thermal behavior and DB overhead not established.
- Next: larger local synthetic runs and database-specific benchmark; avoid third-party banking systems.
