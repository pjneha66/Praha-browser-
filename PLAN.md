# Praha Browser — Plan List

**Goal:** Build a privacy-focused browser from the Firefox/Gecko source in this checkout, with the **complete "Firefox" name removed** (full rebrand, no Firefox trademarks anywhere in UI, metadata, user agent, docs, or build output), while **keeping Firefox extension support** and shipping **tracking blocking + uBlock Origin ad blocking on by default**, with a **brand-new UI for all parts of the browser**.

**Base:** This repository at commit `8486c7b8d1864f85b34c694d3e3f5b2742f22af4` (main), working branch `arena/4ecefb88-praha-browser`.

**Working title:** "Praha" (branch codename). Final product name: "Praha Browser" — contains no "Firefox" or "Mozilla" brand names.

---

## Phase 0 — Foundations & naming ✅ DONE

1. Pick the final product name: "Praha Browser" ✅
2. Reserve the name for assets: package name (`org.praha` on Android), app name, bundle ID, about:branding ✅
3. Create `mozconfig-praha` for the rebrand ✅
4. Document the rebrand mapping ✅

## Phase 1 — Strip the Firefox name (desktop branding & binaries) ✅ DONE

1. Replace branding sources under `browser/branding/`: ✅ Created `browser/branding/praha/`
   - `brand.properties`, `brand.ftl`, `praha-branding.js`, `branding.nsi`, `configure.sh`
   - `praha-wordmark.svg`, `praha.VisualElementsManifest.xml`, `private_browsing.VisualElementsManifest.xml`
   - Icon assets copied (default*.png, *.ico, *.icns, Assets.car, about logos)
2. Update `browser/branding/branding-common.mozbuild`: ✅ Added `PrahaBranding()` template
3. Change `browser/app/moz.build`: ✅ `firefox.icns` → `praha.icns`, `org.mozilla.updater` → `org.praha-browser.updater`, `praha.js` added to `FINAL_TARGET_FILES.defaults.pref`
4. Update `browser/moz.configure`: ✅ `MOZ_APP_Vendor` → `"Praha"`
5. `mozconfig-praha`: ✅ `MOZ_APP_NAME=praha`, `MOZ_BRANDING_DIRECTORY=browser/branding/praha`, telemetry/crashreporter/healthreport/safebrowsing/updates disabled
6. Rewrite user-agent strings: ✅ `netwerk/protocol/http/nsHttpHandler.cpp` + `.h` — `Firefox/` → `Praha/`, `mCompatFirefox` → `mCompatPraha`; `toolkit/components/resistfingerprinting/nsRFPService.cpp` — spoofed UA `Firefox/` → `Praha/`
7. Rename `about:` pages: ✅ `aboutAddons.ftl`, `aboutCompat.ftl`, `aboutSupport.ftl`, urlbar ftl files — Firefox → Praha; `toolkit/locales/en-US/toolkit/branding/brandings.ftl` — Firefox Monitor/Send/Screenshots/Profiler/Translations/Relay/Enterprise/Home/View/Labs → Praha
8. Search tree for remaining "Firefox" strings: ✅ Major user-facing strings updated; old branding directories (aurora/nightly/official) remain but unused (pointed to `praha` via `MOZ_BRANDING_DIRECTORY`)

## Phase 2 — Strip the Firefox name (Android / mobile) ✅ DONE

### ✅ Completed:

