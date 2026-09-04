# SCRUM-118 - Plano de qualidade do HestIA

## Objetivo

Definir criterios objetivos para aceitar uma entrega do MVP, combinando
qualidade de codigo, testes, seguranca, operacao e evidencias no Jira/GitHub.

## Atributos prioritarios

| Atributo | Meta do MVP | Evidencia |
|---|---|---|
| Correcao | regras e validacoes reproduziveis | testes automatizados |
| Seguranca | nenhum segredo ou senha em resposta/log | revisao e testes de contrato |
| Confiabilidade | falha do Node nao corrompe o monolito | testes de indisponibilidade |
| Manutenibilidade | modulo nao acessa repository de outro modulo | revisao arquitetural |
| Portabilidade | clone novo compila e executa | README, Compose e CI |
| Rastreabilidade | branch, commit e PR citam a chave Jira | verificacao do PR |

## Piramide de testes

1. Testes unitarios para regras, validadores e servicos.
2. Testes de persistencia para entidades, consultas e restricoes.
3. Testes de contrato para APIs e integracoes.
4. Testes ponta a ponta somente para fluxos centrais.

Toda correcao de defeito deve incluir teste que falhava antes da correcao.

## Matriz minima por alteracao

| Alteracao | Testes obrigatorios |
|---|---|
| entidade/repository | persistencia, restricoes e isolamento por empresa |
| regra de negocio | unitario de sucesso e falha |
| controller/DTO | validacao, status HTTP e ausencia de dados sensiveis |
| integracao externa | sucesso, timeout, indisponibilidade e resposta invalida |
| Node.js | validacao, idempotencia, consulta e health check |
| modelo matematico | instancia pequena e verificacao independente |
| documentacao | links, comandos e coerencia com o codigo |

## Padroes de API

- DTOs de entrada e saida separados;
- validacao na borda e regra no servico/dominio;
- erros com codigo, mensagem e data, sem stack trace externo;
- recursos isolados por `empresaId`;
- endpoints novos versionados em `/api/v1`;
- operacoes repetiveis usam chave de idempotencia quando houver efeito externo.

## Seguranca e privacidade

- segredos somente por variavel de ambiente;
- senha armazenada com hash e nunca devolvida;
- log sem credencial, token ou conteudo pessoal;
- autorizacao por perfil e empresa;
- dependencia nova passa por auditoria de vulnerabilidades;
- chaves de API nunca sao persistidas na entidade `FerramentaIA`.

## Pipeline minimo

O Pull Request deve executar:

1. build e testes Maven com Java 21;
2. `npm ci` e `npm test` quando o Alert Service for alterado;
3. verificacao de formatacao/compilacao;
4. testes ponta a ponta para a branch de integracao;
5. auditoria de dependencias sem vulnerabilidade critica.

## Definition of Done

- criterios do card atendidos;
- codigo e documentacao na branch correta;
- testes locais e pipeline verdes;
- nenhuma credencial ou artefato de build versionado;
- API e configuracao documentadas;
- revisao humana aprovada;
- evidencias anexadas ao PR e vinculadas ao Jira;
- defeito bloqueante inexistente;
- homologacao registrada quando a tarefa altera comportamento visivel.

## Bloqueadores de aprovacao

- build ou teste vermelho;
- senha/segredo exposto;
- ausencia de teste para regra critica;
- acesso cruzado entre empresas;
- mudanca de contrato sem documentacao;
- duplicidade de entidade/tabela canonica;
- branch sem chave Jira;
- resultado nao reproduzivel em clone limpo.

## Responsabilidades

| Papel | Responsabilidade |
|---|---|
| Autor | implementar, testar e anexar evidencias |
| Revisor | validar codigo, arquitetura e riscos |
| QA/homologador | reproduzir criterios de aceite |
| Scrum Master | garantir rastreabilidade e remover impedimentos |
| Product Owner | aceitar o comportamento entregue |

## Evidencias esperadas

- comandos executados e resumo dos resultados;
- captura ou resposta HTTP quando aplicavel;
- lista de arquivos alterados;
- riscos conhecidos e itens fora do escopo;
- link do PR e dos materiais arquiteturais;
- resultado da demonstracao para entregas do MVP.
