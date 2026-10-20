# Revisão do modelo inicial - Deliverable 1

Revisão conceitual de 20/10/2026. Base: SCRUM-132 a SCRUM-136.
Não há feedback docente específico fornecido.

A variável binária x[d,f] representa a seleção de uma ferramenta para um
departamento. Maximiza-se a soma dos benefícios, com restrições de orçamento,
risco por departamento, número de ferramentas e permissão de uso.

Os custos, benefícios e riscos da instância inicial são dados simulados.
Não correspondem a preços atuais ou medições observadas na empresa. O risco
ordinal é usado como escore aditivo por hipótese; essa hipótese necessita
calibração antes de aplicação operacional.

Para orçamento 1.400, a seleção ótima da instância é Marketing/Midjourney e
TI/Copilot, custo 1.300 e benefício 19. A enumeração examina as 64 combinações
e desempata por menor custo. O resultado é ótimo somente para os parâmetros
e restrições dessa instância.

A validação inicial comprova o resultado conhecido, mas não substitui testes
isolados de cada restrição. A revisão D2 acrescenta esses testes e cenários
comparativos. O algoritmo cresce exponencialmente e não oferece garantia de
tempo viável para instâncias corporativas grandes.

Correções: explicitação da natureza simulada dos dados, hipótese de soma
de riscos e limites da validação e escalabilidade. Justificativa: precisão
na interpretação dos resultados e no alcance da recomendação.
