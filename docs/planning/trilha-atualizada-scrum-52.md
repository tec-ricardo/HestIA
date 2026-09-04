# Trilha tecnica atualizada do HestIA

## Regra de verificacao

O status do Jira nao e usado isoladamente. Uma atividade so e considerada
tecnicamente pronta quando possui entregavel verificavel, testes aplicaveis e
branch identificada. Integracao e homologacao continuam sendo etapas distintas.

## Trilha

```text
Estabilizar base
  |-> SCRUM-150 Planejamento
  |-> SCRUM-106 Arquitetura inicial
       |-> SCRUM-105 C4
       |-> SCRUM-159 Arquitetura Alert Service
            |-> SCRUM-109 Node.js
                 |-> SCRUM-110 Integracao Spring/Node
                      |-> SCRUM-111 Validacao ponta a ponta
       |-> SCRUM-118 Plano de qualidade

SCRUM-52 Definir Ferramenta IA
  |-> SCRUM-54 Implementar modelo de Ferramenta IA

SCRUM-86 Completar cadastro de Empresa

SCRUM-132 Problema de otimizacao
  |-> SCRUM-133 Variaveis e restricoes
       |-> SCRUM-134 Cenario inicial
            |-> SCRUM-135 Validacao
                 |-> SCRUM-136 Documentacao consolidada

Todas as branches revisadas e integradas
  |-> SCRUM-77 Candidato do MVP
       |-> homologacao -> Release PR -> tag
```

## Situacao verificada

| Atividade | Evidencia atual | Situacao tecnica |
|---|---|---|
| Estabilizacao | build, configuracao, Compose e CI | integrada no candidato |
| SCRUM-150 | plano e diagrama | integrada no candidato |
| SCRUM-106 | arquitetura inicial | integrada no candidato |
| SCRUM-105 | diagramas C4 | integrada no candidato |
| SCRUM-52 | PDF e fonte Markdown | integrada no candidato |
| SCRUM-54 | entidade, DTO, repository, migration e testes | integrada no candidato |
| SCRUM-86 | CNPJ, validacao, erros e testes | integrada no candidato |
| SCRUM-159 | limite arquitetural e OpenAPI | integrada no candidato |
| SCRUM-109 | Alert Service Node.js | integrada e testada |
| SCRUM-110 | cliente Spring resiliente | integrada e testada |
| SCRUM-111 | testes de contrato e ponta a ponta | integrada e aprovada tecnicamente |
| SCRUM-118 | plano de qualidade | integrado no candidato |
| SCRUM-132 a 136 | modelo e instancia reproduzivel | integradas e testadas |
| SCRUM-77 | branch candidata integrada | aguarda revisao/homologacao |

## Pendencias humanas e de governanca

1. publicar as branches componentes e a branch candidata no remoto;
2. abrir e revisar o PR da SCRUM-77 para `develop`;
3. homologar os fluxos visiveis;
4. integrar o candidato aprovado em `develop`;
5. reconciliar `develop` e `main` por Release PR;
6. criar a tag somente depois da aprovacao.

Nenhuma dessas etapas deve ser confundida com a implementacao ja realizada.
