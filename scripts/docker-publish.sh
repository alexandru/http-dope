#!/usr/bin/env bash

set -e

DIR="$(
    cd "$(dirname "$0")/.."
    pwd -P
)"
cd "$DIR"

./scripts/refresh-maxmind-db.sh
exec sbt clean update stage docker:publish
