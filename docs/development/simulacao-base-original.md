# Demonstracao calculada a partir da base original

Fonte: `database/ai_usage_synthetic.db`, SHA-256
`11d10be9a03f6ce73653523dd7dbe8bdac42c9e65435a1a865250cffe4d20ab3`.
As 17 tabelas somam 311.556 linhas e 18 empresas. O recorte de demonstracao e
C01, NexoCode Solucoes, com 11 departamentos e 363 pessoas no extrato.
Nao representa dados reais da QSoft nem dados do tenant autenticado.

## Substituicoes

`/maturidade/demonstracao` e `/custos` deixam de usar notas e valores fixos
escolhidos para o wireframe. A tela consulta o snapshot calculado da origem,
sem combinar respostas parciais da API operacional com demonstracao.
Os demais fluxos operacionais e as contas de usuario nao sao alterados.
Mocks em trechos antigos inalcancaveis nao sao fontes deste snapshot.

## Reproduzir

Na raiz do repositorio, com Node 22.13+ (validado em 24.19):

```powershell
node scripts/calcular-base-original.cjs CAMINHO/ai_usage_synthetic.db Front-End/hestia-prototipo-sprint1-atualizado/dados/base-original-c01.json --cambio=5 --moeda-orcamento=BRL --meta-dias=5
npm test --prefix Front-End/hestia-prototipo-sprint1-atualizado
```

O script abre SQLite em modo somente leitura, sem alterar o original ou o banco
operacional. Empresa, inicio e fim aceitam opcoes `--empresa=C01`,
`--inicio=2025-06-01` e `--fim=2026-08-10`. Regenerar o snapshot requer reiniciar
o BFF. O JSON traz o hash e as contagens das tabelas para rastreabilidade.

Sem as tres opcoes de premissas, nao ha cambio, nota de produtividade ou
simulacao financeira. Os valores 5 BRL/USD e 5 dias sao hipoteses didaticas
escolhidas para este cenario, nao cotacao vigente, meta aprovada ou medida
obtida na base. Podem ser alterados sem editar o codigo.

## Resultado do recorte

Periodo de uso: 01/06/2025 a 10/08/2026. Meses financeiros completos: junho/2025
a julho/2026; nunca ratear fatura mensal em dias sem uma premissa especifica.

| Medida | Resultado |
|---|---:|
| Eventos de uso | 99.604 |
| Requisicoes agregadas | 3.922.913 |
| Tokens de entrada | 1.254.471.321 |
| Tokens de saida | 1.073.493.700 |
| Sucesso tecnico por evento | 94,96% |
| Conclusoes de treinamento validas | 214 |
| Eventos de seguranca | 4 |
| Faturas observadas | 1.272 |
| Custos em BRL, sem conversao | 1.219.396,34 |
| Custos em USD, sem conversao | 2.552,35 |
| AI Literacy, descritiva | 76,87 |
| Governanca, descritiva | 97,21 |
| Colaboracao, descritiva | 83,43 |
| Competencias, descritiva | 31,85 |

AI Literacy da fonte sintetica segue a media das medias por pessoa das
conclusoes validas de IA Generativa. Nao aplicar a selecao de tentativas
versionadas do LMS operacional em tabelas que nao contem essa identidade.
Governanca preserva datas, vinculos e listas canonicas de departamentos; o
cadastro nao comprova historico de revogacoes. Competencias segue a coorte
e o tratamento de estados do AdaptadorSinteticoMaturidade.

Produtividade usa duracao de issues fechadas, separada por tipo. A nota com
meta didatica e `min(100,100*meta/medianaDias)`; o resumo combina as notas
por tipo ponderadas pelo numero de issues e e uma convencao de demonstracao,
nao uma nova formula aprovada para o indice operacional.

FinOps mostra os valores originais por moeda e, separadamente, a simulacao
com cambio explicito e hipotese de orcamento em BRL. Faturas ausentes,
centros desconhecidos e centros ambiguos nao autorizam afirmar completude
ou economia. No departamento, centros compartilhados sao excluidos, sem
rateio arbitrario. Diferencas residuais devem ser tratadas como nao alocadas.

## Ausencias preservadas

