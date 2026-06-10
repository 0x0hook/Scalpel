#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${GHIDRA_INSTALL_DIR:-}" ]]; then
    echo "Set GHIDRA_INSTALL_DIR to your Ghidra install path." >&2
    exit 1
fi

if [[ -x "./gradlew" ]]; then
    ./gradlew buildExtension
else
    gradle buildExtension
fi
