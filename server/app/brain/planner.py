class MonuPlanner:
    """
    Converts the user's request into an execution plan.

    This is intentionally separate from the external model.
    """

    def create_plan(self, analysis):

        request = analysis.get("request", "")
        lower = request.lower()

        actions = []

        if any(
            word in lower
            for word in [
                "video",
                "वीडियो",
                "split",
                "merge",
                "cartoon",
                "voice"
            ]
        ):
            actions.append("video_pipeline")

        if any(
            word in lower
            for word in [
                "upload",
                "अपलोड",
                "photo",
                "फोटो"
            ]
        ):
            actions.append("media_input")

        if not actions:
            actions.append("conversation")

        return {
            "request": request,
            "model": analysis.get("model"),
            "actions": actions,
            "requires_verification": True
        }
