# SCRUM-503, SCRUM-529 e SCRUM-534 — Validações integradas

## Referências
- [SCRUM-503](https://luccastoffano.atlassian.net/browse/SCRUM-503) / [PO-92 — US56](https://luccastoffano.atlassian.net/browse/PO-92): qualidade, revisão e retirada de respostas.
- [SCRUM-534](https://luccastoffano.atlassian.net/browse/SCRUM-534) / [PO-106 — US90](https://luccastoffano.atlassian.net/browse/PO-106): autorização e elegibilidade da recuperação.
- [SCRUM-529](https://luccastoffano.atlassian.net/browse/SCRUM-529) / [PO-104 — US86](https://luccastoffano.atlassian.net/browse/PO-104): recorrência após capacitação.

## Resultado no produto
A validação exercita as rotas HTTP, a autenticação existente, a persistência e a busca com pgvector. Além dos testes, corrige duas falhas concretas encontradas e completa o registro obrigatório da retirada:
1. A seleção de uma resposta e o registro da reutilização eram separados. Revogar a permissão após a seleção ainda permitia contabilizar uma reutilização pela operação interna. O teste de regressão reproduziu o problema antes da correção.
2. Consultas com prompt nulo falhavam no PostgreSQL por tipagem do parâmetro usado em lower/concat. A consulta agora tipa o texto explicitamente e foi validada nas rotas reais.
3. A retirada não guardava justificativa, autor e data, exigidos na PO-92. Esses campos agora são persistidos e podem ser consultados por um responsável autorizado.

As alterações das SCRUM-372/373, ainda sem commit, foram preservadas e reunidas nesta entrega para aplicar a sequência V18/V19 em um único PR.

## SCRUM-503 — Revisão e retirada

### Consultar contexto e versões, inclusive após retirada
GET /api/respostas-reutilizaveis/{id}/revisao

Exige Bearer e permite acesso ao ADMIN da empresa ou ao autor quando seu perfil continua autorizado. Não dá permissão administrativa a qualquer usuário que possa apenas ler ou reutilizar o conteúdo.

A resposta inclui autoria do conteúdo, data de criação, categoria, modelo, prompt, resposta, situação de reutilização, versões com origem/data/textos e os dados da retirada. Não expõe credenciais nem embedding no DTO de revisão. A resposta usa Cache-Control: no-store.

### Revisar texto
PUT /api/respostas-reutilizaveis/{id}

O contrato de AtualizacaoConteudoDTO existente continua: promptOriginal, respostaGerada, origem e tipoConteudo. Uma mudança preserva a versão anterior, registra uma nova versão e invalida os embeddings antigos. Conteúdo retirado não pode ser atualizado por essa rota.

### Retirar com justificativa
DELETE /api/respostas-reutilizaveis/{id}

Agora exige corpo JSON:
```json
{"justificativa":"O conteúdo está desatualizado e não deve voltar às sugestões."}
```

Justificativa obrigatória, não composta apenas de espaços, limitada a 1000 caracteres. Envie Content-Type: application/json. A autoria e a data vêm do servidor; campos extras de autoria enviados pelo cliente não substituem o usuário autenticado.

Retorna 204 e:
- marca reutilizavel=false;
- registra retiradaPorId, retiradaEm e justificativaRetirada;
- invalida embedding e embedding_vector;
- conserva o conteúdo e suas versões anteriores;
- impede novas leituras elegíveis, sugestões e reutilizações.

Repetir uma retirada válida retorna 204 sem alterar o primeiro autor, data ou motivo. Retiradas legadas continuam com metadados desconhecidos e retiradaDocumentada=false, sem inventar evidências retroativas. O bloqueio legado por reutilizavel=false continua sendo respeitado.

Não é feita exclusão física nem reativação silenciosa de conteúdo retirado. A consulta administrativa de revisão permite conferir o registro preservado.

## SCRUM-534 — Isolamento e autorização
O registro de reutilização agora recebe o usuário autenticado, bloqueia a linha e valida empresa, perfil, reutilizavel e ausência de dados sensíveis dentro da mesma transação da contagem e recompensa. A autorização/revogação de perfis usa o mesmo bloqueio da resposta.

A aplicação aplica:
- empresa efetiva da sessão, sem permitir trocar a empresa no corpo da busca semântica;
- perfil autorizado antes da recuperação;
- retirada e dados sensíveis como impedimentos à reutilização;
- ADMIN ou autoria autorizada para manutenção;
- ADMIN para concessão de perfis;
- sessão válida e conta ativa.

Os testes verificam IDs de outra empresa, IDs inexistentes/negativos, usuário da mesma empresa sem perfil permitido, tentativa de elevar seus próprios privilégios, token ausente/inválido, conta inativa, autoria falsa no cadastro e embedding enviado pelo navegador.

A recusa não incrementa quantidadeReutilizacoes, tokensEconomizados ou movimentos de experiência. O teste de retirada confirma que uma reutilização anterior válida continua preservada e que tentativas posteriores não geram novas recompensas.

A busca vetorial é validada com PostgreSQL e pgvector reais. O gerador de embedding é substituído por um vetor determinístico de 1536 dimensões somente no teste: isso prova filtros, persistência e retirada, mas não mede a qualidade semântica de um provedor de IA.

## SCRUM-529 — Cenários conhecidos de recorrência
Integra registros de uso, avaliações com fundamentos preservados, conclusão de capacitação, fonte de recorrência e indicador de efetividade.

Convenções existentes verificadas:
- recorrência: pelo menos três ocorrências distintas;
- período anterior: [conclusão - 30 dias, conclusão);
- período posterior: [conclusão, conclusão + 30 dias);
- corte de observação inclusivo;
- comparação: 100 × (ocorrências antes - ocorrências depois) / ocorrências antes, quando há cobertura e período suficiente.

| Antes | Depois | Resultado | Redução |
| --- | --- | --- | --- |
| 4 | 1 | MELHORIA | 75,00% |
| 3 | 3 | ESTABILIDADE | 0,00% |
| 2 | 3 | PIORA | -50,00% |
| 3 | 0 | MELHORIA | 100,00% |
| 3 | 2 | MELHORIA | 33,33% |

Também foram verificados:
- zero ocorrências posteriores com registros ainda sem avaliação: resultado insuficiente, sem afirmar melhoria;
- nenhum registro versus registros avaliados sem erros: situações distintas;
- ausência de ocorrências anteriores: percentual nulo, sem divisão por zero;
- janela posterior ainda aberta: sem comparação definitiva;
- limites temporais lidos do banco, sem duplicar o instante de conclusão;
- temas sem mapeamento preciso no checklist: cobertura não confirmada, sem inventar ocorrência;
- conclusão de outra empresa ou de outra pessoa para FUNCIONARIO: acesso negado.

A implementação atual confirma individualmente FERRAMENTA_NAO_AUTORIZADA e VIOLACAO_POLITICA_INTERNA. Outros temas exigem uma fonte que identifique esse erro com precisão. A cobertura se restringe aos usos cadastrados e avaliados no HestIA; não mede todo uso externo. Os testes não afirmam causalidade entre capacitação e mudança de comportamento.

Os cenários usam fixtures exclusivamente na base de testes. Não são resultados reais da Qsoft nem comprovam a efetividade de treinamentos reais. O catálogo operacional de microtreinamentos existente ainda é o catálogo definido no código.

## Banco e compatibilidade
A V19 acrescenta os três metadados de retirada e uma restrição de consistência, sem reescrever retiradas legadas. Ela acompanha a V18 das SCRUM-372/373 na entrega consolidada.

Os testes de migração foram ajustados para verificar migrações pendentes e versão final sem uma contagem rígida que quebre ao incluir novas migrações. Uma prova de atualização preserva uma resposta legada retirada sem atribuir autor, data ou motivo inventados.

Nenhuma migração foi aplicada à base operacional durante os testes; os cenários PostgreSQL usam schemas temporários da base de verificação. O endpoint DELETE passou a exigir justificativa; clientes da API devem enviar o corpo mostrado acima.

A develop 5fd8de5 trouxe a tela /curadoria das SCRUM-514/515/516/517. Ela continua explicitamente demonstrativa, com armazenamento local do navegador. Seus botões ainda não representam uma retirada persistida na API; a validação desta entrega é da API real e do banco. A conexão dessa tela com a API é uma integração posterior.

## Como conferir
1. No GitHub Desktop, use a branch features/SCRUM-372-373-503-529-534-validacoes na pasta HestIA habitual.
2. Revise os arquivos, faça um único commit e abra um PR para develop, incluindo as cinco SCRUMs.
3. Ao iniciar a versão integrada, deixe o Flyway aplicar V18 e V19 em sequência.
4. Com uma conta autorizada, cadastre uma resposta, autorize o perfil de quem vai reutilizá-la e consulte /{id}/revisao.
5. Altere o texto e confira as versões preservadas.
6. Reutilize uma vez e confira contagem, economia e recompensa.
7. Retire com justificativa e confira a autoria/data no endpoint de revisão.
8. Repita busca, consulta elegível e reutilização: o conteúdo deve estar indisponível e as contagens não devem aumentar.
9. Para recorrência, consulte /microtreinamentos/conclusoes/{id}/efetividade?tipoErro=FERRAMENTA_NAO_AUTORIZADA com uma conclusão e dados reais autorizados; falta de dados deve produzir insuficiência, não uma melhoria fictícia.

Durante os testes HTTP, a auditoria geral usa um substituto somente no teste porque grava em uma transação separada e não enxerga as contas ainda não commitadas das fixtures. Autenticação, autorização, serviços, metadados da retirada, versões, economia, gamificação, SQL e pgvector são exercitados de fato. A auditoria de produção não foi alterada.

## Evidências finais — 21/10/2026
- Suíte consolidada com V18 e V19, PostgreSQL e pgvector: 236 testes encontrados, 234 aprovados, 2 condicionais ignorados, nenhuma falha ou erro.
- Inclui os 19 cenários novos desta entrega e os testes das 372/373. A prova específica da V18 foi fixada nessa versão; a prova de retirada valida a sequência mais recente.
- Frontend: 4 testes aprovados, sem falhas; sintaxe de server.js válida.
- git diff --check aprovado.
- Branch selecionada na pasta principal: features/SCRUM-372-373-503-529-534-validacoes, criada da develop 5fd8de5. Nenhum commit, push ou PR automático.
- O checkout temporário de validação foi removido após verificar cópia e backup. Não é necessário adicionar outra pasta ao GitHub Desktop.
- Backup das alterações anteriores à consolidação: C:/Users/luccas.toffano/Documents/Codex/2026-10-13/cha/backup-consolidacao-scrums.
