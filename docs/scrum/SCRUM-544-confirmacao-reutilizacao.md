# SCRUM-544 — Confirmação de reutilização sem duplicar créditos

Implementação: 12/11/2026. Responsável: Luccas Toffano.
Branch: features/SCRUM-544-confirmacao-reutilizacao.
Base: develop, commit 55b5cc0. Produto: PO-98, PO-99, PO-127 e PO-139.

## Comportamento entregue

- CONSULTAR recupera sugestões/fontes e grava a consulta, sem contar reutilização, tokens evitados ou recompensa.
- GERAR_NOVA preserva a geração com fontes e seu registro de uso, sem premiar reutilização.
- Somente REUTILIZAR, enviado por ação explícita do usuário, confirma uma resposta autorizada e registra os efeitos existentes de economia e gamificação.
- A interação persistida funciona como comprovante da operação. Mesma chave, empresa e usuário retornam o resultado existente. Trocar pergunta, modo, conversa informada ou resposta selecionada com a mesma chave resulta em conflito 409.
- Bloqueio do usuário serializa reenvios concorrentes; bloqueio da resposta serializa incrementos de usuários distintos. Confirmação, histórico e efeitos compartilham a transação. Não foi criada uma segunda lógica de créditos.
- Empresa, perfil, aprovação, sensibilidade e validade continuam sendo verificados. Fonte retirada/alterada não é recuperada por reenvio ou histórico.

## Contratos e interface

- POST /api/rag/perguntas mantém o contrato de chave, pergunta, modo, respostaId e conversaId opcional.
- PATCH /api/respostas-reutilizaveis/{id}/reutilizar agora exige o cabeçalho Idempotency-Key (não vazio, até 100 caracteres) e usa a mesma transação de confirmação do RAG. A ausência é erro 400. Clientes externos dessa rota precisam enviar o cabeçalho; o BFF e a tela de utilizações foram ajustados.
- Assistente, banco da sessão e sugestões locais passam pelo servidor antes de confirmar a reutilização e copiar. Não registram sucesso apenas por copiar texto local.
- A chave de confirmação do assistente deriva da interação consultada e da resposta selecionada, permanecendo igual ao retomar a mesma consulta pelo histórico. Consultas independentes podem originar confirmações independentes.
- Cliques em andamento são bloqueados. Uma falha de rede mantém a chave para nova tentativa; falha de copiar depois da confirmação informa o problema sem desfazer o resultado no servidor.
- Foram removidos o crédito automático na consulta e a promessa fixa de +15 XP na tela. O Passe continua sendo a fonte dos créditos efetivamente concedidos.

## Fonte, semelhança e limites

O resultado informa ID, título, trecho, revisão quando cadastrada, hash e data de cadastro da fonte. A natureza distingue consulta, resposta gerada e reutilização confirmada.

Na busca vetorial, a similaridade é o valor de cosseno calculado pelo PostgreSQL/pgvector; é preservada junto às fontes do histórico. O limiar padrão existente de 0,70 foi mantido. Esse número não é probabilidade de acerto nem uma validação semântica do provedor. Na recuperação textual ou na confirmação direta, a similaridade é ausente e a interface informa que não foi medida. A confiança permanece NAO_CLASSIFICADA, sem percentuais fabricados.

Não há nova migration. A estrutura de interações, suas chaves únicas e a revalidação das fontes existentes foram reaproveitadas. Registros e recompensas históricos não são apagados nem recalculados retroativamente.

## Verificação

Execução final em 12/11/2026: Java BUILD SUCCESS, 322 testes, 320 aprovados, zero falhas/erros e 2 não executados (embedding externo e alert-service ponta a ponta). Frontend: 31 testes aprovados, zero falhas. Os seis blocos JavaScript das telas alteradas foram verificados após renderizar os valores EJS. Verificação de espaços do Git sem erros.

Cenários novos: consulta sem recompensa; confirmação com reenvio; conflito de resposta com mesma chave; retirada de fonte entre consulta e escolha; isolamento entre empresas; seis requisições concorrentes de dois usuários (duas confirmações, uma recompensa para cada usuário); exigência de chave no endpoint legado; similaridade calculada pelo pgvector; falha de conexão, duplo clique, fonte indisponível, falha de copiar e retomada da consulta no frontend.

Os testes usam dados controlados, embeddings/geração simulados onde necessário e PostgreSQL descartável para concorrência e pgvector. Não são prova da qualidade semântica do provedor nem da política de demonstração carregada. Esses pontos continuam nas SCRUM-543, 545 e 556.

## Conferência antes do merge

1. Consultar uma resposta disponível: deve aparecer como consulta, sem aumentar contadores ou créditos.
2. Confirmar Reutilizar resposta: deve indicar confirmação no servidor e registrar uma reutilização.
3. Repetir a mesma operação/chave ou retomar a mesma consulta: não deve acrescentar outro crédito.
4. Retirar a fonte e tentar reutilizar: deve informar indisponibilidade.
5. Fazer commit somente dos arquivos desta tarefa e abrir PR para develop. O arquivo não versionado da SCRUM-591 já existia e não faz parte desta alteração.

Implementação e testes locais não substituem revisão do PR, CI e aceite do fluxo integrado.
