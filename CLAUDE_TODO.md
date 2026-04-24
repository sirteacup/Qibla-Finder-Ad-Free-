# Claude TODO — Qibla Finder Play Store Launch

This file tracks remaining work to get the app onto Google Play Store.
Read this at the start of every session to get up to speed.

---

## App Info
- **App name:** Qibla Finder (Ad-Free)
- **Package:** com.znazmul.qiblafinder
- **GitHub:** https://github.com/sirteacup/Qibla-Finder-Ad-Free-
- **Privacy policy (once Pages is live):** https://sirteacup.github.io/Qibla-Finder-Ad-Free-/privacy-policy.html
- **Dev email:** zaiyannazm@gmail.com

---

## TODO

### Code / Repo
- [ ] Verify release build compiles cleanly after package rename (`./gradlew bundleRelease`)

### Privacy Policy Hosting
- [ ] User enables GitHub Pages on the repo (Settings → Pages → master / root → Save)
- [ ] Confirm URL is live: https://sirteacup.github.io/Qibla-Finder-Ad-Free-/privacy-policy.html

### Google Play Console Setup
- [ ] Create Play Console developer account (if not already done) — one-time $25 fee
- [ ] Create new app in Play Console with package `com.znazmul.qiblafinder`
- [ ] Upload release AAB (`./gradlew bundleRelease` → `app/build/outputs/bundle/release/`)
- [ ] Fill in store listing:
  - [ ] App title: "Qibla Finder - Ad Free"
  - [ ] Short description (80 chars max)
  - [ ] Full description
  - [ ] Category: Tools or Navigation
- [ ] Upload store assets:
  - [ ] Feature graphic (1024x500px)
  - [ ] Minimum 2 screenshots (phone)
- [ ] Complete IARC content rating questionnaire
- [ ] Paste privacy policy URL into Play Console
- [ ] Set price to Free
- [ ] Submit for review

### Nice to Have (not blocking launch)
- [ ] Add real unit tests for `calculateQiblaBearing()` in MainActivity.kt
- [ ] Add a sensor unavailable error state (if device has no rotation vector sensor)

---

## Completed
- [x] Package renamed from com.example.qiblafinder → com.znazmul.qiblafinder
- [x] Git repo initialised and pushed to GitHub
- [x] privacy-policy.html exists at repo root (needs hosting — see above)
- [x] Release signing keystore configured (keystore.properties excluded from git)
- [x] R8 minification + ProGuard rules configured
- [x] All launcher icon densities present including adaptive icon
- [x] Compass gimbal lock fix (AXIS_X/AXIS_Z remap for vertical hold)
- [x] Compass responsiveness improved (SENSOR_DELAY_GAME, filter alpha 0.2)
- [x] Flat/vertical orientation switching at 25° pitch threshold
