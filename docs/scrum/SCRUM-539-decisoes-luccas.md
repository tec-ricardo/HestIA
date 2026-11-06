# SCRUM-539 — Decisões informadas por Luccas

Registro de conversa em 06/11/2026. Complementa SCRUM-539-regras-metas-indicadores.md.
Não constitui aprovação de escolhas ainda não respondidas nem altera o código ou os parâmetros do servidor.

## Decisões recebidas

- Pesos: Luccas permite manter os existentes se a mudança não trouxer melhoria. Sem evidência apresentada que justifique uma redistribuição, manter 15/15/15/10/10/20/5/10; não alegar que esses pesos foram estatisticamente otimizados.
- Quantidade/cobertura: Luccas aceitou a proposta de pelo menos 30 registros válidos e 95% dos dados necessários. A unidade de amostra continua sendo própria de cada fator. As exceções de universo completo para FinOps e Segurança precisam ser esclarecidas na confirmação final.
- Fragilidade: quando a quantidade de dados for insuficiente, o site deve avisar ao consumidor que a informação é frágil. Luccas sugeriu laranja para fragilidade e verde para parâmetros atendidos, com legenda a ser definida depois. Cores finais, texto exato e detalhes da legenda não foram aprovados.
- Produto: o objetivo declarado é melhorar a produtividade e o controle do uso de IA nas empresas. Isso não define ainda a medida de produtividade nem uma meta numérica.
- Segurança: preparar proposta de controles, conforme SCRUM-539-controles-seguranca-proposta.md. Preparação não equivale a aprovação.
- Dados: Luccas informou que dispõe somente dos dados da QSoft já conhecidos; não existem fontes adicionais fornecidas por ele nesta conversa. A documentação do projeto identifica a base disponibilizada como sintética. Preservar essa natureza, sem apresentá-la como operação real.
- Custos: a pergunta não foi compreendida; nenhuma moeda, orçamento ou regra de conversão foi aprovada.

## Comportamento de qualidade a detalhar na interface

Exibir resultado numérico somente quando houver base válida para calculá-lo. Dados insuficientes para confiança podem produzir valor descritivo com aviso de fragilidade, sujeito às regras do fator. Nenhum dado disponível não significa nota zero; mostrar indisponibilidade e motivo.

Proposta de comunicação: legenda laranja/verde acompanhada de rótulo legível, quantidade observada, mínimo esperado, cobertura e período. Verde significa critérios de qualidade de dados atendidos, não bom desempenho do negócio. Uma nota de desempenho baixa pode ter dados suficientes; uma nota alta pode ser frágil. Não afirmar que 30/95% garantem validade estatística.

O tratamento da contribuição de fatores frágeis ao score global não foi alterado nem aprovado por esta resposta. O contrato atual só publica global com todos os fatores elegíveis; não incluir silenciosamente valores frágeis na nota global. Detalhes de interface pertencem às tarefas já planejadas dos indicadores e sua homologação.

## Pontos que ainda exigem resposta

1. Produtividade: definir o que será medido e como comparar, considerando os campos disponíveis; não criar meta numérica a partir do objetivo geral.
2. Custos: explicar que se trata do gasto da empresa com ferramentas de IA, não do preço cobrado pela HestIA; confirmar regra para quando moeda/orçamento estiverem ausentes.
3. Segurança: revisar a proposta de oito controles e sua regra específica de cobertura.
4. AI Literacy e demais mapeamentos permanecem sujeitos à conferência do catálogo e das fontes, sem aprovação automática das decisões não respondidas.

## Lembrete solicitado: demonstração final do RAG

Luccas pediu ampliar as frases/conteúdos usados no teste final da apresentação, além do exemplo já utilizado para mostrar o funcionamento do RAG. Registrar como preparação de massa de demonstração, relacionada às SCRUM-542/543/545/556.

Não afirmar que o RAG possui memória treinada: o mecanismo examinado recupera fontes indexadas e preserva interações. O pedido não fornece os novos textos ou regras empresariais. Antes de carregar, combinar os conteúdos e resultados esperados, identificando material didático como demonstração e mantendo autorização, empresa/perfis, versão e fonte.

Proposta de cenários para posterior confirmação: pergunta direta, reformulação, pergunta de continuação, assunto sem fonte, nova conversa e fonte retirada/sem permissão. Esta etapa registra o pedido; não inseriu frases no banco, não inventou política oficial e não modifica o histórico real.

## Complemento aprovado por Luccas — resposta mais recente

- Produtividade: comparar cenários sem fiscalização e com a HestIA, usando simulação quando necessário. Os valores precisam ser identificados como hipóteses; não existe prazo-meta, funcionário médio observado ou benefício real comprovado informado nesta conversa.
- Custos: deixar sem valor por enquanto. Luccas esclarecerá o tema com a professora. Nenhuma moeda, orçamento, preço de hora ou conversão foi definida. Não transformar ausência em zero.
- Segurança: Luccas aprovou a proposta dos oito controles e autorizou sua implementação em documentos/testes/código conforme necessário. A aprovação é do catálogo e da regra, não do cumprimento de cada controle pela aplicação.

A tela de comparação recebe tempos por tarefa e quantidade de tarefas, sem valores presumidos. Calcula redução de tempo e variação de capacidade separadamente; aceita piora ou nenhuma mudança. A página nunca grava essas hipóteses na base QSoft ou nas avaliações operacionais e não as usa para liberar score global.

A implementação usa a mesma regra vigente de elegibilidade para o aviso de qualidade de cada fator. Valor válido porém parcial fica visível com aviso de informação frágil; falta de dados mantém indisponibilidade. Cor é acompanhada de texto e legenda. A escolha de tons pode ser ajustada depois sem alterar a regra.