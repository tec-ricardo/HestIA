"""Cada caso isola uma restrição; os demais limites não bloqueiam a solução."""
import unittest
from solver import solve, is_feasible


class ConstraintsTest(unittest.TestCase):
    def setUp(self):
        self.a = dict(department="TI", tool="A", cost=100, benefit=5, risk=2, allowed=True)
        self.b = dict(department="TI", tool="B", cost=100, benefit=6, risk=2, allowed=True)

    def test_budget_alone(self):
        self.assertFalse(is_feasible([self.a], 99, {"TI": 10}, {"TI": 10}))
        self.assertTrue(is_feasible([self.a], 100, {"TI": 10}, {"TI": 10}))

    def test_risk_alone(self):
        self.assertFalse(is_feasible([self.a, self.b], 1000, {"TI": 3}, {"TI": 10}))
        self.assertTrue(is_feasible([self.a, self.b], 1000, {"TI": 4}, {"TI": 10}))

    def test_quantity_alone(self):
        self.assertFalse(is_feasible([self.a, self.b], 1000, {"TI": 10}, {"TI": 1}))
        self.assertTrue(is_feasible([self.a, self.b], 1000, {"TI": 10}, {"TI": 2}))

    def test_policy_alone(self):
        self.assertFalse(is_feasible([{**self.a, "allowed": False}], 1000, {"TI": 10}, {"TI": 10}))

    def test_zero_budget_selects_nothing(self):
        self.assertEqual([], solve([self.a], 0, {"TI": 10}, {"TI": 10})["selected"])

    def test_tie_chooses_lower_cost(self):
        b = {**self.b, "benefit": 5, "cost": 90}
        self.assertEqual(["TI::B"], solve([self.a, b], 1000, {"TI": 10}, {"TI": 1})["selected"])


if __name__ == "__main__":
    unittest.main()
