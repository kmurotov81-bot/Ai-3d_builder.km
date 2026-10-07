# Socratic Math Tutor (Android, Kotlin + Jetpack Compose)

A patient, Socratic math tutor. Snap or type a calculus/algebra problem; the tutor teaches
ONE step at a time and asks a guiding question. Tap **Why did we do that?** on any step to get
an explanation of just that concept (without advancing). A Desmos graphing calculator sits
above the chat and the tutor can push functions, tangents, etc. into it.

## Setup
1. Open this folder in Android Studio (Koala or newer). Let it sync (it creates the Gradle wrapper).
2. Copy `local.properties.example` into `local.properties` and set:
   - `ANTHROPIC_API_KEY`  (console.anthropic.com)
   - `DESMOS_API_KEY`     (www.desmos.com/api)
3. Run on a device/emulator with Android 9+ (API 28).

## How it works
- `TutorPrompt.kt`: the pedagogy. One step per reply, one question, [WHY]/[STUCK] modes, JSON output.
- `ClaudeClient.kt`: Anthropic Messages API (text + base64 image blocks) over OkHttp.
- `TutorViewModel.kt`: conversation history, photo handling, Why / Stuck actions.
- `DesmosPanel.kt`: Desmos in a WebView; the tutor's expressions are replaced on each update, the student's own are kept.
- `TutorScreen.kt`: Compose UI (chat, camera/gallery, graph toggle).

## Before you ship
- **Do not ship the API key inside the APK.** It can be extracted. For release, send requests to
  your own small backend that holds the key and forwards to the API (add per-user auth and rate limits).
- Use your own Desmos API key; check their terms for your use case.
- Math in chat is shown as Unicode text (x², √, ∫). For typeset math, render messages with KaTeX in a WebView.

## Building on GitHub (Actions)
1. Create a **private** repo and push this folder (`.github/workflows/android.yml` is included).
2. Repo > Settings > Secrets and variables > Actions > add `ANTHROPIC_API_KEY` (and `DESMOS_API_KEY` once you have it).
3. Push to `main` (or run the workflow manually from the Actions tab). Download the APK from the run's **Artifacts**.
Without `DESMOS_API_KEY` the build falls back to Desmos' public demo key (testing only).
Anyone who can download the artifact can extract the embedded Anthropic key, so keep the repo private
and move to a backend proxy before distributing the app.
