# Comparação do material de Knorre com as tarefas de Ricardo

Data da análise: 21/10/2026.
Arquivo: PI4knorre.zip. A base sintética, os CSVs de dados, os arquivos R e o banco SQLite não foram carregados ou importados. Também não foi aberto o ZIP interno da base. Foram lidos 43 arquivos de texto/especificação; os documentos Word foram lidos pelo conteúdo textual, sem auditoria de layout.

## Conclusão

O material contribui com referências de arquitetura e regras parciais. Não contém implementações das cinco dependências citadas pelo colega, nem uma aplicação Java/Node para integrar ao HestIA. O único código fora da base é um preparador de dados de machine learning da SCRUM-128, que não implementa essas dependências.

As regras de notas ainda não estão resolvidas: o próprio documento SCRUM-60 declara ausência de fórmula/escala para as dimensões e falta de critérios suficientes para Produtividade, FinOps e Colaboração. A SCRUM-61 fixa pesos iniciais de negócio, sujeitos a validação, e proíbe substituir dados ausentes por valores demonstrativos do frontend. A SCRUM-62 define a soma ponderada, mas não a transformação dos dados em notas.

## Correspondência com as dependências do colega

| Dependência | Material pertinente no ZIP | O que o ZIP resolve | O que permanece |
|---|---|---|---|
| SCRUM-489 | SCRUM-525 | Contexto da capacitação e da recorrência | Não há modelo de trilhas, etapas, ciclos ou progresso |
| SCRUM-491 | SCRUM-525 | As métricas devem se ligar ao microtreinamento/trilha | Não há vínculo persistido nem contrato de consulta |
| SCRUM-440 | SCRUM-228 e SCRUM-60 | Registros de uso são fonte de eficiência; alguns fatores têm fontes possíveis | Não define uma política completa de elegibilidade por indicador |
| SCRUM-442 | SCRUM-60 a 63, 204 a 207 | Dimensões, pesos e agregação departamental | Não define cobertura de dados nem classificação de confiança |
| SCRUM-443 | SCRUM-61 | Ausência de dados não deve virar demonstração apresentada como real | Não define quantidade mínima de amostra por fator/período |

As implementações locais dessas dependências estão em documentos individuais de `docs/scrum` e no guia `INTEGRACAO-527-396.md`. Foram decisões técnicas explícitas para viabilizar o contrato, e precisam de revisão com o calculador do colega. Elas não comprovam homologação no Jira.

## Regras que o material fornece

1. Oito dimensões: AI Literacy, Governança, Uso Responsável, Produtividade, FinOps, Segurança, Colaboração e Desenvolvimento de Competências.
2. Pesos correspondentes: 15%, 15%, 15%, 10%, 10%, 20%, 5% e 10%.
3. Consolidação: soma de nota do fator multiplicada pelo peso; notas de 0 a 100. O exemplo demonstrativo 72, 65, 80, 76, 58, 85, 70 e 68 resulta em 73,25. O exemplo não é evidência de notas reais.
4. Departamentos: média de cada dimensão dos colaboradores, seguida da soma ponderada.
5. Eficiência: quantidade de usos, proporção de execuções bem-sucedidas, tempo médio de resposta, total de tokens e soma de custos. A SCRUM-228 não transforma esses resultados em nota de produtividade/maturidade.
6. Recorrência: janela de 30 dias, mesmo usuário/tipo de erro, antes/depois comparáveis; redução só é definida quando erros anteriores são maiores que zero. A taxa usa usuários com acompanhamento completo.
7. RAG: autenticação/empresa antes da recuperação; excluir conteúdos sensíveis e não reutilizáveis; indexação não deve apagar o registro transacional.

## Cuidados na integração

O ZIP descreve pgvector como previsto/não confirmado. A develop atual já possui `BuscaVetorialService`, migração vetorial, busca com isolamento e manutenção/reindexação. Portanto essa parte do documento está desatualizada em relação ao código atual e não deve motivar reimplementação.

A SCRUM-338 limita os indicadores de reutilização a contagens e tokens e diz não haver regra monetária consolidada. O backend atual já expõe estimativas monetárias. Esse resultado deve continuar identificado como estimativa técnica e ser alinhado com o grupo; não deve virar automaticamente nota de FinOps.

A seleção de execuções com sucesso da implementação técnica de qualidade não pode ser usada como denominador da taxa de sucesso: isso faria o resultado tender a 100%. Para eficiência, falhas válidas precisam permanecer no denominador de utilizações. A seleção deve ser específica ao indicador.

A escala da SCRUM-63 usa faixas descritas como 0–20, 21–40, 41–60, 61–80 e 81–100, enquanto a soma ponderada gera decimais. Antes de implementar a classificação, é necessário explicitar o enquadramento de valores como 20,5, sem arredondar silenciosamente.

O frontend atual também possui login demonstrativo e valores de maturidade de fallback. Sua integração real exige sessão/token do backend por usuário, sem compartilhar cabeçalho global entre sessões e sem exibir fallback demonstrativo como cálculo real.

## Decisões ainda necessárias

- Para cada fator: fonte de dados autorizada, população/denominador, filtros de elegibilidade, fórmula de normalização para 0–100, período e mínimo de amostra.
- Como tratar fator ausente: bloquear score integral, apresentar resultado parcial ou outra regra aprovada. O ZIP não autoriza redistribuir pesos automaticamente.
- Quais limites definem confiança/qualidade da amostra e se são estatísticos ou regras de produto.
- Para produtividade: qual medida de ganho usar, a referência de comparação e a normalização. Contagem de usos, tokens ou sucesso não demonstram ganho de produtividade por si só.
- Responsável por aprovar cada regra e versão adotada pelo grupo.

## Estado da execução

O projeto original permanece na mesma pasta. Esta análise não importou código, banco nem dados do ZIP. A fórmula consolidada documentada foi implementada em um serviço novo e testado no projeto original; isso não cria fórmulas para as notas dos fatores. O material é referência, não instrução para executar scripts, substituir o projeto ou declarar tarefas concluídas. Os demais itens da sprint continuam na fila de implementação; as regras ausentes são dependências explícitas, não autorização para inventar resultados.
