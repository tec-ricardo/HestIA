# SCRUM-77 - Candidato da primeira versao do MVP

## Incrementos agregados

- base Spring Boot executavel e testavel;
- entidade e persistencia de Ferramenta de IA;
- cadastro de Empresa com CNPJ e respostas seguras;
- arquitetura, implementacao e integracao do Alert Service;
- validacao ponta a ponta de notificacoes;
- plano de qualidade;
- problema, cenario, solucionador e documentacao do modelo matematico.

## Verificacoes do candidato

- Maven Wrapper inicia no Windows;
- testes Java executam com banco H2 isolado;
- testes Node cobrem criacao, consulta, validacao e idempotencia;
- teste ponta a ponta conecta Java e Node;
- teste matematico reproduz beneficio otimo 19;
- Compose inicia PostgreSQL e Alert Service;
- pipeline executa Java, Node e Python separadamente.

## Pendencias para a entrega oficial

Esta branch e um candidato tecnico. A SCRUM-77 so pode ser marcada como
concluida depois de:

1. revisao humana das branches componentes;
2. integracao aprovada em `develop`;
3. homologacao dos fluxos no navegador;
4. correcao de qualquer defeito bloqueante encontrado;
5. Release PR de `develop` para `main`;
6. criacao da tag de versao.

Essas etapas alteram branches protegidas e representam aceite da equipe; nao
devem ser automatizadas antes da revisao.
