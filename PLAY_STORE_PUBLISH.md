# Publish Shifa Patient App to Google Play Console

## Build outputs (already created)

| File | Location | Use |
|------|----------|-----|
| **App Bundle (.aab)** | `build\app\outputs\bundle\release\app-release.aab` | **Upload this to Play Console** (recommended) |
| **APK (.apk)** | `build\app\outputs\flutter-apk\app-release.apk` | For direct install or testing; Play accepts it but prefers AAB |

---

## Rebuilding later

From the project root (`c:\shifa_patient_app_v1`):

```powershell
# App Bundle (for Play Store) – preferred
flutter build appbundle --release

# APK (for direct install / testing)
flutter build apk --release
```

Before each release, update version in `pubspec.yaml` (e.g. `version: 1.0.0+1` → `1.0.1+2`).  
`versionCode` (the number after `+`) must increase for every upload to Play Console.

---

## Steps to publish on Google Play Console

### 1. Sign in and create the app (first time only)

1. Go to [Google Play Console](https://play.google.com/console).
2. Accept the **Developer Distribution Agreement** if prompted.
3. Click **Create app**.
4. Fill in:
   - **App name:** Shifa Patient
   - **Default language:** Your primary language (e.g. English).
   - **App or game:** App
   - **Free or paid:** Free (or Paid if you charge).
5. Declare if the app uses **ads** (yes/no).
6. Click **Create app**.

---

### 2. Complete the “Set up your app” checklist

In the left menu, open **Policy and programs** and complete any required agreements (e.g. **Developer Program Policies**, **US export laws**).

---

### 3. Set up the app’s store listing

1. In the left menu: **Grow** → **Store presence** → **Main store listing**.
2. Fill in:
   - **Short description** (up to 80 characters).
   - **Full description** (up to 4000 characters).
   - **App icon:** 512×512 px PNG (no transparency).
   - **Feature graphic:** 1024×500 px (optional but recommended).
   - **Screenshots:** At least 2 phone screenshots (e.g. 1080×1920 or 9:16). Add 7″ tablet if you support tablets.
3. **Save** the main store listing.

---

### 4. Content rating

1. **Policy** → **App content** → **Content rating**.
2. Click **Start questionnaire**.
3. Choose **Category** (e.g. “Health & Fitness” or “Medical”).
4. Answer the questionnaire.
5. **Submit** and apply the rating to your app.

---

### 5. Target audience and content (age groups)

1. **Policy** → **App content** → **Target audience and content**.
2. Set **Target age groups** (e.g. 18+ if it’s medical/health).
3. Complete any other required declarations (e.g. **COVID-19 apps**, **Data safety**).

---

### 6. Data safety form

1. **Policy** → **App content** → **Data safety**.
2. Declare what data you collect (e.g. email, phone, health-related data, location).
3. State whether data is shared with third parties and how it’s used (e.g. account management, app functionality).
4. **Save** and submit.

---

### 7. Create a release and upload the app

1. In the left menu: **Release** → **Production** (or **Testing** → **Internal testing** for a first test).
2. Click **Create new release**.
3. **Upload** the App Bundle:
   - Click **Upload** and select:
     - `c:\shifa_patient_app_v1\build\app\outputs\bundle\release\app-release.aab`
   - Or drag and drop the file.
4. Add **Release name** (e.g. “1.0.0 (1)”) and optional **Release notes** (what’s new for users).
5. Click **Save** (then **Review release** if available).

---

### 8. Complete “App content” and other required items

In **Policy** → **App content**, finish any items still marked as required, for example:

- **Privacy policy** (URL to your privacy policy page).
- **Ads** (if your app shows ads): declare ad provider and behavior.
- **News apps** (if applicable).
- **COVID-19 contact tracing / status** (if applicable).
- **Financial features** (if you handle payments outside of Play billing).
- **Government apps** (if applicable).

---

### 9. Submit for review

1. Go to **Release** → **Production** (or the testing track you used).
2. Confirm the release shows the correct AAB and version.
3. Click **Send for review** (or **Start rollout to Production**).
4. Google will review the app (often 1–7 days). You’ll get an email when it’s approved or if changes are needed.

---

## Summary checklist

- [ ] Developer account and app created in Play Console  
- [ ] Store listing (short/full description, icon, screenshots)  
- [ ] Content rating questionnaire completed  
- [ ] Target audience and content set  
- [ ] Data safety form filled  
- [ ] Privacy policy URL added  
- [ ] App Bundle (`app-release.aab`) uploaded in a release  
- [ ] All required “App content” items completed  
- [ ] Release submitted for review  

---

## Keystore and signing (already set up)

Your release build is signed using `android/key.properties` and the keystore it points to. **Keep these safe and backed up.** If you lose the keystore, you cannot update the same app on Play Store with a new key.

- **Do not** commit `key.properties` or the `.jks`/`.keystore` file to public Git.  
- For Play, you can later opt in to **Google Play App Signing** and use an upload key; the Play Console guide will walk you through it when you create the first release.

---

## Useful links

- [Play Console Help](https://support.google.com/googleplay/android-developer)  
- [Release your app (official guide)](https://support.google.com/googleplay/android-developer/answer/9859152)  
- [App signing by Google Play](https://support.google.com/googleplay/android-developer/answer/9842756)
