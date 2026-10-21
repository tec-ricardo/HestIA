# SCRUM-398 e SCRUM-399 — Fundamentos e histórico de políticas

## Objetivo no produto

Uma avaliação precisa responder: qual registro foi avaliado, qual política e revisão foram usadas, qual regra produziu o resultado e quais são os limites dessa conclusão. Atualizações futuras da política não podem reescrever a explicação de decisões anteriores.

O fluxo existente foi mantido: empresa autenticada → usuário e registro de uso → política da mesma empresa → critérios de conformidade → avaliação. Não há uma base paralela nem dados oficiais criados para viabilizar a funcionalidade.

## O que foi implementado

- Cada criação, atualização ou exclusão de política registra uma revisão imutável em `politica_uso_versoes`, com conteúdo, título, descrição, rótulo de versão, situação, empresa, autor autenticado, origem e data.
- A revisão numérica é interna e crescente. O campo de negócio `versao` continua aceitando rótulos como `1.0`. Mesmo que o administrador repita esse rótulo, o histórico diferencia as revisões.
- Cada nova avaliação referencia a revisão exata e preserva motivo, descrição e versão da regra, limitações e critérios. Consultas não recalculam a explicação.
- A regra atual é `CONFORMIDADE_CRITERIOS`, versão `1.0`. Ela usa os sete critérios já existentes. Autorização da ferramenta, vigência da política, presença de finalidade e registro são apuradas pelo sistema; o solicitante não pode sobrepor esses resultados.
- A regra é um checklist. Ela não interpreta semanticamente o texto da política e não constitui uma avaliação jurídica ou uma análise RAG.
- Excluir uma política a retira das consultas operacionais e impede novas avaliações com ela. Revisões e avaliações anteriores continuam disponíveis. Repetir a exclusão não gera outra revisão.
- Atualização, exclusão e avaliação bloqueiam a linha da política durante a transação, para que uma avaliação não se vincule a uma revisão diferente daquela utilizada. A política também possui controle de versão para concorrência.
- Consultas e alterações validam a empresa da sessão. Não é permitido transferir políticas entre empresas.

## Endpoints e permissões

Todas as chamadas exigem a sessão já usada pelo projeto, no cabeçalho `Authorization: Bearer <token>`.

| Método e caminho | Resultado | Perfil |
| --- | --- | --- |
| `POST /politicas` | Cria política e revisão 1 | ADMIN |
| `PUT /politicas/{id}` | Atualiza e registra uma nova revisão | ADMIN |
| `DELETE /politicas/{id}` | Exclui logicamente e registra a retirada | ADMIN |
| `GET /politicas/{id}/versoes` | Histórico, da revisão mais recente para a mais antiga | ADMIN |
| `GET /politicas/{id}/versoes/{revisao}` | Conteúdo e metadados de uma revisão | ADMIN |
| `POST /avaliacoes-conformidade` | Avalia e preserva os fundamentos | ADMIN ou GESTOR |
| `GET /avaliacoes-conformidade/{id}` | Resultado, critérios e fundamentos preservados | ADMIN ou GESTOR |
| `GET /avaliacoes-conformidade/{id}/politica` | Política histórica efetivamente usada pela avaliação | ADMIN ou GESTOR |

O último endpoint permite que o gestor consulte o fundamento da decisão sem ganhar acesso à administração das políticas.

A resposta da avaliação mantém os campos anteriores e acrescenta:
`politicaVersaoId`, `politicaRevisao`, `politicaVersao`, `regraCodigo`, `regraVersao` e `fundamentosPreservados`.

## Migração e dados anteriores

A migração Flyway V16 cria as estruturas e preserva o estado conhecido das políticas existentes, com origem `MIGRACAO` e autor nulo. Ela roda automaticamente na inicialização normal da aplicação com o PostgreSQL configurado.

Não é possível deduzir qual conteúdo uma avaliação antiga utilizou antes de haver versionamento. Por isso, avaliações anteriores continuam com seu resultado e seus critérios originais, mas retornam `fundamentosPreservados=false`, identificação histórica nula e um aviso em `limitacoes`. Motivo e regra preservados ficam nulos. Consultar a política histórica dessas avaliações retorna 404.

Não se deve preencher esse vínculo retroativamente com a política atual. Também não se deve modificar migrações antigas já aplicadas.

## Como conferir manualmente

