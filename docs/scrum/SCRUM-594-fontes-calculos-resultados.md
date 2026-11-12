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

## Evidências pendentes para a versão final

| Área | Evidência a incorporar |
| --- | --- |
| Maturidade e risco | Resultado dos oito fatores, cobertura e confiança no mesmo escopo e período; critérios homologados de classificação de risco e validação do ranking departamental. |
| Comparação entre períodos | Validação com fontes integradas, versões compatíveis e períodos equivalentes. |
| RAG | Evidências da validação com embeddings, corpus autorizado, permissões e comportamento quando não houver correspondência segura. |
| Análises no produto | Evidência das telas e da jornada da versão final, quando exigida pelas rubricas da Entrega 3. |
