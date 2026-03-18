# Counter-Investigation: iOS Feasibility for PB-001

**Date:** 2026-03-18
**Scope:** Academic-sourced reassessment of iOS capabilities for the Data Interview app
**Methodology:** Peer-reviewed literature, Apple developer documentation, validated research platforms
**Constraints:** Research project, hand-picked participants with informed consent, Telegram upload (solved)

---

## Summary of Original Investigation's iOS Verdict

The original PB-001 investigation (Axis 1) concluded that iOS is **"severely limited"** and that
**"iOS cannot fulfill any core requirement."** It recommended Android-only development.

**This counter-investigation demonstrates that conclusion was premature.** The original
investigation examined only standard third-party APIs (Screen Time/DeviceActivity, background
modes, MDM) and missed the single most important Apple framework for this use case:
**Apple SensorKit.**

---

## Finding 1: Apple SensorKit — The Missing Framework

### What It Is

Apple SensorKit is a **research-specific framework** (iOS 14.0+) that provides approved research
applications with access to device sensor data that is otherwise unavailable to third-party apps.
It is entirely separate from the Screen Time/DeviceActivity API explored in the original
investigation. SensorKit requires:

1. IRB (or equivalent ethics board) approval
2. Apple review and approval (submit proposal to `sensorkitrequest@apple.com`)
3. Per-participant opt-in consent

### What the Device Usage Sensor Captures

The `SRDeviceUsageReport` sensor provides **exactly the data PB-001 requires**:

| Data Field | Description | Maps to PB-001 Requirement |
|---|---|---|
| `totalUnlocks` | Number of times user unlocked the phone | **Phone unlock tracking** |
| `totalUnlockDuration` | Total unlocked duration in seconds | Phone unlock tracking |
| `totalScreenWakes` | Number of screen wake events | Phone unlock tracking |
| App usage by `bundleIdentifier` | Per-app usage with **actual bundle IDs** | **App usage tracking** |
| App usage by category | 29 categories (Entertainment, Social Networking, Health & Fitness, etc.) | App usage tracking |
| Notification counts by app type | Notifications received per category | Supplementary data |
| Web browsing by domain category | Duration by website category | Supplementary data |
| Text input sessions | Keyboard, dictation, pencil input sessions | Supplementary data |

**Critical detail:** Unlike the DeviceActivityReport extension (which sandboxes data and
prevents export), SensorKit data flows directly into the research app and **can be
programmatically exported** — to CSV, to Telegram, to any destination.

### Peer-Reviewed Validation

