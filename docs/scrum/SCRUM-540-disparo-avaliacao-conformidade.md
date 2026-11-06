# SCRUM-540 — Disparo e tratamento da avaliação automática

Versão de especificação: 1.0, 06/11/2026. Rastreabilidade: PO-141, PO-103 e PO-105.
Entrega: contrato de produto para implementação nas SCRUM-557, 558 e 562. Não substitui o motor existente nem afirma que o disparo já está implementado.

## Gatilho e política

1. Disparar somente após a confirmação da persistência de um uso válido e pertencente à empresa autenticada. Uma transação revertida não gera avaliação.
2. Usar uma referência estável do uso e do evento. Reenvio, timeout ou retentativa devem recuperar o processamento existente.
3. Selecionar a versão de política aplicável à empresa, ao escopo e ao instante de uso, com vigência verificável. Preservar versão, revisão, hash e justificativa da seleção.
4. Não escolher a primeira política arbitrariamente. Ausência, sobreposição ou impossibilidade de comprovar vigência gera REQUER_REVISAO, com motivo explícito; não inventar aprovação ou violação.
5. Política atualizada depois do uso não reescreve fundamentos antigos. Reavaliação é ato autorizado, com nova revisão e histórico.
6. As evidências são obtidas no servidor; uma declaração do usuário deve ter autoria e data e ser apresentada como declaração.

## Evidência por critério existente

| Critério do enum | Evidência necessária | Quando ficar não verificado |
| --- | --- | --- |
| REGISTRO_DE_USO | ID persistido, autor, empresa, ferramenta, data, finalidade e origem | Registro inexistente/inconsistente: rejeitar antes do disparo; não fabricar avaliação |
| IA_AUTORIZADA | Ferramenta, status/vigência e permissões aplicáveis ao uso | Cadastro atual sem histórico suficiente ou escopo ambíguo |
| POLITICA_INTERNA | Texto e revisão da política, empresa, vigência e aplicabilidade | Nenhuma política aplicável, conflito entre políticas ou evidência de vigência ausente |
| FINALIDADE_ADEQUADA | Finalidade informada, mapeamento com as finalidades permitidas da política e contexto suficiente | Apenas enum preenchido sem regra que comprove adequação |
| DADOS_SENSIVEIS | Declaração sobre categorias de dados e controles aplicados, mais evidências autorizadas quando necessárias | Nenhuma declaração/evidência; não inferir ausência de dados sensíveis |
| VALIDACAO_HUMANA | Declaração/autoria/data de revisão quando exigida e vínculo com o resultado utilizado | Revisão requerida mas não comprovada; distinguir evidência ausente de declaração explícita de não revisão |
| LGPD | Evidências dos requisitos de tratamento definidos pela política e declaração responsável; validação humana quando necessária | Caixa não marcada ou evidência insuficiente; não concluir ilegalidade automaticamente |

Não solicitar cópia de dados pessoais/confidenciais reais para provar conformidade. Referências, categorias e evidências minimizadas devem bastar quando cabível. A avaliação da plataforma não equivale a parecer jurídico.

## Estados de processamento e resultado

Processamento e resultado de negócio são eixos separados:

| Processamento | Significado e efeitos |
| --- | --- |
| PENDENTE | Uso confirmado, aguardando execução; sem resultado conclusivo |
| EM_PROCESSAMENTO | Execução em andamento; mesma chave não cria outra execução efetiva |
| CONCLUIDA | Resultado e fundamentos persistidos de forma consistente |
| FALHA_TECNICA | IA, rede, banco ou integração falhou; permitir retentativa controlada, sem reprovar o usuário |

Cada critério recebe ATENDIDO, NAO_ATENDIDO ou NAO_VERIFICADO, com evidência e motivo. Se existir condição não aplicável, o responsável precisa versionar a dispensa e sua justificativa; não removê-la silenciosamente do conjunto obrigatório.

| Resultado existente | Regra para o novo fluxo |
| --- | --- |
| CONFORME | Todos os critérios obrigatórios comprovadamente atendidos |
| NAO_CONFORME | Pelo menos um requisito crítico comprovadamente não atendido: IA_AUTORIZADA, POLITICA_INTERNA ou LGPD |
| REQUER_REVISAO | Algum critério não verificado ou requisito não crítico não atendido, sem violação crítica comprovada |

Violação crítica comprovada prevalece mesmo havendo outras evidências desconhecidas; essas limitações continuam visíveis. Falha técnica sem resultado persistido não participa de C/N/R. Não existe conversão automática de NAO_VERIFICADO em NAO_ATENDIDO.

## Compatibilidade com o motor

AvaliacaoConformidadeService, regra CONFORMIDADE_CRITERIOS 1.0, hoje usa um conjunto booleano de critérios. Critério crítico ausente pode produzir NAO_CONFORME; finalidade preenchida e política ativa são verificações simplificadas. Esse contrato não contém a distinção exigida acima.

A SCRUM-557 deve implementar o novo disparo e a representação de evidências/estados, reutilizando persistência e consulta existentes quando compatíveis. Deve versionar a regra para distinguir o novo comportamento dos fundamentos 1.0. Não alimentar o conjunto antigo com confirmações inventadas para obter CONFORME, nem sobrescrever avaliações legadas. Esta especificação não altera esse código na branch.

## Efeitos e idempotência

- XP de REGISTRO_USO reconhece o registro, não a aprovação da conformidade.
- Recompensa da ação existente USO_CONFORME só após resultado CONFORME persistido e elegível; sem alterar pontuações existentes nesta tarefa.
- Chaves de evento diferentes por finalidade: registro, avaliação e eventual compensação; mesma referência de negócio não concede recompensa duas vezes, inclusive sob concorrência.
- NAO_CONFORME pode abrir/vincular tratamento e capacitação conforme regras existentes. REQUER_REVISAO pede evidência/revisão e não deve ser apresentado como punição.
- Falha após gravar resultado permite recuperar o resultado e concluir efeitos pendentes; resultado e efeitos precisam de coordenação transacional/idempotente.
- Reavaliação autorizada preserva resultado anterior. Se for necessário ajustar recompensa, usar movimento de compensação rastreável, sem apagar extrato.
- Registrar tentativas, datas, versão de regra/política, evidências, origem automática/manual, autor de revisão e correlação; nunca segredos.

## Cenários de aceite para implementação

| Cenário | Esperado |
| --- | --- |
| Uso não persistido | Nenhuma avaliação nem recompensa |
| Evento repetido/concorrente | Uma avaliação efetiva e um efeito por referência |
| Falta declaração LGPD | REQUER_REVISAO, sem concluir violação |
| Ferramenta comprovadamente proibida | NAO_CONFORME, com fundamento preservado |
| Política indisponível/ambígua | Revisão com motivo; nenhuma escolha arbitrária |
| Todos os requisitos comprovados | CONFORME e efeito de conformidade único |
| Provedor indisponível | FALHA_TECNICA, sem reprovação; retentativa recuperável |
| Resultado gravado e resposta perdida | Consulta recupera o resultado, sem duplicação |
| Alteração posterior de política | Histórico continua com fundamentos originais |
| Evento de outra empresa | Rejeitado antes da avaliação |
