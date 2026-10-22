# SCRUM-389 — Integração da gestão de ocorrências

Data: 22/10/2026. Branch: `features/SCRUM-389-integracao-ocorrencias`.

## Entrega funcional

- A listagem consulta `GET /tratamentos-nao-conformidade` com sessão Bearer, filtro por status e paginação.
- O detalhe consulta `GET /tratamentos-nao-conformidade/{id}` e mostra o uso de IA, a avaliação, a regra, as limitações e a revisão histórica da política.
- A tela permite escolher um usuário ativo retornado por `GET /usuarios`, informar a ação corretiva e alterar o estado entre `ABERTA`, `EM_TRATAMENTO` e `RESOLVIDA`.
- O salvamento usa `PUT /tratamentos-nao-conformidade/{id}` e envia a `versaoRegistro` exibida pelo backend. Após sucesso, a página é recarregada com os dados persistidos.
- A interface impede enviar tratamento ou resolução sem responsável e ação. O backend continua sendo a autoridade final para escopo, usuário ativo e regras de negócio.
- Respostas 400, 401, 403, 404, 409 e indisponibilidade são apresentadas sem inventar dados. Em conflito 409, o detalhe atual é recarregado para evitar sobrescrever a alteração de outra pessoa.
- Acesso continua restrito aos perfis administrador e gestor; empresa e escopo vêm da sessão autenticada.

## Validação automatizada

- Frontend: 15 testes aprovados, nenhuma falha.
- O teste da SCRUM-389 cobre login, token Bearer, listagem, filtro, lista vazia, detalhe, escape de HTML, seleção de usuário ativo, atualização, confirmação de persistência, validação de formulário, conflito de versão e ausência de exemplos locais.
- Backend Java: 264 testes executados, 239 aprovados e 25 condicionais ignorados, sem falhas ou erros.
- Os 15 testes de controller e integração de tratamento passaram, incluindo criação, consulta, alteração, conflito, escopo e regras de status.
- As 23 migrações foram validadas e aplicadas nos bancos em memória usados pelos testes.
- `node --check ocorrencias-api.js` e `git diff --check` foram executados sem erro.

## Validação anterior com PostgreSQL

O fluxo de consulta já foi testado com frontend Node real, backend Java real e PostgreSQL 17 com PGVector em banco descartável migrado até V23. O teste criou empresa, departamento e usuário somente no banco descartável, usou a API para cadastrar ferramenta, política, registro de uso, avaliação não conforme e tratamento, e confirmou lista e detalhe pela interface autenticada.

Uma cópia integral do banco principal, originalmente na V15, também recebeu V16–V23 com sucesso. Os dados existentes permaneceram com 1 empresa, 11 departamentos, 7 ferramentas e 311.556 linhas em `base_sintetica.import_linhas`; o hash determinístico permaneceu `a511b1c309bedd7eeb19a80846b13080`.

O banco principal não foi migrado ou modificado. Como ele não contém usuários operacionais, políticas, registros de uso ou avaliações, a demonstração do tratamento depende de uma carga de demonstração identificada ou de dados autorizados. Isso não altera o funcionamento da integração entregue.

## Limite do escopo atual

O backend da `develop` registra a atualização na auditoria geral, mas não oferece um endpoint de histórico detalhado das mudanças do tratamento. A interface integra todas as operações disponíveis para a SCRUM-389 sem simular um histórico inexistente.
