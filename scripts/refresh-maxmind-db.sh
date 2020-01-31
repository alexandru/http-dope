#!/usr/bin/env bash

set -e

DIR="$(
    cd "$(dirname "$0")/.."
    pwd -P
)"
cd "$DIR"

if [ -z "$DOPE_MAXMIND_GEOIP_API_KEY" ]; then
    echo "ERROR: env value not set: DOPE_MAXMIND_GEOIP_API_KEY" 1>&2
    exit 1
fi

TMP=$(mktemp -d)
function finish {
  rm -rf "$TMP"
}
trap finish EXIT

URL="https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=$DOPE_MAXMIND_GEOIP_API_KEY&suffix=tar.gz"
curl "$URL" -o "$TMP/download.tar.gz"

cd "$TMP"
tar -xvzf "download.tar.gz"

cp $(find . -iname "*.mmdb") "$DIR/src/main/resources/maxmind.mmdb"
rm -f "$DIR/src/main/resources/maxmind.mmdb.gz"
gzip -9 "$DIR/src/main/resources/maxmind.mmdb"
