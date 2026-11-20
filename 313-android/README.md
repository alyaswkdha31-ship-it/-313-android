# مركز قيادة إيلاف

This branch integrates the existing native 313 Android application with the ONYX CONTROL dashboard.

## Included
- Existing 313 native chat and project Team Console.
- Arabic speech input and copy-to-clipboard controls from the 313 app.
- Existing Termux loopback gateway integration and encrypted Android gateway-token storage.
- Strict zero-cost OmniRoute policy gate: AI requests remain blocked unless strict free-access mode is confirmed; no paid fallback is allowed.
- ONYX CONTROL opens in a dedicated in-app WebView activity from the 313 home screen.
- Launcher display name: مركز قيادة إيلاف.
- Solar-energy features are intentionally excluded.

## Build
The GitHub Actions workflow builds the existing Android project at `313-android/` and uploads a debug APK artifact named `elafyas-command-center-debug-apk`.

## Current limitations
- The hosted ONYX CONTROL preview needs an internet connection and its own sign-in.
- Android/Termux loopback connectivity has not yet been tested on the user's physical phone.
- Camera cart workflow and prayer-times integration are not implemented in this branch yet.
- A successful CI APK build is not equivalent to a physical-device install/runtime test.
