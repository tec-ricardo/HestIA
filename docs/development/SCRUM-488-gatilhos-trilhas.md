# Gatilhos das trilhas corretivas e adaptativas

Task: [SCRUM-488](https://luccastoffano.atlassian.net/browse/SCRUM-488)
História relacionada: US38 — Acionar trilhas corretivas/adaptativas (SCRUM-474).
Status: proposta de regras de negócio para orientar a implementação.

## Objetivo e escopo

Definir quais erros, não conformidades, resultados de avaliação e recorrências acionam uma trilha de capacitação. A trilha organiza microtreinamentos relacionados à necessidade do funcionário, com etapas e critérios de conclusão.

Esta entrega documenta as regras. Não implementa disparos automáticos, novas entidades ou alterações no banco. A modelagem, a integração e a validação funcional permanecem nas demais tarefas da US38, incluindo a SCRUM-493.

## Referências do projeto

- `CriterioErroRecorrente` define três erros em um período de 30 dias.
- `TipoErro` já relaciona os erros monitorados aos temas de treinamento.
- `NecessidadeMicrotreinamentoService` identifica necessidade a partir de ocorrências recebidas.
- `RecomendacaoMicrotreinamentoService` consulta o catálogo fictício de microtreinamentos por tema.

Essas classes são a base existente. Não comprovam que os gatilhos e o ciclo de trilhas descritos aqui já funcionam. O serviço atual recebe datas de ocorrências; a futura integração deverá garantir que elas sejam distintas e pertençam ao mesmo usuário, empresa e tipo de erro.

## Tipos de trilha

| Tipo | Finalidade | Evidência de origem |
| --- | --- | --- |
| Corretiva | Tratar um problema confirmado no uso de IA | Erro ou não conformidade com critério identificado |
| Adaptativa | Ajustar a capacitação a uma dificuldade de aprendizagem | Avaliação finalizada sem aprovação ou resultado por tema que permita identificar uma lacuna |

Uma pontuação baixa de maturidade ou poucos registros não demonstram, isoladamente, erro individual ou necessidade de capacitação. Falta de dados não deve gerar reprovação ou trilha automática.

## Matriz de gatilhos proposta

| Evento | Condição | Ação |
| --- | --- | --- |
| Erro recorrente | Três ou mais ocorrências distintas do mesmo tipo, para o mesmo usuário e empresa, na janela de 30 dias | Acionar uma trilha corretiva do tema correspondente |
| Não conformidade de proteção de dados, LGPD, política interna ou ferramenta não autorizada | Problema confirmado, com identificação do critério e evidência válida | Acionar trilha corretiva já na primeira ocorrência |
| Finalidade inadequada, ausência de validação humana ou uso não registrado | Aplicar a regra de recorrência | Acionar trilha corretiva ao atingir o limite |
| Avaliação sem aprovação | Tentativa finalizada e corrigida segundo a versão vigente da regra | Acionar ou adaptar uma trilha para o tema avaliado |
| Dificuldade por tema em avaliação aprovada | A avaliação disponibiliza resultado por tema e um critério explícito identifica a lacuna | Recomendar reforço adaptativo, sem retirar a aprovação |
| Reincidência após capacitação | Nova ocorrência confirmada do mesmo tipo, posterior à conclusão da trilha | Criar novo ciclo corretivo vinculado ao anterior, preservando o histórico |

Os disparos imediatos, o reforço após aprovação e a abertura de novo ciclo são propostas desta especificação. Não alteram silenciosamente a regra de recorrência existente. A SCRUM-494 deve definir notas, tentativas e aprovação; este documento não cria uma nota mínima paralela.

Uma avaliação marcada como `REQUER_REVISAO` não aciona trilha por si só. É necessário confirmar o problema e identificar seu critério antes de atribuir uma necessidade ao funcionário. Se a regra não permite vincular o resultado a um dos temas existentes, encaminhar para revisão em vez de adivinhar o tema.

## Mapeamento dos erros e temas

O mapeamento segue o enum `TipoErro` atual.

| Tipo de erro | Tema |
| --- | --- |
| `DADOS_SENSIVEIS` | `PROTECAO_DE_DADOS` |
| `FERRAMENTA_NAO_AUTORIZADA` | `USO_DE_FERRAMENTAS_AUTORIZADAS` |
| `VIOLACAO_POLITICA_INTERNA` | `POLITICAS_INTERNAS` |
| `FINALIDADE_INADEQUADA` | `BOAS_PRATICAS_DE_USO` |
| `AUSENCIA_VALIDACAO_HUMANA` | `VALIDACAO_HUMANA` |
| `USO_NAO_REGISTRADO` | `REGISTRO_E_AUDITORIA` |
| `VIOLACAO_LGPD` | `LGPD` |

## Contagem e elegibilidade

1. Considerar apenas eventos válidos do mesmo usuário e empresa.
2. Para recorrência, agrupar pelo mesmo tipo de erro. Não somar problemas diferentes para atingir o limite.
3. Contar cada ocorrência uma única vez pelo identificador do evento de origem. Reprocessamento não cria novo erro.
4. Considerar datas entre o instante de análise menos 30 dias e o instante de análise, com limites inclusivos. Descartar datas futuras, ausentes ou inválidas.
5. Exigir usuário ativo, vínculo autorizado e conteúdo ativo compatível com o tema.
6. Considerar somente avaliações finalizadas e corrigidas. Avaliação não iniciada, incompleta ou indisponível não equivale a reprovação.
7. Se faltar conteúdo para a necessidade identificada, registrar a pendência de catálogo para revisão. Não criar uma trilha vazia nem atribuir treinamento de tema diferente.

## Prevenção de duplicidade e prioridade

Manter no máximo um ciclo ativo para o mesmo usuário, empresa e tema. Se outro gatilho ocorrer enquanto a trilha estiver pendente ou em andamento, associar a nova evidência ao ciclo existente e reavaliar seu conteúdo, sem apagar etapas realizadas.

Quando gatilhos corretivo e adaptativo tratarem do mesmo tema, priorizar a correção do problema confirmado e incorporar o reforço de aprendizagem no mesmo ciclo. Necessidades de temas distintos podem gerar trilhas distintas.

O mesmo evento não pode abrir novos ciclos quando reprocessado. Reincidência após conclusão exige evento novo, com data posterior à conclusão, e referência ao ciclo anterior. Ocorrências antigas não bastam para reabrir automaticamente uma trilha concluída.

## Conclusão e preservação da aprendizagem

Uma trilha só deve constar como concluída quando todas as suas etapas obrigatórias e avaliações exigidas estiverem atendidas, segundo a versão registrada de seus requisitos.

Treinamentos já concluídos e aprovados podem ser aproveitados quando atendem à mesma versão de conteúdo e aos requisitos do novo ciclo. Nova conclusão de conteúdo, revisão dirigida ou avaliação só deve ser exigida quando houver justificativa registrada, como reincidência ou mudança de requisito. Não apagar conclusões anteriores.

Ausência de novas ocorrências não conclui a trilha sozinha. Reprovação gera reforço ou nova tentativa conforme a SCRUM-494, sem criar ciclos duplicados. Aprovação por si só também não concede certificação: a SCRUM-483 define seus requisitos próprios.

## Evidências e explicação ao usuário

Registrar, na implementação futura:

- Usuário e empresa.
- Tipo de trilha, tema e motivo do acionamento.
- Tipo e identificador dos eventos de origem.
- Datas de ocorrência e de processamento.
- Quantidade e janela consideradas, quando houver recorrência.
- Avaliação e tentativa utilizadas, quando aplicável.
- Versão da regra e dos requisitos da trilha.
- Ciclo anterior, se houver reincidência.
- Etapas aproveitadas e etapas adicionais, com justificativa.

A interface deve explicar o motivo em linguagem simples e mostrar as etapas pendentes. Exemplo: “Você teve três ocorrências de ausência de validação humana nos últimos 30 dias. Esta trilha orienta como revisar respostas de IA antes de utilizá-las.” Não expor conteúdo sensível do evento na mensagem.

## Cenários de aceite para a SCRUM-493

| Cenário | Resultado esperado |
| --- | --- |
| Duas ocorrências de ausência de validação humana em 30 dias | Não acionar pelo gatilho de recorrência |
| Três ocorrências distintas desse tipo na janela | Acionar uma trilha corretiva de validação humana |
| Terceira ocorrência fora da janela | Não contar essa ocorrência |
| Ocorrência exatamente no início da janela | Contar a ocorrência |
| Ocorrência futura ou sem data | Não contar |
| Evento reprocessado | Não duplicar a contagem nem o ciclo |
| Não conformidade confirmada de proteção de dados | Acionar trilha corretiva na primeira ocorrência |
| Avaliação requer revisão, sem problema confirmado | Aguardar confirmação |
| Avaliação corrigida sem aprovação | Acionar ou adaptar trilha do tema avaliado |
| Avaliação incompleta | Não tratar como reprovação |
| Novo gatilho do mesmo tema com ciclo ativo | Vincular evidência ao ciclo existente |
| Erro novo após conclusão | Criar novo ciclo com vínculo ao histórico anterior |
| Mesmo erro antigo após conclusão | Não criar novo ciclo |
| Usuário de outra empresa ou inativo | Não acionar |
| Tema sem conteúdo ativo disponível | Registrar pendência de catálogo e não criar trilha vazia |
| Etapas e avaliações obrigatórias atendidas | Concluir a trilha preservando as evidências |

## Dependências para implementar

A definição das regras pode avançar sem a carga real da QSoft. O funcionamento automático exige fontes de eventos identificáveis, isolamento por empresa, catálogo de conteúdos, resultados de aprendizagem e persistência dos ciclos. Estes pontos precisam ser integrados e testados nas tarefas de implementação da US38.
