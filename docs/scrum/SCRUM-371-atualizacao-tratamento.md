# SCRUM-371 — Alterar responsável, ação e status

Implementa PUT /tratamentos-nao-conformidade/{id}, integrado à abertura e às consultas da SCRUM-370. Não exige nova migração: utiliza os campos da V17.

Corpo completo de exemplo:
```json
{"responsavelId": 7, "acaoCorretiva": "Orientar a equipe e corrigir o uso", "status": "EM_TRATAMENTO", "versaoRegistro": 0}
```

Leia primeiro GET /tratamentos-nao-conformidade/{id} e envie a versaoRegistro recebida em tratamento. A resposta PUT contém o detalhe e a nova versão. Uma edição desatualizada retorna 409; recarregue antes de reenviar. Não reutilize automaticamente a nova versão para sobrescrever alterações de outra pessoa.

ADMIN edita na própria empresa e pode atribuir um usuário ativo dessa empresa. GESTOR edita e atribui apenas no próprio departamento. FUNCIONARIO não edita. Identificadores fora do escopo retornam 404. Responsável inativo e ausência de responsável ou ação em EM_TRATAMENTO/RESOLVIDA retornam 409. Payload inválido retorna 400. Use a sessão Bearer existente.

ABERTA permite responsável e ação opcionais; null remove os valores. Ação é limitada a 1000 caracteres e espaços nas extremidades são removidos. EM_TRATAMENTO e RESOLVIDA exigem ambos. Os três estados podem ser definidos por um gestor autorizado, inclusive reabertura ou resolução direta quando já há ação concluída; a task não especifica uma sequência obrigatória.

A edição preserva avaliação, motivo, ocorrência, revisão de política e autor/data de abertura. Resolver tratamento não transforma a avaliação original em CONFORME. O interceptor de auditoria existente registra método, rota, usuário e resultado. Histórico detalhado das alterações e revisão justificada da avaliação permanecem no escopo da SCRUM-372.

## Como conferir
1. Abra uma ocorrência NAO_CONFORME pela API da SCRUM-370.
2. Consulte o detalhe e copie id e versaoRegistro.
3. Envie o PUT acima com um responsável real autorizado.
4. Consulte novamente: status, responsável e ação devem persistir; ocorrência e fundamentos permanecem iguais.
5. Marque RESOLVIDA usando a versão nova e uma ação concluída.
6. Reenvie uma versão antiga: deve retornar 409.
7. Teste outro departamento com GESTOR e outra empresa: acesso negado sem alteração.

Os testes criam dados apenas no ambiente de testes. Não são inseridos usuários operacionais fictícios. A interface visual para esse fluxo não faz parte desta entrega de API.

## Validação executada em 21/10/2026
- Suíte geral: 194 testes encontrados, 174 aprovados, 20 condicionais ignorados, nenhuma falha.
- Rodada PostgreSQL: 29 testes aprovados, nenhum ignorado ou com falha; inclui os 11 cenários de tratamento e as verificações existentes de busca/reutilização.
- PostgreSQL executado em schemas temporários da base de verificação, removidos pelos testes. A base operacional não foi alterada.
- git diff --check sem problemas.
