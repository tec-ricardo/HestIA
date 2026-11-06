# SCRUM-539 — Implementação das decisões

Branch: features/SCRUM-539-regras-confiabilidade, baseada na develop após o PR 119.
Aprovação de produto: respostas de Luccas registradas em SCRUM-539-decisoes-luccas.md.

## Comportamento entregue

1. Indicadores de maturidade mantêm notas descritivas parciais, acompanhadas de aviso de informação frágil, legenda laranja/verde/cinza e texto. A qualidade de dados é separada do desempenho. Zero observado é diferente de ausência. A classificação respeita a elegibilidade calculada pelo servidor.
2. Página autenticada de simulação em /maturidade/simulacao-produtividade, para gestores e administradores pela proteção já existente de /maturidade. Recebe tempos e quantidade; inicia sem números. Link disponível na consulta de resultados.
3. A simulação compara tarefas de mesma complexidade/qualidade e separa redução de tempo de variação de capacidade na mesma jornada. Não força benefício: valores piores produzem variação negativa. Não calcula dinheiro nem altera dados do projeto.
4. Catálogo seguranca-1.0 com oito controles obrigatórios e cálculo testável. Evidências de outra release, futuras ou sem referência não aprovam o controle; duplicatas são rejeitadas. Todos os oito precisam ser avaliados conclusivamente para elegibilidade completa.
5. Custos sem valor definido até orientação da professora; não foi configurada moeda/orçamento, nem criada estimativa monetária.

## Limites explícitos

A simulação não comprova produtividade real nem representa a média medida de funcionários. Não adiciona 30 eventos artificiais ao score para aparentar qualidade de dados. Os parâmetros são hipóteses escolhidas no formulário; não substituem a base QSoft. Para uma comparação observacional real, serão necessárias medições comparáveis e controle de diferenças de complexidade/qualidade.

CatalogoControlesSeguranca oferece o contrato e cálculo do catálogo. A persistência/coleta operacional de evidências e integração ao adaptador de Segurança continua na SCRUM-571; não há marcação automática de oito controles atendidos. O score operacional não foi preenchido com resultados de fixtures. Backup e demais controles dependem das evidências reais das tarefas correspondentes.

O limite 30/95% é critério adotado do projeto, não garantia estatística. Segurança/FinOps mantêm exigências específicas de completude. A nota global continua indisponível quando faltam fatores elegíveis.

## Exemplo didático para ensaio

Somente como hipótese: 60 minutos antes e 45 depois, para 30 tarefas equivalentes, resultam em redução de tempo de 25%, capacidade simulada aproximadamente 33,33% maior e 450 minutos economizados. Não são valores padrão da página nem resultados observados. Exemplo inverso 30 antes/60 depois mostra piora, sem omitir o sinal negativo.

## Validação

Testes de frontend cobrem simulação, entradas vazias/inválidas, piora, ausência de ganho, falta de valor monetário e renderização do aviso/legenda. Testes Java do catálogo cobrem evidências ausentes, incompletas, duplicadas, futuras, de outra versão e resultado com universo completo. A execução completa é registrada ao final da validação. Não houve commit/push automático.

## RAG na apresentação

Mantido o pedido de ampliar exemplos para a demonstração final nas tarefas 542/543/545/556. Nenhuma frase ou política fictícia foi carregada em banco por esta entrega.

Resultado final em 06/11/2026: frontend node --test, 25 aprovados e 0 falhas; Java mvnw.cmd -o --batch-mode test, com PostgreSQL/pgvector e as três variáveis de integração do CI habilitadas, 314 contabilizados, 312 aprovados, 0 falhas, 0 erros e 2 ignorados por condições existentes, BUILD SUCCESS. git diff --check sem erros. Testes de renderização EJS foram executados; não houve inspeção visual manual em navegador nesta etapa.