**Study 1:** Torous et al. (2023). "Exploring the Potential of Apple SensorKit and Digital
Phenotyping Data as New Digital Biomarkers for Mental Health Research." *Digital Biomarkers*,
7(1), 104. [PMC10601905](https://pmc.ncbi.nlm.nih.gov/articles/PMC10601905/)

Key findings from a single-participant feasibility study using the mindLAMP platform:
- Device Usage sensor captured **42–152 daily unlocks**
- Unlocked duration ranged from **3,971–33,502 seconds daily**
- **51–182 screen wake events** per day
- Application category breakdowns (healthcare, sports, social media, etc.)
- The authors conclude SensorKit data **"far surpasses the current capabilities of any digital
  phenotyping app"** for contextual behavioral data

**Study 2:** Necamp et al. (2025). "Feasibility and acceptability of collecting passive phone
usage and sensor data via Apple SensorKit." *PLOS One*.
[PMC12349082](https://pmc.ncbi.nlm.nih.gov/articles/PMC12349082/)

Key findings from the longitudinal Intern Health Study (n=1,164 iPhone users):
- **59.7% opt-in rate** (695/1,164) for at least one sensor
- **94.0% retention** after 2 months (653/695 remained active)
- Five sensors deployed: Ambient Light, Keyboard Metrics, Message Usage, Phone Usage,
  Frequently Visited Locations
- Successful large-scale deployment in a peer-reviewed, IRB-approved study

### How SensorKit Addresses Each PB-001 Requirement

| PB-001 Requirement | SensorKit Capability | Verdict |
|---|---|---|
| Track phone unlocks with timestamps | `totalUnlocks`, `totalScreenWakes` with temporal data | **FULFILLED** |
| Track app usage (name, open time, close time) | Bundle identifiers + usage duration by category. Note: provides duration per reporting period, not individual open/close events | **PARTIALLY FULFILLED** (duration yes, individual open/close events no) |
| Track widget interactions | Not available via SensorKit | **NOT FULFILLED** (same as Android) |
| Background operation | SensorKit collection is **managed by the OS**, not the app. No foreground service needed. Data accumulates in the background automatically | **FULFILLED** |
| Survive reboot | OS-level collection persists across reboots. App queries accumulated data on next launch | **FULFILLED** |
| CSV output | Data is programmatically accessible — can be formatted to CSV within the app | **FULFILLED** |
| Upload via Telegram | Data can be exported and sent via Telegram Bot API | **FULFILLED** |

### Approval Process

1. **Prepare research proposal** using Apple's template (available at researchandcare.org)
2. **Submit to Apple** at `sensorkitrequest@apple.com` with IRB documentation
3. **Receive development entitlement** (limited devices for testing)
4. **Obtain IRB approval** for the study protocol
5. **Receive distribution entitlement** for participant-facing app
6. **App Store review** (standard process)

Apple requires for each requested data type:
- Brief justification (1–2 sentences) of why it is necessary
- Clear disclosure to participants in the consent form
- Institutional Apple Developer account with SensorKit Addendum signed

### Research Platforms with SensorKit Integration

Two established platforms already integrate SensorKit, reducing development effort:

1. **mindLAMP** (Beth Israel Deaconess Medical Center / Division of Digital Psychiatry)
   - Open-source: [docs.lamp.digital](https://docs.lamp.digital/)
   - SensorKit integration validated in Torous et al. (2023)
   - iOS + Android support
   - Peer-reviewed: Torous et al. (2022). *JMIR mHealth and uHealth*, 10(1), e30557.
     [PMC8783287](https://pmc.ncbi.nlm.nih.gov/articles/PMC8783287/)

2. **Avicenna Research**
   - Commercial platform with SensorKit support
   - Handles approval coordination with Apple
   - 1-minute active collection cycles every 5 minutes
   - 8-hour collection windows for most sensors
   - Available on iOS App Store

3. **MyDataHelps** (CareEvolution)
   - Used in the Intern Health Study (Necamp et al., 2025)
   - Managed SensorKit enrollment and consent flows
   - Large-scale deployment validated

---

## Finding 2: Beiwe Platform — Lock/Unlock Detection on iOS

### Peer-Reviewed Source

Torous et al. (2016). "New Tools for New Research in Psychiatry: A Scalable and Customizable
Platform to Empower Data Driven Smartphone Research." *JMIR Mental Health*, 3(2), e16.
[PMC4873624](https://pmc.ncbi.nlm.nih.gov/articles/PMC4873624/)

### iOS Capabilities Confirmed

The Beiwe platform (Harvard Onnela Lab) **does detect lock/unlock events on iOS** via power
state monitoring:

- **"Unlocked"** and **"Locked"** state events with UTC timestamps (milliseconds)
- Battery level at each event (percentage as decimal, e.g., 0.68)
- Charging status (Unplugged, Charging, Full)
- Screen on/off as a **proxy for phone usage** — "if a participant wakes up at 3:44am, checks
  his/her phone for 10 seconds, and then goes back to sleep, Beiwe will record that the phone
  screen turned on for 10 seconds at 3:44am"

**This directly contradicts the original investigation's claim that "phone unlock detection is
NOT POSSIBLE" on iOS.** Beiwe has been doing it in production since 2016 and the app (Beiwe2)
is available on the App Store.

### Limitations on iOS vs. Android

| Data Type | iOS | Android |
|---|---|---|
| Lock/Unlock events | Yes | Yes |
| App usage tracking | **No** | Yes |
| Call/text metadata | **No** | Yes |
| Accelerometer | Yes | Yes |
| GPS | Yes | Yes |
| Gyroscope | Yes | Yes |
| Wi-Fi/Bluetooth scanning | **No** | Yes |

Beiwe source code: [github.com/onnela-lab/beiwe-ios](https://github.com/onnela-lab/beiwe-ios)

---

## Finding 3: AWARE Framework iOS — 97% Sensor Data Retrieval

### Peer-Reviewed Source

Nishiyama et al. (2020). "iOS Crowd-Sensing Won't Hurt a Bit!: AWARE Framework and Sustainable
Study Guideline for iOS Platform." In *Distributed, Ambient and Pervasive Interactions*
(DAPI 2020), Springer LNCS.
[SpringerLink](https://link.springer.com/chapter/10.1007/978-3-030-50344-4_17)

### iOS Capabilities

The AWARE Framework iOS achieves **>97% sensor data retrieval** using a combination of
Experience Sampling Method (ESM) and Silent Push Notifications (SPN) in real-world conditions.

**Supported iOS sensors:**
- **Hardware:** Accelerometer, Gyroscope, Magnetometer, Gravity, Rotation, Barometer, Proximity
- **Location:** GPS, Timezone, WiFi
- **Device State:** Battery, Network, **Screen Events**, Call, Processor, Memory
- **Health:** HealthKit, Heartrate (BLE), Headphone Motion (iOS 14+)
- **Activity:** Motion Activity, Pedometer
- **External:** Fitbit, NTPTime, OpenWeatherMap

**Screen Events** are monitored, providing screen on/off state changes. Combined with Battery
state changes, this provides unlock/lock proxy detection similar to Beiwe.

**Background execution strategy:** Uses location updates background mode +
`requestPermissionForBackgroundSensing` to maintain continuous sensor collection.

Source code: [github.com/tetujin/AWAREFramework-iOS](https://github.com/tetujin/AWAREFramework-iOS)

---

## Finding 4: Screen Time Data Donation — Validated Methodology

### Peer-Reviewed Source

Ohme, J., Araujo, T., de Vreese, C.H., & Piotrowski, J.T. (2021). "Mobile data donations:
Assessing self-report accuracy and sample biases with the iOS Screen Time function."
*Mobile Media & Communication*, 9(2), 293–313.
[SAGE](https://journals.sagepub.com/doi/10.1177/2050157920959106)

### Methodology

Participants take screenshots of their iOS Screen Time settings page and upload them. This
provides:
- **Per-app usage duration** (specific app names)
- **Number of pickups** (unlock proxy)
- **Number of notifications** per app
- **Onscreen vs. background time**

### Validation Results (n=404, Dutch general population)

- 72% of participants successfully uploaded screenshots from their mobile device
- Strong tendency to **underreport** smartphone usage in self-reports vs. Screen Time data
- Method validated for population-level research

### Relevance to PB-001

For a **hand-picked cohort with informed consent**, this method is significantly more feasible
than in general population studies. Compliance can be reinforced through direct researcher
contact. The screenshots provide the exact data needed: app names, usage durations, and
pickup counts.

**Enhancement for PB-001:** Instead of generic screenshots, participants can screenshot from
a `DeviceActivityReportExtension` view built into the study app, which provides a more
structured display of the same data. The app can then guide participants through the Telegram
upload flow.

### Additional Validation

Noë et al. (2019). "A Novel Approach to Evaluating Mobile Smartphone Screen Time for iPhones:
Feasibility and Preliminary Findings." *JMIR mHealth and uHealth*, 6(11), e11012.
[PMC6277825](https://pmc.ncbi.nlm.nih.gov/articles/PMC6277825/)

---

## Finding 5: iOS vs. Android Comparative Data Quality

### Peer-Reviewed Source

Böttcher et al. (2024). "Comparative Assessment of Multimodal Sensor Data Quality Collected
Using Android and iOS Smartphones in Real-World Settings." *Sensors*, 24(19), 6246.
[PMC11478693](https://pmc.ncbi.nlm.nih.gov/articles/PMC11478693/)

### Key Findings

- Android: up to **21 passive sensor data types**
- iOS: up to **7 sensors** (without SensorKit; with SensorKit this increases substantially)
- iOS demonstrated **superior data consistency** (lower anomalous point density, better
  sampling rate consistency)
- iOS showed **worse GPS completeness** (higher missingness due to stricter privacy controls)

**Implication:** When iOS can collect data, the data quality tends to be higher than Android.
The limitation is scope of collection, not quality.

---

## Finding 6: App Distribution for Research

For a hand-picked research cohort with informed consent, multiple distribution paths exist:

### Option 1: TestFlight (Recommended for Research)

- Up to **10,000 external testers** per app
- **90-day build expiry**, renewable by uploading a new build
- No App Store review required for internal testers (up to 100 per team)
- External testers require Beta App Review (lighter than full App Store review)
- Participants install via TestFlight app + invite link

For a study with hand-picked participants, TestFlight's 90-day cycle is manageable: upload a
new build each quarter. Participants receive an automatic update notification.

### Option 2: Unlisted App Store Distribution

- App is on the App Store but **not discoverable via search**
- Distributed via direct link
- No expiry
- Requires full App Store review

### Option 3: Ad Hoc Distribution

- Signed with specific device UDIDs (up to 100 devices per year)
- 1-year certificate validity
- No App Store review
- Requires physical device registration

---

## Revised iOS Feasibility Matrix

| PB-001 Requirement | Original Verdict | Counter-Investigation Verdict | Method |
|---|---|---|---|
| Phone unlock tracking | NOT POSSIBLE | **FEASIBLE** | SensorKit `totalUnlocks` + Beiwe power state monitoring |
| App usage tracking | NOT POSSIBLE | **FEASIBLE** (with caveats) | SensorKit `bundleIdentifier` + category breakdown; provides duration per period, not individual open/close events |
| Widget interactions | NOT POSSIBLE | **NOT FEASIBLE** | Same conclusion — no API on either platform for third-party widgets |
| Background operation | NOT POSSIBLE | **FEASIBLE** | SensorKit is OS-managed; Beiwe/AWARE use location background mode |
| Survive reboot | NOT POSSIBLE | **FEASIBLE** | SensorKit data persists OS-level; Beiwe relaunches via location services |
| CSV export | NOT ADDRESSED | **FEASIBLE** | SensorKit data is programmatically accessible |
| Telegram upload | NOT ADDRESSED | **FEASIBLE** | Standard HTTP POST from app |

### Caveats

1. **SensorKit requires Apple approval** — timeline not guaranteed, proposal review is opaque
2. **App usage is by category + bundle ID** — provides duration per reporting period, not
   individual app open/close timestamps with HH:MM precision as the PDF spec requires
3. **SensorKit is not available for Screen Time API apps** — it's a separate entitlement
4. **Per-participant opt-in** — each participant must individually consent to each sensor
5. **Widget tracking** remains infeasible on both platforms

---

## Recommended iOS Architecture

### Tier 1: SensorKit Path (Best — If Apple Approves)

```
┌──────────────────────────────────────────────────┐
│                 Study App (Swift)                 │
│                                                  │
│  ┌────────────┐  ┌─────────────┐  ┌───────────┐ │
│  │ SensorKit  │  │ CSV         │  │ Telegram  │ │
│  │ Device     │→ │ Generator   │→ │ Bot API   │ │
│  │ Usage      │  │             │  │ Upload    │ │
│  └────────────┘  └─────────────┘  └───────────┘ │
│                                                  │
│  ┌────────────┐  ┌─────────────┐                │
│  │ ResearchKit│  │ Consent +   │                │
│  │ Consent    │  │ Onboarding  │                │
│  │ Flow       │  │ UI          │                │
│  └────────────┘  └─────────────┘                │
└──────────────────────────────────────────────────┘
         ↓ OS-managed background collection
┌──────────────────────────────────────────────────┐
│           iOS SensorKit (System Level)           │
│  • Device unlocks, screen wakes                  │
│  • App usage by bundle ID + category             │
│  • Keyboard metrics, message usage               │
│  • Collected passively, survives reboots         │
└──────────────────────────────────────────────────┘
```

**Data flow:**
1. SensorKit collects device usage data passively at the OS level
2. App queries SensorKit for accumulated data at configurable intervals
3. Data is formatted to CSV (matching PB-001 schema as closely as possible)
4. CSV is uploaded to Telegram channel via Bot API

**Development effort:** Medium (Swift app + SensorKit integration + Telegram upload)
**Apple approval effort:** Medium-High (proposal + IRB + entitlement review)

### Tier 2: Beiwe/AWARE + Data Donation (Fallback — No Apple Approval Needed)

If SensorKit approval is denied or takes too long:

1. **Beiwe or AWARE iOS** for passive lock/unlock + screen state detection
2. **DeviceActivityReportExtension** to display per-app usage data within the app
3. **Guided screenshot donation** — app prompts participant to screenshot the report view
4. **Telegram upload** of screenshots via share sheet

This provides:
- Lock/unlock events (automated via Beiwe/AWARE)
- App usage data (semi-automated via guided screenshots)
- Background operation (via location services background mode)

**Development effort:** Low-Medium (integrate existing open-source platform)
**Apple approval effort:** Low (standard App Store review; no SensorKit entitlement needed)

### Tier 3: Pure Data Donation (Simplest — Guaranteed Approval)

Minimal app that:
1. Schedules ESM notifications at configurable intervals
2. Prompts participants to screenshot their iOS Screen Time page
3. Provides a Telegram share button to upload screenshots
4. Optionally includes a manual data entry form

**Development effort:** Low
**Apple approval effort:** Low

---

## Impact on Framework Decision

The original investigation recommended **Option A: Native Android (Kotlin)** based on the
premise that iOS "cannot fulfill any core requirement." This counter-investigation changes
that calculus:

| Option | Original Recommendation | Revised Assessment |
|---|---|---|
| **A: Native Android only** | Recommended | Still valid for Android. But iOS is no longer impossible. |
| **E: Dual Native (Kotlin + Swift)** | Rejected (too costly) | **Reconsider.** With SensorKit, the iOS app is meaningfully functional, not a "shell." For a hand-picked research cohort, maintaining two codebases for a limited time is manageable. |
| **D: KMP** | Maybe later | **More attractive now.** Shared CSV generation, Telegram upload, and data models in Kotlin `commonMain`. Platform-specific SensorKit integration in `iosMain`, UsageStats in `androidMain`. |

**Revised recommendation:** If the study requires iOS participants (which is likely given
~55% iPhone market share in the US and Europe), pursue **SensorKit approval in parallel with
Android development**. Use **Option E (Dual Native)** or **Option D (KMP)** depending on
team expertise. Telegram upload code is identical across platforms.

---

## Academic Sources

### Peer-Reviewed Articles

1. Torous, J. et al. (2023). Exploring the Potential of Apple SensorKit and Digital Phenotyping
   Data as New Digital Biomarkers for Mental Health Research. *Digital Biomarkers*, 7(1), 104.
   [PMC10601905](https://pmc.ncbi.nlm.nih.gov/articles/PMC10601905/)

2. Necamp, T. et al. (2025). Feasibility and acceptability of collecting passive phone usage
   and sensor data via Apple SensorKit. *PLOS One*.
   [PMC12349082](https://pmc.ncbi.nlm.nih.gov/articles/PMC12349082/)

3. Torous, J. et al. (2016). New Tools for New Research in Psychiatry: A Scalable and
   Customizable Platform to Empower Data Driven Smartphone Research. *JMIR Mental Health*,
   3(2), e16. [PMC4873624](https://pmc.ncbi.nlm.nih.gov/articles/PMC4873624/)

4. Nishiyama, Y. et al. (2020). iOS Crowd-Sensing Won't Hurt a Bit!: AWARE Framework and
   Sustainable Study Guideline for iOS Platform. *DAPI 2020*, Springer LNCS.
   [SpringerLink](https://link.springer.com/chapter/10.1007/978-3-030-50344-4_17)

5. Ohme, J. et al. (2021). Mobile data donations: Assessing self-report accuracy and sample
   biases with the iOS Screen Time function. *Mobile Media & Communication*, 9(2), 293–313.
   [SAGE](https://journals.sagepub.com/doi/10.1177/2050157920959106)

6. Böttcher, L. et al. (2024). Comparative Assessment of Multimodal Sensor Data Quality
   Collected Using Android and iOS Smartphones in Real-World Settings. *Sensors*, 24(19), 6246.
   [PMC11478693](https://pmc.ncbi.nlm.nih.gov/articles/PMC11478693/)

7. Noë, B. et al. (2019). A Novel Approach to Evaluating Mobile Smartphone Screen Time for
   iPhones: Feasibility and Preliminary Findings. *JMIR mHealth and uHealth*, 6(11), e11012.
   [PMC6277825](https://pmc.ncbi.nlm.nih.gov/articles/PMC6277825/)

8. Torous, J. et al. (2022). Enabling Research and Clinical Use of Patient-Generated Health
   Data (the mindLAMP Platform): Digital Phenotyping Study. *JMIR mHealth and uHealth*,
   10(1), e30557. [PMC8783287](https://pmc.ncbi.nlm.nih.gov/articles/PMC8783287/)

9. Huckins, J.F. et al. (2025). Design Guidelines for Improving Mobile Sensing Data
   Collection: Prospective Mixed Methods Study. *J Med Internet Res*, 26, e55694.
   [JMIR](https://www.jmir.org/2024/1/e55694)

### Apple Developer Documentation

10. [SensorKit Documentation](https://developer.apple.com/documentation/sensorkit)
11. [SRDeviceUsageReport](https://developer.apple.com/documentation/sensorkit/srdeviceusagereport)
12. [SensorKit Research Proposal Guide](https://www.researchandcare.org/resources/accessing-sensorkit-data/)
13. [ResearchKit & CareKit](https://www.researchandcare.org)

### Open-Source Research Platforms

14. [Beiwe iOS (Harvard Onnela Lab)](https://github.com/onnela-lab/beiwe-ios)
15. [AWARE Framework iOS](https://github.com/tetujin/AWAREFramework-iOS)
16. [mindLAMP Platform](https://docs.lamp.digital/)
17. [Avicenna Research — SensorKit Reference](https://learn.avicennaresearch.com/reference/data-sources/from-wearable/apple/sensorkit/)
