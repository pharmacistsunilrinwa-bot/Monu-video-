from fastapi import APIRouter, UploadFile, File
from pathlib import Path
import uuid

router = APIRouter(
    prefix="/upload",
    tags=["upload"]
)

UPLOAD_DIR = (
    Path(__file__).resolve()
    .parents[2] / "data" / "uploads"
)

UPLOAD_DIR.mkdir(
    parents=True,
    exist_ok=True
)


@router.post("")
async def upload_media(
    file: UploadFile = File(...)
):

    extension = Path(
        file.filename or ""
    ).suffix

    filename = (
        f"{uuid.uuid4().hex}"
        f"{extension}"
    )

    destination = UPLOAD_DIR / filename

    with destination.open("wb") as output:

        while True:
            chunk = await file.read(1024 * 1024)

            if not chunk:
                break

            output.write(chunk)

    return {
        "filename": filename,
        "path": str(destination),
        "original_name": file.filename
    }
