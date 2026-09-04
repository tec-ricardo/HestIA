from __future__ import annotations

import csv
import itertools
import json
from pathlib import Path


def load_scenario(base: Path):
    with (base / "scenario.csv").open(encoding="utf-8", newline="") as stream:
        items = [
            {
                "department": row["department"],
                "tool": row["tool"],
                "cost": int(row["cost"]),
                "benefit": int(row["benefit"]),
                "risk": int(row["risk"]),
                "allowed": row["allowed"] == "1",
            }
            for row in csv.DictReader(stream)
        ]

    budget = None
    risk_limits = {}
    tool_limits = {}
    with (base / "limits.csv").open(encoding="utf-8", newline="") as stream:
        for row in csv.DictReader(stream):
            value = int(row["value"])
            if row["key"] == "budget":
                budget = value
            elif row["key"] == "risk_limit":
                risk_limits[row["department"]] = value
            elif row["key"] == "tool_limit":
                tool_limits[row["department"]] = value
    if budget is None:
        raise ValueError("budget ausente")
    return items, budget, risk_limits, tool_limits


def solve(items, budget, risk_limits, tool_limits):
    best = None
    for bits in itertools.product((0, 1), repeat=len(items)):
        selected = [item for bit, item in zip(bits, items) if bit]
        if not is_feasible(selected, budget, risk_limits, tool_limits):
            continue
        candidate = {
            "selected": [f"{item['department']}::{item['tool']}" for item in selected],
            "cost": sum(item["cost"] for item in selected),
            "benefit": sum(item["benefit"] for item in selected),
        }
        if best is None or (candidate["benefit"], -candidate["cost"]) > (
            best["benefit"], -best["cost"]
        ):
            best = candidate
    return best


def is_feasible(selected, budget, risk_limits, tool_limits):
    if any(not item["allowed"] for item in selected):
        return False
    if sum(item["cost"] for item in selected) > budget:
        return False
    for department in risk_limits:
        department_items = [item for item in selected if item["department"] == department]
        if sum(item["risk"] for item in department_items) > risk_limits[department]:
            return False
        if len(department_items) > tool_limits[department]:
            return False
    return True


if __name__ == "__main__":
    base = Path(__file__).resolve().parent
    print(json.dumps(solve(*load_scenario(base)), ensure_ascii=False, indent=2))
