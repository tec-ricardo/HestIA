# SCRUM-144 - Fluxo de Pull Requests e revisão

## 1. Objetivo

Definir como uma alteração do projeto HestIA é proposta, revisada, testada, corrigida e integrada, impedindo mudanças diretas nas branches protegidas e criando evidências do processo de qualidade.

## 2. Fluxo padrão

```text
Tarefa pronta no Jira
  │ Início do trabalho
  ↓
Branch criada a partir de develop
  │ Commits e push
  ↓
Pull Request para develop
  │ Revisão humana
  ↓
Testes e verificações
  │ Aprovação
  ↓
Squash merge em develop
  │ Teste integrado
  ↓
Homologação
  │ Aprovação da sprint
  ↓
Release PR de develop para main
  │ Merge commit e tag
  ↓
Versão entregável
```

## 3. Tipos de Pull Request

### Pull Request de tarefa

```text
Base: develop
Compare: tipo/SCRUM-XX-descricao
Método: Squash and merge
```

Utilizado para funcionalidades, correções, documentação, testes, refatorações e configurações.

### Release Pull Request

```text
Base: main
Compare: develop
Método: Create a merge commit
```

Utilizado somente quando o conjunto integrado em `develop` foi testado e homologado.

## 4. Quando abrir um Pull Request

O autor pode abrir um Draft Pull Request assim que houver uma estrutura mínima útil para colaboração. O PR deve ser marcado como pronto para revisão quando:

- O objetivo principal estiver implementado.
- Os critérios de aceite tiverem sido conferidos.
- Os testes relevantes tiverem sido criados ou atualizados.
- O autor tiver realizado uma auto-revisão.
- Não houver credenciais ou arquivos pessoais.
- A branch estiver atualizada e sem conflitos conhecidos.

## 5. Convenção de título

Formato:

```text
[CHAVE-JIRA] Verbo no infinitivo + objetivo
```

Exemplos:

```text
[SCRUM-86] Implementar cadastro de Empresa
[SCRUM-102] Registrar decisão de monólito modular
[SCRUM-111] Validar envio e consulta de notificações
```

## 6. Template de Pull Request

```markdown
## Objetivo
Descreva o resultado esperado desta alteração.

## Jira
SCRUM-XX

## Alterações realizadas
- Alteração 1
- Alteração 2

## Como testar
1. Passo 1
2. Passo 2
3. Resultado esperado

## Evidências
Inclua capturas, logs ou resultados de testes quando aplicável.

## Riscos e impactos
Informe migrations, mudanças de contrato, configurações ou impactos conhecidos.

## Checklist do autor
- [ ] A alteração atende aos critérios de aceite.
- [ ] Revisei meu próprio código.
- [ ] Criei ou atualizei os testes necessários.
- [ ] Não incluí credenciais ou arquivos pessoais.
- [ ] A documentação foi atualizada quando necessário.
- [ ] A branch está pronta para revisão.
```

## 7. Processo de revisão

### Autor

1. Abre o PR com título e descrição completos.
2. Indica a chave Jira.
3. Solicita ao menos um revisor que não seja o autor.
4. Responde aos comentários.
5. Faz as correções na mesma branch.
6. Solicita nova análise após mudanças relevantes.

### Revisor

Verifica:

- Aderência ao escopo da tarefa.
- Correção funcional.
- Clareza e manutenção do código.
- Respeito à arquitetura e aos padrões.
- Validações e tratamento de erros.
- Segurança e ausência de dados sensíveis.
- Qualidade dos testes.
- Impacto em banco, API e integrações.
- Atualização da documentação.

O revisor deve classificar seus comentários:

| Prefixo | Significado | Bloqueia merge? |
|---|---|---:|
| `bloqueante:` | Erro, risco ou requisito não atendido | Sim |
| `sugestão:` | Melhoria recomendada | Não necessariamente |
| `dúvida:` | Solicitação de esclarecimento | Até ser respondida |
| `elogio:` | Prática positiva observada | Não |

## 8. Resultados possíveis da revisão

### Aprovar

Usar quando a alteração atende aos critérios e pode ser integrada.

### Solicitar mudanças

Usar quando existir comentário bloqueante. O autor envia novos commits para a mesma branch; o PR é atualizado automaticamente.

### Comentar

Usar para dúvidas e observações que ainda não justificam aprovação ou rejeição.

## 9. Requisitos para merge em `develop`

- Uma aprovação válida de pessoa diferente do autor.
- Nenhum comentário bloqueante aberto.
- Todas as conversas resolvidas.
- Ausência de conflitos.
- Testes existentes aprovados.
- Status checks aprovados quando a CI estiver configurada.
- Evidência de teste manual quando aplicável.
- Chave Jira presente no título ou descrição.

Método obrigatório: **Squash and merge**.

Mensagem sugerida do squash:

```text
[SCRUM-86] Implementar cadastro de Empresa
```

## 10. Teste e homologação

Após o merge em `develop`:

1. Testar a funcionalidade na versão integrada.
2. Verificar interação com alterações de outras tarefas.
3. Registrar evidências no Jira.
4. Homologar conforme os critérios de aceite.

Se houver reprovação, criar tarefa de correção e uma branch `fix/` a partir de `develop`.

## 11. Requisitos para Release PR

Antes de abrir `develop → main`:

- Tarefas da entrega homologadas.
- Testes automatizados aprovados.
- Defeitos bloqueantes resolvidos.
- Documentação atualizada.
- Versão executável e demonstrável.
- Lista das tarefas incluídas preparada.

Título:

```text
Release v0.1.0 - Sprint 1 / Deliverable 1
```

Descrição mínima:

```markdown
## Entrega
Sprint e Deliverable correspondentes.

## Tarefas incluídas
- SCRUM-XX - Descrição
- SCRUM-YY - Descrição

## Validação
- [ ] Testes aprovados
- [ ] Homologação realizada
- [ ] Documentação atualizada
- [ ] Sem defeitos bloqueantes
```

Método obrigatório: **Create a merge commit**.

Depois do merge, criar tag/release, por exemplo:

```text
v0.1.0-deliverable-1
```

## 12. Conflitos

Quando o GitHub indicar conflito:

1. Atualizar a branch com `develop`.
2. Resolver os conflitos localmente.
3. Executar novamente os testes.
4. Fazer push da resolução.
5. Solicitar nova revisão quando a alteração for relevante.

Não resolver conflitos apagando alterações de outro integrante sem alinhamento.

## 13. Critérios de aceite da SCRUM-144

- [x] Tipos de Pull Request definidos.
- [x] Critérios de abertura definidos.
- [x] Template de PR criado.
- [x] Responsabilidades de autor e revisor definidas.
- [x] Critérios de aprovação e merge definidos.
- [x] Fluxo de correção definido.
- [x] Fluxo de Release PR definido.

