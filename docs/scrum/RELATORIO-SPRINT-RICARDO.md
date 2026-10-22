> Atualização de 21/10/2026: este documento contém o diagnóstico histórico. A situação atual, os testes e as dependências estão na [revisão final das 40 tasks](REVISAO-FINAL-SPRINT-RICARDO-2026-10-21.md).

# Relatório de alterações — sprint de Ricardo

Data: 21/10/2026. Pasta original: `C:\Users\ricardo.davila\OneDrive - ESPM\QUARTO SEMESTRE\PROJETO INTEGRADO\HestIA`.
Base da develop: `f33216617dfc1a81addf6dc0e3c7bffa4998f973`. Branch local: `feature/SCRUM-489-491-440-442-443-integracao`.

## Escopo e estado real

O usuário confirmou todos os seus itens da sprint. A consulta atual ao Jira expirou;
a lista histórica de 40 itens desta conversa precisa ser reconciliada com o estado atual.
Esta etapa implementou as cinco dependências da mensagem do colega, além do diagnóstico
de duplicidade usado na elegibilidade. Não representa conclusão de todos os itens da sprint.
O ZIP PI4knorre.zip foi recebido e analisado sem carregar a base de dados. A correspondência está em ANALISE-ZIP-KNORRE.md e o inventário preliminar em INVENTARIO-SPRINT-RICARDO.md. O material não resolve as regras de notas/mínimos por fator.

## Alterações e influência

| Task | Implementação | Influência |
|---|---|---|
| 489 | Trilhas, etapas ordenadas, participação e progresso persistidos | Estrutura para consultar a capacitação por usuário e empresa |
| 491 | Vínculos às conclusões reais, datas e consulta reversa | Libera o ponto de integração da SCRUM-527 |
| 440 | Seleção autorizada e validação de registros | Define uma amostra técnica rastreável para indicadores |
| 442 | Totais, exclusões, problemas e cobertura observada | Alimenta o diagnóstico do calculador SCRUM-396 |
| 443 | Guarda de dados mínimos e endpoint com HTTP 422 | Impede cálculo através do contrato quando a amostra é insuficiente |
| 441 (parte) | Detecção de IDs repetidos e inconsistências | Evita contar cópias e sinaliza dados inválidos; não equivale à homologação da task inteira |

Cada uma das cinco tasks possui documento individual em `docs/scrum/SCRUM-<id>-implementacao-impacto.md`.
O guia `docs/scrum/INTEGRACAO-527-396.md` descreve as rotas e o contrato para o colega.

## Verificação e limites

A compilação e a primeira suíte completa em diretório limpo passaram: 160 testes,
zero falhas, zero erros, 20 ignorados. Depois foram adicionados os testes de duplicidade
e do bloqueio HTTP 422; o resultado final é registrado na seção de evidências abaixo.
Testes usam H2. A migração V17 é validada pelo Flyway em H2; execução no PostgreSQL real
e integração com o calculador do colega permanecem pendentes.

A primeira tentativa de testar na pasta target antiga encontrou uma migração V11 obsoleta;
a execução limpa dos mesmos fontes resolveu o conflito. O código permanece na pasta original.

Não foi aplicado deploy, alterado status no Jira, publicado PR nem enviado mensagem ao colega.
As regras de notas de maturidade e mínimos por fator não foram inventadas.

## Próxima etapa

Comparar o ZIP sem utilizar sua base, atualizar o inventário da sprint e implementar
os itens restantes usando contratos já definidos no projeto. Itens que dependam de regras
de produto devem ficar identificados explicitamente, sem status falso de concluído.

## Evidências finais desta etapa

Após os últimos ajustes, os 6 testes de integração das dependências e o teste de migrações passaram: 7 executados, zero falhas, zero erros e zero ignorados. A suíte completa anterior passou com 160 testes, 20 ignorados. Não foi repetida integralmente após os dois últimos testes; a repetição foi focada no módulo alterado e na migração.

## Atualização após leitura do ZIP

Foram acrescentados CalculoMaturidadeService e a rota /indicadores/maturidade/metodologia, com base na fórmula consolidada da SCRUM-62 e na composição versionada existente. O serviço não calcula notas de fatores a partir de dados brutos. Sem uma nota, mantém o score integral indisponível e não redistribui pesos. A API expõe fontes potenciais e as regras ainda pendentes.

