# SCRUM-114 e SCRUM-117 — Qualidade do MVP

## Atributos verificáveis nesta versão
- Navegação: links da sidebar devem abrir suas respectivas telas.
- Controle de acesso: páginas restritas devem bloquear perfis sem permissão.
- Entrada de dados: campos obrigatórios dos wireframes não devem aceitar envio vazio.
- Segurança visual: senha deve permanecer mascarada por padrão e possuir opção de mostrar/ocultar.
- Saída: o botão Sair deve remover o acesso atual e retornar à tela de login.
- Execução: o servidor deve iniciar sem erro de sintaxe e responder na porta configurada.

## Validações básicas implementadas
- `required` nos campos de cadastro apresentados no protótipo.
- `checkValidity()` / `reportValidity()` antes da ação demonstrativa de cadastro.
- Redirecionamento para `/acesso` quando não há identificação de acesso.
- Restrição das rotas conforme perfil definido no protótipo.

Não foram definidos nos materiais enviados valores numéricos de desempenho, disponibilidade ou cobertura; por isso este documento não cria metas artificiais.
