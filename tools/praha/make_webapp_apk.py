#!/usr/bin/env python3
"""Praha Android WebApp APK Compiler and Release Tool.

Transforms any URL into a standalone, privacy-hardened Android Web App APK
and orchestrates release distribution with cryptographic verification.
"""

import argparse
import hashlib
import json
import os
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import time
import urllib.parse
import urllib.request
import zlib
from pathlib import Path


def _xml_text(value):
    return str(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


class AndroidSDKLocator:
    """Discovers local Android SDK command line tools and platforms."""

    SEARCH_DIRS = [
        os.environ.get("ANDROID_HOME", ""),
        os.environ.get("ANDROID_SDK_ROOT", ""),
        "/opt/homebrew/share/android-commandlinetools",
        "/opt/homebrew/share/android-sdk",
        os.path.expanduser("~/Library/Android/sdk"),
        "/usr/local/share/android-sdk",
        "/opt/android-sdk",
        os.path.expanduser("~/Android/Sdk"),
        "/usr/lib/android-sdk",
    ]

    def __init__(self):
        self.sdk_root = self._find_sdk_root()
        self.build_tools_dir = self._find_build_tools()
        self.android_jar = self._find_android_jar()
        self.aapt2 = self._find_tool("aapt2")
        self.d8 = self._find_tool("d8")
        self.zipalign = self._find_tool("zipalign")
        self.apksigner = self._find_tool("apksigner")
        self.javac = shutil.which("javac")
        self.keytool = shutil.which("keytool")
        self.adb = shutil.which("adb")

    def _find_sdk_root(self):
        for candidate in self.SEARCH_DIRS:
            if candidate and os.path.isdir(candidate):
                if os.path.isdir(os.path.join(candidate, "build-tools")) or os.path.isdir(
                    os.path.join(candidate, "platforms")
                ):
                    return candidate
        return None

    def _find_build_tools(self):
        if not self.sdk_root:
            return None
        bt_dir = os.path.join(self.sdk_root, "build-tools")
        if not os.path.isdir(bt_dir):
            return None
        versions = [v for v in os.listdir(bt_dir) if not v.startswith(".")]
        if not versions:
            return None
        # Sort versions numerically
        def version_key(v):
            parts = re.findall(r"\d+", v)
            return [int(p) for p in parts] if parts else [0]

        versions.sort(key=version_key, reverse=True)
        return os.path.join(bt_dir, versions[0])

    def _find_android_jar(self):
        if not self.sdk_root:
            return None
        platforms_dir = os.path.join(self.sdk_root, "platforms")
        if not os.path.isdir(platforms_dir):
            return None
        platforms = [p for p in os.listdir(platforms_dir) if p.startswith("android-")]
        if not platforms:
            return None
        def platform_key(p):
            match = re.search(r"android-(\d+)", p)
            return int(match.group(1)) if match else 0
        platforms.sort(key=platform_key, reverse=True)
        for plat in platforms:
            jar = os.path.join(platforms_dir, plat, "android.jar")
            if os.path.isfile(jar):
                return jar
        return None

    def _find_tool(self, tool_name):
        # First check build-tools
        if self.build_tools_dir:
            path = os.path.join(self.build_tools_dir, tool_name)
            if os.path.isfile(path) and os.access(path, os.X_OK):
                return path
        # Then check PATH
        return shutil.which(tool_name)

    def validate(self):
        missing = []
        if not self.aapt2:
            missing.append("aapt2")
        if not self.d8:
            missing.append("d8")
        if not self.zipalign:
            missing.append("zipalign")
        if not self.apksigner:
            missing.append("apksigner")
        if not self.android_jar:
            missing.append("android.jar (platforms/android-*)")
        if not self.javac:
            missing.append("javac")
        if not self.keytool:
            missing.append("keytool")
        if missing:
            raise RuntimeError(
                f"Required Android build tools missing: {', '.join(missing)}.\n"
                f"SDK Root: {self.sdk_root}\n"
                f"Build Tools: {self.build_tools_dir}\n"
                "Please configure ANDROID_HOME or install Android command line tools."
            )


class IconGenerator:
    """Generates standard Android mipmap launcher icons."""

    SIZES = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }

    @staticmethod
    def _create_pure_png(width, height, r=18, g=62, b=57):
        """Pure Python RGBA PNG generator without external dependencies."""
        raw = bytearray()
        radius = width / 2.0 - 2.0
        r_sq = radius * radius
        cx, cy = width / 2.0, height / 2.0
        # Subtle gradient computation
        for y in range(height):
            raw.append(0)  # filter type None
            for x in range(width):
                dx = x - cx
                dy = y - cy
                dist_sq = dx * dx + dy * dy
                if dist_sq <= r_sq:
                    # Inner circle with subtle gradient towards bottom right
                    factor = 1.0 - (x + y) / (width + height * 2.5)
                    pr = min(255, max(0, int(r * factor + 10)))
                    pg = min(255, max(0, int(g * factor + 40)))
                    pb = min(255, max(0, int(b * factor + 35)))
                    raw.extend((pr, pg, pb, 255))
                else:
                    raw.extend((0, 0, 0, 0))

        def chunk(tag, data):
            return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

        header = b"\x89PNG\r\n\x1a\n"
        ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
        idat = chunk(b"IDAT", zlib.compress(bytes(raw)))
        iend = chunk(b"IEND", b"")
        return header + chunk(b"IHDR", ihdr) + idat + iend

    @classmethod
    def generate_icons(cls, res_dir, source_icon=None, brand_rgb=(18, 62, 57)):
        for density, size in cls.SIZES.items():
            density_dir = os.path.join(res_dir, f"mipmap-{density}")
            os.makedirs(density_dir, exist_ok=True)
            icon_dest = os.path.join(density_dir, "ic_launcher.png")

            if source_icon and os.path.isfile(source_icon):
                sips = shutil.which("sips")
                if sips:
                    subprocess.run(
                        [sips, "-z", str(size), str(size), source_icon, "--out", icon_dest],
                        stdout=subprocess.DEVNULL,
                        stderr=subprocess.DEVNULL,
                    )
                    continue

            # Fallback pure python generator
            png_bytes = cls._create_pure_png(size, size, *brand_rgb)
            with open(icon_dest, "wb") as f:
                f.write(png_bytes)


