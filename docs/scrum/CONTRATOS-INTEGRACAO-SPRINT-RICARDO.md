# Contratos de integração das tasks de Ricardo - 21/10/2026

## Origem e preenchimento (390 a 393)

`GET /registros-uso-ia/contexto` fornece usuário, empresa e ferramentas do escopo autenticado. Tokens, custo, modelo e finalidade não são inferidos de dados inexistentes. O formulário real está em `/utilizacoes/registros`; os adaptadores BFF são `/api/registros` e `/api/registros/contexto`.

Novos registros manuais recebem MANUAL. `RegistroUsoIAService.criarCapturado` é um ponto interno de captura, sem endpoint que permita ao cliente declarar-se PLATAFORMA. Ele exige evento identificável e medidas disponíveis. Eventos de geração RAG usam esse ponto somente com ferramenta configurada e autorizada e tokens efetivamente retornados. Registros anteriores sem prova preservam DESCONHECIDA. Uso manual não significa monitoramento integral de ferramentas externas.

## Avaliações (495 e 496) e integração com 491

ADMIN publica `POST /aprendizagem/catalogo` com microtreinamentoId, versao e cinco questoes. Cada questão tem id, enunciado, alternativas (mapa com pelo menos três), correta e explicacao. A publicação preserva o SHA-256 do conteúdo ativo. Publicações são novas versões crescentes; não há edição de gabarito de tentativa aberta.

`GET /aprendizagem/conteudos` e `/{id}` fornecem conteúdo e progresso. `POST /aprendizagem/conteudos/{id}/progresso` recebe etapa e só avança uma etapa por vez. Ao finalizar o conteúdo, cria evidência de leitura e conserva a conclusão existente. Revisões de reforço podem ser registradas com chave nova em `POST /aprendizagem/microtreinamentos/{id}/conclusao`, corpo `{chave}`. Isso não apaga nem muda a data de uma conclusão anterior.

`POST /aprendizagem/catalogo/{id}/tentativas` abre ou retorna a tentativa em andamento. `GET /aprendizagem/tentativas/{id}` omite gabarito antes da finalização. `POST /aprendizagem/tentativas/{id}/respostas` recebe `{respostas:[{questaoId,alternativa}]}`. Somente envio completo e válido é corrigido. Cinco questões têm peso igual, aprovação exige 80 e há até três tentativas finalizadas por ciclo. Reenvio de tentativa finalizada retorna a nota original. Uma aprovação válida não é anulada por média de reprovações.

Terceira reprovação exige reforço. `POST /aprendizagem/ciclos/{id}/liberacao` recebe chave idempotente, justificativa e IDs de leiturasReforco do usuário/conteúdo compatível, posteriores à última tentativa. ADMIN tem autorização administrativa; outros responsáveis precisam de concessão explícita em `PUT /aprendizagem/permissoes/{usuarioId}` feita por ADMIN. O perfil GESTOR, por si só, não libera novos ciclos. `DELETE` na mesma rota revoga a permissão. O ciclo novo referencia o anterior. Histórico: `/aprendizagem/usuarios/{id}/historico`.

Os templates de Kevin usam adaptadores BFF, sem correção ou gabarito no navegador. Não há equivalência presumida entre IDs textuais do antigo catálogo demonstrativo e IDs do catálogo Java. Os conteúdos Java ativos são a fonte dessa integração.

## Certificações (484 e 485)

`POST /certificacoes/definicoes`, somente ADMIN, publica codigo, nome, nivel, versao e avaliacoesObrigatorias (IDs de versões publicadas). Requisitos não são adivinhados quando faltam. As evidências de aprendizagem precisam pertencer ao mesmo usuário e empresa e corresponder à versão e ao conteúdo exigidos.

Eventos de leitura e correção verificam automaticamente requisitos publicados no fechamento da transação. `POST /certificacoes/usuarios/{id}/verificar` permite reprocessar de forma idempotente. `GET /certificacoes/usuarios/{id}` fornece o histórico, incluindo código, nome, nível, versão, data e evidências preservadas. A unicidade é por empresa, usuário e definição versionada. XP ou acessos não são critérios paralelos; não há recompensa de XP nova inventada por esta implementação.

A proposta da SCRUM-483 descreve a certificação de fundamentos com quatro temas. O ADMIN precisa publicar seus requisitos compatíveis; a API não cria conquistas antes disso. Nova versão não remove concessões anteriores; não foi definida expiração automática de certificações.

## Trilhas (489, 490 e 491)

O motor usa eventos de avaliações reprovadas e fatos de conformidade identificáveis. IA não autorizada e política inativa são condições automáticas reconhecidas na regra de conformidade 1.0. Outros critérios exigem confirmação humana explícita vinculada à avaliação em `POST /trilhas/ocorrencias-confirmadas` (avaliacaoId, tipoErro, justificativa). REQUER_REVISAO, sozinho, não é tratado como falha confirmada.

Recorrência usa três eventos distintos do mesmo tipo, usuário e empresa em 30 dias, limites inclusivos e sem datas futuras. Reprocessamento não duplica evidências. Proteção de dados, LGPD, política e ferramenta não autorizada têm gatilho imediato quando confirmados. Falhas de aprendizagem acionam trilha adaptativa. Tema sem conteúdo ativo e avaliação compatível fica em pendências de catálogo; não se cria trilha vazia.

