from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.brain.monu_brain import MonuBrain
from app.brain.planner import MonuPlanner


def test_brain_model():

    brain = MonuBrain()

    result = brain.analyze_request(
        "मेरा वीडियो process करो"
    )

    assert result["assistant"] == "Monu"
    assert result["owner"] == "Sunil Rinwa"
    assert result["requires_verification"] is True


def test_planner():

    brain = MonuBrain()
    planner = MonuPlanner()

    analysis = brain.analyze_request(
        "video split करो"
    )

    plan = planner.create_plan(
        analysis
    )

    assert "video_pipeline" in plan["actions"]
