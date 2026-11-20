#!/usr/bin/env bash
set -u

BASE="${ONYX_BASE:-https://onyx-connect.alyaswkdha31.workers.dev}"
AUTH=()
if [[ -n "${ONYX_TOKEN:-}" ]]; then
  AUTH=(-H "Authorization: Bearer ${ONYX_TOKEN}")
else
  echo "ONYX_TOKEN is unset; running unauthenticated checks."
fi

echo "ONYX CONNECT API checks (status and response length only)"
for path in / /v1/jobs /v1/status /health; do
  metrics="$(curl -sS -o /tmp/onyx_api_body.$$ -w '%{http_code} %{size_download}' "${BASE}${path}" "${AUTH[@]}" 2>/dev/null || printf '000 0')"
  printf '%-12s HTTP/bytes: %s\n' "${path}" "${metrics}"
done
rm -f /tmp/onyx_api_body.$$

echo
echo "OPTIONS /v1/jobs (status and response length only):"
metrics="$(curl -sS -X OPTIONS -o /tmp/onyx_api_options.$$ -w '%{http_code} %{size_download}' "${BASE}/v1/jobs" \
  -H 'Origin: https://chat.openai.com' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: authorization,content-type' \
  "${AUTH[@]}" 2>/dev/null || printf '000 0')"
printf 'OPTIONS /v1/jobs HTTP/bytes: %s\n' "${metrics}"
rm -f /tmp/onyx_api_options.$$
