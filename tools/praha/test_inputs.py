"""Offline checks for release inputs; no compiler or Android SDK required."""

import hashlib
import io
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "browser/app/distribution"))
import ublock


class BuildInputs(unittest.TestCase):
    def test_xpi_packaging_is_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "fixture.xpi"
            path.write_bytes(b"original")
            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            with patch.object(ublock, "DIGEST", digest):
                output = io.BytesIO()
                ublock.main(output, path)
                self.assertEqual(output.getvalue(), b"original")
                path.write_bytes(b"altered")
                output = io.BytesIO()
                with self.assertRaises(ValueError):
                    ublock.main(output, path)
                self.assertFalse(output.getvalue())

    def test_windows_tiles_exist_at_manifest_paths(self):
        brand = ROOT / "browser/branding/praha"
        for manifest in brand.glob("*.VisualElementsManifest.xml"):
            tiles = ET.parse(manifest).getroot().find("VisualElements")
            for size in (70, 150):
                directory, filename = tiles.attrib[f"Square{size}x{size}Logo"].rsplit("\\", 1)
                self.assertEqual(directory, r"browser\VisualElements")
                self.assertTrue((brand / filename).is_file(), filename)

    def test_privacy_settings_are_first_on_android(self):
        res = ROOT / "mobile/android/fenix/app/src/main/res/xml"
        android = "{http://schemas.android.com/apk/res/android}"
        root = ET.parse(res / "preferences.xml").getroot()
        self.assertEqual(root[0].get(android + "key"), "@string/pref_key_privacy_security_category")
        root = ET.parse(res / "tracking_protection_preferences.xml").getroot()
        for key, expected in (("strict_default", "true"), ("standard_option", "false")):
            option = root.find(f"*[@{android}key='@string/pref_key_tracking_protection_{key}']")
            self.assertEqual(option.get(android + "defaultValue"), expected)


if __name__ == "__main__":
    unittest.main()
