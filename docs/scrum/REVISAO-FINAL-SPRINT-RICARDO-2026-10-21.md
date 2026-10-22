# Revisão das 40 tasks de Ricardo D’Ávila — 21/10/2026

## Resultado e limites do aceite

Implementações feitas diretamente no projeto original, sem cópia do projeto. Escopo reconciliado com os 40 itens atribuídos a Ricardo na sprint e com os critérios de PO-101 e PO-132 a PO-136. A tabela distingue código implementado, configuração, dependência e homologação. Nenhum status do Jira foi alterado. Nenhum commit, push, merge remoto ou mensagem ao grupo foi realizado nesta revisão.

O código local não equivale ao aceite de todos os 40 itens. A confiança oficial exige SCRUM-427/428 de Luccas; a cobertura SCRUM-396 tem PR94 de Knorre ainda separado; fontes operacionais completas de FinOps/controles, parâmetros aprovados e definições publicadas de aprendizagem/certificação precisam estar disponíveis para homologação. O documento de maturidade recebido é tratado como proposta-0.1, explicitamente não homologada.

## Task, mudança e efeito

| Task | Situação técnica | O que foi feito | Influência no projeto |
|---|---|---|---|
| SCRUM-390 | Implementada | Origem MANUAL/PLATAFORMA/DESCONHECIDA persistida; captura interna com evento idempotente. | Distingue autorrelato e captura sem reclassificar histórico sem prova. |
| SCRUM-391 | Implementada | Contexto autenticado e ferramentas autorizadas preenchem formulário /utilizacoes/registros. | Reduz preenchimento sem inventar tokens, custo, modelo ou finalidade. |
| SCRUM-392 | Implementada | Validação de valores observados, usuário ativo, empresa, datas e estouro de tokens. | Impede persistência de entradas inválidas e atribuição a terceiros. |
| SCRUM-393 | Implementada | Origem na resposta e tela; cenários manual/capturado e idempotência. | Permite interpretar a abrangência do registro. |
| SCRUM-422 | Implementada | ADR-002 registra uso do Express existente como BFF e backend como autoridade. | Evita serviço duplicado e regra de negócio no navegador. |
| SCRUM-423 | Implementada; integração externa pendente | Consulta consolidada tipada com notas elegíveis, evidências, fontes e versões. | Oferece contrato para a cobertura do PR94; não assume o calculador do colega. |
| SCRUM-424 | Implementação local; aceite dependente | Histórico, comparação compatível, cobertura e porta de confiança; ausente classificador retorna NAO_CLASSIFICADA. | Aceite completo exige 427/428 e integração da cobertura 396. |
| SCRUM-425 | Implementada | Escopo por usuário, empresa e departamento; erros explícitos, ausência não vira zero. | Evita vazamento e apresentação de dado incompleto como definitivo. |
| SCRUM-426 | Implementada; homologação conjunta | Frontend chama BFF com sessão real, resultados e histórico. | Remove caminho demonstrativo do indicador; confiança aguarda fornecedor. |
| SCRUM-431 | Implementada como proposta 0.1 | Oito fatores conforme documento de métricas recebido. | Notas mantêm fórmulas e limitações; não são publicação oficial homologada. |
| SCRUM-432 | Implementada | Metodologia expõe fonte, unidade, natureza, mínimos, ausências e dados necessários. | Identifica fontes indisponíveis em vez de estimar valores sem suporte. |
| SCRUM-433 | Implementada | Versões de cálculo, pesos e parâmetros preservadas na apuração com hash. | Nova regra não altera silenciosamente o histórico. |
| SCRUM-434 | Implementada | API e página legível de metodologia junto ao resultado. | Permite verificar composição e limites sem consultar código. |
| SCRUM-435 | Implementada como proposta 0.1 | Produtividade depende de tempo de entrega, tipo de issue e meta configurada. | Quantidade de usos de IA não substitui evidência de produtividade. |
| SCRUM-436 | Implementada | Filtros de período e escopo nas fontes e comparação entre períodos compatíveis. | Impede misturar populações, versões e períodos incompatíveis. |
| SCRUM-437 | Adaptador implementado; dependência externa | ConfiancaIndicadoresPort recebe resultado e integra classificador 427/428 quando disponível. | Sem implementação de Luccas informa não classificada; não há classificador oficial paralelo. |
| SCRUM-438 | Implementada; homologação conjunta | Composição, fatores e metodologia na API e frontend. | Global ausente continua nulo; parcial é exploratório e identificado. |
| SCRUM-439 | Implementada | Cenários conhecidos dos oito fatores, composição e erros matemáticos. | Verifica fórmulas do documento e ausência de denominador/amostra. |
| SCRUM-440 | Implementada | Elegibilidade por registro e requisitos específicos dos fatores. | Entrega evidências à cobertura sem substituir o calculador de Knorre. |
| SCRUM-441 | Implementada | Inconsistências e duplicidades diagnosticadas sem apagar registros. | Dados inválidos não entram silenciosamente no cálculo. |
| SCRUM-442 | Implementada no escopo atribuído | Cobertura técnica da amostra observada e motivos; origem desconhecida sinalizada. | Não confunde essa razão com cobertura populacional ou cobertura ponderada de fatores do PR94. |
| SCRUM-443 | Implementada | Bloqueio de cálculo sem dados mínimos; fatores não elegíveis não publicam score definitivo. | Ausência não recebe nota zero; parâmetros continuam proposta não homologada. |
| SCRUM-444 | Implementada | Testes de isolamento, período, duplicidade, qualidade e mínimos em H2/PostgreSQL. | Inclui restrição de gestor ao departamento e migração incremental. |
| SCRUM-462 | Implementada; provedor a configurar | Tela RAG ligada ao BFF e histórico do autor; ações revalidam fontes. | Remove respostas e ganhos demonstrativos do caminho integrado. |
| SCRUM-463 | Implementada; provedor a configurar | Orquestra buscador PGVector existente, fontes autorizadas e gerador no servidor. | Sem fonte não gera; chamada paga real não foi executada na validação. |
| SCRUM-467 | Material existente; entrega acadêmica pendente | Pacotes/PDFs das duas disciplinas produzidos anteriormente; relatório atual acompanha esta revisão. | ZIPs anteriores não incorporam automaticamente este novo código. Submissão acadêmica não realizada. |
| SCRUM-482 | História agrupadora; homologação pendente | Revisão integrada de 531/532/533 e autorização do fluxo RAG. | Não é implementação extra nem autorização para encerrar a história no Jira. |
| SCRUM-484 | Implementada; configuração necessária | Definições versionadas de certificação, requisitos e histórico de concessões com provas. | ADMIN publica critérios compatíveis com 483; não se inventa certificação sem definição. |
| SCRUM-485 | Implementada; configuração necessária | Eventos de aprendizagem concedem por requisitos reais, de forma idempotente. | Reprocessamento explícito atende definições publicadas após a aprendizagem; sem XP extra inventado. |
| SCRUM-489 | Implementada | Trilhas, etapas, inscrições, status e progresso persistidos. | Conteúdo iniciado fica em andamento; trilha automática exige avaliação compatível. |
| SCRUM-490 | Implementada; catálogo necessário | Motor usa ocorrências confirmadas, recorrência e reprovação; ciclos e evidências deduplicados. | Não toma REQUER_REVISAO isolado como falha; sem conteúdo/avaliação cria pendência explicada. |
| SCRUM-491 | Implementada | Conclusões vinculadas à trilha com usuário, empresa, conteúdo e datas; porta da efetividade. | Acrescenta contexto à 527 sem alterar cálculo de recorrência do Knorre. |
| SCRUM-495 | Implementada | Catálogo, ciclos, tentativas e snapshots versionados do questionário. | Preserva resultado original, autoria, critérios e histórico de novas tentativas. |
| SCRUM-496 | Implementada | Servidor corrige cinco questões, aprovação 80%, três tentativas por ciclo e reforço autorizado. | Não expõe gabarito da tentativa aberta e não consome envio inválido. |
| SCRUM-500 | Implementada | Estados de revisão, validade, justificativa, responsável, hash e versão. | Edição exige revisão e invalida vetor; retirada é terminal para a publicação. |
| SCRUM-502 | Implementada | API administrativa paginada e isolada para consulta/revisão. | Aprovação exige conteúdo, perfis, validade e ausência de sensibilidade. |
| SCRUM-518 | Implementada; visual final é de Kevin | Adaptadores e página funcional /administracao/respostas. | Integra backend sem substituir o desenvolvimento visual das 514–517. |
| SCRUM-531 | Existente na develop; reforçada e testada | Filtros de empresa/perfil antes do LIMIT; revalidação das fontes do modelo. | Somente contexto autorizado segue à geração; histórico também revalida. |
| SCRUM-532 | Implementada | Status e expiração filtrados antes do limite e na recuperação/reutilização. | Bloqueia conteúdo retirado, bloqueado, em revisão ou expirado. |
| SCRUM-533 | Implementada | Decisões de recuperação persistidas em transação própria; consulta ADMIN por empresa. | Preserva evidência dos bloqueios sem registrar pergunta ou conteúdo no log de decisão. |

