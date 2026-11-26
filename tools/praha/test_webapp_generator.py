"""Test suite for Praha Android WebApp compiler and release tooling."""

import tempfile
import unittest
from pathlib import Path

from make_webapp_apk import AndroidSDKLocator, WebAppCompiler


class WebAppGeneratorTest(unittest.TestCase):
    def setUp(self):
        self.sdk = AndroidSDKLocator()
        self.compiler = WebAppCompiler(self.sdk)

    def test_metadata_derivation(self):
        cases = [
            ("https://start.me/p/L1rEYQ/osint4all", "Osint4all", "org.praha.webapp.osint4all"),
            ("https://fmhy.net/", "FMHY", "org.praha.webapp.fmhy"),
            ("https://example.com/app", "APP", "org.praha.webapp.app"),
        ]
        for url, exp_name, exp_pkg in cases:
            with self.subTest(url=url):
                name, pkg, slug = self.compiler._auto_metadata(url)
                self.assertEqual(name, exp_name)
                self.assertEqual(pkg, exp_pkg)

    def test_package_id_sanitization(self):
        raw = "org.praha.web-app.123_test!site"
        cleaned = self.compiler._sanitize_package_id(raw)
        self.assertRegex(cleaned, r"^[a-zA-Z_][a-zA-Z0-9_]*(\.[a-zA-Z_][a-zA-Z0-9_]*)+$")

    def test_build_signed_webapp_apk(self):
        with tempfile.TemporaryDirectory() as td:
            keystore = Path(td) / "signing.keystore"
            apk_path = self.compiler.build(
                url="https://example.com",
                name="TestWebApp",
                package_id="org.praha.webapp.test",
                output_dir=td,
                is_release=True,
                keystore_path=str(keystore),
            )
            self.assertTrue(apk_path.is_file())
            self.assertGreater(apk_path.stat().st_size, 5000)
            self.assertTrue(keystore.is_file(), "keystore must persist for signer continuity")

            manifest_file = Path(td) / "release-manifest.json"
            self.assertTrue(manifest_file.is_file())

            sums_file = Path(td) / "SHA256SUMS.txt"
            self.assertTrue(sums_file.is_file())

            # A second build must reuse the same keystore (same signer)
            apk_path2 = self.compiler.build(
                url="https://example.com",
                name="TestWebApp",
                package_id="org.praha.webapp.test",
                output_dir=td,
                is_release=True,
                keystore_path=str(keystore),
            )
            self.assertTrue(apk_path2.is_file())


if __name__ == "__main__":
    unittest.main()
