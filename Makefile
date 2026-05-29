.PHONY: check-env dev jar dist clean rebuild format

check-env:
	@test -n "$$IBC_BIN" || (echo "Error: environment variable 'IBC_BIN' not set (set it to the directory containing the TWS/Gateway jar files)" && exit 1)

dev: check-env
	mvn -q package -DskipAssembly=true

jar: check-env
	mvn -q package -DskipAssembly=true

dist: check-env
	mvn -q package

clean:
	mvn -q clean

rebuild: clean dist

format:
	@prettier --write "**/*.md" "**/*.json" --ignore-path .gitignore
