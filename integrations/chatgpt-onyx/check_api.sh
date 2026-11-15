#!/usr/bin/env bash
set -u

BASE="${ONYX_BASE:-https://onyx-connect.alyaswkdha31.workers.dev}"

echo "ONYX CONNECT public API checks (no token)"
echo
echo "GET /v1/jobs (expected 401 or 405):"
curl -sS -o /dev/null -w 'HTTP %{http_code}\n' "${BASE}/v1/jobs" || true

echo
echo "OPTIONS /v1/jobs CORS preflight headers:"
curl -sS -i -X OPTIONS "${BASE}/v1/jobs" \
  -H 'Origin: https://chat.openai.com' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: authorization,content-type' \
  -o /dev/stderr -w 'HTTP %{http_code}\n' 2>&1 || true

echo
for path in / /v1/jobs /v1/status /health; do
  status="$(curl -sS -o /dev/null -w '%{http_code}' "${BASE}${path}" 2>/dev/null || printf '000')"
  printf '%-12s %s\n' "${path}" "${status}"
done
