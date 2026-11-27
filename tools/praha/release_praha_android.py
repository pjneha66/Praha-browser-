#!/usr/bin/env python3
"""Build, sign, and verify the Praha GeckoView upgrade APK."""

import argparse
import hashlib
import json
import os
import re
import subprocess
import tempfile
from pathlib import Path
from zipfile import ZipFile

from make_webapp_apk import AndroidSDKLocator

ROOT = Path(__file__).resolve().parents[2]
PACKAGE = "org.praha.browser"
LAST_GECKOVIEW_VERSION_CODE = 2026201701
MAX_VERSION_CODE = 2100000000
SIGNER = "7edb81223b5fb190e630167c17f5483800e1083419a064c34c3a5896125c8dcc"
GECKO_LIB = "lib/arm64-v8a/libxul.so"
VERSION_NAME = re.compile(r"[0-9]+(?:\.[0-9]+){2,3}")


def read_command(*command):
    return subprocess.check_output(command, text=True)


def sdk_tool(sdk, name):
    path = Path(sdk.build_tools_dir or "") / name
    if not sdk.build_tools_dir or not path.is_file():
        raise ValueError(f"Android SDK build tool {name} is missing")
    return path


def verify_key(sdk, keystore, alias):
    if not sdk.keytool or not keystore.is_file():
        raise ValueError("keytool and the existing v2.0.17 release keystore are required")
    if not os.environ.get("PRAHA_ANDROID_KEYSTORE_PASSWORD"):
        raise ValueError("PRAHA_ANDROID_KEYSTORE_PASSWORD is required")
    certificate = subprocess.check_output([
        sdk.keytool, "-exportcert", "-keystore", keystore, "-alias", alias,
        "-storepass:env", "PRAHA_ANDROID_KEYSTORE_PASSWORD",
    ])
    if hashlib.sha256(certificate).hexdigest() != SIGNER:
        raise ValueError("The keystore certificate does not match Praha Browser v2.0.17")


def verify_identity(aapt, apk, version_name, version_code):
    badging = read_command(aapt, "dump", "badging", apk)
    header = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'", badging, re.M)
    if not header or header.groups() != (PACKAGE, str(version_code), version_name):
        raise ValueError(f"Unexpected APK identity: {badging.splitlines()[0] if badging else apk}")
    manifest = read_command(aapt, "dump", "xmltree", apk, "AndroidManifest.xml")
    min_sdk = re.search(r"^sdkVersion:'(\d+)'", badging, re.M)
    if not min_sdk or int(min_sdk[1]) > 26:
        raise ValueError("The upgrade must support Android API 26")
    if "android:sharedUserId(" in manifest:
        raise ValueError("The v2.0.17 upgrade must have no sharedUserId")
    with ZipFile(apk) as archive:
        if GECKO_LIB not in archive.namelist():
            raise ValueError("The APK does not contain the ARM64 GeckoView engine")


def verify_signer(apksigner, apk):
    details = read_command(apksigner, "verify", "--verbose", "--print-certs", apk)
    digests = set(re.findall(r"certificate SHA-256 digest: ([0-9a-f]+)", details, re.I))
    if digests != {SIGNER}:
        raise ValueError("The APK signing certificate does not match Praha Browser v2.0.17")
    if "Verified using v2 scheme (APK Signature Scheme v2): true" not in details:
        raise ValueError("The APK must have a valid v2 signature for Android 8.0 and later")


