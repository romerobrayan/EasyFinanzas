# FIREBASE_SETUP.md — connecting Tinto to `tintoapp-romerodev`

The code side of the Firebase integration is **done**: Google sign-in, per-user
Cloud Firestore persistence (with offline cache), Analytics and Crashlytics are
all wired in. What the repo ships with is a **placeholder**
`app/google-services.json`, so the app builds and runs in demo mode but cannot
talk to your real Firebase project yet.

Follow these one-time steps in the [Firebase console](https://console.firebase.google.com/u/0/project/tintoapp-romerodev/overview)
(~10 minutes, everything below is on the free Spark plan).

## 1. Register the Android app

1. Console → **Project settings** (gear icon) → **Your apps** → **Add app** → Android.
2. Package name — must be exactly:

   ```
   dev.romerobrayan.tinto
   ```

3. Nickname: `Tinto` (anything works).
4. **SHA-1 debug signing certificate** — the repo pins a shared debug keystore
   (`app/debug.keystore`, used by local builds *and* CI, so this fingerprint
   never changes):

   ```
   B0:E3:A1:F4:C0:35:5F:44:A3:4D:02:3A:6A:03:18:EB:91:B3:69:B7
   ```

   Also add the SHA-256 (Project settings → your Android app → *Add fingerprint*):

   ```
   0E:8A:8C:3B:D7:C1:C9:DB:6E:85:E0:A4:9C:AC:96:9B:79:50:E0:62:19:70:65:A3:B6:66:A0:09:3D:C3:4E:78
   ```

5. You can skip downloading `google-services.json` for now — we download it in
   step 5, **after** enabling Google sign-in (the file only includes the OAuth
   client once the provider exists).

## 2. Enable Google sign-in

1. Console → **Authentication** → **Get started** → **Sign-in method**.
2. Enable **Google**, pick your support email, save.

## 3. Create the Firestore database

1. Console → **Firestore Database** → **Create database**.
2. Mode: **production** (rules below take over).
3. Region: pick once, it is permanent — `southamerica-east1` (São Paulo) is the
   closest to Colombia; `us-east1` is also fine.

## 4. Publish the security rules

Console → **Firestore Database** → **Rules**, replace the contents with the
repo's [`firestore.rules`](firestore.rules) and **Publish**:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

Each user can only read and write their own `users/{uid}/...` subtree; every
other path is denied.

## 5. Download the real `google-services.json`

1. Console → **Project settings** → **Your apps** → the Android app →
   **google-services.json** (download).
2. Replace the placeholder at **`app/google-services.json`** with it.
3. Commit and push — this file is safe to commit for this app: the API key in
   it is a client identifier, not a secret; access control comes from the
   Firestore rules and the registered SHA-1.

> Re-download the file any time you add a provider or fingerprint — stale
> copies are the #1 cause of sign-in failures.

## 6. Enable Crashlytics

Console → **Crashlytics** → **Enable**. The dashboard shows data after the
first session/crash arrives from a device.

Analytics needs no extra step (the project was created with Google Analytics
enabled; if the console prompts you to link it, accept).

## 7. Verify end to end

1. Push to `main` or any `claude/**` branch → CI builds and publishes the APK
   to the `ci-apk` branch; install `tinto-debug.apk` on your phone.
   (Because of the new pinned keystore, uninstall any previously installed
   Tinto build once — the old signature can't be updated in place.)
2. Open the app → **Continuar con Google** → pick your account.
3. Add a movement, then check console → **Firestore Database** → `users/{your-uid}/transactions` — the document appears.
4. Airplane-mode test: add a movement offline, it shows instantly in the app
   and syncs to Firestore when connectivity returns.
5. Analytics: events (`login`, `screen_view`, `add_transaction`) appear in
   **Realtime** within minutes and in reports within ~24 h. For instant streams
   use DebugView: `adb shell setprop debug.firebase.analytics.app dev.romerobrayan.tinto`.

## Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| Sign-in dialog closes with a generic error, logcat shows `DEVELOPER_ERROR` or code `10` | The SHA-1 above isn't registered on the Android app, or `google-services.json` is stale — re-check step 1.4 and re-download (step 5). |
| "Esta compilación aún no tiene Firebase configurado" | The build still contains the placeholder `google-services.json`. |
| Sign-in works but reads/writes fail with `PERMISSION_DENIED` | Rules not published (step 4), or the database wasn't created (step 3). |
| No accounts offered on the device | Add a Google account in Android settings; the device needs Play Services. |

## What's on the free (Spark) plan

More than enough for personal use:

- **Authentication**: 50k monthly active users.
- **Cloud Firestore**: 1 GiB storage, 50k reads / 20k writes / 20k deletes per day.
- **Analytics & Crashlytics**: free, unlimited.

No billing account is needed for any feature this app uses.
