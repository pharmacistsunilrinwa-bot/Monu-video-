import json
from pathlib import Path


class ModelRouter:

    def __init__(self):
        config = (
            Path(__file__).resolve()
            .parents[3] / "config" / "models.json"
        )

        self.config = json.loads(
            config.read_text()
        )

        self.models = {
            item["id"]: item
            for item in self.config["models"]
        }

    def list_models(self):
        return list(self.models.values())

    def resolve(self, requested_model=None):

        if (
            requested_model
            and requested_model in self.models
        ):
            return requested_model

        return self.config["default"]

    def role(self, model):
        item = self.models.get(model)

        if not item:
            return None

        return item.get("role")
