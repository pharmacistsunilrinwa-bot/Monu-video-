#!/data/data/com.termux/files/usr/bin/bash
set -e

PROJECT="$HOME/Monu-Video-AI"

echo "=========================================="
echo " MONU VIDEO AI - PROJECT BOOTSTRAPPER"
echo " Owner: Sunil Rinwa"
echo "=========================================="

pkg update -y
pkg upgrade -y

pkg install -y \
  git \
  curl \
  wget \
  unzip \
  zip \
  jq \
  python \
  nodejs \
  ffmpeg \
  sqlite \
  openjdk-17

mkdir -p "$PROJECT"

cd "$PROJECT"

echo "[1/12] Creating project directories..."

mkdir -p \
  server/app/brain \
  server/app/api \
  server/app/models \
  server/app/video \
  server/app/audio \
  server/app/storage \
  server/app/database \
  server/app/verification \
  server/tests \
  server/data/uploads \
  server/data/chunks \
  server/data/processed \
  server/data/final \
  server/logs \
  server/scripts \
  android \
  config \
  docs \
  scripts

echo "[2/12] Creating root configuration..."

cat > .gitignore <<'EOF'
.env
.env.*
__pycache__/
*.pyc
.gradle/
build/
.idea/
*.iml
server/data/uploads/*
server/data/chunks/*
server/data/processed/*
server/data/final/*
server/logs/*
EOF

cat > .env.example <<'EOF'
MONU_OWNER=Sunil Rinwa
MONU_NAME=Monu

# NEVER commit real API keys.
GEMINI_API_KEY=

DEFAULT_MODEL=gemini-3.1-flash-lite

DATABASE_URL=sqlite:///./data/monu.db

SERVER_HOST=0.0.0.0
SERVER_PORT=8000

STATUS_POLL_SECONDS=60

MAX_VIDEO_SECONDS=3600
VIDEO_CHUNK_SECONDS=300

FFMPEG_BIN=ffmpeg
FFPROBE_BIN=ffprobe

ENABLE_THREE_STAGE_VERIFICATION=true
ENABLE_PERMANENT_STORAGE=true
EOF

cat > config/models.json <<'EOF'
{
  "default": "gemini-3.1-flash-lite",
  "models": [
    {
      "id": "gemini-3.1-pro-preview",
      "provider": "google",
      "enabled": true,
      "role": "reasoning"
    },
    {
      "id": "gemini-3-flash-preview",
      "provider": "google",
      "enabled": true,
      "role": "fast_reasoning"
    },
    {
      "id": "gemini-2.5-pro",
      "provider": "google",
      "enabled": true,
      "role": "reasoning"
    },
    {
      "id": "gemini-3.1-flash-lite",
      "provider": "google",
      "enabled": true,
      "role": "default"
    },
    {
      "id": "gemini-3.5-flash",
      "provider": "google",
      "enabled": true,
      "role": "fast_reasoning"
    },
    {
      "id": "gemma-4-31b-it",
      "provider": "google",
      "enabled": true,
      "role": "local_or_api_reasoning"
    },
    {
      "id": "gemma-4-26b-a4b-it",
      "provider": "google",
      "enabled": true,
      "role": "fast_local_or_api_reasoning"
    }
  ]
}
EOF

echo "[3/12] Creating Monu Brain..."

cat > server/app/brain/monu_brain.py <<'PY'
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
PY

echo "[4/12] Creating database..."

cat > server/app/database/db.py <<'PY'
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DB = ROOT / "data" / "monu.db"


def connect():
    DB.parent.mkdir(parents=True, exist_ok=True)
    return sqlite3.connect(DB)


def initialize():
    con = connect()

    con.execute("""
    CREATE TABLE IF NOT EXISTS jobs (
        id TEXT PRIMARY KEY,
        status TEXT NOT NULL,
        progress INTEGER DEFAULT 0,
        input_file TEXT,
        output_file TEXT,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT DEFAULT CURRENT_TIMESTAMP
    )
    """)

    con.execute("""
    CREATE TABLE IF NOT EXISTS conversations (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        role TEXT NOT NULL,
        content TEXT NOT NULL,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
    )
    """)

    con.execute("""
    CREATE TABLE IF NOT EXISTS verification_results (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        job_id TEXT NOT NULL,
        stage INTEGER NOT NULL,
        passed INTEGER NOT NULL,
        details TEXT,
        created_at TEXT DEFAULT CURRENT_TIMESTAMP
    )
    """)

    con.commit()
    con.close()
PY

echo "[5/12] Creating video pipeline..."

cat > server/app/video/pipeline.py <<'PY'
import subprocess
from pathlib import Path


class VideoPipeline:

    def __init__(self, root):
        self.root = Path(root)
        self.chunks = self.root / "data" / "chunks"
        self.processed = self.root / "data" / "processed"
        self.final = self.root / "data" / "final"

        for directory in [
            self.chunks,
            self.processed,
            self.final
        ]:
            directory.mkdir(parents=True, exist_ok=True)

    def probe(self, source):
        result = subprocess.run(
            [
                "ffprobe",
                "-v", "error",
                "-show_entries",
                "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                str(source)
            ],
            capture_output=True,
            text=True,
            check=True
        )

        return float(result.stdout.strip())

    def split(self, source, chunk_seconds=300):
        source = Path(source)

        pattern = self.chunks / f"{source.stem}_%04d.mp4"

        subprocess.run(
            [
                "ffmpeg",
                "-y",
                "-i", str(source),
                "-map", "0",
                "-c", "copy",
                "-f", "segment",
                "-segment_time", str(chunk_seconds),
                "-reset_timestamps", "1",
                str(pattern)
            ],
            check=True
        )

        return sorted(self.chunks.glob(f"{source.stem}_*.mp4"))

    def join(self, chunks, output):
        list_file = self.final / "concat.txt"

        with list_file.open("w") as f:
            for chunk in chunks:
                f.write(f"file '{Path(chunk).resolve()}'\n")

        subprocess.run(
            [
                "ffmpeg",
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", str(list_file),
                "-c", "copy",
                str(output)
            ],
            check=True
        )

        return Path(output)
PY

echo "[6/12] Creating triple verification..."

cat > server/app/verification/triple_check.py <<'PY'
import subprocess
from pathlib import Path


class TripleVerification:

    def verify(self, source, output):
        results = []

        results.append(self._check_exists(output))
        results.append(self._check_readable(output))
        results.append(self._check_duration(source, output))

        return {
            "passed": all(results),
            "checks": results
        }

    def _check_exists(self, output):
        return Path(output).exists() and Path(output).stat().st_size > 0

    def _check_readable(self, output):
        try:
            subprocess.run(
                [
                    "ffprobe",
                    "-v", "error",
                    str(output)
                ],
                check=True,
                capture_output=True
            )
            return True
        except Exception:
            return False

    def _check_duration(self, source, output):
        def duration(path):
            result = subprocess.run(
                [
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    str(path)
                ],
                capture_output=True,
                text=True,
                check=True
            )
            return float(result.stdout.strip())

        try:
            a = duration(source)
            b = duration(output)

            if a == 0:
                return False

            difference = abs(a - b) / a
            return difference < 0.02
        except Exception:
            return False
PY

echo "[7/12] Creating API server..."

cat > server/app/main.py <<'PY'
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from fastapi import FastAPI
from pydantic import BaseModel

from app.brain.monu_brain import MonuBrain
from app.database.db import initialize

app = FastAPI(
    title="Monu Video AI",
    version="1.0.0"
)

brain = MonuBrain()

initialize()


class ChatRequest(BaseModel):
    message: str
    model: str | None = None


@app.get("/health")
def health():
    return {
        "status": "ok",
        "assistant": "Monu",
        "owner": "Sunil Rinwa"
    }


@app.get("/models")
def models():
    return brain.available_models()


@app.post("/chat/analyze")
def analyze(request: ChatRequest):
    return brain.analyze_request(
        request.message,
        request.model
    )
PY

echo "[8/12] Creating Python dependencies..."

cat > server/requirements.txt <<'EOF'
fastapi
uvicorn[standard]
python-multipart
pydantic
google-genai
httpx
EOF

echo "[9/12] Creating server launcher..."

cat > server/scripts/start.sh <<'EOF'
#!/data/data/com.termux/files/usr/bin/bash
cd "$(dirname "$0")/.."

python -m pip install -r requirements.txt

python -m uvicorn app.main:app \
  --host 0.0.0.0 \
  --port 8000
EOF

chmod +x server/scripts/start.sh

echo "[10/12] Creating Android project descriptor..."

cat > android/README.md <<'EOF'
# Monu Video AI Android

The Android client must provide:

- Main chat-driven operating system interface
- "What can I help with, Sunil?"
- Model dropdown
- Plus button
- Photo upload
- Video upload
- Send
- Voice input
- Action triggers
- Text-to-Speech
- Job status screen
- 60-second status polling
- Persistent conversation state
- Server connection configuration
EOF

cat > docs/ARCHITECTURE.md <<'EOF'
# Monu Video AI Architecture

User
 |
 v
Android Chat UI
 |
 v
Monu API
 |
 v
Monu Brain / Orchestrator
 |
 +---- Model Router
 |
 +---- Tool Router
 |
 +---- Video Pipeline
 |       |
 |       +---- FFmpeg
 |       +---- Split
 |       +---- Process
 |       +---- Rejoin
 |
 +---- Memory / Database
 |
 +---- Triple Verification
 |
 v
Final Result

The model is NOT treated as the complete application brain.
Monu's orchestration layer controls routing, tool execution,
state, validation and verification.
EOF

echo "[11/12] Creating automated verification..."

cat > scripts/verify_project.sh <<'EOF'
#!/data/data/com.termux/files/usr/bin/bash
set -e

PROJECT="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== MONU PROJECT VERIFICATION ==="

test -f "$PROJECT/server/app/main.py"
test -f "$PROJECT/server/app/brain/monu_brain.py"
test -f "$PROJECT/server/app/video/pipeline.py"
test -f "$PROJECT/server/app/verification/triple_check.py"
test -f "$PROJECT/server/app/database/db.py"
test -f "$PROJECT/config/models.json"

python -m py_compile \
  "$PROJECT/server/app/main.py" \
  "$PROJECT/server/app/brain/monu_brain.py" \
  "$PROJECT/server/app/video/pipeline.py" \
  "$PROJECT/server/app/verification/triple_check.py" \
  "$PROJECT/server/app/database/db.py"

ffmpeg -version >/dev/null
ffprobe -version >/dev/null

echo
echo "MONU VIDEO AI STRUCTURE: PASS"
echo "FFmpeg: PASS"
echo "Python syntax: PASS"
echo "Model registry: PASS"
echo "Database module: PASS"
echo "Triple verification module: PASS"
EOF

chmod +x scripts/verify_project.sh

echo "[12/12] Creating project README..."

cat > README.md <<'EOF'
# Monu Video AI

Owner: Sunil Rinwa
Assistant: Monu

## Primary interface

The Android application is chat-first.

Greeting:

"What can I help with, Sunil?"

## Core architecture

Monu Brain
- intent analysis
- model selection
- tool selection
- execution planning
- result validation
- verification

## Video

Maximum intended input duration:
1 hour

Pipeline:

upload
 -> probe
 -> split
 -> process
 -> join
 -> triple verification
 -> final output

## Storage

Persistent SQLite database is used for project state.

Important:
Permanent data should never be automatically deleted by cleanup jobs.

## Status

The Android client should poll the server every 60 seconds
for long-running video jobs.

## API key security

Real API keys must remain on the server.
Never place GEMINI_API_KEY inside the Android APK.

## Development

Run:

./scripts/verify_project.sh

Then:

cd server
./scripts/start.sh
EOF

cd "$PROJECT"

./scripts/verify_project.sh

echo
echo "=========================================="
echo " MONU VIDEO AI CREATED SUCCESSFULLY"
echo "=========================================="
echo
echo "Project:"
echo "$PROJECT"
echo
echo "Next:"
echo "  cd ~/Monu-Video-AI"
echo "  ./scripts/verify_project.sh"
echo
echo "Server:"
echo "  cd ~/Monu-Video-AI/server"
echo "  ./scripts/start.sh"
echo
