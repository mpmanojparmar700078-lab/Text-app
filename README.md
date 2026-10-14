# ITI PDF AI Exam Android App

An AI-powered Android application built for Industrial Training Institute (ITI) students to prepare for semester theory and practical examinations. The application extracts educational content directly from syllabus PDFs, generates structured multiple-choice questions (MCQs) using the Google Gemini API, conducts mock tests with instant scoring, and stores complete test histories locally.

---

## 🛠️ Mandatory Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: MVVM with Repository pattern
- **Local Database**: Room (SQLite) with multi-user isolation
- **PDF Selection**: Android Storage Access Framework (SAF)
- **PDF Text Extraction**: PDFBox-Android (`com.tom-roush:pdfbox-android`)
- **AI Engine**: Google Gemini API (`gemini-3.5-flash`) via Retrofit & OkHttp (60s timeout)
- **Preferences**: Jetpack DataStore Preferences
- **Build System**: Gradle Kotlin DSL (`.gradle.kts`) with Version Catalog (`libs.versions.toml`)

---

## 🚀 Key Features

### 1. Local Profile System
- Multi-user support on a single shared device without needing internet cloud accounts.
- Each profile has its own private PDFs, questions, attempts, and test histories.
- Profile switching is instantaneous.
- Admin profile protection: Switching to or creating an Admin profile requires an Admin Authorization PIN (Default PIN: `8070`).

### 2. Two Distinct PDF Libraries
- **My Private PDFs**:
  - Personal study materials imported by individual students via Android's native file picker.
  - Stored securely in app-private directories (`filesDir/pdfs/{userId}/`).
  - Strict privacy: User A cannot see or delete User B's private PDFs.
- **Admin PDF Library**:
  - Official trade documents (Electrician, Fitter, Welder, COPA, Wireman, etc.) uploaded by instructors.
  - Visibility controls: Toggle between **Visible to All Local Profiles** (Published) and **Hidden**.
  - Database-enforced isolation: Hidden PDFs are strictly filtered out of normal students' exam choices.

### 3. Scanned PDF & OCR Detection
- Detects image-only/scanned documents where extracted digital characters are insufficient.
- Flags document with an **OCR Needed** status badge and provides clear student guidance.

### 4. Gemini AI Question Generation
- Generates 10, 20, 30, 40, or 50 questions strictly from the chosen PDF.
- Language support: English or हिंदी (Hindi).
- Validation: Exactly 4 distinct options, exactly 1 validated correct index (0..3), source page reference, and clear technical explanation.
- Deduplication and schema enforcement.

### 5. Exam & Result Review
- Full practice mock exam screen with question counter, progress bar, selectable options, and interactive question palette.
- Formula score calculation:
  $$\text{Score Percentage} = \left(\frac{\text{Correct Answers}}{\text{Total Questions}}\right) \times 100$$
- Detailed question-by-question review showing student's choice, correct answer, explanation, and page citation.

---

## 🔐 API Key Configuration & Security

The Gemini API key can be configured in two ways:
1. **Secrets Panel in AI Studio**: Set the `GEMINI_API_KEY` secret. AI Studio will automatically inject it into `BuildConfig.GEMINI_API_KEY` at runtime.
2. **In-App Settings**: Enter your API key manually under **Settings > Google Gemini API Key**.

> **Security Note**: In distributed Android APKs, client-side API keys cannot be guaranteed 100% secret. For production release apps distributed to millions of users, Google recommends routing generative requests through a secure proxy backend or using Firebase App Check.

---

## 📦 How to Build the App

### Debug APK Build
```bash
gradle assembleDebug
```
The generated APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Local JVM Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```
