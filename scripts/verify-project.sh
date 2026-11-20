#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:---full}"
if [[ "$MODE" == "--static" ]]; then
  python3 "$ROOT/scripts/verify-static.py"
  exit $?
fi
if [[ "$MODE" != "--full" ]]; then echo "Usage: $0 [--static|--full]" >&2; exit 2; fi
for tool in java mvn node npm; do command -v "$tool" >/dev/null || { echo "Missing required tool: $tool" >&2; exit 2; }; done
[[ -d "$ROOT/frontend/node_modules" ]] || { echo 'Run npm install in frontend first.' >&2; exit 2; }
[[ -d "$ROOT/blockchain/node_modules" ]] || { echo 'Run npm install in blockchain first.' >&2; exit 2; }
python3 "$ROOT/scripts/verify-static.py"
(cd "$ROOT/backend" && mvn --batch-mode clean verify)
(cd "$ROOT/frontend" && npm run build)
(cd "$ROOT/blockchain" && npm run compile && npm test)
echo 'Full local build/test suite completed. This does not replace live MongoDB/Hardhat/MetaMask acceptance tests.'
