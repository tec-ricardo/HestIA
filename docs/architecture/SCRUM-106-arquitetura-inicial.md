# SCRUM-106 — Arquitetura inicial do MVP HestIA

## 1. Decisão arquitetural

O MVP será desenvolvido como um **monólito modular** em Java 21 e Spring Boot,
com camadas internas por módulo de negócio, PostgreSQL compartilhado e um
**Alert Service** auxiliar em Node.js integrado por REST síncrono.

A decisão complementa a ADR-001. O processo Node.js não representa a
decomposição do núcleo em microserviços: ele é um componente especializado para
avaliar ou registrar alertas. Uma evolução para serviços independentes só deve
ocorrer quando um requisito arquitetural justificar autonomia de implantação,
dados ou escala.

## 2. Contexto e necessidades

O HestIA apoia governança, monitoramento e uso responsável de Inteligência
Artificial em empresas.

| Ator | Necessidade principal |
|---|---|
| Colaborador | Consultar políticas, registrar uso e acompanhar recomendações. |
| Gestor/Líder | Acompanhar consumo, risco, produtividade e evolução da equipe. |
| Executivo | Analisar maturidade, retorno, governança e evolução organizacional. |
| Administrador | Gerenciar empresas, departamentos, usuários, ferramentas e políticas. |

## 3. Requisitos arquiteturalmente significativos

As metas abaixo são propostas para o MVP e precisam ser confirmadas com os
requisitos funcionais da equipe.

| Atributo | Cenário ou medida proposta | Consequência arquitetural |
|---|---|---|
| Segurança | Senha nunca é persistida ou retornada em texto aberto; acesso é limitado à empresa. | Hash, DTOs de resposta, autorização e auditoria. |
| Manutenibilidade | Um módulo não acessa o repositório interno de outro. | Fachadas públicas e testes de arquitetura. |
| Confiabilidade | Falha do Alert Service não corrompe o registro principal. | Timeout, erro controlado e alerta pendente. |
| Desempenho | CRUDs respondem em até 2 segundos no ambiente de demonstração. | Consultas simples, índices e limites de paginação. |
| Testabilidade | Casos de uso críticos possuem testes automatizados e pipeline. | Separação de responsabilidades e configuração de teste. |
| Portabilidade | A aplicação é reproduzível em outra máquina da equipe. | Docker Compose, variáveis de ambiente e health checks. |
| Evolução | Um módulo só é extraído quando um ASR justificar. | Contratos explícitos e baixa dependência entre domínios. |

## 4. Alternativas consideradas

| Alternativa | Benefício | Custo ou risco | Decisão |
|---|---|---|---|
| Monólito em camadas globais | Simplicidade inicial. | Mistura domínios e aumenta acoplamento. | Rejeitada. |
| Monólito modular | Limites de domínio, testes e baixo custo operacional. | Exige disciplina sobre dependências. | Adotada. |
| Microsserviços | Escala e implantação independentes. | Complexidade operacional e dados distribuídos. | Adiada até existir ASR. |

## 5. Módulos de negócio

| Módulo | Responsabilidade | Estado observado |
|---|---|---|
| `empresa` | Empresa, configurações corporativas e orçamento. | Parcialmente implementado. |
| `departamento` | Estrutura organizacional vinculada à empresa. | Parcialmente implementado. |
| `usuario` | Identidade, perfil, empresa e departamento. | Parcial; segurança é bloqueante. |
| `politica` | Políticas, versões, situação e vínculo empresarial. | Implementado com duplicidade a corrigir. |
| `ferramenta` | Catálogo de ferramentas/modelos, risco, aprovação e custo. | Somente enum; implementação pendente. |
| `consumo` | Uso, custo, tokens e resultado de avaliação. | Planejado. |
| `indicadores` | Métricas e maturidade organizacional. | Incremental; primeiro painel no protótipo. |
| `notificacao` | Orquestração de alertas e consulta de eventos. | Gateway Spring e serviço Node planejados. |

## 6. Regras de modularidade

1. Toda funcionalidade pertence a um módulo de negócio explícito.
2. Controller chama caso de uso/fachada, e não repository diretamente.
3. Repository pertence ao módulo responsável pela entidade.
4. Um módulo usa outro apenas por contrato público.
5. Uma entidade JPA possui um único modelo canônico.
6. DTOs de entrada e saída protegem o domínio e os dados sensíveis.
7. Dependências circulares são proibidas; mudanças de limite exigem ADR.

