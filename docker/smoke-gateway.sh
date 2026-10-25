#!/bin/bash
# End-to-end smoke test: drive the Gateway installed in this image with the
# freshly built IBC, and require IBC to reach the login step.
#
# Credentials are deliberately invalid: the test proves the release wires up
# (launcher -> jar -> Gateway), not that an IB account can log in. It fails if
# IBC never submits a login, or if it dies on a stub class.
set -uo pipefail

ibc_path=/opt/ibc
gateway_home=/home/trader
log_file=/tmp/smoke/ibc-run.log
timeout_seconds=${SMOKE_TIMEOUT:-240}
failures=0

ok() { printf '  ok    %s\n' "$1"; }
ko() { printf '  FAIL  %s\n' "$1"; failures=$((failures + 1)); }

echo "== IBC gateway smoke test =="

gateway_version=$(basename "$(ls -d "${gateway_home}"/Jts/ibgateway/*/ | head -n 1)")
ibc_version=$(cat "${ibc_path}/version")
if [[ -z "${gateway_version}" ]]; then
    echo "no Gateway installation under ${gateway_home}/Jts/ibgateway"
    echo "RESULT: FAIL"
    exit 1
fi
echo "  gateway ${gateway_version}, IBC ${ibc_version}"

mkdir -p "${gateway_home}/ibc/logs" /tmp/smoke
cp "${ibc_path}/config.ini" "${gateway_home}/ibc/config.ini"

Xvfb :99 -screen 0 1280x1024x24 >/tmp/smoke/xvfb.log 2>&1 &
xvfb_pid=$!
trap 'kill "${xvfb_pid}" 2>/dev/null' EXIT
export DISPLAY=:99
sleep 2

echo "  starting IBC (timeout ${timeout_seconds}s)"
timeout "${timeout_seconds}" bash "${ibc_path}/scripts/ibcstart.sh" "${gateway_version}" -g \
    --tws-path="${gateway_home}/Jts" \
    --ibc-path="${ibc_path}" \
    --ibc-ini="${gateway_home}/ibc/config.ini" \
    --mode=paper \
    --user=ibc-smoke-test --pw=not-a-real-password >"${log_file}" 2>&1 &
run_pid=$!

# Poll instead of waiting out the timeout: the run is supposed to end with IBC
# parked on the login dialog, which never exits on its own.
deadline=$((SECONDS + timeout_seconds))
while (( SECONDS < deadline )); do
    if grep -q 'Login attempt' "${log_file}" 2>/dev/null; then
        break
    fi
    if ! kill -0 "${run_pid}" 2>/dev/null; then
        break
    fi
    sleep 2
done

sleep 5
kill "${run_pid}" 2>/dev/null
pkill -f 'ibcalpha.ibc' 2>/dev/null
wait "${run_pid}" 2>/dev/null

# ---------------------------------------------------------------- assertions
if grep -q "version: ${ibc_version}" "${log_file}"; then
    ok "IBC reports the version stamped in the release (${ibc_version})"
else
    ko "IBC reports the version stamped in the release (${ibc_version})"
fi

if grep -q 'Location of java executable=' "${log_file}"; then
    ok "launcher resolved a java executable ($(grep -m1 'Location of java executable=' "${log_file}" | cut -d= -f2-))"
else
    ko "launcher resolved a java executable"
fi

if grep -q 'Login attempt' "${log_file}"; then
    ok "IBC reached the login step"
else
    ko "IBC reached the login step"
fi

if grep -aq 'Unresolved compilation problem' "${log_file}" "${ibc_path}"/IBC.jar; then
    ko "no IDE stub class in the packaged jar"
else
    ok "no IDE stub class in the packaged jar"
fi

if grep -q 'An exception has occurred' "${log_file}"; then
    ko "no uncaught IBC exception"
else
    ok "no uncaught IBC exception"
fi

echo "---- last 15 lines of ${log_file} ----"
tail -n 15 "${log_file}"

if [[ ${failures} -eq 0 ]]; then
    echo "RESULT: PASS"
    exit 0
fi

echo "RESULT: FAIL (${failures} check(s))"
exit 1
