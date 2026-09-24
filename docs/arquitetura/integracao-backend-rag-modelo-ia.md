# Integração entre backend, RAG e modelo de IA

## Objetivo

Este documento define o fluxo de integração responsável entre o backend HestIA, a camada de recuperação de conhecimento (RAG) e provedores de modelos de IA. O desenho prioriza reutilização, rastreabilidade, isolamento por empresa e controle de custos.

## Fluxo principal

1. O cliente autenticado envia a solicitação ao backend HestIA.
2. O backend valida sessão, perfil, empresa e política aplicável.
3. A camada RAG procura uma resposta reutilizável da mesma empresa, sem dados sensíveis e marcada como reutilizável.
4. Se houver correspondência adequada, a resposta é devolvida sem nova chamada ao modelo.
5. A reutilização incrementa `quantidade_reutilizacoes` e `tokens_economizados`, registra XP e avança a missão `REUTILIZACAO_EFICIENTE`.
6. Sem correspondência, o backend consulta o provedor de IA autorizado.
7. A execução é persistida em `registros_uso_ia`, incluindo modelo, finalidade, tokens, custo, duração e status.
8. A resposta elegível é indexada para reutilização futura.
9. A ação HTTP é gravada em `logs_auditoria` e pode ser submetida à avaliação de conformidade.

## Responsabilidades

### Backend HestIA

- autenticar e autorizar usuários;
- aplicar configurações e políticas da empresa;
- impedir ferramentas bloqueadas ou não aprovadas;
- orquestrar busca RAG e chamada do modelo;
- registrar uso, auditoria, conformidade, economia e gamificação;
- nunca expor senha, hash de sessão ou conteúdo de outra empresa.

### Camada RAG

- filtrar documentos por `empresa_id` antes da busca semântica;
- excluir respostas com dados sensíveis ou `reutilizavel = false`;
- retornar identificador, similaridade e metadados da resposta selecionada;
- tratar o embedding como índice derivado, mantendo o texto original no banco transacional.

### Provedor de IA

- receber somente o contexto mínimo necessário;
- usar ferramenta e modelo previamente aprovados;
- devolver métricas de tokens e latência quando disponíveis;
- não ser chamado quando a camada RAG produzir resposta válida.

## Contratos e rastreabilidade

- `POST /registros-uso-ia`: registra uma chamada efetivamente realizada.
- `PATCH /api/respostas-reutilizaveis/{id}/reutilizar`: registra uma chamada evitada.
- `GET /api/respostas-reutilizaveis/empresa/{empresaId}/economia`: consolida chamadas e tokens economizados.
- `POST /avaliacoes-conformidade`: associa registro, política, critérios e resultado.
- `GET /auditoria`: permite consulta administrativa dos eventos.
- `GET /gamificacao/usuarios/{usuarioId}/passe`: apresenta XP, nível e missões.

## Segurança e falhas

- Toda consulta deve ser limitada à empresa do usuário autenticado.
- Tokens de sessão são armazenados somente como SHA-256 e expiram em oito horas.
- Falha no provedor gera registro com status `ERRO`; não deve criar resposta reutilizável.
- Falha ao indexar no RAG não invalida o registro transacional; a indexação pode ser repetida de forma idempotente.
- Reutilizações e recompensas usam referências únicas para impedir contagem duplicada.

## Observabilidade

Os indicadores mínimos são chamadas realizadas, chamadas evitadas, tokens de entrada e saída, tokens economizados, custo estimado, latência, taxa de erro, conformidade e evolução das missões. Logs técnicos não devem conter prompts completos quando houver risco de dados pessoais.
