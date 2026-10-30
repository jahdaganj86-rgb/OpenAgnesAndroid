# Agent Instructions

## Android APK build

- Build the Android APK **only via GitHub Actions**, never locally.
- The workflow is `.github/workflows/build.yml` (`Build Debug APK`). It runs on pushes to `main` and on manual dispatch, then uploads `app/build/outputs/apk/debug/app-debug.apk` to a release.
- Do not run `./gradlew assembleDebug` (or other APK packaging tasks) in the dev container.
- Local verification is limited to compile/lint checks, e.g.:
  - `./gradlew :app:compileDebugKotlin`
  - `./gradlew :app:lintDebug`