# Definições da Sprint 4 para integração

Versão: decisoes-sprint4-1.2. Data: 12/11/2026. Responsável pelo aceite de produto: Luccas Toffano. Branch: correções. Base: 096a06a. Implementação local complementar, aguardando novo commit/push.

Luccas respondeu “pode considerar aprovadas” e autorizou definir/implementar o necessário para liberar as integrações, seguindo as decisões anteriores. Os detalhes técnicos abaixo foram definidos sob essa autorização; não são falas literais dele, aprovação da professora ou validação da QSoft. Este documento prevalece sobre pendências históricas da 539 nos assuntos resolvidos aqui. Dados ausentes continuam ausentes; nenhuma aprovação cria evidência de cumprimento.

## AI Literacy — 573

Usar a última tentativa **válida e finalizada** de cada pessoa, por avaliação e versão, com finalização dentro do período. APROVADA e REPROVADA são válidas; não escolher a melhor nota nem contar repetições como observações novas. Em empate de instante, maior ID. Tentativa mais recente inválida não elimina uma anterior válida. Não incluir tentativas abertas, fora do período, notas fora de 0–100, conteúdo incompatível ou fonte sem elegibilidade histórica. Retirada posterior não invalida automaticamente uma avaliação que era publicada/vigente quando finalizada, conforme a consulta histórica da 573.

Calcular primeiro a média das avaliações selecionadas de cada pessoa; depois a média dessas médias, dando peso igual a cada pessoa. Essa regra mede AI Literacy; não altera a regra de aprovação/certificação ou de Competências. O mínimo de confiança é de 30 **pessoas**, não 30 tentativas. Cobertura técnica mínima 95%; sem notas válidas, SEM_DADOS. Nota descritiva de coorte pequena permanece visível com aviso de informação frágil, sem liberar global.

Implementação reutilizável: SelecaoAiLiteracy.java. A consulta EvidenciasAiLiteracyRepository.buscar da branch 573 continua responsável por empresa, departamento, vínculo com catálogo e validade histórica. Mapear os resultados para SelecaoAiLiteracy.Tentativa, com evidenciaElegivel=true somente após esses filtros. Chamar selecionar(lista,inicio,fim), depois calculadoras.literacy(selecao.notasPorPessoa(),selecao.avaliacoesCandidatas()). O denominador usa grupos pessoa/avaliação/versão candidatos, não todas as retentativas. Excluir registros não autoriza afirmar cobertura de toda a população de empregados.

## Risco — 579/580

Regra adotada e versionada: RISCO-DEPARTAMENTAL-1.0.md. Implementação: ClassificacaoRiscoDepartamental.java. Usa Governança, Uso Responsável e Segurança, com pesos 30/30/40, normalizados dos pesos já existentes 15/15/20. Índice = 100 − aderência ponderada. Faixas: abaixo de 25 BAIXO; 25 até abaixo de 50 MEDIO; 50 até 100 ALTO.

Sem um dos três fatores, não classificar nem redistribuir os pesos. Dados presentes mas frágeis permitem índice/faixa **indicativos**, com classificação definitiva nula e aviso. Não deduzir risco de número de prompts ou do score global parcial. Vincular cada fator à fonte, amostra, numerador/denominador, versão e consulta de evidências autorizada da 580. Os limites são regra interna de produto/demo, sem calibração estatística ou caráter de probabilidade de incidente.

## Colaboração — 576

Autorizado usar PRs reais de tec-ricardo/HestIA na demonstração, com empresa/departamento de demo separados dos dados da QSoft. Cadastro e reprodução: COLABORACAO-GITHUB-DEMO.md. A coleta observou quatro logins: GKnorre, kevinnleee, tec-ricardo e toffan0. Não se presumiu identidade civil nem vínculo empregatício.

Fonte de eventos: dados/colaboracao-github-demo.json. Vínculo: GitHub ID → usuarioId → empresaId/departamentoId, gerado por publicar-colaboracao-demo.cjs no banco demo de cada integrante. Nunca copiar os IDs do banco de validação para outro banco. São autores históricos de demo, com login desabilitado, sem senha de acesso e sem alteração de contas existentes.