Estrutura recomendada:

```text
br.com.hestia
├── HestiaApplication
├── empresa
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── departamento
├── usuario
├── politica
├── ferramenta
├── consumo
├── notificacao
└── shared       # somente recursos técnicos neutros
```

## 7. Arquitetura de execução

![Contêineres propostos para o MVP](diagrams/c4-conteineres.png)

O [arquivo-fonte Mermaid](diagrams/c4-conteineres.mmd) permite atualizar o
diagrama junto com a arquitetura.

### Integração Spring Boot e Node.js

| Aspecto | Decisão do MVP |
|---|---|
| Protocolo | HTTP REST síncrono com JSON. |
| Chamada | A API Spring chama o Alert Service depois de validar o caso de uso. |
| Entrada | Empresa, origem, tipo, valor e contexto necessário. |
| Saída | Nível (`INFO`, `WARNING` ou `CRITICAL`) e mensagem. |
| Falha | Timeout curto, erro controlado, correlação e possibilidade de alerta pendente. |
| Saúde | `/health` no Node e health check no Docker Compose. |
| Evolução | Persistência própria ou mensageria somente se um ASR justificar. |

## 8. Persistência e dados

- PostgreSQL é compartilhado pelo monólito no MVP.
- Cada tabela possui um módulo proprietário.
- Relações carregam `empresa_id` para garantir segregação organizacional.
- Migrações são versionadas; atualização destrutiva automática não é permitida.
- `PoliticaUso` deve possuir uma única entidade canônica no módulo `politica`.
- Logs não contêm senha, token, conteúdo sensível ou credenciais.

## 9. API, erros e segurança

| Tema | Padrão |
|---|---|
| URLs | Recursos no plural; `/api/v1` quando os contratos estiverem estabilizados. |
| DTOs | Separar entrada, saída e integrações; nunca retornar senha. |
| Validação | Jakarta Validation na API e regras na aplicação/domínio. |
| Erros | Código, mensagem, campos, `timestamp` e `correlationId`. |
| Autenticação | Spring Security; hash de senha é requisito bloqueante. |
| Autorização | Perfis `FUNCIONARIO`, `GESTOR` e `ADMIN`, sempre por empresa. |

## 10. Testes, observabilidade e operação

- testes unitários para domínio e casos de uso;
- testes de integração para JPA, banco e contratos REST;
- testes arquiteturais para dependências entre módulos;
- logs estruturados com `correlationId`, sem dados sensíveis;
- health checks da API, PostgreSQL e Alert Service;
- pipeline de Pull Request executando build e testes.

## 11. Lacunas entre o código e a arquitetura-alvo

| Lacuna atual | Correção | Prioridade |
|---|---|---|
| Duas entidades `PoliticaUso` | Manter um modelo canônico no módulo `politica`. | Bloqueante. |
| Services acessam repositories de outros módulos | Criar `EmpresaFacade` e `DepartamentoFacade`. | Alta. |
| Senha em texto aberto | Aplicar hash e DTO de resposta sem senha. | Bloqueante. |
| Inicialização/configuração não consolidada em `develop` | Reconciliar branches e documentar variáveis. | Bloqueante. |
| Testes e CI insuficientes | Criar teste-base, testes por módulo e pipeline. | Alta. |
| Node.js ausente | Implementar somente após aprovação do contrato. | Após a arquitetura. |

## 12. Critérios de aceite

- estilo, módulos, responsabilidades e dependências estão definidos;
- ASRs e trade-offs estão documentados;
- Spring Boot, PostgreSQL e Node.js possuem papéis e tratamento de falha;
- componentes existentes, planejados e futuros são diferenciados;
- a SCRUM-105 pode derivar os diagramas sem decisões essenciais pendentes;
- lacunas do código possuem prioridade de correção.

## Referências

- Canvas ESPM — Entregáveis 1, atividade 293213.
- LELES, Andréia Damasio. *Introdução à Arquitetura de Software — C4_v1*.
- LELES, Andréia Damasio. *Monólitos em Camadas x Monólito Modular x Microsserviços*.
- LELES, Andréia Damasio. *Integração do monólito com Node.js via REST*.
- HestIA — ADR-001: monólito modular com camadas internas.
- Jira — SCRUM-106, consultada em 2 de setembro de 2026.
