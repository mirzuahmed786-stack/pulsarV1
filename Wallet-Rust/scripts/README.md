# Wallet-Rust integration scripts

This folder contains helper scripts to generate UniFFI Kotlin bindings and to build Android `.so` artifacts for the Kotlin frontend.

1) Generate Kotlin UniFFI bindings

Usage (from this folder):

```powershell
./generate_uniffi_bindings.ps1 -UdlPath ../wallet.udl -OutDir ../kotlin-bindings/src/main/kotlin
```

Requires: `uniffi-bindgen` installed (`cargo install uniffi_bindgen_cli`).

2) Build Android `.so` and copy to `jniLibs`

Usage (from this folder):

```powershell
./build_android_so.ps1 -CrateDir .. -Targets arm64-v8a,armeabi-v7a,x86_64 -OutJniLibs ../front-kotlin/android/android/app/src/main/jniLibs
```

Requires: Android NDK on PATH and `cargo-ndk` installed (`cargo install cargo-ndk`). The script calls `cargo ndk` which builds the native crates and writes `.so` into the `jniLibs/<abi>/` directories.

Notes:
- Adjust `-CrateDir` to the Rust crate directory that produces the `libwallet_core.so` (repo layouts differ). If your crate is under `backend/`, point there.
- After copying `.so`, rebuild the Android app: `cd front-kotlin\android\android; .\gradlew.bat assembleDebug`.
