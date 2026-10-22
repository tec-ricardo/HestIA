# SCRUM-489 — Modelo de trilhas, etapas e progresso

Data: 21/10/2026. Implementação local, pendente de revisão e homologação.

Arquivos: `trilha/model/Trilha.java`, `trilha/model/ParticipacaoTrilha.java`, repositórios do pacote `trilha`, `TrilhaService.java` e `TrilhaController.java`.

Uma trilha pertence a uma empresa e contém uma lista ordenada, não vazia e sem repetições de IDs do catálogo ativo. Gestores e administradores criam trilhas; funcionários consultam e iniciam apenas o próprio progresso. O serviço valida o usuário ativo, a empresa e o catálogo no servidor.

A participação registra usuário, empresa, início, status e fim. Os estados são `NAO_INICIADA`, `EM_ANDAMENTO` e `CONCLUIDA`. O progresso é etapas concluídas / total de etapas × 100. Repetir a inscrição devolve a participação existente. O bloqueio do usuário na transação e a restrição única no banco evitam inscrições duplicadas concorrentes.

Impacto: oferece a estrutura que faltava à SCRUM-527 para associar os microtreinamentos a uma trilha identificável. É uma trilha de conclusão de conteúdos: avaliações obrigatórias e novos ciclos corretivos precisam das tarefas próprias de aprendizagem/acionamento. As etapas são imutáveis depois da criação nesta API, preservando a interpretação do progresso.

Persistência: migração V18 cria `trilhas`, `trilha_etapas`, `participacoes_trilha` e `trilha_conclusoes`. Foi validada em PostgreSQL isolado, sem aplicar alterações na base do projeto. A numeração evita conflito com a V17 de tratamentos de não conformidade integrada na develop. Testes verificam estados, ordem, percentuais, idempotência, catálogo inválido e isolamento por usuário/empresa.