Use o PostgreSQL local de desenvolvimento e uma conta ADMIN autorizada da sua empresa. Os testes automatizados usam dados próprios e não cadastram funcionários fictícios na carga operacional.

1. Inicie o PostgreSQL e a aplicação Java com a configuração habitual. Verifique no log que a V16 foi aplicada.
2. No Postman, use a autenticação do projeto. Configure `empresaId` com o ID real da empresa da sessão.
3. Crie uma política com `POST /politicas`, preenchendo `titulo`, `descricao`, `conteudo`, `versao`, `ativa` e `empresaId`. Anote o ID retornado.
4. Consulte `GET /politicas/{id}/versoes`. Deve existir uma revisão com origem `CRIACAO`.
5. Utilize um registro de uso real, da mesma empresa e ainda não avaliado. Envie `POST /avaliacoes-conformidade`:

```json
{
  "registroUsoId": {{registroUsoId}},
  "politicaUsoId": {{politicaUsoId}},
  "criteriosConfirmados": ["DADOS_SENSIVEIS", "VALIDACAO_HUMANA", "LGPD"],
  "justificativa": "Revisão humana realizada sobre o uso registrado."
}
```

6. Anote o ID da avaliação, o resultado, a revisão e os fundamentos. Confirme que `fundamentosPreservados` é verdadeiro.
7. Atualize o conteúdo da política com `PUT /politicas/{id}`, enviando o DTO completo. A revisão deve aumentar, inclusive se o rótulo `versao` permanecer igual.
8. Consulte novamente a avaliação e `GET /avaliacoes-conformidade/{id}/politica`. Os fundamentos e o conteúdo devem continuar sendo os originais. Uma nova avaliação de outro registro usa a nova revisão.
9. Exclua a política. Ela deve desaparecer da listagem operacional, mas a consulta histórica da avaliação deve continuar funcionando.
10. Tente avaliar outro registro com a política excluída. A aplicação deve recusar. Uma sessão de outra empresa também não pode ler a avaliação ou o histórico.

Não confirme critérios humanos sem efetivamente verificá-los. Ter política ativa e ferramenta aprovada, por si só, não garante um resultado CONFORME.

## Testes e manutenção

- Testes de serviço cobrem versionamento, autoria, isolamento por empresa, exclusão lógica, recusa de critérios automáticos indevidos e dados legados.
- `PoliticaHistoricoIntegrationTest` usa os serviços e a persistência reais. Verifica a avaliação antes e depois de atualizar/excluir a política, limpa o cache de entidades e consulta novamente os dados gravados.
- `HistoricoPoliticasMigrationTest` migra uma base com dados na V15 para V16 e verifica que o histórico legado não é inventado nem duplicado.
- A integração roda em H2 normalmente e em PostgreSQL quando `HESTIA_PG_TEST_URL`, `HESTIA_PG_TEST_USER` e `HESTIA_PG_TEST_PASSWORD` estiverem definidos. O teste PostgreSQL cria e remove um schema exclusivo em uma base de testes.

Se a lógica de classificação mudar futuramente, incremente `REGRA_VERSAO` e preserve os fundamentos já gravados. Não recalcule decisões antigas durante uma consulta.

## Relação com o restante da entrega

Esta entrega implementa persistência, regras e consultas da API. O cadastro/listagem de políticas do frontend mantém os contratos existentes. A tela do protótipo ainda não consome os novos endpoints de histórico e avaliação; essa apresentação visual continua sendo uma integração de frontend.

As revisões de políticas não alimentam automaticamente a base vetorial nem substituem a curadoria de conteúdo do RAG. Histórico de políticas e histórico de respostas reutilizáveis possuem finalidades distintas.

## Validação desta entrega — 21/10/2026

- Suíte Java completa: 149 testes encontrados, 129 executados sem falhas e 20 condicionais não executados nessa rodada.
- Rodada PostgreSQL 17 no Docker: 22 testes executados, zero falhas, zero erros e zero ignorados, incluindo o fluxo de histórico e as verificações PostgreSQL já existentes.
- Migração V16 validada em H2 e PostgreSQL; atualização de uma base com dados na V15 validada pelo teste de migração.
- Banco PostgreSQL usado nas verificações: `hestia_verificacao_20261021`, com schemas temporários exclusivos. A carga operacional C01 não foi alterada por esta implementação.
- Branch: `features/SCRUM-398-399-historico-politicas`, criada de `origin/develop` no commit `759b55d`.
- Alterações deixadas sem commit e sem push para revisão e PR pelo responsável.