## Compatibilidade com o grupo

Base da revisão: develop `90c9529`, incluindo PR104 de Kevin. O servidor Express foi conciliado preservando a rota e a tela de ocorrências. Não foram sobrescritos os cálculos existentes de recorrência/efetividade (525–528), sinais de qualidade (501) ou o buscador vetorial.

PR94, branch `features/Scrum-396`: interseção de arquivos alterados/adicionados com esta implementação = 0. A rota `/indicadores/maturidade/cobertura` permanece reservada ao controller do PR. Nossos resultados fornecem `notasElegiveis` e os fatores com amostra, fonte e motivo; a integração do calculador do colega ainda deve ser concluída e testada quando aquele PR estiver disponível. Ausência de conflito textual não prova compatibilidade funcional futura.

O classificador de Luccas entra por `ConfiancaIndicadoresPort`; atualmente o fallback informa NAO_CLASSIFICADA. As regras de amostra/eligibilidade do documento não são apresentadas como uma implementação concorrente das SCRUM-427/428. As telas de microtreinamento de Kevin são reutilizadas com adaptadores; a página administrativa de integração não substitui o visual final das 514–517. O vínculo de trilhas é aditivo no resultado da efetividade, preservando o construtor de dois argumentos para os testes/consumidores Java existentes.

