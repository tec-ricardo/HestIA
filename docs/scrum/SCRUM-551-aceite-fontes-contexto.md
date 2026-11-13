# SCRUM-551 — aceite das fontes no contexto e resumo

Data: 12/11/2026. Base f71021d, branch finalizacao. Código funcional na integração de correções; novos testes/evidências preparados para commit.

A seleção do contexto usa somente interações revalidadas por InteracaoRagService.ler. Fonte retirada, bloqueada, cujo conteúdo foi alterado ou sem permissão atual produz resposta histórica indisponível. Uma quebra de elegibilidade impede trazer o assunto anterior por resumo. Não se cria um resumo persistido que escape das verificações de fonte/empresa/perfil.

## Cenários executados

O teste fonteInelegivelNaoEntraNoResumoNemNaProximaGeracao de AprendizagemSprintIntegrationTest executa quatro casos: RETIRADA, BLOQUEADA, CONTEUDO_ALTERADO e SEM_PERFIL. Cada caso cria 16 mensagens autorizadas, comprova resumo não vazio, torna a fonte inelegível e confere zero turnos completos/truncados/resumidos. O histórico fica sem texto/fontes e uma pergunta de continuação recebe esclarecimento com contexto vazio. O mock de geração não recebe nenhuma chamada nesses casos.

Os testes anteriores preservam isolamento por autor/empresa, orçamento, Unicode/JSON e bloqueio de fonte antes de geração. O ensaio HTTP da 568 também verifica retirada de permissão sobre conversa longa. Foram 50 testes direcionados aprovados no conjunto técnico, zero falhas/erros/ignorados; as quatro linhas da matriz pertencem à 551. Evidência: evidencias/FINALIZACAO-LUCCAS.json e código de teste versionado. Não houve chamada real OpenAI nesta etapa. A homologação contextual real/publicada é escopo da 556.

Aceite técnico local: aprovado. Não equivale a implantação em cloud nem substitui confirmação de merge/CI da nova branch.

## Compatibilidade após novos merges

A branch finalizacao incorporou develop 3760a40 por avanço direto, incluindo PRs 154 (correções), 153 (histórico de autorização/V29) e 149 (AI Literacy). Foram aprovados 70 testes de compatibilidade, zero falhas/erros/ignorados. O teste de histórico que dependia da resolução do relógio foi estabilizado com instantes controlados e reexecutado isoladamente; a regra de vigências não mudou. Backend reempacotado e stack descartável renovada: 21 verificações da jornada e 10 de relatórios passaram de novo. Novos relatórios com sufixo -v29 preservam a referência ao metadado de base antiga do roteiro e registram a base da imagem realmente executada. Evidências iniciais anteriores continuam identificadas como snapshots históricos.
