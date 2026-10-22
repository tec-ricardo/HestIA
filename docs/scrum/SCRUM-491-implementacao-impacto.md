# SCRUM-491 — Integração das trilhas ao histórico de microtreinamentos

Data: 21/10/2026. Implementação local, pendente de revisão e homologação.

Arquivos: `TrilhaService.java`, `ParticipacaoTrilha.java` e `ConclusaoMicrotreinamentoRepository.java`.

A sincronização usa as conclusões persistidas do participante, e nunca uma lista de conclusões enviada pelo navegador. Aproveita registros concluídos com data válida, inclusive conclusões anteriores à inscrição; esse aproveitamento é uma decisão técnica explícita desta implementação. O vínculo preserva o ID da conclusão, o ID do microtreinamento, a ordem da etapa e sua data de conclusão.

As consultas de participação/histórico sincronizam e persistem os vínculos dentro da transação. A última data de conclusão das etapas define o fim da trilha; datas futuras e conclusões incompletas não contam. O bloqueio do usuário serializa as sincronizações. Se for necessário guardar o instante de reconhecimento da conclusão separadamente da data da evidência, isso exige uma extensão explícita.

Impacto na SCRUM-527: `GET /trilhas/conclusoes/{id}` localiza as participações que usam uma conclusão já tratada pela recorrência. O colega pode cruzar o resultado com `RecorrenciaCapacitacaoService` sem duplicar o modelo de conclusões. A rotina existente da SCRUM-527 foi preservada; a chamada automática desse serviço à consulta de trilhas não foi introduzida.

Impacto no histórico: as evidências de microtreinamento não são sobrescritas. A trilha agrega esses registros. A sincronização atual ocorre nas consultas/inscrições; não há processo em segundo plano.

Teste: concluir duas etapas em datas distintas, consultar por conclusão, verificar o progresso intermediário e a data final e repetir a inscrição.
