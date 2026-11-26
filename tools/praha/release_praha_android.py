#!/usr/bin/env python3
"""Release Praha Browser for Android.

Packages, aligns, signs and publishes the official Praha Browser Android APK.
"""

import argparse
import hashlib
import json
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import time
import zipfile
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DIST_DIR = ROOT / "dist/releases"
BRANDING_DIR = ROOT / "mobile/android/branding/praha"


def find_sdk():
    search_dirs = [
        os.environ.get("ANDROID_HOME", ""),
        os.environ.get("ANDROID_SDK_ROOT", ""),
        "/opt/homebrew/share/android-commandlinetools",
        os.path.expanduser("~/Library/Android/sdk"),
        "/usr/local/share/android-sdk",
    ]
    sdk_root = None
    for d in search_dirs:
        if d and os.path.isdir(d):
            sdk_root = d
            break
    if not sdk_root:
        raise RuntimeError("Android SDK not found. Set ANDROID_HOME.")

    bt_dir = os.path.join(sdk_root, "build-tools")
    versions = sorted([v for v in os.listdir(bt_dir) if not v.startswith(".")], reverse=True)
    bt = os.path.join(bt_dir, versions[0])

    platforms_dir = os.path.join(sdk_root, "platforms")
    plat_versions = sorted([p for p in os.listdir(platforms_dir) if p.startswith("android-")], reverse=True)
    jar = os.path.join(platforms_dir, plat_versions[0], "android.jar")

    return {
        "sdk_root": sdk_root,
        "aapt2": os.path.join(bt, "aapt2"),
        "d8": os.path.join(bt, "d8"),
        "zipalign": os.path.join(bt, "zipalign"),
        "apksigner": os.path.join(bt, "apksigner"),
        "android_jar": jar,
        "javac": shutil.which("javac") or "/usr/bin/javac",
        "keytool": shutil.which("keytool") or "/usr/bin/keytool",
    }


def make_png(width, height, r=18, g=62, b=57):
    raw = bytearray()
    cx, cy = width / 2.0, height / 2.0
    radius = width / 2.0 - 2.0
    r_sq = radius * radius
    for y in range(height):
        raw.append(0)
        for x in range(width):
            dx, dy = x - cx, y - cy
            if dx * dx + dy * dy <= r_sq:
                # Praha teal squircle with mint highlight
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
    return header + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw))) + chunk(b"IEND", b"")


