from pathlib import Path
import uuid

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.database.db import connect
from app.video.pipeline import VideoPipeline
from app.video.transformer import VideoTransformer
from app.verification.triple_check import TripleVerification

router = APIRouter(prefix="/media", tags=["media"])

ROOT = Path(__file__).resolve().parents[2]
UPLOADS = ROOT / "data" / "uploads"
FINAL = ROOT / "data" / "final"


class ProcessRequest(BaseModel):
    filename: str
    message: str = ""


def update_job(job_id, status, progress, output_file=None):
    con = connect()
    con.execute(
        """
        UPDATE jobs
        SET status=?, progress=?, output_file=?,
            updated_at=CURRENT_TIMESTAMP
        WHERE id=?
        """,
        (status, progress, output_file, job_id),
    )
    con.commit()
    con.close()


@router.post("/process")
def process_media(request: ProcessRequest):
    source = UPLOADS / Path(request.filename).name

    if not source.exists():
        raise HTTPException(status_code=404, detail="Uploaded file not found")

    suffix = source.suffix.lower()
    video_types = {".mp4", ".mov", ".mkv", ".webm", ".avi", ".m4v"}

    if suffix not in video_types:
        return {
            "ok": False,
            "status": "unsupported",
            "message": (
                "File uploaded successfully, but this processing pipeline "
                "currently supports video files."
            ),
        }

    job_id = uuid.uuid4().hex
    output = FINAL / f"{source.stem}_{job_id[:8]}.mp4"

    con = connect()
    con.execute(
        """
        INSERT INTO jobs(id, status, progress, input_file)
        VALUES (?, ?, ?, ?)
        """,
        (job_id, "processing", 10, str(source)),
    )
    con.commit()
    con.close()

    try:
        text = request.message.lower()
        cartoon = any(
            word in text
            for word in ["cartoon", "कार्टून", "cartoonize", "कार्टून बनाओ"]
        )

        update_job(job_id, "processing", 25)

        pipeline = VideoPipeline(ROOT)
        transformer = VideoTransformer(ROOT)

        duration = pipeline.probe(source)

        if duration > 3600:
            update_job(job_id, "failed", 0)
            raise HTTPException(
                status_code=400,
                detail="Video exceeds the maximum supported duration of 1 hour.",
            )

        chunks = pipeline.split(source)

        processed = []
        for index, chunk in enumerate(chunks):
            current = chunk

            if cartoon:
                out = pipeline.processed / f"{chunk.stem}_cartoon.mp4"
                current = transformer.cartoon(current, out)

            processed.append(current)
            progress = 30 + int(((index + 1) / len(chunks)) * 50)
            update_job(job_id, "processing", progress)

        pipeline.join(processed, output)

        verification = TripleVerification().verify(source, output)

        if not verification["passed"]:
            update_job(job_id, "failed", 0, str(output))
            raise HTTPException(
                status_code=500,
                detail="Triple verification failed",
            )

        update_job(job_id, "completed", 100, str(output))

        return {
            "ok": True,
            "job_id": job_id,
            "status": "completed",
            "input": str(source),
            "output": str(output),
            "cartoon": cartoon,
            "duration": duration,
            "chunks": len(chunks),
            "verification": verification,
        }

    except HTTPException:
        raise
    except Exception as exc:
        update_job(job_id, "failed", 0)
        raise HTTPException(status_code=500, detail=str(exc))
