# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from marionette_driver.marionette import Marionette


def check(browser):
    home = browser.get_pref("browser.startup.homepage", default_branch=True)
    assert home == "about:branding"
    browser.navigate(home)
    assert browser.execute_script("""
        return Array.from(document.querySelectorAll("nav a[target='_blank']"), a => a.href);
    """) == ["https://pjneha66.github.io/beckyfinds/", "https://fmhy.net/"]
    assert "Praha Browser" in browser.title, browser.title
    assert browser.get_pref("browser.contentblocking.category") == "strict"
    assert browser.get_pref("network.cookie.cookieBehavior") == 5
    for pref, expected in {
        "startup.homepage_welcome_url": "about:branding",
        "toolkit.telemetry.enabled": False,
        "datareporting.healthreport.uploadEnabled": False,
        "dom.security.https_only_mode": True,
        "network.trr.mode": 2,
    }.items():
        assert browser.get_pref(pref, default_branch=True) == expected, pref
    with browser.using_context("chrome"):
        active = browser.execute_async_script("""
            const done = arguments[0];
            const { AddonManager } = ChromeUtils.importESModule(
              "resource://gre/modules/AddonManager.sys.mjs"
            );
            AddonManager.getAddonByID("uBlock0@raymondhill.net").then(
              addon => done(addon?.isActive === true), () => done(false)
            );
        """)
    assert active, "The bundled extension should be active on first run"


def main(binary):
    # The regular test harness disables distro add-ons and tracking protection.
    with tempfile.TemporaryDirectory() as directory:
        profile = Path(directory)
        (profile / "user.js").write_text(
            'user_pref("marionette.port", 0);\nuser_pref("remote.prefs.recommended", false);\n'
        )
        process = subprocess.Popen(
            [binary, "-no-remote", "-headless", "-profile", directory, "-marionette"],
            env={**os.environ, "MOZ_REMOTE_ALLOW_SYSTEM_ACCESS": "1"},
        )
        try:
            port_file = profile / "MarionetteActivePort"
            deadline = time.monotonic() + 90
            while not port_file.exists():
                if process.poll() is not None or time.monotonic() >= deadline:
                    raise RuntimeError("Packaged browser did not start Marionette")
                time.sleep(0.1)
            browser = Marionette(host="127.0.0.1", port=int(port_file.read_text()))
            browser.start_session()
            try:
                check(browser)
            finally:
                browser.delete_session()
        finally:
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


if __name__ == "__main__":
    main(sys.argv[1])
