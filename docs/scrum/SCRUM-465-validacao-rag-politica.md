# SCRUM-465 — Validação ponta a ponta do RAG com política da empresa

## Objetivo

Comprovar que a inovação RAG da HestIA consulta conteúdo autorizado da empresa, apresenta a fonte utilizada e preserva a interação no histórico sem inventar respostas quando não existe fonte válida.

A política usada nos testes é um dado controlado de validação. Ela não substitui uma política oficial da QSoft. No ambiente de demonstração, o grupo deve cadastrar o texto autorizado fornecido pela empresa na base de conhecimento, aprová-lo para reutilização e indexá-lo antes da apresentação.

## Fluxo validado

1. O funcionário autenticado envia uma pergunta pela tela **Assistente RAG**.
2. O BFF encaminha chave, pergunta e modo ao backend mantendo a identidade da sessão.
3. O backend obtém a empresa do usuário autenticado.
4. A busca vetorial considera apenas conteúdo da mesma empresa, aprovado, reutilizável, sem dados sensíveis e permitido para o perfil.
5. O conteúdo recuperado é revalidado antes de entrar no contexto da geração.
6. A consulta retorna o texto da política, seu identificador, título, versão e hash de integridade.
7. Quando o modo é **GERAR_NOVA**, o provedor recebe somente os trechos autorizados recuperados.
8. A interação é gravada no histórico do próprio usuário.
9. Repetir a mesma chave devolve a interação existente e não gera outra chamada.
10. Sem conteúdo autorizado, o estado é **SEM_CONTEUDO_AUTORIZADO** e nenhuma resposta é inventada.

## Evidências automatizadas

### Backend

Arquivo: `src/test/java/br/com/hestia/rag/service/FluxoRagPoliticaEmpresaIntegrationTest.java`

Cenários:

- consulta recupera a política autorizada e preserva fonte, versão e hash;
- geração recebe somente o trecho autorizado;
- histórico mantém a interação;
- chave idempotente evita processamento duplicado;
- ausência de política retorna estado explícito e não chama o gerador.

### Interface/BFF

Arquivo: `Front-End/hestia-prototipo-sprint1-atualizado/tests/integracoes-sprint.test.js`

O teste confirma que o BFF encaminha o contrato correto e devolve para a tela a resposta e todos os metadados da política sem substituí-los por dados demonstrativos.

## Roteiro para demonstração

Pré-requisitos:

- PostgreSQL com as migrations aplicadas;
- backend Java e frontend Express ativos;
- usuário de demonstração ativo e vinculado à empresa;
- conteúdo da política cadastrado como resposta reutilizável;
- status `APROVADA`, `reutilizavel=true` e `possui_dados_sensiveis=false`;
- perfil do usuário incluído em `resposta_perfis`;
- conteúdo indexado no pgvector;
- `OPENAI_API_KEY` e `hestia.rag.modelo` somente se for demonstrado o modo **Gerar nova**.

Passos:

1. Entrar na HestIA com o funcionário da empresa.
2. Abrir **Assistente RAG**.
3. Perguntar: “Posso enviar dados pessoais para uma ferramenta de IA?”
4. Verificar a resposta e abrir a identificação da fonte.
5. Confirmar que título e versão correspondem à política cadastrada.
6. Atualizar a página e confirmar que a interação reaparece no histórico.
7. Fazer uma pergunta sem fonte autorizada e confirmar a mensagem de ausência.
8. Opcionalmente retirar a autorização da fonte e confirmar que o histórico não exibe conteúdo que deixou de ser permitido.

## Resultado

O fluxo de software está integrado da interface ao histórico do backend e possui cobertura automatizada sem depender de chave externa. A qualidade semântica real do embedding e a chamada ao modelo externo continuam sendo testes de ambiente: exigem PostgreSQL/pgvector, credenciais do provedor e uma política oficialmente autorizada para a demonstração.
