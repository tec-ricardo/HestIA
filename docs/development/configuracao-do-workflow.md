# SCRUM-145 - Configurar o workflow básico do repositório

## 1. Objetivo

Configurar o GitHub do projeto HestIA para que alterações em `develop` e `main` ocorram somente por Pull Request, com revisão humana, histórico organizado e proteção contra exclusões ou sobrescritas acidentais.

## 2. Pré-requisitos

- Branch `main` criada.
- Branch `develop` criada a partir de `main`.
- Estratégia da SCRUM-143 aprovada.
- Fluxo de Pull Requests da SCRUM-144 aprovado.
- Acesso administrativo ao repositório.

## 3. Ruleset da `develop`

Configuração:

```text
Ruleset name: Proteção da develop
Enforcement status: Active
Target: Include by pattern → develop
```

Regras:

- [x] Restrict deletions.
- [x] Require a pull request before merging.
- [x] Required approvals: 1.
- [x] Dismiss stale pull request approvals when new commits are pushed.
- [x] Require conversation resolution before merging.
- [x] Block force pushes.
- [x] Allowed merge method: Squash.

Manter desativado inicialmente:

- Restrict creations.
- Restrict updates.
- Require linear history.
- Require deployments to succeed.
- Require signed commits.
- Require status checks to pass, até existir uma CI funcional.
- Code Owners, até existir um arquivo `CODEOWNERS` aprovado.
- Code scanning, code quality e coverage, até as ferramentas estarem configuradas.
- Regras adicionais de Copilot.

## 4. Ruleset da `main`

Configuração:

```text
Ruleset name: Proteção da main
Enforcement status: Active
Target: Include by pattern → main
```

Regras:

- [x] Restrict deletions.
- [x] Require a pull request before merging.
- [x] Required approvals: 1.
- [x] Dismiss stale pull request approvals when new commits are pushed.
- [x] Require conversation resolution before merging.
- [x] Block force pushes.
- [x] Allowed merge method: Merge.

As opções ainda não implantadas na `develop` também permanecem desativadas na `main`.

## 5. Métodos de merge do repositório

Em `Settings → General → Pull Requests`:

- [x] Allow merge commits.
- [x] Allow squash merging.
- [ ] Allow rebase merging.
- [x] Automatically delete head branches.

Política:

```text
Branches de tarefa → develop = Squash and merge
develop → main              = Create a merge commit
```

## 6. Template de Pull Request

Adicionar no repositório:

```text
.github/pull_request_template.md
```

O template deve solicitar:

- Objetivo.
- Chave Jira.
- Alterações realizadas.
- Como testar.
- Evidências.
- Riscos e impactos.
- Checklist do autor.
- Checklist do revisor.

O arquivo produzido para esta entrega é `SCRUM-144-template-de-pull-request.md`, que deverá ser salvo no caminho acima.

## 7. Teste controlado do workflow

Criar uma branch de documentação:

```text
docs/SCRUM-145-validar-workflow
```

Procedimento:

1. Criar uma pequena alteração documental.
2. Fazer commit contendo `[SCRUM-145]`.
3. Publicar a branch.
4. Abrir PR para `develop`.
5. Confirmar que o GitHub exige uma aprovação.
6. Solicitar aprovação de outro integrante.
7. Confirmar que o merge liberado é `Squash and merge`.
8. Fazer o merge.
9. Confirmar que a branch temporária foi excluída automaticamente.
10. Confirmar que o commit consolidado aparece em `develop`.

Não testar force push ou exclusão usando branches com trabalho relevante.

## 8. Pipeline de integração contínua

A opção `Require status checks to pass` deverá ser ativada depois que a equipe definir e validar a rotina de build e testes da aplicação.

Para o backend Spring Boot, a futura CI deve executar, conforme o gerenciador adotado:

```text
./mvnw test
```

ou:

```text
./gradlew test
```

Não é seguro criar uma pipeline definitiva antes de confirmar a estrutura real do projeto, o gerenciador de dependências e os comandos que funcionam localmente.

## 9. Evidências para o Jira

Anexar:

- Captura do ruleset da `develop` ativo.
- Captura do ruleset da `main` ativo.
- Captura dos métodos de merge permitidos.
- Link do template de Pull Request.
- Link do PR usado no teste controlado.
- Evidência da aprovação e do squash merge.

## 10. Critérios de aceite

- [x] Configuração necessária da `develop` especificada.
- [x] Configuração necessária da `main` especificada.
- [x] Métodos de merge especificados.
- [x] Template de PR produzido.
- [x] Procedimento de teste especificado.
- [ ] Ruleset da `develop` criado e testado no GitHub.
- [ ] Ruleset da `main` criado e testado no GitHub.
- [ ] Template publicado no repositório.
- [ ] Evidências anexadas ao Jira.

