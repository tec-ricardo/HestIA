# Integração das dependências de Ricardo

## Trilhas para a SCRUM-527

Autenticar pela API existente e enviar `Authorization: Bearer <token>`.

1. Como ADMIN/GESTOR, `POST /trilhas` com `{"titulo":"Capacitação","microtreinamentos":[1,2]}`.
2. `POST /trilhas/{trilhaId}/usuarios/{usuarioId}` cria ou devolve a participação.
3. Registrar conclusões pelo fluxo existente de microtreinamentos.
4. `GET /trilhas/usuarios/{usuarioId}` retorna o histórico de participações atualizado.
5. `GET /trilhas/conclusoes/{conclusaoId}` localiza os vínculos para a SCRUM-527.
6. `GET /trilhas/participacoes/{id}` retorna usuário, empresa, status, datas e etapas.

Cada etapa contém `ordem`, `microtreinamentoId`, `conclusaoId`, `dataConclusao` e `status`.
O progresso contém `id`, `trilhaId`, `titulo`, `usuarioId`, `empresaId`, `status`, `totalEtapas`,
`etapasConcluidas`, `percentual`, `iniciadaEm`, `dataConclusao` e `etapas`.

O funcionário consulta apenas suas próprias participações; responsáveis consultam usuários ativos da própria empresa.
A API não disponibiliza o conteúdo de conclusões de outra empresa.

## Qualidade para a SCRUM-396

Diagnóstico: `GET /indicadores/dados/qualidade?inicio=2026-10-01&fim=2026-10-21&minimoRegistros=10`.
Validação com bloqueio: `GET /indicadores/dados/validacao` com os mesmos parâmetros.

O integrador Java pode injetar `QualidadeDadosService`, chamar `consultar` e depois
`exigirDadosMinimos` antes de executar um cálculo. Os IDs elegíveis devem ser usados
para selecionar a mesma amostra. A fonte e a versão da regra acompanham o retorno.

O mínimo 10 no exemplo é ilustrativo. Nenhum mínimo de negócio foi aprovado automaticamente.
Cobertura da amostra observada não substitui a cobertura de população/fatores da SCRUM-396.
O mapeamento fonte → nota de fator e seus limites continuam pendentes do grupo/ZIP.

## Banco, execução e testes

V18 é uma migração adicional; não altera migrações já aplicadas nem insere dados de demonstração. A V17 foi reservada pela develop para tratamentos de não conformidade; as trilhas usam V18 para evitar colisão.
Ela será executada quando o backend atualizado iniciar em um ambiente com Flyway habilitado.
Não foi aplicada no banco real nesta execução.

```powershell
.\mvnw.cmd "-Dtest=DependenciasSprintIntegrationTest,FlywayMigrationTest" test
```

Se a pasta `target` contiver migrações de branches antigas, use uma pasta de compilação limpa
com `-Dhestia.build.directory=CAMINHO_ABSOLUTO` ou limpe apenas os artefatos de compilação.
Isso não cria outra cópia dos fontes do projeto.
