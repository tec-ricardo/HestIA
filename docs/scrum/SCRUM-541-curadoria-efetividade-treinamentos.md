# SCRUM-541 — Treinamento válido e efetivo

Versão de especificação: 1.0, 06/11/2026. Rastreabilidade: PO-140, PO-97 e PO-104.
Usar com SCRUM-483 (certificações), SCRUM-494 (avaliações) e SCRUM-497 (evolução); não duplicar suas implementações.

## Publicação no catálogo real

Um treinamento só pode ser recomendado como operacional depois de revisão e publicação autorizadas na empresa. Metadados mínimos:

- Identificador, título, tema, objetivo observável, público e pré-requisitos.
- Fonte/provedor, URL ou arquivo autorizado, autoria, idioma, duração estimada e condições de acesso.
- Permissão de uso/distribuição; conteúdo pago ou restrito não é copiado sem autorização.
- Conteúdo/versão/hash, data da revisão, revisor e empresa/perfis autorizados.
- Relação com a regra/política vigente e com o problema que motivou a recomendação.
- Atividade prática, evidência esperada, critério de aceite e avaliação compatível.
- Estado: rascunho, em revisão, publicado ou retirado; datas e motivo de retirada.

Revisor autorizado precisa verificar acesso, correção, atualidade, adequação ao tema e ao público, autorização de uso e ausência de dados sensíveis indevidos. Se houver link externo, conferir acesso real; link quebrado ou credencial não disponível impede recomendação operacional. Publicar/retirar e persistir o catálogo pertence às SCRUM-549 e 560; a seleção/revisão dos conteúdos reais à 559.

## Critério de atividade prática

Cada conteúdo deve exigir uma aplicação observável no tema, com instrução, rubrica e evidência minimizada. Exemplos de atividades são cenários didáticos, não texto de política oficial:

| Tema | Atividade | Aceite |
| --- | --- | --- |
| Ferramenta autorizada | Escolher uma ferramenta permitida em cenário de uso | Justificar com regra e fonte aplicáveis |
| Proteção de dados | Classificar informações fictícias e preparar entrada minimizada | Identificar restrições e retirar identificadores desnecessários |
| Validação humana | Revisar uma resposta de IA com erros controlados | Identificar erros e registrar correção verificável |
| Política interna | Resolver um cenário citando trecho vigente | Relacionar decisão à revisão correta, sem inventar norma |

A atividade não cria nova obrigação acadêmica de nota numérica. O responsável registra ATENDIDA, NAO_ATENDIDA ou EVIDENCIA_INSUFICIENTE e motivo. Não penalizar falta técnica de envio como reprovação de conhecimento. A evidência da atividade não deve ser lançada como prova implementada antes de existir persistência compatível.

## Cursos e evidência externos

Certificado/declaração externos devem identificar provedor, curso, participante autorizado, conclusão/data, versão ou referência verificável e vínculo com o conteúdo exigido. Revisor autorizado confirma autenticidade/acesso e compatibilidade; registrar responsável e decisão. Minimizar dados e restringir acesso.

Um certificado externo não equivale automaticamente a avaliação HestIA aprovada, não cria conta a partir de identidade sintética e não concede XP/certificação interna por simples upload. Equivalência exige regra explícita, versionada e evidência preservada. Até lá, indicar evidência externa em revisão ou conclusão externa verificada, sem inventar tentativa interna.

## Conclusão e aprendizagem

Preservar o que já foi entregue na SCRUM-494 e implementado em AvaliacaoAprendizagemService:

- Conclusão do conteúdo e aprovação de aprendizagem são fatos separados.
- Avaliação possui cinco questões obrigatórias, pesos iguais e pelo menos três alternativas; gabarito e conteúdo versionados.
- Aprovação a partir de 80%, até três tentativas finalizadas por ciclo; terceira reprovação exige reforço e liberação autorizada.
- Envio incompleto/inválido ou falha técnica não consome tentativa finalizada.
- Reenvio recupera resultado sem duplicar efeitos; aprovação posterior preserva reprovações anteriores.
- Mudança de conteúdo não troca o gabarito de tentativa aberta nem apaga histórico.
- Certificação segue seus próprios requisitos completos, sem concessão apenas por presença, XP ou um certificado externo.

O backend examinado ainda consulta MicrotreinamentoFicticioService em AvaliacaoAprendizagemService.conteudo. A SCRUM-549 precisa trocar essa fonte pelo catálogo persistido; este documento não afirma que o catálogo real já existe.

## Efetividade observada

Aprovação de aprendizagem não comprova redução de incidentes. Separar:

1. Conclusão: conteúdo/atividade registrados para usuário, empresa e versão.
2. Aprendizagem: avaliação aprovada conforme regra vigente.
3. Aplicação/recorrência: observações de uso após a capacitação com tema comparável.
4. Efetividade: interpretação explícita das evidências e limitações, sem inferir causalidade.

Reutilizar ConsultaEfetividadeCapacitacaoService e a janela posterior de 30 dias já referenciada na SCRUM-497, limitada ao instante observado, com fim exclusivo e indicação de janela incompleta. Preservar contagens, exposição/volume, cobertura e situações insuficientes. Zero ocorrências com zero observações não prova efetividade.

Notas só são comparadas entre tentativas compatíveis do mesmo catálogo/versão; menos de duas produz DADOS_INSUFICIENTES. Não criar índice combinado entre nota e recorrência. Ajustes de fonte/catálogo/recorrência pertencem às SCRUM-561 e 565.

## Cenários de aceite

| Cenário | Esperado |
| --- | --- |
| Conteúdo sem fonte/autorização/revisão | Não publicar nem recomendar como operacional |
| Conteúdo retirado | Fora de novas recomendações, histórico preservado |
| Link indisponível | Indicar indisponibilidade e solicitar revisão |
| Certificado externo sem verificação | Sem aprendizagem/XP/certificação presumidos |
| Quatro acertos em cinco | Aprovar com nota 80 |
| Atividade sem evidência suficiente | Evidência pendente, sem aprovação inventada |
| Tentativa reenviada | Mesmo resultado, sem recompensa duplicada |
| Zero ocorrências e nenhum uso observado | Dados insuficientes |
| Janela posterior ainda incompleta | Mostrar observação parcial e data de corte |
| Conteúdo de outra empresa/perfil | Acesso/recomendação rejeitados |
