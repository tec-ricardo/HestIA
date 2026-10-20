# Critérios da avaliação pós-microtreinamento

Task: [SCRUM-494](https://luccastoffano.atlassian.net/browse/SCRUM-494)

História relacionada: US55 — Verificar aprendizagem após microtreinamento (SCRUM-475).

Status: proposta inicial de regras de produto. Os limites numéricos abaixo são decisões propostas nesta especificação, e não requisitos previamente definidos no Jira ou funcionalidades já implementadas.

## Objetivo e escopo

Verificar se o funcionário compreendeu o conteúdo concluído e consegue aplicá-lo a situações de uso responsável de IA. Registrar evidências de aprendizagem para alimentar o histórico, as trilhas de capacitação e os critérios de certificação.

Esta entrega define regras. Não implementa questionários, correção, API, interface ou persistência. A SCRUM-498 deverá validar os cenários após a implementação.

## Estrutura inicial proposta

| Elemento | Regra |
| --- | --- |
| Vínculo | Uma avaliação associada ao microtreinamento, ao conteúdo e às respectivas versões |
| Quantidade | Cinco questões obrigatórias por avaliação |
| Formato | Múltipla escolha, com uma única alternativa correta e pelo menos três alternativas por questão |
| Conteúdo | Questões ligadas ao conteúdo apresentado, incluindo situações de aplicação no trabalho |
| Peso | Mesmo peso para todas as questões, sem pontuação parcial |
| Nota | Percentual de acertos entre 0 e 100 |
| Aprovação | Pelo menos 80% de acertos, equivalentes a quatro das cinco questões |
| Tempo | Sem limite de tempo na primeira entrega |
| Tentativas | Até três tentativas finalizadas por ciclo de avaliação |

Cada questão deve ter identificador, tema, alternativas, gabarito e explicação da resposta correta. O conteúdo precisa estar aprovado para uso pela empresa. Questões não podem exigir conhecimento que o microtreinamento não oferece.

A primeira entrega usa um tema principal por microtreinamento. A quantidade de questões, o gabarito e a nota mínima devem compor a versão publicada da avaliação. Sua alteração cria nova versão, preservando as tentativas existentes.

## Elegibilidade e abertura da avaliação

- Exigir usuário ativo e pertencente à empresa autorizada.
- Exigir conclusão registrada do conteúdo correspondente antes da primeira tentativa.
- Disponibilizar apenas avaliações ativas e com configuração completa.
- Vincular tentativa, usuário e empresa no servidor a partir do contexto autenticado.
- Não disponibilizar gabaritos ou respostas de outros usuários antes do envio.
- Manter no máximo uma tentativa em andamento por usuário, empresa, avaliação e ciclo.

Concluir o conteúdo e demonstrar aprendizagem são fatos distintos. A avaliação não deve apagar ou reescrever o registro de conclusão do microtreinamento.

## Correção e resultado

A fórmula é: nota = quantidade de acertos / quantidade de questões válidas * 100.

Corrigir no servidor, utilizando o gabarito e a versão registrados na abertura da tentativa. A classificação usa a nota calculada, sem arredondamento para atingir a aprovação. No formato inicial, as notas possíveis são 0, 20, 40, 60, 80 e 100.

Somente um envio completo e válido pode finalizar uma tentativa. Envio incompleto, alternativa inexistente, questão duplicada, questão externa à avaliação ou erro técnico deve retornar uma mensagem clara, sem registrar reprovação e sem consumir uma tentativa finalizada.

Após finalizar, apresentar nota, situação, quantidade de acertos e explicações das questões. Indicar quais conteúdos revisar. Falha de comunicação após o envio deve permitir consultar o resultado já registrado.

O reenvio do mesmo identificador de tentativa não pode gerar nova nota, consumir outra tentativa ou disparar nova recompensa. Tentativas finalizadas são imutáveis. Corrigir defeitos de conteúdo ou gabarito exige revisão autorizada e registro do motivo, preservando o resultado original e seu histórico.

## Estados propostos

| Estado | Significado |
| --- | --- |
| Não iniciada | Usuário elegível ainda não abriu uma tentativa |
| Em andamento | Tentativa aberta, ainda sem envio completo e válido |
| Aprovada | Tentativa finalizada com nota maior ou igual a 80% |
| Reprovada | Tentativa finalizada com nota abaixo de 80% |
| Aguardando reforço | Terceira reprovação no ciclo, exigindo intervenção de capacitação antes de novo ciclo |

Não iniciada e em andamento não representam reprovação. Aguardando reforço descreve a situação do ciclo; as três tentativas anteriores mantêm seus resultados individuais.

## Tentativas e reforço

1. A primeira reprovação recomenda revisar os conteúdos relacionados aos erros e aciona ou adapta a trilha pertinente conforme a SCRUM-488.
2. Enquanto houver menos de três tentativas finalizadas no ciclo e nenhuma aprovação, permitir nova tentativa após a apresentação do feedback. Revisitar o conteúdo é recomendado, sem inventar uma nova conclusão do treinamento.
3. Depois da terceira reprovação, colocar o ciclo em aguardando reforço e informar os próximos passos. Não bloquear o acesso do funcionário ao restante da plataforma.
4. Para liberar outro ciclo, exigir conclusão das etapas de reforço indicadas e liberação registrada por responsável de capacitação com permissão específica na empresa. A implementação deve definir essa permissão no controle de acesso, sem atribuí-la automaticamente a todo gestor.
5. Registrar justificativa, responsável, data, evidências do reforço e vínculo com o ciclo anterior. Um novo ciclo oferece até três tentativas, sem apagar as anteriores.
6. Após aprovação, impedir novas tentativas comuns da mesma versão e ciclo. Uma reavaliação precisa de justificativa explícita, como reincidência ou mudança dos requisitos, e deve abrir novo ciclo vinculado ao histórico.

Repetir o mesmo pedido de abertura ou liberação não cria ciclos adicionais. A ausência de avaliador autorizado não deve liberar tentativas automaticamente: informar que a intervenção está pendente.

## Critério de aprendizagem validada

Considerar aprendizagem validada quando houver conclusão do conteúdo e pelo menos uma tentativa aprovada, ambas do mesmo usuário e empresa e compatíveis com a versão exigida.

Preservar todas as notas e usar a aprovação como evidência, sem calcular média que elimine uma aprovação posterior válida. Um novo requisito ou ciclo de reavaliação pode ficar pendente sem apagar a aprendizagem demonstrada no ciclo anterior.

Não há expiração automática nesta proposta. Mudanças de conteúdo devem declarar se exigem reavaliação. A decisão deve ser explícita e versionada, sem invalidar silenciosamente o histórico.

## Integrações com as SCRUM-483 e 488

- Certificação: a aprovação atende somente ao requisito de aprendizagem do microtreinamento correspondente. A concessão continua exigindo todos os critérios da SCRUM-483.
- Trilhas: cada reprovação finalizada pode acionar ou adaptar a trilha do tema. Uma trilha ativa recebe a nova evidência sem duplicação, conforme a SCRUM-488.
- Conclusão da trilha: exige todas as etapas e avaliações obrigatórias, não apenas esta avaliação isolada.
- Reforço após aprovação: nesta primeira versão de tema único, uma questão errada gera feedback, mas não aciona sozinha outra trilha adaptativa. O gatilho por lacuna em avaliação aprovada da SCRUM-488 exige critérios adicionais explícitos e versionados, caso futuramente adotado.
- Gamificação: uma eventual recompensa segue sua regra própria e não se repete por reenvio, repetição de tentativa ou consulta do resultado. Este documento não define valores de XP.

## Histórico mínimo

Registrar usuário, empresa, treinamento e versão do conteúdo, avaliação e versão, ciclo e motivo de abertura, número da tentativa, datas de início e finalização, questões e alternativas apresentadas, respostas enviadas, gabarito versionado utilizado, quantidade de acertos, nota e resultado.

Registrar também vínculos com trilhas, ciclos anteriores e intervenções de liberação, quando houver. Restringir acesso aos dados de aprendizagem conforme as permissões da empresa. Não utilizar conteúdo sensível real de funcionários ou clientes nas questões de exemplo.

## Cenários de aceite para a SCRUM-498

| Cenário | Resultado esperado |
| --- | --- |
| Quatro acertos em cinco questões | Aprovar com nota 80 |
| Três acertos em cinco questões | Reprovar com nota 60 e apresentar feedback |
| Cinco acertos | Aprovar com nota 100 |
| Nenhum acerto em envio completo | Reprovar com nota 0 |
| Questão sem resposta ou alternativa inválida | Rejeitar o envio, mantendo a tentativa em andamento |
| Erro técnico antes da persistência | Não registrar resultado nem consumir tentativa finalizada |
| Reenvio de tentativa finalizada | Retornar o resultado existente, sem novos efeitos |
| Usuário inativo ou de outra empresa | Rejeitar o acesso |
| Conteúdo ainda não concluído | Não liberar a avaliação |
| Avaliação sem cinco questões ou gabarito completo | Não disponibilizar para início |
| Aprovação na segunda tentativa | Validar aprendizagem e preservar a primeira reprovação |
| Terceira reprovação | Aguardar reforço antes de novo ciclo |
| Liberação sem permissão ou sem evidências de reforço | Rejeitar a liberação |
| Reforço concluído e liberação autorizada | Abrir um único novo ciclo vinculado ao anterior |
| Atualização da avaliação | Preservar versões e resultados antigos, sem trocar o gabarito de tentativa aberta |
| Reprovação com trilha ativa do mesmo tema | Vincular ao ciclo de capacitação existente |
| Aprovação sem demais requisitos da certificação | Não conceder certificação automaticamente |

## Dependências de implementação

A especificação pode ser definida sem a carga real da QSoft. O fluxo funcional depende do catálogo versionado, das conclusões de treinamento, do modelo de tentativas, da correção no servidor, do controle de acesso e da integração com o histórico e as trilhas. Essas entregas permanecem nas tarefas de implementação da US55 e das histórias relacionadas.
