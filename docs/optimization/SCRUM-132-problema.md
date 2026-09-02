# SCRUM-132 - Problema de otimizacao do HestIA

## Decisao apoiada

Selecionar quais licencas de ferramentas de IA devem ser disponibilizadas aos
departamentos de uma empresa, respeitando orcamento, risco e politicas, para
maximizar o beneficio organizacional esperado.

## Unidade de decisao

Cada decisao corresponde ao par departamento-ferramenta. O modelo responde se
uma ferramenta sera ou nao disponibilizada a determinado departamento durante
um ciclo de planejamento.

## Entradas

- custo da licenca para cada par departamento-ferramenta;
- beneficio esperado, em pontos comparaveis;
- nivel de risco da ferramenta;
- orcamento total da empresa;
- limite de risco por departamento;
- compatibilidade entre politica e ferramenta;
- limite de ferramentas por departamento.

## Saida

Lista dos pares selecionados, custo total, beneficio total e verificacao de
todas as restricoes.

## Hipoteses do MVP

- custos e beneficios sao conhecidos para o ciclo;
- uma decisao e binaria;
- cada par pode ser escolhido no maximo uma vez;
- risco e representado por uma escala numerica de 1 a 5;
- pares proibidos por politica nao podem ser selecionados;
- efeitos entre ferramentas e descontos por volume ficam fora do MVP.

## Criterio de sucesso

Uma instancia pequena deve ser resolvida de forma reproduzivel e o resultado
deve ser verificavel por enumeracao completa, permitindo explicar por que
nenhuma solucao viavel possui beneficio maior.
