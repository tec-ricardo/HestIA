# SCRUM-473 — Conceder certificações por evolução

## Objetivo no produto

Fechar o fluxo mínimo de certificações da HestIA: a empresa publica critérios versionados, o funcionário conclui os conteúdos e demonstra aprendizagem, o backend concede uma única certificação e a interface apresenta tanto a conquista quanto o progresso restante.

A implementação usa os critérios da [SCRUM-483](../development/SCRUM-483-criterios-certificacoes.md). XP, nível do Passe HestIA e quantidade de acessos não substituem as evidências obrigatórias.

## Fluxo funcional

1. Um administrador publica a definição versionada em `POST /certificacoes/definicoes`, informando as avaliações obrigatórias já publicadas.
2. Cada requisito fica ligado à versão do microtreinamento, ao conteúdo e à avaliação correspondente.
3. Conclusão de leitura sem aprovação não atende ao requisito.
4. Ao atualizar a aprendizagem, o backend verifica automaticamente as definições vigentes.
5. `POST /certificacoes/usuarios/{id}/verificar` permite reprocessar o fluxo com segurança e retorna progresso e pendências.
6. Quando todos os requisitos forem atendidos, a concessão é persistida uma única vez com as evidências usadas.
7. `GET /certificacoes/usuarios/{id}` preserva o histórico, inclusive de versões anteriores.
8. O BFF usa sempre o usuário da sessão, reprocessa a situação e devolve ao navegador somente dados de apresentação. IDs de tentativas, leituras e demais evidências internas não são expostos.
9. A tela de microtreinamentos mostra certificados concedidos e, para definições pendentes, a quantidade atendida e a quantidade restante.

## Regras preservadas

- Usuário e evidências devem pertencer à mesma empresa.
- O usuário precisa estar ativo.
- A definição precisa conter requisitos publicados e compatíveis.
- Uma concessão é única por empresa, usuário e versão da definição.
- Nova versão preserva a conquista anterior e exige os novos requisitos.
- Dados ausentes contam como pendência.
- Repetir a verificação não duplica a concessão.
- Erros posteriores não removem automaticamente uma certificação já concedida.

## Como conferir

1. Entre como administrador e publique avaliações e uma definição de certificação.
2. Entre como colaborador e abra **Microtreinamentos**.
3. Antes das aprovações, confira o cartão com o progresso e os requisitos restantes.
4. Conclua os conteúdos e obtenha aprovação nas avaliações obrigatórias.
5. Volte à tela de microtreinamentos: o cartão passa a mostrar a certificação concedida e o botão **Ver certificado**.
6. Recarregue a página e confirme que não surgiu uma segunda concessão.
7. Publique uma nova versão da definição e confirme que o certificado anterior continua no histórico enquanto a nova versão apresenta suas próprias pendências.

## Validação automatizada

- Backend: cobre ausência de aprovação, progresso, concessão após conclusão e aprovação, evidências preservadas e idempotência.
- BFF: cobre uso do usuário autenticado, reprocessamento seguro, remoção das evidências internas e contrato de progresso.
- Frontend: cobre visualização de pendências e troca para a certificação concedida.
