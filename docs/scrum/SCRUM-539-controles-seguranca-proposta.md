# SCRUM-539 — Proposta de controles de segurança

Versão: seguranca-1.0, 06/11/2026. Catálogo aprovado por Luccas nesta conversa; a evidência de cumprimento continua obrigatória.
Finalidade: critérios verificáveis do indicador de Segurança da HestIA. Não representa uma política já existente na QSoft nem comprova que os controles foram implementados.

| Código | Controle proposto | Evidência de aprovação |
| --- | --- | --- |
| SEG01 | Login e acesso por perfil | Testes autenticados dos três perfis, incluindo tentativa de operação não permitida e sessão inválida |
| SEG02 | Isolamento por empresa e departamento autorizado | Testes negativos de consulta, alteração, histórico e exportação entre empresas/departamentos |
| SEG03 | Revogação de acesso | Teste de usuário desativado ou com perfil/permissão alterados, incluindo sessão já aberta |
| SEG04 | RAG limitado às fontes autorizadas | Busca e histórico bloqueiam fonte retirada, alterada ou não permitida; contexto/resumo também deve respeitar a revogação |
| SEG05 | Proteção de credenciais e dados nos canais | Configuração e revisão verificáveis: credenciais fora do repositório/logs, transporte protegido no ambiente publicado e respostas sem dados indevidos |
| SEG06 | Auditoria rastreável | Registro de autor, empresa, operação, data e resultado para ações críticas, com consulta restrita e sem senhas/tokens |
| SEG07 | Falhas e repetição sem efeitos indevidos | Testes de timeout, retentativa e concorrência preservam dados e evitam créditos ou operações duplicados |
| SEG08 | Backup e recuperação | Restauração controlada em ambiente descartável comprova recuperação de dados e migrações, com instruções e responsável |

## Avaliação proposta

Cada controle é ATENDIDO, NAO_ATENDIDO ou NAO_VERIFICADO, com evidência, ambiente, versão/commit, data e responsável. Existência de tarefa, tela ou código não basta para marcar ATENDIDO. A evidência deve corresponder à versão avaliada; mudança relevante exige nova verificação.

Reutilizar fórmula existente: 100 × controles atendidos / controles avaliados conclusivamente. Publicar também avaliados / obrigatórios como cobertura. Não verificado não equivale a controle aprovado ou reprovado. Cobertura incompleta precisa de aviso e impede tratar o resultado como avaliação completa. A aplicação literal de mínimo de 30 registros a este catálogo de oito controles não faz sentido; a regra específica exige confirmação de Luccas.

Proposta de aplicabilidade: os oito controles são obrigatórios para a versão publicada. Não excluir controles silenciosamente do denominador. Alteração de escopo ou dispensa exige decisão explícita e versionada. Aprovação integral exige evidências dos oito; nota calculada não representa certificação externa ou avaliação universal de segurança.

## Vínculos de implementação

Reutilizar entregas planejadas de permissões/revogação (548/550/551), ambiente publicado (555), idempotência (544/558/562), auditoria e interface (611), backup (588) e homologação final (590). Este documento não amplia silenciosamente o escopo dessas tarefas e não as marca como entregues.

## Aprovação registrada

Luccas aprovou esta proposta e autorizou implementação. O catálogo completo tem oito controles obrigatórios, com cobertura completa em vez de mínimo de 30 registros. Nenhum controle é presumido atendido.
