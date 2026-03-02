#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="$(pwd)/../target/android-libs"
mkdir -p "$OUT_DIR"

if ! command -v cargo-ndk >/dev/null 2>&1; then
  echo "Please install cargo-ndk (cargo install cargo-ndk) or use rustup + cargo-ndk via rustup toolchain." >&2
  exit 2
fi

ABIS=(arm64-v8a armeabi-v7a x86_64)
for abi in "${ABIS[@]}"; do
  echo "Building for $abi"
  cargo ndk -t ${abi} --release build -p wallet_core
  # locate .so
  SRC="$(pwd)/target/${abi}/release/libwallet_core.so"
  if [ -f "$SRC" ]; then
    mkdir -p "$OUT_DIR/$abi"
    cp "$SRC" "$OUT_DIR/$abi/libwallet_core.so"
    echo "Copied $SRC -> $OUT_DIR/$abi/"
  else
    echo "Warning: $SRC not found" >&2
  fi
done

echo "Android ABIs built into $OUT_DIR"
