# SCRUM-150 - Planejamento técnico do HestIA

## 1. Objetivo

Organizar a construção do MVP do HestIA em incrementos pequenos, rastreáveis e
testáveis, respeitando a decisão arquitetural da ADR-001 e o fluxo de trabalho
definido para Jira e GitHub.

Este plano transforma as tarefas técnicas atuais em uma sequência de execução,
explicita suas dependências e define o que deve ser entregue para que cada uma
possa ser aceita.

## 2. Escopo do MVP

O MVP deve permitir que uma empresa governe o uso corporativo de ferramentas de
Inteligência Artificial. A primeira versão deverá oferecer, no mínimo:

- cadastro e consulta de empresas, departamentos e usuários;
- cadastro e consulta de ferramentas de IA vinculadas à empresa;
- cadastro e consulta de políticas de uso vinculadas à empresa;
- aplicação das permissões previstas para os perfis de usuário;
- criação e consulta de notificações;
- um cenário inicial de otimização documentado e reproduzível;
- aplicação executável, testada e demonstrável.

Não fazem parte deste planejamento a decomposição prematura em microserviços,
uma interface definitiva, alta disponibilidade, mensageria distribuída ou uma
solução de otimização em escala de produção.

## 3. Direção arquitetural

Conforme a ADR-001, o backend principal será um monólito modular em Java 21 com
Spring Boot e PostgreSQL. Cada módulo de negócio deve concentrar seus próprios
controllers, DTOs, serviços, entidades e repositórios.

Dependências de negócio permitidas no MVP:

```text
departamento ──> empresa
usuario ───────> empresa, departamento
politica ──────> empresa
ferramenta ────> empresa
```

Controllers devem chamar serviços, e serviços coordenam acesso a repositórios.
Não devem existir dependências circulares nem duas entidades canônicas para a
mesma tabela.

O serviço de notificações em Node.js solicitado nas tarefas SCRUM-109,
SCRUM-110, SCRUM-111 e SCRUM-159 será tratado inicialmente como um componente
auxiliar acadêmico, com contrato HTTP explícito e responsabilidade limitada.
Como um processo implantável separado diverge da decisão de "um único artefato"
da ADR-001, a SCRUM-159 deverá registrar uma destas decisões antes da
implementação:

1. manter Node.js como adaptador executado apenas para demonstração; ou
2. aprovar uma exceção arquitetural por meio de nova ADR.

## 4. Diagnóstico inicial do repositório

Levantamento realizado em `develop` em 1 de setembro de 2026:

- existem módulos Java para empresa, departamento, usuário e política;
- o módulo de ferramenta possui somente o enum de tipos e ainda não possui uma
  entidade canônica;
- existem duas entidades `PoliticaUso` mapeadas para a tabela
  `politicas_uso`, nos pacotes `politica` e `politicauso`;
- não existe serviço Node.js de notificações;
- não existem testes automatizados na branch `develop`;
- a classe de inicialização do Spring Boot e o teste de contexto existem em
  `main`, mas não em `develop`;
- `main` e `develop` estão divergentes: `develop` possui 30 commits exclusivos e
  `main` possui 4 commits exclusivos;
- não existe pipeline de integração contínua no repositório;
- o Maven Wrapper versionado não inicia corretamente no Windows na forma atual;
- não existe configuração versionada e segura para iniciar a aplicação com um
  banco de desenvolvimento.

Esses itens impedem considerar o MVP executável no estado atual e devem ser
tratados antes de ampliar o número de funcionalidades.

## 5. Ordem de execução

### Etapa 0 - Estabilizar a base

Objetivo: obter uma linha de desenvolvimento que compile, inicie e possa ser
testada por qualquer integrante.

Entregas:

- reconciliar `main` e `develop` por Pull Request, preservando a classe de
  inicialização e removendo artefatos de build versionados em `main`;
- corrigir ou regenerar o Maven Wrapper;
- adicionar configuração local por variáveis de ambiente, sem credenciais;
- adicionar teste mínimo de inicialização;
- criar pipeline para executar os testes a cada Pull Request;
- eliminar o mapeamento JPA duplicado de `PoliticaUso` em uma tarefa própria de
  correção, mantendo `br.com.hestia.politica` como módulo canônico.

Critério de saída: um clone novo do repositório executa os testes e inicia a
aplicação seguindo instruções documentadas.

### Etapa 1 - Consolidar a arquitetura

#### SCRUM-106 - Definir arquitetura inicial do MVP

Transformar a decisão da ADR-001 em uma visão operacional: módulos, limites,
dependências, responsabilidades, persistência, segurança e integrações.