Foram conferidos os PRs abertos consultados durante a revisão e as referências disponíveis localmente. Alterações não publicadas por colegas e futuros commits não podem ser certificados como livres de conflito. A revisão independente exigida por CONTRIBUTING.md continua necessária antes do merge.

## Evidências de validação

- Java completo com PostgreSQL 17/PGVector: **249 executados, 247 aprovados, zero falhas, zero erros, dois ignorados**. Log `sprint-restante-revisao-final.log`, BUILD SUCCESS. Ignorados: chamada real ao provedor de embeddings e ponta a ponta do serviço de alertas sem ALERT_E2E.
- As 23 migrações foram aplicadas em PostgreSQL descartável; atualização da versão 17 para 23 e reaplicação sem migração adicional verificadas. Nenhum banco real ou base do ZIP foi importado.
- Teste adicional de contrato da SCRUM-491 verifica trilha concluída, usuário/empresa, datas e bloqueio de outra empresa. As 11 integrações de aprendizagem foram reexecutadas em PostgreSQL após esse acréscimo: 11 aprovadas, zero falhas/erros.
- Frontend: **6 aprovados**; otimização Python: **8 aprovados**.
- Inicialização completa da aplicação em PostgreSQL descartável, com schema validado: **API OpenAPI HTTP 200**, **contexto sem autenticação HTTP 401**, 87 caminhos registrados. A inicialização final foi executada depois dos testes para evitar concorrência na extensão do banco descartável. Aplicação de teste e container exclusivo encerrados ao final.
- Testes do frontend verificam contexto de sessão, adaptadores, respostas de falha, questionário real e análise dos scripts EJS. Não substituem homologação visual pelo grupo.
- CI passa a executar os testes novos de frontend e os testes PostgreSQL de aprendizagem/maturidade em banco descartável próprio. A execução remota do workflow depende da publicação do código.
- Corrigidos na revisão: parâmetro textual nulo no PostgreSQL; relógio de validade da aplicação na consulta vetorial; status de conteúdo iniciado em trilhas; compatibilidade da retirada legada; isolamento departamental do diagnóstico de qualidade.

