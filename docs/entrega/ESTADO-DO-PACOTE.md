# Pacote de preparação da entrega final

Base: f71021d, branch finalizacao, 12/11/2026. Ainda não é a release final aceita. O pacote preserva documentos existentes e referencia a rubrica levantada na 591; não reescreve entregáveis de outras disciplinas.

## Matriz de atendimento

| Área | Evidência localizada/preparada | Situação para fechamento |
| --- | --- | --- |
| Regras e fontes | DECISOES-SPRINT4-INTEGRACAO.md, 539 e dados/evidências demo | Produto definido, com ausências explícitas de custos/meta |
| RAG | 545/546, 551 e evidências da 612/correções | Aceite técnico local; 556 publicada pendente |
| Jornada integrada | SCRUM-568-aceite-jornada.md e roteiro/evidência | Aceite local aprovado |
| Indicadores/risco/exportação | Regras e cenários do Knorre; SCRUM-585-homologacao-parcial.md | Integração final das branches analíticas ainda pendente |
| Arquitetura/manual | docs/arquitetura/release-sprint4.md e complemento 612 | Conferir release aceita; recuperação de senha cancelada prevalece sobre descrição histórica de SMTP |
| Ajuda/perfis | Material e telas existentes; 595 pronta no Jira | Validar na release publicada |
| Apresentação | preparacao/HestIA-DemoDay-preparacao.pptx e roteiro 597 | Preparação editável, 14 slides/18 minutos; grupo ainda não ensaiou |
| Modelagem/otimização | Entregáveis existentes em docs/deliverables/Deliverable_2 e docs/optimization | D2 não foi reclassificado como D3; confirmar versões finais/modelos integrados |
| Marketing, estatística, ML, predição e qualidade final | Matriz docente da 591 e materiais do repositório | Conferir arquivos finais específicos, autoria e aceite por disciplina; não inventar resultados |
| Publicação e homologação final | Scripts/Compose/evidências locais | Usuário confirmou uso somente local; 590 não concluída |
| Ensaio/submissão | Checklist 598 | Ação humana com grupo/recibo ainda pendente |

## Conferência Canvas em 12/11/2026

Curso TI4A-Projeto Integrado IV (53394): o template 7646953 continua acessível no visualizador, com 20 minutos + 10 de arguição e participação de todos. Atividades mostra somente Deliverables I e II/correções; nenhum Deliverable III foi exibido. Avisos mostra somente a publicação de 14/10 sobre Deliverable II. Permanecem as divergências de datas já documentadas na 591. Não tratar o fim da sprint em 24/11 como prazo acadêmico confirmado.

## Empacotamento reproduzível

scripts/montar-pacote-entrega.cjs recebe um diretório de saída novo fora do repositório. Copia apenas arquivos versionáveis escolhidos de docs e produz manifesto SHA-256. Não inclui .env, banco/dump, logs privados, credenciais ou saídas de compilação. O ZIP de preparação não representa comprovante de submissão. Após fechar as dependências, regenerar da release aceita e comparar a matriz antes de usar como pacote final.

## Atualização de integração

Base atual de código: develop 3760a40, incorporada à branch finalizacao. PRs 154/153/149 integrados, com compatibilidade local conferida. AI Literacy e histórico de autorização já entraram; 578 e contratos restantes de ranking/risco/comparabilidade seguem pendentes no recorte examinado. Pacote anterior de 151 arquivos é um snapshot de preparação; regenerar com o script para incluir os novos relatórios -v29. Não é entrega acadêmica final.
