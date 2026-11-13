# Homologações locais — 13/11/2026

Branch finalizacao, base 9f06150, incluindo PR 152/SCRUM-580. Imagens backend/frontend reconstruídas; ID da imagem backend conferido com o container em execução. Banco exclusivo do projeto Docker hestiafinalizacaovalidacao, sem dados empresariais reais.

- SCRUM-568-jornada-9f06150.json: 21 verificações HTTP aprovadas.
- SCRUM-585-relatorios-9f06150.json: 10 verificações HTTP aprovadas.
- SCRUM-585-ranking-http.json: 25 verificações HTTP aprovadas.
- SCRUM-590-perfis-local.json: 16 verificações HTTP aprovadas.

Total: 72 verificações HTTP aprovadas. Mais 12 testes unitários direcionados de investigação, ranking e classificação aprovados, zero falhas/erros/ignorados. Não somar este recorte aos ensaios anteriores como cenários distintos.

Conferidos: jornada de conformidade/capacitação e idempotência; fontes revogadas; oito fatores, valores da massa conhecida e consistência JSON/CSV; ranking restrito à empresa e gestor restrito ao departamento sem posição comparativa; funcionário sem acesso; período inválido; investigação com mesmos fatores/risco do ranking; IDs, estados e responsáveis de pendências comparados diretamente ao banco; departamento sem dados sem classificação inventada. Frontend: login dos três perfis, renderização gerencial, aviso de ausência, cache no-store, permissão, erro de período e logout.

Limites: chamadas HTTP locais, sem inspeção visual desktop/celular ou medição de desempenho acordada. Cabeçalho de proxy HTTPS foi simulado apenas para testar a sessão local; não comprova TLS ou publicação externa. Sem nova chamada a modelo de IA.

585 permanece parcial: integração 578/582 e fontes analíticas ainda em revisão, comparação completa na release aceita. A 580 está integrada e suas consultas foram testadas. 590 permanece parcial: falta URL cloud, sessão HTTPS real, checklist visual/responsivo e jornada contextual com IA real da 556. Sem hospedagem informada não é possível concluir as etapas publicadas.
