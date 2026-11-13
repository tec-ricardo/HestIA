# SCRUM-556 — roteiro de aceite contextual com IA real

Preparado em 12/11/2026. Pendente: ambiente publicado e provedor ativo no servidor. Luccas confirmou que o uso atual é somente no computador. Nenhuma chave ativa foi encontrada no ambiente do processo examinado; não incluir chaves nos arquivos ou evidências.

Reaproveitar corpus/relatórios 545 e 546 e o orçamento/resumo da 612/551. Não repetir calibração da busca como se fosse uma medição nova de contexto. Na release publicada registrar URL, commit, data, modelo, corpus/hash, perfil, conversa e IDs das fontes, preservando segredos.

| Caso | Aceite esperado |
| --- | --- |
| Pergunta direta e reformulação | Recuperar fonte autorizada, título/versão e limites verificáveis |
| Pergunta de continuação | Manter conversa e interpretar referência com histórico elegível |
| Retomar após recarregar | Mesmos IDs da conversa e interação; continuidade preservada |
| Nova conversa | Não incorporar assunto de outra conversa |
| Conversa longa | Orçamento respeitado; resumo parcial identificado, sem inventar fonte |
| Fonte ausente | Ausência explícita; resposta geral só se habilitada e identificada como sem fontes |
| Falha do provedor | Mensagem de indisponibilidade, sem resposta fabricada |
| Revogação/retirada/alteração | Fonte não aparece em histórico utilizável, contexto, resumo ou próxima geração |
| Sugestão e confirmação | Consultar não concede crédito; confirmar e reenviar produz um único efeito |
| Geração alternativa | Resposta nova com as fontes atuais; identificar limites da geração |

545/546 registram execuções reais anteriores, com suas limitações. Os testes atuais de fonte e jornada são controlados; não atribuir a eles uma chamada OpenAI ao vivo. Resultado final desta tarefa: ainda não homologado no ambiente publicado.

## Aceite técnico local com provedor real — 13/11

OpenAI configurada apenas no processo Java, política fictícia v1.1 indexada. Busca 8/8 e conversa 7/7 aprovadas; geração confirmada no site local. Correção do limiar demo configurável incluída. Evidências RAG-FINAL-BUSCA.json e RAG-FINAL-CONVERSA.json. Não representa publicação cloud nem aceite de URL pública.
