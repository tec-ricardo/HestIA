# Implementação experimental das métricas de maturidade

Data: 21/10/2026. Fonte: HestIA_Especificacao_Metricas_Maturidade.docx, fornecido pelo usuário.
Versão dos pesos: 1.0, preservada da develop. Versão do cálculo: proposta-0.1.

## Resultado e limites

O projeto original recebeu calculadoras das oito fórmulas, adaptadores de dados e a consulta separada `GET /indicadores/maturidade/resultados`. A tela de maturidade passou a consultar esse contrato com a sessão real do backend e exibe indisponibilidade, parcialidade, cobertura e motivos. Os pesos e o endpoint `/composicao` existentes foram preservados.

O documento se identifica como proposta sujeita à validação do grupo. A implementação mantém essa identificação e não a promove a metodologia aprovada. Não foram calculadas notas sobre o banco real nesta execução, nem importados dados do ZIP ou criadas credenciais sintéticas. O Docker estava desligado; não houve implantação.

## O que cada componente faz e influencia

| Arquivo ou módulo | Alteração | Influência |
|---|---|---|
| CalculadorasMaturidade | Fórmulas, denominadores, coberturas e critérios da proposta | Centraliza regras testáveis e distingue nota descritiva de elegibilidade global |
| FatorCalculado | Contrato com peso, nota, estado, elegibilidade, motivo, amostra, coorte, fonte, campos, estatísticas e natureza | Permite rastrear a origem e a limitação de cada parcela |
| SnapshotMaturidadeRepository | Leitura JSONB filtrada por snapshot/empresa, tabelas permitidas, joins de projeto e detecção de ambiguidades | Impede leitura de outros tenants e mistura de importações |
| AdaptadorSinteticoMaturidade | Quatro fatores descritivos do staging e produtividade com meta explícita | Consome identificadores anônimos sem transformá-los em usuários |
| AdaptadorOperacionalMaturidade | Uso Responsável com fundamentos e versão preservados | Mantém legadas fora da nota e separa avaliações pendentes de reprovação |
| ConfiguracaoMaturidade | Mapas autorizados de empresas/departamentos, metas e versão dos parâmetros no servidor | Evita assumir que C01 seja a empresa numérica 1 ou aceitar tenant livre do navegador |
| ResultadoMaturidadeService | Permissões, escopo, período, natureza, composição, parcial e arredondamento | Só publica global com oito fatores elegíveis e não mistura populações |
| ResultadoMaturidadeController | Novo endpoint de resultados | Preserva o contrato de composição anterior |
| MetodologiaMaturidadeController | Atualização das referências para a proposta recebida | A metodologia deixa de afirmar que todas as fórmulas são desconhecidas |
| ContextoAtualController | GET /api/contexto autenticado, sem senhas e com no-store | Fornece ao BFF a identidade e a empresa reais do usuário |
| api-contexto.js | Contexto assíncrono por requisição | Sessões simultâneas não compartilham token ou empresa |
| server.js | Login real, escopo por sessão, logout e consulta de maturidade sem fallback numérico | O protótipo deixa de tratar sufixos de e-mail como autenticação e de exibir números fictícios nessa tela |
| maturidade-resultados.ejs | Formulário de período/fonte e apresentação de resultados | Mostra score parcial como exploratório, cobertura e pendências |

Todos os caminhos Java da nova implementação de métricas estão em `src/main/java/br/com/hestia/indicadores/maturidade`, salvo os controllers de contexto/metodologia indicados. O frontend permanece em sua pasta original.

## Implementação por fator

| Fator | Fórmula e evidência | Situação da consulta |
|---|---|---|
| AI Literacy | Média por pessoa e média entre pessoas com conclusões válidas de IA Generativa | Implementada para staging; quantidade de cursos não altera o peso da pessoa |
| Governança | Requests aprovados / requests classificados × 100, com listas canônicas de departamentos | Implementada como descrição do cadastro; nunca elegível global sem histórico temporal comprovado |
| Uso Responsável | C / (C + N) × 100; R reduz cobertura; legadas separadas | Implementada na fonte operacional; na sintética retorna SEM_DADOS |
| Produtividade | Mediana das durações de issues de um tipo; min(100, 100 × meta / mediana) | Implementada no staging quando tipo/meta/versão estão configurados; sem meta devolve mediana e CONFIGURACAO_PENDENTE |
| FinOps | limitar(100 × (2 − custo/orçamento), 0, 100) | Fórmula testada; adaptador retorna CONFIGURACAO_PENDENTE enquanto moeda/completude não forem implementadas e autorizadas |
| Segurança | Controles aprovados / avaliados × 100; universo completo para o global | Fórmula testada; adaptador retorna CONFIGURACAO_PENDENTE por ausência de catálogo/evidências |
| Colaboração | PRs integrados com review_count ≥ 1 / PRs integrados válidos × 100 | Implementada no staging; não comprova revisão independente |
| Competências | Pessoas qualificadas / pessoas com resultado determinável × 100 | Implementada no staging; aprovação válida prevalece sobre outra conclusão indeterminada; não concede certificado ou XP |

