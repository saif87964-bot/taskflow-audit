# TaskFlow Audit — Production Setup Guide

## 1. Create Firebase Project

1. Go to https://console.firebase.google.com → **Add project** → name it `taskflow-audit`
2. Enable **Google Analytics** → Continue
3. In the project: **Add app** → Android → package: `com.taskflow.audit`
4. Download `google-services.json` → place it at:
   ```
   app/google-services.json
   ```

## 2. Enable Firebase Services

In the Firebase Console:

- **Authentication** → Sign-in method → Enable **Email/Password**
- **Firestore Database** → Create database → **Production mode** (not test mode)
- **App Check** → Register app → Choose **Play Integrity**

## 3. Deploy Firestore Security Rules

In Firebase Console → Firestore → Rules tab, paste the contents of `firestore.rules`.

Or install Firebase CLI and run:
```bash
npm install -g firebase-tools
firebase login
firebase init firestore   # point to your project
firebase deploy --only firestore:rules
```

## 4. Seed Staff Accounts (run once)

```bash
cd scripts
npm install firebase-admin
# Set your service account key:
set GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\serviceAccountKey.json
# Edit PROJECT_ID in seed_firebase.js, then:
node seed_firebase.js
```

This creates Firebase Auth users (email: `{shortId}@taskflow.audit`, default PIN: `1234`)
and writes all staff + engagement documents to Firestore.

## 5. Generate Release Keystore

Run this once and store the keystore securely (NOT in the repo):

```bash
keytool -genkey -v \
  -keystore taskflow-release.jks \
  -alias taskflow \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Then add to `local.properties` (already in .gitignore):
```properties
KEYSTORE_PATH=C:/path/to/taskflow-release.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=taskflow
KEY_PASSWORD=your_key_password
```

## 6. Build Release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

Verify it is signed:
```bash
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

## 7. Security Checklist Before Distribution

- [ ] `google-services.json` is in `.gitignore`
- [ ] `local.properties` (keystore creds) is in `.gitignore`
- [ ] `taskflow-release.jks` is backed up to a secure location (not the repo)
- [ ] Firestore security rules are deployed (not in test mode)
- [ ] App Check is enabled in Firebase Console
- [ ] Default PIN changed for all staff after first login

## 8. Certificate Pinning Update Schedule

The pins in `res/xml/network_security_config.xml` expire 2026-12-31.
Google announces root CA rotations 90+ days ahead. Subscribe to:
https://groups.google.com/g/firebase-announcements
