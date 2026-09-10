import os
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]

OWNER = os.getenv("MONU_OWNER", "Sunil Rinwa")
ASSISTANT_NAME = os.getenv("MONU_NAME", "Monu")

DEFAULT_MODEL = os.getenv(
    "DEFAULT_MODEL",
    "gemini-3.1-flash-lite"
)

STATUS_POLL_SECONDS = int(
    os.getenv("STATUS_POLL_SECONDS", "60")
)

MAX_VIDEO_SECONDS = int(
    os.getenv("MAX_VIDEO_SECONDS", "3600")
)

VIDEO_CHUNK_SECONDS = int(
    os.getenv("VIDEO_CHUNK_SECONDS", "300")
)

ENABLE_THREE_STAGE_VERIFICATION = (
    os.getenv(
        "ENABLE_THREE_STAGE_VERIFICATION",
        "true"
    ).lower() == "true"
)
