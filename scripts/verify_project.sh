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
