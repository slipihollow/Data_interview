# Implementation Plan: PB-001 — Data Interview (Android)

## 1. Project Scaffold
- [ ] Create Android project (Kotlin, minSdk 26, targetSdk 35)
- [ ] Configure Gradle dependencies (Room, WorkManager, Kotlin coroutines)
- [ ] Set up AndroidManifest with all permissions and service declarations
- [ ] Create French strings.xml

## 2. Data Layer
- [ ] Define Room database schema (Event entity: type, timestamp, appName, closeTime, widgetLocation)
- [ ] Define Activation entity (id, startTime, endTime, status)
- [ ] DAO for inserting events and querying by activation
- [ ] CSV generator (query events by activation → write CSV with 5-column schema)

## 3. Tracking Service (ForegroundService)
- [ ] DataInterviewService: foreground service with specialUse type
- [ ] Persistent notification (French: "Data Interview - Collecte en cours")
- [ ] Unlock tracking: register BroadcastReceiver for ACTION_USER_PRESENT, SCREEN_ON, SCREEN_OFF
- [ ] App usage tracking: poll UsageStatsManager.queryEvents() every 5s for ACTIVITY_RESUMED/PAUSED
- [ ] Media control tracking: MediaSessionManager.getActiveSessions() + OnActiveSessionsChangedListener
   - Log play/pause/skip state changes with app name + timestamp
   - Column 1: "widget", Column 3: app name, Column 5: "ecran_verrouillage"
- [ ] Write each event to Room database

## 4. Boot Receiver
- [ ] BootReceiver: restart service on RECEIVE_BOOT_COMPLETED if an activation is in progress
- [ ] Store activation state in SharedPreferences for quick check on boot

## 5. Activation System
- [ ] ActivationManager: start/stop tracking service + create Activation record
- [ ] Scheduled activation: AlarmManager exact alarms for scheduled start/stop
- [ ] On activation end: stop service → generate CSV → trigger Telegram upload

## 6. Upload System
- [ ] TelegramUploader: HTTP POST multipart/form-data to api.telegram.org/bot<TOKEN>/sendDocument
- [ ] WhatsApp share: ACTION_SEND intent with CSV file URI + FileProvider
- [ ] Upload status tracking (success/failure) in Activation entity

## 7. UI (French)
- [ ] MainActivity: activation toggle, status display, scheduled activation form
- [ ] HistoryActivity: list of past activations + CSV files
- [ ] CSV viewer: simple table view of CSV contents
- [ ] SettingsActivity: Telegram bot token, chat ID configuration
- [ ] PermissionsActivity: step-by-step grant flow (Usage Access, Notification Listener, Battery)

## 8. Permissions Onboarding
- [ ] Check PACKAGE_USAGE_STATS via AppOpsManager
- [ ] Guide to Settings → Usage Access
- [ ] Check NOTIFICATION_LISTENER_SERVICE
- [ ] Guide to Settings → Notification Access
- [ ] Request POST_NOTIFICATIONS (Android 13+)
- [ ] Request IGNORE_BATTERY_OPTIMIZATIONS

## 9. Distribution & Documentation
- [ ] GitHub repo setup with README (French)
- [ ] Build signed APK via GitHub Actions or local
- [ ] User manual: install APK, grant permissions, configure Telegram, start activation
- [ ] OEM-specific battery optimization instructions (Samsung, Xiaomi, Huawei)