Entregas:

- mapa dos módulos e suas responsabilidades;
- regras de dependência entre módulos;
- padrão de erros e validação;
- estratégia de configuração e persistência;
- registro da decisão sobre o componente Node.js.

Critério de aceite: uma nova funcionalidade pode ser posicionada em um módulo e
em uma camada sem ambiguidade.

#### SCRUM-105 - Criar documentação C4

Representar a arquitetura em três níveis:

- contexto: pessoas e sistemas que interagem com o HestIA;
- contêineres: aplicação Spring Boot, PostgreSQL e o componente Node.js, se
  aprovado;
- componentes: módulos internos do monólito e suas dependências.

Critério de aceite: os diagramas correspondem ao código e contêm legenda,
responsabilidades e relações.

#### SCRUM-159 - Definir arquitetura do backend em Node.js

Delimitar o componente de notificações, seu contrato HTTP, armazenamento,
configuração, observabilidade, testes e tratamento de falhas.

Critério de aceite: a SCRUM-109 pode ser implementada sem decisões arquiteturais
em aberto e a relação com a ADR-001 está documentada.

### Etapa 2 - Completar o núcleo do domínio

#### SCRUM-54 - Modelar entidade Ferramenta de IA

Criar a entidade canônica de ferramenta, ligada a uma empresa. Os campos devem
ser confirmados com as definições das SCRUM-52 e SCRUM-53 e cobrir identificação,
fornecedor, tipo, finalidade, versão ou URL, situação de aprovação, risco, custo
ou licença e datas de auditoria.

Critério de aceite: entidade, DTO, repository e migration possuem um único
contrato consistente, com validações e testes.

#### SCRUM-86 - Implementar cadastro de Empresa no MVP

Auditar e completar o CRUD já existente, em vez de criar uma segunda
implementação. A entrega deve conferir os campos definidos, unicidade da empresa,
validações, respostas de erro, relações e testes. O campo textual `politicas` da
entidade atual deve ser removido ou justificado, pois políticas já são um módulo
próprio.

Critério de aceite: criar, consultar e atualizar uma empresa funciona com dados
válidos; entradas inválidas e duplicadas produzem respostas previsíveis.

### Etapa 3 - Implementar notificações

#### SCRUM-109 - Implementar serviço básico de notificações em Node.js

Criar um serviço mínimo capaz de receber, armazenar e consultar notificações,
com validação, identificadores, estado, data, logs e testes automatizados.

#### SCRUM-110 - Integrar serviço Node.js ao backend principal

Criar no Spring Boot um adaptador para o contrato Node.js. URL, timeout e demais
configurações devem vir do ambiente. Falhas de conexão ou respostas inválidas
devem ser convertidas em erros controlados, sem expor detalhes internos.

#### SCRUM-111 - Validar envio e consulta de notificações

Executar testes de contrato e ponta a ponta para sucesso, dados inválidos,
indisponibilidade, timeout, consulta inexistente e isolamento por empresa. A
repetição da mesma solicitação não pode gerar efeitos inesperados.

Critério de saída da etapa: o backend principal cria e consulta uma notificação
em uma execução local reproduzível e os cenários de falha estão testados.

### Etapa 4 - Qualidade

#### SCRUM-118 - Documentar plano de qualidade

Consolidar atributos de qualidade, estratégia de testes, Definition of Done e
validações básicas produzidos nas tarefas relacionadas.

O plano deverá indicar:

- quais testes são obrigatórios por tipo de alteração;
- padrões mínimos para API, validação, segurança e logs;
- comando local e pipeline de CI;
- evidências exigidas no Pull Request e no Jira;
- responsáveis por revisão, teste e homologação;
- critérios objetivos para bloquear ou aprovar uma entrega.

Critério de aceite: a equipe consegue decidir de modo reproduzível se uma tarefa
está pronta.

### Etapa 5 - Modelo matemático

#### SCRUM-132 - Definir problema de otimização

Definir a decisão que o modelo apoiará. Hipótese inicial a validar: selecionar ou
alocar ferramentas/licenças de IA entre departamentos, respeitando orçamento,
políticas e limites de risco, para maximizar o benefício esperado.

#### SCRUM-134 - Criar cenário inicial de aplicação

Criar uma instância numérica pequena, com empresas ou departamentos,
ferramentas, custos, benefícios, orçamento e restrições. O resultado ótimo deve
ser verificável manualmente.

#### SCRUM-136 - Documentar modelo matemático

