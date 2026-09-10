from fastapi import APIRouter
from pydantic import BaseModel

from app.brain.monu_brain import MonuBrain
from app.brain.planner import MonuPlanner
from app.brain.tool_router import MonuToolRouter

router = APIRouter(
    prefix="/chat",
    tags=["chat"]
)

brain = MonuBrain()
planner = MonuPlanner()
tools = MonuToolRouter()


class ChatRequest(BaseModel):
    message: str
    model: str | None = None


@router.post("/plan")
def plan(request: ChatRequest):

    analysis = brain.analyze_request(
        request.message,
        request.model
    )

    plan = planner.create_plan(
        analysis
    )

    selected_tools = tools.select(
        plan["actions"]
    )

    return {
        "analysis": analysis,
        "plan": plan,
        "tools": selected_tools
    }
