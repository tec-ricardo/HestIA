# Metodologia e consolidação da maturidade

Referências lidas em PI4knorre.zip: SCRUM-60, 61, 62, 63 e 204–207. A base de dados do ZIP foi ignorada.

## Implementação

`CalculoMaturidadeService` consolida notas de fontes autorizadas usando a composição versionada existente. Reproduz o exemplo 73,25, valida 0–100 e rejeita fatores desconhecidos. Se faltar um fator, o score integral fica indisponível: nota ausente não vira zero e os pesos não são redistribuídos. Essa guarda é uma decisão técnica explícita, pendente de homologação, apoiada na exigência de não apresentar dados demonstrativos como reais.

`GET /indicadores/maturidade/metodologia` expõe a composição, as fontes potenciais e a pendência das regras de notas. Exige autenticação e contexto de empresa válidos. O frontend não pode interpretar esse endpoint como uma entrega de notas calculadas.

O método `calcular` recebe notas já calculadas pelo integrador; não é um endpoint para o navegador enviar notas arbitrárias. A aplicação ainda precisa de fontes e regras aprovadas para produzir cada nota.

## Influência nas tarefas

- SCRUM-431 e 432: disponibiliza documentação e metadados sobre cálculo consolidado, fontes potenciais e resultados ausentes. A fórmula de cada nota continua pendente.
- SCRUM-433: reutiliza a versão 1.0 da composição da develop. Não implementa um editor nem histórico de versões de fórmulas de notas ainda inexistentes.
- SCRUM-434: acrescenta exposição de metodologia pela API. Integração na interface continua pendente.
- SCRUM-439: quatro testes unitários validam exemplo conhecido, extremos, ausência, limites e fatores inválidos. Isso não valida uma fórmula de produtividade que o ZIP não define.

A cobertura `pesoComNota` é a soma dos pesos com nota disponível (0–1); não equivale à qualidade de registros da SCRUM-442, à cobertura de população ou a um grau de confiança estatística.
