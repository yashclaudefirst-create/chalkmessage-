# Build Instructions for Claude Code / AI Assistant

Follow these steps IN ORDER. Do not skip ahead.

## Step 1: Create Android Studio Project
- File -> New -> New Project -> Empty Activity (Compose)
- Name: ChalkMessage
- Package: com.example.chalkmessage
- Language: Kotlin
- Minimum SDK: API 26 (Android 8.0)

## Step 2: Replace Gradle Files
Replace the following files with the versions in this zip:
- `build.gradle.kts` (project root)
- `settings.gradle.kts` (project root)
- `gradle.properties` (project root)
- `app/build.gradle.kts`
- `app/proguard-rules.pro`

## Step 3: Firebase Setup (Manual)
1. Go to https://console.firebase.google.com/
2. Create new project
3. Add Android app with package name: `com.example.chalkmessage`
4. Download `google-services.json`
5. Place it in `app/` directory
6. Enable Firestore Database and Cloud Messaging in Firebase Console

## Step 4: Copy Source Files
Copy ALL `.kt` files maintaining the exact package folder structure:
```
app/src/main/java/com/example/chalkmessage/
```

## Step 5: Copy Resource Files
Copy XML files to:
```
app/src/main/res/xml/chalk_widget_info.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
```

## Step 6: Copy AndroidManifest.xml
Replace the auto-generated manifest with the one provided.

## Step 7: Build & Test by Phase
See README.md for the 8-phase build order.
