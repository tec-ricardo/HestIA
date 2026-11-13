# Fonte e cadastro de Colaboração para a demo

Versão: colaboracao-demo-1.0, 12/11/2026. Luccas autorizou PRs do projeto HestIA como fonte da demonstração, separados dos dados QSoft. Os eventos vêm do GitHub; os vínculos de empresa/departamento são hipóteses didáticas autorizadas. Não afirmar vínculo empregatício ou identidade civil a partir de um login.

## Fonte congelada

dados/colaboracao-github-demo.json contém somente os metadados necessários: número/URL do PR, merge/SHA, GitHub ID/login do autor, aprovações observadas e limitações. Período 03 a 12/11/2026, fuso America/Sao_Paulo, até coletadoEm. A Sprint 4 vai de 03 a 24/11; esta coleta não cobre o seu encerramento nem acontecimentos posteriores ao instante da coleta. Paginação de PRs fechados foi conferida até antes do início; 25 merges foram selecionados. Todos os 25 têm reviews consultados. Não contém tokens, corpo de PR, comentários ou credenciais.

| Login observado | GitHub ID |
| --- | --- |
| GKnorre | 199632675 |
| kevinnleee | 199633216 |
| tec-ricardo | 199632860 |
| toffan0 | 189694698 |

São quatro contas observadas, sem atribuição automática a nomes civis. O script registra usuários históricos desativados, identificados pelo GitHub ID; não cria logins funcionais nem fornece senha.

## Cadastro no banco demo de cada integrante

Empresa exclusiva: Equipe HestIA - GitHub (DEMO), CNPJ fictício reservado 99999999000272, orçamento ausente. Departamento único: Desenvolvimento GitHub (DEMO). Esta escolha é de organização da demonstração, não reconstrução de departamentos reais. A empresa QSoft demo existente não recebe os PRs.

O script exige alvo com nome demo/validação, confirmação explícita --confirmar-demo e a carga de referência DemoDataInitializer já existente. Ele cria a empresa/departamento/autores em transação, com trava de concorrência, e recusa identidades reservadas divergentes em vez de mover contas existentes. Executar somente em banco demo, com acesso Docker/PostgreSQL autorizado.

No diretório do projeto, em uma única linha:

node docs/scrum/publicar-colaboracao-demo.cjs --container NOME_CONTAINER_DEMO --banco NOME_BANCO_DEMO --usuario USUARIO_POSTGRES --entrada docs/scrum/dados/colaboracao-github-demo.json --saida CAMINHO_FORA_DO_GIT/vinculos-github.json --confirmar-demo

Usar valores reais do ambiente; não são senhas. O resultado contém vinculos (GitHub ID/login → usuarioId → empresaId/departamentoId) e pullRequests no formato do record ApuracaoColaboracao.PullRequest da 576: repositorio, numero, dataMerge, empresaId, departamentoId, autorId, quantidadeRevisoes. Os campos adicionais evidencia e autorGithubId preservam rastreabilidade; mapear explicitamente os sete campos ao record, sem desserializar cegamente JSON com campos adicionais.

Chamar ApuracaoColaboracao.calcular com empresaId/departamentoId retornados, período do arquivo e somente esse conjunto. Não converter GitHub ID em usuarioId diretamente. Os IDs de evidencias/colaboracao-github-vinculos-validacao.json são exclusivos do container descartável do ensaio; **não importá-los** em outro banco. A geração local foi repetida com os mesmos quatro IDs de usuário, sem duplicar cadastros. Em outro ambiente, executar novamente e usar os novos IDs retornados.

Os autores desativados representam autoria histórica; não são contas aptas a login ou uma população de colaboradores ativos para AI Literacy/Competências. A lista retornada é a fonte de vínculos desta demo da 576, não um cadastro automático de pessoal QSoft.

## Revisões e cálculo

PR válido: merge no período, número/repositório válidos, autor com vínculo único, revisão conhecida. Deduplicar por repositório+número; divergências devem ser excluídas e informadas. Revisão elegível nesta demo: aprovação APPROVED por outra conta antes do merge. Dismissed/comentários/revisão do próprio autor não contam. Zero retornado após consulta bem-sucedida é ausência observada; null significa review não verificado e reduz cobertura. O fato de outra pessoa fazer o merge não comprova aprovação formal.

Na coleta: 25 candidatos, 25 válidos, 0 revisados, cobertura 100%, nota descritiva 0, amostra <30 → FRAGIL. A fórmula mede aprovações formais nos PRs observados, não toda forma de colaboração ou benefício da plataforma. Não inserir aprovações artificiais para melhorar a nota.

Como a empresa/departamento são didáticos, o conjunto deve continuar identificado EVENTOS_GITHUB_REAIS_COM_VINCULOS_DEMO na UI/relatório, separado da apuração operacional da QSoft. Não misturar snapshots ou ocultar essa natureza para liberar score global.

## Recoletar

node docs/scrum/coletar-github-demo.cjs CAMINHO_NOVO/coleta-github.json

O coletor desta versão tem o período congelado 03–12/11/2026; mudar o período exige nova coleta/versionamento, mantendo o arquivo anterior. Usa API pública sem credencial. Limites/erros de API não são substituídos por zero; preservar limitações do arquivo. Não sobrescreve uma saída existente.

## Acesso à empresa exclusiva de demonstração

Para navegar autenticado nessa empresa sem mover contas QSoft, acrescentar --criar-admin-demo ao comando do publicador. A opção cria admin.github.demo@hestia.local com perfil ADMIN no departamento GitHub demo, copiando somente o hash da conta admin.demo@hestia.local já existente no banco demo. A senha continua a mesma da conta administrativa demo já documentada; o script não imprime nem grava senha em relatório. Não utiliza ou altera credenciais reais. Os quatro autores históricos continuam desativados. A opção é explícita e exclusiva do ensaio; identidade reservada divergente faz a transação falhar.

A criação opcional e o login/contexto foram exercitados no container descartável: empresa e departamento correspondem ao cadastro retornado. O script não liga automaticamente a fonte à rota de maturidade: a 576 deve consumir a lista pullRequests com esse escopo.
