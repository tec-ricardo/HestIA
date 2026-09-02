# SCRUM-134 - Cenario inicial do modelo

## Instancia

O cenario possui dois departamentos e tres ferramentas. O orcamento total e
R$ 1.400,00. Cada departamento pode receber no maximo duas ferramentas e soma
de risco ate 4.

Os dados reproduziveis estao em `model/optimization/scenario.csv` e
`model/optimization/limits.csv`.

## Solucao otima esperada

- Marketing recebe Midjourney: custo 600, beneficio 9, risco 3;
- TI recebe Copilot: custo 700, beneficio 10, risco 2.

Totais: custo 1.300, beneficio 19.

## Verificacao manual

As duas ferramentas de Marketing nao podem ser combinadas porque o risco seria
5. Em TI, ChatGPT e Copilot podem ser combinados, mas custam 1.100 e deixam
somente 300 de orcamento, insuficiente para qualquer outra escolha. A melhor
combinacao cruzada alternativa e Marketing/ChatGPT com TI/Copilot, de beneficio
18. Logo, a solucao de beneficio 19 e otima para esta instancia.
