.PHONY: init check-env dev jar dist clean rebuild format deps-update plugins-update update deps-upgrade

# Maven version pinned by the wrapper. Source of truth for `make init`; the
# generated .mvn/wrapper/maven-wrapper.properties carries it to ./mvnw.
MAVEN_VERSION := 3.9.16

# Install/refresh the Maven wrapper at MAVEN_VERSION. Needs a system `mvn` to
# run the generator; afterwards every other target goes through ./mvnw.
init:
	@mvn -N -B org.apache.maven.plugins:maven-wrapper-plugin:wrapper -Dmaven=$(MAVEN_VERSION)

check-env:
	@test -n "$$IBC_BIN" || (echo "Error: environment variable 'IBC_BIN' not set (set it to the directory containing the TWS/Gateway jar files)" && exit 1)

dev: check-env
	@./mvnw -q package -DskipAssembly=true

jar: check-env
	@./mvnw -q package -DskipAssembly=true

dist: check-env
	@./mvnw -q package

clean:
	@./mvnw -q clean

rebuild: clean dist

format:
	@prettier --write "**/*.md" "**/*.json" --ignore-path .gitignore

# Pre-release filter shared by every versions:* target. Case-insensitive ((?i))
# and tolerant of separators, so milestones such as 3.6.0-M1 and release
# candidates such as 4.0.0-rc-2 are never proposed as upgrades.
PRERELEASE_FILTER := (?i).*-(alpha|beta|rc|cr|m)[-._]?[0-9]*.*

deps-update:
	@./mvnw versions:display-dependency-updates \
		-DallowSnapshots=false \
		-DignoredVersions="$(PRERELEASE_FILTER)"

plugins-update:
	@./mvnw versions:display-plugin-updates \
		-DallowSnapshots=false \
		-DignoredVersions="$(PRERELEASE_FILTER)"

update: deps-update plugins-update

deps-upgrade:
	@./mvnw versions:use-latest-releases \
		-DallowSnapshots=false \
		-DignoredVersions="$(PRERELEASE_FILTER)" \
		-DprocessDependencies=true \
		-DprocessPlugins=true
