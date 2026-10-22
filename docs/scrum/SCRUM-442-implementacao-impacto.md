# SCRUM-442 — Cobertura e sinalização de problemas

Data: 21/10/2026. Implementação local, pendente de revisão e homologação.

Arquivos: `QualidadeDadosService.java` e `QualidadeDadosController.java`.

`GET /indicadores/dados/qualidade?inicio=2026-10-01&fim=2026-10-21&minimoRegistros=10` retorna versão, empresa, usuário quando houver escopo individual, fonte, período, total observado, elegíveis, excluídos, cobertura, problemas e IDs elegíveis.

A cobertura implementada é **qualidade da amostra observada**: registros elegíveis / registros encontrados no período × 100. Com nenhum registro, retorna `cobertura: null`. Esse denominador não mede adoção na população nem a cobertura dos fatores de maturidade; para isso ainda é necessário definir a população esperada e as fontes específicas.

Os problemas possuem códigos estáveis: `REGISTRO_NULO`, `SEM_IDENTIFICADOR`, `DUPLICADO`, `DATA_INVALIDA`, `FERRAMENTA_FORA_ESCOPO`, `METADADOS_INCOMPLETOS`, `EXECUCAO_SEM_SUCESSO`, `TOKENS_INCONSISTENTES`, `CUSTO_INVALIDO`, `TEMPO_INVALIDO`.

Impacto: o colega recebe uma base rastreável para integrar à SCRUM-396, sem confundir falta de observações com cobertura zero. Não foram atribuídos rótulos de confiança ou notas sem regras acordadas.

Teste: um registro elegível e um inválido produzem 50%; outra empresa não entra no denominador; período vazio retorna null.
