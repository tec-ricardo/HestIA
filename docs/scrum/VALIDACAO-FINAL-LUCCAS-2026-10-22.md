# Validação final das tarefas de Luccas

## Escopo verificado

Esta validação cruza as tarefas abertas no Jira com o código presente em `develop` e com a branch de validação final. O objetivo é confirmar o produto demonstrável sem considerar a criação da massa de dados da apresentação.

| Tarefa | Evidência no produto | Situação técnica |
| --- | --- | --- |
| SCRUM-465 | Pergunta passa pelo frontend, BFF, busca autorizada e retorna política com fonte e revisão | Integrada em `develop` e coberta por teste de integração |
| SCRUM-487 | Certificação não é concedida sem os requisitos; após aprovação é concedida uma única vez e preservada no histórico | Integrada em `develop` e coberta por teste de integração |
| SCRUM-493 | Erros recorrentes acionam uma única trilha; reincidência após conclusão cria novo ciclo vinculado | Implementada e coberta por teste de integração |
| SCRUM-498 | Aprovação, reprovação, nova tentativa, limite de tentativas e liberação de reforço são validados no servidor | Integrada em `develop` e coberta por teste de integração |
| SCRUM-513 | Frontend suporta consultar, gerar nova resposta e confirmar reutilização no backend, com fonte, histórico e retorno visual | Integrada e coberta pelo teste do BFF e pelo contrato da tela |
| SCRUM-523 | Passe apresenta nível atual, jornada, desafios, conquistas e próximo objetivo a partir da API de gamificação | Integrada e coberta pelos testes de progressão e contrato da tela |
| SCRUM-535 | Geração usa somente contexto autorizado, retorna fontes e não inventa resposta quando falta contexto ou o modelo falha | Integrada e coberta por testes de sucesso, ausência de fonte e falha do provedor |

## Fluxos para a demonstração

1. Entrar com uma conta real cadastrada e manter a empresa da sessão.
2. Abrir o Assistente RAG e consultar uma pergunta coberta por uma política indexada.
3. Mostrar a resposta recuperada, a fonte e a revisão; depois escolher **Gerar nova resposta**.
4. Reutilizar uma resposta elegível e mostrar a confirmação do servidor e o efeito no Passe HestIA.
5. Abrir um microtreinamento, concluir o conteúdo e enviar a avaliação.
6. Mostrar reprovação/nova tentativa ou aprovação, conforme a conta de demonstração.
7. Abrir o Passe HestIA para mostrar XP, nível, desafios, certificações e próximo objetivo.
8. Mostrar uma trilha corretiva gerada por ocorrência confirmada e seu vínculo com o histórico de aprendizagem.

## Condições externas

- O PostgreSQL deve estar ativo e com as migrações aplicadas.
- Backend e frontend devem usar as variáveis de ambiente documentadas no projeto.
- A demonstração de geração nova exige modelo e credencial configurados no servidor.
- A política usada no RAG precisa estar aprovada, dentro da validade, permitida para o perfil e indexada.
- A massa de apresentação deve utilizar contas e conteúdos autorizados pelo grupo; ela não faz parte desta validação de código.

## Critério de prontidão

O produto está pronto para ensaio quando as suítes Java e Node passam, o login real funciona, o banco responde e a política escolhida aparece na consulta RAG. Se a credencial do modelo não estiver configurada, a consulta e a reutilização continuam demonstráveis e a geração nova deve mostrar indisponibilidade sem fabricar resposta.
