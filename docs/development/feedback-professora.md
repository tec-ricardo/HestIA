# Ajustes após a demonstração

## Objetivo
A jornada do colaborador registra atividades e XP; o gestor consulta evidências operacionais, interpreta indicadores e decide ações. XP não é nota de produtividade, competência ou desempenho profissional.

## Escopo e fonte
- O resumo executivo usa o endpoint autenticado de maturidade e preserva as oito fórmulas originais.
- O painel de colaboradores usa os últimos 30 dias, somente funcionários ativos da empresa e, para gestor, do departamento autorizado.
- A fonte DEMO contém observações fictícias agregadas, não uma vinculação aos colaboradores operacionais.
- QSoft/base original C01 é um cenário didático separado, acessível por /maturidade/demonstracao, /custos/demonstracao e /mapa-risco?fonte=SINTETICA. Não é a empresa autenticada.
- A consulta de maturidade SINTETICA do endpoint autenticado exige o mapeamento autorizado.
- Segurança, produtividade, orçamento e colaboração por revisão de PR são fatores organizacionais/departamentais. Sem mapeamento operacional confiável de autoria, não se atribui nota individual de colaboração. Reutilização de respostas é mostrada separadamente como evidência de contribuição.
- Referência de consumo em tokens configurável na tela de acompanhamento; 0 desativa, é específica da sessão e vale para os 30 dias exibidos. Não constitui punição.

## Slides: entregar o racional
XP = soma dos movimentos de experiência validados (registro 10, conformidade 25, treinamento 40, reutilização 15, missão conforme recompensa cadastrada). Exemplos atuais de missões: 50, 75, 100 XP. Ranking ordenado pelo saldo de XP; empates têm mesma posição. A composição mostra divergências se saldo e movimentos não conferirem.

| Indicador | Peso | Racional resumido |
|---|---:|---|
| AI Literacy | 15% | Agregação das notas individuais elegíveis |
| Governança | 15% | 100 × usos aprovados / usos classificados |
| Uso responsável | 15% | 100 × conformes / (conformes + não conformes), revisões separadas |
| Produtividade | 10% | mínimo(100, 100 × meta em dias / mediana observada) |
| FinOps | 10% | limitar 100 × (2 − custo/orçamento) a 0–100 |
| Segurança | 20% | 100 × controles atendidos / avaliados |
| Colaboração | 5% | 100 × PRs revisados / PRs válidos |
| Competências | 10% | 100 × pessoas qualificadas / resultado determinável |

Score completo = soma(nota × peso) com todos os fatores elegíveis. Score parcial = soma(nota × peso)/soma(pesos elegíveis), sempre acompanhado de cobertura. As faixas gerenciais <60 e <80 são propostas de sinalização e não modificam o cálculo.

## Roteiro demonstrativo
1. Em ambiente demo, criar cinco funcionários fictícios pelo cadastro da administração no mesmo departamento do gestor. Usar credenciais temporárias distintas; não versionar senhas nem chaves.
2. Entrar como colaborador e usar o assistente com uma política corporativa publicada e fonte autorizada. Reutilizar uma resposta explicitamente; conferir a movimentação real.
3. Realizar microtreinamento/avaliação válidos, registrando XP pela operação real da plataforma.
4. Mostrar ranking e clicar no líder. Conferir que o total por atividade concilia com o saldo.
5. Com HESTIA_RAG_PERMITIR_RESPOSTA_GERAL=false, fazer consulta sem conteúdo autorizado. Demonstrar a recusa/ausência de resposta e registro na curadoria. Não classificar automaticamente falta de conteúdo como violação.
6. Entrar como gestor. Mostrar resumo executivo (fonte e período), prioridades nominais e composição do XP. Ajustar referência de tokens apenas para exemplificar alerta de acompanhamento e revisar as ocorrências efetivamente registradas.
7. Indicar a ação: capacitação, avaliação de ocorrência, revisão da base ou orçamento. A decisão cabe ao gestor, não a uma punição automática por baixa pontuação ou consumo.
8. Separar o cenário histórico QSoft da empresa atual.

## Validação
- npm test na pasta Front-End/hestia-prototipo-sprint1-atualizado
- mvnw.cmd test na raiz
- docker compose up -d --build
- Validar conta gestor e administrador, funcionário negado no painel, XP de outra empresa não acessível, fontes operacionais sem QSoft, notas ausentes não classificadas como críticas.
- A ausência de vínculo individual confiável e a homologação de faixas/racionais devem ser explicitadas à banca. Não apresentar números simulados como evidência real.
