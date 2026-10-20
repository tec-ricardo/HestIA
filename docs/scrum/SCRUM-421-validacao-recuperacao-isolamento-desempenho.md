# SCRUM-421 — Recuperação, isolamento e desempenho

## Cobertura

- BuscaVetorialPostgresTest: filtros de empresa, perfil e elegibilidade,
  persistência vetorial e rejeição de conteúdo alterado durante a geração.
- PerguntasConhecidasPostgresTest: recuperação com perguntas e vetores controlados.
- LimiteSimilaridadePostgresTest: ordenação, limite de resultados e similaridade mínima.
- DesempenhoBuscaPostgresTest: recuperação com volume e medição de tempos.

## Execução local de desempenho

Comando:
`.\mvnw.cmd "-Dtest=DesempenhoBuscaPostgresTest" test`

Resultado: BUILD SUCCESS.

| Parâmetro | Valor |
|---|---|
| Registros | 1.000 |
| Dimensões | 1.536 |
| Aquecimentos | 5 |
| Amostras | 20 |
| Limite de resultados | 5 |
| Similaridade mínima | 0,70 |
| Mediana | 182,306 ms |
| P95 | 299,252 ms |
| Máximo | 904,866 ms |

O teste confirmou cinco resultados distintos, da empresa solicitada,
com perfil autorizado, disponíveis para reutilização e sem indicação
de dados sensíveis.

## Limites da medição

Embeddings simulados e idênticos. PostgreSQL, pgvector e filtros SQL reais.
Hidratação por JDBC em repositório simulado.

Os tempos incluem busca e hidratação, excluindo chamadas à OpenAI.
A execução é sequencial, sem carga concorrente.

A medição não representa benchmark de produção, não comprova uso do índice
HNSW e não estabelece aprovação por desempenho. Não há meta de latência definida.

Relatório gerado em:
`target/performance/SCRUM-421-busca-vetorial.txt`.