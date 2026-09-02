# SCRUM-52 - Definir os campos da entidade Ferramenta de IA

## Objetivo

Definir o contrato de dominio da ferramenta de Inteligencia Artificial
cadastrada no HestIA. Esta tarefa especifica o modelo; a implementacao da
entidade e da API pertence a SCRUM-54.

## Entidade canonica

- Classe: `FerramentaIA`
- Pacote: `br.com.hestia.ferramenta.model`
- Tabela: `ferramentas_ia`
- Proprietario do dado: modulo `ferramenta`

## Campos do MVP

| Campo | Tipo | Obrigatorio | Regra principal |
|---|---|---:|---|
| `id` | `Long` | sim | identificador gerado pelo banco |
| `nome` | `String` | sim | 2 a 120 caracteres |
| `fornecedor` | `String` | sim | 2 a 120 caracteres |
| `descricao` | `String` | nao | ate 500 caracteres |
| `tipo` | `TipoFerramentaIA` | sim | persistido como texto |
| `finalidadeUso` | `String` | sim | 10 a 500 caracteres |
| `urlAcesso` | `String` | nao | URL valida, ate 2048 caracteres |
| `status` | `StatusFerramentaIA` | sim | inicia em `EM_ANALISE` |
| `nivelRisco` | `NivelRiscoIA` | sim | inicia em `NAO_AVALIADO` |
| `trataDadosPessoais` | `Boolean` | sim | deve ser informado antes da aprovacao |
| `dataCadastro` | `LocalDateTime` | sim | criada automaticamente e imutavel |
| `dataAtualizacao` | `LocalDateTime` | sim | atualizada automaticamente |
| `empresa` | `Empresa` | sim | relacao muitos-para-um por `empresa_id` |

## Enumeracoes

O enum existente `TipoFerramentaIA` deve ser reutilizado com
`EnumType.STRING`.

`StatusFerramentaIA`:

- `EM_ANALISE`
- `APROVADA`
- `RESTRITA`
- `BLOQUEADA`
- `DESCONTINUADA`

`NivelRiscoIA`:

- `NAO_AVALIADO`
- `BAIXO`
- `MEDIO`
- `ALTO`
- `CRITICO`

## Persistencia e validacoes

- chave estrangeira obrigatoria em `empresa_id`;
- indice em `empresa_id` e em `status`;
- unicidade composta recomendada para `empresa_id`, `nome` e `fornecedor`;
- `nome`: `@NotBlank` e `@Size(min = 2, max = 120)`;
- `fornecedor`: `@NotBlank` e `@Size(min = 2, max = 120)`;
- `descricao`: `@Size(max = 500)`;
- `tipo`, `trataDadosPessoais` e `empresaId`: `@NotNull`;
- `finalidadeUso`: `@NotBlank` e `@Size(min = 10, max = 500)`;
- `urlAcesso`: `@Size(max = 2048)` e formato de URL quando preenchida.

## Regras de negocio

1. Toda ferramenta pertence a exatamente uma empresa.
2. O cadastro comum inicia em `EM_ANALISE` e `NAO_AVALIADO`.
3. Somente `ADMIN` altera aprovacao, restricao, bloqueio ou descontinuacao.
4. `GESTOR` cadastra para analise e edita enquanto estiver em analise.
5. `FUNCIONARIO` consulta ferramentas liberadas para seu contexto.
6. Ferramenta com historico de uso e descontinuada, nao excluida fisicamente.
7. A avaliacao sobre dados pessoais e obrigatoria antes da aprovacao.
8. Consultas e alteracoes sempre respeitam o isolamento por empresa.
9. Mudancas de status e risco devem ser auditaveis em evolucao futura.

## Fora do MVP

Custos detalhados, licenciamento, responsavel interno, liberacao por
departamento, chaves de API, historico completo de decisoes, versoes de modelo
e anexos de conformidade exigem casos de uso proprios. Segredos nunca devem ser
armazenados nesta entidade.

## Criterios de aceite

- campos de identificacao e governanca definidos;
- relacionamento com `Empresa` definido;
- enums e valores iniciais definidos;
- restricoes, validacoes e permissoes documentadas;
- escopo futuro separado do MVP;
- contrato suficiente para iniciar a SCRUM-54 sem decisoes de modelagem abertas.

## Entregaveis

- este arquivo-fonte versionavel;
- `SCRUM-52-Ferramenta-IA.pdf`, com a apresentacao completa da definicao.
