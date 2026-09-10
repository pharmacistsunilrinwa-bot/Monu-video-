from app.brain.monu_brain import MonuBrain
from app.brain.model_router import ModelRouter
from app.brain.planner import MonuPlanner
from app.brain.tool_router import MonuToolRouter
from app.providers.gemini_provider import GeminiProvider


class MonuRuntime:
    def __init__(self):
        self.brain = MonuBrain()
        self.model_router = ModelRouter()
        self.planner = MonuPlanner()
        self.tool_router = MonuToolRouter()
        self.provider = GeminiProvider()

    def run(self, message: str, model: str | None = None) -> dict:
        analysis = self.brain.analyze_request(
            message,
            requested_model=model,
        )

        selected_model = self.brain.choose_model(
            requested=model,
        )

        plan = self.planner.create_plan(analysis)

        tool_result = self.tool_router.select(plan)

        prompt = (
            "You are Monu, the AI assistant owned and controlled by "
            "Sunil Rinwa.\n\n"
            "The Monu application has already analyzed this request "
            "using its own orchestration layer.\n\n"
            f"Brain analysis:\n{analysis}\n\n"
            f"Execution plan:\n{plan}\n\n"
            f"Tool routing:\n{tool_result}\n\n"
            f"User message:\n{message}"
        )

        provider_result = self.provider.generate(
            selected_model,
            prompt,
        )

        return {
            "assistant": "Monu",
            "owner": "Sunil Rinwa",
            "model": selected_model,
            "analysis": analysis,
            "plan": plan,
            "tool_result": tool_result,
            "provider_result": provider_result,
        }
