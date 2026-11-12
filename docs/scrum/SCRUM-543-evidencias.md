# SCRUM-543 — Política de demonstração no RAG

Validação em 12/11/2026 no PostgreSQL local, banco `hestia_demo_543`, perfil `demo`. Por decisão da equipe, a demonstração usa a política fictícia HestIA v1.1. O conteúdo não representa a política oficial da QSoft.

| Verificação | Resultado observado |
| --- | --- |
| Cadastro e aprovação | Fonte 2, empresa demo 1, status APROVADA e revisão interna 1. |
| Indexação | Vetor da fonte 2 gravado no PostgreSQL. |
| Busca vetorial | Fonte 2 recuperada com o limiar padrão da API. |
| Consulta como colaborador | Fonte 2 retornada pelo Assistente RAG no modo CONSULTAR, com estado REUTILIZADA. |
| Reindexação | Fonte 2 retornou INDEXADA. |
| Retirada | Em registro separado, a fonte 3 ficou RETIRADA e sem vetor. A fonte 2 permaneceu APROVADA e indexada. |

Para usar a mesma fonte em outro banco demo, execute a carga nesse ambiente e anote o ID retornado. O ID 2 se refere somente ao banco validado. O script de publicação e o documento da política não contêm credenciais.