Quatro testes validam a soma 73,25 do exemplo, ausência de fatores, extremos e entradas inválidas. Após essa alteração, 11 testes focados passaram (4 de cálculo, 6 de integração e 1 de migração), sem falhas, erros ou testes ignorados nessa seleção. A suíte completa anterior tinha 160 testes e 20 ignorados.

O inventário inclui os 40 itens históricos; não equivale ao status atual do Jira, cuja consulta expirou. As demais implementações permanecem pendentes. As especificações de origem das notas, mínimos de amostra por fator e classificação de confiança não foram encontradas no ZIP.

## Especificação das notas recebida e implementada como proposta

O usuário forneceu HestIA_Especificacao_Metricas_Maturidade.docx. Ela supera a ausência de fórmulas descrita nas etapas anteriores, mas mantém aprovação de produto pendente. A versão proposta-0.1 foi implementada em um novo módulo de calculadoras, adaptadores e endpoint de resultados. A tela de maturidade usa sessão real, score parcial e cobertura; não usa mais números demonstrativos nessa rota. As fontes e configurações ainda ausentes permanecem explícitas.

O relatório MATURIDADE-PROPOSTA-0.1-IMPLEMENTACAO.md detalha arquivos, efeitos, configuração, testes e limites. Os demais itens da sprint continuam sujeitos a implementação/revisão; o recebimento da especificação não fecha automaticamente todas as tasks.

Validação final desta etapa: 177 testes Java passaram, 20 ignorados, zero falhas/erros; 4 testes de frontend passaram. Nenhuma migração adicional, atualização do Docker ou escrita no banco real foi executada nesta etapa.

## Validação PostgreSQL — 21/10/2026

Docker disponibilizado e PostgreSQL 17.11/PGVector iniciado em contêiner exclusivo de testes, sem volume da base real. Validados upgrade V16 → V17, histórico/checksums Flyway, consultas JSONB com isolamento por empresa/snapshot/projeto, projeto ambíguo e recorte de período de uso responsável. Adicionada MaturidadePostgresTest e atualizada expectativa de migrações em BuscaVetorialPostgresTest de 16 para 17.

Relatórios finais: 225 testes Java, 223 aprovados, 2 ignorados (alertas), nenhuma falha ou erro. Houve uma falha inicial pela expectativa antiga; após correção, as duas classes afetadas passaram novamente (10 testes). A aplicação iniciou com Hibernate validate e Flyway no PostgreSQL; OpenAPI respondeu 200 e contexto sem login respondeu 401. Aplicação encerrada e contêiner parado ao término. Frontend permanece com 4 testes aprovados na execução anterior, sem alterações nesta etapa.

A pendência técnica de validação PostgreSQL desta revisão foi resolvida. A aprovação de negócio da proposta 0.1 e as fontes de dados ainda pendentes permanecem registradas em MATURIDADE-PROPOSTA-0.1-IMPLEMENTACAO.md. Nenhuma alteração foi enviada ao remoto.

### Compatibilidade com a develop atualizada

Durante a conferência final, a develop avançou para c1989f8 (SCRUM-370) e passou a usar V17 para tratamentos de não conformidade. A branch foi atualizada sem perder as alterações locais. A migração de trilhas ainda não publicada foi renumerada para V18; a migração publicada V17 permaneceu intacta. As expectativas de Flyway e PGVector passaram a 18 versões, e o teste de upgrade passou a verificar V17 → V18. A validação foi repetida em outro contêiner vazio, sem reaproveitar o banco que tinha a antiga V17 local. As evidências anteriores desta seção descrevem a primeira passagem e são substituídas pelos resultados finais abaixo.

Resultado final com c1989f8 e V18: suíte Java completa aprovada (236 testes, 234 aprovados, 2 ignorados, nenhuma falha/erro). Aplicação iniciou com Java 21, Flyway e Hibernate validate no PostgreSQL 17.11 isolado. OpenAPI respondeu HTTP 200 e contexto sem autenticação respondeu HTTP 401. Aplicação encerrada e contêiner final parado. Verificação de diferenças sem erros de formatação. Pronto para revisão por PR, mantendo explícitas as pendências funcionais da proposta 0.1 descritas no relatório detalhado.
