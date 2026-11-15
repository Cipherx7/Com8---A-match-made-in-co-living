# CoM8 — Flatmate & Roommate Discovery for Android

**CoM8** is an open-source Kotlin & Jetpack Compose Android app for discovering compatible flatmates, roommates, and PG mates based on lifestyle habits, budget, and preferred city/locality.

---

## Features

- **Authentication & Phone Verification**
  - **Google Sign-In** via Jetpack `CredentialManager` and Firebase Authentication.
  - **Phone OTP Sign-In & Linking** via Firebase Phone Authentication (`PhoneAuthManager`), including `verifyPhoneNumber`, 6-digit OTP verification, resend token handling, and linking a verified phone number to an existing Google account.
- **5-Step Lifestyle Onboarding**
  - Guided setup covering personal identity, living role (`Seeking Flat`, `Have a Flat`, `PG Mate`), preferred cities/localities & monthly rent budget, lifestyle preferences (smoking, drinking, sleep schedule, cleanliness, guest policy, food habits), and verification badges.
  - Friendly post-onboarding pre-prompt for **Notifications** (`POST_NOTIFICATIONS`) and **Approximate Location** (`ACCESS_COARSE_LOCATION`).
- **Swipe-to-Match Discovery & Filters**
  - Compatibility-scored swipe deck with city, role, gender, budget, and strict dealbreaker filters.
  - Interactive **AI Compatibility Insights** powered by Firebase AI Logic (`Firebase.ai`) and App Check.
  - **Native Advanced Ads** integrated into the swipe deck via Google Mobile Ads SDK (`public/app-ads.txt` included for website root hosting).
- **Real-Time Two-User Chat & Safety**
  - Live shared two-user chat threads backed by Cloud Firestore with unread badges and smooth animated transitions.
  - **Report User** moderation flow inside chat storing immutable user-scoped reports in Firestore with reporter ID and server timestamp.
- **Listing Management, Settings & Theming**
  - Create, pause, update, or delete your own flat/roommate post.
  - Light / Dark / System theme persistence via Jetpack DataStore, privacy toggles, app version/last-updated metadata, and external legal links (`Terms`, `Privacy Policy`, `Disclaimer`, `Usage`).

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| **Language & UI** | Kotlin `2.2.10`, Jetpack Compose (Material 3, Edge-to-Edge) |
| **Architecture** | MVVM (`ViewModel`, `StateFlow`, `collectAsStateWithLifecycle`) + Repository pattern |
| **Cloud Backend** | Firebase Authentication, Cloud Firestore (hardened `firestore.rules`), Firebase AI Logic + App Check |
| **Local Storage** | Room (`2.7.0`) cache + Jetpack Preferences DataStore (`1.1.7`) |
| **Ads** | Google Mobile Ads SDK (`play-services-ads`) — Native Advanced |
| **Image Loading** | Coil Compose |
| **Testing** | JUnit 4, Robolectric (`4.16.1`), Kotlin Coroutines Test, Firebase Emulator Suite + `node --test` |
| **Build** | Android Gradle Plugin `9.1.1`, KSP, Secrets Gradle Plugin, Google Services |

---

## Prerequisites

- **Android Studio** (Meerkat / Narwhal or newer) with **Android SDK 36** (`minSdk = 24`, `targetSdk = 36`)
- **JDK 11+** (recommended JDK 17+)
- **Firebase Project** with:
  - **Authentication** enabled for **Google** and **Phone** providers
  - **Cloud Firestore** database created
  - **Firebase App Check** configured (Play Integrity for release, Debug provider for local testing)