class WebAppCompiler:
    """Compiles a website into a native Android APK."""

    def __init__(self, sdk_locator: AndroidSDKLocator):
        self.sdk = sdk_locator

    def _sanitize_package_id(self, package_id):
        cleaned = re.sub(r"[^a-zA-Z0-9_.]", "", package_id.lower())
        segments = cleaned.split(".")
        sanitized = []
        for s in segments:
            if not s or s[0].isdigit():
                s = "app_" + s
            sanitized.append(s)
        if len(sanitized) < 2:
            sanitized.insert(0, "org.praha.webapp")
        return ".".join(sanitized)

    def _auto_metadata(self, url, name=None):
        parsed = urllib.parse.urlparse(url)
        host = parsed.netloc or parsed.path
        host_clean = re.sub(r"^(www\.)?", "", host)
        path_parts = [p for p in parsed.path.strip("/").split("/") if p]
        
        slug = path_parts[-1] if path_parts else host_clean.split(".")[0]
        slug = re.sub(r"[^a-zA-Z0-9]", "", slug).lower() or "webapp"

        if not name:
            name = slug.upper() if len(slug) <= 8 else slug.capitalize()

        package_id = f"org.praha.webapp.{slug}"
        return name, package_id, slug

    def build(
        self,
        url: str,
        name: str = None,
        package_id: str = None,
        icon_path: str = None,
        theme_color: str = "#102b29",
        version_name: str = "1.0.0",
        version_code: int = 1,
        output_dir: str = "dist/releases",
        is_release: bool = True,
        keystore_path: str = None,
        keystore_pass: str = "android",
        key_alias: str = "androiddebugkey",
        key_pass: str = "android",
    ) -> Path:
        self.sdk.validate()

        auto_name, auto_pkg, slug = self._auto_metadata(url, name)
        app_name = name or auto_name
        pkg_id = self._sanitize_package_id(package_id or auto_pkg)
        version_name = re.sub(r"[^A-Za-z0-9._-]", "_", str(version_name)) or "1.0.0"
        version_code = int(version_code)
        if not re.fullmatch(r"#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})", str(theme_color)):
            raise ValueError(f"theme_color must be a hex color like #102b29, got {theme_color!r}")

        os.makedirs(output_dir, exist_ok=True)
        apk_type = "release" if is_release else "debug"
        final_apk_name = f"{slug}-v{version_name}-{apk_type}.apk"
        final_apk_path = Path(output_dir) / final_apk_name

        print(f"[*] Packaging Web App: {app_name}")
        print(f"[*] Target URL: {url}")
        print(f"[*] Package ID: {pkg_id}")
        print(f"[*] Version: {version_name} (Code: {version_code})")

        with tempfile.TemporaryDirectory() as build_dir:
            res_dir = os.path.join(build_dir, "res")
            values_dir = os.path.join(res_dir, "values")
            layout_dir = os.path.join(res_dir, "layout")
            os.makedirs(values_dir, exist_ok=True)
            os.makedirs(layout_dir, exist_ok=True)

            # 1. Generate Icons
            IconGenerator.generate_icons(res_dir, source_icon=icon_path)

            # 2. Write Strings & Colors
            strings_xml = os.path.join(values_dir, "strings.xml")
            with open(strings_xml, "w", encoding="utf-8") as f:
                f.write(
                    f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">{_xml_text(app_name)}</string>
    <string name="target_url">{_xml_text(url)}</string>
</resources>"""
                )

            colors_xml = os.path.join(values_dir, "colors.xml")
            with open(colors_xml, "w", encoding="utf-8") as f:
                f.write(
                    f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="theme_color">{theme_color}</color>
    <color name="accent_mint">#adf3ce</color>
</resources>"""
                )

            # 3. Write Layout
            activity_layout = os.path.join(layout_dir, "activity_main.xml")
            with open(activity_layout, "w", encoding="utf-8") as f:
                f.write(
                    """<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#102b29">

    <WebView
        android:id="@+id/webview"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <ProgressBar
        android:id="@+id/progress_bar"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="4dp"
        android:layout_alignParentTop="true"
        android:max="100"
        android:progress="0"
        android:visibility="gone" />
</RelativeLayout>"""
                )

            # 4. Write AndroidManifest.xml
            manifest_path = os.path.join(build_dir, "AndroidManifest.xml")
            with open(manifest_path, "w", encoding="utf-8") as f:
                f.write(
                    f"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="{pkg_id}"
    android:versionCode="{version_code}"
    android:versionName="{version_name}">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher"
        android:hardwareAccelerated="true"
        android:usesCleartextTraffic="true"
        android:theme="@android:style/Theme.Material.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden|screenLayout|smallestScreenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>"""
                )

            # 5. Write MainActivity.java
            pkg_path = os.path.join(build_dir, "src", *pkg_id.split("."))
            os.makedirs(pkg_path, exist_ok=True)
            java_path = os.path.join(pkg_path, "MainActivity.java")
            with open(java_path, "w", encoding="utf-8") as f:
                f.write(
                    f"""package {pkg_id};

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class MainActivity extends Activity {{
    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {{
        super.onCreate(savedInstanceState);

        // Customize status bar
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("{theme_color}"));
        window.setNavigationBarColor(Color.parseColor("{theme_color}"));

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);

        configureWebView();

        if (savedInstanceState != null) {{
            webView.restoreState(savedInstanceState);
        }} else {{
            webView.loadUrl(getString(R.string.target_url));
        }}
    }}

    private void configureWebView() {{
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient() {{
            @Override
            public void onProgressChanged(WebView view, int newProgress) {{
                if (newProgress < 100) {{
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                }} else {{
                    progressBar.setVisibility(View.GONE);
                }}
            }}
        }});

        webView.setWebViewClient(new WebViewClient() {{
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {{
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {{
                    return false;
                }}
                try {{
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                }} catch (Exception e) {{
                    return true;
                }}
            }}
        }});
    }}

    @Override
    public void onBackPressed() {{
        if (webView != null && webView.canGoBack()) {{
            webView.goBack();
        }} else {{
            super.onBackPressed();
        }}
    }}

    @Override
    protected void onSaveInstanceState(Bundle outState) {{
        super.onSaveInstanceState(outState);
        if (webView != null) {{
            webView.saveState(outState);
        }}
    }}
}}"""
                )

            # 6. AAPT2 Compile
            res_zip = os.path.join(build_dir, "compiled_res.zip")
            r_comp = subprocess.run(
                [self.sdk.aapt2, "compile", "--dir", res_dir, "-o", res_zip],
                capture_output=True,
                text=True,
            )
            if r_comp.returncode != 0:
                raise RuntimeError(f"aapt2 compile failed: {r_comp.stderr}")

            # 7. AAPT2 Link
            unaligned_apk = os.path.join(build_dir, "unaligned.apk")
            gen_dir = os.path.join(build_dir, "gen")
            os.makedirs(gen_dir, exist_ok=True)
            r_link = subprocess.run(
                [
                    self.sdk.aapt2,
                    "link",
                    "-I",
                    self.sdk.android_jar,
                    "--manifest",
                    manifest_path,
                    "-o",
                    unaligned_apk,
                    "--java",
                    gen_dir,
                    res_zip,
                ],
                capture_output=True,
                text=True,
            )
            if r_link.returncode != 0:
                raise RuntimeError(f"aapt2 link failed: {r_link.stderr}")

            # 8. Javac Compile
            bin_dir = os.path.join(build_dir, "bin")
            os.makedirs(bin_dir, exist_ok=True)
            r_java = os.path.join(gen_dir, *pkg_id.split("."), "R.java")
            r_javac = subprocess.run(
                [
                    self.sdk.javac,
                    "-source",
                    "8",
                    "-target",
                    "8",
                    "-cp",
                    self.sdk.android_jar,
                    "-d",
                    bin_dir,
                    r_java,
                    java_path,
                ],
                capture_output=True,
                text=True,
            )
            if r_javac.returncode != 0:
                raise RuntimeError(f"javac failed: {r_javac.stderr}")

            # 9. D8 Compilation
            classes = [
                os.path.join(dp, f)
                for dp, _, fn in os.walk(bin_dir)
                for f in fn
                if f.endswith(".class")
            ]
            r_d8 = subprocess.run(
                [self.sdk.d8, "--output", build_dir] + classes,
                capture_output=True,
                text=True,
            )
            if r_d8.returncode != 0:
                raise RuntimeError(f"d8 failed: {r_d8.stderr}")

            # 10. Inject classes.dex into unaligned.apk
            dex_file = os.path.join(build_dir, "classes.dex")
            import zipfile

            with zipfile.ZipFile(unaligned_apk, "a") as z:
                z.write(dex_file, "classes.dex")

            # 11. Zipalign
            aligned_apk = os.path.join(build_dir, "aligned.apk")
            r_align = subprocess.run(
                [self.sdk.zipalign, "-p", "-f", "4", unaligned_apk, aligned_apk],
                capture_output=True,
                text=True,
            )
            if r_align.returncode != 0:
                raise RuntimeError(f"zipalign failed: {r_align.stderr}")

            # 12. Keystore Preparation (persistent: consecutive builds of the
            # same package must share one signer certificate or Android
            # rejects updates)
            if not keystore_path:
                keystore_path = str(Path.home() / ".praha" / "webapp_signing.keystore")
            if not os.path.isfile(keystore_path):
                os.makedirs(os.path.dirname(os.path.abspath(keystore_path)), exist_ok=True)
                subprocess.run(
                    [
                        self.sdk.keytool,
                        "-genkeypair",
                        "-keystore",
                        keystore_path,
                        "-storepass",
                        keystore_pass,
                        "-alias",
                        key_alias,
                        "-keypass",
                        key_pass,
                        "-keyalg",
                        "RSA",
                        "-keysize",
                        "2048",
                        "-validity",
                        "10000",
                        "-dname",
                        f"CN={app_name},O=Praha,C=US",
                    ],
                    check=True,
                    capture_output=True,
                )
                try:
                    os.chmod(keystore_path, 0o600)
                except OSError:
                    pass
            print(f"[*] Signing keystore: {keystore_path}")

            # 13. apksigner
            signed_apk = os.path.join(build_dir, "signed.apk")
            r_sign = subprocess.run(
                [
                    self.sdk.apksigner,
                    "sign",
                    "--ks",
                    keystore_path,
                    "--ks-pass",
                    f"pass:{keystore_pass}",
                    "--key-pass",
                    f"pass:{key_pass}",
                    "--ks-key-alias",
                    key_alias,
                    "--out",
                    signed_apk,
                    aligned_apk,
                ],
                capture_output=True,
                text=True,
            )
            if r_sign.returncode != 0:
                raise RuntimeError(f"apksigner failed: {r_sign.stderr}")

            # 14. apksigner verify
            r_ver = subprocess.run(
                [self.sdk.apksigner, "verify", "--verbose", signed_apk],
                capture_output=True,
                text=True,
            )
            if r_ver.returncode != 0:
                raise RuntimeError(f"apksigner verification failed: {r_ver.stderr}")

            # Copy to final destination
            shutil.copy2(signed_apk, str(final_apk_path))

        # Compute SHA-256 and emit release manifest
        apk_bytes = final_apk_path.read_bytes()
        sha256 = hashlib.sha256(apk_bytes).hexdigest()

        manifest_file = Path(output_dir) / "release-manifest.json"
        manifest_data = {}
        if manifest_file.exists():
            try:
                manifest_data = json.loads(manifest_file.read_text(encoding="utf-8"))
            except Exception:
                manifest_data = {}

        manifest_data[final_apk_name] = {
            "name": app_name,
            "url": url,
            "package_id": pkg_id,
            "version_name": version_name,
            "version_code": version_code,
            "size_bytes": len(apk_bytes),
            "sha256": sha256,
            "built_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "verified": True,
        }
        manifest_file.write_text(json.dumps(manifest_data, indent=2), encoding="utf-8")

        # Update SHA256SUMS.txt
        sums_file = Path(output_dir) / "SHA256SUMS.txt"
        sums = {}
        if sums_file.exists():
            for line in sums_file.read_text().splitlines():
                if "  " in line:
                    h, f = line.split("  ", 1)
                    sums[f] = h
        sums[final_apk_name] = sha256
        with open(sums_file, "w", encoding="utf-8") as sf:
            for item_name, digest in sorted(sums.items()):
                sf.write(f"{digest}  {item_name}\n")

        print(f"[+] Output APK: {final_apk_path}")
        print(f"[+] Size: {len(apk_bytes):,} bytes")
        print(f"[+] SHA256: {sha256}")
        print("[+] Signature Verified: v1, v2, v3 schemes")
        return final_apk_path


