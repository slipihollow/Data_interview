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

### iOS — Severely Limited via Standard APIs, but Feasible via SensorKit

Standard third-party iOS APIs cannot fulfill the core requirements:

| Capability | Standard APIs | Details |
|---|---|---|
| App open/close tracking | **BLOCKED** | No public API to monitor which apps a user opens in real-time |
| Phone unlock detection | **BLOCKED** | No reliable public API (but see counter-investigation) |
| Background service | **BLOCKED** | iOS suspends apps after ~30s. No persistent foreground service |
| Survive reboot | **BLOCKED** | No auto-launch for third-party apps |
| Widget interactions | **BLOCKED** | Same as Android — no API for third-party widget monitoring |

**However**, a counter-investigation using peer-reviewed academic sources revealed that
**Apple SensorKit** (a research-specific framework, iOS 14+) provides passive, OS-managed
collection of device usage data including unlock counts, app usage by bundle identifier,
and screen time — fulfilling most core PB-001 requirements. Additionally, the Beiwe platform
(Harvard) has been detecting iOS lock/unlock events in production since 2016.

**See:** [Counter-Investigation: iOS Feasibility](./counter-investigation-ios.md) for full
analysis with 9 peer-reviewed sources and 3 validated research platforms (academic validation
of the approaches documented in Axis 4 below).

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

## Axis 4: iOS Deep Dive — Every Possible Path

### iOS Option 1: Apple SensorKit (BEST — Purpose-Built for Academic Research)

**Description**: Apple SensorKit (`SRDeviceUsageReport`) is a framework specifically designed
for IRB-approved academic health/wellness research. It provides per-app usage duration,
unlock counts, notification interactions, and more — data that NO other iOS API exposes.

**What it provides:**
- Total screen wakes and unlocks (with counts)
- Total unlock duration (seconds)
- **Per-app usage time** (seconds) with bundle identifiers (pseudonymized on iOS 15+)
- App usage grouped by 29+ categories (Social, Games, Health, Productivity, etc.)
- Notification interactions by category
- Web usage by category
- Additional sensors: call logs, message logs, keyboard metrics, accelerometer, location, etc.

**How to get access:**
1. Submit research proposal to `sensorkitrequest@apple.com`
2. Apple reviews → grants development entitlement
3. Obtain IRB approval for your study
4. Apple grants distribution entitlement
5. Publish via App Store (standard review) or unlisted distribution
6. Participants install, consent on-device to each sensor type

**Proven at scale:**
- **Intern Health Study** (Univ. of Michigan, 2023-24): 695 iPhone participants, 94% retention at 2 months
- **Apple Heart & Movement Study**: Large-scale ongoing
- Multiple published papers in PLOS ONE, PMC, Nature

**Trade-offs:**
- ✅ **Richest iOS usage data available** — nothing else comes close
- ✅ Apple-sanctioned, App Store viable
- ✅ Proven with hundreds of participants
- ✅ No jailbreak, no MDM, no supervision required
- ✅ Includes data far beyond what Android provides (call logs, keyboard metrics, etc.)
- ❌ Apple's approval timeline is unpredictable (weeks to months)
- ❌ Restricted to IRB-approved health/wellness research (not commercial)
- ❌ iOS 15+ uses pseudonymized app identifiers (privacy tradeoff)
- ❌ Requires iOS 14.0+

**Risks**: Apple approval delays. Pseudonymized app IDs may reduce data utility.
**Effort**: Medium (app development) + Variable (Apple approval process)

---

### iOS Option 2: Screen Time API (DeviceActivityMonitor → UserDefaults → Main App)

**Description**: Use Apple's Screen Time framework to detect usage thresholds and relay
timestamps to the main app. This is how production apps (Opal, ScreenZen, Clearspace) work.

**Architecture (the "Kingstinct technique"):**
1. Request FamilyControls entitlement from Apple (2-5 week approval)
2. Use `DeviceActivityMonitor` with named threshold events (e.g., `minutes_reached_1`,
   `minutes_reached_5`, `minutes_reached_10`)
3. When `eventDidReachThreshold` fires, **write event name + timestamp to UserDefaults**
   via App Group (this works — confirmed on real devices)
4. Main app reads from shared UserDefaults, reconstructs approximate usage timeline
5. `DeviceActivityReport` extension displays pretty per-app charts in-app (display only)
6. Main app handles CSV generation and upload (Telegram/WhatsApp)

**Key constraints:**
- `DeviceActivityMonitor` extension: CAN read/write App Group UserDefaults, CANNOT make network calls
- `DeviceActivityReport` extension: CAN read App Group, CANNOT write, CANNOT network
- Maximum 20 simultaneous monitors
- Minimum schedule interval: 15 minutes
- Monitor extension memory limit: 6 MB
- Up to 15-minute latency in Screen Time data updates (reported by Opal)

