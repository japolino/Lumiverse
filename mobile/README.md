# Lumiverse Android companion

A small native WebView client for an existing Lumiverse server. Android volume
buttons can page through the chat without touching the screen. This is a separate
mobile client, not a port of the desktop tray or its local server runner.

## Use

1. Install the debug APK from the **Build Android companion** workflow artifact.
2. Tap **Server**, enter your Lumiverse server address, and sign in using the web UI.
3. Turn on **Volume paging**. Volume Up pages up, Volume Down pages down.
   Each press moves 85% of the visible chat height. Holding a button does not repeat.
4. Turn the switch off to use normal volume controls. The preference is saved.
   On-screen **Up** and **Down** buttons also page the chat.

Paging targets the visible `data-chat-scroll` container already present in
Lumiverse. It does nothing while an input is focused, a dialog is open, or no
chat is visible. While volume paging is enabled on a loaded server, volume
buttons are consumed even when paging does nothing. Disable the switch to adjust
audio, including during TTS playback. Background volume controls are unaffected.

The companion uses the chat's existing wheel handler to stop automatic following
before scrolling. Paging needs no server update. TLS errors are never bypassed;
HTTPS requires a valid certificate trusted by Android. Private IPv4 addresses, including
100.64.0.0/10 VPN addresses, default to HTTP when no scheme is entered. Other
addresses default to HTTPS. An explicit http:// or https:// always takes precedence.
HTTP has no TLS encryption; use it over a trusted connection such as your VPN.
Web content has no JavaScript-to-native bridge and no file access. Links outside
the selected origin open in the system browser when explicitly tapped.

Sign-in uses the WebView cookie store, separate from Chrome and Lumiverse Desktop.
External identity-provider flows that require browser callbacks are not implemented.
File uploads, downloads, notifications, floating widgets and local server management
are not included in this initial reading companion. Android WebView renders the
server's current frontend, so frontend updates arrive from your server.

## Build

Requires JDK 17 and Android SDK platform/build tools 35. The Gradle wrapper pins
Gradle 8.11.1 and the Android Gradle plugin is pinned to 8.9.2.

```sh
bun test mobile/tests/page.test.ts
cd mobile/android
# Set ANDROID_HOME, or set sdk.dir in an untracked local.properties.
./gradlew assembleDebug lintDebug testDebugUnitTest
```

On Windows use `gradlew.bat`. The installable, development-signed APK is at
`app/build/outputs/apk/debug/app-debug.apk`. This is a test build, not a Play Store
release. A release needs a maintained signing key; CI debug keys can change
between builds, requiring uninstalling an older test build before reinstalling.

## Device verification

Before merging, test on a physical Android phone:

- Connect, sign in, restart the app, and verify session persistence.
- Page a long chat in both directions, including while a response is streaming.
- Confirm 15% overlap, no repeated paging when held, and loading older history.
- Open the keyboard or a modal and verify the chat behind it does not move.
- Switch volume paging off and test media volume; test it with the app backgrounded.
- Rotate the device and check system bars, keyboard insets and the controls.
- Verify external links open in the browser and invalid TLS is rejected.

The automated paging tests cover direction, visible viewport height, and guards.
They do not replace device testing of hardware buttons and WebView integration.
