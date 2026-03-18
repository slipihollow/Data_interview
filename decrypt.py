#!/usr/bin/env python3
"""Decrypt .enc files produced by the Data Interview app.

Usage:
    python decrypt.py --key private_key.pem --input file.enc

Requires: pip install cryptography
"""
import argparse
import struct
from pathlib import Path

from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.ciphers.aead import AESGCM


def decrypt(private_key_path: str, enc_path: str) -> None:
    # Load private key
    with open(private_key_path, "rb") as f:
        private_key = serialization.load_pem_private_key(f.read(), password=None)

    # Read .enc file
    data = Path(enc_path).read_bytes()
    offset = 0

    # [4 bytes] encrypted AES key length
    (enc_key_len,) = struct.unpack(">I", data[offset : offset + 4])
    offset += 4

    # [enc_key_len bytes] encrypted AES key
    enc_key = data[offset : offset + enc_key_len]
    offset += enc_key_len

    # [12 bytes] IV
    iv = data[offset : offset + 12]
    offset += 12

    # [remaining] ciphertext + GCM tag
    ciphertext = data[offset:]

    # Decrypt AES key with RSA-OAEP
    aes_key = private_key.decrypt(
        enc_key,
        padding.OAEP(
            mgf=padding.MGF1(algorithm=hashes.SHA256()),
            algorithm=hashes.SHA256(),
            label=None,
        ),
    )

    # Decrypt CSV with AES-GCM
    aesgcm = AESGCM(aes_key)
    plaintext = aesgcm.decrypt(iv, ciphertext, None)

    # Post-process CSV: add metrics and summary
    csv_text = plaintext.decode()
    lines = csv_text.strip().split("\n")
    header = lines[0]
    rows = lines[1:]

    from collections import defaultdict
    from datetime import datetime

    # Friendly display names for known packages
    FRIENDLY_NAMES = {
        "com.android.chrome": "Chrome",
        "com.brave.browser": "Brave",
        "com.instagram.android": "Instagram",
        "com.facebook.katana": "Facebook",
        "com.facebook.orca": "Messenger",
        "com.twitter.android": "Twitter/X",
        "com.zhiliaoapp.musically": "TikTok",
        "com.snapchat.android": "Snapchat",
        "com.reddit.frontpage": "Reddit",
        "com.linkedin.android": "LinkedIn",
        "org.telegram.messenger": "Telegram",
        "com.whatsapp": "WhatsApp",
        "com.discord": "Discord",
        "com.pinterest": "Pinterest",
        "com.tumblr": "Tumblr",
        "com.bereal.ft": "BeReal",
        "com.google.android.contacts": "Contacts",
        "com.google.android.gm": "Gmail",
        "com.google.android.apps.maps": "Google Maps",
        "com.google.android.youtube": "YouTube",
        "com.google.android.apps.photos": "Google Photos",
        "com.google.android.calendar": "Google Calendar",
        "com.android.launcher3": "Launcher",
        "com.android.settings": "Settings",
        "com.android.vending": "Play Store",
        "com.coingecko.coingeckoapp": "CoinGecko",
        "ch.admin.meteoswiss": "MeteoSwiss",
        "com.behance.behance": "Behance",
        "notion.id": "Notion",
        "com.datainterview.app": "Data Interview",
    }

    def friendly(name):
        """Return friendly name. If unknown package, extract last segment and capitalize."""
        if name in FRIENDLY_NAMES:
            return FRIENDLY_NAMES[name]
        # Already a readable name (no dots)
        if "." not in name:
            return name
        # Extract last meaningful segment: com.example.myapp -> Myapp
        parts = name.split(".")
        return parts[-1].replace("_", " ").capitalize()

    SOCIAL_MEDIA_APPS = {
        "com.instagram.android", "com.facebook.katana", "com.facebook.orca",
        "com.twitter.android", "com.zhiliaoapp.musically",  # TikTok
        "com.snapchat.android", "com.reddit.frontpage", "com.linkedin.android",
        "org.telegram.messenger", "com.whatsapp", "com.discord",
        "com.pinterest", "com.tumblr", "com.bereal.ft",
    }

    # Add duration column to header
    header += ";duree_secondes"

    # Parse all rows
    app_stats = defaultdict(lambda: {"count": 0, "total_seconds": 0})
    processed_rows = []
    unlocks = []
    all_times = []
    sessions = []  # list of (start_time, end_time, apps_used, duration_sec)

    # Track sessions: unlock starts a session, collect events until next unlock
    current_session_start = None
    current_session_apps = []
    current_session_last_time = None

    for row in rows:
        cols = row.split(";")
        interaction = cols[0] if len(cols) > 0 else ""
        heure = cols[1] if len(cols) > 1 else ""
        app = cols[2] if len(cols) > 2 else ""
        heure_fermeture = cols[3] if len(cols) > 3 else ""

        # Track all timestamps
        for t_str in [heure, heure_fermeture]:
            if t_str:
                try:
                    all_times.append(datetime.strptime(t_str, "%H:%M"))
                except ValueError:
                    pass

        # Track unlocks and sessions
        if interaction == "deverrouillage" and heure:
            try:
                unlock_time = datetime.strptime(heure, "%H:%M")
                # Close previous session
                if current_session_start is not None and current_session_last_time is not None:
                    sess_dur = (current_session_last_time - current_session_start).total_seconds()
                    if sess_dur < 0:
                        sess_dur += 86400
                    sessions.append((current_session_start, current_session_last_time, current_session_apps, sess_dur))
                # Start new session
                unlocks.append(unlock_time)
                current_session_start = unlock_time
                current_session_apps = []
                current_session_last_time = unlock_time
            except ValueError:
                pass

        # Track app usage within session
        duration_sec = ""
        if heure and heure_fermeture:
            try:
                t_open = datetime.strptime(heure, "%H:%M")
                t_close = datetime.strptime(heure_fermeture, "%H:%M")
                delta = (t_close - t_open).total_seconds()
                if delta < 0:
                    delta += 86400
                duration_sec = str(int(delta))

                if app:
                    app_stats[app]["count"] += 1
                    app_stats[app]["total_seconds"] += int(delta)

                if current_session_start is not None:
                    if app and app != "com.android.launcher3":
                        current_session_apps.append(app)
                    current_session_last_time = t_close
            except ValueError:
                pass

        processed_rows.append(f"{row};{duration_sec}")

    # Close last session
    if current_session_start is not None and current_session_last_time is not None:
        sess_dur = (current_session_last_time - current_session_start).total_seconds()
        if sess_dur < 0:
            sess_dur += 86400
        sessions.append((current_session_start, current_session_last_time, current_session_apps, sess_dur))

    # Compute metrics
    unlock_count = len(unlocks)

    # Time between unlocks
    gaps_between_unlocks = []
    for i in range(1, len(unlocks)):
        gap = (unlocks[i] - unlocks[i - 1]).total_seconds()
        if gap < 0:
            gap += 86400
        gaps_between_unlocks.append(gap)
    avg_gap = sum(gaps_between_unlocks) / len(gaps_between_unlocks) if gaps_between_unlocks else 0

    # Session stats
    session_durations = [s[3] for s in sessions]
    avg_session = sum(session_durations) / len(session_durations) if session_durations else 0
    quick_checks = sum(1 for d in session_durations if d <= 30)

    # App switches per session
    switches_per_session = []
    for s in sessions:
        unique_apps = len(set(s[2]))
        switches_per_session.append(unique_apps)
    avg_switches = sum(switches_per_session) / len(switches_per_session) if switches_per_session else 0

    # Social media stats
    social_time = sum(
        stats["total_seconds"]
        for app, stats in app_stats.items()
        if app in SOCIAL_MEDIA_APPS
    )
    total_app_time = sum(
        stats["total_seconds"]
        for app, stats in app_stats.items()
        if app != "com.android.launcher3"
    )
    social_ratio = (social_time / total_app_time * 100) if total_app_time > 0 else 0

    # First / last use
    first_use = min(all_times).strftime("%H:%M") if all_times else "?"
    last_use = max(all_times).strftime("%H:%M") if all_times else "?"

    # Total screen time (excluding launcher)
    total_screen_min = total_app_time // 60
    total_screen_sec = total_app_time % 60

    def fmt_duration(seconds):
        m, s = int(seconds) // 60, int(seconds) % 60
        return f"{m}m{s:02d}s"

    # Build summary
    summary_lines = [
        f"# === DIGITAL SOBRIETY REPORT ===",
        f"#",
        f"# Total screen time: {fmt_duration(total_app_time)}",
        f"# First use: {first_use} | Last use: {last_use}",
        f"#",
        f"# --- Unlock behavior ---",
        f"# Unlock count: {unlock_count}",
        f"# Avg time between unlocks: {fmt_duration(avg_gap)}",
        f"# Quick checks (<=30s): {quick_checks}/{len(sessions)} sessions",
        f"#",
        f"# --- Session patterns ---",
        f"# Total sessions: {len(sessions)}",
        f"# Avg session duration: {fmt_duration(avg_session)}",
        f"# Avg app switches/session: {avg_switches:.1f}",
        f"#",
        f"# --- Social media ---",
        f"# Social media time: {fmt_duration(social_time)} ({social_ratio:.0f}% of screen time)",
    ]

    # List social apps used
    social_used = {app: stats for app, stats in app_stats.items() if app in SOCIAL_MEDIA_APPS and stats["count"] > 0}
    for app, stats in sorted(social_used.items(), key=lambda x: x[1]["total_seconds"], reverse=True):
        summary_lines.append(f"#   {friendly(app)}: {stats['count']}x, {fmt_duration(stats['total_seconds'])}  ({app})")

    summary_lines += [
        f"#",
        f"# --- App usage (by time) ---",
    ]
    for app, stats in sorted(app_stats.items(), key=lambda x: x[1]["total_seconds"], reverse=True):
        if app == "com.android.launcher3":
            continue
        summary_lines.append(f"# {friendly(app)};{stats['count']}x;{fmt_duration(stats['total_seconds'])}  ({app})")
    summary_lines.append("#")

    # Write output
    out_path = Path(enc_path).with_suffix(".csv")
    output = "\n".join(summary_lines + [header] + processed_rows) + "\n"
    out_path.write_text(output)
    print(f"Decrypted: {out_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Decrypt Data Interview .enc files")
    parser.add_argument("--key", required=True, help="Path to RSA private key (PEM)")
    parser.add_argument("--input", required=True, help="Path to .enc file")
    args = parser.parse_args()
    decrypt(args.key, args.input)