A adaptação de score 80 e os limites 95%/30 observações são identificados como convenções propostas. Governança usa linhas de uso como amostra e requests apenas como peso. Segurança e FinOps exigem completude do universo, sem mínimo de 30. As calculadoras puras de FinOps/Segurança não significam que existem fontes reais habilitadas para esses fatores.

## Cobertura e composição

`scoreGlobal` é nulo enquanto faltar elegibilidade em qualquer fator. `coberturaPesos` soma apenas os pesos elegíveis. `scoreParcial` divide a contribuição desses fatores pela cobertura e permanece nulo quando a cobertura é zero. Ele é exploratório, não comparável ao índice completo.

As notas e scores são arredondados na saída em duas casas com HALF_UP. Razões de cobertura e estatísticas brutas preservam precisão decimal no JSON para rastreabilidade e decisão dos limites; a tela apresenta as porcentagens com duas casas. Uma razão desconhecida permanece nula, não zero. Essa preservação das razões brutas é uma escolha técnica explícita para não converter, por exemplo, 66,666...% em 67% antes de apresentar a porcentagem.

O score da proposta usa elegibilidade. O serviço anterior `CalculoMaturidadeService` permanece como helper aritmético de consolidação de notas já fornecidas, sem fontes; ele não é utilizado pela nova rota de resultados e não deve ser tratado como publicação de score elegível.

## Acesso e identidade

O novo endpoint de resultados exige ADMIN ou GESTOR. O GESTOR fica restrito ao seu departamento autenticado e ao mapeamento de origem autorizado. ADMIN pode consultar a empresa e seus departamentos. A permissão do endpoint de composição anterior não foi restringida.

O usuário não escolhe a empresa por parâmetro. O servidor deriva o ID da sessão e consulta a configuração de vínculo de origem. Um mapeamento ausente retorna 409, sem tentar adivinhar C01. O departamento de origem informado pelo ADMIN é validado contra o snapshot da empresa; para GESTOR o mapeamento é obrigatório e não pode ser substituído pela entrada.

O login do frontend agora usa `/auth/login` e `/api/contexto`, renova o identificador de sessão e guarda o token no servidor. Contas demonstrativas baseadas em sufixos @adm/@gestor/@colaborador deixam de autenticar sem conta válida no backend. Não foram criadas contas ou senhas para substituir esses exemplos. A rota de saída solicita revogação do token e encerra a sessão local mesmo se o backend estiver indisponível.

## Configuração antes de consultar dados reais

As propriedades ficam no namespace `hestia.maturidade`. Nenhum vínculo ou meta foi gravado automaticamente em application.properties.

- `empresas-origem`: mapa de ID operacional autorizado para código da empresa de origem.
- `departamentos-origem`: mapa de ID operacional autorizado para código do departamento de origem.
- `versao-parametros`: identificação aprovada para o conjunto de parâmetros. Sem essa versão, uma meta eventualmente fornecida não é aplicada.
- `metas-dias`: mapa com chave `ID_EMPRESA:DEPARTAMENTO_ORIGEM:TIPO_ISSUE`; para toda a empresa, o departamento é `*`. Os valores precisam ser metas positivas aprovadas. O valor 5 do documento é apenas exemplo de teste.

Exemplo de estrutura, com marcadores que precisam ser substituídos por configurações autorizadas:

```properties
hestia.maturidade.empresas-origem[ID_EMPRESA_AUTORIZADO]=CODIGO_ORIGEM
hestia.maturidade.departamentos-origem[ID_DEPARTAMENTO_AUTORIZADO]=DEPARTAMENTO_ORIGEM
hestia.maturidade.versao-parametros=VERSAO_VALIDADA_PELO_GRUPO
hestia.maturidade.metas-dias[ID_EMPRESA_AUTORIZADO:*:Bug]=META_DIAS_APROVADA
```

O exemplo não é um arquivo pronto para executar. Não substitua os marcadores por IDs inferidos de nome, total de empregados ou posição de linhas.

## API e semântica temporal

`GET /indicadores/maturidade/resultados` recebe início/fim obrigatórios, fonte SINTETICA ou OPERACIONAL, snapshot SHA-256 para a fonte sintética e filtros autorizados de departamento/tipo de issue.

O retorno contém empresa/escopo efetivos, período, instante da apuração, hash do snapshot quando houver, natureza, versões, parâmetros, fatores, avisos e os três campos de composição. O período usa início do primeiro dia inclusivo e início do dia posterior ao fim exclusivo. Timestamps sintéticos permanecem sem fuso declarado; não são reinterpretados como UTC. A resposta operacional não aceita identidade/snapshot sintético e não faz conversão entre populações.

O hash deve corresponder exatamente a uma importação existente. Consultar um período sem observações produz ausência de nota, não uma nota zero. Colunas/enums desconhecidos não recebem equivalência aproximada silenciosa. Fatos de issues/PRs são atribuídos pela tabela projects do mesmo hash e empresa; ambiguidades de projeto impedem o cálculo afetado.

## Testes e limitações de validação

