# SCRUM-440 — Validação dos registros elegíveis

Data: 21/10/2026. Implementação local, pendente de revisão e homologação.

Arquivo: `indicadores/service/QualidadeDadosService.java`. Versão técnica da regra: `elegibilidade-1.0`.

Fonte: entidades persistidas de `registros_uso_ia`, relacionadas ao usuário e à ferramenta. A consulta aplica empresa, período inclusivo por dia e escopo do funcionário antes da avaliação. O limite final é implementado como início do dia seguinte exclusivo.

Para esta versão técnica, um registro elegível possui ID, data válida, usuário/empresa autorizados, ferramenta da mesma empresa, modelo/finalidade preenchidos, execução `SUCESSO`, tokens não negativos e total consistente. Custos ou tempos negativos são inválidos. Custo e tempo ausentes não invalidam contagem de uso; por isso esse conjunto, isoladamente, não autoriza cálculo de produtividade ou FinOps. IDs duplicados são excluídos integralmente quando o serviço recebe uma coleção repetida.

Impacto na SCRUM-396: retorna IDs elegíveis e motivos de exclusão para que o calculador use a mesma seleção. As escolhas sobre sucesso, campos obrigatórios e janela temporal estão explícitas e precisam de homologação do grupo. Elas não definem notas dos fatores de maturidade.

Teste: registros de duas empresas, diferentes usuários, execução sem sucesso e amostra insuficiente.
