#!/usr/bin/env bash
# 複製 iOS 捷徑「取得 URL 內容」對後端送的 JSON 請求。
# 用法：
#   ./test-request.sh https://api.example.com conversation.txt  # 送出 OCR 文字
#   ./test-request.sh http://localhost:8000                    # demo 模式
set -euo pipefail

BASE="${1:-http://localhost:8000}"
TEXT_FILE="${2:-}"

if [[ -n "$TEXT_FILE" ]]; then
  if [[ ! -f "$TEXT_FILE" ]]; then
    printf '找不到 OCR 文字檔：%s\n' "$TEXT_FILE" >&2
    exit 64
  fi

  OCR_TEXT="$(<"$TEXT_FILE")"
  PAYLOAD="$(python3 -c 'import json, sys; print(json.dumps({"text": sys.argv[1], "demo": False}, ensure_ascii=False))' "$OCR_TEXT")"
else
  # demo=true 時後端不呼叫 DeepSeek；仍送齊 text/demo 兩個合約欄位。
  PAYLOAD='{"text":"","demo":true}'
fi

curl -sS -X POST "$BASE/api/wingman" \
  -H 'Content-Type: application/json' \
  --data "$PAYLOAD" | python3 -m json.tool --no-ensure-ascii
