# Investigation: PB-001 — Data Interview App

## Context

This investigation covers three axes: (1) what the target platforms actually allow, (2) which
development framework to use, and (3) how to handle serverless CSV upload. The findings
reveal a **fundamental platform asymmetry** between Android and iOS that shapes every option.

---

## Axis 1: Platform Feasibility

### Android — Fully Feasible

Android provides all necessary APIs:

| Capability | API | Notes |
|---|---|---|
| App open/close | `UsageStatsManager.queryEvents()` → `ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` | Requires `PACKAGE_USAGE_STATS` (user grants in Settings) |
| Phone unlock | `KEYGUARD_HIDDEN` event (type 18) + `ACTION_USER_PRESENT` broadcast | Works reliably |
| Screen on/off | `SCREEN_INTERACTIVE` / `SCREEN_NON_INTERACTIVE` events | Real-time via BroadcastReceiver |
| Background service | `ForegroundService` with `specialUse` type | Runs indefinitely, no timeout |
| Survive reboot | `RECEIVE_BOOT_COMPLETED` receiver | Restarts service after unlock |
| Widget interactions | **NOT AVAILABLE** | No API to detect taps on third-party widgets |

**Key limitation:** Widget interaction tracking is **not possible** on Android for third-party widgets.
`UsageStatsManager` does not report widget taps. The only option is tracking interactions with
your *own* widgets via `AppWidgetProvider`, which is not what the spec requires.

**OEM battery optimization** is the main operational risk. Huawei, Xiaomi, Samsung, OnePlus all
aggressively kill background apps. Mitigation: guide users to whitelist the app, request
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, use persistent foreground notification.

### iOS — Severely Limited (Near-Infeasible for Core Requirements)

| Capability | Status | Details |
|---|---|---|
| App open/close tracking | **NOT POSSIBLE** | No public API for third-party apps to monitor which apps a user opens. Screen Time API (`DeviceActivityMonitor`) uses opaque tokens, cannot export data, and is designed for parental controls only. |
| Phone unlock detection | **NOT POSSIBLE** | No reliable public API. `protectedDataDidBecomeAvailable` is an unreliable proxy. |
| Background service | **NOT POSSIBLE** | iOS suspends apps after ~30s in background. No equivalent to Android's foreground service. `BGAppRefreshTask` gives ~30s at OS-determined intervals. |
| Survive reboot | **NOT POSSIBLE** | iOS does not allow third-party apps to auto-launch after reboot. |
| Widget interactions | **NOT POSSIBLE** | Same as Android — no API for third-party widget monitoring. |

**Bottom line:** iOS cannot fulfill any of the core requirements (unlock tracking, app usage
monitoring, persistent background execution). This is by Apple's design for privacy.

**What academic researchers do instead on iOS:**
- Participants manually screenshot their Screen Time settings page
- ESM/EMA self-report prompts
- MDM-managed devices (does NOT provide usage analytics, only installed app lists)

---

## Axis 2: Development Framework Options

### Option A: Native Android (Kotlin) — Android Only

**Description**: Build a native Android app in Kotlin using Android Studio. Direct access to all
system APIs. Single-platform focus. Distribute via GitHub as an APK + build instructions.

