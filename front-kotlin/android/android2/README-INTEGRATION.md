Integration steps for UniFFI/Rust + Android

Local - prerequisites
- Install Android SDK & NDK (r21+). Set `ANDROID_HOME`/`ANDROID_SDK_ROOT`.
- Install Rust toolchain and `cargo-ndk` and `uniffi-bindgen` if generating bindings locally.

Generate UniFFI Kotlin bindings (local)
1. From `Wallet-Rust` run `uniffi-bindgen generate --language kotlin --out-dir ../kotlin-bindings src/wallet.udl` (adjust paths).

Build Rust native libraries for Android (local)
1. Install `cargo-ndk`: `cargo install cargo-ndk`
2. From `Wallet-Rust` run:
```
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -- build --release
```
3. Copy resulting `libwallet_core.so` files into `front-kotlin/android/android/app/src/main/jniLibs/<ABI>/`.

CI (GitHub Actions)
- This repo includes `.github/workflows/rust-android-ci.yml` which builds Rust ABIs and then runs `./gradlew assembleDebug`.

Android build
```
cd front-kotlin/android/android
./gradlew assembleDebug
```

Testing
```
./gradlew test
```

Notes
- Native toolchain steps require local Android NDK and Rust cross toolchains — CI automates this but local dev must install them.
- If UniFFI UDL changes, re-run `uniffi-bindgen` and commit generated Kotlin under `Wallet-Rust/kotlin-bindings`.
