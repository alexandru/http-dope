#!/usr/bin/env bash

set -e

DIR="$(
    cd "$(dirname "$0")/.."
    pwd -P
)"
cd "$DIR"

if [ -z "$(git status --untracked-files=no --porcelain)" ]; then
    if [ $(git reflog HEAD | grep 'checkout:' | head -1 | awk '{print $NF}') == master ]; then
          git pull

          DATE="`date +%Y%m%d-%H%M`"
          TAG="v$DATE"

          echo "Tagging release: $TAG"
          git tag -s $TAG -m '$DATE release'
          git push origin $TAG
    else
      >&2 echo
      >&2 echo "ERROR: Cannot deploy new image due to wrong branch. Only master is accepted"
      >&2 echo
      exit 1
    fi
else
  >&2 echo
  >&2 echo "ERROR: Cannot deploy new image due to uncommited Git changes!"
  >&2 echo
  exit 1
fi
