# Sprint 4 — onde conferir cada tarefa

Projeto: HestIA. Base: develop 4617f77, com alterações locais ainda sem publicação. Não considerar as 18 tarefas encerradas a partir deste documento. Os testes automatizados comprovam cenários técnicos específicos; aceite de conteúdo e operação externa continuam separados.

## Erro do GitHub Desktop

O pull estava impedido porque sobrescreveria três arquivos alterados. As alterações foram preservadas em stash, develop foi atualizada e os conflitos de server.js e menu.ejs foram conciliados. O stash codex-preservacao-2026-11-12-antes-atualizacao foi mantido. Não aplicar novamente esse stash sobre as alterações atuais, pois elas já foram recuperadas. Revise os arquivos na aba Changes antes de criar um commit.

## Conferência por tarefa

| Jira | Onde mexer / passo a passo | Situação e limite |
| --- | --- | --- |
| SCRUM-544 | Na recuperação de respostas, confirme uma reutilização; repita a mesma confirmação e confira que não soma benefício duas vezes. Código recebido de origin/develop em integracoes-sprint.js e serviços existentes. | Implementação recebida e coberta pelos testes existentes de confirmação/reuso. |
| SCRUM-548 | Entre como ADMIN, altere perfil/departamento/atividade de uma conta com sessão aberta; a conta deve precisar entrar novamente. UsuarioService e SessaoUsuarioRepository. | Revogação implementada; alteração só de nome preserva sessão. |
| SCRUM-549 | ADMIN → Catálogo de treinamentos → cadastre fonte, autorização, objetivo, público, conteúdo e cinco questões. Fora de demo, não há catálogo fictício como fallback. | Persistência e integração implementadas; a equipe ainda precisa fornecer/homologar conteúdos reais. |
| SCRUM-550 | Confira como ADMIN, gestor e colaborador. Apenas ADMIN altera curadoria/configuração; gestor exporta seu departamento; cada empresa consulta seus próprios dados. | Restrições acrescentadas e verificadas nos cenários cobertos; revisar matriz de acesso com o grupo. |
| SCRUM-554 | Siga SPRINT4-operacao.md, usando Java 21, Node 22 e PostgreSQL 17/pgvector, ou os Dockerfiles. Compile em pasta limpa para não carregar migrações antigas de target. | Configuração local e imagens preparadas; não usar dados demonstrativos como entrega operacional. |
| SCRUM-555 | Escolher hospedagem; configurar variáveis de compose.release.yaml, proxy HTTPS e domínio; publicar depois de conferir imagens. | Pendente: usuário confirmou somente localhost; nenhum ambiente externo foi contratado/publicado. |
| SCRUM-557 | Registre uso e abra Avaliações automáticas; confira registro, estado, política preservada e justificativa. AvaliacaoAutomaticaService. | Regra 2.1 e matriz persistida: critérios atendidos, não atendidos e não verificados; conferência autorizada por critério no registro; fontes e datas preservadas. Aguardando validação final integrada. |
| SCRUM-558 | Na tela Avaliações automáticas, gestor/ADMIN usa Tentar novamente em pendência/falha técnica. Resultado concluído é preservado. | Retentativa e histórico implementados; não é reavaliação de mérito. |
| SCRUM-560 | Abra o título do rascunho para conferir conteúdo/prova. Envie à revisão, registre evidência e publique. Retire com motivo. Para alteração de conteúdo, crie nova versão indicando a referência anterior. | Fluxo de estados implementado; homologação humana de fonte e conteúdo permanece necessária. |
| SCRUM-562 | Confira efeitos pelo registro de origem: tratamento de não conformidade, trilhas e recompensas existentes. EfeitosAvaliacaoService e MotorTrilhasService. | Integração implementada; aceite completo depende das evidências de 557 e recomendações de 561. Não há aprovação fictícia para gerar XP. |
| SCRUM-574 | Ciência de políticas → leia a versão → confirme. Repita e confira que não duplica. Nova revisão requer nova ciência; histórico anterior continua acessível. | Implementado; ciência não equivale a conformidade. |
| SCRUM-583 | Relatório de usos → escolha período/departamento/ferramenta/finalidade → Aplicar filtros → Exportar este resultado em CSV. | Exporta usos, resultados de maturidade e comparação de apurações existentes com os mesmos filtros/permissões. Integração final com risco/economia de 580/581/582 continua dependente dessas tasks, que estão A fazer no Jira. |
| SCRUM-587 | .github/workflows/release-check.yml testa e empacota imagens com SHA do commit. Após commit/push, conferir execução no GitHub Actions. | Pipeline preparado, não executado no GitHub; publicação no host depende de 555. |
| SCRUM-588 | scripts/validar-restauracao.ps1 gera backup e restaura em container isolado; conferir contagens e hash. | Ensaio local separado do banco do usuário; falta ensaio no futuro ambiente de entrega e retenção externa. |
| SCRUM-589 | Consulte /health do frontend e /actuator/health da API. Use procedimentos de diagnóstico em SPRINT4-operacao.md. | Saúde e procedimentos locais; monitoramento externo depende de 555. |
| SCRUM-592 | docs/arquitetura/release-sprint4.md e SPRINT4-operacao.md descrevem containers, contratos, migrações e recuperação. | Documentação do estado local; atualizar SHA/evidências quando houver release publicada. |
| SCRUM-599 | ADMIN → Configuração inicial: confira pendências, ajuste parâmetros e retome a tela. Faça cadastros pelos módulos existentes. | Fluxo implementado sem recriar cadastros. Catálogo publicado conta como conteúdo disponível. |
| SCRUM-600 | Recuperar senha no login. Para envio real, configurar servidor SMTP, remetente, credenciais no ambiente e URL HTTPS. | Implementação de entrega e testes com remetente simulado; envio real pendente de provedor. |

