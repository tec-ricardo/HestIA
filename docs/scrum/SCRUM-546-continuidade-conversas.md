# SCRUM-546 — Continuação das conversas do RAG

Data: 12/11/2026. Responsável: Luccas Toffano. Branch: features/SCRUM-545-546-busca-contexto. Depende da recuperação validada na SCRUM-545 e das conversas persistidas da SCRUM-536.

## Entrega

- A interface cria uma conversa no servidor, envia conversaId em cada interação e retoma o histórico do ID selecionado. O BFF preserva esse ID e valida seu formato. Abrir outra conversa não herda o assunto anterior.
- Antes de usar histórico, o backend verifica autor/empresa e revalida fontes, revisões, validade e permissões pelo mecanismo já existente.
- ContextualizacaoRagService recebe somente turnos elegíveis. Com histórico, o modelo resolve referências para uma pergunta autossuficiente destinada à busca. O JSON retornado é validado; resposta malformada ou falha técnica resulta em indisponibilidade, sem consulta ou resposta fabricada.
- Histórico é dado não confiável para interpretar o assunto, não instrução ou política. Fontes são recuperadas novamente com a pergunta contextual. GERAR_NOVA recebe a pergunta atual, o histórico selecionado e as fontes atuais autorizadas.
- Referência sem contexto ou ambiguidade retorna PRECISA_ESCLARECIMENTO, preservando a pergunta original e o pedido de esclarecimento no histórico. Não busca, gera resposta de negócio nem concede créditos nesse caso.
- CONSULTAR continua separado de REUTILIZAR. A geração não concede recompensa de reutilização. A chave da operação mantém a idempotência existente.
- A leitura de fonte indisponível não invalida a transação. Foi corrigido o erro 500 que ocorria quando uma recusa esperada de leitura era capturada no histórico após revogação.

## Contratos

POST /api/rag/conversas; GET /api/rag/conversas; GET /api/rag/conversas/{id}/historico; POST /api/rag/perguntas com conversaId opcional para clientes legados. O cliente legado sem ID ainda inicia conversa independente.

O modelo de interpretação e geração utiliza hestia.rag.modelo, configurável também por HESTIA_RAG_MODELO. A chave OPENAI_API_KEY permanece somente no servidor. Nesta validação foi usado gpt-4.1-mini com o provedor real.

## Verificação

SCRUM-546-evidencias-reais.json registra o ensaio com IA real, política didática v1.1 e PostgreSQL/pgvector no banco hestia_demo_545:

- pergunta inicial e continuação com fontes;
- mesma continuação em conversa nova pede esclarecimento;
- retomada mantém as duas interações na conversa correta;
- outro usuário, inclusive ADMIN, não acessa a conversa do autor;
- revogação oculta fontes antigas e impede que entrem na interpretação seguinte;
- geração não aumenta reutilizações/créditos.

Os sete controles passaram. Os testes automatizados também cobrem a reformulação da busca, envio de histórico/fontes atuais ao modelo, esclarecimento, isolamento e o erro de transação após revogação. Suíte Java: 329 aprovados e 2 externos não executados, sem falhas. Frontend final: 34 testes aprovados, zero falhas. Os seis blocos JavaScript das telas foram verificados após renderizar os valores EJS.

## Reproduzir e integrar

Preparar o ambiente e a fonte pela SCRUM-543. Definir HESTIA_RAG_FONTE_ID, HESTIA_RAG_BASE_URL se necessário, HESTIA_RAG_RELATORIO para o arquivo de saída e executar node docs/scrum/validar-conversa-rag.cjs. O roteiro é exclusivo da demonstração isolada: altera temporariamente os perfis da fonte e restaura a lista original no finally.

A janela atual é provisória: até quatro interações recentes revalidadas, pergunta anterior limitada a 1000 caracteres e resposta anterior a 1500, além dos limites existentes de fontes. Isso evita envio irrestrito nesta implementação, mas não representa a conclusão da SCRUM-547.

**SCRUM-547:** está atribuída a Ricardo no Jira. Seu ponto de integração está marcado no InteracaoRagService, antes de chamar ContextualizacaoRagService. A política de orçamento configurável e resumos deve preservar a ligação com as fontes/revisões para a SCRUM-551. Não persistir resumo como se fosse política normativa. SCRUM-551 e 556 continuam com suas dependências e aceites próprios.

Ocultar histórico na interface não apaga os registros persistidos do servidor. Não foi implementada uma segunda base de histórico nem uma API nova de exclusão.

Implementação e validação locais prontas para revisão; sem commit, push ou merge realizados pelo assistente.
