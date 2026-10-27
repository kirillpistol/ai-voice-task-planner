# GENESIS — Storage Capacity Benchmark (1 TB)

**Status:** analytical model; no measured Ozon Bank request throughput.  
**Scope:** synthetic, consented or anonymized data only. No phone-number enumeration, bank recipient scraping or live banking load generation.

## Model
Assume **1 TB = 1,000,000,000,000 bytes** of logical data. The estimate depends on the average *stored* record size, including schema, metadata and indexes.

| Stored record | Records per TB |
|---|---:|
| 256 B | 3,906,250,000 |
| 512 B | 1,953,125,000 |
| 1 KiB | 976,562,500 |
| 4 KiB | 244,140,625 |

**Time to write 1 TB** (ideal sustained write speed, excludes retries, latency, backups and storage overhead):

| Sustained write throughput | Time |
|---|---:|
| 1 MiB/s | ~11.0 days |
| 5 MiB/s | ~2.21 days |
| 10 MiB/s | ~26.5 hours |
| 50 MiB/s | ~5.30 hours |
| 100 MiB/s | ~2.65 hours |

Formulas:
- `records = target_bytes / stored_bytes_per_record`
- `duration_seconds = target_bytes / sustained_bytes_per_second`
- `physical_capacity = logical_capacity * replication_factor / compression_ratio + reserved_space`

## Benchmark protocol
1. Generate synthetic records with no phone numbers or personal data.
2. Record write speed, p50/p95 latency, CPU, RAM, disk I/O, and failure rate.
3. Test in isolated local or authorized GENESIS infrastructure, never against third-party banking APIs.
4. Increase synthetic dataset size gradually (1 GB → 10 GB → 100 GB), extrapolate to 1 TB, then validate on a suitable test volume.
5. Record database engine, disk type, batch size, index configuration, compression, durability, replication, and hardware specifications.

## Reporting schema
```json
{
  "test_id": "GENESIS-STORAGE-001",
  "dataset": "synthetic",
  "target_bytes": 1000000000000,
  "sample_bytes": null,
  "elapsed_seconds": null,
  "write_mib_per_second": null,
  "p95_latency_ms": null,
  "error_rate": null,
  "hardware": "not_measured",
  "status": "estimate_only"
}
```

**Interpretation:** 1 TB is a storage capacity target, not a requests-per-second measurement. No measured third-party application capacity is implied.
