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
| Estabilizacao | build, configuracao, Compose e CI | implementada em branch |
| SCRUM-150 | plano e diagrama | branch de documentacao |
| SCRUM-106 | arquitetura inicial | branch de documentacao |
| SCRUM-105 | diagramas C4 | branch de documentacao |
| SCRUM-52 | PDF e fonte Markdown | definida; ainda nao integrada |
| SCRUM-54 | entidade, DTO, repository, migration e testes | implementada em branch |
| SCRUM-86 | CNPJ, validacao, erros e testes | implementada em branch |
| SCRUM-159 | limite arquitetural e OpenAPI | implementada em branch |
| SCRUM-109 | Alert Service Node.js | implementada e testada |
| SCRUM-110 | cliente Spring resiliente | implementada e testada |
| SCRUM-111 | testes de contrato e ponta a ponta | aprovada tecnicamente |
| SCRUM-118 | plano de qualidade | documentada |
| SCRUM-132 a 136 | modelo e instancia reproduzivel | implementadas e testadas |
| SCRUM-77 | branch candidata integrada | aguarda revisao/homologacao |

## Pendencias humanas e de governanca

1. publicar as branches locais no remoto;
2. abrir e revisar um PR por atividade;
3. integrar na ordem de dependencia;
4. homologar os fluxos visiveis;
5. reconciliar `develop` e `main` por PR;
6. criar Release PR e tag somente depois da aprovacao.

Nenhuma dessas etapas deve ser confundida com a implementacao ja realizada.
