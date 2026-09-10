from fastapi import APIRouter

router = APIRouter(
    prefix="/jobs",
    tags=["jobs"]
)


@router.get("/{job_id}")
def get_job(job_id: str):

    # Persistent job retrieval will be connected
    # to the job database in the next integration step.

    return {
        "job_id": job_id,
        "status": "unknown",
        "message": "Job persistence endpoint foundation."
    }
