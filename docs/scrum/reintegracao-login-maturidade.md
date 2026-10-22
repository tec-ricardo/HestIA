# Reimplementação da integração de login e maturidade — 22/10/2026

Base: develop, commit 81ba088. Branch: fix/reintegracao-login-maturidade.

Implementação nova, sem checkout de arquivos históricos. O contrato Java de autenticação e indicadores foi mantido.

## Comportamento

- E-mail real usa POST /auth/login. A sessão local é regenerada antes da autenticação. O token permanece no servidor, sem ser renderizado na página.
- Os acessos com sufixos @adm, @gestor e @colaborador continuam demonstrativos e não recebem token da API.
- GET /maturidade consulta GET /indicadores/confiabilidade com token, período e visão. A empresa e o usuário são determinados pelo backend. IDs enviados pelo navegador não são encaminhados.
- A tela usa o menu, cabeçalho, estilos e cartões do projeto. Exibe nota, confiança, cobertura, completude, amostra, motivos, fonte, fórmula e versões. Ausência de dados é diferente de nota zero. A nota global não é substituída pela parcial.
- O layout recuperado por Kevin permanece em /maturidade/demonstracao, com aviso explícito de dados fictícios. As notas demonstrativas não aparecem como resultado da consulta real.
- GET /sair tenta revogar o token na API e encerra a sessão local mesmo quando a API está indisponível.

## Conferência manual

1. Executar npm ci e npm start em Front-End/hestia-prototipo-sprint1-atualizado, com API_URL apontando para o backend Java.
2. Entrar com conta real já cadastrada e abrir /maturidade. Alterar período e visão conforme permissões.
3. Conferir valores e justificativas contra a resposta de /indicadores/confiabilidade. Dados ausentes não podem virar zero.
4. Com API indisponível, conferir a mensagem de indisponibilidade sem notas de exemplo.
5. Sair e tentar reabrir /maturidade: deve exigir login.
6. Entrar com acesso demonstrativo: a página principal informa que é necessária conta real. O link de demonstração abre a interface anterior com identificação de dados fictícios.

## Validação executada

Seis testes Node aprovados, zero falhas. Incluem requisições HTTP reais ao frontend com API simulada, login recusado, renderização para os três perfis, escape de conteúdo, zero versus ausência, isolamento dos parâmetros, expiração e logout com falha do serviço remoto. Sintaxe de server.js e git diff --check aprovados.

Esses testes verificam a integração do frontend; não comprovam disponibilidade do Java ou existência de contas operacionais. Nenhuma conta foi criada e nenhum banco foi alterado. Não houve alteração de backend ou de migrações.

## Entrega

Alterações aplicadas e verificadas via Desktop Commander no repositório original C:\Users\luccas.toffano\OneDrive - ESPM\Projeto4\ProjetoGit\HestIA, branch fix/reintegracao-login-maturidade criada desde origin/develop 81ba088. Sem commit, push ou PR. A branch anterior com as SCRUM-372/373/503/529/534 permanece preservada.

Esta correção recompõe a integração relacionada às SCRUM-427 a 430; não conclui as validações bloqueadas 465, 487, 493, 498, 513 ou 523.
