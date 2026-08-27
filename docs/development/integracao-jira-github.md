# SCRUM-147 - Vinculação do fluxo de desenvolvimento ao Jira

## 1. Objetivo

Relacionar cada tarefa do Jira às branches, commits e Pull Requests correspondentes no GitHub, permitindo identificar quem alterou o projeto, por qual motivo, como a alteração foi revisada e em qual versão foi entregue.

## 2. Regra central de rastreabilidade

Toda alteração versionada deve informar a chave da tarefa Jira em três pontos:

```text
Branch: feature/SCRUM-86-cadastro-empresa
Commit: feat: cria entidade Empresa [SCRUM-86]
PR:     [SCRUM-86] Implementar cadastro de Empresa
```

Essa convenção funciona mesmo antes de uma integração automática entre Jira e GitHub.

## 3. Mapeamento recomendado entre Jira e GitHub

| Estado no Jira | Evento no desenvolvimento | Evidência no GitHub |
|---|---|---|
| Backlog/A fazer | Tarefa ainda não iniciada | Nenhuma branch necessária |
| Pronto para iniciar | Tarefa refinada e priorizada | Critérios de aceite definidos |
| Em desenvolvimento | Responsável inicia o trabalho | Branch criada a partir de `develop` |
| Em revisão | Implementação pronta para análise | Pull Request aberto para `develop` |
| Em teste | PR aprovado e integrado | Merge em `develop` e testes executados |
| Em homologação | Versão integrada pronta para aceite | Evidências anexadas ao Jira |
| Concluído | Critérios aceitos | Homologação registrada; release associada quando aplicável |

## 4. Adaptação ao quadro atual

Caso o quadro possua apenas `A fazer`, `Em desenvolvimento`, `Em análise` e `Concluído`, utilizar provisoriamente:

| Estado atual | Uso temporário |
|---|---|
| A fazer | Backlog e tarefas prontas ainda não iniciadas |
| Em desenvolvimento | Branch criada e trabalho em execução |
| Em análise | PR, revisão, teste e homologação |
| Concluído | Alteração aprovada e aceita |

Recomenda-se futuramente separar `Em análise` em:

```text
Em revisão → Em teste → Em homologação
```

Isso permite identificar rapidamente onde cada tarefa está parada.

## 5. Transições e responsáveis

| Origem | Transição | Destino | Responsável |
|---|---|---|---|
| A fazer | Iniciar tarefa e criar branch | Em desenvolvimento | Desenvolvedor |
| Em desenvolvimento | Abrir PR pronto para revisão | Em revisão/Em análise | Autor do PR |
| Em revisão | Solicitar mudanças | Em desenvolvimento | Revisor |
| Em revisão | Aprovar e integrar em `develop` | Em teste | Revisor/equipe |
| Em teste | Teste reprovado | Em desenvolvimento | Tester |
| Em teste | Teste aprovado | Em homologação | Tester |
| Em homologação | Homologação reprovada | Em desenvolvimento | PO/equipe |
| Em homologação | Aceitar critérios | Concluído | PO |

## 6. Exemplo completo

Tarefa:

```text
SCRUM-86 - Implementar cadastro de Empresa no MVP
```

### Início

Jira:

```text
A fazer → Em desenvolvimento
```

GitHub:

```text
feature/SCRUM-86-cadastro-empresa
```

### Desenvolvimento

Commits:

```text
feat: cria entidade Empresa [SCRUM-86]
feat: adiciona migration de Empresa [SCRUM-86]
test: valida cadastro de Empresa [SCRUM-86]
```

### Revisão

Pull Request:

```text
Base: develop
Compare: feature/SCRUM-86-cadastro-empresa
Título: [SCRUM-86] Implementar cadastro de Empresa
```

Jira:

```text
Em desenvolvimento → Em revisão
```

### Integração e teste

Após aprovação:

```text
Squash merge → develop
```

Jira:

```text
Em revisão → Em teste
```

### Homologação

Evidências anexadas ao Jira:

- Link do Pull Request.
- Resultado dos testes.
- Captura ou vídeo da funcionalidade.
- Observações da validação.

Jira:

```text
Em teste → Em homologação → Concluído
```

## 7. Conteúdo obrigatório da tarefa Jira

Cada tarefa técnica deve conter:

- Descrição do objetivo.
- Contexto ou necessidade.
- Critérios de aceite verificáveis.
- Responsável.
- Estimativa.
- Dependências conhecidas.
- Link do Pull Request.
- Evidências de teste e homologação.

Template sugerido:

```markdown
## Objetivo
Descrever o resultado esperado.

## Critérios de aceite
- [ ] Critério 1
- [ ] Critério 2

## Dependências
- SCRUM-XX, se aplicável

## Evidências
- Branch:
- Pull Request:
- Testes:
- Homologação:
```

## 8. Configuração da integração Jira-GitHub

Esta etapa requer permissão administrativa no Jira e acesso ao repositório no GitHub.

Procedimento recomendado:

1. No Jira, abrir `Apps` ou a área de integrações do projeto.
2. Localizar a integração oficial com GitHub disponível na instância.
3. Autorizar a organização ou o repositório `tec-ricardo/HestIA`.
4. Limitar o acesso ao repositório do projeto quando possível.
5. Criar uma branch e um PR de teste contendo uma chave Jira.
6. Confirmar se branches, commits e PRs aparecem na área de desenvolvimento da tarefa.
7. Registrar uma captura da integração funcionando como evidência.

Não inserir tokens, chaves ou credenciais em arquivos do repositório ou comentários do Jira.

## 9. Automação recomendada

Se a integração e o plano do Jira permitirem automações, configurar:

```text
Branch criada com chave Jira
→ mover tarefa para Em desenvolvimento

Pull Request aberto
→ mover tarefa para Em revisão

Pull Request recusado ou com mudanças solicitadas
→ mover tarefa para Em desenvolvimento

Pull Request integrado em develop
→ mover tarefa para Em teste
```

A transição para `Concluído` não deve depender apenas do merge. Ela deve ocorrer depois dos testes e da homologação dos critérios de aceite.

Antes de ativar uma automação global, testá-la com uma tarefa de exemplo para evitar movimentações incorretas no quadro.

## 10. Relação entre tarefa, história e épico

- Épicos e histórias-pai agrupam trabalho e normalmente não geram branch.
- A branch corresponde à menor tarefa técnica executável.
- Uma história é concluída quando suas tarefas-filhas obrigatórias são aceitas.
- Uma sprint é promovida a `main` por um Release PR após homologação.

Exemplo:

```text
SCRUM-77 - Primeira versão do MVP
├── SCRUM-86 - Cadastro de Empresa
├── SCRUM-91 - Cadastro de Política
├── SCRUM-110 - Integração Node.js
└── SCRUM-111 - Validação de notificações
```

Não criar uma branch ampla `feature/SCRUM-77-mvp` se as tarefas-filhas estiverem sendo desenvolvidas separadamente.

## 11. Evidências para concluir a SCRUM-147

- [x] Convenção de chave Jira em branches definida.
- [x] Convenção de chave Jira em commits definida.
- [x] Convenção de chave Jira em Pull Requests definida.
- [x] Estados do Jira mapeados para eventos do GitHub.
- [x] Responsáveis pelas transições definidos.
- [x] Procedimento de configuração da integração documentado.
- [ ] Integração externa autorizada por administrador.
- [ ] Teste real de sincronização executado.
- [ ] Evidência do teste anexada à tarefa Jira.

Os três últimos itens dependem de acesso administrativo à instância Jira e devem ser realizados no ambiente da equipe antes de marcar a tarefa como concluída.

