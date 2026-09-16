# VpnGate-Android

An Android client for browsing and exporting the public **VPN Gate** server list, with English and Persian (RTL) support.

## Features

- **Live server list** – Pulls the latest servers directly from vpngate.net and parses each OpenVPN config.
- **Offline cache** – Every fetch is saved locally (accumulated across refreshes, deduplicated by IP), so the list stays available even without an internet connection. A banner shows when the app is displaying saved data offline.
- **Search & sort** – Filter the list by keyword and sort by speed (ping), score, or country.
- **Filter by country** – Pick a country from a dialog and view only servers from that country.
- **Protocol badge** – Each server shows its OpenVPN protocol type: `OVPN UDP`, `OVPN TCP`, or `OVPN UDP/TCP`.
- **Ping measurement** – Live ping/response time shown for each server.
- **Favorites** – Star servers and view them in a dedicated Favorites screen.
- **Export `.ovpn` files** – Export configs to `Download/VPNGate` (MediaStore on Android 10+) so they can be imported into OpenVPN apps.
- **Selection mode** – Multi-select servers to delete in bulk, select all, or export several configs at once.
- **Manage saved data** – Delete an individual server from its detail page, or clear the whole saved list from Settings.
- **Bilingual UI** – Switch between English and Persian (Farsi, RTL) right inside the app.
- **Material 3** – Modern Compose-based UI with a clean, compact server card layout.

## App languages

| English | فارسی |
| ------- | ---- |
| [README.md](README.md) | [README.fa.md](README.fa.md) |

## Requirements / Build

```bash
git clone https://github.com/hjfisher/VpnGate-Android.git
cd VpnGate-Android
./gradlew assembleRelease
```

- minSdk 26 · targetSdk 34 · compileSdk 34
- Gradle 8.4 · AGP 8.2.2 · Kotlin 1.9.22 · Jetpack Compose (BOM 2024.02.00)

## Releases

Signed release and debug APKs are published automatically on each version tag:

https://github.com/hjfisher/VpnGate-Android/releases

## License

For personal and educational use.