# Reel2Playlist — Free Spotify companion MVP scaffold

## What this version provides
- Native Kotlin / Jetpack Compose Android app
- Floating overlay music-note button
- Tap bubble to open the app and start a microphone recognition attempt
- AudD recognition adapter (requires your own AudD API token)
- Local song history saved on device
- Open a recognized song in Spotify via Spotify search URI / web fallback
- No Spotify Premium or Spotify Web API required for this free workflow

## Important limitations
1. Song recognition is not free/unlimited by default. You must obtain an AudD API token and comply with its plan, quotas, pricing and terms. Audio is uploaded to AudD for recognition.
2. This first implementation records 12 seconds through the microphone. It does not directly capture Instagram's internal audio. Recognition works best when Reel audio is audible through the phone speaker; headphones/Bluetooth routing and background noise may prevent recognition.
3. Android foreground-service and microphone restrictions vary by OS version. Start the service from the app while visible; grant overlay and microphone permissions. Test on the target device.
4. Spotify search deep links open search results; they do not add songs to playlists. Adding to playlists remains manual in this free version.
5. This is source code, not a tested APK. Build and test in Android Studio before installing.
6. The current overlay opens the app when tapped; it is a functional UI scaffold, not yet the final seamless background-listening interaction. Android 14+ microphone foreground services have strict while-in-use rules, so the production flow must start microphone capture while the app is visible and keep that user-started capture session active before switching back to Instagram.

## Setup
1. Open this folder in Android Studio and sync Gradle.
2. Run the app on an Android device.
3. Obtain an AudD API token from https://audd.io/ and enter it in the app.
4. Grant microphone permission.
5. Enable the floating button (grant Display over other apps).
6. Play a Reel aloud, tap the bubble, wait for recognition, then tap Open in Spotify in history.

Never put private service credentials in a public repository. This MVP stores the AudD token locally on-device; for a production release, move API calls behind a secure backend.
