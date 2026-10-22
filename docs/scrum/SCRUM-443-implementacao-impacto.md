# SCRUM-443 — Bloqueio de cálculo sem dados mínimos

Data: 21/10/2026. Implementação local, pendente de revisão e homologação.

Arquivos: `QualidadeDadosService.java` e `QualidadeDadosController.java`.

O campo `calculavel`. Ele é verdadeiro quando a quantidade de elegíveis atinge o mínimo explícito. O mínimo deve ser um inteiro entre 1 e 100000. Não há mínimo universal inventado para o produto.

`exigirDadosMinimos(resultado)` gera HTTP 422 quando a amostra é insuficiente. `GET /indicadores/dados/validacao` aplica essa guarda antes de devolver o resultado. O endpoint de diagnóstico `/qualidade` continua retornando a situação e os problemas, mesmo quando o cálculo está bloqueado.

O parâmetro `minimoRegistros` é um contrato técnico para o integrador, não uma política homologada por fator. O futuro calculador deve escolher o mínimo a partir de uma regra de servidor aprovada, em vez de permitir ao usuário final reduzir a exigência. As notas/fórmulas e mínimos por fator continuam dependentes do alinhamento solicitado ao grupo.

Impacto: fornece uma guarda reutilizável para a SCRUM-396. A integração dentro do calculador do colega continua necessária; consultar apenas `/qualidade` e ignorar `calculavel` não garante bloqueio.

Teste: amostra abaixo do mínimo retorna `DADOS_INSUFICIENTES`, guarda lança 422 e a API de validação retorna 422.