**Trade-offs**:
- ✅ Direct access to `UsageStatsManager`, `ForegroundService`, `BroadcastReceiver`
- ✅ Zero abstraction overhead — no plugins, no bridge, no framework bugs
- ✅ Largest body of documentation, tutorials, and StackOverflow answers
- ✅ Simplest architecture for a research team
- ✅ Aligned with the bmp-pathways-app reference (also native Android/Java)
- ❌ No iOS version (but iOS can't do this anyway)
- ❌ Requires Kotlin knowledge (easy to learn for most developers)

**Risks**: OEM battery optimization killing the service. Mitigable with user guidance.
**Effort**: Medium

### Option B: Flutter — Cross-Platform

**Description**: Build with Flutter (Dart). Use `flutter_background_service` for Android foreground
service, `app_usage` plugin for UsageStats. iOS side would be a limited "companion" UI.

**Trade-offs**:
- ✅ Single codebase for UI on both platforms
- ✅ Hot reload speeds up UI development
- ❌ Background service plugins are wrappers with known bugs (reboot issues on Flutter)
- ❌ `app_usage` plugin is Android-only, stats only precise to daily level
- ❌ Unlock detection requires custom platform channel (writing native Kotlin anyway)
- ❌ iOS version would be a shell — cannot track anything meaningful
- ❌ Plugin ecosystem for system-level tracking is thin and community-maintained
- ❌ You end up writing native code via platform channels, negating cross-platform benefit

**Risks**: Plugin compatibility issues on framework updates. Known Flutter foreground service
bugs after reboot. Thin community support for this specific use case.
**Effort**: Medium-High (plugin debugging + platform channels for features not covered by plugins)

### Option C: React Native — Cross-Platform

**Description**: Build with React Native (TypeScript/JavaScript). Use Headless JS + native
modules for background execution. Sparse plugin ecosystem for usage stats.

**Trade-offs**:
- ✅ JavaScript is widely known
- ✅ Existing (but sparse) plugins for usage stats and device activity
- ❌ ~150ms bridge latency per native call
- ❌ Native module ecosystem for usage tracking is poorly maintained
- ❌ Frequent RN version upgrades break native module compatibility
- ❌ Same iOS limitations as Flutter
- ❌ Higher maintenance burden than Flutter or native

**Risks**: Bridge overhead for frequent event logging. Plugin abandonment.
**Effort**: High

### Option D: Kotlin Multiplatform (KMP) — Cross-Platform

**Description**: Share business logic (data models, CSV generation, storage, upload) in Kotlin
`commonMain`. Platform-specific code in `androidMain` (UsageStats, ForegroundService) and
`iosMain` (limited Screen Time API). UI via Compose Multiplatform or native.

**Trade-offs**:
- ✅ Shared logic in one language (Kotlin)
- ✅ Android-specific code has direct API access (no plugins, no bridge)
- ✅ `expect`/`actual` pattern cleanly separates platform capabilities
- ✅ Production-ready (Netflix, Philips, VMware use KMP in production)
- ✅ Best option IF iOS support is genuinely needed later
- ❌ Higher learning curve (KMP concepts, Compose Multiplatform)
- ❌ iOS debugging in Kotlin/Native is less polished than native Swift
- ❌ iOS version would still be severely limited by Apple's restrictions
- ❌ More complex project setup than pure native Android

**Risks**: KMP iOS tooling is still maturing. Compose Multiplatform on iOS is stable but newer.
**Effort**: Medium-High

### Option E: Native Android (Kotlin) + iOS Companion (Swift) — Dual Native

**Description**: Full native Kotlin app for Android (core tracking). Separate limited Swift app for
iOS that offers what little is possible (scheduled ESM prompts, Screen Time screenshot
collection, manual data entry).

**Trade-offs**:
- ✅ Maximum capability on each platform
- ✅ No framework overhead or plugin dependencies
- ❌ Two completely separate codebases
- ❌ Requires expertise in both Kotlin and Swift
- ❌ Double the maintenance burden

**Risks**: Maintaining two codebases is unsustainable for a small research team.
**Effort**: High

---

## Axis 3: CSV Upload Method (No Server)

### Upload Option 1: Telegram Bot API (Direct HTTP POST)

**Description**: Create a Telegram bot (5-minute setup via @BotFather). Embed bot token in app.
App silently POSTs CSV to a Telegram channel via `https://api.telegram.org/bot<TOKEN>/sendDocument`.
Research team reads files from the channel.

**Trade-offs**:
- ✅ **Zero taps** for the user — fully automated background upload
- ✅ Completely free, no business verification
- ✅ No server needed — Telegram IS your server
- ✅ 5-minute setup
- ✅ Files stored on Telegram indefinitely
- ✅ Simple HTTP POST from any language
- ❌ Bot token is embedded in app (extractable if decompiled — acceptable for research)
- ❌ 20 MB file size limit (more than enough for CSV)
- ❌ Telegram less ubiquitous than WhatsApp in some regions

**Risks**: Bot token exposure (mitigable: limited research cohort, token is revocable).
**Effort**: Low

### Upload Option 2: WhatsApp Share Sheet (Intent/UIActivityViewController)

**Description**: User taps "Share" in the app, selects WhatsApp, picks a contact/group, sends.

**Trade-offs**:
- ✅ No setup needed — uses existing WhatsApp
- ✅ No server, no API keys
- ✅ End-to-end encrypted
- ❌ **3-4 taps every time** — cannot be automated
- ❌ User must manually select the researcher as recipient
- ❌ No confirmation that file was actually sent
- ❌ Files land in a chat thread — manual retrieval on researcher's end

**Risks**: User forgets to share, sends to wrong contact, or simply doesn't bother.
**Effort**: Low

### Upload Option 3: WhatsApp Business API (Bot Receives Files)

**Description**: Set up a WhatsApp Business account. Participants send CSV to the bot number.

**Trade-offs**:
- ✅ Familiar UX for participants
- ✅ Free for user-initiated conversations
- ❌ **Requires a webhook endpoint** (even if serverless: Lambda, Cloud Function)
- ❌ Requires Meta business verification (takes time)
- ❌ Requires dedicated phone number
- ❌ Still requires 3-4 manual taps from the user
- ❌ Complex setup compared to Telegram

**Risks**: Meta verification delays. Webhook maintenance.
**Effort**: Medium-High

### Upload Option 4: Firebase Cloud Storage

**Description**: App uploads CSV directly to a Firebase Cloud Storage bucket via Firebase SDK.

**Trade-offs**:
- ✅ Zero-tap automated upload
- ✅ Native SDKs for Android (and iOS, Flutter, RN)
- ✅ Automatic retry on poor network
- ✅ Generous free tier (5 GB storage, 20K uploads/day)
- ✅ Files land in a proper cloud bucket
- ❌ Technically uses Google's servers (but "serverless" from dev perspective)
- ❌ Requires Firebase project setup
- ❌ Requires Google Cloud billing account for overages

**Risks**: Free tier limits (unlikely to be an issue for research).
**Effort**: Low-Medium

### Upload Option 5: Google Drive API

**Description**: App authenticates via OAuth and uploads CSV to a shared Google Drive folder.

**Trade-offs**:
- ✅ Zero-tap after initial OAuth consent
- ✅ Files in familiar Google Drive interface
- ✅ 15 GB free storage
- ❌ Requires OAuth flow (initial friction for participants)
- ❌ Requires Google account
- ❌ More complex implementation than Telegram

**Risks**: OAuth token expiry, rate limits.
**Effort**: Medium

### Upload Option 6: Email (SMTP)

**Description**: App sends CSV as email attachment via SMTP (e.g., Gmail SMTP).

**Trade-offs**:
- ✅ Zero-tap automated sending
- ✅ Universally available
- ❌ Requires hardcoding SMTP credentials (security risk)
- ❌ Gmail may block automated sending
- ❌ Emails may land in spam
- ❌ Harder on iOS (no native JavaMail)

**Risks**: Account blocking, credential exposure.
**Effort**: Low (Android), Medium (iOS)

---

## Upload Method Comparison

| Method | User taps | Fully automated? | Server needed? | Setup effort | Cost |
|---|---|---|---|---|---|
| **Telegram Bot API** | **0** | **Yes** | **No** | **5 min** | **Free** |
| WhatsApp Share Sheet | 3-4 | No | No | None | Free |
| WhatsApp Business API | 3-4 | No | Yes (webhook) | Hours | Free (low vol) |
| Firebase Storage | 0 | Yes | No (managed) | 30 min | Free tier |
| Google Drive API | 0 (after OAuth) | Yes | No | 1 hour | Free |
| Email (SMTP) | 0 | Yes | No | 15 min | Free |

---

## Recommendation

### Framework: Option A — Native Android (Kotlin)

**Rationale:**
1. iOS cannot fulfill any core requirement — building for iOS would be wasted effort
2. The bmp-pathways-app reference is also native Android
3. Direct API access eliminates plugin/bridge failure modes
4. Simplest architecture = lowest risk for a research team
5. If iOS is needed later, a limited companion app can be built separately, or migrate to KMP

### Upload: Telegram Bot API (Option 1), with WhatsApp Share Sheet (Option 2) as fallback

**Rationale:**
1. Telegram Bot API enables zero-tap automated upload — critical for research data completeness
2. No server at all — Telegram's infrastructure handles everything
3. WhatsApp Share Sheet as a manual fallback for participants who prefer WhatsApp
4. Both can coexist in the app — automated Telegram + manual WhatsApp share button

### Widget Tracking: Scoped Down

**Rationale:**
Widget interaction tracking is not possible on Android (or iOS) for third-party widgets.
Recommend either:
- (a) Drop the widget requirement entirely
- (b) Track only the app's own widgets
- (c) Use AccessibilityService (powerful but Google Play restricts it; acceptable for sideloaded research apps)

---

## Open Questions for User

1. **iOS**: Given that iOS fundamentally blocks all core tracking features, should we proceed
   Android-only? Or do you want a limited iOS companion (e.g., ESM self-report prompts)?

2. **Widget tracking**: This is technically infeasible for third-party widgets. Should we drop it,
   or explore AccessibilityService (which can observe all UI interactions but is restricted by
   Google Play and raises privacy concerns)?

3. **Telegram vs WhatsApp**: Telegram enables fully automated, zero-tap upload. WhatsApp
   always requires manual user interaction (3-4 taps). Do you want both, or is one sufficient?

4. **Distribution**: The bmp-pathways-app used sideloading (direct APK). Google is tightening
   sideloading restrictions in 2026-2027 (developer identity verification). Do you want to
   target Play Store, sideloading, or both?

5. **Research context**: Is this for an IRB-approved academic study? This affects whether
   AccessibilityService and sideloading are acceptable, and whether you need consent flows
   in the app.

6. **Number of participants**: How many people will use this? This affects distribution method,
   Telegram bot token security considerations, and whether Play Store listing is worthwhile.

7. **Language**: The PDF is in French — should the app UI be in French, English, or bilingual?
