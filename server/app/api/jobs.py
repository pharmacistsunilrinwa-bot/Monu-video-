from fastapi import APIRouter
from app.database.db import connect

router = APIRouter(
    prefix="/jobs",
    tags=["jobs"]
)


@router.get("/{job_id}")
def get_job(job_id: str):
    con = connect()
    row = con.execute(
        """
        SELECT id, status, progress, input_file, output_file,
               created_at, updated_at
        FROM jobs
        WHERE id=?
        """,
        (job_id,),
    ).fetchone()
    con.close()

    if row is None:
        return {
            "job_id": job_id,
            "status": "unknown",
        }

    return {
        "job_id": row[0],
        "status": row[1],
        "progress": row[2],
        "input_file": row[3],
        "output_file": row[4],
        "created_at": row[5],
        "updated_at": row[6],
    }
