# SCRUM-594 — Fontes, cálculos e resultados da versão final

Revisão parcial da Sprint 4. As evidências abaixo correspondem às mudanças integradas à develop. Os resultados numéricos identificam o cenário de teste ou a análise sintética em que foram obtidos.

| Área | Fonte e cálculo | Resultado e limite |
| --- | --- | --- |
| Competências | Catálogo persistido, versões de avaliações, conclusões, leituras e tentativas finalizadas. Coorte de usuários ativos por empresa ou departamento. Nota = 100 × pessoas qualificadas / pessoas com resultado determinável. Aprovação válida a partir de 80 prevalece sobre reprovação. | Cenário com uma pessoa avaliada: reprovação gera 0/1 = 0; aprovação posterior gera 1/1 = 100. Pessoas sem resultado reduzem a cobertura de classificação. Ausência de denominador deixa a nota indisponível. [PR #147](https://github.com/tec-ricardo/HestIA/pull/147) |
| Efetividade | Ocorrências confirmáveis pelas avaliações de conformidade dos registros de uso. A conclusão identifica o participante e a capacitação. Com cobertura confirmada, janela posterior completa e ocorrências anteriores, redução = 100 × (antes − depois) / antes. | Exemplo da fórmula: 5 ocorrências antes e 3 depois resultam em 40% de redução. A integração preserva a consulta após a retirada do treinamento e restringe o acesso à empresa. O resultado depende da cobertura das avaliações e não estabelece causalidade. [PR #146](https://github.com/tec-ricardo/HestIA/pull/146) |
| Economia monetária | Depende de preços e moeda autorizados para converter consumo evitado em valor monetário. | A estimativa monetária permanece indisponível enquanto faltar essa configuração. [PR #141](https://github.com/tec-ricardo/HestIA/pull/141) |
| Estatística Multivariada | Base sintética de 18 empresas. Regressão de requisições por tamanho e setor. | R² = 0,4256; R² ajustado = 0,1123; p global = 0,3117. |
| Machine Learning | Base sintética, com 14 empresas no treino e quatro no teste. | Regressão linear selecionada; MAE de teste 536.755,45. A referência pela média do treino teve MAE de 534.944,25. |
| Análise Preditiva | Série diária sintética da empresa C01. Suavização exponencial simples selecionada por validação temporal. | MAE de teste 3,3989 falhas/dia. Projeção de 195,7553 falhas em 14 dias, sem observações futuras para confronto. |

## Reprodução

- Competências e vínculo com efetividade: executar o cenário abaixo na raiz do HestIA. Ele usa dados de teste e exercita o catálogo persistido, a avaliação, a trilha e a consulta após retirada.
- Análises acadêmicas: usar os arquivos, versões e procedimentos descritos na [SCRUM-593](SCRUM-593-consolidacao-analises.md).

```powershell
.\mvnw.cmd "-Dtest=AprendizagemSprintIntegrationTest#integraConclusaoTrilhaEEfetividadeComCatalogoPersistido" test
```

## Colaboração na demonstração

A fonte é a coleta congelada de PRs do HestIA, de 03 a 12/11/2026, com fuso America/Sao_Paulo. Os vínculos de quatro autores com empresa e departamento são didáticos e exclusivos da empresa GitHub demo. Os eventos ficam separados da apuração da QSoft.

A API retornou 25 PRs válidos, zero aprovações formais elegíveis, nota 0, cobertura de 100% e amostra frágil (mínimo de 30 PRs). A nota é o percentual de PRs válidos com aprovação elegível antes do merge. O recorte termina na coleta e não mede toda a colaboração da equipe. A reprodução com o banco da demonstração está no [manual da fonte](COLABORACAO-GITHUB-DEMO.md). [PR #144](https://github.com/tec-ricardo/HestIA/pull/144)

## RAG na demonstração

| Verificação | Resultado registrado | Evidência |
| --- | --- | --- |
| Busca semântica e confirmação de reutilização | OpenAI, text-embedding-3-small (1536 dimensões), PostgreSQL/pgvector e política didática v1.1. Após reindexar título e corpo e aplicar limiar 0,40 no perfil demo, a busca recuperou os cinco casos pertinentes e rejeitou os três negativos. A busca sem confirmação não incrementou contadores; a confirmação explícita seguida de reenvio incrementou uma vez. | [Roteiro e limites](SCRUM-545-validacao-busca-real.md), [corpus](SCRUM-545-corpus.json), [resultados](SCRUM-545-evidencias-finais.json) |
| Continuidade e acesso | O histórico é associado à conversa e restrito ao autor. A validação verificou retomada do assunto, isolamento entre conversas e usuários e exclusão de fontes revogadas da interpretação seguinte. A geração não incrementou reutilizações ou créditos. | [Roteiro](SCRUM-546-continuidade-conversas.md), [resultados](SCRUM-546-evidencias-reais.json) |
| Orçamento de contexto | A seleção preserva turnos recentes elegíveis e pode resumir trechos antigos de forma extrativa e parcial. Os limites são medidos em caracteres JSON UTF-16. A consulta de contexto e os ensaios de revogação são descritos nas evidências. | [Complemento técnico](SCRUM-612-complemento-ricardo.md), [validação](evidencias/SCRUM-612-validacao.json), [verificações da integração](evidencias/correcoes-validacao.json) |

A política utilizada é fictícia. Os oito casos sustentam a calibração da demo nesse corpus e provedor. A configuração padrão fora do perfil demo permanece em 0,70. Para reproduzir o ensaio, use o corpus e o roteiro da SCRUM-545 e reindexe a fonte existente no banco demo antes de repetir as perguntas.

## Maturidade operacional e comparação entre períodos

No teste de integração da SCRUM-578, 30 pessoas do mesmo departamento e período forneceram evidências para quatro fatores elegíveis: Governança (100), AI Literacy (100), Uso Responsável (80) e Competências (100). O score parcial ponderado foi 94,55, com cobertura de 55% dos pesos. O score global ficou indisponível. FinOps, Produtividade, Segurança e Colaboração ficaram sem contribuição operacional nesse cenário. A coleta GitHub demo permanece separada. [PR #151](https://github.com/tec-ricardo/HestIA/pull/151)

A comparação entre apurações exige períodos válidos, sem sobreposição e de mesma duração, além de escopo, origem, parâmetros, versões e convenção temporal compatíveis. Convenção ausente ou divergente bloqueia o cálculo das variações. A cobertura pode variar entre períodos, e a variação global exige score global nas duas apurações. [PR #142](https://github.com/tec-ricardo/HestIA/pull/142)

```powershell
.\mvnw.cmd "-Dtest=AdaptadorOperacionalMaturidadeTest#compoeQuatroFatoresOperacionaisNoMesmoEscopo,ApuracaoMaturidadeServiceTest" test
```

## Risco departamental

O índice é 100 menos a aderência ponderada de Governança (30%), Uso Responsável (30%) e Segurança (40%). A faixa é baixa abaixo de 25, média de 25 a menos de 50 e alta a partir de 50. Evidência insuficiente pode gerar faixa indicativa ou ausência de classificação, conforme os fatores disponíveis. O teste de investigação usa dados simulados e verifica índice 12, faixa baixa e evidências suficientes dos três fatores. [Regra de risco](RISCO-DEPARTAMENTAL-1.0.md) · [Ranking](https://github.com/tec-ricardo/HestIA/pull/130) · [Investigação](https://github.com/tec-ricardo/HestIA/pull/152)

## Limites da versão avaliada

A demonstração de Produtividade usa simulação identificada. A nota operacional depende de fonte e meta aprovadas. FinOps depende de fonte, moeda e orçamento aprovados. A homologação de Segurança depende de evidências dos controles na versão e no ambiente avaliados. A reprodução das análises acadêmicas está registrada na SCRUM-593. A evidência das telas e da jornada da Entrega 3 segue as rubricas aplicáveis.
