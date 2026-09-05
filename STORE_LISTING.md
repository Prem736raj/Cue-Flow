# CueFlow: Floating Teleprompter — Store Listing Assets Document

Prepare your Google Play Console store presence using this fully optimized, professional asset guide.

---

## 1. Google Play Console Core Text Metadata

### **App Title (Exactly 29 Characters — Limit: 30)**
```text
CueFlow: Floating Teleprompter
```

### **Short Description (Exactly 79 Characters — Limit: 80)**
```text
Speak confidently with a dual floating camera overlay and smart voice sync!
```

---

### **Full Description (Limit: 4000 Characters)**

```text
CueFlow is the ultimate production-grade, offline-first floating teleprompter designed for presenters, content creators, public speakers, teachers, and video professionals. Speak with absolute authority and crystal-clear execution while keeping direct eye contact with your camera at all times.

Whether you are recording YouTube videos, TikToks, Instagram Reels, Zoom presentations, or professional lectures, CueFlow gives you a seamless, edge-to-edge transparent overlay that glides perfectly over any application.

---

🌟 KEY FEATURES THAT SET CUEFLOW APART:

1. DUAL FLOATING OVERLAYS (PICTURE-IN-PICTURE)
Need to run your script over Zoom, Google Meet, or your native camera app? CueFlow offers adjustable transparent floating text windows. Resize, reposition, and customize opacity on the fly so you can reference your lines while keeping your gaze locked onto the lens.

2. REAL-TIME VOCAL RHYTHM SYNC (VOICE SYNC Tracking)
Tired of pre-set scroll speeds? Switch on our integrated voice sync mode. CueFlow dynamically analyzes your vocal cadence, reading pauses, and accents, moving the text forward ONLY as you speak. No internet required, no voice recordings saved.

3. BUILT-IN VIDEO ACQUISITION CAMERA
Create, record, and review within a single interface. Use our smart background camera preview with front/rear mirror toggles, flash options, and 1080p high-definition video capture.

4. NEXT-GEN AI SCRIPT GENERATOR
Stuck on what to say? Craft high-converting scripts in seconds. Choose your topic, tone, and target duration, and let our Gemini-powered smart AI draft compelling opening hooks, mid-arguments, and call-to-actions directly into your editor.

5. SECURE OFFLINE CORE & DECENTRALIZED PRIVACY
Your scripts and data belong to you. CueFlow operates with 100% database containment. No accounts, no data logging, no analytics trackers, and no mandatory internet connections. Completely local, completely private.

6. FULL STYLE & VELOCITY CUSTOMIZATION
Make CueFlow look exactly the way you want. Adjust margins, scrolling velocity from 1x to 10x, text alignments, borders, background opacities, and size parameters. Save configurations dynamically across custom AMOLED, Midnight Blue, Sunset Amber, or Light themes.

---

🚀 WHY CHOOSE CUEFLOW?
• Completely free with absolute feature access!
• No distracting watermarks ever printed on your recordings.
• Full compatibility with Bluetooth clickers and hardware media controller keys.
• Zero internet requirement for standard prompter features, fully functional offline in airplane mode.

Unlock your full presentation flow today. Download CueFlow: Floating Teleprompter and own the room!
```

---

## 2. Graphic Asset Design Concepts

### **Feature Graphic (1024 x 500 px)**
* **Visual Theme:** Deep Cosmic Slate Slate Background (`#090A0F`) with vibrant glowing, soft gradient laser lines of Electric Purple (`#9F7AEA`) and Electric Cyan (`#38BDF8`).
* **Main Art Component:** A central 3D-styled floating tablet showing an active, transparent teleprompter overlay with text. Beside the tablet, a smartphone displaying a glowing camera lens looking direct at the presenter.
* **Header Typography (Bold, high contrast display):** "CUEFLOW"
* **Supporting Tagline Text:** "Own Your Presentation. Free Floating Teleprompter."

---

## 3. High-Converting Play Store Screenshots (5-8 Sequence Plan)

These screenshots should show off CueFlow's modern, atomic visual identity and premium theme options.

