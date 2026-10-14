# Sector security profiles

Security profile definitions are independent of future model/service bindings.
`genesis_security_profiles.SECURITY_PROFILES` is the profile registry;
`SECTOR_SECURITY_PROFILES` assigns a profile to a sector.

Initial assignment:

| Sector | Profile |
| --- | --- |
| FINANCE | Inbound VPN -> VM -> temporary Docker container with cleanup -> outbound VPN |
| MEDICINE | Not assigned |
| MANUFACTURING | Not assigned |
| MILITARY | Not assigned |
| GOVERNMENT | Not assigned |
| LOGISTICS | Not assigned |

Profile ID: `vpn-vm-ephemeral-container-vpn`, version 1. Applies to sector
services. Cleanup scope describes the intended container and temporary workdir
cleanup. Actual infrastructure deployment is recorded as `not_deployed`.
Selecting a profile does not provision VPN/VM/container resources or change the
core host network, resource settings, mTLS gateway or execution permissions.

Memory workspace responses include structured `security_profile` metadata and
localized `security_system` text. Android shows the finance preset immediately
when that sector is selected. Other sectors have no inherited finance scheme.
Future model and service selection can reference the profile independently;
alternate military/manufacturing profiles can be added without replacing L1.
