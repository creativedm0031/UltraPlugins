#!/bin/sh
# Builds all plugins and copies each jar into Ultra<Name>/release/.
set -e
cd "$(dirname "$0")/.."
mvn -q clean package
for dir in Ultra*/; do
  name="${dir%/}"
  [ "$name" = "UltraCore" ] && continue
  rm -f "$name"/release/"$name"-*.jar
  cp "$name"/target/"$name"-*.jar "$name"/release/
  echo "$name/release/$(basename "$name"/target/"$name"-*.jar)"
done
