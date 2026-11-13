# SCRUM-571 — Matriz de evidências dos controles de segurança

Catálogo: `seguranca-1.0`, conforme a SCRUM-539. Esta matriz identifica evidências candidatas e o que falta para verificar cada controle na release e no ambiente avaliados. Os checks do PR validam a implementação; não comprovam, por si só, o atendimento dos controles na release.

| Controle | Evidência disponível ou candidata | O que falta para verificar |
| --- | --- | --- |
| SEG01 — Acesso por perfil | Na demo, GET /auditoria retornou 200 para ADMIN, 403 para GESTOR e FUNCIONARIO e 401 com token inválido. | Validado para a rota de auditoria. |
| SEG02 — Isolamento | Na demo, administradores das empresas 1 e 3 receberam apenas registros próprios sem filtro e 403 ao solicitar a outra empresa, nos dois sentidos. AuditoriaControllerTest cobre o filtro padrão e o bloqueio. | Isolamento da auditoria validado. Alteração, histórico e exportação por empresa e departamento permanecem com cobertura parcial. |
| SEG03 — Revogação | Na demo, GET /api/contexto retornou 200 antes do logout, POST /auth/logout retornou 204 e o mesmo token recebeu 401 na consulta seguinte. | Revogação por logout validada na demo. |
| SEG04 — Fontes RAG | ManutencaoConteudoServiceTest e OrcamentoContextoRagServiceTest passaram. Cobrem invalidação de vetor por alteração de conteúdo e descarte de contexto após retirada. | Validado nos cenários automatizados executados. |
| SEG05 — Credenciais e canais | Backend lê a chave da OpenAI do ambiente. | Conferir transporte protegido no ambiente publicado, tratamento de segredos e conteúdo de logs/respostas. Ambiente publicado: SCRUM-555. |
| SEG06 — Auditoria | Na demo, a consulta registrou GET /api/contexto, HTTP 200, usuário 1 e empresa 1 em 13/11/2026 às 01:19:18. O acesso por perfil e o isolamento entre empresas foram conferidos no SEG01 e SEG02. | Registro e acesso validados para o cenário. Falta conferir ausência de segredos nos fluxos críticos. |
| SEG07 — Falhas e repetição | Três testes passaram, com zero falhas, erros ou ignorados. Os cenários cobrem falha de geração, reenvio de interação e reenvios concorrentes sem duplicação de contadores. | Validado nos cenários executados. Timeouts e retentativas dos demais fluxos permanecem pendentes. |
| SEG08 — Restauração | Ensaio de 13/11/2026 com backup de hestia_demo_543 restaurado em container PostgreSQL 17/pgvector isolado. Checkout registrado no ensaio: 61c4252 (SCRUM-594). SHA-256: C971D2EB18FE4999A5F0A84183C65B1B7768024757B8426A51166F706375AA50. Contagens iguais na origem e na restauração: 29 migrations, 8 usuários, 3 registros de uso, 0 treinamentos e 0 registros de ciência. | Validado no ambiente demo com V29. A homologação no ambiente publicado requer evidência própria. |

Para registrar uma verificação conclusiva no manifesto, vincular o resultado e sua referência à release ou commit, ambiente, data, responsável e escopo autorizado. Sem comprovação suficiente, manter `NAO_VERIFICADO`. Nenhum resultado foi lançado no manifesto por esta matriz.