**Distribution**: Unlisted App Store distribution — Apple explicitly supports this for
"research studies." Full App Store review, but discoverable only via direct link.
No tester limits, no 90-day expiry (unlike TestFlight).

**Trade-offs:**
- ✅ Official App Store app — professional, trustworthy for participants
- ✅ Threshold-based timestamps give approximate usage data
- ✅ Beautiful in-app display via DeviceActivityReport
- ✅ No supervision or jailbreak needed
- ✅ FamilyControls `.individual` mode — user manages own device
- ❌ Data is **approximate** (threshold-based, not continuous app open/close events)
- ❌ Cannot identify WHICH specific app caused a threshold (only the monitored group)
- ❌ 20-monitor limit constrains per-app granularity
- ❌ FamilyControls entitlement approval: 2-5 weeks, no tracking, no status page
- ❌ Extension sandbox is fragile — Apple may tighten restrictions further
- ❌ iOS 26 has known bugs with premature threshold firing

**Risks**: Apple entitlement delays. Sandbox restrictions may tighten. Data granularity
insufficient for research needs.
**Effort**: Medium-High

---

### iOS Option 3: iOS Shortcuts Automations (No App Needed — Low-Tech)

**Description**: Use iOS Personal Automations to trigger on app open/close and log timestamps
to a CSV file in iCloud Drive. No custom app required — just the built-in Shortcuts app.

**How it works:**
1. Create a Personal Automation per app: trigger = "App → Is Opened"
2. Action = "Append to Text File" → writes `app_name,timestamp` to a CSV in iCloud Drive
3. Create a second automation per app: trigger = "App → Is Closed"
4. Action = appends close timestamp
5. iOS 17+: automations run immediately, no confirmation tap needed
6. Notification banner can be silenced ("Notify When Run" = off)

**Trade-offs:**
- ✅ **Zero development effort** — uses built-in iOS features
- ✅ No app needed, no entitlement, no App Store review
- ✅ Runs immediately without user confirmation (iOS 17+)
- ✅ Logs to iCloud Drive → accessible as CSV
- ✅ Survives reboots (automations persist)
- ✅ Can also POST to a URL (Telegram bot, webhook, etc.)
- ❌ **One automation per app** — must be created manually by the participant
- ❌ No API to create automations programmatically — user must set up each one
- ❌ No "all apps" wildcard — if you want 30 apps, you need 60 automations
- ❌ Cannot detect unlocks (no trigger for that)
- ❌ "Is Closed" trigger may not always fire reliably
- ❌ ~1-2 second latency

**Risks**: Participant fatigue during setup. Missed events if automations break after iOS updates.
**Effort**: Low (development), Medium (participant setup burden)

---

### iOS Option 4: macOS knowledgeC.db Sync (Richest Data, Requires a Mac)

**Description**: iOS syncs Screen Time data to macOS when "Share across devices" is enabled.
On macOS, the `knowledgeC.db` SQLite database contains **full per-app, per-session usage data
with precise timestamps** — far richer than any iOS API provides.

**How it works:**
1. Participant enables "Share across devices" in Settings → Screen Time
2. Participant has a Mac with the same iCloud account
3. Python script (`ScreenTime2CSV` by Felix Kohlhas) queries:
   ```sql
   SELECT * FROM ZOBJECT WHERE ZSTREAMNAME = '/app/usage'
   ```
4. Returns: app bundle ID, usage duration (seconds), start/end timestamps, device model
5. Data covers ~28 days

**Trade-offs:**
- ✅ **Richest per-app data available** — precise session-level timestamps
- ✅ No app development needed — just a Python script
- ✅ No jailbreak, no MDM, no entitlement
- ✅ Proven methodology (Felix Kohlhas, forensics community)
- ❌ **Requires participant to own a Mac** — major limitation
- ❌ Participant must run the Python script (or you provide a packaged tool)
- ❌ iOS 16+ migrating data to Biome format (less accessible)
- ❌ Fragile across OS versions
- ❌ Not scalable for large studies

**Risks**: macOS requirement excludes many participants. Biome migration may break this approach.
**Effort**: Low (scripting), but High (operational — requires Mac access per participant)

---

### iOS Option 5: Content Filter Network Extension (Proxy via Network Traffic)

**Description**: Deploy a `NEFilterDataProvider` on supervised (MDM-enrolled) research devices.
The filter observes all TCP/UDP flows and logs `sourceAppIdentifier` — which app made the
network request — as a proxy for "which apps were active."

**Trade-offs:**
- ✅ Can see bundle identifiers of network-active apps with timestamps
- ✅ System-level visibility, not limited by sandbox
- ❌ **Requires supervised devices** (must be MDM-enrolled via Apple Configurator)
- ❌ Only captures apps that generate network traffic (offline apps invisible)
- ❌ Filter extension has severe sandbox: cannot write to disk or network
- ❌ Communication with main app only via shared UserDefaults (fragile)
- ❌ Complex architecture, hard to maintain
- ❌ iOS 26's new `NEURLFilter` uses zero-knowledge bloom filter — cannot identify apps

