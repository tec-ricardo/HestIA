# Contribuindo com o HestIA

Este documento resume o fluxo oficial de desenvolvimento do projeto HestIA.

## Branches

Branches permanentes:

- `main`: versão estável, homologada e entregável.
- `develop`: versão integrada da sprint.

Toda alteração versionada deve ocorrer em uma branch temporária criada a partir de `develop`:

```text
feature/SCRUM-XX-descricao
fix/SCRUM-XX-descricao
docs/SCRUM-XX-descricao
test/SCRUM-XX-descricao
chore/SCRUM-XX-descricao
refactor/SCRUM-XX-descricao
```

Não utilize branches por pessoa, papel ou etapa do quadro.

## Iniciando uma tarefa

1. Confirme que a tarefa está priorizada e possui critérios de aceite.
2. Mova a tarefa para `Em desenvolvimento`.
3. Atualize `develop` e crie a branch:

```bash
git switch develop
git pull origin develop
git switch -c feature/SCRUM-XX-descricao
git push -u origin feature/SCRUM-XX-descricao
```

## Commits

Formato:

```text
tipo: descrição objetiva [CHAVE-JIRA]
```

Exemplos:

```text
feat: cria entidade Empresa [SCRUM-86]
fix: impede duplicidade de CNPJ [SCRUM-152]
docs: registra decisão arquitetural [SCRUM-102]
test: valida envio de notificação [SCRUM-111]
```

## Pull Request de tarefa

```text
Base: develop
Compare: branch da tarefa
Método: Squash and merge
```

Título:

```text
[SCRUM-XX] Descrição objetiva
```

Antes do merge são obrigatórios:

- Uma aprovação de pessoa diferente do autor.
- Conversas resolvidas.
- Ausência de conflitos.
- Testes relevantes aprovados.
- Chave Jira informada.

## Revisão

Comentários de revisão podem usar:

- `bloqueante:` precisa ser corrigido antes do merge.
- `sugestão:` melhoria recomendada.
- `dúvida:` requer esclarecimento.
- `elogio:` destaca uma boa prática.

Correções solicitadas devem ser enviadas para a mesma branch; o Pull Request será atualizado automaticamente.

## Jira

Mapeamento recomendado:

```text
A fazer
  │ Branch criada
  ↓
Em desenvolvimento
  │ Pull Request aberto
  ↓
Em revisão
  │ PR aprovado e integrado
  ↓
Em teste
  │ Testes aprovados
  ↓
Em homologação
  │ Critérios aceitos
  ↓
Concluído
```

A chave Jira deve constar na branch, nos commits e no título do Pull Request.

## Release

Após testes e homologação da sprint:

```text
Base: main
Compare: develop
Método: Create a merge commit
```

Depois do merge, criar uma tag, por exemplo:

```text
v0.1.0-deliverable-1
```

## Regras essenciais

- Não fazer push direto em `main` ou `develop`.
- Não utilizar force push nas branches protegidas.
- Não misturar tarefas sem relação no mesmo PR.
- Não versionar senhas, tokens ou arquivos de ambiente pessoais.
- Excluir a branch temporária depois do merge.
- Não considerar uma tarefa concluída apenas porque houve merge; testes e homologação também são necessários.

