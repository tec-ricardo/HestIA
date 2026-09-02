# SCRUM-136 - Modelo matematico consolidado do HestIA

## Finalidade

O modelo seleciona licencas de ferramentas de IA para departamentos com o
maior beneficio esperado, sem ultrapassar orcamento, risco, limite de
ferramentas e regras de politica.

## Formulacao

Para departamentos `d` e ferramentas `f`, `x[d,f]` vale 1 quando o par e
selecionado. O objetivo e:

```text
max Z = soma b[d,f] * x[d,f]
```

Sujeito a:

```text
soma c[d,f] * x[d,f] <= B                         (orcamento)
soma_f r[f] * x[d,f] <= R[d]                      (risco por departamento)
soma_f x[d,f] <= K[d]                             (limite por departamento)
x[d,f] = 0 quando o par nao e permitido           (politica)
x[d,f] em {0,1}                                   (dominio)
```

`c` usa reais por ciclo, `b` usa pontos de beneficio e `r` usa escala ordinal
de 1 a 5.

## Cenario reproduzivel

Os CSVs em `model/optimization` representam dois departamentos e tres
ferramentas, com orcamento de R$ 1.400. A solucao otima seleciona:

- Marketing / Midjourney;
- TI / Copilot.

O custo e R$ 1.300 e o beneficio e 19 pontos.

## Validacao

`solver.py` enumera as 64 combinacoes binarias da instancia, descarta as
inviaveis e escolhe o maior beneficio, usando menor custo como desempate. Os
testes confirmam o otimo e rejeitam violacoes de politica, risco e orcamento.

## Reproducao

```text
cd model/optimization
python -m unittest -v
python solver.py
```

## Interpretacao e limites

O resultado nao substitui decisao humana. Beneficios e riscos precisam ser
estimados e aprovados pela organizacao. O MVP nao considera desconto por volume,
incerteza, dependencia entre ferramentas, multiplos periodos ou equidade entre
departamentos. Esses itens exigem extensao do modelo e novo processo de
validacao.

## Rastreabilidade

- SCRUM-132: problema e decisao apoiada;
- SCRUM-133: indices, parametros, variavel, objetivo e restricoes;
- SCRUM-134: instancia numerica;
- SCRUM-135: verificacao independente e testes;
- SCRUM-136: consolidacao e interpretacao.