O motor foi testado com os exemplos do documento: médias por pessoa 60/100 → 80; governança 80/100 → 80; conformidade 8/10 → 80 e 10/15 de conclusão; meta 5/mediana 10 → 50; orçamento 1000/custo 1500 → 50; controles 4/4 de 5 → nota 100 e cobertura 80%; colaboração 6/10 → 60; competências 7/10 → 70.

Também foram testados ausência, limites, zeros observados, duplicidades, datas, tipo de issue, revisão nula, configuração ausente, snapshots parametrizados, joins por empresa, escopo de gestor, mistura de fontes e sessões concorrentes. Os testes de cálculos/operacional usam fixtures locais e H2; os do leitor JSONB validam consultas e parâmetros por mock JDBC.

O PostgreSQL/JSONB real foi validado em um contêiner separado com PostgreSQL 17.11 e PGVector, usando somente fixtures de teste. Há leitura em streaming de fatos sintéticos filtrados, com agregação incremental em Java; a otimização adicional de pré-agregação SQL das calculadoras sintéticas, sugerida no documento, ainda não foi feita. A fonte operacional já agrega contagens no banco. A base real e o dataset enviado pelo colega não foram usados nesta validação.

Não foi criado um fluxo de publicação persistida de apurações: a rota é somente de consulta. Os metadados completos são devolvidos, e a auditoria HTTP existente permanece ativa, mas histórico imutável de resultados publicados e gestão de versões de parâmetros continuam como evolução necessária. As fontes de controles, moedas e histórico temporal de ferramentas também permanecem pendentes. A implementação atual não deve ser apresentada como cumprimento integral de todos os itens da sprint.

## Evidências desta execução

### Correção da revisão para develop — 21/10/2026

Em `AdaptadorOperacionalMaturidade`, a nota de uso responsável e a cobertura de avaliações agora compartilham o mesmo filtro: registro de uso e avaliação precisam estar no período solicitado. O total de registros mantém os usos do período, inclusive os ainda não avaliados, para que a cobertura continue sinalizando essas ausências. Antes, uma avaliação no período referente a um uso antigo podia alterar a nota sem integrar o universo da cobertura.

O teste `notaECoberturaUsamMesmoPeriodoComLimitesInclusivoEExclusivo` cobre usos anteriores ao início, usos e avaliações no início do dia seguinte ao fim, avaliações legadas de usos fora do período e os limites válidos. Confere nota 50, dois resultados preservados para três usos do período e cobertura de avaliação 2/3.

A branch de implementação foi atualizada por fast-forward até `041c621`, incorporando os quatro commits novos da develop. As alterações locais foram preservadas. Nenhum merge foi enviado ao remoto.

### Validação PostgreSQL isolada

O teste `MaturidadePostgresTest` valida as migrações reais, a atualização V17 → V18, a repetição sem migrações adicionais e as consultas JSONB por empresa, snapshot e projeto. Inclui projeto ambíguo entre empresas e a regressão do período de uso responsável usando as tabelas completas criadas pelo Flyway.

Para executá-lo, use exclusivamente um banco descartável chamado `hestia_validacao` e defina `HESTIA_MATURIDADE_PG_TEST_URL`, `HESTIA_PG_TEST_USER` e `HESTIA_PG_TEST_PASSWORD`. A classe verifica o nome do banco antes de migrar. Ela usa um schema novo por execução, removido ao final; os dados de cada cenário são revertidos. O staging fixo é criado pela migração V13 no banco descartável. Nunca aponte a URL para a base do projeto. Os testes existentes de PGVector usam `HESTIA_PG_TEST_URL`.

A execução real identificou uma expectativa antiga em `BuscaVetorialPostgresTest`: o teste ainda esperava 16 migrações. Durante a validação, a develop avançou para `c1989f8`, com a V17 de tratamentos de não conformidade. A branch foi atualizada e a migração local de trilhas, ainda não publicada/aplicada na base do projeto, passou a V18. Os testes de migração agora esperam 18 versões; nenhuma migração publicada foi renumerada. A inicialização da aplicação é verificada com `ddl-auto=validate`, Flyway habilitado e conexão ao PostgreSQL isolado.

Após atualizar a branch até `c1989f8` e compatibilizar a V18: a suíte Java completa terminou com BUILD SUCCESS, 236 testes contabilizados, 234 aprovados, 2 ignorados, 0 falhas e 0 erros. A execução usa PostgreSQL/PGVector real para os testes condicionais de banco. Os dois testes ainda ignorados são do serviço de alertas e exigem `ALERT_E2E=true`; não fazem parte desta validação de banco. Frontend: 4 testes passaram na execução anterior; não houve alteração no frontend nesta etapa.

A validação usa contêineres exclusivos (`hestia-validacao-sprint-20261021` na primeira passagem e `hestia-validacao-sprint-20261021-v18` na passagem final), sem compartilhar o volume nem a porta da base do projeto. Eles são parados ao término. A validação técnica está concluída para os cenários acima; a homologação das regras de negócio da proposta 0.1 e as fontes ainda pendentes continuam necessárias conforme descrito neste documento. Nenhum commit, PR ou merge foi enviado nesta etapa.
