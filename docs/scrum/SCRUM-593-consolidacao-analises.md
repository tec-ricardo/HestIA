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

## Pendência de aceite

O Jira indica a SCRUM-591 pronta para entrega, mas o item consultado não contém a rubrica nem anexo. A reprodução técnica não substitui o cruzamento com as exigências docentes vigentes. Não foi identificado, com as fontes disponíveis, se a entrega final exige análises adicionais.
