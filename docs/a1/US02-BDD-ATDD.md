# US02 - Cadastro de Departamento

## História de usuário

Como administrador corporativo, quero cadastrar um departamento vinculado a
uma empresa, para organizar a estrutura empresarial e governar o uso de IA por
área.

## Cenários BDD e testes

| Cenário | Teste automatizado |
| --- | --- |
| Cadastro válido | `deveCadastrarDepartamentoQuandoDadosForemValidos` |
| Nome vazio | `deveRejeitarCadastroQuandoNomeEstiverVazio` |
| Empresa inexistente | `deveRejeitarCadastroQuandoEmpresaNaoExistir` |
| Nome duplicado | `deveRejeitarCadastroQuandoNomeJaExistirNaEmpresa` |
| HTTP 201 | `deveRetornarHttp201QuandoDepartamentoForCadastrado` |

## Regras implementadas

1. O nome é obrigatório e normalizado com `trim()`.
2. A empresa informada deve existir.
3. Não pode existir outro departamento com o mesmo nome na mesma empresa.
4. O departamento é vinculado à empresa e salvo.
5. O endpoint `POST /departamentos` retorna HTTP 201.

## Ciclo TDD

### RED

Tag: `a1-red`

```text
git switch --detach a1-red
mvn -Dtest=DepartamentoServiceTest test
```

Resultado esperado: quatro testes executados, duas falhas e zero erros. As
regras de nome obrigatório e duplicidade ainda não estavam implementadas.

### GREEN

Tag: `a1-green`

```text
git switch --detach a1-green
mvn -Dtest=DepartamentoServiceTest test
```

Resultado esperado: quatro testes executados, zero falhas e BUILD SUCCESS.

### BLUE

Tag: `a1-blue`

```text
git switch --detach a1-blue
mvn clean test
```

Resultado esperado: suíte completa aprovada após a separação das validações em
métodos menores.

Retorne à versão final com:

```text
git switch main
```

## Execução validada

Em 14/09/2026, a suíte completa apresentou 13 testes, zero falhas, zero erros e
um teste de integração externa ignorado de forma intencional.