- **Node.js 18+** and **Firebase CLI** (optional, for local Firestore security rules testing)

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/<your-username>/CoM8.git
cd CoM8
```

### 2. Configure Firebase

1. Create or select a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
2. Register an Android app using your package name (default in `app/build.gradle.kts`: `com.aistudio.applet.ievans`) and add your signing **SHA-1** and **SHA-256** fingerprints.
3. Enable **Google** and **Phone** under **Authentication → Sign-in method**.
4. Download your `google-services.json` and place it in `app/google-services.json`.
5. Update `app/src/main/res/values/firebase_applet_config.xml` (or `default_web_client_id` / `firestore_database_id`) to match your Firebase project and Firestore database ID, and ensure `firebase.json` matches your database ID if running local emulators.

### 3. Configure Environment Secrets (Optional)

The Secrets Gradle Plugin reads optional environment variables from `.env` (gitignored) with defaults in `.env.example`:

```bash
cp .env.example .env
```

> **Security Note:** Never commit `.env`, `debug.keystore`, upload keystores (`*.jks`), or production API credentials to version control.

### 4. Configure Google Mobile Ads (Optional)

If you fork or publish your own build, replace the AdMob App ID in `app/src/main/AndroidManifest.xml`, the Native Ad Unit ID in `app/src/main/java/com/example/ui/components/NativeAdSwipeCard.kt`, and `public/app-ads.txt` with your own AdMob publisher IDs.

### 5. Build & Install Debug APK

```bash
# Using system Gradle (or ./gradlew if a Gradle wrapper is present in your local environment)
gradle :app:assembleDebug
```

The output APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

---

## Running Tests

### Unit & Robolectric Tests

```bash
gradle :app:testDebugUnitTest
```

To run `PhoneAuthManager` state transition tests specifically:

```bash
gradle :app:testDebugUnitTest --tests '*PhoneAuthManagerTest*'
```

### Firestore Security Rules Tests (Firebase Emulator)

1. Start the local Firestore and Auth emulators:

```bash
firebase emulators:start --only firestore,auth --project demo-no-project
```

2. Run the JavaScript security rules suite:

```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```

3. Run the Kotlin/Robolectric Firestore rule tests:

```bash
gradle :app:testDebugUnitTest --tests '*Rule*'
```

4. Deploy rules to your Firebase project:

```bash
firebase deploy --only firestore:rules
```

---

## Project Structure

```text
.
├── app/
│   ├── build.gradle.kts              # Module dependencies, signing configs, Secrets & Google Services plugins
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Permissions & AdMob App ID metadata
│       │   ├── java/com/example/
│       │   │   ├── core/             # App Check setup & Firestore error handler
│       │   │   ├── data/             # Room database, DataStore, models, Firestore & AI repositories
│       │   │   ├── ui/               # Auth (PhoneAuthManager, MobileAuthScreen), screens, components, theme
│       │   │   └── util/             # Permission & approximate location helpers
│       │   └── res/                  # Vector/adaptive icons, fonts, strings, Firebase resource config
│       └── test/                     # Robolectric & Firestore emulator rule tests
├── firestore.rules                   # Hardened Firestore security rules
├── firestore.test.js                 # Node emulator security rules tests
├── firebase-blueprint.json           # Firestore schema blueprint
├── firebase.json                     # Emulator & Firestore rules binding
└── public/app-ads.txt                # IAB app-ads.txt for developer domain root
```

---

## Open-Source Publishing Checklist

Before pushing publicly to GitHub:

- [ ] Verify `.env`, `*.jks`, `debug.keystore`, and any local credential files are excluded in `.gitignore`.
- [ ] Review `app/google-services.json` and `AndroidManifest.xml` to decide whether to keep sample/project IDs or replace them with placeholders for contributors.
- [ ] Add a `LICENSE` file (e.g., [MIT License](https://choosealicense.com/licenses/mit/) or [Apache 2.0](https://choosealicense.com/licenses/apache-2.0/)) matching your preferred open-source terms.
- [ ] Enable GitHub Issue / PR templates and document your contributor guidelines in `CONTRIBUTING.md` if accepting external pull requests.

---

## License

This project is open source. Add your preferred license file (`LICENSE`, e.g. MIT or Apache-2.0) before distribution.
