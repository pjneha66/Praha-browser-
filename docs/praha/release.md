# Qualifying a Praha release

## Inputs and licensing

Record the exact source revision, engine version, toolchain versions, target, mozconfig, and artifact checksums. Retain copyright headers, `LICENSE`, `other-licenses/`, and the notices exposed by `about:license`. Branding work must not erase attribution or upstream protocol identifiers needed for interoperability.

The desktop build uses the unmodified **uBlock Origin** signed XPI, not a renamed or repacked extension. The release URL and SHA-256 are pinned in `browser/app/distribution/ublock.py`. See the [1.76.6 release and source](https://github.com/gorhill/uBlock/releases/tag/1.76.6). For an upgrade, review the source, release asset, permissions, minimum browser version, signature and bundled filter changes; update the pin and digest together. Do not promote a build that silently omits the XPI.

Before distributing binaries, publish the matching modified covered source and build instructions, including corresponding source, dependencies/filter notices and GPL license for the bundled extension. Supply the source-access information with each binary; a moving branch or a link to the latest upstream version is not sufficient. Review third-party licenses and trademarks separately. No publisher identity or rights to upstream trademarks are implied.

## Test the actual packages

1. Run `python3 tools/praha/test_inputs.py`. On provisioned Linux runners the manually dispatched **Praha checks** workflow builds desktop and Android. The `praha-desktop` runner needs a full desktop bootstrap; `praha-android` needs the full Android bootstrap and JDK/SDK paths. Do not attach release secrets to these runners.
2. Build and package desktop (`./mach package`). Run the Marionette smoke test from the workflow. Also extract the **shipping package**, run `praha --version`, then use a new disposable profile. Check first launch, `about:branding`, `about:addons`, settings, about dialog, UA and application icon. Test light/dark/private windows, high contrast, keyboard access and narrow layouts. Inspect the actual installer/APK and each supported locale, not just en-US source strings.
3. Verify Strict is selected, cookies are partitioned, HTTPS-only is on, and uBlock Origin is **active without a network install**. With a local test filter, confirm blocking, then disable or uninstall and restart: the user's choice must persist. Install a separate signed WebExtension; review its permission prompt. Change ETP to Standard and verify it survives restart.
4. Verify the chosen DoH resolver is used and test native fallback (TRR mode 2 is not fail-closed). On a monitored test network check for telemetry, account, sync, recommendation and update traffic during first launch, idle, shutdown and restart. A pref's presence is not evidence that a request was suppressed.
5. Install the actual Android APK on a device/emulator. Inspect its application ID, launcher, first-run, privacy settings, permission dialog, widget, add-ons and upgrade behavior. Repeat the network checks. Do not claim Android uBlock coverage from desktop tests.
6. The updater is disabled in the desktop configuration. Until a verified update channel exists, document a manual replacement procedure and security notification channel. Do not point the updater at a placeholder domain.
7. On isolated release hosts, sign macOS bundles with the project's Developer ID and notarize; sign and timestamp Windows binaries/installers with the publisher's certificate; sign release APKs with the protected Android release key. Verify with `spctl`/`codesign`, `signtool verify /pa` and `apksigner verify --print-certs` respectively. Publish only after checks, source availability and signer identity have been verified. Debug APKs are not release artifacts.

## Security maintenance

Track the engine revision against the supported upstream ESR and advisories. Assign an owner to triage each upstream security release, record the new base, review conflicting patch hunks, and compare upstream preference definitions and network endpoints. Re-run packaging, fresh-profile and upgrade tests after each integration. Review extension/filter releases independently of the engine. Publish an advisory and the matching signed builds; a written checklist alone is not an update service.
