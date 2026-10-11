#!/usr/bin/env bash
set -euo pipefail
task_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$task_root"
if [[ -n $(git status --porcelain --untracked-files=all | python3 -c 'import sys; from tools.package_release import source_allowed; print("".join(x for x in sys.stdin if source_allowed(x[3:].strip())))') ]]; then
    echo 'Commit source changes before producing a release.' >&2
    exit 1
fi
export SILENTCAMERA_SOURCE_COMMIT=$(git rev-parse HEAD)
./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/package_release.py