No máximo um ciclo ativo por usuário/empresa/tema. Evidência nova se vincula ao ciclo ativo. Reincidência posterior à conclusão cria novo ciclo com vínculo ao anterior; eventos anteriores não bastam para reabrir. A nova trilha preserva requisitos de avaliação e exige aprendizagem compatível para concluir. Após reincidência, aprovações anteriores ao corte não concluem automaticamente o ciclo novo; pode ser necessária liberação autorizada de um ciclo de reavaliação, conforme a SCRUM-494.

`POST /trilhas/usuarios/{id}/processar` reprocessa fontes existentes. `GET /trilhas/usuarios/{id}/ciclos` e `/pendencias` explicam estado e motivo. Endpoints originais de trilhas continuam disponíveis. `/trilhas/conclusoes/{id}` relaciona etapas às conclusões. O indicador de efetividade existente recebe um campo adicional trilhas por meio de VinculosTrilhaConclusaoPort; seus cálculos de recorrência permanecem os de Knorre.

## Revisão, elegibilidade e auditoria (500, 502, 518, 532 e 533)

Estados: EM_REVISAO, APROVADA, BLOQUEADA e RETIRADA; expiração é uma condição de elegibilidade pelo horário da aplicação. Registros legados mantêm elegibilidade compatível com flags e perfis anteriores, sem atribuição fictícia de revisor; reutilizavel=false legado é conservado como retirado. Novos cadastros do serviço iniciam EM_REVISAO.

ADMIN consulta `/api/admin/respostas?pagina=0&tamanho=50`, detalhes `/{id}` e decide por `POST /{id}/revisoes` com status, justificativa e expiraEm opcional. Aprovação exige conteúdo, perfis autorizados, ausência de dados sensíveis e validade compatível. Retirada é terminal para aquela publicação. Alterações de texto exigem nova revisão e invalidam embeddings. O histórico registra responsável, estados, justificativa, versão, hash e data.

Os filtros de status, validade, empresa e perfil ocorrem antes do LIMIT da busca vetorial e também na recuperação textual. Reutilização revalida a elegibilidade. Decisões de recuperação usam log específico, sem pergunta ou conteúdo sensível, em transação independente para preservar bloqueios; consulta administrativa por empresa em `/auditoria/decisoes-recuperacao`.

`/administracao/respostas` é uma página funcional de integração. Não substitui a elaboração visual das tasks 514 a 517 de Kevin. Os indicadores de uso existentes permanecem no módulo de Knorre (501); ausência de sinais de aceitação/rejeição não recebe contagens inventadas.

## RAG (462 e 463)

`POST /api/rag/perguntas` recebe chave idempotente, pergunta, modo CONSULTAR, GERAR_NOVA ou REUTILIZAR e respostaId para reutilização. A empresa e a identidade vêm da sessão. A consulta chama o BuscaVetorialService existente, revalida candidatos e envia somente seus trechos ao gerador. Sem conteúdo autorizado, não chama geração. REUTILIZAR usa o serviço existente de reutilização e recompensas, sem duplicar seus cálculos. Geração depende de `OPENAI_API_KEY` e `hestia.rag.modelo`, configurados no servidor. Instrumentação de uso depende de `hestia.rag.ferramenta-id` da empresa autorizada.

`GET /api/rag/historico` é restrito ao próprio usuário/empresa e revalida fontes e versões; retirada, expiração ou revogação impede reexpor o texto anterior. Não há cache compartilhado entre empresas ou perfis. A tela existente foi ligada a essas rotas, removendo respostas e estimativas de tempo/XP demonstrativas do caminho integrado. Similaridade numérica e confiança não são fabricadas quando o contrato atual do buscador não as fornece.

## Maturidade e produtividade (422 a 439)

Mantêm-se `/indicadores/maturidade/resultados`, `/metodologia` e a composição existente. `POST /consulta-consolidada` agrega resultado e confiança pelo contrato ConfiancaIndicadoresPort. Sem fornecedor do classificador de Luccas, expõe NAO_CLASSIFICADA; as evidências de amostra, cobertura e elegibilidade são fornecidas.

`POST /apuracoes` guarda um registro experimental completo de consulta, com período, fontes, escopo, parâmetros, versões e hash. `GET /apuracoes` e `/{id}` preservam a regra original, sem recalcular o passado com regra atual. `/evolucao?anteriorId=...&atualId=...` exige mesmo escopo, natureza, versões, parâmetros e duração, com períodos não sobrepostos. Diferenças globais ausentes não viram zero; há comparação dos fatores elegíveis compatíveis.

A interface fornece metodologia legível, histórico, preservação da apuração e comparação. As regras seguem a proposta de maturidade 0.1 recebida. A confiança oficial (427/428), a conclusão do PR #94 de cobertura, fontes de controles/FinOps e parâmetros aprovados continuam dependências explícitas; não são tasks atribuídas a Ricardo que possam ser substituídas silenciosamente.
