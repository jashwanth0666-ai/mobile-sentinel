# Mobile Sentinel

Mobile Sentinel is a personal Android anti-theft companion. It lets you enroll one original phone as the protected device, then sign in on a spare phone in controller mode to view the protected phone's last reported location and queue supported security actions.

**Developer:** [B Jashwanth Sai](https://github.com/jashwanth0666-ai)  
**Website:** [jashwanthonline.netlify.app](https://jashwanthonline.netlify.app/)  
**Instagram:** [@jashwanth1_17](https://www.instagram.com/jashwanth1_17)  
**LinkedIn:** [Jashwanth Sai](https://www.linkedin.com/in/jashwanth-sai-57601a300)  
**Current release:** 1.0.1 (version code 3)  
**Android package:** `com.mobilesentinel`

## Release APK

The installable signed APK is `release/MobileSentinel-v1.0.1.apk`. Install it on Android 8.0 or newer. Android may ask you to allow installation from the app used to open the APK.

The release signing key and its local Gradle properties are stored outside version control. Keep a secure backup of the keystore at `%USERPROFILE%\.android\MobileSentinel-release.jks` and the ignored `.release-signing.properties` file. Future updates for existing installs must be signed with the same key. If the key is lost, Android will not accept an update over the existing installation. Do not publish or share the signing key or properties file.

To rebuild the signed APK from this project, with Android SDK, Java, and `app/google-services.json` configured, run:

```powershell
.\gradlew.bat assembleRelease
Copy-Item app\build\outputs\apk\release\app-release.apk release\MobileSentinel-v1.0.1.apk
```

## Features

- Firebase email/password sign-in on the protected phone and controller phones.
- One protected-device enrollment per account. Other phones remain controllers and are not enrolled or tracked by this app.
- A device status view with battery, remote-lock setup, and location status on the protected phone.
- An owner-scoped Firestore command queue for remote lock and alarm commands.
- An armed foreground service that listens for commands and periodically reports the protected phone's location. Lost mode requests more frequent updates.
- App logo, pastel wallpaper, glass-style surfaces, and an About screen describing the app and developer.

## First-time setup

1. Create a Firebase project, add an Android app with package `com.mobilesentinel`, and put its `google-services.json` in `app/`.
2. Enable Email/Password authentication and create a Firestore database.
3. Deploy the included Firestore rules using Firebase CLI: `firebase deploy --only firestore:rules`.
4. Install the app and create/sign in to the owner account on the original phone. Enroll that phone once.
5. Enable Android device-admin access for remote locking. Grant location and notification permissions, then arm remote protection while the phone is with you.
6. Install the APK on a spare phone and sign in with the same account. It operates as a controller; do not enroll it.

## Important operating limits

- Remote actions require the protected phone to be powered on, connected to the internet, and running its armed foreground service. This Spark-compatible design does not remotely wake an unarmed or force-stopped app. Android battery management or a reboot can interrupt the service.
- Remote locking requires the user to enable this app as a device administrator and set a screen lock on the protected phone. Android permissions remain under the device owner's control.
- Location is periodically reported and may be stale or unavailable. This app does not guarantee device recovery and does not yet provide an embedded map or location history.
- A controller phone can send supported commands only to the account's single enrolled protected phone.

## Build configuration

The Android client uses Kotlin, Jetpack Compose, Firebase Authentication, and Cloud Firestore. No Firebase service-account credentials are included. `google-services.json`, local SDK configuration, and release signing files are intentionally excluded from version control.
