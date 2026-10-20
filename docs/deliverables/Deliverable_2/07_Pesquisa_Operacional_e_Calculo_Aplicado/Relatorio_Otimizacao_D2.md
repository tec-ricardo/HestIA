# Modelo, simulações e resultados - Deliverable 2

Projeto HestIA | Pesquisa Operacional e Cálculo Aplicado | 20/10/2026

## Problema e decisão
Selecionar ferramentas de IA por departamento de modo a maximizar o benefício esperado, respeitando orçamento, risco, quantidade máxima de ferramentas e permissões. Os dados são simulados para validação acadêmica. Valores de custo não constituem preços atuais de mercado, e benefícios não representam ganhos empiricamente medidos.

## Formulação matemática
Índices: d representa departamento; f representa ferramenta.
Variável: x[d,f] pertence a {0,1}, indicando seleção do par.
Parâmetros: c[d,f] é custo por ciclo; b[d,f] é benefício em pontos; r[d,f] é risco; B é orçamento; R[d] é risco máximo; K[d] é quantidade máxima; a[d,f] indica permissão.
Objetivo: maximizar Z = soma(d,f) b[d,f] * x[d,f].
Orçamento: soma(d,f) c[d,f] * x[d,f] <= B.
Risco: para cada d, soma(f) r[d,f] * x[d,f] <= R[d].
Quantidade: para cada d, soma(f) x[d,f] <= K[d].
Política: x[d,f] <= a[d,f]. Domínio binário.
O risco é tratado como escore aditivo de simulação. Uma escala ordinal real não garante distâncias comparáveis ou aditividade; a organização precisará calibrar e justificar esse uso.

## Implementação e reprodução
solver.py enumera as 2^6 = 64 combinações da instância inicial, rejeita as inviáveis e maximiza o benefício; empates são resolvidos pelo menor custo. Para esta instância, a enumeração permite encontrar o ótimo global, não apenas uma aproximação.
Na pasta model/optimization, executar: python -m unittest -v; python run_scenarios.py.
Entradas: scenario.csv e limits.csv. O gerador escreve as entradas completas e a saída JSON de cada cenário em scenarios/, além de results/comparison.csv e results/results.json. Os artefatos estão no projeto original, sem duplicação do código.
Foram executados oito testes: ótimo conhecido, rejeição de seleções inválidas, orçamento isolado, risco isolado, quantidade isolada, política isolada, orçamento zero e desempate por custo. Todos passaram.

## Experimento controlado
Dois departamentos (Marketing e TI), três ferramentas e seis pares candidatos. Mantêm-se os parâmetros base, alterando somente o fator indicado. O cenário limite_uma_ferramenta deve ser comparado ao orcamento_ampliado: ambos possuem orçamento 2.000, isolando o efeito de K.

## Cenário: base
Orçamento: R$ 1400; custo: R$ 1300; benefício: 19 pontos; saldo: R$ 100.
Seleção: Marketing::Midjourney, TI::Copilot. Viabilidade verificada: sim. Risco máximo: {'Marketing': 4, 'TI': 4}. Quantidade máxima: {'Marketing': 2, 'TI': 2}.

## Cenário: orcamento_reduzido
Orçamento: R$ 800; custo: R$ 800; benefício: 14 pontos; saldo: R$ 0.
Seleção: Marketing::ChatGPT, TI::ChatGPT. Viabilidade verificada: sim. Risco máximo: {'Marketing': 4, 'TI': 4}. Quantidade máxima: {'Marketing': 2, 'TI': 2}.

## Cenário: orcamento_ampliado
Orçamento: R$ 2000; custo: R$ 1700; benefício: 25 pontos; saldo: R$ 300.
Seleção: Marketing::Midjourney, TI::ChatGPT, TI::Copilot. Viabilidade verificada: sim. Risco máximo: {'Marketing': 4, 'TI': 4}. Quantidade máxima: {'Marketing': 2, 'TI': 2}.

## Cenário: risco_restritivo
Orçamento: R$ 1400; custo: R$ 1100; benefício: 18 pontos; saldo: R$ 300.
Seleção: Marketing::ChatGPT, TI::Copilot. Viabilidade verificada: sim. Risco máximo: {'Marketing': 2, 'TI': 2}. Quantidade máxima: {'Marketing': 2, 'TI': 2}.

## Cenário: bloqueio_copilot
Orçamento: R$ 1400; custo: R$ 1000; benefício: 15 pontos; saldo: R$ 400.
Seleção: Marketing::Midjourney, TI::ChatGPT. Viabilidade verificada: sim. Risco máximo: {'Marketing': 4, 'TI': 4}. Quantidade máxima: {'Marketing': 2, 'TI': 2}.

## Cenário: limite_uma_ferramenta
Orçamento: R$ 2000; custo: R$ 1300; benefício: 19 pontos; saldo: R$ 700.
Seleção: Marketing::Midjourney, TI::Copilot. Viabilidade verificada: sim. Risco máximo: {'Marketing': 4, 'TI': 4}. Quantidade máxima: {'Marketing': 1, 'TI': 1}.

## Interpretação comparativa e sensibilidade
Reduzir o orçamento de 1.400 para 800 reduz o benefício de 19 para 14 pontos (queda de 26,32%) e altera ambas as escolhas para ChatGPT.
Aumentar o orçamento para 2.000 permite benefício 25 (+31,58%), com gasto 1.700. O saldo não deve ser forçado a zero: maximiza-se benefício, não despesa.
Limitar risco a 2 por departamento substitui Midjourney por ChatGPT no Marketing; o benefício cai de 19 para 18 (-5,26%), e o custo cai para 1.100.
Bloquear Copilot reduz o benefício a 15 (-21,05%) e o custo a 1.000, ilustrando o custo de oportunidade de uma política.
Com orçamento 2.000 e apenas uma ferramenta por departamento, o benefício volta a 19. Comparado ao cenário ampliado, a restrição de quantidade custa 6 pontos (24% do benefício).
O aumento de orçamento de 1.400 para 2.000 produz 6 pontos adicionais para 600 unidades de orçamento disponibilizado: razão incremental de 0,01 ponto por real de orçamento. O gasto efetivo cresce 400, resultando em 0,015 ponto por real adicional gasto. Essas razões são diferenças finitas entre cenários, não derivadas ou preços-sombra de um modelo contínuo.
Por ser um problema binário, a função valor em relação ao orçamento é em degraus: aumentos pequenos podem não alterar a seleção. Não se deve inferir crescimento linear de benefício.

## Limitações e conexão com o MVP
Benefícios e custos são determinísticos; não há descontos, múltiplos períodos, incerteza ou garantia de equidade. A soma de escores de risco é uma hipótese simplificadora. O algoritmo cresce como 2^n e serve à instância pequena; para escala maior, avaliar programação inteira.
A HestIA pode futuramente fornecer departamentos, ferramentas aprovadas, custos e restrições. O resultado pode apoiar uma recomendação ao gestor, sujeita à revisão humana. Não foi implementado um endpoint de otimização nesta entrega.
O modelo base corresponde às SCRUM-132 a 136, da Sprint 1. A evolução D2 é o conjunto de seis cenários reproduzíveis, testes isolados das restrições, análise de sensibilidade e interpretação dos resultados.

## Fontes e rastreabilidade
https://github.com/tec-ricardo/HestIA/tree/develop/model/optimization
https://github.com/tec-ricardo/HestIA/blob/develop/docs/optimization/SCRUM-136-modelo-consolidado.md
https://luccastoffano.atlassian.net/browse/SCRUM-131
