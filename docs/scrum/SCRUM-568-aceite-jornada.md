# SCRUM-568 — aceite da jornada de uso e conformidade

Data: 12/11/2026. Base f71021d, branch finalizacao. Stack descartável em Docker/PostgreSQL 17 com fontes da branch atual e catálogo persistido; sem fallback de cursos fictícios. Contas e textos didáticos, sem incidente empresarial real.

## Resultado

21 verificações HTTP/API aprovadas. Evidência: evidencias/SCRUM-568-jornada.json. Reprodução: validar-jornada-568.cjs dentro do frontend da stack demo recém-criada e descartável. O roteiro altera dados; não executar em produção, banco de colegas ou ambiente com histórico operacional.

- Colaborador registra uso sem evidências: processamento concluído com REQUER_REVISAO e critérios NAO_VERIFICADO. Não recebe XP de conformidade e não pode solicitar retentativa de gestor.
- Gestor/ADMIN conferem evidências no ensaio: uso conforme recebe uma avaliação e uma recompensa. Retentativa não duplica os efeitos.
- Violação LGPD comprovada no cenário: uma não conformidade e um tratamento; a trilha usa o curso publicado do tema.
- Colaborador lê o conteúdo persistido e envia a prova: correção no servidor, nota 100 no caso de respostas corretas, participação concluída. Reenviar a mesma prova não aumenta o número de movimentos de XP.
- Retirada do curso preserva prova/histórico; permissão da fonte do RAG retirada não reaparece no resumo.

Os testes AvaliacaoAutomaticaIntegrationTest verificam separadamente falha técnica, recuperação, rollback, concorrência, estado de revisão e preservação da política. Falha técnica não é reprovação e retentativa técnica não refaz mérito já concluído. O tratamento não é declarado resolvido só por concluir capacitação: a resolução da ocorrência continua sujeita ao fluxo responsável.

Aceite de negócio local das transições/persistência/repetição: aprovado. Os testes não afirmam desempenho em cloud, responsividade visual final, eficácia educacional em empresa real ou análise automática do texto confidencial enviado a um provedor. A homologação publicada dos perfis permanece na 590.

## Compatibilidade após novos merges

A branch finalizacao incorporou develop 3760a40 por avanço direto, incluindo PRs 154 (correções), 153 (histórico de autorização/V29) e 149 (AI Literacy). Foram aprovados 70 testes de compatibilidade, zero falhas/erros/ignorados. O teste de histórico que dependia da resolução do relógio foi estabilizado com instantes controlados e reexecutado isoladamente; a regra de vigências não mudou. Backend reempacotado e stack descartável renovada: 21 verificações da jornada e 10 de relatórios passaram de novo. Novos relatórios com sufixo -v29 preservam a referência ao metadado de base antiga do roteiro e registram a base da imagem realmente executada. Evidências iniciais anteriores continuam identificadas como snapshots históricos.
