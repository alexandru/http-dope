#!/usr/bin/env bash

set -e

DIR="$(
    cd "$(dirname "$0")/.."
    pwd -P
)"
cd "$DIR"

exec sbt clean update stage docker:publishLocal
