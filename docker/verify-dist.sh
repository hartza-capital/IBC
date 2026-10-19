#!/bin/bash
# Structural checks on the IBC release ZIP produced by the build stage.
#
# Everything asserted here is what a consumer of IBCLinux-<version>.zip relies
# on: the archive is intact, the launchers kept their executable bit, the
# version stamped inside matches the file name, and the packaged jar is a real
# compile rather than a stub left behind by a half-finished IDE build.
set -uo pipefail

release_dir=/dist
work_dir=/tmp/dist-check
failures=0

ok() { printf '  ok    %s\n' "$1"; }
ko() { printf '  FAIL  %s\n' "$1"; failures=$((failures + 1)); }

echo "== IBC release check =="

# ---------------------------------------------------------------- the archive
mapfile -t archives < <(compgen -G "${release_dir}/IBCLinux-*.zip" || true)
if [[ ${#archives[@]} -ne 1 ]]; then
    ko "exactly one IBCLinux-*.zip in ${release_dir} (found ${#archives[@]})"
    echo "RESULT: FAIL"
    exit 1
fi

archive=${archives[0]}
archive_name=$(basename "${archive}")
archive_version=${archive_name#IBCLinux-}
archive_version=${archive_version%.zip}
ok "archive ${archive_name}"

if unzip -tq "${archive}" >/dev/null 2>&1; then
    ok "archive integrity (unzip -t)"
else
    ko "archive integrity (unzip -t)"
    echo "RESULT: FAIL"
    exit 1
fi

rm -rf "${work_dir}"
if unzip -q "${archive}" -d "${work_dir}"; then
    ok "archive extracts"
else
    ko "archive extracts"
    echo "RESULT: FAIL"
    exit 1
fi

# ------------------------------------------------------------------- contents
required_paths=(
    IBC.jar
    version
    config.ini
    COPYING.txt
    README.txt
    gatewaystart.sh
    twsstart.sh
    scripts/ibcstart.sh
    scripts/displaybannerandlaunch.sh
)
for path in "${required_paths[@]}"; do
    if [[ -f "${work_dir}/${path}" ]]; then
        ok "contains ${path}"
    else
        ko "contains ${path}"
    fi
done

# The launchers abort unless their scripts are executable in the unpacked
# release, so the ZIP must carry the bit (assembly fileMode 0755).
non_executable=$(find "${work_dir}" -name '*.sh' ! -perm -u+x -print)
if [[ -z "${non_executable}" ]]; then
    ok "every .sh is executable"
else
    ko "every .sh is executable (not executable: ${non_executable//$'\n'/ })"
fi

# -------------------------------------------------------------------- version
stamped_version=$(cat "${work_dir}/version" 2>/dev/null)
if [[ "${stamped_version}" = "${archive_version}" ]]; then
    ok "version file matches the archive name (${archive_version})"
else
    ko "version file matches the archive name ('${stamped_version}' vs '${archive_version}')"
fi

# ------------------------------------------------------------------------ jar
unzip -l "${work_dir}/IBC.jar" >/tmp/ibc-jar-list.txt 2>/dev/null
if grep -q 'ibcalpha/ibc/IbcVersionInfo.class' /tmp/ibc-jar-list.txt; then
    ok "IBC.jar carries IbcVersionInfo.class"
else
    ko "IBC.jar carries IbcVersionInfo.class"
fi

# A class compiled by an IDE (Eclipse JDT) while a symbol was unresolvable ships
# with this marker and throws UnresolvedCompilationError at runtime, so the jar
# would start and die immediately. Catch it here instead of in a container.
stub_count=$(unzip -p "${work_dir}/IBC.jar" 'ibcalpha/ibc/*.class' 2>/dev/null | grep -ac 'Unresolved compilation problem')
if [[ "${stub_count}" -eq 0 ]]; then
    ok "IBC.jar has no IDE-generated stub class"
else
    ko "IBC.jar has no IDE-generated stub class (${stub_count} marker(s) found)"
fi

# -------------------------------------------------------------------- launcher
if bash "${work_dir}/scripts/ibcstart.sh" -h >/tmp/ibcstart-help.txt 2>&1 \
    && grep -q 'Usage:' /tmp/ibcstart-help.txt; then
    ok "scripts/ibcstart.sh -h prints its usage"
else
    ko "scripts/ibcstart.sh -h prints its usage"
fi

bash "${work_dir}/scripts/ibcstart.sh" --not-an-option >/tmp/ibcstart-invalid.txt 2>&1
invalid_status=$?
if [[ ${invalid_status} -eq 3 ]] && grep -q 'Invalid parameter' /tmp/ibcstart-invalid.txt; then
    ok "scripts/ibcstart.sh rejects an unknown option (exit ${invalid_status})"
else
    ko "scripts/ibcstart.sh rejects an unknown option (exit ${invalid_status})"
fi

# --------------------------------------------------------------------- result
if [[ ${failures} -eq 0 ]]; then
    echo "RESULT: PASS (${archive_name})"
    exit 0
fi

echo "RESULT: FAIL (${failures} check(s))"
exit 1
