# Socratic Math Tutor (Android)

Teaches students to solve math problems step by step (Socratic questions, hint ladder, "Why?", mistake finding)
instead of giving answers. AI: **Gemini** (default), **Claude**, or **your own backend**.

## Run
1. Copy `local.properties.example` to `local.properties`, set `GEMINI_API_KEY`.
2. Open in Android Studio (or `gradle assembleDebug` / `./gradlew assembleDebug` if you have a wrapper).
3. Tests: `gradle testDebugUnitTest`. Backend tests: `cd backend && node --test`.

## Security
Keys placed in `local.properties` are compiled into the APK: use that for development only.
For release run `backend/` (holds the key, validates, rate-limits) and build with `BACKEND_URL` (+ `APP_TOKEN`).
Never commit `local.properties` or keystores. CI reads secrets from GitHub (Settings > Secrets and variables > Actions).

## Layout (`app/src/main/java/com/example/socratictutor`)
- Core (pure Kotlin, unit tested): `Domain`, `TutorReply` (structured JSON parsing/validation), `TutorSession`
  (state machine + hint ladder), `Progress` (accuracy, level, weak/mastered topics, streak, badges, recommendations),
  `TestModels`, `AiErrors`, `TutorRepository` (retry on invalid JSON), `NavState`.
- AI: `AiClient` + `GeminiClient` / `ClaudeClient` / `BackendClient`, prompts in `TutorPrompt`.
- UI: `HomeScreen`, `TutorScreens` (Tutor, Practice), `ChatScreen`, `TestScreen`, `ProgressScreen`, `SettingsScreen`,
  `MathText` (KaTeX), `DesmosPanel` (graph dialog). ViewModels: `TutorViewModel`, `TestViewModel`.
- Strings: `res/values` (English), `res/values-uz` (Uzbek).

## Known limits
- Math typesetting (KaTeX) and Desmos load from the internet; offline, LaTeX stays as readable text.
- Test questions are AI-written (multiple choice) and not independently verified; the results screen says so.
- No accounts yet (Settings says so). Camera uses the system camera app, so no camera permission prompt exists.
- The Gradle wrapper jar is not included; CI uses `gradle-version: 8.9`. Run `gradle wrapper` once to add it.
