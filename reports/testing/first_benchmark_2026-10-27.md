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
