# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import hashlib
from pathlib import Path
from urllib.request import urlopen

VERSION = "1.76.6"
DIGEST = "debffadf7d5117bd2d4e9218e27bfdf3112544fd2e87f14882d274a11dc95375"
URL = f"https://github.com/gorhill/uBlock/releases/download/{VERSION}/uBlock0_{VERSION}.firefox.signed.xpi"


def verified(data):
    if hashlib.sha256(data).hexdigest() != DIGEST:
        raise ValueError(f"Expected the signed uBlock Origin {VERSION} XPI ({DIGEST})")
    return data


def main(output, source):
    output.write(verified(Path(source).read_bytes()))


if __name__ == "__main__":
    destination = Path(__file__).with_name("ublock.xpi")
    if destination.exists():
        verified(destination.read_bytes())
    else:
        with urlopen(URL, timeout=60) as response:
            destination.write_bytes(verified(response.read()))