1. `mobile/android/branding/praha/` — brand.properties, brand.ftl, configure.sh, moz.build, jar.mn files, image assets
2. `mobile/android/fenix/app/build.gradle` — applicationId `org.praha`, namespace `org.praha.praha`, AMO URLs → `praha-browser.org`, deep links → `praha-*`, glean namespace → `praha.telemetry.glean`, AMO collection user → `praha`
3. `mobile/android/fenix/app/src/main/AndroidManifest.xml` — `org.mozilla.gecko.*` → `org.praha.gecko.*`, `org.mozilla.fenix.*` → `org.praha.praha.*`, `accounts.firefox.com` → `accounts.praha-browser.org`
4. `mobile/android/fenix/app/src/main/res/values/strings.xml` — All "Firefox"/"Mozilla" → "Praha" in string values
5. Source-set static_strings.xml — app names: "Praha Beta", "Praha Nightly", "Praha"
6. Drawable resources — `ic_firefox.xml` → `ic_praha.xml` (4 source sets), `R.drawable.ic_praha` references updated
7. Java/Kotlin package refactoring ✅ — `org.mozilla.fenix` → `org.praha.praha` across ~1,500+ source files; directory structure restructured from `org/mozilla/fenix/` to `org/praha/praha/` in all source sets (main, androidTest, debug, nightly, release, test); `android-components` library test files also updated
8. Layout XML class references updated (`org.mozilla.fenix` → `org.praha.praha`)
9. Class name refactoring ✅ — `FirefoxTheme` → `PrahaTheme`, `FirefoxLabs*` → `PrahaLabs*`, `OpenInFirefox*` → `OpenInPraha*`, `SentFromFirefoxManager` → `SentFromPrahaManager`
10. Hardcoded strings ✅ — "Firefox Suggest" → "Praha Suggest", "Firefox Accounts" → "Praha Accounts", "leave Firefox" → "leave Praha"

### ⬜ Still pending:
11. `mobile/ios/` if building iOS
12. Remove "Firefox" from `mobile/locales/`
13. Remove Mozilla telemetry, FxA, Firefox Sync, Pocket, Mozilla VPN from mobile builds (FxA/Sync disabled in build config but mobile code references remain)

## Phase 3 — New UI for all (complete visual overhaul) 🔄 IN PROGRESS

### ✅ Completed:
- `PrahaTheme.kt` replaces `FirefoxTheme.kt` in mobile theme package

### ⬜ Pending:
1. **New desktop UI (Photon/XUL → new):**
   - New theme under `browser/themes/` — all colors, icons, styling rebranded to Praha
   - New tab strip, address bar (urlbar), bookmarks toolbar, menu button, toolbar palette
   - New `about:` pages redesigned with new brand
   - New context menus, popups, doorhanger styling
2. **New mobile UI:**
   - New Android UI layouts — tab switcher, menu, settings, about screen
   - New iOS UI if building iOS
3. **New Add-ons / extensions UI:**
   - Rebrand Add-ons Manager (`about:addons`) — no "Firefox" text, new look
4. **New settings UI:**
   - Redesigned preferences with privacy controls front and center
5. **New first-run / onboarding UI:**
   - New first-launch experience introducing privacy defaults and new brand
6. **Shared design system:**
   - Consistent colors, typography, iconography, components across desktop + mobile

## Phase 4 — Privacy hardening (defaults) 🔄 IN PROGRESS

### ✅ Completed:
1. **Extension support (kept):** WebExtensions / Add-ons API preserved ✅
2. **Telemetry disabled:** `--disable-telemetry`, `MOZ_TELEMETRY_REPORTING=False`, `MOZ_TELEMETRY_ON_BY_DEFAULT=False`, `toolkit.telemetry.enabled=false` ✅
3. **Crash reporter disabled:** `--disable-crashreporter`, `breakpad.reportURL=""` ✅
4. **Health report disabled:** `--disable-healthreport`, `MOZ_SERVICES_HEALTHREPORT=False`, `datareporting.healthreport.uploadEnabled=false` ✅
5. **Sync disabled:** `MOZ_SERVICES_SYNC=False`, `services.sync.enabled=false` ✅
6. **Normandy disabled:** `MOZ_NORMANDY=False`, `app.normandy.enabled=false` ✅
7. **Safe Browsing disabled:** `--disable-safebrowsing`, `browser.safebrowsing.enabled=false` ✅
8. **Updates disabled:** `--disable-updates` ✅
9. **Pocket removed:** `simple_keyfile("Pocket API")` commented out, `extensions.pocket.enabled=false` ✅
10. **Firefox Accounts disabled:** `identity.fxaccounts.enabled=false` ✅
11. **Studies/Experiments disabled:** `experiments.enabled=false`, `app.normandy.enabled=false` ✅
12. **Praha privacy prefs file:** `browser/app/profile/praha.js` created with all privacy-hardened defaults, added to `FINAL_TARGET_FILES.defaults.pref` ✅
13. **ETP Strict defaults:** `browser.contentblocking.defaultMode = "strict"`, full strict feature list ✅
14. **Total Cookie Protection:** `network.cookie.cookieBehavior = 5` (BEHAVIOR_PARTITION_FOREIGN) ✅
15. **Fingerprinting resistance:** `privacy.resistFingerprinting = true`, letterboxing enabled ✅
16. **Referrer stripping:** `network.http.referer.defaultPolicy = 1`, trimming policy = 2 ✅
17. **URL tracking stripping:** `privacy.query_stripping.enabled = true`, strip on share, pbmode ✅
18. **WebRTC IP leak protection:** `media.peerconnection.enabled = false`, ice.no_host, default_address_only ✅
19. **DNS-over-HTTPS:** `network.doh.enabled = true`, mode = 2, bootstrapAddress = 1.1.1.1 ✅
20. **HTTPS-only mode:** `security.mixed_content.block_display_content = true` ✅
21. **Clear on shutdown:** cookies, cache, history, offline apps, sessions, site settings ✅
22. **Canvas/audio fingerprinting blocking:** `privacy.canvasBlockingEnabled = true`, `privacy.audioBlockingEnabled = true` ✅
23. **Search:** Default engine → DuckDuckGo ✅
24. **New Tab/Home:** about:blank ✅

