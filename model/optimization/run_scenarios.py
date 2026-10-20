"""Experimentos reproduzíveis do Deliverable 2; dados exclusivamente simulados."""
import csv
import json
from copy import deepcopy
from pathlib import Path

from solver import load_scenario, solve, is_feasible

BASE = Path(__file__).resolve().parent


def experiments():
    original = load_scenario(BASE)
    for name in ("base", "orcamento_reduzido", "orcamento_ampliado",
                 "risco_restritivo", "bloqueio_copilot", "limite_uma_ferramenta"):
        items, budget, risks, limits = deepcopy(original)
        if name == "orcamento_reduzido":
            budget = 800
        elif name == "orcamento_ampliado":
            budget = 2000
        elif name == "risco_restritivo":
            risks = dict.fromkeys(risks, 2)
        elif name == "bloqueio_copilot":
            for item in items:
                if item["tool"] == "Copilot":
                    item["allowed"] = False
        elif name == "limite_uma_ferramenta":
            budget = 2000
            limits = dict.fromkeys(limits, 1)
        yield name, items, budget, risks, limits


def run():
    results = []
    for name, items, budget, risks, limits in experiments():
        folder = BASE / "scenarios" / name
        folder.mkdir(parents=True, exist_ok=True)
        with (folder / "scenario.csv").open("w", encoding="utf-8", newline="") as f:
            writer = csv.DictWriter(f, fieldnames=list(items[0]))
            writer.writeheader()
            writer.writerows([{**x, "allowed": int(x["allowed"])} for x in items])
        with (folder / "limits.csv").open("w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f)
            writer.writerow(["key", "department", "value"])
            writer.writerow(["budget", "", budget])
            writer.writerows(["risk_limit", k, v] for k, v in risks.items())
            writer.writerows(["tool_limit", k, v] for k, v in limits.items())
        result = solve(*load_scenario(folder))
        selected = [x for x in items if f"{x['department']}::{x['tool']}" in result["selected"]]
        assert is_feasible(selected, budget, risks, limits)
        result.update(scenario=name, budget=budget, remaining=budget-result["cost"],
                      feasible=True, risk_limits=risks, tool_limits=limits)
        (folder / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        results.append(result)
    output = BASE / "results"
    output.mkdir(exist_ok=True)
    (output / "results.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    with (output / "comparison.csv").open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["cenario", "orcamento", "custo", "beneficio", "saldo", "selecao", "viavel"])
        for r in results:
            writer.writerow([r["scenario"], r["budget"], r["cost"], r["benefit"],
                             r["remaining"], "; ".join(r["selected"]), r["feasible"]])
    print(json.dumps(results, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    run()