def build_apk(version_name, version_code):
    env = os.environ.copy()
    env.pop("PRAHA_ANDROID_KEYSTORE_PASSWORD", None)
    env.pop("PRAHA_ANDROID_KEY_PASSWORD", None)
    env["MOZCONFIG"] = env.get("MOZCONFIG") or str(ROOT / "tools/praha/android.mozconfig")
    subprocess.run(["./mach", "build"], cwd=ROOT, env=env, check=True)
    subprocess.run([
        "./mach", "gradle", "fenix:assembleRelease", "-PdisableDebugSigning",
        f"-PprahaVersionName={version_name}", f"-PprahaVersionCode={version_code}",
    ], cwd=ROOT, env=env, check=True)
    environment = subprocess.check_output(["./mach", "environment", "--format", "json"], cwd=ROOT, env=env, text=True)
    objdir = Path(json.loads(environment)["topobjdir"])
    outputs = objdir / "gradle/build/mobile/android/fenix/app/outputs/apk/release"
    candidates = list(outputs.glob("*arm64-v8a*unsigned.apk"))
    if len(candidates) != 1:
        raise ValueError(f"Expected one unsigned ARM64 Fenix release APK, found {len(candidates)}")
    return candidates[0]


def release(args):
    sdk = AndroidSDKLocator()
    verify_key(sdk, args.keystore, args.key_alias)
    aapt = sdk_tool(sdk, "aapt")
    apksigner = sdk_tool(sdk, "apksigner")
    apk = args.apk or build_apk(args.version_name, args.version_code)
    verify_identity(aapt, apk, args.version_name, args.version_code)

    output = args.output_dir
    output.mkdir(parents=True, exist_ok=True)
    filename = f"praha-browser-android-v{args.version_name}-release.apk"
    final = output / filename
    if final.exists():
        raise ValueError(f"Release APK already exists: {final}")
    manifest_path = output / "release-manifest.json"
    manifest = json.loads(manifest_path.read_text()) if manifest_path.exists() else {}
    sums_path = output / "SHA256SUMS.txt"
    sums = sums_path.read_text() if sums_path.exists() else ""

    with tempfile.TemporaryDirectory(dir=output) as directory:
        signed = Path(directory) / filename
        key_password = "PRAHA_ANDROID_KEY_PASSWORD" if os.environ.get("PRAHA_ANDROID_KEY_PASSWORD") else "PRAHA_ANDROID_KEYSTORE_PASSWORD"
        subprocess.run([
            apksigner, "sign", "--ks", args.keystore, "--ks-key-alias", args.key_alias,
            "--ks-pass", "env:PRAHA_ANDROID_KEYSTORE_PASSWORD", "--key-pass", f"env:{key_password}",
            "--v4-signing-enabled", "false", "--out", signed, apk,
        ], check=True)
        verify_signer(apksigner, signed)
        with signed.open("rb") as source:
            digest = hashlib.file_digest(source, "sha256").hexdigest()
        size = signed.stat().st_size
        os.link(signed, final)

    manifest[filename] = {
        "name": "Praha Browser", "package_id": PACKAGE, "engine": "GeckoView",
        "version_name": args.version_name, "version_code": args.version_code,
        "sha256": digest, "size_bytes": size, "signer_sha256": SIGNER,
    }
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
    sums_path.write_text(f"{sums.rstrip()}\n{digest}  {filename}\n".lstrip())
    print(f"Verified GeckoView upgrade APK: {final}")


def add_arguments(parser):
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--version-code", type=int, required=True)
    parser.add_argument("--keystore", type=Path, required=True)
    parser.add_argument("--key-alias", required=True)
    parser.add_argument("--apk", type=Path, help="Use an existing unsigned ARM64 Fenix release APK")
    parser.add_argument("--output-dir", type=Path, default=ROOT / "dist/releases")


def run_release(args):
    if not VERSION_NAME.fullmatch(args.version_name):
        raise SystemExit("Provide a numeric version name, such as 2.0.17.3")
    version = tuple(map(int, args.version_name.split(".")))
    if version + (0,) * (4 - len(version)) <= (2, 0, 17, 0):
        raise SystemExit("Provide a version name newer than 2.0.17")
    if not LAST_GECKOVIEW_VERSION_CODE < args.version_code <= MAX_VERSION_CODE:
        raise SystemExit(f"versionCode must be between {LAST_GECKOVIEW_VERSION_CODE + 1} and {MAX_VERSION_CODE}")
    try:
        release(args)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        raise SystemExit(str(error)) from error


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    add_arguments(parser)
    run_release(parser.parse_args())
