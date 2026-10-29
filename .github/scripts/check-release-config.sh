#!/usr/bin/env bash
# Checks that release-please-config.json, .release-please-manifest.json and the modules agree with each other.
set -euo pipefail

status=0
fail() { echo "::error::$1"; status=1; }

config_modules="$(jq -r '.packages | keys[]' release-please-config.json | sort)"
manifest_modules="$(jq -r 'keys[]' .release-please-manifest.json | sort)"
[ "$config_modules" = "$manifest_modules" ] ||
  fail "release-please-config.json and .release-please-manifest.json must list the same modules"

for module in $config_modules; do
  properties="$module/gradle.properties"
  if ! grep -q 'x-release-please-start-version' "$properties" 2>/dev/null; then
    fail "$properties must declare its version between release-please markers (see docs/PUBLISHING.md)"
    continue
  fi
  declared="$(sed -n 's/^version=//p' "$properties")"
  released="$(jq -r --arg m "$module" '.[$m]' .release-please-manifest.json)"
  [ "$declared" = "$released" ] ||
    fail "$properties declares version $declared but .release-please-manifest.json says $released"
  if [ "$module" != "plugins" ] && ! grep -q 'com.infomaniak.multiplatform_core.convention.publish' "$module/build.gradle.kts"; then
    fail "$module must apply the com.infomaniak.multiplatform_core.convention.publish plugin"
  fi
  publish_npm="$(jq -r --arg m "$module" '.packages[$m]["publish-npm"] // false' release-please-config.json)"
  case "$publish_npm" in
    false) ;;
    true)
      if [ "$module" = "plugins" ] || ! grep -Eq '^\s*js\s*[({]' "$module/build.gradle.kts"; then
        fail "$module sets \"publish-npm\": true but has no Kotlin/JS target"
      fi
      ;;
    *) fail "$module: \"publish-npm\" must be true or false" ;;
  esac
done

exit $status