Consolidar índices, parâmetros, variáveis de decisão, função objetivo,
restrições, unidades, hipóteses, cenário e interpretação do resultado. Esta
tarefa depende da definição de variáveis e restrições da SCRUM-133 e da validação
da SCRUM-135.

Critério de saída da etapa: outra pessoa consegue reproduzir o cenário e explicar
por que a solução respeita todas as restrições.

### Etapa 6 - Fechar o MVP

#### SCRUM-77 - Desenvolver primeira versão do MVP

Esta é uma entrega agregadora, não uma única alteração de código. Deve reunir as
tarefas menores homologadas, estabilizar a integração e preparar uma versão
demonstrável.

Critérios mínimos:

- aplicação inicia com configuração documentada;
- fluxos centrais do domínio funcionam;
- notificações funcionam conforme o contrato aprovado;
- testes automatizados e pipeline estão verdes;
- documentação de execução e arquitetura está atualizada;
- não existem defeitos bloqueantes;
- Release PR de `develop` para `main` revisado e homologado;
- versão identificada por tag.

## 6. Dependências

```text
SCRUM-150
  └─> SCRUM-106
       ├─> SCRUM-105
       ├─> SCRUM-159 ─> SCRUM-109 ─> SCRUM-110 ─> SCRUM-111
       └─> SCRUM-118

SCRUM-52 + SCRUM-53 ─> SCRUM-54
campos/regras de Empresa ─> SCRUM-86
SCRUM-132 ─> SCRUM-133 ─> SCRUM-134 ─> SCRUM-135 ─> SCRUM-136

todas as entregas homologadas ─> SCRUM-77
```

As frentes de domínio, notificações e modelo matemático podem avançar em
paralelo depois da estabilização da base e da definição da arquitetura.

## 7. Estratégia de branches e entregas

Cada tarefa executável deve possuir uma branch própria criada a partir de
`develop`, por exemplo:

```text
docs/SCRUM-150-planejamento-tecnico
feature/SCRUM-54-modelar-ferramenta-ia
feature/SCRUM-109-servico-notificacoes-node
test/SCRUM-111-validar-notificacoes
```

O Pull Request deve ter `develop` como base, conter uma única tarefa principal,
explicar como reproduzir os testes e receber revisão humana. A SCRUM-77 não deve
gerar uma branch ampla com implementações misturadas; ela agrega entregas já
integradas e homologadas.

## 8. Riscos e respostas

| Risco | Impacto | Resposta planejada |
|---|---|---|
| Divergência entre `main` e `develop` | perda de arquivos e versão não executável | reconciliar branches antes de novas funcionalidades |
| Entidade de política duplicada | conflito de schema e comportamento indefinido | manter um modelo canônico e remover o duplicado |
| Node.js divergir da ADR-001 | arquitetura incoerente | delimitar componente e registrar exceção antes de implementar |
| Tarefas sem critérios de aceite | retrabalho e conclusão subjetiva | incluir entregáveis e critérios verificáveis antes do código |
| Ausência de testes e CI | regressões não detectadas | criar teste-base e pipeline na etapa de estabilização |
| Credenciais de banco no repositório | exposição de segredo | usar variáveis de ambiente e arquivo de exemplo sem valores reais |
| Senha de usuário armazenada em texto | incidente de segurança | criar tarefa bloqueante para hash e impedir retorno da senha na API |
| Relações JPA serializadas diretamente | ciclos e exposição de dados | usar DTOs de resposta e testes de contrato da API |

## 9. Definition of Done técnica

Uma tarefa de implementação só pode seguir para homologação quando:

- os critérios de aceite estão descritos e atendidos;
- o código respeita os limites dos módulos;
- testes relevantes foram criados e executados;
- o pipeline está aprovado;
- configurações e mudanças de contrato estão documentadas;
- não existem segredos nem artefatos locais versionados;
- branch, commits e Pull Request contêm a chave Jira;
- outra pessoa revisou a alteração;
- evidências de teste foram vinculadas à tarefa.

## 10. Próximas ações imediatas

1. revisar e aprovar este planejamento na SCRUM-150;
2. criar tarefa de estabilização da base ou vincular os itens da Etapa 0 a uma
   tarefa existente;
3. reconciliar `main` e `develop`;
4. executar a SCRUM-106 e registrar a decisão referente ao Node.js;
5. produzir os diagramas da SCRUM-105;
6. iniciar SCRUM-54 e SCRUM-86 somente sobre a base executável;
7. seguir a cadeia SCRUM-159, SCRUM-109, SCRUM-110 e SCRUM-111;
8. consolidar qualidade e modelo matemático;
9. homologar os incrementos e fechar a SCRUM-77 por Release PR.
