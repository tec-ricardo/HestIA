# SCRUM-539 — Regras, fontes e decisões dos oito indicadores

Versão documental: 1.1, 06/11/2026. Base examinada: develop 08f4c9b.
Situação: decisões de Luccas registradas em SCRUM-539-decisoes-luccas.md; esse registro posterior prevalece sobre os itens pendentes históricos deste documento. Não há aprovação empresarial/acadêmica presumida.
Rastreabilidade: PO-132, PO-133, PO-134 e PO-135.

## Referências e alcance

Preservar CalculadorasMaturidade (proposta-0.1), ConfiguracaoMaturidade, ResultadoMaturidadeService e docs/scrum/MATURIDADE-PROPOSTA-0.1-IMPLEMENTACAO.md. A especificação anterior HestIA_Especificacao_Metricas_Maturidade, de 21/10/2026, também se identifica como proposta. Este documento resolve a organização do contrato e explicita as decisões que ainda exigem dados do grupo. Não altera pesos, fórmulas ou configuração de servidor.

## Fichas dos fatores

| Fator / peso preservado | Cálculo existente a manter | Fonte operacional requerida e população | Período e cobertura |
| --- | --- | --- | --- |
| AI Literacy / 15% | Média das médias de notas por pessoa, escala 0–100 | Tentativas finalizadas de aprendizagem e conteúdo versionado, vinculados ao colaborador/empresa. Cada pessoa tem o mesmo peso; não usar número de cursos como peso | Finalização no período; informar pessoas observadas, conclusões candidatas/válidas e cobertura. Confirmar a seleção de tentativas da mesma avaliação para não favorecer repetição |
| Governança / 15% | 100 × usos aprovados / usos com classificação conhecida; ponderar por requests somente quando houver contagem operacional auditável | Registros de uso, ferramenta e histórico de autorização por empresa/departamento; ciência de política é evidência adicional, não substituto silencioso da fórmula | Instante de cada uso; aprovação/retirada precisam de vigência histórica. Desconhecidos reduzem cobertura; cadastro atual não prova autorização passada |
| Uso Responsável / 15% | 100 × C / (C + N) | Avaliações com política histórica, regra e fundamentos preservados, ligadas aos usos da empresa; uma avaliação efetiva por uso | Uso e avaliação no período, conforme adaptador existente. R não é aprovação/reprovação; legadas separadas; informar cobertura de avaliação |
| Produtividade / 10% | min(100, 100 × metaDias / medianaDias) | Tempos reais de itens de um mesmo tipo/processo e escopo. Se a aplicação não capta esses tempos, declarar fonte indisponível; não substituir por volume de prompts | Itens concluídos no período; duração válida e unidade em dias; meta por empresa/departamento/tipo, versionada. Sem meta: mediana descritiva e nota nula |
| FinOps / 10% | limitar(100 × (2 − custo/orçamento), 0, 100) | Custos e orçamentos autorizados, por centro e mês, mesma moeda/unidade. Custo de tokens é estimado se não conciliado com faturamento | Mesmos meses/centros; universo completo, orçamento positivo, custo observado não negativo. Zero observado difere de valor ausente |
| Segurança / 20% | 100 × controles aprovados / controles avaliados | Catálogo versionado de controles obrigatórios e suas evidências, por empresa; incidentes são evidências possíveis, não o denominador | Evidência válida no período/instante da apuração; todos os controles obrigatórios avaliados para elegibilidade global. Sem catálogo, não publicar nota global |
| Colaboração / 5% | 100 × PRs integrados revisados / PRs integrados válidos | PRs reais associados a projetos/empresa e reviews. Se houver apenas review_count, informar que independência da revisão não foi comprovada | Merge no período; excluir PRs sem vínculo válido e informar cobertura. Sem fonte Git operacional autorizada, SEM_DADOS |
| Competências / 10% | 100 × pessoas qualificadas / pessoas com resultado determinável | Colaboradores da coorte e conclusões/aprovações compatíveis com os requisitos versionados, usando catálogo real | Declarar coorte e corte temporal; aprovação válida prevalece sobre falha anterior da mesma exigência. Ausência de resultado não é reprovação; não conceder certificação por esta consulta |

