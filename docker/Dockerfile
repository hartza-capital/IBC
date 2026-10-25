# syntax=docker/dockerfile:1

# Build stages for the IBC release, plus two ways of proving the result:
# `verify` checks the shipped ZIP's structure, `gateway-smoke` drives the
# Gateway already installed in the runtime image with the freshly built IBC.
#
# The TWS/Gateway jars IBC compiles against are harvested from the published
# runtime image: it carries the exact Gateway build the release is meant to
# drive, and using it spares the build the installer's license click-through.
ARG GATEWAY_IMAGE=quay.io/hartza-capital/ib-gateway:stable
ARG JDK_IMAGE=eclipse-temurin:25-jdk
ARG JRE_IMAGE=eclipse-temurin:25-jre

# ---------------------------------------------------------------------------
# Compile classpath: the Gateway jars, copied out of the runtime image.
# This stage is linux/amd64 (the Gateway image has no arm64 build); only the
# jars cross into the next stage, so nothing here runs on the host platform.
# ---------------------------------------------------------------------------
FROM ${GATEWAY_IMAGE} AS gateway-jars
USER root
RUN mkdir -p /opt/ibc-bin \
 && cp /home/trader/Jts/ibgateway/*/jars/*.jar /opt/ibc-bin/ \
 && echo "harvested $(ls /opt/ibc-bin | wc -l) Gateway jars"

# ---------------------------------------------------------------------------
# Build: JDK 25 + Maven wrapper, exactly what the pom requires.
# ---------------------------------------------------------------------------
FROM ${JDK_IMAGE} AS build
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl unzip \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
COPY --from=gateway-jars /opt/ibc-bin /opt/ibc-bin
ENV IBC_BIN=/opt/ibc-bin

COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
COPY src/ src/
COPY test/ test/
COPY resources/ resources/
COPY COPYING.txt ./

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -DskipTests package

# Unpack here, where unzip exists: the runtime image has no unzip.
RUN mkdir -p /opt/ibc-dist \
 && unzip -q /workspace/dist/IBCLinux-*.zip -d /opt/ibc-dist \
 && chmod +x /opt/ibc-dist/*.sh /opt/ibc-dist/scripts/*.sh \
 && ls -l /opt/ibc-dist

# ---------------------------------------------------------------------------
# verify: structural checks on the ZIP, run on a plain JRE.
# ---------------------------------------------------------------------------
FROM ${JRE_IMAGE} AS verify
RUN apt-get update \
 && apt-get install -y --no-install-recommends bash unzip \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/dist /dist
COPY docker/verify-dist.sh /usr/local/bin/verify-dist.sh
RUN chmod +x /usr/local/bin/verify-dist.sh
ENTRYPOINT ["/usr/local/bin/verify-dist.sh"]

# ---------------------------------------------------------------------------
# gateway-smoke: replace the image's IBC with the freshly built one and drive
# the installed Gateway under Xvfb until IBC submits a login.
# ---------------------------------------------------------------------------
FROM ${GATEWAY_IMAGE} AS gateway-smoke
USER root
RUN rm -rf /opt/ibc && mkdir -p /opt/ibc
COPY --from=build /opt/ibc-dist/ /opt/ibc/
COPY docker/smoke-gateway.sh /usr/local/bin/smoke-gateway.sh
RUN chmod +x /usr/local/bin/smoke-gateway.sh \
 && chown -R trader:trader /opt/ibc /usr/local/bin/smoke-gateway.sh
USER trader
ENV DISPLAY=:99
ENTRYPOINT ["/usr/local/bin/smoke-gateway.sh"]
