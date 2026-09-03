# SCRUM-101 — Principais decisões arquitetônicas

## Estrutura atual
- Aplicação monolítica em módulos de interface, executada em Node.js.
- Express é responsável pelas rotas HTTP.
- EJS é responsável pela renderização das telas.
- CSS e JavaScript do navegador ficam em `public/`.
- As telas ficam em `views/`.
- `server.js` concentra as rotas e o controle de acesso do protótipo.

## Navegação
As rotas existentes são: `/`, `/maturidade`, `/creditos`, `/utilizacoes`, `/competencias`, `/governanca`, `/custos`, `/administracao`, `/acesso` e `/sair`.

## Controle de acesso atual
O protótipo diferencia colaborador, gestor e administrador. A implementação atual usa cookies para manter o perfil durante a navegação. Isso é adequado apenas para demonstração visual; não substitui autenticação persistida em banco de dados.

## Persistência
O MVP visual ainda não possui banco de dados. Os botões de cadastro validam os campos, mas não gravam registros.
