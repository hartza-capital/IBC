# IBC Build System Makefile
# Requires: Java 8+, Ant, Ivy
# Environment: IBC_BIN must be set to TWS jar files directory

# Variables
VERSION := 3.21.2a
SRC_DIR := src
RESOURCES_DIR := resources
TARGET_DIR := target
DIST_DIR := dist
LIB_DIR := lib

# Default target
.PHONY: all
all: dist

# Clean build artifacts
.PHONY: clean
clean:
	@echo "Cleaning build artifacts..."
	ant clean

# Resolve dependencies via Ivy
.PHONY: deps
deps:
	@echo "Resolving dependencies..."
	ant resolve-deps

# Initialize version info
.PHONY: init
init:
	@echo "Updating version info..."
	ant init

# Compile Java sources
.PHONY: compile
compile: deps init
	@echo "Compiling Java sources..."
	ant compile

# Build JAR file
.PHONY: jar
jar: compile
	@echo "Building JAR..."
	ant jar

# Create distribution packages
.PHONY: dist
dist: jar
	@echo "Creating distribution packages..."
	ant dist

# Build everything from scratch
.PHONY: rebuild
rebuild: clean dist

# Check environment
.PHONY: check-env
check-env:
	@echo "Checking environment..."
	@if [ -z "$$IBC_BIN" ]; then \
		echo "ERROR: IBC_BIN environment variable not set"; \
		echo "Set it to the directory containing TWS jar files"; \
		exit 1; \
	fi
	@echo "IBC_BIN: $$IBC_BIN"
	@echo "Java version: $$(java -version 2>&1 | head -1)"
	@echo "Ant version: $$(ant -version 2>&1 | head -1)"

# Rebase on upstream repository
.PHONY: rebase-upstream
rebase-upstream:
	@echo "Setting up upstream remote..."
	@git remote | grep -q '^upstream$$' || git remote add upstream git@github.com:IbcAlpha/IBC.git
	@echo "Fetching upstream repository..."
	git fetch upstream
	@echo "Rebasing master on upstream/master..."
	git rebase upstream/master
	@echo "Rebase complete!"

# Install built JAR to resources
.PHONY: install
install: jar
	@echo "JAR installed to $(RESOURCES_DIR)/IBC.jar"

# Quick build for development
.PHONY: dev
dev: compile jar
	@echo "Development build complete"

# Show build status
.PHONY: status
status:
	@echo "=== IBC Build Status ==="
	@echo "Version: $(VERSION)"
	@echo "Source dir: $(SRC_DIR)"
	@echo "Target dir: $(TARGET_DIR)"
	@echo "Dist dir: $(DIST_DIR)"
	@echo "========================"
	@if [ -f "$(RESOURCES_DIR)/IBC.jar" ]; then \
		echo "JAR exists: $(RESOURCES_DIR)/IBC.jar"; \
		ls -la "$(RESOURCES_DIR)/IBC.jar"; \
	else \
		echo "JAR not found: $(RESOURCES_DIR)/IBC.jar"; \
	fi
	@if [ -d "$(DIST_DIR)" ]; then \
		echo "Distribution files:"; \
		ls -la "$(DIST_DIR)"/; \
	else \
		echo "No distribution files found"; \
	fi

# Help target
.PHONY: help
help:
	@echo "IBC Build System"
	@echo "================"
	@echo ""
	@echo "Available targets:"
	@echo "  all         - Build everything (default)"
	@echo "  clean       - Remove build artifacts"
	@echo "  deps        - Resolve dependencies"
	@echo "  compile     - Compile Java sources"
	@echo "  jar         - Build JAR file"
	@echo "  dist        - Create distribution packages"
	@echo "  rebuild     - Clean and build everything"
	@echo "  dev         - Quick development build"
	@echo "  install     - Install JAR to resources"
	@echo "  status      - Show build status"
	@echo "  check-env   - Check environment setup"
	@echo "  rebase-upstream - Rebase on upstream IbcAlpha/IBC repository"
	@echo "  help        - Show this help"
	@echo ""
	@echo "Environment variables:"
	@echo "  IBC_BIN     - Directory containing TWS jar files (required)"
