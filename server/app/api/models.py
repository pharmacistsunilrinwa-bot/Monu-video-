from fastapi import APIRouter

from app.brain.model_router import ModelRouter

router = APIRouter(
    prefix="/models",
    tags=["models"]
)

model_router = ModelRouter()


@router.get("")
def get_models():

    return {
        "default": model_router.config["default"],
        "models": model_router.list_models()
    }