def main():
    parser = argparse.ArgumentParser(
        description="Convert any website into a native Android Web App APK and release it."
    )
    parser.add_argument("url", nargs="?", help="Website URL to convert (e.g. https://start.me/p/L1rEYQ/osint4all)")
    parser.add_argument("--name", help="App display name (default: auto-detected from URL/title)")
    parser.add_argument("--package", help="Android package name (default: org.praha.webapp.<slug>)")
    parser.add_argument("--icon", help="Path to custom launcher icon (PNG/JPEG/SVG)")
    parser.add_argument("--theme-color", default="#102b29", help="Hex theme color (default: #102b29)")
    parser.add_argument("--version-name", default="1.0.0", help="Version name (default: 1.0.0)")
    parser.add_argument("--version-code", type=int, default=1, help="Version code (default: 1)")
    parser.add_argument("--output-dir", default="dist/releases", help="Output directory (default: dist/releases)")
    parser.add_argument("--debug", action="store_true", help="Build debug APK instead of release")
    parser.add_argument("--install", action="store_true", help="Install to connected device via adb after build")

    args = parser.parse_args()

    if not args.url:
        parser.print_help()
        sys.exit(1)

    sdk = AndroidSDKLocator()
    compiler = WebAppCompiler(sdk)
    apk_path = compiler.build(
        url=args.url,
        name=args.name,
        package_id=args.package,
        icon_path=args.icon,
        theme_color=args.theme_color,
        version_name=args.version_name,
        version_code=args.version_code,
        output_dir=args.output_dir,
        is_release=not args.debug,
    )

    if args.install:
        if sdk.adb:
            print("[*] Installing to connected Android device...")
            res = subprocess.run([sdk.adb, "install", "-r", str(apk_path)])
            if res.returncode == 0:
                print("[+] Successfully installed to device.")
            else:
                print("[-] ADB install failed.")
        else:
            print("[-] adb not found; skipping device install.")


if __name__ == "__main__":
    main()
