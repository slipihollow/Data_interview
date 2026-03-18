---
id: PB-001
status: open
created: 2026-03-18
branch: PB-001-data-interview-app
---

# Problem: Design & Build the "Data Interview" Research Telemetry App

## Problem Statement

Build a smartphone application called "Data Interview" for academic research that passively
monitors and logs user-phone interactions (unlocks, app opens/closes, widget interactions).
The app must run in the background, survive reboots, and produce one CSV file per activation
period. Data upload must happen via WhatsApp or Telegram (no custom server). The app should
run on both iOS and Android, be distributed via GitHub, and include a user manual.

### Core Requirements (from PDF)

1. **Track phone unlocks** — log each unlock with timestamp (HH:MM)
2. **Track app usage** — log app name, open time, close time
3. **Track widget interactions** — log widget name, time, location (lock screen vs home screen)
4. **Background operation** — survive app closure and device reboots
5. **Activation model** — manual start/stop + scheduled activation (start/end date+time)
6. **Output** — 1 CSV file per activation period, stored locally
7. **CSV schema** — 5 columns: interaction_type | time | app_or_widget_name | close_time | widget_location
8. **File management** — view, download, share CSV from within the app
9. **Upload** — via WhatsApp or Telegram (no server)
10. **Cross-platform** — iOS and Android
11. **Distribution** — GitHub repo (inspired by jonnyhuck/bmp-pathways-app)
12. **User manual** — installation and usage instructions

## Acceptance Criteria

- [ ] App logs phone unlocks with timestamps
- [ ] App logs app opens/closes with names and timestamps
- [ ] App logs widget interactions with names, timestamps, and locations
- [ ] App runs as a background service, survives closure and reboot
- [ ] Manual start/stop and scheduled activation work correctly
- [ ] One CSV file is generated per activation period with correct schema
- [ ] User can view, download, and share CSV files from the app UI
- [ ] CSV upload works via WhatsApp and/or Telegram
- [ ] App runs on Android (and iOS where feasible)
- [ ] GitHub repo with README and user manual
