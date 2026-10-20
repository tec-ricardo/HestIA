# Integração da base sintética

## 1. Fonte oficial

O arquivo `synthetic_ai_usage_dataset.zip` é a base oficial de desenvolvimento do HestIA, pois a QSoft não disponibilizará a base real.

- Formato principal: SQLite (`database/ai_usage_synthetic.db`).
- Formato de referência: CSVs do diretório `csv/`.
- SHA-256: `46E11EA1D5B7C4C1CDF8757E818F433031415DF4FF9359A0301B25FEE`.
- Empresas: 18.
- Tabelas: 17.
- Linhas importadas para o staging: 311.556.

O arquivo original deve permanecer inalterado. Cada desenvolvedor utiliza sua própria cópia local e seu próprio PostgreSQL.

## 2. Armazenamento da origem

As tabelas são armazenadas em `base_sintetica.import_linhas`.

Cada linha possui `origem_sha256`, `tabela`, `chave_origem`, `dados` em JSONB e `importado_em`. A combinação `(origem_sha256, tabela, chave_origem)` é única. A importação deve ser idempotente.

Todas as 18 empresas permanecem no staging para análises, consultas agregadas, validações e testes de isolamento.

## 3. Empresa operacional

O fluxo operacional do HestIA utiliza uma única empresa representativa da QSoft:

| Campo | Valor |
|---|---|
| Código de origem | `C01` |
| Nome | `NexoCode Soluções` |
| Setor | `Tecnologia / Desenvolvimento de Software` |
| Porte | `Grande` |
| Funcionários informados | `1.450` |

O código `C01` deve ser usado como referência de origem nos relacionamentos do carregamento operacional. As outras 17 empresas não serão convertidas em empresas operacionais nesta etapa.

## 4. Mapeamento operacional

| Origem | Destino HestIA | Regra |
|---|---|---|
| `companies` | `empresas` | Criar a empresa operacional correspondente a `C01`. |
| `departments` | `departamentos` | Carregar registros com `company_id = C01` e relacionar ao `empresa_id` criado. |
| `employees` | `base_sintetica` | Manter os 363 funcionários de `C01` no staging. `employee_id` é identificador anônimo e não representa um usuário autenticável. |
| `ai_tools` | `ferramentas_ia` | Carregar as ferramentas utilizadas por `C01` e relacioná-las ao `empresa_id` operacional. |
| `ai_usage_logs` | `base_sintetica` | Manter os 99.604 registros de `C01` no staging para análises agregadas por ferramenta, departamento e identificador anônimo. |
| `ai_billing`, `ai_budgets`, `ai_licenses`, `ai_models` | `base_sintetica` | Permanecer no staging nesta etapa. |
| `approved_ai_tools`, `builds`, `deployments`, `issues`, `projects`, `pull_requests` | `base_sintetica` | Permanecer no staging nesta etapa. |
| `security_events`, `training_records` | `base_sintetica` | Permanecer no staging nesta etapa. |

## 5. Dados ausentes e restrições

A base não contém nomes reais, credenciais, senhas, e-mails de usuários, documentos pessoais, CNPJ real, prompts, respostas, políticas ou documentos textuais.

- Não criar credenciais, senhas, e-mails, CNPJ ou dados de autenticação inventados.
- Não converter `employee_id` em `usuarios`.
- Não carregar `ai_usage_logs` em `registros_uso_ia`, pois essa tabela exige um usuário autenticado.
- Não criar `respostas_reutilizaveis` a partir da base sintética.
- Não gerar embeddings nem executar RAG com as tabelas atuais.
- Não aceitar `empresa_id` livremente da entrada para definir o tenant.
- Derivar os relacionamentos operacionais do vínculo de origem com `C01`.

## 6. Ordem de execução

1. Executar as migrations do HestIA.
2. Importar o SQLite para `base_sintetica.import_linhas`.
3. Validar quantidade de linhas, SHA-256 e unicidade das chaves.
4. Criar `C01` em `empresas`.
5. Carregar departamentos e ferramentas de `C01`.
6. Manter funcionários e registros de uso no staging para análises agregadas.
7. Validar chaves estrangeiras, registros órfãos e isolamento por `empresa_id`.

## 7. Critérios de validação

- O staging contém todas as linhas da origem sem duplicidade.
- Existe uma única empresa operacional correspondente a `C01`.
- Departamentos e ferramentas pertencem a essa empresa.
- Não existem registros operacionais sem `empresa_id` válido.
- Não existem relacionamentos com funcionários, departamentos ou ferramentas de outra empresa.
- Consultas de outra empresa não retornam dados de `C01`.
- Execuções repetidas do importador não duplicam registros.
- Funcionários e registros de uso permanecem consultáveis no staging por identificadores anônimos.
