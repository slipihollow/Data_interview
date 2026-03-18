# High-Level Plan: PB-001 — Data Interview App

## Phase 1: Android App (immediate)

1. Project setup — Kotlin, Android Studio, Gradle, min SDK 26
2. Core tracking service
   - Foreground service (specialUse) with persistent notification
   - Unlock detection (BroadcastReceiver: ACTION_USER_PRESENT, SCREEN_ON/OFF)
   - App usage tracking (UsageStatsManager: ACTIVITY_RESUMED/PAUSED)
   - Media control tracking (MediaSessionManager: playback state changes)
   - Boot receiver (RECEIVE_BOOT_COMPLETED) for reboot survival
3. Activation system
   - Manual start/stop toggle
   - Scheduled activation (start date+time, end date+time)
   - AlarmManager for scheduled start/stop
4. CSV generation
   - 1 CSV per activation period
   - Schema: type | time | app_or_widget_name | close_time | widget_location
   - Local storage (app-internal)
5. Upload system
   - Telegram Bot API: automated POST on activation end
   - WhatsApp: share sheet button (manual fallback)
6. UI (French)
   - Activation toggle + scheduler
   - CSV file list (view, share, delete)
   - Settings (Telegram bot config)
   - Permissions onboarding flow
7. Permissions & battery optimization
   - PACKAGE_USAGE_STATS grant flow
   - NOTIFICATION_LISTENER grant flow
   - Battery optimization whitelist prompt
   - OEM-specific guidance
8. Distribution
   - GitHub repo with APK releases
   - User manual (French) with install + usage instructions

## Phase 2: iOS SensorKit App (parallel, Apple-dependent)

1. Submit SensorKit proposal to sensorkitrequest@apple.com
2. Prepare IRB documentation referencing SensorKit sensors
3. Build Swift app once entitlement received
4. TestFlight distribution to participants

## Phase 3: Polish

1. English localization
2. User manual updates
3. Edge case hardening (OEM battery killers)
