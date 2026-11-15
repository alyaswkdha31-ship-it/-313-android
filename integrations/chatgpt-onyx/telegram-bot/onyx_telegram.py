#!/usr/bin/env python3
"""Telegram long-polling bridge to the existing ONYX CONNECT jobs API."""

from __future__ import annotations

import os
import sys
import time
from typing import Any

import requests

TELEGRAM_BOT_TOKEN = os.environ.get("TELEGRAM_BOT_TOKEN", "").strip()
TELEGRAM_CHAT_ID = os.environ.get("TELEGRAM_CHAT_ID", "").strip()
ONYX_TOKEN = os.environ.get("ONYX_TOKEN", "").strip()
ONYX_BASE = os.environ.get("ONYX_BASE", "").strip().rstrip("/")

missing = [
    name for name, value in (
        ("TELEGRAM_BOT_TOKEN", TELEGRAM_BOT_TOKEN),
        ("TELEGRAM_CHAT_ID", TELEGRAM_CHAT_ID),
        ("ONYX_TOKEN", ONYX_TOKEN),
        ("ONYX_BASE", ONYX_BASE),
    ) if not value
]
if missing:
    print("Missing required environment variable(s): " + ", ".join(missing), file=sys.stderr)
    raise SystemExit(2)

telegram = requests.Session()
gateway = requests.Session()
gateway.headers.update({
    "Authorization": f"Bearer {ONYX_TOKEN}",
    "Accept": "application/json",
    "Content-Type": "application/json",
})
TG_URL = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}"
HTTP_TIMEOUT = 40
JOB_TIMEOUT = 210


def send_message(chat_id: str, text: str) -> None:
    try:
        response = telegram.post(
            f"{TG_URL}/sendMessage",
            json={"chat_id": chat_id, "text": text[:4000]},
            timeout=20,
        )
        response.raise_for_status()
    except requests.RequestException:
        # Do not log request URLs: Telegram bot URLs contain the bot token.
        print("Telegram sendMessage failed.", file=sys.stderr)


def api_json(response: requests.Response) -> dict[str, Any]:
    if response.status_code == 401:
        raise RuntimeError("تعذرت المصادقة: تحقق من إعداد ONYX_TOKEN.")
    if response.status_code == 404:
        raise RuntimeError("لم يتم العثور على المهمة.")
    try:
        data = response.json()
    except ValueError as exc:
        raise RuntimeError(f"استجابة غير مفهومة من البوابة (HTTP {response.status_code}).") from exc
    if response.status_code >= 400:
        raise RuntimeError(str(data.get("error", f"خطأ HTTP {response.status_code}")))
    if not isinstance(data, dict):
        raise RuntimeError("استجابة البوابة غير صالحة.")
    return data


def submit_and_wait(user_text: str) -> dict[str, Any]:
    if not user_text.strip() or len(user_text) > 12000:
        raise RuntimeError("الرسالة فارغة أو أطول من الحد المسموح.")

    response = gateway.post(
        f"{ONYX_BASE}/v1/jobs",
        json={
            "type": "313.chat",
            "payload": {
                "messages": [{"role": "user", "content": user_text.strip()}]
            },
        },
        timeout=15,
    )
    envelope = api_json(response)
    job_id = envelope.get("id")
    if not envelope.get("ok") or not isinstance(job_id, str) or not job_id:
        raise RuntimeError("لم تُرجع البوابة رقم المهمة.")

    deadline = time.monotonic() + JOB_TIMEOUT
    while time.monotonic() < deadline:
        response = gateway.get(f"{ONYX_BASE}/v1/jobs/{job_id}", timeout=15)
        current_envelope = api_json(response)
        current = current_envelope.get("job")
        if not isinstance(current, dict):
            raise RuntimeError("بيانات المهمة غير صالحة.")
        status = current.get("status")
        if status in ("done", "failed", "error"):
            return current
        time.sleep(2)
    return {"id": job_id, "status": "timeout"}


def format_result(job: dict[str, Any]) -> str:
    status = job.get("status")
    if status == "done":
        result = job.get("result")
        if isinstance(result, dict) and isinstance(result.get("answer"), str):
            return result["answer"]
        return "اكتملت المهمة، لكن لم يصل نص الرد المتوقع."
    if status in ("failed", "error"):
        return "تعذر تنفيذ الطلب:\n" + str(job.get("error") or "سبب غير محدد")
    if status == "timeout":
        return "انتهت مهلة انتظار رد 313. رقم المهمة: " + str(job.get("id"))
    return "حالة المهمة: " + str(status)


def main() -> None:
    print("ONYX Telegram bot started. Authorized chat ID is configured.")
    offset = 0
    while True:
        try:
            response = telegram.get(
                f"{TG_URL}/getUpdates",
                params={"timeout": 30, "offset": offset, "allowed_updates": ["message"]},
                timeout=HTTP_TIMEOUT,
            )
            response.raise_for_status()
            data = response.json()
            if not data.get("ok"):
                time.sleep(2)
                continue
            for update in data.get("result", []):
                offset = max(offset, int(update.get("update_id", 0)) + 1)
                message = update.get("message") or {}
                chat = message.get("chat") or {}
                chat_id = str(chat.get("id", ""))
                if chat_id != TELEGRAM_CHAT_ID:
                    continue
                text = message.get("text")
                if not isinstance(text, str) or not text.strip():
                    continue
                try:
                    job = submit_and_wait(text)
                    send_message(chat_id, format_result(job))
                except (requests.RequestException, RuntimeError) as exc:
                    send_message(chat_id, "خطأ أثناء تنفيذ الطلب: " + str(exc))
        except requests.RequestException:
            # Do not print exception strings: they can include token-bearing URLs.
            print("Telegram polling connection failed; retrying.", file=sys.stderr)
            time.sleep(3)
        except (ValueError, TypeError, KeyError):
            print("Received an invalid Telegram update; continuing.", file=sys.stderr)
            time.sleep(1)


if __name__ == "__main__":
    main()
