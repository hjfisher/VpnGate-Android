# VpnGate-Android

نرمافزار اندرویدی برای دریافت و نمایش لیست سرورهای VPN Gate با پشتیبانی از فارسی و انگلیسی.
An Android client for browsing and exporting VPN Gate server lists, with English and Persian (RTL) support.

## امکانات / Features

- دریافت لیست سرورها از vpnGate.net و ذخیرهٔ خودکار آن برای استفادهٔ آفلاین / Fetches server list from vpngate.net and caches it for offline use
- جستجو، مرتبسازی و فیلتر بر اساس کشور / Search, sort, and filter by country
- لیست علاقهمندیها / Favorites
- خروجی فایل `.ovpn` (saved to `Download/VPNGate`) / Export `.ovpn` configs
- حذف سرور و پاکسازی حافظه / Delete servers and clear saved data
- رایگان و بدون تبلیغات / Free, no ads

## ساخت / Build

```bash
git clone https://github.com/hjfisher/VpnGate-Android.git
cd VpnGate-Android
./gradlew assembleRelease
```

- minSdk 26 · targetSdk 34 · Gradle 8.4 · AGP 8.2.2

## انتشار / Releases

APKهای امضاشده در بخش Releases منتشر میشوند.
Signed APKs are published to the Releases section: https://github.com/hjfisher/VpnGate-Android/releases

## لایسنس / License

استفادهٔ شخصی و آموزشی.
For personal and educational use.