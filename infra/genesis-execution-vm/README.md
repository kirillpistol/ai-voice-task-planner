# GENESIS Execution VM (required for real L2 execution)

Status: **planning and host preflight only**. No guest VM has been created or started.

## Provisioning decision
- Host candidate: Ubuntu 24.04 KVM-capable VPS; initial host had 2 vCPU and 1.9 GiB RAM with no swap, so **do not run a guest on it at that capacity**.
- Requested target host: **8 vCPU, 16 GiB RAM, at least 160 GiB SSD/NVMe**.
- Guest budget: 4 vCPU, 8 GiB RAM, 80 GiB qcow2 disk; reserve remaining host resources for existing API, PostgreSQL, MED and monitoring.
- Obtain the upgraded host's provider **snapshot/backup** and confirm hosting provider supports nested KVM before provisioning.
- Run `bash infra/genesis-execution-vm/preflight.sh` **before** installing packages. Exit code 2 blocks rollout.
- VM must be an Ubuntu Linux guest, started and observable through libvirt/QEMU and logged into via one explicitly provisioned SSH public key; no password SSH, no copied host secrets.

## Network and trust design
- Treat guest as untrusted relative to host. No Docker socket or host /run/secrets mounts inside guest.
- Guest isolated on its own network segment, no public inbound ports. Security gateway forwards only specifically approved, authenticated task RPC traffic; other host and private service networks denied.
- mTLS client/server auth, role-based operation policy, certificate pin updates and certificate lifecycle are coordinated with `feature/genesis-certificate-manager-20261025`.
- Avoid broad NAT-to-LAN access. Outbound internet must be allowlisted only for explicit package provisioning and later controlled provider integrations.
- No user documents, banking credentials, PIN/CVV or production identifiers in smoke tests.

## Bring-up acceptance gates
1. Provider resources really upgraded (8 vCPU / 16 GiB) and VPS backup verified.
2. `preflight.sh` exits 0; `/dev/kvm` access verified.
3. Install Ubuntu's packaged `qemu-kvm`, `libvirt-daemon-system`, `libvirt-clients`, `virtinst`, `cloud-image-utils`; confirm libvirt service availability.
4. Prepare signed/verifiable Ubuntu cloud image from trusted source, storage pool and cloud-init configuration **without shared secrets**; review guest image checksum before importing it.
5. Provision explicitly named `genesis-execution-01` with configured vCPU/RAM limits, disk snapshot strategy, dedicated isolated virtual network, SSH public-key access, and autostart only once recovery was tested.
6. Validate `virsh list --all`, guest console/cloud-init completion, isolated network rules, and clean VM stop/start.
7. Install Docker *inside guest*, with rootless workers where feasible and pinned image digests; test guest container execution with synthetic payload only.
8. Implement authenticated L3 → L1 → VM executor dispatch. Successful synthetic Circle and mTLS probe from old host **do not** meet this acceptance criterion.
9. Tests: least-privilege isolation, no host Docker socket, resource pressure, disk exhaustion, failed worker, expired client certificate, rollback and restore.
10. Only after gates 1–9 consider customer tasks.

## Rollout protections
- No host installation or VM startup from the GitHub change.
- No destructive `virsh` commands, network flush or host firewall modification until provider upgrade confirmed.
- Keep GENESIS Certificate Manager and VM Execution Node as separate review units.
