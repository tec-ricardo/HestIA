# SCRUM-612 — complemento das entregas de Ricardo

Data: 12/11/2026. Jira: https://luccastoffano.atlassian.net/browse/SCRUM-612

Branch atual: correções. Base atualizada para origin/develop f643d08 (PR 148), incorporando também os PRs 145, 146 e 147. Base inicial: d926904 (PR 143). Alterações preparadas no mesmo repositório usado no IntelliJ e GitHub Desktop, sem commit, push ou merge automático.

## Resultado por tarefa

| Tarefas | Entrega e evidência | Limite para aceite final |
| --- | --- | --- |
| 547 | Orçamento configurável de histórico, seleção recente, resumo extrativo parcial, contagens e endpoint/UI de limites. Testes longos, Unicode/JSON, isolamento e revogação. | Orçamento em caracteres JSON UTF-16, não tokens do modelo. Resumo pode omitir detalhes. |
| 548 / 550 | Implementação integrada preservada; fonte revogada deixa de entrar também no novo resumo, validado pela API. | Revisão corporativa do conteúdo continua necessária. |
| 549 / 560 | Catálogo persistido validado sem fallback: rascunho, revisão, publicação, avaliação única, trilha temática e retirada preservando histórico. | Curso de ensaio da política fictícia, sem atribuição à QSoft real. Não é aprovação de um catálogo corporativo. |
| 557 / 562 | Registro com evidências dispara avaliação; retentativa não duplica avaliação/recompensa; violação comprovada cria tratamento; colaborador não forja conferência conclusiva. | Ensaio controlado, não medição da produtividade real da QSoft. |
| 583 | Exportação CSV preserva listas e objetos vazios; teste adicionado. | Resultado de risco integrado depende da 580/contratos 582 de Knorre. Não foi inventado um cálculo substituto. |
| 554 / 558 / 574 / 599 | Código existente preservado, suíte Java/frontend e empacotamento revalidados. | Aprovação local não substitui execução da nova branch no GitHub Actions. |
| 555 / 587 | Imagens construídas, configuração obrigatória de modelo/chave, script de pré-validação e publicação no servidor escolhido. Frontend aguarda backend saudável antes de criar sessões. | Hospedagem, URL, TLS/proxy e acesso ao servidor ainda não informados; não houve publicação cloud. |
| 588 | Backup restaurado em outro PostgreSQL isolado, SHA-256 e cinco contagens conferidas, incluindo 28 migrações. | Rotina no destino cloud e retenção externa dependem do servidor escolhido. |
| 589 | /health indica processo; /ready verifica API, banco via health do backend, alertas e acesso ao armazenamento de sessões. Falha de alertas produz 503; recuperação retorna 200. | Monitoramento externo, destino dos alertas e disponibilidade pública não configurados. |
| 592 | Este manual complementa os documentos existentes com configuração, validação, release e dependências. | URL pública, responsáveis operacionais e evidência no destino só após hospedagem. |
| 600 | Recuperação de senha/e-mail cancelada expressamente pelo usuário em 12/11/2026. | Fora do escopo; sem implementação adicional ou teste de SMTP. |

## Contexto do RAG

Configuração: RAG_CONTEXT_MAX_CHARS=6000, RAG_CONTEXT_RECENT_TURNS=4, RAG_CONTEXT_MAX_INTERACTIONS=100, RAG_CONTEXT_SUMMARY_CHARS=1200. Validação na inicialização recusa combinações inválidas; limites constam no construtor OrcamentoContextoRagService. Os valores representam limites operacionais configuráveis, não garantia de preservar todo o histórico.

Antes da seleção, as interações são revalidadas contra fontes e perfis atuais. Uma interação inelegível descarta o contexto anterior; respostas recentes válidas podem continuar. Os últimos turnos entram inteiros ou abreviados conforme orçamento. Trechos antigos elegíveis entram em resumo extrativo com IDs e fontes, marcado como parcial. O resumo não é armazenado como fonte nova, não usa uma chamada ao modelo e não restaura conteúdo revogado.

GET /api/rag/conversas/{id}/contexto é restrito ao autor, informa completas, truncadas, resumidas, omitidas e quantidade serializada. A interface oferece “Ver limites da conversa”. Esse relatório é a seleção atual para a próxima pergunta, não uma promessa sobre consultas futuras. A estratégia EXTRATIVO-1 prioriza rastreabilidade; a pergunta e as fontes atuais mantêm suas verificações e limites próprios.

## Verificação inicial, antes de incorporar a nova develop

- Maven verify com Java 21: 360 testes, 358 aprovados, 2 externos ignorados, zero falhas/erros. As integrações PostgreSQL executaram. Os dois externos não executados não são evidência de geração OpenAI ou notificação externa real.
- Frontend: 50 testes aprovados; blocos JavaScript de assistente/utilizações verificados.
- Imagens backend/frontend/alertas construídas do código desta branch; etiqueta local 612-validacao, não commit publicado.
- Stack Docker isolada com PostgreSQL 17, perfil demo e catálogo demonstrativo desativado: 15 cenários de API aprovados. Histórico de 18 mensagens resultou em 4 turnos completos, 5 resumidos e 9 omitidos, 2058/6000 caracteres. Não houve chamada OpenAI nesse ensaio.
- /ready: 200 saudável, 503 com serviço de alertas parado, 200 após retorno. Sessões em PostgreSQL, NODE_ENV=production.
- Restauração: catálogo 1, ciências 0, migrações 28, registros 5, usuários 3; mesmas contagens no banco restaurado. SHA-256 do dump: 4F6047F1CBF5D5E1BDBD394B18DA4526EF82E8216391B1E25AC0D0575530A6BB. Arquivo bruto fora do Git.