## O que falta fornecer

A equipe precisa escolher hospedagem/domínio e servidor de e-mail, fornecer conteúdos autorizados e concluir contratos de evidência/regras e relatórios dependentes. Não enviar senhas ou chaves no chat; guardar no ambiente. Nenhuma task foi marcada como concluída no Jira e nenhum commit foi enviado ao GitHub nesta etapa.

## Atualização da revisão final

A develop foi atualizada até dd9259e sem perder alterações locais; o conflito de application-demo.properties preservou RAG e catálogo demo. A recuperação de senha ganhou bloqueio transacional para consumo único concorrente, revogação de sessões após troca e resposta pública uniforme em indisponibilidade de envio. Há novos relatórios em /relatorios-maturidade, acessíveis a partir do Relatório de usos. Hospedagem e e-mail foram confirmados pelo usuário como não definidos.

## Evidências por critério — regra 2.1

No registro de uso, gestor ou ADMIN pode expandir Conferência de evidências pelo responsável e registrar estado e referência para finalidade, categorias de dados/controles, revisão humana e LGPD. Referências devem identificar regra da política e contexto verificado, sem dados confidenciais. O servidor confere autoria, perfil e escopo. Colaborador não pode inserir conferência conclusiva. Os demais critérios vêm do registro, da revisão da política com hash e do status contemporâneo da ferramenta. Para uso realizado agora, usar horário do salvamento; o cadastro retrospectivo não comprova status histórico. Em Avaliações automáticas → Consultar critérios, evidências e tentativas é possível conferir os fundamentos preservados. A conferência não é parecer jurídico nem análise automática de conteúdo confidencial. Alterar evidências depois do resultado exige fluxo separado de reavaliação; repetir processamento técnico preserva o resultado original.

## Economia acumulada

No Relatório de usos, ADMIN pode abrir Consultar e exportar economia acumulada. A consulta e o CSV usam o serviço existente da empresa. Economia monetária indisponível permanece explicitamente indisponível; não há filtragem de período/departamento no acumulado. Filtros não suportados são recusados.
