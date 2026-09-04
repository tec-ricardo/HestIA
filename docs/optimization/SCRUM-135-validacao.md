# SCRUM-135 - Validacao do modelo de otimizacao

O modelo foi validado por enumeracao de todas as decisoes binarias da instancia
inicial. Esse metodo e adequado ao cenario pequeno e fornece uma referencia
independente para futuros solucionadores.

O teste confirma:

- beneficio otimo igual a 19;
- custo da solucao igual a 1.300;
- selecao de Marketing/Midjourney e TI/Copilot;
- rejeicao de pares proibidos por politica;
- rejeicao de combinacoes que excedem orcamento ou risco.

Execucao:

```text
cd model/optimization
python -m unittest -v
python solver.py
```
