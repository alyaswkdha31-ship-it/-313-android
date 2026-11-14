# 313 Android

Initial Android MVP for the 313 AI command-center app.

## Current milestone
- Native Android app shell (Java, no third-party runtime dependencies)
- Arabic RTL dark dashboard
- Entry cards for ChatGPT, Claude, Gemini, GitHub, and Codex
- Opens each official service in the device browser

## Not yet implemented
Direct API/OAuth integrations, secure credential storage, project sync, agent orchestration, and Codex execution. These require separate authentication and backend work; this first milestone intentionally does not collect or embed API keys.

## Build
Requires JDK 17+, Android SDK 35, and Gradle 8.7-compatible Android Gradle Plugin 8.6.1.

```bash
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`
