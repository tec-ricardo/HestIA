# SCRUM-427 a SCRUM-430 — Confiabilidade dos indicadores

## Referência e entrega
História [PO-133](https://luccastoffano.atlassian.net/browse/PO-133), US97:
- SCRUM-427: critérios mensuráveis e classificação versionada.
- SCRUM-428: confiança, cobertura e justificativa pela API.
- SCRUM-429: avisos junto aos resultados na tela de maturidade.
- SCRUM-430: testes de dados completos, parciais, ausentes, acesso e interface.

A implementação trabalha com o produto existente. Não inventa fórmulas de maturidade, empresas, funcionários ou notas. Usa a composição oficial já publicada por ComposicaoMaturidadeService, sem alterar seus oito pesos.

## O que já pode ser calculado
Uso Responsável utiliza registros de uso e avaliações operacionais com fundamentos preservados pela implementação das SCRUM-398/399. A nota descritiva é 100 × avaliações CONFORME válidas / avaliações conclusivas válidas. Resultados REQUER_REVISAO não entram no denominador dessa nota e reduzem a completude. Avaliações legadas sem revisão/fundamentos não são promovidas a evidências completas.

As outras sete dimensões retornam valor nulo e FONTE_PENDENTE: a develop ainda não oferece calculadoras e fontes validadas dessas notas. Elas aparecem individualmente na tela. A base de staging e seus funcionários anônimos não são associados a contas operacionais. Esta entrega não substitui as tarefas de cálculo de maturidade do Ricardo.

## Regra CONFIANCA-1.0
Convenção inicial de produto implementada para Uso Responsável, baseada nos critérios propostos na especificação de métricas preparada para o grupo:
- Universo observado: registros distintos do escopo e período.
- Cobertura: registros com avaliação e fundamentos válidos / universo observado.
- Completude: avaliações válidas conclusivas / avaliações válidas.
- Amostra: registros distintos com avaliação válida conclusiva.
- CONFIAVEL: nota medida, cobertura e completude de pelo menos 95% e amostra de pelo menos 30.
- PARCIAL: nota medida disponível, mas algum limite não atendido.
- ESTIMADO: a regra também suporta valores explicitamente estimados; nunca os promove a medição definitiva. Nenhuma nota estimada é inventada pelo endpoint atual.
- SEM_DADOS é uma situação separada da classificação: nota nula, motivo explícito e cobertura nula se o universo for vazio. A interface mostra “Sem dados”, não uma nota estimada.
- FONTE_PENDENTE: nota e percentuais nulos. Os critérios de dimensões futuras serão definidos com suas calculadoras; não se impõe 30 observações a universos pequenos como controles de segurança.

Os limites são convenções versionadas, não garantia estatística e não aprovação formal da professora. Contagens são comparadas exatamente, sem promoção causada pelo arredondamento dos percentuais exibidos. Alterar os limites exige nova versão da regra e testes; o cliente pode solicitar uma versão conhecida pelo parâmetro versao. Versões desconhecidas retornam 404.

A API recalcula a cada GET, em uma transação de leitura consistente, e responde Cache-Control: no-store. Dados atualizados entram na consulta seguinte. O resultado informa período, data de apuração, versão da confiança e versão da composição. Não há persistência de snapshots de indicadores nesta task.

## Índice completo e parcial
scoreGlobal só existe se todos os pesos forem cobertos por dimensões CONFIAVEL.
scoreParcial usa apenas dimensões elegíveis, renormalizando seus pesos; informa coberturaPesos e aparece como exploratório, sem comparação com o índice completo.
Sem dimensão elegível, ambos ficam nulos. Uma nota zero efetivamente calculada continua zero. Falta de dados não significa imaturidade.
Na versão atual, o índice global permanece nulo porque sete calculadoras estão pendentes.

## API e autorização
GET /indicadores/confiabilidade?inicio=2026-01-01&fim=2026-01-31&visao=PESSOAL

Exige a sessão Bearer existente. Parâmetros:
- inicio e fim obrigatórios e inclusivos; timestamps filtrados por início inclusivo e início do dia seguinte ao fim exclusivo.
- visao=PESSOAL: qualquer perfil autenticado, somente seus registros.
- visao=GERENCIAL: GESTOR somente seu departamento; ADMIN toda a empresa.
- versao opcional, padrão CONFIANCA-1.0.
- Períodos invertidos, futuros ou maiores que dez anos são rejeitados com 400.

Empresa, pessoa e departamento efetivos vêm da sessão. Não há parâmetros que permitam escolher outra empresa/pessoa. FUNCIONARIO não pode usar GERENCIAL. Gestor sem departamento válido é bloqueado. A consulta agrega depois dos filtros e valida que ferramenta, política e revisão pertencem à mesma empresa; não publica detalhes pessoais de outras pessoas.

Resposta por indicador:
codigo, nome, peso, valor, unidade, fonte, formula, numerador, denominador e confianca:
classificacao, situacao, versaoRegra, cobertura, completude, amostra, universoObservado,
amostraMinima, limiteCobertura e motivos.

A confiança se refere somente aos registros cadastrados; não comprova cobertura de usos externos. Timestamps seguem a semântica local existente, sem inventar um fuso para a base.

## Frontend
A rota /maturidade foi conectada à nova API e deixou de apresentar notas ou departamentos fictícios como resultados nessa tela.
- Seleção de período e visão pessoal/gerencial.
- Classificação, nota ou ausência, cobertura, completude, amostra e motivos juntos de cada indicador.
- Índice completo e parcial separados; limitações e versões visíveis.
- Falha de rede, acesso negado e sessão expirada não acionam fallback numérico.
- HTML escapa nomes e mensagens; token fica somente na sessão do servidor e não é enviado no HTML.
- Respostas pessoais não ficam em cache.

Como o login antigo só escolhia perfis pelo sufixo, foi acrescentada a autenticação mínima necessária para esta integração: e-mail real e senha são enviados a /auth/login; o token fica na sessão BFF, regenerada após login. /sair revoga a sessão da API quando possível e encerra a sessão local. Acessos @adm/@gestor/@colaborador continuam demonstrativos e não têm acesso a dados da API. O restante das telas antigas e seus fallbacks não foi refeito nesta entrega.

Uma conta real autorizada precisa existir no banco para a conferência manual. Não foram criadas contas operacionais de teste. Na ausência de conta, os testes automatizados exercitam o fluxo com fixtures isoladas.

## Conferência manual
1. Inicie PostgreSQL, backend Java e frontend conforme a configuração habitual.
2. Em /acesso, entre com uma conta real cadastrada.
3. Abra /maturidade e escolha um período em que existam registros.
4. Confira que Uso Responsável reflete as avaliações desse recorte e mostra os motivos de confiança.
5. Consulte um período vazio: nota nula e aviso, sem zero fictício.
6. Com GESTOR, compare PESSOAL e GERENCIAL; o segundo recorte é somente seu departamento.
7. Com FUNCIONARIO, tente GERENCIAL: deve ser negado.
8. Acrescente avaliações válidas e consulte de novo: cobertura, amostra e classificação são recalculadas.
9. Desligue a API: a tela deve indicar indisponibilidade, sem recuperar notas demonstrativas.

## Testes
- Regra: limites exatos, amostra mínima, dados ausentes, nota zero, parcial, estimativa e contagens inválidas.
- Integração H2/PostgreSQL: consulta real, recálculo, fundamentos legados, avaliações pendentes, isolamento de empresa/departamento/pessoa, período e versão.
- Controller: sessão obrigatória, datas/enum e contrato JSON sem cache.
- Frontend: renderização nos três perfis, nulo versus zero, parcial/estimado, escape de HTML, ausência de fallback e integração HTTP de login/consulta/logout com API de teste.
- O job frontend-checks do CI agora executa npm test além da verificação de sintaxe.

## Evidências em 21/10/2026
- Suíte Java inicial: 202 testes encontrados, 182 aprovados e 20 condicionais ignorados, sem falhas.
- Suíte Java final com PostgreSQL configurado e novos testes de controller: 205 encontrados, 203 aprovados e 2 condicionais ignorados, sem falhas ou erros.
- Frontend: 4 testes aprovados; verificações cobrem os três perfis e dados ausentes, parciais, estimados e zero medido. Sintaxe de server.js válida.
- git diff --check aprovado.
- Testes PostgreSQL em schemas temporários da base de verificação, com limpeza ao término. Não houve alteração na base operacional nem criação de usuários reais de teste.
- Branch: features/SCRUM-427-430-confiabilidade-indicadores, criada de origin/develop f94d802 (já inclui a SCRUM-371 do PR108). Sem commit, push ou PR automático.
