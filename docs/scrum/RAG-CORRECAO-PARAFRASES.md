# Correção da recuperação por paráfrases — 13/11/2026

A frase “Posso colocar o CPF do cliente no ChatGPT?” não recuperava a política: similaridade 0,382, corte demo 0,40. Os oito casos originais não cobriam essa forma de perguntar; a afirmação anterior de funcionamento genérico não era sustentada pelo corpus.

A indexação agora complementa o documento completo com trechos derivados de seus títulos e parágrafos. A consulta agrega por fonte, sem duplicá-la, e aplica empresa, perfil, status, expiração e ausência de dados sensíveis antes do limite. Trechos cujo snapshot não coincide com o conteúdo atual não participam. Reindexar substitui os trechos na mesma transação da verificação da fonte. V30 adiciona o armazenamento portátil; a distância é calculada pelo pgvector em PostgreSQL. H2 mantém a compatibilidade das migrações de testes, sem simular busca vetorial.

A calibração demonstrativa passou para 0,35 após conferir paráfrases e controles negativos; padrão fora de demo permanece 0,70. Não há lista de perguntas decoradas nem resposta fixa para CPF. Reindexação da fonte 2 realizada no banco demonstrativo hestia_demo_final_rag. A V30 foi ajustada antes de commit para também atender H2; somente o banco de ensaio que a tinha aplicado durante o desenvolvimento teve o tipo de coluna compatibilizado e o histórico reparado/validado, preservando seus dados. Nenhuma migração antiga de produção foi alterada.

## Evidências

- 15/15 casos ampliados aprovados: dez formas pertinentes e cinco controles sem fonte; geração real da resposta sobre CPF com fonte confirmada na tela.
- Corpus anterior: 8/8, consulta sem recompensa e confirmação única preservadas.
- Conversa: 7/7, incluindo isolamento e revogação.
- PostgreSQL: 10 testes direcionados aprovados, incluindo trecho mais próximo, snapshot antigo, deduplicação e permissão antes do limite.
- Regressão geral: 399 testes, 396 aprovados e 2 externos ignorados; um teste ainda esperava 29 migrações. Sua contagem e última versão foram corrigidas para 30 e ele passou em execução separada com verify. Não apresentar os recortes como uma única execução sem falhas.

Arquivos RAG-PARAFRASES-TRECHOS.json, RAG-TRECHOS-CORPUS-ORIGINAL.json e RAG-TRECHOS-CONVERSA.json em evidencias/. Script validar-parafrases-trechos.cjs usa somente o ambiente local demonstrativo e fonte 2; não executar sobre produção.

Limite: amostra didática pequena, não garante todas as formulações ou documentos futuros. O assistente continua fundamentado na base aprovada; estar relacionado ao trabalho não torna automaticamente conhecida uma regra que não foi cadastrada. Ausência de fonte deve continuar explícita.
