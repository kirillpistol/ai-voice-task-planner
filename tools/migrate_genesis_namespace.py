#!/usr/bin/env python3
"""Migrate PISTOL GENESIS Kotlin packages without changing backend/Firebase identity.

Usage:
    python tools/migrate_genesis_namespace.py          # preview only
    python tools/migrate_genesis_namespace.py --apply  # apply locally
Run at repository root, then run Android/iOS builds before committing.
"""
from __future__ import annotations

import argparse
from pathlib import Path

OLD_PACKAGE = "dem.dev.timeflame"
NEW_PACKAGE = "dem.dev.genesis"
OLD_PATH = Path("dem/dev/timeflame")
NEW_PATH = Path("dem/dev/genesis")
KOTLIN_ROOTS = [
    Path("composeApp/src"),
    Path("shared/src"),
]


def plan(root: Path) -> list[tuple[Path, Path, str]]:
    changes: list[tuple[Path, Path, str]] = []
    for source_root in KOTLIN_ROOTS:
        for file in sorted((root / source_root).rglob("*.kt")):
            old_relative = file.relative_to(root)
            new_relative = Path(str(old_relative).replace(str(OLD_PATH), str(NEW_PATH)))
            text = file.read_text(encoding="utf-8")
            updated = text.replace(OLD_PACKAGE, NEW_PACKAGE)
            if old_relative != new_relative or text != updated:
                changes.append((file, root / new_relative, updated))
    gradle = root / "composeApp/build.gradle.kts"
    text = gradle.read_text(encoding="utf-8")
    updated = text.replace('namespace = "dem.dev.timeflame"', 'namespace = "dem.dev.genesis"')
    if updated != text:
        changes.append((gradle, gradle, updated))
    shared_gradle = root / "shared/build.gradle.kts"
    text = shared_gradle.read_text(encoding="utf-8")
    updated = text.replace('namespace = "dem.dev.timeflame.shared"', 'namespace = "dem.dev.genesis.shared"')
    if updated != text:
        changes.append((shared_gradle, shared_gradle, updated))
    targets = [destination for _, destination, _ in changes]
    if len(set(targets)) != len(targets):
        raise RuntimeError("Two migrated sources would overwrite the same file.")
    for source, destination, _ in changes:
        if source != destination and destination.exists():
            raise RuntimeError(f"Refusing to overwrite existing file: {destination}")
    return changes


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="Write migration changes")
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    changes = plan(root)
    for source, target, _ in changes:
        print(f"{source.relative_to(root)} -> {target.relative_to(root)}")
    print(f"Planned edits: {len(changes)}")
    if not args.apply:
        print("Dry-run only. Pass --apply to migrate.")
        return
    for source, target, content in changes:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
        if target != source:
            source.unlink()
    for source_root in KOTLIN_ROOTS:
        old_parent = root / source_root
        for folder in sorted(old_parent.rglob("*"), reverse=True):
            if folder.is_dir() and not any(folder.iterdir()):
                folder.rmdir()
    stale = [str(p.relative_to(root)) for base in KOTLIN_ROOTS for p in (root / base).rglob("*.kt") if OLD_PACKAGE in p.read_text(encoding="utf-8") or str(OLD_PATH) in str(p)]
    if stale:
        raise RuntimeError("Legacy Kotlin references remain: " + ", ".join(stale[:10]))
    print("Kotlin migration complete: no legacy package references or paths in Kotlin sources.")
    print("Build and inspect git diff before committing.")
    print("Firebase config, applicationId, iOS bundle ID, and backend URL were NOT changed.")


if __name__ == "__main__":
    main()
