.PHONY: check-env dev jar dist clean rebuild format deps-update deps-upgrade

check-env:
	@test -n "$$IBC_BIN" || (echo "Error: environment variable 'IBC_BIN' not set (set it to the directory containing the TWS/Gateway jar files)" && exit 1)

dev: check-env
	@mvn -q package -DskipAssembly=true

jar: check-env
	@mvn -q package -DskipAssembly=true

dist: check-env
	@mvn -q package

clean:
	@mvn -q clean

rebuild: clean dist

format:
	@prettier --write "**/*.md" "**/*.json" --ignore-path .gitignore

deps-update:
	@mvn versions:display-dependency-updates versions:display-plugin-updates \
		-DallowSnapshots=false \
		-DignoredVersions=".*[-.]alpha.*,.*[-.]beta.*,.*[-.]rc[0-9]*,.*[-.]cr[0-9]*,.*[-.]m[0-9].*"

deps-upgrade:
	@mvn versions:use-latest-releases \
		-DallowSnapshots=false \
		-DignoredVersions=".*[-.]alpha.*,.*[-.]beta.*,.*[-.]rc[0-9]*,.*[-.]cr[0-9]*,.*[-.]m[0-9].*" \
		-DprocessDependencies=true \
		-DprocessPlugins=true
