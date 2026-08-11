# Chalk Message - Android App

A native Android app built with Kotlin and Jetpack Compose. Users send each other hand-drawn, chalk-style messages that appear on the recipient's home screen widget.

## Tech Stack
- **Kotlin** + **Jetpack Compose** (UI)
- **Jetpack Glance** (Home screen widget)
- **Firebase Firestore** (Cloud database)
- **Firebase Cloud Messaging** (Push notifications)
- **Room** (Local SQLite cache)
- **MVVM Architecture**

## Project Structure

```
app/src/main/java/com/example/chalkmessage/
├── data/
│   ├── model/
│   │   └── Models.kt                 # Domain models (DrawPoint, Stroke, ChalkMessage)
│   ├── local/
│   │   ├── AppDatabase.kt            # Room database singleton
│   │   ├── Entity.kt                 # Room entity (MessageEntity)
│   │   ├── MessageDao.kt             # Room DAO (database queries)
│   │   ├── StrokeConverter.kt        # Room type converter (JSON <-> List)
│   │   └── UserPrefs.kt              # DataStore (user settings)
│   ├── remote/
│   │   ├── FirebaseRepository.kt     # Firestore operations
│   │   └── FirestoreModels.kt        # Firestore data models
│   └── ChalkRepository.kt            # Main repository (single source of truth)
├── ui/
│   ├── screen/
│   │   ├── OnboardingScreen.kt       # Name input + invite code + connect
│   │   ├── DrawingScreen.kt          # Full-screen chalk canvas
│   │   └── HistoryScreen.kt          # Message history list
│   ├── theme/
│   │   ├── Color.kt                  # Chalk colors
│   │   ├── Theme.kt                  # MaterialTheme setup
│   │   └── Type.kt                   # Typography
│   └── viewmodel/
│       ├── OnboardingViewModel.kt    # Onboarding state management
│       ├── DrawingViewModel.kt       # Drawing canvas state
│       └── HistoryViewModel.kt       # Message list state
├── widget/
│   ├── ChalkWidget.kt                # Glance widget implementation
│   ├── ChalkWidgetReceiver.kt        # Widget broadcast receiver
│   └── WidgetUpdater.kt              # Helper to refresh widget
├── service/
│   └── ChalkMessagingService.kt      # FCM push notification service
├── MainActivity.kt                   # Entry point + Navigation
└── ChalkMessageApp.kt                # Application class (DI container)
```

## Build Order (Follow strictly!)

### Phase 0: Project Skeleton
1. Create new Android Studio project: **Empty Activity (Compose)**
2. Copy `build.gradle.kts` (project level) and `app/build.gradle.kts`
3. Copy `settings.gradle.kts` and `gradle.properties`
4. Add `google-services.json` to `app/` (download from Firebase Console)
5. Sync Gradle

### Phase 1: Data Layer
1. Copy all files in `data/model/`, `data/local/`, `data/remote/`
2. Build project - should compile

### Phase 2: Repository
1. Copy `data/ChalkRepository.kt`
2. Build project - should compile

### Phase 3: Application Class
1. Copy `ChalkMessageApp.kt`
2. Update `AndroidManifest.xml` with `android:name=".ChalkMessageApp"`
3. Build project - should compile

### Phase 4: ViewModels
1. Copy all files in `ui/viewmodel/`
2. Build project - should compile

### Phase 5: UI Screens
1. Copy theme files (`ui/theme/`)
2. Copy screen files (`ui/screen/`)
3. Copy `MainActivity.kt`
4. Run app - test onboarding, drawing, history navigation

### Phase 6: Firestore Integration
1. Set up Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Enable Firestore and Cloud Messaging
3. Add Firestore security rules (test mode for MVP)
4. Update `ChalkRepository.kt` to write to Firestore
5. Test: send message, verify in Firestore console

### Phase 7: Widget
1. Copy `res/xml/chalk_widget_info.xml`
2. Copy widget files (`widget/`)
3. Update `AndroidManifest.xml` with widget receiver
4. Long-press home screen -> Widgets -> Add ChalkMessage widget

### Phase 8: FCM
1. Copy `service/ChalkMessagingService.kt`
2. Update `AndroidManifest.xml` with FCM service
3. Send test message from Firebase Console
4. Verify notification appears and widget updates

## Firebase Setup Checklist
- [ ] Create Firebase project
- [ ] Add Android app (package: `com.example.chalkmessage`)
- [ ] Download `google-services.json` and place in `app/`
- [ ] Enable Firestore Database
- [ ] Enable Cloud Messaging
- [ ] Set Firestore rules (test mode for development)

## Web -> Android Concept Map

| Web | Android | Notes |
|-----|---------|-------|
| React / JSX | Jetpack Compose | Declarative UI |
| React Router | Navigation Compose | `NavHost` + `composable { }` |
| Redux / Zustand | ViewModel + StateFlow | Survives config changes |
| localStorage | DataStore | Type-safe, async |
| IndexedDB | Room (SQLite) | ORM over SQLite |
| fetch() / axios | Firebase SDK | Direct SDK calls |
| WebSocket | Firestore snapshots | Real-time listeners |
| Service Worker | WorkManager / FCM | Background execution |
| CSS | MaterialTheme + Modifiers | Styling system |
| Canvas API | Compose Canvas | Same coordinate system |
| SVG Path | Compose Path | Same commands |

## Firebase Security Rules (Development / MVP)

Place these security rules in your Firebase Console under Firestore Database -> Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User Profiles: Anyone can register/lookup. Protect with request.auth inside production
    match /users/{userId} {
      allow read, write: if true;
    }

    // Invite Codes: Used for reverse lookups during connection onboarding
    match /inviteCodes/{inviteCode} {
      allow read, write: if true;
    }

    // Connections: Keeps track of connected friends. connectionId = smallerUserId_largerUserId
    match /connections/{connectionId} {
      allow read, write: if true;
    }

    // Messages: Real-time drawing sharing
    match /messages/{messageId} {
      allow read, write: if true;
    }
  }
}
```

## Notes
- Each phase must compile and run before moving to the next
- The drawing canvas uses `detectDragGestures` to capture touch input
- Widgets render strokes to a Bitmap because Glance doesn't support Canvas directly
- Messages are stored as serialized paths (JSON) rather than bitmaps for efficiency