### **Shot 1: The Hook (Welcome / Primary Dashboard)**
* **Visual Content:** The gorgeous Cosmic dashboard showing your organized library of scripts with modern folders, card components, and the neon glowing floating FAB.
* **Heading Text Overlay:** "ORGANIZED ATOMIC WORKSPACE"
* **Sub-caption:** "Review, search, and generate scripts from one cohesive hub."

### **Shot 2: The Core (Floating Teleprompter Mode)**
* **Visual Content:** CueFlow running overlay-style atop a native camera app recording a video. Text scrolling smoothly inside a semi-transparent, neon-bordered window.
* **Heading Text Overlay:** "FLOATING SYSTEM OVERLAY"
* **Sub-caption:** "Keep constant, direct eye contact with your camera lens."

### **Shot 3: The Tech (Voice Rhythm Sync)**
* **Visual Content:** The playback calibration dialog showcasing the real-time audio microphone meter fluctuating. The "Vocal Sync" toggle lit up in electric cyan.
* **Heading Text Overlay:** "SMART VOCAL RHYTHM SYNC"
* **Sub-caption:** "Text crawls dynamically, locking onto your natural voice speed."

### **Shot 4: The Creator (Dual Camera Acquisition)**
* **Visual Content:** Inline front-camera recording feed inside the app with mirror orientation buttons, high-definition video parameters, and active teleprompter lines.
* **Heading Text Overlay:** "CAPTURE STUNNING VIDEOS"
* **Sub-caption:** "Record crisp 1080p presentations without watermarks."

### **Shot 5: The Writer (AI Script Generator)**
* **Visual Content:** The smart AI script modal generator prompting with topic selection, customized tones (Professional, Energetic, Casual), and the Gemini assistant drafting a high-converting script outline.
* **Heading Text Overlay:** "GEMINI-AI SCRIPT ENGINE"
* **Sub-caption:** "Draft compelling pitches and YouTube hooks in seconds."

### **Shot 6: Creative Control (Custom Styling Options)**
* **Visual Content:** The comprehensive settings dialog showing premium AMOLED black, Sunset Amber, and Midnight naval themes with precise font sliders and speed tuning.
* **Heading Text Overlay:** "FULL ESTHETIC CONTROL"
* **Sub-caption:** "Style scrolling velocities, opacity margins, and color presets."

---

## 4. Play Store Launch Checklist (Technical Optimizations and Key Creation)

Follow these direct, step-by-step instructions to compile your production-signed bundle and launch onto Google Play:

### Step 1: Generate a Upload Keystore
To sign your app for Google Play distribution, you must generate a secure keystore file if you don't already have one:
1. Open your terminal or shell in your local development workspace.
2. Run the `keytool` utility (bundled with your Java Development Kit/JDK):
   ```bash
   keytool -genkey -v -keystore cueflow-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias cueflow-key
   ```
3. Enter a secure password (make sure to write this password down!) and answer the profile questions.
4. This generates the `cueflow-release.jks` file in your root workspace. **Never commit this file to public repositories.**

### Step 2: Configure your `app/build.gradle.kts`
Modify the `signingConfigs` and `buildTypes` inside your app's Gradle configurations:
```kotlin
android {
    ...
    signingConfigs {
        create("release") {
            storeFile = file("../cueflow-release.jks")
            storePassword = "YOUR_KEYSTORE_PASSWORD"
            keyAlias = "cueflow-key"
            keyPassword = "YOUR_KEY_PASSWORD"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### Step 3: Compile your Production Android App Bundle (.aab)
Using Android Studio or your console terminal, run the following Gradle task:
```bash
gradle :app:bundleRelease
```
This command compiles the optimized distribution-ready `.aab` file located at:
`/app/build/outputs/bundle/release/app-release.aab`

### Step 4: Upload to Google Play Console
1. Navigate to the [Google Play Console](https://play.google.com/apps/publish).
2. Create a new Application called **CueFlow: Floating Teleprompter** and complete the Declarations questionnaires.
3. Set up your **Internal Testing** or **Production Track**.
4. Upload the generated `app-release.aab` file. Google Play App Signing will handle signing keys seamlessly.
5. Fill out the Store Listing assets using Section 1 details, upload your high-resolution adaptive app icon, feature graphic, and screenshots.
6. Submit for review! Our compliance targets ensure rapid approvals.
