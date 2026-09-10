class MonuToolRouter:

    ALLOWED_TOOLS = {
        "conversation",
        "media_input",
        "video_pipeline"
    }

    def select(self, actions):

        return [
            action
            for action in actions
            if action in self.ALLOWED_TOOLS
        ]
