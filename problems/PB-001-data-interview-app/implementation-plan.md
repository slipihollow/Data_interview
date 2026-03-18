# Implementation Plan: PB-001 — Data Interview (Android)

## 1. Project Scaffold
- [x] Create Android project (Kotlin, minSdk 21, targetSdk 35) — lowered from 26 for max backward compat
- [x] Configure Gradle dependencies (Room, WorkManager, Kotlin coroutines, OkHttp)
- [x] Set up AndroidManifest with all permissions and service declarations
- [x] Create French strings.xml

## 2. Data Layer
- [x] Define Room database schema (Event entity: type, timestamp, appName, closeTime, widgetLocation)
- [x] Define Activation entity (id, startTime, endTime, status, csvFilePath, uploadStatus, eventCount)
- [x] DAO for inserting events and querying by activation
- [x] CSV generator (query events by activation → write CSV with 5-column schema, semicolon delimiter)
- [x] Unit tests for CSV generator (5 tests)

## 3. Tracking Service (ForegroundService)
- [x] DataInterviewService: foreground service with specialUse type (API 34+ gated)
- [x] Persistent notification (French: "Data Interview – Collecte en cours")
- [x] Unlock tracking: register BroadcastReceiver for ACTION_USER_PRESENT, SCREEN_ON, SCREEN_OFF
- [x] App usage tracking: poll UsageStatsManager.queryEvents() every 5s
  - MOVE_TO_FOREGROUND/BACKGROUND for API 21-28, ACTIVITY_RESUMED/PAUSED for API 29+
- [x] Media control tracking: MediaSessionManager.getActiveSessions() + OnActiveSessionsChangedListener
   - Log play/pause/skip state changes with app name + timestamp
   - Column 1: "widget", Column 3: app name (action), Column 5: "ecran_verrouillage"
- [x] MediaNotificationListenerService for MediaSessionManager requirement
- [x] Write each event to Room database

## 4. Boot Receiver
- [x] BootReceiver: restart service on RECEIVE_BOOT_COMPLETED if an activation is in progress
- [x] Store activation state in SharedPreferences for quick check on boot

## 5. Activation System
- [x] ActivationManager: start/stop tracking service + create Activation record
- [x] Scheduled activation: AlarmManager exact alarms for scheduled start/stop
  - setExactAndAllowWhileIdle on API 23+, setExact on API 21-22
- [x] AlarmReceiver for scheduled start/stop broadcasts
- [x] On activation end: stop service → generate CSV → trigger Telegram upload

## 6. Upload System
- [x] TelegramUploader: HTTP POST multipart/form-data to api.telegram.org/bot<TOKEN>/sendDocument
- [x] Upload status tracking (success/failure) in Activation entity
- ~~WhatsApp share~~ — removed per user decision (Telegram only)

## 7. UI (French)
- [x] MainActivity: activation toggle, status display, scheduled activation form (date/time pickers)
- [x] HistoryActivity: list of past activations with RecyclerView + CardView items
- [x] CsvViewerActivity: simple table view of CSV contents with share option
- [x] SettingsActivity: Telegram bot token, chat ID configuration (Material TextInputLayout)
- [x] PermissionsActivity: step-by-step grant flow with RecyclerView

## 8. Permissions Onboarding
- [x] Check PACKAGE_USAGE_STATS via AppOpsManager (unsafeCheckOpNoThrow on API 29+, checkOpNoThrow on older)
- [x] Guide to Settings → Usage Access
- [x] Check NOTIFICATION_LISTENER_SERVICE via enabled_notification_listeners
- [x] Guide to Settings → Notification Access
- [x] Request POST_NOTIFICATIONS (Android 13+ only, gated)
- [x] Request IGNORE_BATTERY_OPTIMIZATIONS (Android 6+ only, gated)

## 9. Distribution & Documentation
- [ ] GitHub repo setup with README (French)
- [ ] Build signed APK via GitHub Actions or local
- [ ] User manual: install APK, grant permissions, configure Telegram, start activation
- [ ] OEM-specific battery optimization instructions (Samsung, Xiaomi, Huawei)

## Backward Compatibility Notes (drift from original plan)

Original plan specified minSdk 26. Changed to **minSdk 21** (Android 5.0 Lollipop) per user request.
Key version-gated features:
- PendingIntent.FLAG_IMMUTABLE: API 23+ (FLAG_UPDATE_CURRENT fallback)
- Notification channels: API 26+ (auto-handled by NotificationCompat)
- Foreground service type specialUse: API 34+ (tools:targetApi annotation)
- POST_NOTIFICATIONS permission: API 33+
- SCHEDULE_EXACT_ALARM: API 31+
- setExactAndAllowWhileIdle: API 23+ (setExact fallback)
- Battery optimization bypass: API 23+
- UsageEvents event types: MOVE_TO_FOREGROUND/BACKGROUND on API 21-28, ACTIVITY_RESUMED/PAUSED on API 29+
- AppOpsManager.unsafeCheckOpNoThrow: API 29+ (checkOpNoThrow fallback)
