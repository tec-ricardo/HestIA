# SCRUM-571 — Matriz de evidências dos controles de segurança

Catálogo: `seguranca-1.0`, conforme a SCRUM-539. Esta matriz identifica evidências candidatas e o que falta para verificar cada controle na release e no ambiente avaliados. Os checks do PR validam a implementação; não comprovam, por si só, o atendimento dos controles na release.

| Controle | Evidência disponível ou candidata | O que falta para verificar |
| --- | --- | --- |
| SEG01 — Acesso por perfil | `ControleAcessoInterceptorTest`: funcionário bloqueado na administração; gestor autorizado na avaliação. | Registrar os cenários dos três perfis e sessão inválida na versão avaliada. |
| SEG02 — Isolamento | `ManutencaoConteudoServiceTest` verifica bloqueio de alteração e retirada entre empresas; `FluxoRagPoliticaEmpresaIntegrationTest` verifica isolamento de conversas. | Confirmar a cobertura de consulta, alteração, histórico e exportação por empresa e departamento. |
| SEG03 — Revogação | `ReindexacaoServiceTest` cobre interrupção por revogação e bloqueio de usuário inativo. | Demonstrar revogação com sessão já aberta na release avaliada. |
| SEG04 — Fontes RAG | `PerguntasConhecidasPostgresTest` cobre fonte retirada; a SCRUM-543 registrou retirada e remoção do vetor em demo local. | Comprovar também invalidação de histórico e contexto quando a fonte é alterada ou deixa de ser autorizada. A evidência local não vale automaticamente para outra release ou ambiente. |
| SEG05 — Credenciais e canais | Backend lê a chave da OpenAI do ambiente. | Conferir transporte protegido no ambiente publicado, tratamento de segredos e conteúdo de logs/respostas. Ambiente publicado: SCRUM-555. |
| SEG06 — Auditoria | `AuditoriaInterceptorTest` cobre o registro de uma ação autenticada. | Conferir campos, acesso restrito e ausência de segredos nas operações críticas. Interface: SCRUM-611. |
| SEG07 — Falhas e repetição | `FluxoRagPoliticaEmpresaIntegrationTest` cobre falha de geração e reenvio de interação. | Confirmar timeout, retentativa e concorrência sem efeitos duplicados nos fluxos críticos da release. |
| SEG08 — Restauração | [Ensaio da SCRUM-588](evidencias/SCRUM-612-validacao.json) em PostgreSQL isolado, com SHA-256 e cinco contagens conferidas. A base restaurada tinha 28 migrations. | NAO_VERIFICADO para a versão atual. Registrar a restauração de um backup da versão avaliada, que inclui a V29. |

Para registrar uma verificação conclusiva no manifesto, vincular o resultado e sua referência à release ou commit, ambiente, data, responsável e escopo autorizado. Sem comprovação suficiente, manter `NAO_VERIFICADO`. Nenhum resultado foi lançado no manifesto por esta matriz.
