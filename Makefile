.PHONY: init check-env check-jdk dev jar dist clean rebuild checkstyle test format deps-update plugins-update update deps-upgrade

# Maven version pinned by the wrapper. Source of truth for `make init`; the
# generated .mvn/wrapper/maven-wrapper.properties carries it to ./mvnw.
MAVEN_VERSION := 3.9.16

# The pom compiles with <release>25</release>; an older javac fails with
# "release version 25 not supported" and no hint about the cause.
REQUIRED_JAVA_VERSION := 25

# Where Homebrew installs the keg-only JDK that satisfies REQUIRED_JAVA_VERSION;
# check-jdk quotes it in both shell syntaxes.
HOMEBREW_JDK := /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home

# JDK selection. The ambient JAVA_HOME wins when it is new enough, then a
# keg-only Homebrew openjdk@25 (not on PATH and not in the macOS JVM registry),
# then whatever javac is on PATH, which check-jdk vets. Each recipe imports the
# exported JAVA_HOME, so ./mvnw and Ant's <javac> both see the chosen JDK.
AMBIENT_JAVA_HOME := $(JAVA_HOME)
JDK_CANDIDATES := $(AMBIENT_JAVA_HOME) \
	/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
	/usr/local/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home

JDK_HOME := $(shell for home in $(JDK_CANDIDATES); do \
	[ -x "$$home/bin/javac" ] || continue; \
	java_major=`"$$home/bin/javac" -version 2>&1 | sed 's/^javac //' | cut -d. -f1`; \
	if [ "$$java_major" -ge $(REQUIRED_JAVA_VERSION) ] 2>/dev/null; then echo "$$home"; break; fi; \
	done)

ifneq ($(JDK_HOME),)
export JAVA_HOME := $(JDK_HOME)
endif

# Install/refresh the Maven wrapper at MAVEN_VERSION. Needs a system `mvn` to
# run the generator; afterwards every other target goes through ./mvnw.
init:
	@command -v mvn >/dev/null 2>&1 || { \
		echo "Error: 'mvn' is not on PATH; this target needs a system Maven to generate the wrapper."; \
		echo "  brew install maven   (or run ./mvnw, which needs no system mvn)"; \
		exit 1; \
	}
	@mvn -N -B org.apache.maven.plugins:maven-wrapper-plugin:wrapper -Dmaven=$(MAVEN_VERSION)

check-env:
	@test -n "$$IBC_BIN" || (echo "Error: environment variable 'IBC_BIN' not set (set it to the directory containing the TWS/Gateway jar files)" && exit 1)
	@test -d "$$IBC_BIN" || (echo "Error: IBC_BIN='$$IBC_BIN' is not a directory (point it at the TWS/Gateway jars/ directory of the installed version)" && exit 1)
	@test -n "$$(find "$$IBC_BIN" -maxdepth 1 -name '*.jar' -print -quit 2>/dev/null)" || (echo "Error: IBC_BIN='$$IBC_BIN' contains no .jar files (point it at the TWS/Gateway jars/ directory)" && exit 1)

# Resolves the javac the wrapper will use (JAVA_HOME if set, else PATH) and
# fails with an actionable message when it is older than REQUIRED_JAVA_VERSION.
check-jdk:
	@jdk_hint() { \
		echo "  install or select JDK $(REQUIRED_JAVA_VERSION):"; \
		echo "    brew install openjdk@25"; \
		echo "    bash/zsh:  export JAVA_HOME=$(HOMEBREW_JDK)"; \
		echo "    fish:      set -gx JAVA_HOME $(HOMEBREW_JDK)"; \
	}; \
	if [ -n "$$JAVA_HOME" ] && [ -x "$$JAVA_HOME/bin/javac" ]; then \
		javac_bin="$$JAVA_HOME/bin/javac"; \
	else \
		javac_bin=$$(command -v javac 2>/dev/null); \
	fi; \
	if [ -z "$$javac_bin" ] || [ ! -x "$$javac_bin" ]; then \
		echo "Error: no javac found (JAVA_HOME='$$JAVA_HOME')."; \
		jdk_hint; \
		exit 1; \
	fi; \
	java_version=$$("$$javac_bin" -version 2>&1 | sed 's/^javac //'); \
	java_major=$$(echo "$$java_version" | cut -d. -f1); \
	if [ "$$java_major" -lt $(REQUIRED_JAVA_VERSION) ]; then \
		echo "Error: javac $$java_version is too old; the pom compiles with release=$(REQUIRED_JAVA_VERSION)."; \
		jdk_hint; \
		exit 1; \
	fi
	@if [ -n "$(JDK_HOME)" ] && [ "$(JDK_HOME)" != "$(AMBIENT_JAVA_HOME)" ]; then \
		if [ -z "$(AMBIENT_JAVA_HOME)" ]; then \
			echo "make: no JAVA_HOME in the environment; building with $(JDK_HOME)"; \
		else \
			echo "make: JAVA_HOME=$(AMBIENT_JAVA_HOME) is older than $(REQUIRED_JAVA_VERSION); building with $(JDK_HOME)"; \
		fi; \
	fi

dev: check-env check-jdk
	@./mvnw -q package -DskipTests -DskipAssembly=true

# Alias of dev: same artifact, so the recipe lives in one place.
jar: dev

# No -q here: these two run the test phase, and with the test output redirected to
# target/surefire-reports the console keeps Maven's summary and hides the chatter.
dist: check-env check-jdk
	@./mvnw -B package

clean:
	@./mvnw -q clean

# Lint and tests through make, so they inherit the JDK selection above: the
# lefthook pre-commit hook used to call ./mvnw directly and failed with
# "release version 25 not supported" in a shell whose JAVA_HOME is older.
checkstyle: check-jdk
	@./mvnw -q checkstyle:check

test: check-env check-jdk
	@./mvnw -q -B test

# clean and package in one Maven invocation: expressed as two prerequisites
# (clean, dist) a parallel make runs them concurrently, and clean then deletes
# target/classes while javac is compiling - which surfaces as "bad class file
# ... NoSuchFileException" for the main classes and "cannot find symbol" for
# every test stub.
rebuild: check-env check-jdk
	@./mvnw -B clean package

# Both ignore files: .gitignore keeps target/ and dist/ out, .prettierignore is
# what lefthook passes on staged files - using only one of them means `make
# format` and the hook disagree about the file set.
format:
	@command -v prettier >/dev/null 2>&1 || { \
		echo "Error: 'prettier' is not on PATH."; \
		echo "  npm install -g prettier"; \
		exit 1; \
	}
	@prettier --write "**/*.md" "**/*.json" \
		--ignore-path .gitignore --ignore-path .prettierignore

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

# Rewrites pom.xml in place (dependencies and plugins). Review the diff: this is
# the only target that mutates a tracked file.
deps-upgrade:
	@./mvnw versions:use-latest-releases \
		-DallowSnapshots=false \
		-DignoredVersions="$(PRERELEASE_FILTER)" \
		-DprocessDependencies=true \
		-DprocessPlugins=true