Critério: PR integrado no período e vinculado a esse cadastro; repositório+número deduplicam. Para a demo, revisão significa aprovação formal registrada por **outra conta** antes do merge. Comentário, autor revisar a própria alteração ou colega apertar Merge não substituem esse evento. Informação de review indisponível é null, nunca zero. Fórmula preservada: 100 × PRs revisados / PRs válidos. Esta coleta de 03–12/11, até o instante da coleta, tem 25 PRs válidos e nenhuma aprovação formal independente: nota descritiva zero, amostra frágil (<30). Isso não afirma que o grupo não colaborou; mede somente o evento especificado.

## FinOps — 570

Decisão anterior mantida: deixar custos sem valor enquanto Luccas esclarece com a professora. Não há fonte de faturamento, moeda, orçamento ou conversão aprovados. Não adotar BRL, orçamento fictício do seed, custo zero, preço por hora ou converter tokens em moeda silenciosamente. Tokens e custos estimados existentes não comprovam faturamento. Usar CONFIGURACAO_PENDENTE/SEM_DADOS, com motivo e nota nula. A implementação do tratamento de ausência pode ser concluída sem inventar os valores pendentes.

## Segurança — 571

Catálogo aprovado: SCRUM-539-controles-seguranca-proposta.md; regra implementada em CatalogoControlesSeguranca.java, versão seguranca-1.0. Oito controles obrigatórios, todos precisam de evidência válida da release e do corte avaliados. ATENDIDO/NAO_ATENDIDO/NAO_VERIFICADO são distintos. Nota = 100 × atendidos / avaliados conclusivamente; cobertura = avaliados/8. Cobertura integral substitui o mínimo de 30. Nada marcado atendido só porque há código ou uma tarefa concluída. Transporte HTTPS publicado segue sem evidência externa enquanto não houver hospedagem.

## Produtividade — 572

Decisão anterior mantida: comparar cenários sem e com HestIA, usando simulação identificada quando faltarem medições. Tempos e quantidades devem ser hipóteses explícitas, nunca um “funcionário médio” observado inventado. Redução de tempo e variação de capacidade são medidas separadas, podendo mostrar piora ou ausência de benefício. Não há meta numérica operacional aprovada. Sem fonte operacional/meta, a mediana pode ser descritiva quando válida e a nota operacional fica nula. Não usar volume de IA como produtividade; não inserir simulação nos dados QSoft nem no score operacional/global. Referências: SCRUM-539-decisoes-luccas.md, SCRUM-539-implementacao-validacao.md e a tela de simulação já existente.

## Relatórios do RAG — 545/594

- SCRUM-545-validacao-busca-real.md e SCRUM-545-evidencias-finais.json: embeddings reais, política didática 1.1, 8/8 casos; amostra pequena, não validação universal.
- SCRUM-546-continuidade-conversas.md e SCRUM-546-evidencias-reais.json: continuidade contextual. O limite provisório de quatro turnos foi substituído pelo orçamento/resumo descrito na atualização 612.
- SCRUM-612-complemento-ricardo.md e evidencias/correcoes-validacao.json: combinação da interface e serviços, 361 testes Java aprovados/2 externos ignorados, 52 frontend, 15 cenários de API e 9 HTTP pelo frontend. Não inclui nova chamada OpenAI nesse ensaio.
- DEFINICOES-SPRINT4-VALIDACAO.md: validação das definições desta entrega. Não substituir teste de qualidade de RAG por teste da fórmula de risco.

## Integração por Knorre

As branches 573/576/579/580 continuam pertencendo ao fluxo de integração do Knorre; não foram mergeadas automaticamente nesta entrega. Os contratos e implementações auxiliares acima estão prontos para consumo. Na 579, anexar o resultado de classificar(apuracao.fatores()) a cada departamento, substituir CRITERIOS_DE_RISCO_PENDENTES pela versão ativa e preservar todos os controles de autorização já existentes. Na 580, ligar as evidências agregadas às consultas/IDs de origem autorizados; a classe não inventa IDs de registros que o agregado não contém. Não misturar períodos, snapshots, versões ou fontes para produzir completude.
