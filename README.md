# MobileCursor IDE (Android)

A mobile-first IDE concept inspired by Cursor: code editor with Monaco in a WebView, built-in terminal, and AI chat panel (wire this to your backend). Ships as a native Android app (Jetpack Compose).

## Features (MVP)
- **Editor**: Monaco editor loaded from CDN (vs-dark), inline bridge for get/set content.
- **Terminal**: Runs commands via `/system/bin/sh -c <cmd>`; streams stdout+stderr into the UI.
- **AI Chat stub**: Chat box; hook to your AI endpoint and stream replies.
- Compose UI with bottom navigation (Editor / Terminal / AI).

## Build
GitHub Actions workflow provided (`.github/workflows/android.yml`) builds a debug APK.
Local build:
```sh
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## TODO / Next steps
- Wire AI panel to your backend (REST/WebSocket streaming) and surface inline code actions.
- Implement real file-tree + save/apply patches between editor and local storage.
- Upgrade terminal to a full PTY + Ubuntu (proot) environment; persist workspace at `/storage/emulated/0/Android/data/com.mobilecursor.ide/files/workspace`.
- Secure sandbox: ask confirmation before AI writes files or runs commands.
- Add gesture-friendly cursor controls and external keyboard shortcuts.

## Licensing
- Monaco editor is MIT (loaded from CDN). Ensure network access.
- The rest of this scaffold is MIT.
