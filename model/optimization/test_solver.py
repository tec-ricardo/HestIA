import unittest
from pathlib import Path

from solver import is_feasible, load_scenario, solve


class OptimizationModelTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = load_scenario(Path(__file__).resolve().parent)

    def test_finds_expected_optimum(self):
        result = solve(*self.data)
        self.assertEqual(19, result["benefit"])
        self.assertEqual(1300, result["cost"])
        self.assertEqual(
            {"Marketing::Midjourney", "TI::Copilot"}, set(result["selected"])
        )

    def test_rejects_policy_and_budget_violations(self):
        items, budget, risk_limits, tool_limits = self.data
        forbidden = [next(item for item in items if not item["allowed"])]
        self.assertFalse(is_feasible(forbidden, budget, risk_limits, tool_limits))
        allowed = [item for item in items if item["allowed"]]
        self.assertFalse(is_feasible(allowed, budget, risk_limits, tool_limits))


if __name__ == "__main__":
    unittest.main()
