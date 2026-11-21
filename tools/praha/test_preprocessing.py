"""Run with ./mach python to check branded and unbranded packaging paths."""

import io
import re
import tempfile
import unittest
from pathlib import Path

from mozbuild.preprocessor import Preprocessor

ROOT = Path(__file__).resolve().parents[2]


class BrandedInputs(unittest.TestCase):
    def test_preprocessed_resources(self):
        files = {
            "browser/app/profile/firefox.js": "browser.contentblocking.strictByDefault",
            "browser/base/content/browser.xhtml": "/praha.css",
            "toolkit/mozapps/extensions/content/aboutaddons.html": "content/addons.css",
            "browser/installer/package-manifest.in": "praha-branding.js",
        }
        with tempfile.TemporaryDirectory() as directory:
            locales = Path(directory) / "locale-manifest.in"
            locales.touch()
            for path, needle in files.items():
                for branded in (False, True):
                    with self.subTest(path=path, branded=branded):
                        source = ROOT / path
                        pp = Preprocessor()
                        pp.out = io.StringIO()
                        if branded:
                            pp.context["MOZ_PRAHA_BROWSER"] = 1
                        if path.endswith(".in"):
                            for key in re.findall(r"@([A-Z_]+)@", source.read_text()):
                                pp.context[key] = "fixture"
                            pp.context["PKG_LOCALE_MANIFEST"] = str(locales)
                        pp.do_include(str(source))
                        self.assertEqual(needle in pp.out.getvalue(), branded)


if __name__ == "__main__":
    unittest.main()
