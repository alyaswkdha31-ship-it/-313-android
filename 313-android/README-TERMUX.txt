313 Android APK - Termux build/install

1. Download 313-android-source.zip to /sdcard/Download.
2. Extract it in Termux or copy the folder to $HOME/313-android.
3. Run:
   bash ~/313-android/build-install-termux.sh

The script reuses ~/gradle-8.7/bin/gradle and the Android SDK path from
~/OnyxSmartTV/local.properties when available. It builds app-debug.apk,
copies it to /sdcard/Download/313.apk, and opens Android's package installer.
You must confirm the final Android installation prompt yourself.

Note: this first build is a launcher shell with official service links only.
It does not yet implement provider API/OAuth integrations or multi-agent task orchestration.
