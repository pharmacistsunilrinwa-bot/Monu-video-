from fastapi import APIRouter

router = APIRouter(
    prefix="/system",
    tags=["system"]
)


@router.get("/health")
def health():

    return {
        "status": "ok",
        "assistant": "Monu",
        "owner": "Sunil Rinwa",
        "brain": "Monu Orchestrator"
    }