As fontes operacionais acima são requisitos de integração, não afirmação de disponibilidade. Na base examinada, AdaptadorOperacionalMaturidade só calcula Uso Responsável; os sete demais fatores ainda retornam SEM_DADOS. As integrações pertencem às SCRUM-569 a 579 e tarefas relacionadas.

## Semântica comum

- Empresa vem da sessão; gestor permanece no próprio departamento; administrador no escopo autorizado.
- API usa datas inicial e final inclusivas; consultas por timestamp usam início do primeiro dia inclusivo e início do dia seguinte ao final exclusivo.
- Registrar fonte, natureza, população, amostra, exclusões, cobertura, versão dos pesos/cálculo/parâmetros e instante de apuração.
- Não misturar SINTETICA e OPERACIONAL, snapshots, coortes ou versões para fabricar completude.
- Ausência de dados, configuração pendente e nota zero são estados distintos.
- Manter os pesos existentes, que somam 100%. Não redistribuir pesos para esconder fatores ausentes.
- Global só com os oito fatores elegíveis. Parcial é soma ponderada dos elegíveis dividida pela soma de seus pesos; rotular como exploratório e mostrar cobertura. Se não houver fator elegível, parcial nulo.
- Arredondar apresentação final em duas casas com HALF_UP; preservar precisão para decidir limiares.
- Resultado indica observações e aderência aos critérios, não prova causal de benefício da IA.

## Decisões numéricas para aceite

| Decisão | Referência existente | Encaminhamento e condição de liberação |
| --- | --- | --- |
| Pesos | 15/15/15/10/10/20/5/10 | Preservados; confirmar formalmente no aceite do grupo |
| Cobertura mínima | 95% técnica e classificação | Manter como proposta existente; documentar aceite. Não é garantia estatística universal |
| Amostra mínima | 30 observações, com exceções FinOps/Segurança | Manter como proposta; unidade de amostra é a ficha do fator. FinOps/Segurança exigem universo completo, não mínimo arbitrário de 30 |
| Limiar de competências | 80 | Preservar compatibilidade com avaliação atual; mapear evidência equivalente no catálogo real |
| Meta de produtividade | Não configurada | Informar valor positivo, tipo, empresa/departamento, unidade, vigência e responsável. O exemplo 5 dias não é meta aprovada |
| Moeda e conversão FinOps | Não configuradas | Informar moeda de cada orçamento e custo; se diferentes, fonte/data/taxa autorizadas. Não presumir BRL ou converter silenciosamente |
| Catálogo de segurança | Não fornecido | Responsável deve listar controles obrigatórios, critérios, evidências, validade e avaliador. Sem isso, manter configuração pendente |
| População de AI Literacy | Seleção operacional ainda não integrada | Aceitar regra de seleção de tentativas por versão antes de integrar; aprovação de aprendizagem e média descritiva têm propósitos diferentes |

## Casos de aceite

| Entrada | Resultado esperado |
| --- | --- |
| Notas médias pessoais 60 e 100 | AI Literacy 80 |
| 80 requests aprovados / 100 classificados | Governança 80, com cobertura separada |
| C=8, N=2, R=5 | Uso Responsável 80; cobertura conclusiva 10/15 |
| Meta autorizada 5 dias e mediana 10 | Produtividade 50; exemplo de teste, não configuração real |
| Orçamento 1000 e custo 1500 na mesma moeda | FinOps 50 |
| 4 controles aprovados de 4 avaliados, universo 5 | Nota descritiva 100, cobertura 80%, inelegível global |
| 6 PRs revisados entre 10 válidos | Colaboração 60 |
| 7 pessoas qualificadas entre 10 determináveis | Competências 70 |
| Um fator ausente | Global nulo, parcial e cobertura explícitos |
| Fonte operacional indisponível | SEM_DADOS; não usar staging como substituto |

## Registro de aprovação

Pendente: responsável, data, decisão sobre parâmetros acima e referência da ata/aceite.
Após aceite, registrar nova versão de parâmetros no servidor e anexar as evidências; não editar resultados históricos. Não há aprovação presumida neste documento.
