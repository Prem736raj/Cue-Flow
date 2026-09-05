# CueFlow Play Release Checklist

This checklist is for the audited **CueFlow 1.0.0** Android release (`com.cueflow.floating.teleprompter`).

## Build identity

- [x] Application ID: `com.cueflow.floating.teleprompter`
- [x] App name: CueFlow
- [x] `versionCode = 1`
- [x] `versionName = 1.0.0`
- [x] `minSdk = 24`
- [x] `targetSdk = 36`
- [x] Java/JDK 17 build configuration
- [x] Release minification and resource shrinking enabled
- [x] Gradle wrapper committed
- [ ] Final signed Android App Bundle produced with the developer-controlled upload key

### Required release-signing environment values

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The build intentionally does not fall back to a repository-local release keystore.

## Privacy policy

- [x] Repository privacy policy matches the audited behavior
- [x] In-app privacy summary explains local data, online imports, Android speech recognition, recording, and Wi-Fi remote behavior
- [ ] Verify that `support@cueflow-app.com` is controlled and monitored
- [ ] Publish `PRIVACY_POLICY.md` as a stable, public, non-geofenced HTML/web page
- [ ] Put that exact public URL in Play Console > App content > Privacy policy

Google Play requires a privacy policy even when the developer does not collect user data. The production policy should remain synchronized with the Data Safety form whenever behavior changes.

## Data Safety working declaration

Treat this as a conservative starting point and re-check the final AAB plus every bundled SDK before submission.

### CueFlow developer backend

- Developer-operated account system: **No**
- Developer-operated cloud script storage: **No**
- Advertising SDK: **No**
- Analytics SDK: **No**
- Developer-owned AI API: **No**

### Local data

The following are processed/stored locally and are not transmitted to a CueFlow developer server:

- scripts and folders;
- bookmarks and teleprompter settings;
- camera preview frames;
- videos and recording audio saved through Android MediaStore;
- local-network teleprompter state used by the explicitly enabled paired Wi-Fi remote.

On-device-only processing is not treated as off-device collection for the Data Safety form.

### Android speech recognition — verify and declare conservatively

CueFlow invokes Android `SpeechRecognizer` for dictation and voice sync. The configured recognition provider may transmit microphone audio off-device. Before submission:

- [ ] Identify the recognition-provider behavior on supported Android builds used for testing.
- [ ] If speech audio can be transmitted off-device through the invoked recognition provider, declare the applicable **Audio / Voice or sound recordings** collection for **App functionality**, optional/user-triggered, using the retention/ephemeral options that accurately match the provider behavior.
- [ ] Ensure the privacy policy names Android/device speech recognition as a possible third-party/system recipient.

Do not claim that voice sync is guaranteed to be offline.

### Online imports

CueFlow connects directly to user-selected HTTPS pages and Google Docs only when the user initiates an import. Re-check whether any final dependency adds telemetry around these requests; the audited implementation does not.

## Permissions and sensitive access

Manifest/runtime behavior to verify on a clean device:

- [x] `CAMERA` — requested at Record Video/camera feature boundary
- [x] `RECORD_AUDIO` — requested separately for recording audio, dictation, or voice sync
- [x] `SYSTEM_ALERT_WINDOW` — user is sent to Android Draw over other apps settings only when launching Floating Overlay
- [x] `INTERNET` — HTTPS imports and explicitly enabled LAN remote
- [x] `POST_NOTIFICATIONS` — foreground-service notification on Android versions that require runtime consent
- [x] `FOREGROUND_SERVICE`
- [x] `FOREGROUND_SERVICE_SPECIAL_USE`
- [x] `FOREGROUND_SERVICE_MICROPHONE`
- [x] Legacy storage permissions removed
- [x] Bluetooth device-list permissions removed

## Foreground service declaration

CueFlow's floating overlay is user-started and user-visible. Play Console requires a foreground-service declaration for target Android 14+ apps.

### `specialUse`

**Functionality:** Keeps the user-started floating teleprompter overlay alive while another camera, social, video-call, or streaming app is in the foreground.

**Why immediate/user-perceptible:** The overlay is the feature the user explicitly launched. Deferring it would mean the teleprompter is missing while the user is trying to record or speak. Interruption removes the visible script and controls during the active session.

### `microphone`

**Functionality:** When voice sync is explicitly enabled and microphone permission has been granted, the foreground teleprompter can use Android speech recognition to follow the user's spoken position.

**Why immediate/user-perceptible:** Voice sync directly controls the visible scroll position while the user is speaking; interruption makes the displayed script fall out of sync.

### Required Play declaration evidence

- [ ] Record a short review video beginning in CueFlow, showing the user launching Floating Overlay and the ongoing notification.
- [ ] In the same or a second video, show the user explicitly enabling voice sync after microphone consent.
- [ ] Provide the user impact descriptions above in Play Console > App content > Foreground service permissions.

## Content rating and target audience

- [ ] Complete the IARC content-rating questionnaire based on the actual app: productivity/creator utility, no user-to-user chat, no gambling, no sexual/violent content supplied by CueFlow.
- [ ] Select the real target age groups. Do not select child-directed age groups simply to broaden reach; camera/microphone handling and Families requirements become more restrictive when children are targeted.
- [ ] Confirm there is no unrated production artifact.

## Store listing

- [x] `STORE_LISTING.md` rewritten around actual v1 features
- [x] No AI feature claim
- [x] No unsupported 1080p-only guarantee; quality preference has fallback
- [x] No absolute privacy/security claims
- [x] No fake Pro/paywall claim
- [ ] 512 x 512 Play Store icon, PNG, max 1024 KB
- [ ] 1024 x 500 feature graphic, JPEG or 24-bit PNG without alpha
- [ ] Minimum 2 phone screenshots
- [ ] Recommended 4+ 1080 x 1920 phone screenshots for stronger merchandising eligibility
- [ ] Add concise alt text for every uploaded screenshot/graphic
- [ ] Verify screenshot UI exactly matches the release AAB

## App access

- [x] No login credentials required
- [x] No subscription/paywall blocks review in v1
- [ ] In Play Console App access, state that all features are accessible without a CueFlow account; Android system permission prompts are feature-triggered rather than account restrictions

## Ads and monetization

- [x] No AdMob/ads SDK in audited v1
- [x] No billing library in audited v1
- [x] Removed misleading `PRO` badge from Floating Overlay
- [ ] If monetization is added later, update store copy, privacy policy, Data Safety, test coverage, and purchase-restoration behavior before rollout

## Final technical gate

Run on the exact release branch/artifact:

```bash
./gradlew clean testDebugUnitTest lintRelease assembleDebug assembleRelease
```

Then, with real signing credentials available:

```bash
./gradlew bundleRelease
```

Before production rollout also test on at least:

- Android 7/8 class low-memory device or emulator (minSdk behavior)
- Android 12/13 device (runtime Bluetooth/storage regressions and notification behavior)
- Android 14/15 device (foreground-service and overlay behavior)
- Android 16/API 36 device (targetSdk behavior)
- One budget/mid-range physical phone with 4–6 GB RAM

## User-owned blockers before production

These cannot be generated safely by code or by the audit:

1. A developer-controlled release/upload signing key and credentials.
2. A verified support/privacy contact channel.
3. A stable public privacy-policy URL.
4. Final screenshots and 1024 x 500 feature graphic captured/designed from the final build.
5. Play Console IARC, target-audience, Data Safety, and foreground-service declaration submissions.
