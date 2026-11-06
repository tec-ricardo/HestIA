# SCRUM-536 — Conversas do histórico RAG

Base: develop, commit 08f4c9b. Branch: features/SCRUM-536-539-540-541-542-conversas-e-regras.

## Entrega

As interações continuam em interacoes_rag. A nova tabela conversas_rag identifica assunto, autor e empresa. A migração V24 preserva cada interação antiga em sua própria conversa: não é possível inferir assuntos compartilhados a partir do histórico legado. A chave estrangeira composta impede vincular uma interação a conversa de outro autor ou empresa.

## Contrato

- POST /api/rag/conversas, corpo {"titulo":"Proteção de dados"}: cria conversa e retorna id, título e data.
- GET /api/rag/conversas: lista as últimas 100 conversas do usuário autenticado.
- POST /api/rag/perguntas: recebe o campo opcional conversaId, além do contrato anterior. Reutilizar o ID continua a mesma conversa; omitir cria uma conversa independente.
- GET /api/rag/conversas/{id}/historico: retorna as últimas 100 interações em ordem cronológica, com fontes revalidadas no acesso.
- GET /api/rag/historico: preservado para clientes existentes.

Conversas inexistentes ou de outro autor/empresa retornam 404. Repetir a mesma chave de pergunta na mesma conversa retorna a interação existente; reutilizar essa chave explicitamente em outra conversa retorna 409. Empresa e usuário vêm do contexto autenticado.

## Validação

Executado com o JDK 21 usado pelo IntelliJ: mvnw.cmd -o -Dtest=FluxoRagPoliticaEmpresaIntegrationTest test. Resultado: BUILD SUCCESS, 7 testes, sem falhas ou erros. Inclui retomada, ordem, separação de assuntos, idempotência, conflito de chave, isolamento de usuário/empresa e compatibilidade com perguntas sem conversaId, além dos cenários anteriores de recuperação/geração.

Os testes de serviço usam H2 com fontes e provedor simulados. Não são homologação com IA real. A migração V24 foi validada em PostgreSQL 17 com a imagem pgvector/pgvector:pg17, em contêiner isolado hestia-scrum536-validacao e banco descartável hestia_validacao. Dois testes adicionais verificaram base vazia, atualização V23 para V24 com registros antigos, sequência de IDs, preservação dos textos, chave estrangeira por autor/empresa, validação do Flyway e repetição sem novas migrações. Execução conjunta: 9 testes, 0 falhas, 0 erros, 0 ignorados, BUILD SUCCESS. O contêiner foi parado após a validação; Docker e IntelliJ permanecem abertos. Nenhum banco operacional foi alterado. O relatório de cobertura existente pode conter avisos de dados antigos; nenhuma porcentagem de cobertura é afirmada.

## Próximas etapas e limites

O frontend de conversas pertence à SCRUM-553. Interpretação de continuação pertence à SCRUM-546; limite/resumo de contexto à SCRUM-547; extensão da revogação ao contexto à SCRUM-551; homologação com IA real à SCRUM-556. Esta entrega organiza o histórico, sem afirmar que essas dependências foram implementadas.

Commit e publicação serão feitos pelo usuário no GitHub Desktop. A tarefa permanece Em desenvolvimento até revisão e aceite.