## Como validar e configurar

1. Revisar `docs/architecture/ADR-002-bff-integracao-sprint3.md` e `docs/scrum/CONTRATOS-INTEGRACAO-SPRINT-RICARDO.md` para rotas, permissões e corpos dos contratos.
2. Executar a suíte Java (`./mvnw test`), frontend (`npm ci` e `npm test` na pasta do frontend) e Python (`python -m unittest -v` em model/optimization). Para PostgreSQL, usar banco descartável, jamais produção. `HESTIA_PG_TEST_URL`, `HESTIA_MATURIDADE_PG_TEST_URL`, `HESTIA_SPRINT_PG_TEST_URL`, `HESTIA_PG_TEST_USER` e `HESTIA_PG_TEST_PASSWORD` ativam os testes nativos; maturidade exige banco chamado hestia_validacao.
3. Configurar no backend `OPENAI_API_KEY` e `hestia.rag.modelo`; opcionalmente `hestia.rag.ferramenta-id` para instrumentação de uso autorizada. O teste automatizado usa gerador simulado para conferir autorização e idempotência, sem chamada paga.
4. Com ADMIN da empresa, publicar questionários e requisitos de certificação aprovados. Não há gabaritos ou conquistas de produção sem decisão do grupo. Publicar requisito depois da aprendizagem requer `/certificacoes/usuarios/{id}/verificar` ou próximo evento de aprendizagem para processar o legado elegível.
5. Em `/utilizacoes/registros`, conferir identidade/ferramentas e origem. Na tela RAG existente, conferir fontes/histórico, indisponibilidade e retirada. Em `/administracao/respostas`, aprovar/bloquear conteúdo e confirmar que busca/reutilização respeitam a decisão. Em microtreinamentos, finalizar leitura e responder avaliação publicada.
6. Para maturidade, configurar mapeamentos autorizados e metas versionadas do servidor. A fonte operacional atual não inventa dados de controles ou produtividade inexistentes. Sem fonte mínima, o fator e o score global continuam ausentes, com motivo.
7. Homologar confiança com Luccas e cobertura com Knorre. Revisar o PR e o pipeline antes de integrar à develop.

## Migrações e efeitos permanentes

V18 adiciona trilhas e progresso; V19 aprendizagem, releituras, avaliações e certificações; V20 ciclos automáticos/evidências; V21 origem e avanço do conteúdo; V22 revisão/validade e auditoria de decisões; V23 histórico RAG e apurações. São aditivas e exigem backup/processo normal de migração ao publicar. A validação local usou container exclusivo, não a base operacional.

## Documentação anterior

Relatórios anteriores mantêm contexto histórico e devem ser lidos junto desta revisão. Os ZIPs/PDFs acadêmicos entregues anteriormente representam a versão de quando foram gerados; não foram apresentados como contendo automaticamente todas as alterações desta rodada. A entrega institucional e o fechamento da SCRUM-467 permanecem ações de Ricardo.
