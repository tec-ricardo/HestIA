# ADR-002 - BFF de composição e integração da Sprint 3

Data: 21/10/2026. Tasks de Ricardo: SCRUM-422, 423, 424, 425, 426, 462, 463 e 518.

## Contexto e decisão

A interface existente é Express/EJS. Ela precisa encaminhar a identidade da sessão, adaptar contratos de avaliações, compor maturidade e tratar indisponibilidade sem resultados demonstrativos. Foi mantido Express como BFF. Controllers Java continuam responsáveis por autorização, persistência, correção de avaliações, revisão de conteúdo e acesso ao PGVector. O BFF não calcula notas, não decide concessões e não consulta o banco diretamente.

`api-contexto.js` mantém o token por requisição com AsyncLocalStorage. `integracoes-sprint.js` concentra adaptadores novos, montados depois da autenticação e antes das rotas de protótipo. A empresa é resolvida no backend e o usuário de novos registros é obtido da sessão. Erros de backend são expostos como indisponibilidade/validação; não se usa fallback de números, gabaritos ou respostas de demonstração.

## Contratos e responsabilidades

* Maturidade: resultado por período, versões, fontes, cobertura e motivos; apurações preservadas e evolução com comparação de escopo, versões, parâmetros e duração.
* Confiança: `ConfiancaIndicadoresPort` é o ponto de integração com as SCRUM-427/428 de Luccas. Sem implementação desse contrato, a resposta é NAO_CLASSIFICADA. O BFF não cria uma classificação alternativa.
* Cobertura: a SCRUM-396 de Knorre está no PR #94. Seu calculador não foi copiado nem substituído. As fontes fornecidas por Ricardo distinguem nota descritiva de nota elegível ao global; o consumidor deve usar apenas notas elegíveis, preservando ausências como null. Cobertura de registros e cobertura de pesos são métricas diferentes.
* RAG: recuperação e autorização em Java usando o BuscaVetorialService existente de Knorre. A geração recebe somente trechos retornados e revalidados; ausência de conteúdo impede chamada de geração. O provedor e o modelo são configuração do servidor, sem credenciais no frontend.
* Aprendizagem: o frontend de Kevin mantém seus templates; a correção demonstrativa de três perguntas/70% foi substituída por chamadas ao catálogo publicado de cinco perguntas/80%, conforme a proposta SCRUM-494. Não se publica questionário nem certificação automaticamente com conteúdo inventado.
* Respostas administrativas: adaptadores da SCRUM-518 e uma página funcional de integração separada. Eles não substituem o planejamento visual das SCRUM-514 a 517 de Kevin.

## Alternativas e consequências

Um BFF Java adicional duplicaria o processo de adaptação que já existe em Express. Acesso direto do navegador ao banco ou ao provedor exporia detalhes e distribuiria regras. Por isso, foram mantidos contratos HTTP e responsabilidade de regras em Java.

O BFF compartilha o processo da interface; sua separação futura depende de demanda de escala. Os templates de demonstração ainda existentes em outras áreas não comprovam integração dessas áreas. A autorização é repetida no backend, independentemente dos filtros ou botões da interface.

## Conciliação com o grupo

A develop foi atualizada até 90c9529 (PR #104). A rota e o template de ocorrências de Kevin foram preservados em uma conciliação de três vias de server.js. Não foram alteradas as regras de tratamento de não conformidade das SCRUM-370 a 373. Não foram reimplementados os cálculos de recorrência/efetividade de Knorre: a integração da SCRUM-491 acrescenta vínculos de trilhas ao resultado existente.

Não houve publicação de PR, merge remoto, mudança de status no Jira ou mensagem enviada aos colegas nesta etapa. Ausência de conflito textual nos arquivos consultados não substitui revisão independente e homologação dos contratos pelo grupo.