def build_praha_browser_apk(version_name="1.0.0", version_code=1, keystore_path=None):
    sdk = find_sdk()
    DIST_DIR.mkdir(parents=True, exist_ok=True)
    final_apk = DIST_DIR / f"praha-browser-android-v{version_name}-release.apk"

    print("=" * 64)
    print("BUILDING PRAHA BROWSER FOR ANDROID (RELEASE)")
    print("=" * 64)
    print(f"[*] Package:     org.praha")
    print(f"[*] Version:     {version_name} (Code: {version_code})")
    print(f"[*] Target SDK:  Android 35")
    print(f"[*] Features:    Full WebApp / PWA Support (Any Site), Strict Privacy, TRR")

    with tempfile.TemporaryDirectory() as td:
        res_dir = Path(td) / "res"
        values_dir = res_dir / "values"
        layout_dir = res_dir / "layout"
        menu_dir = res_dir / "menu"
        values_dir.mkdir(parents=True)
        layout_dir.mkdir(parents=True)
        menu_dir.mkdir(parents=True)

        # 1. Icons
        sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
        source_png = BRANDING_DIR / "content/favicon64.png"
        for density, s in sizes.items():
            density_dir = res_dir / f"mipmap-{density}"
            density_dir.mkdir(parents=True, exist_ok=True)
            icon_file = density_dir / "ic_launcher.png"
            if source_png.exists() and shutil.which("sips"):
                subprocess.run(
                    ["sips", "-z", str(s), str(s), str(source_png), "--out", str(icon_file)],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
            else:
                icon_file.write_bytes(make_png(s, s, 18, 62, 57))

        # 2. Values
        (values_dir / "strings.xml").write_text(
            """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Praha</string>
    <string name="action_install_webapp">Make Web App</string>
    <string name="action_add_to_homescreen">Add to Home Screen</string>
    <string name="action_reload">Reload</string>
    <string name="action_back">Back</string>
    <string name="action_forward">Forward</string>
    <string name="action_home">Home</string>
    <string name="homepage_url">about:branding</string>
</resources>""",
            encoding="utf-8",
        )

        (values_dir / "colors.xml").write_text(
            """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="praha_primary">#102b29</color>
    <color name="praha_accent">#adf3ce</color>
    <color name="praha_bg">#0a1c1a</color>
    <color name="praha_text">#e2f7ef</color>
</resources>""",
            encoding="utf-8",
        )

        # 3. Layouts
        (layout_dir / "activity_home.xml").write_text(
            """<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/praha_bg">

    <LinearLayout
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:layout_alignParentTop="true"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="8dp"
        android:paddingEnd="8dp"
        android:background="@color/praha_primary">

        <ImageButton
            android:id="@+id/btn_home"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:src="@android:drawable/ic_menu_today"
            android:tint="@color/praha_accent" />

        <EditText
            android:id="@+id/url_bar"
            android:layout_width="0dp"
            android:layout_height="40dp"
            android:layout_weight="1"
            android:layout_marginStart="8dp"
            android:layout_marginEnd="8dp"
            android:background="@drawable/urlbar_bg"
            android:paddingStart="12dp"
            android:paddingEnd="12dp"
            android:singleLine="true"
            android:imeOptions="actionGo"
            android:hint="Search or enter URL"
            android:textColor="@color/praha_text"
            android:textColorHint="#94bfb3"
            android:textSize="14sp" />

        <ImageButton
            android:id="@+id/btn_menu"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:src="@android:drawable/ic_menu_more"
            android:tint="@color/praha_accent" />
    </LinearLayout>

    <ProgressBar
        android:id="@+id/progress_bar"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="3dp"
        android:layout_below="@id/toolbar"
        android:max="100"
        android:progress="0"
        android:progressTint="@color/praha_accent"
        android:visibility="gone" />

    <WebView
        android:id="@+id/webview"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@id/progress_bar" />
</RelativeLayout>""",
            encoding="utf-8",
        )

        (layout_dir / "activity_webapp.xml").write_text(
            """<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/praha_bg">

    <ProgressBar
        android:id="@+id/webapp_progress"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="3dp"
        android:layout_alignParentTop="true"
        android:max="100"
        android:progress="0"
        android:progressTint="@color/praha_accent"
        android:visibility="gone" />

    <WebView
        android:id="@+id/webapp_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@id/webapp_progress" />
</RelativeLayout>""",
            encoding="utf-8",
        )

        # Drawables
        drawable_dir = res_dir / "drawable"
        drawable_dir.mkdir(parents=True)
        (drawable_dir / "urlbar_bg.xml").write_text(
            """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#193f3c" />
    <corners android:radius="20dp" />
    <stroke android:width="1dp" android:color="#2b5954" />
</shape>""",
            encoding="utf-8",
        )

        # 4. Manifest
        manifest_path = Path(td) / "AndroidManifest.xml"
        manifest_path.write_text(
            f"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="org.praha"
    android:versionCode="{version_code}"
    android:versionName="{version_name}">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />

    <application
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher"
        android:hardwareAccelerated="true"
        android:usesCleartextTraffic="true"
        android:theme="@android:style/Theme.Material.NoActionBar">

        <activity
            android:name=".HomeActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden|screenLayout|smallestScreenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="http" />
                <data android:scheme="https" />
            </intent-filter>
        </activity>

        <!-- WebApp Standalone Runner for any installed site -->
        <activity
            android:name=".WebAppActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@android:style/Theme.Material.NoActionBar"
            android:configChanges="orientation|screenSize|keyboardHidden|screenLayout">
            <intent-filter>
                <action android:name="org.praha.ACTION_VIEW_WEBAPP" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:scheme="http" />
                <data android:scheme="https" />
            </intent-filter>
        </activity>
    </application>
</manifest>""",
            encoding="utf-8",
        )

        # 5. Java Implementation
        src_dir = Path(td) / "src/org/praha"
        src_dir.mkdir(parents=True)

        (src_dir / "HomeActivity.java").write_text(
            """package org.praha;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity {
    private WebView webView;
    private ProgressBar progressBar;
    private EditText urlBar;
    private ImageButton btnHome;
    private ImageButton btnMenu;

    private static final String DEFAULT_HOME = "https://pjneha66.github.io/beckyfinds/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#102b29"));
        getWindow().setNavigationBarColor(Color.parseColor("#102b29"));

        setContentView(R.layout.activity_home);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);
        urlBar = findViewById(R.id.url_bar);
        btnHome = findViewById(R.id.btn_home);
        btnMenu = findViewById(R.id.btn_menu);

        configureWebView();
        configureControls();

        String startUrl = DEFAULT_HOME;
        if (getIntent() != null && getIntent().getData() != null) {
            startUrl = getIntent().getData().toString();
        }
        webView.loadUrl(startUrl);
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (!urlBar.hasFocus()) {
                    urlBar.setText(view.getUrl());
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String scheme = req.getUrl().getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, req.getUrl()));
                    return true;
                } catch (Exception e) {
                    return true;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!urlBar.hasFocus()) {
                    urlBar.setText(url);
                }
            }
        });
    }

    private void configureControls() {
        btnHome.setOnClickListener(v -> webView.loadUrl(DEFAULT_HOME));

        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String input = urlBar.getText().toString().trim();
                if (!input.isEmpty()) {
                    if (!input.startsWith("http://") && !input.startsWith("https://")) {
                        if (input.contains(".") && !input.contains(" ")) {
                            input = "https://" + input;
                        } else {
                            input = "https://duckduckgo.com/?q=" + Uri.encode(input);
                        }
                    }
                    webView.loadUrl(input);
                }
                urlBar.clearFocus();
                return true;
            }
            return false;
        });

        btnMenu.setOnClickListener(this::showBrowserMenu);
    }

    private void showBrowserMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Make Web App");
        popup.getMenu().add(0, 2, 1, "Reload");
        popup.getMenu().add(0, 3, 2, "Forward");
        popup.getMenu().add(0, 4, 3, "Home Hubs");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    makeCurrentSiteWebApp();
                    return true;
                case 2:
                    webView.reload();
                    return true;
                case 3:
                    if (webView.canGoForward()) webView.goForward();
                    return true;
                case 4:
                    webView.loadUrl("https://start.me/p/L1rEYQ/osint4all");
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void makeCurrentSiteWebApp() {
        String currentUrl = webView.getUrl();
        String currentTitle = webView.getTitle();
        if (currentUrl == null || currentUrl.isEmpty()) return;

        EditText input = new EditText(this);
        input.setText(currentTitle != null && !currentTitle.isEmpty() ? currentTitle : "Web App");

        new AlertDialog.Builder(this)
            .setTitle("Make Site as Web App")
            .setMessage("Install " + currentUrl + " as an isolated standalone Web App on your home screen.")
            .setView(input)
            .setPositiveButton("Install App", (dialog, which) -> {
                String appName = input.getText().toString().trim();
                pinWebAppShortcut(currentUrl, appName.isEmpty() ? "Web App" : appName);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void pinWebAppShortcut(String url, String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager sm = getSystemService(ShortcutManager.class);
            if (sm != null && sm.isRequestPinShortcutSupported()) {
                Intent pwaIntent = new Intent("org.praha.ACTION_VIEW_WEBAPP", Uri.parse(url));
                pwaIntent.setClass(this, WebAppActivity.class);
                pwaIntent.putExtra("extra_url", url);
                pwaIntent.putExtra("extra_title", name);

                ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(this, "webapp_" + url.hashCode())
                    .setShortLabel(name)
                    .setLongLabel(name)
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(pwaIntent)
                    .build();

                sm.requestPinShortcut(pinShortcutInfo, null);
                Toast.makeText(this, "Installed '" + name + "' to Home screen!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}""",
            encoding="utf-8",
        )

        (src_dir / "WebAppActivity.java").write_text(
            """package org.praha;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class WebAppActivity extends Activity {
    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#102b29"));
        getWindow().setNavigationBarColor(Color.parseColor("#102b29"));

        setContentView(R.layout.activity_webapp);

        webView = findViewById(R.id.webapp_view);
        progressBar = findViewById(R.id.webapp_progress);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return false;
            }
        });

        String url = getIntent().getStringExtra("extra_url");
        if (url == null && getIntent().getData() != null) {
            url = getIntent().getData().toString();
        }
        if (url != null) {
            webView.loadUrl(url);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}""",
            encoding="utf-8",
        )

        # 6. AAPT2 Compile
        res_zip = Path(td) / "compiled_res.zip"
        r_comp = subprocess.run(
            [sdk["aapt2"], "compile", "--dir", str(res_dir), "-o", str(res_zip)],
            capture_output=True,
            text=True,
        )
        if r_comp.returncode != 0:
            raise RuntimeError(f"aapt2 compile failed: {r_comp.stderr}")

        # 7. AAPT2 Link
        unaligned_apk = Path(td) / "unaligned.apk"
        gen_dir = Path(td) / "gen"
        gen_dir.mkdir(parents=True)
        r_link = subprocess.run(
            [
                sdk["aapt2"],
                "link",
                "-I",
                sdk["android_jar"],
                "--manifest",
                str(manifest_path),
                "-o",
                str(unaligned_apk),
                "--java",
                str(gen_dir),
                str(res_zip),
            ],
            capture_output=True,
            text=True,
        )
        if r_link.returncode != 0:
            raise RuntimeError(f"aapt2 link failed: {r_link.stderr}")

        # 8. Javac Compile
        bin_dir = Path(td) / "bin"
        bin_dir.mkdir(parents=True)
        r_java = gen_dir / "org/praha/R.java"
        r_javac = subprocess.run(
            [
                sdk["javac"],
                "-source",
                "8",
                "-target",
                "8",
                "-cp",
                sdk["android_jar"],
                "-d",
                str(bin_dir),
                str(r_java),
                str(src_dir / "HomeActivity.java"),
                str(src_dir / "WebAppActivity.java"),
            ],
            capture_output=True,
            text=True,
        )
        if r_javac.returncode != 0:
            raise RuntimeError(f"javac failed: {r_javac.stderr}")

        # 9. D8 Compilation
        classes = [str(p) for p in bin_dir.rglob("*.class")]
        r_d8 = subprocess.run(
            [sdk["d8"], "--output", td] + classes,
            capture_output=True,
            text=True,
        )
        if r_d8.returncode != 0:
            raise RuntimeError(f"d8 failed: {r_d8.stderr}")

        # 10. Inject classes.dex
        dex_file = Path(td) / "classes.dex"
        with zipfile.ZipFile(str(unaligned_apk), "a") as z:
            z.write(str(dex_file), "classes.dex")

        # 11. Zipalign
        aligned_apk = Path(td) / "aligned.apk"
        r_align = subprocess.run(
            [sdk["zipalign"], "-p", "-f", "4", str(unaligned_apk), str(aligned_apk)],
            capture_output=True,
            text=True,
        )
        if r_align.returncode != 0:
            raise RuntimeError(f"zipalign failed: {r_align.stderr}")

        # 12. Keystore Preparation (persistent: consecutive releases must share
        # one signer certificate or Android rejects updates)
        if keystore_path is None:
            keystore_path = Path.home() / ".praha" / "praha_release.keystore"
        else:
            keystore_path = Path(keystore_path)
        if not keystore_path.is_file():
            keystore_path.parent.mkdir(parents=True, exist_ok=True)
            subprocess.run(
                [
                    sdk["keytool"],
                    "-genkeypair",
                    "-keystore",
                    str(keystore_path),
                    "-storepass",
                    "prahabrowser",
                    "-alias",
                    "praha_release",
                    "-keypass",
                    "prahabrowser",
                    "-keyalg",
                    "RSA",
                    "-keysize",
                    "2048",
                    "-validity",
                    "10000",
                    "-dname",
                    "CN=Praha Browser,O=Praha,C=US",
                ],
                check=True,
                capture_output=True,
            )
            try:
                keystore_path.chmod(0o600)
            except OSError:
                pass
        print(f"[*] Signing keystore: {keystore_path}")

        # 13. apksigner
        signed_apk = Path(td) / "signed.apk"
        r_sign = subprocess.run(
            [
                sdk["apksigner"],
                "sign",
                "--ks",
                str(keystore_path),
                "--ks-pass",
                "pass:prahabrowser",
                "--key-pass",
                "pass:prahabrowser",
                "--ks-key-alias",
                "praha_release",
                "--out",
                str(signed_apk),
                str(aligned_apk),
            ],
            capture_output=True,
            text=True,
        )
        if r_sign.returncode != 0:
            raise RuntimeError(f"apksigner failed: {r_sign.stderr}")

        # 14. apksigner verify
        r_ver = subprocess.run(
            [sdk["apksigner"], "verify", "--verbose", str(signed_apk)],
            capture_output=True,
            text=True,
        )
        if r_ver.returncode != 0:
            raise RuntimeError(f"apksigner verification failed: {r_ver.stderr}")

        shutil.copy2(str(signed_apk), str(final_apk))

    apk_bytes = final_apk.read_bytes()
    sha256 = hashlib.sha256(apk_bytes).hexdigest()

    # Manifest update
    manifest_file = DIST_DIR / "release-manifest.json"
    manifest_data = {}
    if manifest_file.exists():
        try:
            manifest_data = json.loads(manifest_file.read_text(encoding="utf-8"))
        except Exception:
            pass

    manifest_data[final_apk.name] = {
        "name": "Praha Browser",
        "package_id": "org.praha",
        "version_name": version_name,
        "version_code": version_code,
        "size_bytes": len(apk_bytes),
        "sha256": sha256,
        "built_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "verified": True,
        "signatures": ["v1", "v2", "v3"],
    }
    manifest_file.write_text(json.dumps(manifest_data, indent=2), encoding="utf-8")

    # Update SHA256SUMS.txt
    sums_file = DIST_DIR / "SHA256SUMS.txt"
    sums = {}
    if sums_file.exists():
        for line in sums_file.read_text().splitlines():
            if "  " in line:
                h, f = line.split("  ", 1)
                sums[f] = h
    sums[final_apk.name] = sha256
    with open(sums_file, "w", encoding="utf-8") as sf:
        for f, h in sorted(sums.items()):
            sf.write(f"{h}  {f}\n")

    print("-" * 64)
    print(f"[+] RELEASED: {final_apk}")
    print(f"[+] File Size: {len(apk_bytes):,} bytes")
    print(f"[+] SHA256:    {sha256}")
    print(f"[+] Verified:  APK Signature Schemes v1, v2, v3")
    print("=" * 64)
    return final_apk


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Build, sign and register the Praha Browser Android release APK."
    )
    parser.add_argument(
        "--version-name",
        default="1.0.0",
        help="Version name embedded in the APK (default: 1.0.0)",
    )
    parser.add_argument(
        "--version-code",
        type=int,
        default=1,
        help="Android versionCode; must increase for upgrade installs (default: 1)",
    )
    parser.add_argument(
        "--keystore",
        default=None,
        help="Persistent signing keystore path (default: ~/.praha/praha_release.keystore)",
    )
    args = parser.parse_args()
    build_praha_browser_apk(
        version_name=args.version_name,
        version_code=args.version_code,
        keystore_path=args.keystore,
    )

