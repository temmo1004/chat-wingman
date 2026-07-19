"""聊天軍師後端 — 實作 docs/api.md 的 POST /api/wingman。

流程：Android 端對截圖做 OCR → 只送對話「文字」上來 → DeepSeek 生成
三風格回覆（聊死指數 + 軍師解說）。圖片不離開手機，這裡只收文字。
DeepSeek API 為 OpenAI 相容格式（純文字，不吃圖，故讀圖在手機端 OCR）。
"""
import json
import os

import requests
from flask import Flask, jsonify, request
from flask_cors import CORS

from prompt import DEMO_RESPONSE, SYSTEM_PROMPT

app = Flask(__name__)
app.json.ensure_ascii = False  # 回傳中文不轉義，方便除錯
CORS(app)

# DeepSeek：OpenAI 相容端點
API_KEY = os.environ.get("DEEPSEEK_API_KEY", "")
API_URL = os.environ.get("DEEPSEEK_URL", "https://api.deepseek.com/chat/completions")
MODEL = os.environ.get("WINGMAN_MODEL", "deepseek-chat")
TIMEOUT = int(os.environ.get("WINGMAN_TIMEOUT", "30"))
STYLES = ["認真", "幽默", "曖昧"]


def _call_deepseek(convo_text):
    """把 OCR 出的對話文字送 DeepSeek，回 dict（已解析）。失敗丟例外。"""
    payload = {
        "model": MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"以下是聊天截圖 OCR 出的對話文字，請依規則回 JSON：\n\n{convo_text}"},
        ],
        "response_format": {"type": "json_object"},
        "temperature": 1.0,
        "max_tokens": 800,
    }
    resp = requests.post(
        API_URL,
        headers={"Authorization": f"Bearer {API_KEY}"},
        json=payload,
        timeout=TIMEOUT,
    )
    resp.raise_for_status()
    content = resp.json()["choices"][0]["message"]["content"]
    return json.loads(content)


def _coerce(data):
    """把模型輸出強制對齊 API 合約：三筆 replies、順序固定、欄位補齊。"""
    replies_in = {r.get("style"): r for r in data.get("replies", []) if isinstance(r, dict)}
    replies = []
    for style in STYLES:
        r = replies_in.get(style, {})
        replies.append({
            "style": style,
            "text": str(r.get("text", "")).strip(),
            "why": str(r.get("why", "")).strip(),
        })
    idx = data.get("chat_death_index", 50)
    try:
        idx = max(0, min(100, int(idx)))
    except (TypeError, ValueError):
        idx = 50
    return {
        "chat_death_index": idx,
        "context": str(data.get("context", "")).strip(),
        "replies": replies,
    }


@app.route("/api/wingman", methods=["POST"])
def wingman():
    body = request.get_json(silent=True) or {}
    demo = bool(body.get("demo", False))
    convo_text = (body.get("text") or "").strip()

    if demo:
        return jsonify(DEMO_RESPONSE)
    if not convo_text:
        return jsonify({"error": "missing text (Android 端 OCR 後的對話文字)"}), 400
    if not API_KEY:
        return jsonify({"error": "server missing DEEPSEEK_API_KEY"}), 500

    try:
        raw = _call_deepseek(convo_text)
        return jsonify(_coerce(raw))
    except requests.Timeout:
        return jsonify({"error": "deepseek timeout"}), 504
    except Exception as e:  # noqa: BLE001 — hackathon: 統一回錯讓客戶端退 demo
        return jsonify({"error": str(e)}), 502


@app.route("/health")
def health():
    return jsonify({"ok": True, "model": MODEL, "has_key": bool(API_KEY)})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", "8000")), debug=True)
