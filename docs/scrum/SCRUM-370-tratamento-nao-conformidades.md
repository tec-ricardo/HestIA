# SCRUM-370 — Dados e regras do tratamento de não conformidades

## Objetivo e referência

Parte da US54 / PO-90: permitir ao gestor acompanhar uma ocorrência e seus fundamentos sem apagar a avaliação que originou o problema.

Esta task entrega a abertura e consulta do tratamento pela API. Ela usa os registros de uso, usuários, departamentos, empresas, avaliações e revisões de políticas existentes no projeto. Não cria uma ocorrência a partir de texto livre nem inventa funcionários ou fatos na base operacional.

## Comportamento

- Só uma avaliação com resultado `NAO_CONFORME` pode originar tratamento. `CONFORME` e `REQUER_REVISAO` não são tratados como não conformidade confirmada.
- A abertura é explícita; avaliar um registro não cria automaticamente um tratamento.
- Há no máximo um tratamento por avaliação, garantido no serviço e pela restrição única do banco. O bloqueio da avaliação serializa aberturas concorrentes.
- O tratamento começa `ABERTA`, com autor e data da abertura. Responsável e ação corretiva ficam inicialmente nulos.
- O status do tratamento é separado do resultado da avaliação. Abrir o tratamento não altera critérios, justificativa, resultado ou fundamentos da avaliação original.
- Empresa e departamento da ocorrência são registrados na abertura e não podem ser escolhidos pelo corpo da requisição.
- ADMIN acessa a empresa autenticada; GESTOR acessa o departamento ao qual está vinculado dentro dessa empresa. Funcionários, usuários inativos e gestores sem departamento válido não podem operar o recurso.
- Consultas de IDs fora do escopo retornam 404, sem revelar o conteúdo.
- O detalhe traz o registro de uso, a avaliação original e a revisão da política efetivamente usada. Atualizar ou excluir uma política não substitui esse fundamento.
- Para avaliações legadas sem versão preservada, `politicaAplicada` é nula e a avaliação mantém o aviso `fundamentosPreservados=false`. Não se usa a política atual como se fosse a original.
- A entidade prepara os campos de responsável, ação corretiva e os estados `EM_TRATAMENTO` e `RESOLVIDA` para a continuidade da história. Esta task não oferece alteração desses campos.
- O controle de acesso e a auditoria HTTP existentes no projeto também se aplicam às novas rotas.

## API

Use a autenticação já existente: `Authorization: Bearer <token>`.

| Método e caminho | Função |
| --- | --- |
| `POST /tratamentos-nao-conformidade` | Abre um tratamento e retorna 201 e o cabeçalho Location |
| `GET /tratamentos-nao-conformidade/{id}` | Consulta ocorrência, avaliação e política histórica |
| `GET /tratamentos-nao-conformidade` | Lista somente os tratamentos do escopo autorizado |

Corpo da abertura:

```json
{
  "avaliacaoId": 123
}
```

O número deve ser o ID real de uma avaliação não conforme no escopo da sessão. Autor, empresa, departamento, estado e data são definidos pelo servidor.

A listagem aceita `status` opcional, `pagina` a partir de zero e `tamanho` entre 1 e 100 (padrão 20). Ordena pela abertura mais recente, com ID como desempate. Retorna `itens`, `pagina`, `tamanho`, `totalItens` e `totalPaginas`.

Exemplo: `GET /tratamentos-nao-conformidade?status=ABERTA&pagina=0&tamanho=20`.

O detalhe contém:
- `tratamento`: identificação, escopo, status, autor/data, resultado original e campos preparados para a 371;
- `ocorrencia`: dados do registro de uso existente;
- `avaliacao`: critérios, motivo, justificativa e fundamentos preservados;
- `politicaAplicada`: revisão histórica da política ou nulo para legado.

Respostas relevantes: 400 para corpo/paginação inválidos; 401 sem sessão; 403 para perfil não autorizado; 404 para ID inexistente/fora do escopo; 409 para avaliação inelegível ou tratamento duplicado.

## Banco

A migração Flyway V17 cria `tratamentos_nao_conformidade`, com vínculos reais, restrição única por avaliação, verificação dos estados e índices para empresa/departamento/status. Ela não importa nem modifica dados do staging.

A migração roda na inicialização normal do backend. Antes disso, esta alteração existe somente na branch. Não há necessidade de reimportar a base CSV/SQLite ou recriar o volume Docker.

A V17 foi numerada após conferir a `origin/develop`, que já contém a V16 das SCRUM-398/399. Não altere migrações antigas já aplicadas.

## Como conferir

1. Inicie o PostgreSQL e o backend com a configuração habitual, verificando a aplicação da V17.
2. Autentique uma conta ADMIN ou GESTOR autorizada. A carga C01 ainda não fornece credenciais de funcionários; não crie acessos inventados para simular operação real.
3. Use uma avaliação `NAO_CONFORME` já existente ou gerada pelo fluxo real da API e abra o tratamento pelo POST.
4. Consulte o endereço devolvido em Location. Confira a ocorrência, o resultado original, o motivo e a revisão da política.
5. Repita o POST com a mesma avaliação. Deve retornar 409.
6. Liste com `status=ABERTA`. O tratamento deve aparecer somente no escopo autorizado.
7. Após atualizar a política, consulte novamente o tratamento. A revisão aplicada deve continuar sendo a original.

Os testes automatizados criam suas próprias entidades em ambiente isolado e não dependem de funcionários inventados na carga operacional. Cobrem abertura, duplicidade, resultados inelegíveis, legado, persistência, histórico de política, escopo por empresa/departamento, perfil e validação HTTP.

## Limites desta entrega

A 371 implementará alterações de responsável, ação e status; a 372 implementará revisão justificada e histórico do tratamento; a 373 ampliará a validação de permissões e isolamento. A restrição básica de acesso já foi incluída para não expor os novos dados.

A interface visual ainda precisa consumir estes endpoints. Resolver um tratamento não deve futuramente apagar ou reescrever a ocorrência original.

## Validação em 21/10/2026

- Suíte Java: 167 testes encontrados, 147 executados sem falhas, 20 condicionais não executados nessa rodada.
- PostgreSQL 17 no Docker: 26 testes executados, zero erros, zero falhas e zero ignorados; inclui os oito testes de integração do tratamento e as verificações PostgreSQL existentes.
- Migração V17 aplicada e mapeamento Hibernate validado no banco isolado hestia_verificacao_20261021, em schema exclusivo removido ao final.
- Branch features/SCRUM-370-tratamento-nao-conformidades, criada de origin/develop no commit f332166, após o merge das SCRUM-398/399.
- A base operacional C01 e seus dados não foram alterados. Código e documentação deixados sem commit/push para revisão do responsável.
