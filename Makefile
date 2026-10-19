.PHONY: init check-env check-jdk dev jar dist clean rebuild checkstyle test format deps-update plugins-update update deps-upgrade docker-verify docker-smoke userguide

# Maven version pinned by the wrapper. Source of truth for `make init`; the
# generated .mvn/wrapper/maven-wrapper.properties carries it to ./mvnw. The
# checksum is the SHA-256 of apache-maven-$(MAVEN_VERSION)-bin.zip, so init
# regenerates the same file - wrapper properties included - without dropping it.
MAVEN_VERSION := 3.10.0
MAVEN_DISTRIBUTION_SHA256 := 1f6d9909266510f039f59aa0e13dcd2c66da85f043e56276e41f2918f8bddaff

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
	@mvn -N -B org.apache.maven.plugins:maven-wrapper-plugin:wrapper \
		-Dmaven=$(MAVEN_VERSION) \
		-DdistributionSha256Sum=$(MAVEN_DISTRIBUTION_SHA256)

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

# -q keeps the per-class noise out, but it also swallows the summary: print the
# totals from the reports of this run, so a green run shows what it ran.
test: check-env check-jdk
	@rm -rf target/surefire-reports; \
	./mvnw -q -B test; \
	status=$$?; \
	reports=$$(ls target/surefire-reports/*.txt 2>/dev/null | grep -v '[-]output[.]txt'); \
	if [ -n "$$reports" ]; then \
		awk -F'[:,]' '/Tests run:/ {t+=$$2; f+=$$4; e+=$$6; s+=$$8} END {printf "Tests run: %d, Failures: %d, Errors: %d, Skipped: %d\n", t, f, e, s}' $$reports; \
	else \
		echo "No test report in target/surefire-reports"; \
	fi; \
	exit $$status

# clean and package in one Maven invocation: expressed as two prerequisites
# (clean, dist) a parallel make runs them concurrently, and clean then deletes
# target/classes while javac is compiling - which surfaces as "bad class file
# ... NoSuchFileException" for the main classes and "cannot find symbol" for
# every test stub.
rebuild: check-env check-jdk
	@./mvnw -B clean package

# Docker release checks, see docker/README.md. `docker-verify` only needs a
# Docker daemon; `docker-smoke` also pulls the amd64 Gateway image and drives
# the Gateway installed in it with the IBC built from this tree.
docker-verify:
	@docker compose run --rm --build verify

docker-smoke:
	@docker compose run --rm --build smoke

# Regenerates resources/userguide.pdf from docs/userguide.md. pandoc and a LaTeX
# engine (tectonic) are documentation tooling, not part of the Java build, so no
# other target depends on this one.
userguide:
	@command -v pandoc >/dev/null 2>&1 || { \
		echo "Error: 'pandoc' is not on PATH."; \
		echo "  brew install pandoc tectonic"; \
		exit 1; \
	}
	@pandoc docs/userguide.md -o resources/userguide.pdf \
		--pdf-engine=tectonic \
		-H docs/userguide-header.tex \
		-V documentclass=article \
		-V fontsize=10pt \
		-V geometry:margin=1in \
		-V colorlinks=true \
		-V linkcolor=RoyalBlue \
		-V urlcolor=RoyalBlue

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

# Any version that is not a release: milestones, alphas, betas, release candidates,
# early access, snapshots, previews. Case-insensitive and separator-tolerant, so
# 3.6.0-M1, 4.0.0-rc-2 and 1.2.0.beta3 are all matched.
PRERELEASE := ([-.](alpha|beta|rc|cr|m|ea|snapshot|preview)[-._]?[0-9])

# Prints a versions:* report with the build boilerplate removed and every pre-release
# candidate dropped. The goal's own -DignoredVersions is accepted and ignored by the
# plugin (measured on versions-maven-plugin 2.23.0: -DignoredVersions=4.0.0-beta-1
# still lists it), so the filtering is done on the report text.
# $1 = Maven goal to run.
VERSIONS_REPORT = report=$$(mktemp) && \
	{ ./mvnw -B $(1) > $$report 2>&1 || { cat $$report; rm -f $$report; exit 1; }; } && \
	sed -n 's/^\[INFO\] //p' $$report \
		| grep -vE '^([[:space:]]*from pom\.xml|Scanning for projects|Building |---|BUILD |Total time|Finished at|Loaded [0-9]+ auto-discovered prefixes|Download(ing|ed) (from|to)|Progress \(|$$)' \
		| grep -viE '$(PRERELEASE)'; \
	rm -f $$report

deps-update:
	@$(call VERSIONS_REPORT,versions:display-dependency-updates)

plugins-update:
	@$(call VERSIONS_REPORT,versions:display-plugin-updates)

update: deps-update plugins-update

# Rewrites pom.xml in place (dependencies and plugins). Review the diff: this is
# the only target that mutates a tracked file. It cannot land a pre-release: the
# goal resolves Maven's "release" metadata, which excludes alpha/beta/rc (measured
# on maven-clean-plugin, whose 4.0.0-beta-* are never proposed here).
deps-upgrade:
	@./mvnw versions:use-latest-releases \
		-DallowSnapshots=false \
		-DignoredVersions="$(PRERELEASE)" \
		-DprocessDependencies=true \
		-DprocessPlugins=true
