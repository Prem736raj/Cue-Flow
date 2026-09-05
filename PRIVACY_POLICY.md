# CueFlow Privacy Policy

**Effective date:** September 6, 2026

CueFlow is an Android teleprompter and local script workspace. This policy explains what the app accesses, when data can leave the device, and how locally stored content is handled.

## 1. Accounts, advertising, and analytics

CueFlow does not require a CueFlow account. The current release does not include an advertising SDK or an analytics SDK and does not operate a CueFlow developer backend for script storage.

## 2. Scripts, folders, bookmarks, and preferences

Scripts, folders, bookmarks, prompting preferences, and related app settings are stored locally on the Android device. CueFlow disables Android application backup for its app data so these private scripts and preferences are not automatically included in the app's cloud/device backup configuration.

CueFlow does not upload this local script database to a CueFlow server.

## 3. Camera

Camera access is used only after the user enters a feature that needs camera preview or video recording and grants Android camera permission.

Camera preview frames are used to display the preview in the app. CueFlow does not upload camera preview frames to a CueFlow server.

When the user records a video, CueFlow uses Android CameraX and saves the resulting video through Android MediaStore under `Movies/CueFlow`. The video remains on the user's device unless the user later chooses to move, back up, or share it through another app or service.

## 4. Microphone and speech recognition

Microphone access is requested when a user chooses a feature that needs audio, such as recording video with audio, dictating a script, or using voice-synchronized prompting.

Video-recording audio is written into the locally saved recording when microphone permission is granted.

Dictation and voice sync use Android's speech-recognition service. The recognition service is supplied by the user's Android device or configured recognition provider, not by CueFlow. Depending on that provider and the user's device settings, speech audio may be processed on-device or transmitted to the recognition provider for processing. CueFlow does not receive a copy of speech audio on a CueFlow developer server.

Users who do not want speech sent to an online recognition provider should avoid dictation/voice-sync features unless they have confirmed that their configured Android recognition provider supports and is using offline recognition.

## 5. Online imports

CueFlow's core script editor and prompting features can operate without internet access. Network access occurs when the user explicitly asks CueFlow to import content from an online source.

### Web article import

CueFlow connects directly to the HTTPS URL supplied by the user, downloads a bounded response, extracts readable text on the device, and adds that text to the user's script only after the user chooses the import.

### Google Docs import

When a user supplies a `docs.google.com` sharing link, CueFlow requests the document's plain-text export directly from Google over HTTPS. The document must already be accessible through the supplied sharing link. The downloaded text is processed on the device.

CueFlow does not proxy these imports through a CueFlow developer server.

## 6. Wi-Fi remote

When the user explicitly enables the Wi-Fi remote during a prompting session, CueFlow opens a temporary HTTP server on the phone's local network.

The remote:

- generates a fresh cryptographically random pairing token for each server session;
- requires that token for remote status and control requests;
- does not expose script text or script titles;
- accepts state-changing controls only through authenticated POST requests;
- does not enable cross-origin requests;
- stops when disabled, when the prompting session closes, or after an idle timeout.

Because the remote is intended for devices on the same local network and does not have a publicly trusted TLS certificate for the phone's temporary LAN address, the local pairing page uses HTTP. Users should enable this feature only on a Wi-Fi network they trust.

## 7. Physical clickers and keyboards

CueFlow can respond to compatible external keyboard-style presentation clickers through Android's input-device APIs. CueFlow does not request access to the user's paired Bluetooth device list for this feature.

## 8. Permissions

Depending on the features the user chooses, CueFlow can request:

- **Camera** — live preview and video recording.
- **Microphone** — recording audio, dictation, and voice sync.
- **Display over other apps** — floating teleprompter mode.
- **Notifications** — ongoing foreground-service status and controls where required by Android.
- **Internet** — user-requested online imports and the local-network remote.

CueFlow also uses an Android foreground service while the floating overlay is active so the user can see and control the ongoing teleprompter session.

## 9. Retention and deletion

CueFlow does not maintain a developer-hosted user account or cloud script database in this release.

Users can delete individual scripts and folders from the app. Deleting a folder keeps its scripts and returns them to the unassigned/all-scripts view. Android app data can be removed by clearing CueFlow storage or uninstalling CueFlow. Locally recorded videos are stored in shared MediaStore storage and may remain after app data is cleared or the app is uninstalled; users can delete those recordings from their gallery or file manager.

Data handled by an Android speech-recognition provider or an online source is subject to that provider's own retention and privacy practices.

## 10. Security

CueFlow limits online imports by size, accepts only HTTPS web imports, does not ship a developer-owned cloud API credential, disables app-data backup, and pairs the optional LAN remote with a per-session token. No software can guarantee absolute security, and users should keep Android and their recognition/network providers up to date.

## 11. Children

CueFlow is a general productivity and creator tool and is not designed specifically for children. The Play Console target-audience selection should match the audience actually chosen by the developer before release.

## 12. Changes to this policy

If a future CueFlow release adds analytics, advertising, accounts, cloud sync, AI services, crash reporting, or other data-processing SDKs, this privacy policy and the Google Play Data Safety declaration must be updated before that release is distributed.

## 13. Contact

Privacy and support inquiries may be sent to **support@cueflow-app.com**.

**Release requirement:** the developer must verify that this mailbox/domain is controlled and monitored before publishing this policy. The final policy must be hosted at a stable, publicly accessible, non-geofenced web URL and linked in Google Play Console.