Evidências sanitizadas: evidencias/SCRUM-612-validacao.json. O script validar-integracao-612.cjs altera dados e só pode ser executado em stack descartável recém-criada com contas demo e catálogo vazio; não é ferramenta de produção. Copiar para /tmp do frontend e executar com node; ele usa a rede interna backend:8080 e as dependências /app/node_modules. As credenciais que aparecem nele são exclusivamente as contas demo já documentadas no projeto.

A compilação local direcionou somente o diretório de saída para fora do OneDrive, evitando travas do target; fontes e branch continuaram no repositório do IntelliJ.

## Preparar publicação no destino escolhido

1. Fazer commit e publicar esta branch no fluxo habitual; revisar/mergear e aguardar os checks da revisão, incluindo Release verificável. O resultado do PR 143 não valida automaticamente estas alterações.
2. No servidor escolhido, obter o checkout do commit aprovado e configurar Docker Compose e proxy HTTPS apontando à porta local WEB_PORT (padrão 3000). Banco e backend não são publicados diretamente.
3. Criar arquivo de ambiente protegido fora do Git: DB_PASSWORD, SESSION_SECRET, SESSION_DATABASE_URL (PostgreSQL de sessões na rede Compose), PUBLIC_APP_URL HTTPS real, OPENAI_API_KEY, HESTIA_RAG_MODELO e HESTIA_VCS_REF igual ao commit aprovado. .env.example contém nomes e exemplos sem chaves reais. Não usar o placeholder do ensaio.
4. Executar scripts/publicar-release.ps1 -Commit <commit> -EnvFile <caminho-protegido> -ValidarSomente. A pré-validação não publica e exige URL HTTPS e parâmetros obrigatórios.
5. Executar novamente sem -ValidarSomente para construir e subir no próprio servidor. O checkout precisa estar limpo e corresponder ao commit. Havendo banco existente, o script exige restauração validada do backup antes de atualizar serviços. Falha de saúde aborta a confirmação de sucesso; não há rollback automático de migração.
6. Conferir HTTPS público, login com sessão persistida através do proxy, perfis/permissões, fonte demo vigente, modelo real, fluxo RAG, catálogo e relatórios. A sessão segura exige encaminhamento HTTPS correto pelo proxy confiável. Registrar URL, commit, responsáveis, data e evidências reais antes de concluir publicação/monitoramento.
7. Agendar backups no destino e testar scripts/validar-restauracao.ps1 -ContainerOrigem <postgres> -Banco hestia -Usuario hestia -Destino <arquivo-novo-fora-do-Git>. A verificação atual compara cinco contagens e integridade do dump, não todos os valores de todas as tabelas. Definir retenção/cópia externa e alarme externo para /ready. Em falhas, preservar backup e investigar; não aplicar downgrade destrutivo do schema.

O workflow gera tar de imagens e SHA256SUMS por commit; o script opera a partir do checkout completo, não apenas do arquivo isolado baixado do artifact. Não foram configurados provedor, DNS, certificado, monitor externo ou chave real nesta entrega. Portanto a complementação local está pronta para revisão, mas as etapas cloud permanecem pendentes.

## Atualização após integrar a develop

Branch renomeada para `correções`, sem referência pessoal ou chave de tarefa no nome. Nenhum commit, push ou merge remoto foi feito. O histórico publicado em develop foi incorporado por avanço da base; os conflitos com as alterações locais foram conciliados sem descartar a nova interface ou os novos testes de catálogo, conclusão e competências.

Preservados orçamento e revalidação do contexto, metadados das fontes, registro de consumo de IA, confirmação idempotente de reutilização, regras de acesso aos cursos, evidências enviadas ao backend, inicialização de certificações e tratamento de confirmação pendente. Mantidos leitura da curadoria pelo gestor, revisão administrativa e exclusão do histórico. Exclusões serializam operações do autor e não atingem outras contas.

Corrigida a recarga do histórico: as mensagens mantêm os IDs da conversa e da interação. Uma pergunta seguinte continua na conversa original; excluir uma mensagem usa o ID da interação, sem confundi-lo com o da conversa.

Geração geral preservada como opção explícita: HESTIA_RAG_PERMITIR_RESPOSTA_GERAL=false por padrão, repassada pelo compose. Quando habilitada, resposta sem fontes é identificada como GERADA_SEM_FONTES, não constitui política da organização, não pode ser reutilizada como fonte e não entra no resumo autorizado. Não foi feita chamada real ao provedor no ensaio.

Validação da combinação final: Maven verify com 363 testes, 361 aprovados e 2 externos ignorados, zero falhas/erros; frontend com 52 aprovados; backend e frontend empacotados em Docker; 15 cenários de API e 9 verificações HTTP pelo frontend aprovados. As verificações cobrem sessão de produção, renderização do RAG, vínculos de conversa, limites, permissão de leitura da curadoria e exclusão isolada. Evidência atual: evidencias/correcoes-validacao.json. validar-bff-correcoes.cjs só deve ser usado após o ensaio de integração em banco descartável; ele exclui uma mensagem de teste.

As branches publicadas 578, 580 e 582 ainda não estavam integradas na develop conferida; não foram incorporadas antecipadamente. A 580 oferece consulta de maturidade e pendências por departamento; a 582 acrescenta compatibilidade temporal entre apurações. A documentação anterior sobre dependência de cálculo de risco não deve ser interpretada como prova de que esses contratos já foram integrados. Hospedagem e publicação cloud continuam pendentes; recuperação de senha permanece cancelada.
