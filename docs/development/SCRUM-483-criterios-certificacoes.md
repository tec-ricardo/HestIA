# Critérios para concessão de certificações

Task: [SCRUM-483](https://luccastoffano.atlassian.net/browse/SCRUM-483)  
História relacionada: US37 — Conceder certificações por evolução (SCRUM-473).  
Status da especificação: proposta inicial de produto para orientar a implementação.

## Finalidade

Reconhecer que o funcionário demonstrou aprendizagem em um tema de uso responsável de IA. A certificação é interna à HestIA e vinculada à empresa e ao usuário.

Este documento define regras de negócio. A implementação, a persistência e a visualização pertencem às demais tarefas da US37.

## Critérios obrigatórios

Todos os critérios abaixo devem ser atendidos em conjunto.

| Critério | Regra |
| --- | --- |
| Usuário elegível | Estar ativo e vinculado à empresa da certificação. |
| Conteúdo concluído | Concluir todos os microtreinamentos exigidos pela certificação. |
| Aprendizagem demonstrada | Obter aprovação nas avaliações obrigatórias. |
| Evidências registradas | Possuir registros de conclusão e aprovação associados ao próprio usuário. |
| Versão compatível | Atender aos requisitos da versão vigente da certificação. |
| Concessão única | Não possuir a mesma certificação na mesma versão e empresa. |

XP, nível e quantidade de acessos, isoladamente, não concedem certificação. A nota mínima e as regras de tentativa serão definidas na SCRUM-494 e utilizadas neste fluxo. Não se estabelece um limite numérico adicional neste documento.

## Primeira certificação proposta

Nome: **Fundamentos do uso responsável de IA**.

Conteúdos obrigatórios:

- Uso de ferramentas autorizadas.
- Proteção de dados e informações sensíveis.
- Políticas internas da empresa.
- Validação humana das respostas de IA.

A concessão acontece após a conclusão dos conteúdos e a aprovação nas avaliações correspondentes. O catálogo deve identificar os treinamentos e avaliações exigidos por cada versão da certificação.

## Condições que impedem a concessão

- Treinamento obrigatório incompleto.
- Avaliação obrigatória ausente ou sem aprovação.
- Usuário inativo.
- Evidências de outra empresa ou de outro usuário.
- Requisitos da certificação ainda não configurados.
- Certificação da mesma versão já concedida.

O sistema deve informar os requisitos pendentes. Dados ausentes não podem contar como requisitos atendidos. Uma solicitação repetida deve preservar a concessão existente, sem criar outra.

## Integração com gamificação e Passe HestIA

A certificação aparece como uma conquista no perfil e no Passe HestIA. Uma eventual recompensa de XP deve seguir a regra da gamificação e ser concedida uma única vez.

Erros posteriores podem gerar novos microtreinamentos. Eles não removem automaticamente uma certificação já conquistada.

## Histórico e atualização

Cada concessão deve preservar:

- Usuário e empresa.
- Código, nome e versão da certificação.
- Data da concessão.
- Conclusões e aprovações utilizadas como evidência.

Uma atualização dos requisitos cria uma nova versão e preserva a conquista anterior. Para esta primeira entrega, a proposta é não estabelecer expiração automática.

## Cenários de aceite para a implementação

| Cenário | Resultado esperado |
| --- | --- |
| Todos os requisitos atendidos | Conceder a certificação e registrar suas evidências. |
| Parte dos requisitos atendida | Informar as pendências e não conceder. |
| Treinamentos concluídos sem aprovação | Não conceder. |
| Solicitação repetida | Manter uma única concessão por usuário, empresa, certificação e versão. |
| Evidências de outra empresa ou de outro usuário | Rejeitar o uso dessas evidências. |
| Usuário inativo | Não conceder. |
| Requisitos ausentes | Não conceder e informar que a configuração está incompleta. |
| Nova versão dos requisitos | Preservar o histórico anterior e verificar os novos requisitos. |

A SCRUM-487 deverá validar os cenários de concessão e não concessão após a implementação. A SCRUM-483 não depende da carga real da QSoft para definir estes critérios, mas a concessão funcional depende dos registros de aprendizagem e das demais entregas da US37.