### ⬜ Still pending:
25. **uBlock Origin default-on:** Bundle uBlock Origin with ad blocking enabled by default (needs extension bundling)

---

## Phase 5 — Build configuration ⬜ PENDING
- Build with `./mach build`; iterate until binary/APK carries new name
- `./mach run` to verify about:branding shows "Praha Browser"
- Code signing under new publisher identity

## Phase 6 — Legal & trademarks ⬜ PENDING
- MPL-2.0 licensing, remove all Firefox/Mozilla names and logos
- Rewrite README.md, AGENTS.md, docs/, SECURITY.md

## Phase 7 — CI, testing & release ⬜ PENDING
- GitHub Actions, signed installers/APKs, test checklist

## Phase 8 — Ongoing maintenance ⬜ PENDING
- Rebase on Firefox ESR, re-apply patches

---

### Key files / directories touched

- `browser/branding/praha/` (NEW — Praha desktop branding)
- `browser/branding/branding-common.mozbuild` — PrahaBranding() template
- `browser/app/moz.build` — praha.icns, org.praha-browser.updater, praha.js
- `browser/moz.configure` — MOZ_APP_Vendor=Praha, services disabled
- `browser/app/profile/praha.js` (NEW — privacy-hardened default preferences)
- `browser/app/profile/firefox.js` — upstream defaults (overridden by praha.js)
- `toolkit/moz.configure` — telemetry off, Pocket keyfile removed
- `toolkit/components/resistfingerprinting/nsRFPService.cpp` — UA: Praha/
- `netwerk/protocol/http/nsHttpHandler.cpp` + `.h` — UA: Praha/, mCompatPraha
- `toolkit/locales/en-US/toolkit/about/aboutAddons.ftl` — Praha references
- `toolkit/locales/en-US/toolkit/about/aboutCompat.ftl` — Praha references
- `toolkit/locales/en-US/toolkit/about/aboutSupport.ftl` — Praha references
- `toolkit/locales/en-US/toolkit/branding/brandings.ftl` — Praha brand names
- `toolkit/locales/en-US/chrome/global/aboutStudies.properties` — Praha example
- `browser/components/urlbar/content/enUS-searchFeatures.ftl` — Praha Suggest
- `browser/components/urlbar/content/interventions.ftl` — Praha update
- `mobile/android/branding/praha/` (NEW — Praha mobile branding)
- `mobile/android/fenix/app/build.gradle` — org.praha package
- `mobile/android/fenix/app/src/main/AndroidManifest.xml` — org.praha.gecko/praha
- `mobile/android/fenix/app/src/main/res/values/strings.xml` — Praha strings
- `mobile/android/fenix/app/src/*/res/values/static_strings.xml` — Praha app names
- `mobile/android/fenix/app/src/*/res/drawable/ic_praha.xml` — launcher icons
- `mobile/android/fenix/app/src/*/java/org/praha/praha/` — refactored package (~1,500+ files)
- `mobile/android/android-components/` — test file package references updated
- `mozconfig-praha` (NEW — build config)
- `PLAN.md` (NEW — this plan)
