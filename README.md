# Bloki

On-device DNS-based ad blocker for Android. Bloki intercepts DNS queries via a local VPN tunnel, blocks domains found on community blocklists, and forwards allowed queries over DNS-over-HTTPS (Cloudflare).

No root required. No data leaves your device except DNS lookups to Cloudflare (1.1.1.1).

## How it works

```
App DNS query → VPN tunnel (port 53)
  → Blocklist check (HashSet lookup with subdomain matching)
    → Blocked: return NXDOMAIN
    → Allowed: forward via DoH (Cloudflare 1.1.1.1) → return response
```

- DNS queries are intercepted using Android's `VpnService`
- Blocked domains get an NXDOMAIN response (no network request made)
- Allowed domains are resolved over encrypted DNS-over-HTTPS (RFC 8484)
- All processing happens on-device

## Features

- One-tap VPN toggle to enable/disable ad blocking
- DNS-over-HTTPS for privacy (Cloudflare 1.1.1.1)
- Multiple blocklist sources (Steven Black, AdAway, Peter Lowe)
- Automatic blocklist download on first launch
- Whitelist for domains you want to allow
- Statistics dashboard (blocked/allowed counts, top blocked domains)
- Minimal battery impact (only intercepts DNS, not all traffic)

## Building

Requirements:
- JDK 17+
- Android SDK with API 36

```bash
git clone https://github.com/YOUR_USER/bloki.git
cd bloki
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Tech stack

- Kotlin, Min API 24 (Android 7.0)
- Jetpack Compose + Material 3
- Room database (stats, whitelist, blocklist metadata)
- OkHttp (DoH client)
- Kotlin Coroutines

## Blocklist sources

| Source | Description |
|--------|-------------|
| [Steven Black](https://github.com/StevenBlack/hosts) | Unified hosts file with ad + malware domains |
| [AdAway](https://adaway.org/hosts.txt) | Default AdAway blocklist |
| [Peter Lowe](https://pgl.yoyo.org/adservers/) | Ad and tracking server list |

## Privacy

- No accounts, no analytics, no telemetry
- DNS queries are forwarded to Cloudflare (1.1.1.1) over HTTPS
- Statistics are stored locally on your device only
- No data is collected or shared with anyone

## License

GPLv3 — see [LICENSE](LICENSE) for details.