**Risks**: Supervision requirement makes this impractical outside lab settings.
**Effort**: High

---

### iOS Option 6: App Privacy Report Export (7-Day Network Activity)

**Description**: iOS's built-in App Privacy Report (Settings → Privacy → App Privacy Report)
exports NDJSON with per-app network domain contacts and sensor access timestamps over 7 days.

**Trade-offs:**
- ✅ Machine-readable NDJSON export
- ✅ Shows which apps contacted which domains and when
- ✅ No app needed — built into iOS
- ❌ Only 7-day window
- ❌ No usage duration — only network contact timestamps
- ❌ Requires manual export by participant
- ❌ Indirect proxy (network activity ≠ foreground usage)

**Risks**: Low data fidelity. Short retention window.
**Effort**: Very Low (no development), Medium (manual participant process)

---

## iOS Options Comparison

| Option | Data Quality | Unlock Detection | App-Level Granularity | Effort | Requires |
|---|---|---|---|---|---|
| **1. SensorKit** | **Excellent** | **Yes (counts)** | **Yes (pseudonymized)** | Medium + Apple approval | IRB approval |
| **2. Screen Time API** | Approximate | No | Threshold-based groups | Medium-High | FamilyControls entitlement |
| **3. Shortcuts** | Per-app open/close | No | Yes (manually configured) | Low dev / Medium setup | Participant effort |
| **4. knowledgeC.db** | **Excellent** | Yes | **Yes (bundle IDs)** | Low dev | Mac + iCloud sync |
| **5. Network Filter** | Partial (net only) | No | Yes (bundle IDs) | High | Supervised device (MDM) |
| **6. Privacy Report** | Low (net contacts) | No | Yes (bundle IDs) | Very Low | Manual export |

---

## Revised Recommendation

### Two-Track Architecture

**Track 1 — Android: Native Kotlin (full passive tracking)**
- Direct `UsageStatsManager`, `ForegroundService`, `BroadcastReceiver`
- Full unlock detection, app open/close with timestamps
- CSV per activation, automated Telegram upload + WhatsApp share fallback
- Sideload via GitHub APK + optional Play Store listing

**Track 2 — iOS: Depends on research context**

| Your Situation | Best iOS Path | Data Quality |
|---|---|---|
| IRB-approved study | **SensorKit** — submit to `sensorkitrequest@apple.com` | Excellent |
| No IRB / Apple rejects SensorKit | **Screen Time API** (Option 2) — FamilyControls entitlement | Approximate |
| Quick pilot / no app budget | **Shortcuts** (Option 3) — participants self-configure | Per-app open/close |
| Participants have Macs | **knowledgeC.db** (Option 4) — Python script | Excellent |

### Framework Decision

| iOS Path Chosen | Framework |
|---|---|
| SensorKit | Native Swift (iOS) + Native Kotlin (Android) — two apps |
| Screen Time API | Consider KMP — shared logic, platform-specific tracking |
| Shortcuts only | Native Kotlin (Android only) — no iOS app needed |

### Upload: Telegram Bot API + WhatsApp Share Sheet

Both coexist in the app:
- **Telegram**: automated zero-tap upload via HTTP POST to bot channel
- **WhatsApp**: manual share button as fallback for participants who prefer it

### Widget Tracking: Scoped Down

Not possible on either platform for third-party widgets. Options:
- (a) Drop the requirement
- (b) Use AccessibilityService on Android (acceptable for sideloaded research apps)

---

## Open Questions for User

1. **IRB approval**: Is this for an IRB-approved academic study? This determines whether
   SensorKit (best iOS path by far) is available to you.

2. **iOS priority**: How critical is iOS coverage? Your options ranked:
   - **(a)** SensorKit app — best data, needs IRB + Apple approval, unpredictable timeline
   - **(b)** Screen Time API app — approximate data, FamilyControls entitlement (2-5 weeks)
   - **(c)** Shortcuts automations — no app needed, participant setup burden
   - **(d)** knowledgeC.db extraction — richest raw data, requires Mac
   - **(e)** Skip iOS entirely, focus on Android

3. **Widget tracking**: Drop it, or explore AccessibilityService (Android only)?

4. **Distribution**: Sideloading (GitHub APK), Play Store, or both?
   For iOS: TestFlight (90-day builds, 10K testers) or unlisted App Store?

5. **Number of participants**: Affects distribution, security, and scale decisions.

6. **Language**: App UI in French, English, or bilingual?

7. **Timeline**: SensorKit approval can take months. Screen Time entitlement: 2-5 weeks.
   Android app could be built in weeks. When do you need this?
