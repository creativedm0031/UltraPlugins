#!/bin/sh
# Usage: tools/mkconfig.sh Spawn < body.yml   -> writes Ultra<Name>/src/main/resources/config.yml
id="ultra$(echo "$1" | tr 'A-Z' 'a-z')"
out="$(dirname "$0")/../Ultra$1/src/main/resources/config.yml"
sed "s/%ID%/$id/" "$(dirname "$0")/config-header.yml" > "$out"
cat >> "$out"
