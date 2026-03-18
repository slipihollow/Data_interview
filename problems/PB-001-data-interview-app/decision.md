# Decision: PB-001

**Chosen option**: Two-track — Android native (Kotlin) NOW + iOS SensorKit proposal in parallel
**Rationale**: IRB-approved study, 50-100 participants, immediate start needed
**Date**: 2026-03-18

## Scope Clarification

The original investigation over-scoped the requirements. Actual needs:

1. **Unlock counter** — count + timestamp (HH:MM) per unlock
2. **App usage** — app name, open timestamp, close timestamp
3. **Media control interactions** — lock screen media controls (play/pause/skip on Spotify etc.)
   NOT arbitrary widget taps. Android: `MediaSessionManager` + `NotificationListenerService`

## Decisions

| Axis | Decision |
|---|---|
| Android framework | Native Kotlin |
| iOS framework | Native Swift + SensorKit (submit Apple proposal today, build when approved) |
| Upload | Telegram Bot API (automated) + WhatsApp share sheet (manual fallback) |
| Distribution | Sideloading APK via GitHub (Android). TestFlight (iOS when ready) |
| Participants | 50-100, IRB-approved |
| Language | French first, English later |
| Widget tracking | Reframed as media control tracking via MediaSessionManager |
| Timeline | Start Android build immediately |
