# SCRUM-133 - Variaveis e restricoes do modelo

## Conjuntos

- `D`: departamentos;
- `F`: ferramentas de IA;
- `A`: pares `(d,f)` permitidos pela politica.

## Parametros

- `c[d,f]`: custo do par em reais;
- `b[d,f]`: beneficio esperado em pontos;
- `r[f]`: risco da ferramenta, de 1 a 5;
- `B`: orcamento total;
- `R[d]`: soma maxima de risco no departamento;
- `K[d]`: quantidade maxima de ferramentas no departamento.

## Variavel de decisao

`x[d,f]` e binaria e vale 1 quando a ferramenta `f` e disponibilizada ao
departamento `d`; caso contrario vale 0.

## Funcao objetivo

Maximizar o beneficio total:

```text
max Z = soma(d em D, f em F) b[d,f] * x[d,f]
```

## Restricoes

Orcamento:

```text
soma(d,f) c[d,f] * x[d,f] <= B
```

Risco por departamento:

```text
para todo d: soma(f) r[f] * x[d,f] <= R[d]
```

Limite de ferramentas:

```text
para todo d: soma(f) x[d,f] <= K[d]
```

Politica:

```text
x[d,f] = 0 para todo par (d,f) que nao pertence a A
```

Dominio:

```text
x[d,f] em {0,1}
```

## Unidades e interpretacao

Custos usam reais por ciclo, beneficios usam pontos relativos e risco e uma
escala ordinal. Pontos e risco nao devem ser somados entre si. A funcao objetivo
compara somente beneficios; custo e risco aparecem como limites de viabilidade.
