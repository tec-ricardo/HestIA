# SCRUM-593 — Consolidação das análises da Entrega 2

Data da conferência: 11/11/2026. Bases sintéticas; os resultados não representam operação real.

| Disciplina | Artefatos da Entrega 2 | Conferência |
| --- | --- | --- |
| Estatística Multivariada | Estatistica_Multivariada_D2.Rmd, indicadores_empresas.csv, Relatorio_Estatistica_D2.pdf | R Markdown renderizado com a base de 18 empresas. Regressão de requisições por tamanho e setor: R² = 0,4256; R² ajustado = 0,1123; p global = 0,3117. |
| Machine Learning | experimento_ml.py, base_ml_empresas.csv, Relatorio_Machine_Learning_D2.pdf | Script reexecutado: 14 empresas no treino, 4 no teste, 5 dobras. Regressão linear selecionada entre os modelos candidatos; no teste, MAE 536.755,45 e R² −0,7144. A referência pela média do treino obteve MAE menor (534.944,25). Não há ganho demonstrado frente à referência. |
| Análise Preditiva | Analise_Preditiva_D2.ipynb, serie_diaria_c01.csv, Relatorio_Analise_Preditiva_D2.pdf | Todas as células de código reexecutadas em ordem, com adaptação apenas da exibição. SES selecionado pela validação temporal (MAE 3,5214); MAE 3,3989 e RMSE 4,0677 falhas por dia no teste. Projeção de 195,7553 falhas em 14 dias, sem observações futuras para confronto. |

## Reprodução

- Estatística: renderizar o Rmd com o CSV na mesma pasta, usando R 4.5.2, rmarkdown e Pandoc.
- Machine Learning: executar `python experimento_ml.py --saida resultados`. O script usa a base ao lado do arquivo e preserva a divisão, métricas, previsões e versões em `experimento.json`.
- Análise Preditiva: abrir o notebook com o CSV ao lado e executar todas as células em Python 3.13 com pandas, NumPy, matplotlib, scikit-learn e statsmodels.

## Conferência com as exigências da Entrega 3

Referência: [levantamento da SCRUM-591](SCRUM-591-conferencia-entrega-final.md), baseado no estudo de caso v2 e nos materiais do Canvas. O levantamento não inclui as rubricas individuais confirmadas de cada disciplina.

| Disciplina | O que a reprodução comprova | Lacuna para a entrega final |
| --- | --- | --- |
| Estatística Multivariada | Regressão e interpretação reproduzíveis na base sintética. | O relatório renderizado não comprova um dashboard integrado nem a interpretação exibida na aplicação. |
| Machine Learning | Modelos e avaliação de desempenho reproduzidos; o modelo selecionado não superou a referência no teste. | A execução do script não comprova que o modelo ou suas recomendações estejam integrados à jornada do HestIA. |
| Análise Preditiva | Validação temporal, métricas e previsão de 14 dias reproduzidas. | O notebook não comprova que a previsão esteja disponível no dashboard nem como apoia uma decisão. |

A consolidação registra as três análises existentes e dimensiona a diferença entre seus resultados e as evidências exigidas na versão final. A integração e a homologação das telas precisam ser verificadas no escopo da SCRUM-585; caso faltem funcionalidades ou análises obrigatórias, registrar e estimar o trabalho separadamente. As rubricas individuais permanecem pendentes de conferência. Este documento não comprova o aceite acadêmico da Entrega 3.
