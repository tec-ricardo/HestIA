# SCRUM-372 e SCRUM-373 — Revisão, histórico e isolamento

Referência: [PO-90 / US54](https://luccastoffano.atlassian.net/browse/PO-90).

## Resultado no produto
O gestor pode confirmar ou revisar a avaliação de uma ocorrência mediante justificativa. A avaliação automática, seu checklist, motivo, política/revisão aplicada e registro de uso original permanecem preservados. O tratamento mantém resultadoOriginal e resultadoVigente separados.

Uma revisão é uma reavaliação da ocorrência original com evidências, não uma forma de afirmar que uma ação corretiva foi concluída. Para registrar a conclusão da correção, altere o status do tratamento para RESOLVIDA pela API da SCRUM-371. Revisar ou confirmar não muda automaticamente responsável, ação ou status do tratamento.

## Endpoints
Todos exigem Authorization: Bearer com a sessão já usada pelo projeto.

### Consultar o estado e a versão atual
GET /tratamentos-nao-conformidade/{id}

O resumo em tratamento agora também informa:
- resultadoOriginal: decisão preservada da avaliação original.
- resultadoVigente: decisão atualmente adotada, inicialmente igual à original.
- revisoesAvaliacao: quantidade de confirmações e revisões registradas.
- versaoRegistro: versão que deve ser enviada para a próxima alteração.

### Confirmar a decisão vigente
POST /tratamentos-nao-conformidade/{id}/revisoes

```json
{
  "decisao": "CONFIRMAR",
  "justificativa": "Conferi as evidências e mantenho a decisão vigente.",
  "versaoRegistro": 0
}
```

Não envie resultado nesta operação. Confirmar mantém a decisão vigente, inclusive quando ela já resulta de uma revisão humana.

### Revisar a decisão
POST /tratamentos-nao-conformidade/{id}/revisoes

```json
{
  "decisao": "REVISAR",
  "resultado": "CONFORME",
  "justificativa": "A evidência de autorização comprova que a classificação original precisa ser revista.",
  "versaoRegistro": 1
}
```

O resultado deve ser diferente da decisão vigente. Valores possíveis: CONFORME, NAO_CONFORME e REQUER_REVISAO. A justificativa é obrigatória, não pode ser composta apenas de espaços e tem limite de 1000 caracteres. Autor e data são determinados pelo servidor, nunca por IDs enviados no corpo.

A resposta 200 contém o detalhe atualizado e a nova versaoRegistro. Recarregue o GET antes de cada alteração. Uma versão antiga retorna 409, sem sobrescrever a decisão nem acrescentar evento. Confirmações também avançam a versão.

### Consultar o histórico
GET /tratamentos-nao-conformidade/{id}/historico?pagina=0&tamanho=20

Resposta paginada com itens, pagina, tamanho, totalItens e totalPaginas. Página não negativa, tamanho entre 1 e 100. Ordenação da maior para a menor sequência.

Cada evento contém tipo, sequência/versão do tratamento, autorId, registradoEm, status do tratamento, responsável, ação corretiva, resultado anterior, resultado vigente e justificativa quando aplicável.

Tipos: ABERTURA, ATUALIZACAO, CONFIRMACAO, REVISAO e ESTADO_MIGRADO. Uma atualização de responsável/ação/status registra uma fotografia após a mudança; repetir um PUT sem mudança não duplica eventos. O estado anterior desses campos pode ser consultado no evento precedente.

Não existem endpoints para editar ou excluir eventos. A entidade de histórico é somente de inserção na aplicação e marcada como imutável no ORM; isso não equivale a proteção contra um administrador com acesso direto ao banco. Usuários operacionais só acessam as operações autorizadas da API.

## Permissões e isolamento — SCRUM-373
- ADMIN: tratamentos e histórico da própria empresa.
- GESTOR: tratamentos e histórico somente do departamento autorizado na própria empresa.
- FUNCIONARIO: não abre, altera, revisa ou consulta esses tratamentos, mesmo quando foi designado como responsável.
- Contas inativas, gestor sem departamento válido e contexto de empresa incompatível são bloqueados.
- Identificadores de outra empresa/departamento retornam 404; o estado não é alterado.
- Solicitações sem sessão válida retornam 401; perfil não autorizado retorna 403.
- Autor é lido do usuário autenticado e revalidado no banco. Campos extras como autorId e empresaId no corpo não substituem esse contexto.

A criação, as mudanças de tratamento e as revisões usam a mesma transação de seus eventos. Bloqueio da linha do tratamento, versão esperada e unicidade de tratamento/sequência evitam sobrescrita e duplicação de revisões obsoletas. O histórico verifica o escopo do tratamento antes de consultar seus eventos.

## Integração com os indicadores
Uso Responsável passa a considerar a decisão vigente do tratamento quando existir; sem tratamento, usa a avaliação original. Assim, uma revisão autorizada entra na próxima consulta dos indicadores implementados nas SCRUM-427 a 430.

A fórmula e a fonte identificam que decisões vigentes e revisões são usadas. A revisão não cria fundamentos que faltam: uma avaliação legada continua sem cobertura histórica completa. As regras de confiança e amostra não são relaxadas. O resultado original continua disponível na avaliação e no histórico.

O indicador representa o estado da decisão na data da consulta. As ocorrências e os dados originais usados em outros fluxos históricos não são reescritos.

## Banco — V18
A migração adiciona resultado vigente e contador de revisões ao tratamento, cria historico_tratamentos e preserva o estado conhecido dos tratamentos já existentes.
- Para tratamentos antigos: um evento ESTADO_MIGRADO, com a sequência atual e o estado conhecido.
- O autor desse evento é nulo e a data é a data da migração. A data e o autor reais da abertura continuam nos campos originais do tratamento.
- Não reconstrói mudanças anteriores nem inventa autoria ou revisão.
- A avaliação original permanece intocada.
- A migração roda pelo Flyway ao iniciar a nova versão. Não execute INSERTs manuais para duplicá-la.
- A entrega foi validada na base de verificação; não aplica a migração automaticamente na base operacional durante os testes.

## Como conferir
1. Faça o checkout desta branch, inicie o backend e deixe o Flyway aplicar a V18 no seu ambiente de desenvolvimento.
2. Entre com uma conta ADMIN/GESTOR autorizada e use um tratamento do seu escopo.
3. Consulte seu detalhe e copie versaoRegistro.
4. Confirme a avaliação com justificativa; consulte o histórico e confira autor, data e decisão.
5. Recarregue a versão e revise a avaliação com outro resultado e justificativa.
6. Confira que resultadoOriginal e os fundamentos originais não mudaram; resultadoVigente deve refletir a revisão.
7. Confira que responsável, ação e status não foram alterados automaticamente.
8. Reenvie a versão antiga: deve retornar 409 e manter a quantidade de eventos.
9. Tente usar outro departamento/empresa: deve retornar 404, sem expor o histórico.
10. Consulte Uso Responsável no mesmo período; a nota descritiva considera a decisão vigente, mantendo os avisos de confiança.

Esta entrega implementa a API e a persistência; não acrescenta uma tela de revisão. As rotas de tratamento anteriores continuam disponíveis.

## Validação
Os testes cobrem preservação dos originais, sequência/autoria/data e fotografias do histórico, confirmação/revisão, justificativas e decisões inválidas, rejeição de versão antiga, atualização sem mudança, paginação, recálculo de Uso Responsável, autorização HTTP e isolamento entre empresas/departamentos/perfis, além da atualização V17 para V18.

### Evidências em 21/10/2026
- Suíte completa com PostgreSQL configurado: 217 testes encontrados, 215 aprovados, 2 condicionais ignorados, nenhuma falha ou erro.
- Primeira rodada focada: 23 testes aprovados. Prova de migração V17 para V18 aprovada após manter a conexão do H2 ativa durante o teste de upgrade.
- Frontend existente: 4 testes aprovados, sem falhas.
- git diff --check sem problemas.
- Migrações e integração PostgreSQL verificadas em schemas temporários da base de testes. A base operacional não foi alterada.
- Branch features/SCRUM-372-373-revisao-historico-isolamento criada da origin/develop 07f5929. Alterações deixadas sem commit, push ou PR automático.

### Consolidação posterior no mesmo dia
As 372/373 ainda estavam sem commit quando foram solicitadas 503/529/534. As cinco foram preservadas e reunidas na branch features/SCRUM-372-373-503-529-534-validacoes, na pasta HestIA habitual, para integrar V18/V19 em um único PR. A suíte conjunta aprovou 234 testes Java e 4 do frontend, sem falhas; 2 testes Java condicionais foram ignorados. Consulte também SCRUM-503-529-534-validacoes.md.
