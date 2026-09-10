import json
from pathlib import Path

MODELS_FILE = Path(__file__).resolve().parents[3] / "config" / "models.json"


class MonuBrain:
    """
    Monu's orchestration layer.

    The external model is an intelligence component, not the entire
    application brain. Monu decides:
      1. What the user actually wants.
      2. Whether tools are required.
      3. Which model should be used.
      4. Which server operation should execute.
      5. Whether the result requires verification.
    """

    def __init__(self):
        self.config = json.loads(MODELS_FILE.read_text())

    def available_models(self):
        return self.config["models"]

    def choose_model(self, requested=None):
        if requested:
            valid = {m["id"] for m in self.config["models"]}
            if requested in valid:
                return requested

        return self.config["default"]

    def analyze_request(self, text, requested_model=None):
        text = (text or "").strip()

        model = self.choose_model(requested_model)

        return {
            "assistant": "Monu",
            "owner": "Sunil Rinwa",
            "model": model,
            "request": text,
            "needs_video_processing": self._looks_like_video_task(text),
            "needs_tool_execution": self._needs_tools(text),
            "requires_verification": True
        }

    def _looks_like_video_task(self, text):
        words = [
            "video", "वीडियो", "cartoon",
            "voice", "आवाज़", "split",
            "merge", "join", "edit"
        ]
        value = text.lower()
        return any(word in value for word in words)

    def _needs_tools(self, text):
        words = [
            "create", "make", "process", "convert",
            "split", "merge", "download", "upload",
            "बनाओ", "करो", "चलाओ", "प्रोसेस"
        ]
        value = text.lower()
        return any(word in value for word in words)
