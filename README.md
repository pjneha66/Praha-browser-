# Praha Browser

Praha is a desktop and Android browser under development on the Gecko engine. The desktop build keeps WebExtensions support and packages a pinned, signed copy of uBlock Origin. Privacy settings remain user-controlled. **These are development builds:** there is no verified, signed release or supported automatic update channel yet.

## Build and try it

On a Linux desktop build host with Gecko's toolchains installed (`./mach bootstrap`, full desktop build), start at the repository root:

```sh
export MOZCONFIG="$PWD/mozconfig-praha"
python3 browser/app/distribution/ublock.py
./mach build
./mach run about:branding
```

The Python step stages a verified third-party build input; it does not install anything into your user profile. For an offline build, place the exact signed XPI in `browser/app/distribution/ublock.xpi` before building. The build verifies the digest again. Do not disable extension signing to use an unsigned archive.

For Android, bootstrap the full Android build environment (including JDK, SDK and NDK), then:

```sh
export MOZCONFIG="$PWD/tools/praha/android.mozconfig"
./mach build
./mach gradle fenix:assembleDebug
```

Android uses a different add-on installation path; desktop extension bundling does not imply that uBlock is bundled in the APK. iOS is not a Praha release target at present.

- [Implementation status and remaining work](PLAN.md)
- [Testing, licensing and release checklist](docs/praha/release.md)
- [Security reporting](SECURITY.md)
- [Licensing and third-party notices](LICENSE)
