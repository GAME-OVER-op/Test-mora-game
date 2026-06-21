# Build output

This repository is configured to output a **Magisk module ZIP**, not a standalone APK.

GitHub Actions workflow:

```text
.github/workflows/build.yml
```

It builds:

1. Rust daemon: `target/aarch64-linux-android/release/perf_daemon`
2. GameSpace APK: `app/build/outputs/apk/release/app-release.apk`
3. Magisk module: `dist/GameSpace-magisk-module.zip`

The uploaded artifact is only the module:

```text
GameSpace-magisk-module
```

Manual packaging after local builds:

```sh
cargo build --release --target aarch64-linux-android
gradle :app:assembleRelease
scripts/package_magisk.sh
```

Output:

```text
dist/GameSpace-magisk-module.zip
```
