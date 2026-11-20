// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.
// Praha Browser - Privacy-hardened default preferences
// These override the Firefox defaults in firefox.js

// === Tracking Protection: ETP Strict by default ===
pref("browser.contentblocking.enabled", true);
pref("browser.contentblocking.features.strict", "tp,tpPrivate,cookieBehavior5,cookieBehaviorPBM5,cryptoTP,fp,stp,emailTP,emailTPPrivate,-consentmanagerSkip,-consentmanagerSkipPrivate,lvl2,rp,rpTop,qps,qpsPBM,fpp,fppPrivate,btp,lna");
pref("browser.contentblocking.defaultMode", "strict");
pref("browser.contentblocking.report.toggle.ui.enabled", true);

// === Resist Fingerprinting ===
pref("privacy.resistFingerprinting", true);
pref("privacy.resistFingerprinting.pbmode", true);
pref("privacy.resistFingerprinting.letterboxing", true);

// === Cookies: Block third-party cookies, Total Cookie Protection ===
pref("network.cookie.cookieBehavior", 5);
pref("network.cookie.cookieBehavior.pbmode", 5);
pref("network.cookie.lifetimePolicy", 0); // 0 = accept until expired

// === Referrer: Strip on cross-site navigation ===
pref("network.http.referer.defaultPolicy", 1); // 1 = send only on same-origin
pref("network.http.referer.default.sameOriginPolicy", 1);
pref("network.http.referer.trimmingPolicy", 2); // 2 = trim to origin

// === URL tracking parameter stripping ===
pref("privacy.query_stripping.enabled", true);
pref("privacy.query_stripping.strip_on_share.enabled", true);
pref("privacy.query_stripping.pbmode.enabled", true);

// === WebRTC: Disable to prevent IP leaks ===
pref("media.peerconnection.enabled", false);
pref("media.peerconnection.use_document_ice_servers", false);
pref("media.peerconnection.ice.no_host", true);
pref("media.peerconnection.ice.default_address_only", true);

// === DNS-over-HTTPS ===
pref("network.doh.enabled", true);
pref("network.doh.mode", 2); // 2 = TRR Mode: Default (use DoH when available)
pref("network.doh.bootstrapAddress", "1.1.1.1");

// === HTTPS-only mode ===
pref("security.mixed_content.block_display_content", true);
pref("security.mixed_content.block_display_content.notify", true);

// === Telemetry: Fully disabled ===
pref("toolkit.telemetry.enabled", false);
pref("toolkit.telemetry.unified", false);
pref("toolkit.telemetry.server", "");
pref("toolkit.telemetry.shutdownPingSender.enabled", false);
pref("toolkit.telemetry.newProfilePing.enabled", false);
pref("toolkit.telemetry.bhrPing.enabled", false);
pref("toolkit.telemetry.firstShutdownPing.enabled", false);

// === Crash reporter: Disabled ===
pref("breakpad.reportURL", "");
pref("breakpad.reportURL.product", "");

// === Health report: Disabled ===
pref("datareporting.healthreport.uploadEnabled", false);
pref("datareporting.healthreport.service.enabled", false);
pref("datareporting.healthreport.service.firstRun", false);

// === Mozilla services: Disabled ===
pref("services.sync.enabled", false);
pref("services.sync.prefs.sync.browser.contentblocking.features.strict", false);

// === Safe Browsing: Disabled (privacy) ===
pref("browser.safebrowsing.enabled", false);
pref("browser.safebrowsing.malware.enabled", false);
pref("browser.safebrowsing.phishing.enabled", false);

// === Studies / Experiments: Disabled ===
pref("experiments.supported", false);
pref("experiments.enabled", false);
pref("app.normandy.enabled", false);
pref("app.normandy.api_url", "");

// === Pocket: Disabled ===
pref("extensions.pocket.enabled", false);
pref("extensions.pocket.api.enabled", false);
pref("extensions.pocket.oAuthConsumerKey", "");
pref("extensions.pocket.oAuthServer", "");

// === Firefox Accounts / FxA: Disabled ===
pref("identity.fxaccounts.enabled", false);

// === Search: Privacy-respecting defaults ===
pref("browser.search.region", "US");
pref("browser.search.defaultenginename", "DuckDuckGo");

// === New Tab: Blank instead of Mozilla content ===
pref("browser.newtabpage.enabled", false);
pref("browser.newtab.url", "about:blank");

// === Home: Blank ===
pref("browser.startup.homepage", "about:blank");
pref("browser.startup.homepage_override.mstone", "");

// === Delete cookies and site data on quit ===
pref("privacy.clearOnShutdown.cookies", true);
pref("privacy.clearOnShutdown.cache", true);
pref("privacy.clearOnShutdown.history", true);
pref("privacy.clearOnShutdown.offlineApps", true);
pref("privacy.clearOnShutdown.sessions", true);
pref("privacy.clearOnShutdown.siteSettings", true);

// === Auto-fill passwords disabled ===
pref("signon.rememberSignons", false);

// === Form autofill disabled ===
pref("browser.formfill.enable", false);

// === Tracking protection in all windows (not just private) ===
pref("privacy.trackingprotection.enabled", true);
pref("privacy.trackingprotection.pbmode.enabled", true);
pref("privacy.trackingprotection.harmfuladdon.enabled", true);
pref("privacy.trackingprotection.fingerprinting.enabled", true);
pref("privacy.trackingprotection.cryptomining.enabled", true);
pref("privacy.trackingprotection.socialtracking.enabled", true);

// === Disable webRTC IP handling policies that leak IPs ===
pref("media.peerconnection.ice.obfuscateCandidates", true);

// === Disable canvas fingerprinting ===
pref("privacy.canvasBlockingEnabled", true);

// === Disable audio fingerprinting ===
pref("privacy.audioBlockingEnabled", true);
