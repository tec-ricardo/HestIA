# Risco departamental — regra 1.0

Definida em 12/11/2026 sob autorização de Luccas para fechar as definições de produto. Não é norma externa, probabilidade de incidente ou regra oficial da QSoft. Implementação: src/main/java/br/com/hestia/indicadores/maturidade/ClassificacaoRiscoDepartamental.java.

## Cálculo e faixas

Índice = 100 − (0,30 × Governança + 0,30 × Uso Responsável + 0,40 × Segurança).

As notas de entrada ficam entre 0 e 100. Os pesos derivam de 15/15/20 já preservados na 539, normalizados sobre esses três fatores. Não usar maturidade global, produtividade, custos ou volume de IA como substitutos de segurança.

| Índice antes de arredondar | Faixa |
| --- | --- |
| 0 ≤ índice < 25 | BAIXO |
| 25 ≤ índice < 50 | MEDIO |
| 50 ≤ índice ≤ 100 | ALTO |

Decidir a faixa antes de arredondar; apresentar índice em duas casas com HALF_UP. Um índice imediatamente abaixo de 25 pode aparecer como 25,00 após arredondar e continuar BAIXO: mostrar precisão adicional ao explicar esse limite, sem alterar a classificação calculada.

## Qualidade e ausência

- Mesmo departamento/empresa, período e natureza. A chamada fica dentro da consulta autorizada da 579/580; a classe matemática não substitui autorização. Não misturar naturezas de dados ou versões de cálculo incompatíveis.
- Governança e Uso Responsável: pelo menos 30 observações próprias de cada fator, cobertura técnica/classificação de pelo menos 95%, fontes e requisitos históricos atendidos.
- Segurança: oito controles obrigatórios, os oito avaliados conclusivamente, mesma release e corte, conforme seguranca-1.0. Não exigir 30 controles, não aceitar 800 avaliações repetidas como universo de oito.
- Exigir elegivelGlobal=true e estado CALCULADO em cada um dos três fatores, além das condições acima, para classificação definitiva. A nota global de maturidade pode continuar indisponível por outros fatores: ela não é pré-requisito nem substituto desta regra.
- Se faltar qualquer nota: estado SEM_CLASSIFICACAO; índice e faixas nulos. Não preencher zero ou recalcular sobre os fatores restantes.
- Se houver três notas válidas mas qualidade insuficiente: estado FRAGIL; índice descritivo e classificacaoIndicativa disponíveis; classificacao definitiva nula. Mostrar aviso laranja e motivos. Sem dados e fragilidade não significam risco baixo.
- Faixa de risco e qualidade dos dados são informações diferentes. Verde de qualidade significa parâmetros atendidos, não aprovação de todos os controles. A regra não substitui resposta a incidente, auditoria ou avaliação de impacto.

## Exemplos reproduzíveis

| Governança | Uso Responsável | Segurança | Índice | Saída com dados suficientes |
| --- | --- | --- | --- | --- |
| 100 | 100 | 100 (8/8 controles atendidos) | 0 | BAIXO |
| 75 | 75 | 75 (6/8 atendidos; 8/8 avaliados) | 25 | MEDIO |
| 50 | 50 | 50 (4/8 atendidos; 8/8 avaliados) | 50 | ALTO |
| 100, mas só 10 observações | 100 | 100 | 0 descritivo | FRAGIL; BAIXO apenas indicativo |
| ausente | 100 | 100 | indisponível | SEM_CLASSIFICACAO |

Cada resultado devolve evidências agregadas: fator, nota, fonte, versão, amostra, numerador/denominador, coberturas, motivo e campos esperados. Para investigar a classificação, a 580 deve oferecer os registros do mesmo escopo, sujeitos a autorização. Esses campos não provam sozinhos o cumprimento dos controles.

Os casos executados e suas saídas estão em evidencias/risco-departamental-cenarios.json; são dados didáticos de teste, sem medição de departamentos reais. Metodologia versionada: risco-departamental-1.0. Mudanças futuras nos pesos/limites exigem nova versão e não devem reescrever apurações históricas.
