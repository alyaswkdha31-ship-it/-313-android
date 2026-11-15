#!/usr/bin/env python3
"""stdio MCP server for the existing ONYX CONNECT jobs API."""

from __future__ import annotations

import os
import time
from typing import Any

import requests
from mcp.server.fastmcp import FastMCP

BASE = os.environ.get("ONYX_BASE", "").strip().rstrip("/")
TOKEN = os.environ.get("ONYX_TOKEN", "").strip()
if not BASE or not TOKEN:
    raise RuntimeError(
        "Missing ONYX_BASE or ONYX_TOKEN. Set both as environment variables; "
        "never place the token in source code."
    )

SESSION = requests.Session()
SESSION.headers.update({
    "Authorization": f"Bearer {TOKEN}",
    "Accept": "application/json",
    "Content-Type": "application/json",
})
HTTP_TIMEOUT = 15
POLL_INTERVAL = 2
JOB_TIMEOUT = 60

mcp = FastMCP("onyx-connect")


def _decode_response(response: requests.Response) -> dict[str, Any]:
    if response.status_code == 401:
        raise RuntimeError("unauthorized: تحقق من ONYX_TOKEN")
    if response.status_code == 404:
        raise RuntimeError("job not found")
    try:
        payload = response.json()
    except ValueError as exc:
        raise RuntimeError(f"Invalid JSON response (HTTP {response.status_code})") from exc
    if response.status_code >= 400:
        message = payload.get("error", f"HTTP {response.status_code}")
        raise RuntimeError(str(message))
    if not isinstance(payload, dict):
        raise RuntimeError("Invalid API response: expected an object")
    return payload


def _submit(job_type: str) -> dict[str, Any]:
    response = SESSION.post(
        f"{BASE}/v1/jobs",
        json={"type": job_type},
        timeout=HTTP_TIMEOUT,
    )
    return _decode_response(response)


def _get_job(job_id: str) -> dict[str, Any]:
    response = SESSION.get(
        f"{BASE}/v1/jobs/{job_id}",
        timeout=HTTP_TIMEOUT,
    )
    return _decode_response(response)


@mcp.tool()
def onyx_submit_job(type: str) -> str:
    """Submit a supported ONYX worker job and return its job ID."""
    payload = _submit(type)
    job = payload.get("job")
    if not payload.get("ok") or not isinstance(job, dict) or not job.get("id"):
        raise RuntimeError("Gateway did not return a valid job ID")
    return str(job["id"])


@mcp.tool()
def onyx_get_job(job_id: str) -> dict[str, Any]:
    """Get the status and result envelope for an ONYX job."""
    return _get_job(job_id)


@mcp.tool()
def onyx_device_info() -> dict[str, Any]:
    """Request device.info and wait up to 60 seconds for its result."""
    submitted = _submit("device.info")
    job = submitted.get("job")
    if not submitted.get("ok") or not isinstance(job, dict) or not job.get("id"):
        raise RuntimeError("Gateway did not return a valid job ID")
    job_id = str(job["id"])
    deadline = time.monotonic() + JOB_TIMEOUT
    while time.monotonic() < deadline:
        envelope = _get_job(job_id)
        current = envelope.get("job")
        if not isinstance(current, dict):
            raise RuntimeError("Invalid job envelope")
        status = current.get("status")
        if status == "done":
            return current
        if status == "error":
            return current
        time.sleep(POLL_INTERVAL)
    return {"ok": False, "error": "timeout", "job_id": job_id}


@mcp.tool()
def onyx_worker_status() -> dict[str, int]:
    """Check gateway authentication response without sending a token; returns HTTP status only."""
    response = requests.get(
        f"{BASE}/v1/jobs/test-id",
        headers={"Accept": "application/json"},
        timeout=HTTP_TIMEOUT,
    )
    return {"http_status": response.status_code}


if __name__ == "__main__":
    mcp.run(transport="stdio")
