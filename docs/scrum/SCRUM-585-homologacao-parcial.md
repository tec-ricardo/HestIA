# SCRUM-585 — conferência parcial de indicadores e relatórios

Data: 12/11/2026. 10 verificações HTTP aprovadas no ambiente local, após a massa da 568. Artefatos: validar-relatorios-585.cjs e evidencias/SCRUM-585-parcial.json.

Conferidos: oito fatores explícitos, natureza operacional, FinOps ausente com nota nula, global nulo quando faltam fatores, mesmo estado/amostra/valor numérico no JSON e CSV, ausência preservada no CSV e exportação gerencial negada ao colaborador. Comparação numérica aceita 100 e 100.00 como o mesmo valor, mantendo vazio diferente de zero.

Na massa controlada, Uso Responsável = 33,33 (C=1, N=2, R=1, sendo um N da carga demo e os demais resultados do ensaio) e Competências = 100 (uma pessoa com resultado determinável). Ambas as notas permanecem PARCIAL; não há 30 observações/pessoas nem cobertura para global. Os valores conferidos são da massa didática, não medições reais da QSoft.

O período da consulta cobre 03–24/11, sem previsão de resultados futuros: os dados observados são somente os existentes no instante do ensaio. O backend operacional declara LOCAL_OPERACIONAL_SEM_CONVERSAO_ENTRE_FONTES. Timestamps do container podem estar em UTC; não converter datas silenciosamente nem chamar esse ensaio de encerramento da sprint.

Aceite final da 585 continua pendente: branches analíticas como 578, 579/580 e 582 ainda não integradas na develop conferida, comparação final de todos os fatores/ranking/risco e release aceita. Não foram reimplementadas as calculadoras do Knorre para fabricar completude. Os cenários das definições já têm testes separados e relatórios, sem substituir o aceite da integração.

## Compatibilidade após novos merges

A branch finalizacao incorporou develop 3760a40 por avanço direto, incluindo PRs 154 (correções), 153 (histórico de autorização/V29) e 149 (AI Literacy). Foram aprovados 70 testes de compatibilidade, zero falhas/erros/ignorados. O teste de histórico que dependia da resolução do relógio foi estabilizado com instantes controlados e reexecutado isoladamente; a regra de vigências não mudou. Backend reempacotado e stack descartável renovada: 21 verificações da jornada e 10 de relatórios passaram de novo. Novos relatórios com sufixo -v29 preservam a referência ao metadado de base antiga do roteiro e registram a base da imagem realmente executada. Evidências iniciais anteriores continuam identificadas como snapshots históricos.

## Atualização com ranking integrado — base 77ce704

PR 130/SCRUM-579 incorporado à branch finalizacao por avanço direto, preservando as alterações locais. Ranking e classificador: 11 testes direcionados aprovados, zero falhas/erros/ignorados, incluindo bloqueio de funcionário, gestor sem departamento e período invertido antes de consultar dados; empate, ausência de score e ausência de posição comparativa do gestor preservados. Teste de vigência com relógio determinístico incluído nesta execução.

Esta verificação é unitária: não substitui o aceite HTTP/UI do ranking na release publicada. Os ensaios HTTP anteriores continuam identificados pelas bases originais. A pendência de integração da 579 foi resolvida; a conclusão integral da 585 ainda depende dos contratos 578/580/582 e da conferência integrada dos oito fatores. Evidência: evidencias/SCRUM-585-ranking-unitario.json.

## Homologação HTTP após integração da 580

Base 9f06150, relatório HOMOLOGACOES-LOCAIS-13-11.md e evidências -9f06150/ranking-http. Investigação/ranking testados contra o mesmo banco e permissões, sem fabricar classificação ou score com dados insuficientes. Aceite final ainda parcial por 578/582.

## Integração final local em 13/11

578/582/576/572/594 incorporadas da develop 85bb4b5; Segurança/571 incorporada por squash na mesma branch, sem commit automático. Verify completo: 395 aprovados e 2 externos ignorados. Frontend: 52 aprovados. Relatórios/ranking/investigação/perfis: 51 verificações HTTP aprovadas. Os bloqueios de integração anteriores foram resolvidos; faltam aceite da release após commit e etapas publicadas/RAG real. Relatório INTEGRACAO-FINAL-PROFESSORA.json e guia docs/entrega/APRESENTACAO-PROFESSORA.md.
