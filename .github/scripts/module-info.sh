#!/usr/bin/env bash
# Prints the publishing metadata of a module of release-please-config.json as "key=value" lines
# (meant to be appended to $GITHUB_OUTPUT). Fails if the module is not enabled for publishing.
# Usage: module-info.sh <module path, e.g. features/Sample>
set -euo pipefail

module="${1%/}"
config="release-please-config.json"

if ! jq -e --arg m "$module" '.packages | has($m)' "$config" > /dev/null; then
  echo "::error::Module '$module' is not enabled for publishing. Enabled modules: $(jq -r '.packages | keys | join(", ")' "$config")" >&2
  exit 1
fi

gradle_project=":${module//\//:}"
group_path="com/infomaniak/multiplatform_core"

if [ "$module" = "plugins" ]; then
  artifact_line='id("com.infomaniak.multiplatform_core.plugins.<plugin>") version "{VERSION}"'
  npm=false
else
  artifact_id="$(basename "$module" | tr '[:upper:]' '[:lower:]')"
  artifact_line="implementation(\"com.infomaniak.multiplatform_core:${artifact_id}:{VERSION}\")"
  # A module is published to npm when it declares a Kotlin/JS target.
  if grep -Eq '^\s*js\s*[({]' "$module/build.gradle.kts"; then npm=true; else npm=false; fi
fi

echo "module=$module"
echo "component=$(jq -r --arg m "$module" '.packages[$m].component' "$config")"
echo "gradle-project=$gradle_project"
echo "gradle-task=${gradle_project}:publishAllPublicationsToReposiliteRepository"
echo "version-property=publish.version.$(echo "${gradle_project#:}" | tr ':' '.')"
echo "artifact-line=$artifact_line"
echo "maven-group-path=$group_path"
echo "npm=$npm"
