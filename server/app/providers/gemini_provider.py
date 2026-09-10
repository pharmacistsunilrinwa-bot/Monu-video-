import os
import json
import urllib.request
import urllib.error


class GeminiProvider:
    def __init__(self):
        self.api_key = os.getenv("GEMINI_API_KEY")
        if not self.api_key:
            raise RuntimeError("GEMINI_API_KEY is not set")

    def generate(self, model: str, message: str) -> dict:
        url = (
            "https://generativelanguage.googleapis.com/v1beta/"
            f"models/{model}:generateContent?key={self.api_key}"
        )

        payload = {
            "contents": [
                {
                    "parts": [
                        {
                            "text": message
                        }
                    ]
                }
            ]
        }

        request = urllib.request.Request(
            url,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )

        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                raw = response.read().decode("utf-8")
                data = json.loads(raw)

                text = ""
                candidates = data.get("candidates", [])

                if candidates:
                    content = candidates[0].get("content", {})
                    parts = content.get("parts", [])

                    for part in parts:
                        if isinstance(part, dict) and part.get("text"):
                            text += part["text"]

                return {
                    "ok": True,
                    "model": model,
                    "text": text,
                    "raw": data,
                }

        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")

            try:
                error_data = json.loads(body)
            except Exception:
                error_data = {"error": body}

            return {
                "ok": False,
                "model": model,
                "status_code": exc.code,
                "error": error_data,
            }

        except Exception as exc:
            return {
                "ok": False,
                "model": model,
                "error": str(exc),
            }