Uso Responsavel nao e sucesso tecnico; faltam avaliacoes fundamentadas.
Seguranca nao e ausencia de incidentes; faltam evidencias de controles.
Score global e parcial permanecem nulos nesta demonstracao: nenhuma media
de notas descritivas e promovida a indice elegivel. Nao sao gerados risco,
ranking global, economia por RAG, prompts, certificados ou XP a partir de
campos que a base nao possui.

## Validacao

### Aplicacao na copia em uso - 17/11/2026

Integracao aplicada na copia `OneDrive - ESPM/Projeto4/ProjetoGit/HestIA`,
preservando as alteracoes anteriores do usuario e mantendo copia dos arquivos
substituidos. O frontend atualizado foi observado em localhost:3000/custos e
localhost:3300/custos. Custos, filtros mensais e simulacao financeira foram
conferidos no navegador. O backend na porta 18080 e seu PostgreSQL nao foram
alterados. A leitura da origem usa SQLite somente leitura em dados-locais,
com hash conferido e cache invalidado por mudanca no arquivo, sem importar
funcionarios anonimos como contas ou registros autenticados. O arquivo local
do banco foi excluido do versionamento.

O mapa de risco agora oferece historico sintetico explicito dos 11 departamentos
e preserva a consulta operacional por fonte=OPERACIONAL. As contagens historicas
nao concedem classificacao de risco. O arquivo da base nao contem os controles
e fundamentos necessarios para essa classificacao. A rota mensal preserva notas
ausentes onde ainda nao existe recalculo daquele fator.

### Conferencia do Canvas e dashboard de custos - 17/11/2026

Materiais lidos no Canvas autenticado:

- Projeto Integrado IV: `ProjetoIntegradoIV.html`, arquivo 7358698,
  https://canvas.espm.br/courses/53394/files/7358698 . Mostra custo mensal,
  evolucao do consumo, orcamento e distribuicao por area. O proprio exemplo
  identifica seus numeros como ficticios para demonstracao academica.
- Estudo de caso v2, arquivo 7447489,
  https://canvas.espm.br/courses/53394/files/7447489 . A interface e uma ideia,
  nao uma restricao; requer indicadores e componentes integrados.
- Modelagem: Exercicio_A4_Gabarito_Tecnico_V5_3_2_v1.docx, arquivo 7670992,
  https://canvas.espm.br/courses/53575/files/7670992 . Aplicacao de referencia
  de gamificacao com Spring Boot, PostgreSQL, RAG, ML, RabbitMQ e avaliacao.
  Esse material nao fornece formulas financeiras para o HestIA.

O dashboard original `custos.ejs` tinha um segundo array fixo no navegador,
independente do mock no BFF. Ambos deixaram de alimentar a rota `/custos`.
O redirecionamento provisoriamente preparado foi substituido pela renderizacao
do proprio dashboard, preservando menu, estilos, cards, graficos e tabela.
Filtros de mes, departamento e moeda recalculam todos os agregados.
BRL e USD permanecem separados por padrao. O modo de simulacao usa as premissas
explicitas para mostrar a comparacao com orcamento. Faturas sem departamento
unico permanecem no total, em categoria nao atribuida; nao sao rateadas.
Foram removidas a eficiencia ficticia e sua media de produtividade/conformidade;
sucesso tecnico e apresentado com seu nome e denominador corretos.

Conferencia automatizada adicional: conciliacao por moeda, mes, departamento e
ferramenta, filtros invalidos, ausencia de fatura e renderizacao da tela.
O total atual e de 74 testes, incluindo a verificacao do SQLite original quando
`HESTIA_ORIGINAL_DB` aponta para a fonte. A base e o ambiente operacional nao
foram modificados. A publicacao remota continua pendente.

74 testes do frontend aprovados, incluindo cinco testes da demonstracao.
Reconciliacao entre meses/departamentos e total; isolamento de C01;
aritmetica da conversao; notas ausentes; controle de perfil e medianas.
As contagens, tokens e somas foram tambem comparados diretamente ao SQLite
original. Template renderizado sem erro. O hash da origem foi preservado.
Nao houve publicacao ou alteracao da base operacional.

## Revisao para commit - 18/11/2026

Progresso por faixa do nivel: 165 XP representam 55% ate 300 XP.
Custos: filtros invalidos retornam HTTP 400, falhas internas HTTP 503 sem detalhes locais.
Testes adicionais cobrem limites de nivel e erros de consulta.
