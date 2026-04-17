# Firebase configuration (Patient app)

Firebase is configured for **Android** in this project. It is used for:

- **Firebase Auth** – Phone number OTP (sign-in and verification during registration)
- **Firebase Cloud Messaging** – Push notifications

## Current setup

- **Project:** `shifa-doctor-staging` (shared with doctor app)
- **Android app:** `com.shifa.patientapp`
- **Config files:**
  - `android/app/google-services.json` – Android config from Firebase Console
  - `lib/firebase_options.dart` – Flutter options (can be regenerated with FlutterFire CLI)

Android Gradle already applies the Google Services plugin (`com.google.gms.google-services`) in `android/app/build.gradle.kts` and `android/settings.gradle.kts`.

## Checklist vs Firebase doc (Phone Auth on Android)

Compared with [Firebase: Authenticate with Firebase on Android using a Phone Number](https://firebase.google.com/docs/auth/android/phone-auth):

| Doc requirement | Status in this project |
|-----------------|-------------------------|
| **Before you begin** | |
| Add Firebase Auth dependency (Gradle) | ✓ Via Flutter `firebase_auth` package; native deps applied by the plugin. |
| Set app SHA-1 in Firebase Console | ✓ Add in Project settings → Your apps → **com.shifa.patientapp** (debug + release). |
| Connect app to Firebase project | ✓ `android/app/google-services.json` present; package `com.shifa.patientapp`, project `shifa-doctor-staging`. |
| Add Firebase to Android project | ✓ Plugin `com.google.gms.google-services` in `settings.gradle.kts` and `app/build.gradle.kts`. |
| **Enable Phone sign-in** | |
| Enable Phone sign-in method | ✓ Do in Firebase Console → Authentication → Sign-in method → Phone → Enable. |
| (Optional) Set SMS region policy | In Firebase Console → Authentication → Settings if needed. |
| **App verification** | |
| SHA-256 for Play Integrity | ✓ Add in Project settings → Your apps → fingerprints (with SHA-1). |
| SHA-1 for reCAPTCHA fallback | ✓ Same as above. |
| API key unrestricted or allowlisted for `PROJECT_ID.firebaseapp.com` | ✓ See troubleshooting “API Key restrictions”; add `https://shifa-doctor-staging.firebaseapp.com/*`. |
| **Implementation** | |
| Call `verifyPhoneNumber` with number, timeout, callbacks | ✓ `lib/features/auth/data/phone_auth_repository.dart`: `verifyPhoneNumber` → `_auth.verifyPhoneNumber(...)`. |
| Handle `onVerificationCompleted`, `onVerificationFailed`, `onCodeSent` | ✓ Mapped to `verificationCompleted`, `verificationFailed`, `codeSent`. |
| Create credential with `PhoneAuthProvider.credential(verificationId, code)` | ✓ In `signInWithPhoneCredential`. |
| Sign in with `signInWithCredential(credential)` | ✓ Same method. |
| **Testing (avoids quota / “blocked” errors)** | |
| Use test phone numbers (fictional) | Optional: Firebase Console → Authentication → Sign-in method → Phone → **Phone numbers for testing** (add up to 10 numbers + 6-digit codes; no real SMS, no throttling). |

**Note:** The doc says that if no Activity is passed to `verifyPhoneNumber`, reCAPTCHA cannot be used. On Flutter, the `firebase_auth` plugin passes the Android activity internally when running on a device/emulator, so no extra code is required.

## Required steps in Firebase Console

### 1. Enable Phone Authentication

1. Open [Firebase Console](https://console.firebase.google.com/) and select project **shifa-doctor-staging**.
2. Go to **Authentication** → **Sign-in method**.
3. Open **Phone** and turn **Enable** on.
4. Save.

Without this, phone OTP (SMS) sign-in will not work.

### 2. (Recommended) Add test phone numbers to avoid throttling / “blocked” errors

To test without real SMS and without triggering “unusual activity” blocks:

1. In Firebase Console: **Authentication** → **Sign-in method** → **Phone**.
2. Open **Phone numbers for testing**.
3. Add up to 10 test numbers (e.g. `+1 650-555-3434`) and a 6-digit verification code (e.g. `123456`) for each.
4. When you use that number in the app, no SMS is sent; use the code you set. This does not consume quota and is not throttled.

### 3. Add Android SHA certificates (required for Phone Auth / Play Integrity)

**If you see:** *"This app is not authorized to use Firebase Authentication"* or *"play_integrity_token was passed, but no matching SHA-256 was registered"* — add the fingerprints below in Firebase Console.

1. In Firebase Console: **Project settings** (gear) → **Your apps**.
2. Select the Android app with package name **com.shifa.patientapp**.
3. Click **Add fingerprint** and add **both SHA-1 and SHA-256** for the build you use:

**Debug** (e.g. `flutter run`; from `./gradlew signingReport` on this machine):

| Type   | Fingerprint |
|--------|-------------|
| SHA-1  | `1C:95:57:3E:3B:2B:68:F5:3B:DC:43:E9:1A:03:14:86:97:A9:EA:8A` |
| SHA-256| `28:A6:60:1E:4B:9B:A1:FE:AF:9A:12:33:8C:C0:B6:94:5E:F1:DD:A1:A6:46:7F:DA:F5:03:AE:C1:DA:1A:5A:4E` |

**Release** (from this machine’s release keystore):

| Type   | Fingerprint |
|--------|-------------|
| SHA-1  | `00:1A:83:26:E0:36:9A:00:24:56:98:7F:93:FE:87:6C:18:8D:C9:C9` |
| SHA-256| `D8:0C:C3:F5:DD:25:29:1E:55:0A:C7:05:37:02:4D:14:01:CC:B6:BD:3A:16:A5:F5:A3:6E:45:C8:F7:11:2C:41` |

4. Wait a few minutes, then uninstall the app from the device, run `flutter clean && flutter pub get`, and run again.

To regenerate fingerprints on another machine: `cd android` then `./gradlew signingReport` (Windows: `gradlew.bat signingReport`); use the **:app** variant (debug or release).

### 4. (Optional) Use a separate Firebase project for the patient app

If you create a dedicated Firebase project for the patient app:

1. Install FlutterFire CLI:  
   `dart pub global activate flutterfire_cli`
2. In the project root:  
   `dart run flutterfire configure`
3. Sign in to Google, select the **patient** Firebase project, and choose the platforms (e.g. Android, and optionally iOS/Web).
4. This will update `lib/firebase_options.dart` and `android/app/google-services.json` (and add iOS/Web config if selected).

## Adding iOS or Web

1. In Firebase Console, add an **iOS** and/or **Web** app to the same project (or the patient project).
2. Download **GoogleService-Info.plist** (iOS) and/or copy the web config.
3. In the project root run:  
   `dart run flutterfire configure`  
   and select iOS and/or Web. FlutterFire will generate the corresponding options in `lib/firebase_options.dart` and tell you where to place the iOS/Web config files.

## Troubleshooting

- **“DefaultFirebaseOptions have not been configured for …”**  
  You’re running on a platform that doesn’t have options yet (e.g. iOS or Web). Run `dart run flutterfire configure` and select that platform, or add the platform in Firebase Console and run the command again.

- **"This request is missing a valid app identifier" (Play Integrity / reCAPTCHA)**  
  Shown when completing registration (Phone Auth) if your app's **SHA-1** is not in Firebase. Add it: Project settings → Your apps → Android **com.shifa.patientapp** → Add fingerprint (SHA-1 and optionally SHA-256). Use debug keystore fingerprints from:  
  `keytool -keystore %USERPROFILE%\.android\debug.keystore -list -v -storepass android -keypass android`  
  Wait a few minutes after adding, then retry.

  **If the error persists after adding SHA-1 and SHA-256:**

  1. **Re-download `google-services.json`**  
     Firebase Console → Project settings → Your apps → **com.shifa.patientapp** → download **google-services.json** and replace `android/app/google-services.json` in the project.

  2. **Enable APIs in Google Cloud Console** (same project as Firebase: **shifa-doctor-staging**):  
     [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services** → **Library** → search and enable:
     - **Google Play Integrity API**
     - (Optional) **Identity Toolkit API** if not already enabled for Firebase Auth)

  3. **Use a real device**  
     Play Integrity often fails on emulators. Test on a physical Android device with Google Play Services.

  4. **Clean rebuild**  
     Uninstall the app from the device, then in the project run:  
     `flutter clean && flutter pub get`  
     then build and run again.

  5. **API Key restrictions (reCAPTCHA)**  
     If reCAPTCHA is used (e.g. phone auth fallback), the API key must allow the Firebase domain. In [Google Cloud Console](https://console.cloud.google.com/) → project **shifa-doctor-staging** → **APIs & Services** → **Credentials**: find the API key used by Firebase (or the **Browser key** / key referenced in your app). Either set **Application restrictions** to **None**, or under **API restrictions** ensure the key is unrestricted or allow **Identity Toolkit API** (Firebase Auth). For HTTP referrer restrictions, if any, add:  
     `https://shifa-doctor-staging.firebaseapp.com/*`  
     and  
     `https://shifa-doctor-staging.web.app/*`  
     so reCAPTCHA requests from the Firebase project domain are allowed.

- **"We have blocked all requests from this device due to unusual activity. Try again later."**  
  This is **Google/Firebase abuse prevention**, not an app bug. It can happen after many verification attempts, using a VPN, or from a flagged IP/device. **What to do:** wait 24–48 hours, then try again; use a different network (e.g. mobile data instead of Wi‑Fi) or disable VPN; avoid rapid repeated attempts. No code change required.

  **Same error on another device?** The block may be on **IP/network** (e.g. same Wi‑Fi or carrier) or **phone number** (same number used for testing). Try: (1) a **different network** (e.g. mobile data on a different SIM, or another Wi‑Fi); (2) a **different phone number** for OTP; (3) wait 24–48 hours. If it still happens everywhere, check [Firebase Console](https://console.firebase.google.com/) → **Authentication** → **Usage** for any alerts, and [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services** → **Identity Toolkit API** → **Quotas** for limits or abuse flags.

- **"Phone verification not configured" (after entering SMS code)**  
  This message comes from the **backend**, not Firebase. The backend must verify the Firebase ID token to complete registration; it needs Firebase Admin SDK credentials. Configure the **shifa-doctor-backend** with either: (1) **GOOGLE_APPLICATION_CREDENTIALS** = path to your Firebase service account JSON file (local), or (2) **FIREBASE_SERVICE_ACCOUNT_JSON** = entire contents of the service account JSON (e.g. on Railway). Get the JSON from Firebase Console → Project settings → **Service accounts** → **Generate new private key**. See backend repo `ENVIRONMENT_VARIABLES_SETUP.md` for details.

- **Phone Auth / SMS not working**  
  Confirm Phone is enabled in Authentication → Sign-in method and that the Android app has the correct package name and SHA certificates in Project settings.

- **Build error about Google Services**  
  Ensure `android/app/google-services.json` exists and that `id("com.google.gms.google-services")` is applied in `android/app/build.gradle.kts`.
