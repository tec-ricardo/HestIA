# SCRUM-497 — Evolução após o microtreinamento

## Consulta

`GET /microtreinamentos/conclusoes/{conclusaoId}/evolucao?tipoErro=FERRAMENTA_NAO_AUTORIZADA`

A identidade vem da sessão autenticada. Funcionários consultam o próprio histórico; gestores, participantes do próprio departamento; administradores, participantes da própria empresa. A conclusão deve estar concluída e corresponder ao tema do erro informado.

## Fontes e período

A recorrência reutiliza `ConsultaEfetividadeCapacitacaoService`, incluindo contagens, cobertura, motivos e vínculos de trilhas. Sua janela posterior de 30 dias também delimita as avaliações de aprendizagem, limitada ao instante observado. O fim da janela é exclusivo. O retorno informa se a janela está completa.

As notas vêm de tentativas finalizadas em `aprendizagem_tentativas`, vinculadas a ciclos e catálogos da empresa e do participante autorizados. A leitura precisa referenciar a conclusão consultada, o microtreinamento e o hash do conteúdo da avaliação, e ocorrer antes da abertura da tentativa. Leituras duplicadas não duplicam tentativas.

Tentativas em andamento, sem nota, anteriores à conclusão ou posteriores ao corte não participam. Conclusões antigas sem leitura versionada vinculada não recebem notas presumidas.

## Comparação

Cada catálogo versionado é comparado separadamente. O retorno preserva IDs das tentativas e ciclos, notas, estados, datas, versão e hash. A variação em pontos é a última nota menos a primeira, ordenadas por finalização e ID. Valores positivos indicam MELHORIA, negativos PIORA e zero ESTABILIDADE. Menos de duas tentativas resultam em DADOS_INSUFICIENTES e variação nula.

A situação de aprendizagem indica apenas a disponibilidade da comparação de notas. A recorrência mantém sua própria situação e requisitos de cobertura e período. Não é calculado um índice combinado. Uma melhora nas tentativas não representa comparação com conhecimento anterior ao treinamento nem comprova causalidade. A consulta não altera aprovação, conclusão ou certificação.

## Validação

`EvolucaoMicrotreinamentoServiceTest` exercita a consulta SQL em H2 isolado, a separação entre versões, vínculo de leitura, exclusão de outras empresas e usuários, janela temporal, dados insuficientes, autorização anterior à leitura dos resultados e a rota HTTP. A validação em PostgreSQL não é substituída por esse teste.
