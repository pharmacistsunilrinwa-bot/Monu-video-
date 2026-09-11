from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from fastapi import FastAPI
from pydantic import BaseModel

from app.brain.monu_brain import MonuBrain
from app.api.process import router as process_router

from app.database.db import initialize
from app.api.upload import router as upload_router

app = FastAPI(
    title="Monu Video AI",
    version="1.0.0"
)
app.include_router(upload_router)

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

app.include_router(process_router)
