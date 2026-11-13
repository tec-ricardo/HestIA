# SCRUM-599 — Configuração inicial e retomada

## Implementação

A tela `/configuracao-inicial`, disponível no menu do administrador, consulta cadastros persistidos e oferece links para os módulos existentes. Verifica empresa, departamento, usuários ativos dos três perfis, ferramenta aprovada e política ativa não excluída, sempre na empresa autenticada.

GET `/empresas/{empresaId}/configuracao-inicial` não cria registros. PUT no mesmo endereço reutiliza ConfiguracaoEmpresaService para persistir os parâmetros. O formulário carrega os valores salvos, incluindo limite vazio e opções desmarcadas. A rota de gravação do frontend valida o token de proteção do formulário e obtém a empresa da sessão, sem aceitar empresa do navegador.

A presença dos cadastros não prova a adequação ao piloto. O catálogo atual é fictício; a tela mantém o conteúdo real como não verificado até a integração da SCRUM-549. A revisão dos parâmetros continua disponível, sem inferir aprovação por simples consulta.

## Verificação manual

1. Entrar como administrador de uma organização de teste.
2. Abrir Configuração inicial e verificar pendências contra os cadastros.
3. Salvar limite, retenção e opções; sair e entrar novamente.
4. Confirmar que os mesmos valores reaparecem.
5. Concluir um cadastro pelo módulo existente e voltar; a pendência deve atualizar.
6. Reabrir a tela várias vezes e conferir que nenhum cadastro foi criado pela consulta.
7. Tentar acessar como colaborador; o acesso deve ser recusado.
8. Interromper a API em ambiente de teste; a tela deve informar indisponibilidade, sem inventar pendências.

## Testes adicionados

- ConfiguracaoInicialControllerTest: rejeita outra empresa antes de consultar/gravar; consulta sem efeitos colaterais e sem declarar catálogo fictício como real.
- configuracao-inicial.test.js: retomada, salvamento no escopo autenticado, proteção do formulário, consulta indisponível e perfil sem acesso.

## Pendências para aceite integrado

Validar a tela com dados autorizados do piloto e conectar a verificação do catálogo real quando SCRUM-549 for entregue. Não marcar a história integrada como concluída apenas com estes testes.

## Resultado automatizado — 10/11/2026

Frontend: 24 testes aprovados. Java: 306 testes contabilizados, 25 ignorados, zero falhas e zero erros, executados em diretório de build novo. A saída target antiga contém V11__perfis_recuperacao_respostas.sql, ausente das fontes atuais; fazer uma compilação limpa antes de iniciar a API. A validação visual/manual do piloto permanece pendente.


## Atualização — 12/11/2026

A verificação do catálogo persistido foi conectada: conteúdo PUBLICADO da empresa conta como disponível. A tela foi validada por HTTP e visualmente em banco descartável. Os resultados atuais e limites de aceite estão em SPRINT4-revisao-commit-merge.md; os números de 10/11 acima são históricos.
