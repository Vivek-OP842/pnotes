# Pnotes

Android app repository. The full Android project lives in the [`pnotes/`](pnotes/) folder — open that folder, not the repository root, in Android Studio.

```
.
├── pnotes/       # Android Studio project (app sources, gradle, fastlane)
└── update.sh     # helper that commits and pushes app changes
```

## Requirements

- JDK 17
- Android SDK (compile SDK as configured in `pnotes/app/build.gradle`)
- No extra setup: the Gradle wrapper is included.

## Build

```bash
cd pnotes
./gradlew assembleDebug
```

On Windows (Git Bash) use `./gradlew assembleDebug` as well; on CMD/PowerShell use `gradlew.bat assembleDebug`.

The debug APK is written to:

```
pnotes/app/build/outputs/apk/debug/app-debug.apk
```

Release builds use `./gradlew assembleRelease` and need a signing config (keystores and `local.properties` are not in the repo).

## Push changes

`update.sh` stages only the app folder (and this README/.gitignore), commits, and pushes to `master`:

```bash
./update.sh "what you changed"
```

The message is optional and defaults to `update app`. If there is nothing new, the script prints a message and exits without committing. Equivalent manual flow:

```bash
git add pnotes README.md .gitignore
git commit -m "what you changed"
git push origin master
```
