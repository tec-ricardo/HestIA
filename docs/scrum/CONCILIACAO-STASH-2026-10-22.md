# Conciliação do stash com a develop — 22/10/2026

Base integrada: `6b4cc94` (PR #112). Trabalho recuperado: stash `042896ff7102a9ded2ca7be656068675c49967ce`, criado sobre `90c9529`.

## Resolução

- `package.json`: preservado `node --test`, que executa tanto `test/` quanto `tests/`.
- `server.js` e `sessao-api.js`: login único com contexto autenticado do backend, usuário/empresa na sessão e token `apiToken`. O contexto por requisição encaminha esse token para as integrações recuperadas. Login recusado invalida a identidade anterior; logout mantém a revogação e o encerramento local. Acesso demonstrativo não autoriza APIs reais.
- `/maturidade` mantém a consulta de confiabilidade das SCRUM-427–430. `/maturidade/resultados` disponibiliza fontes, metodologia, resultados e histórico das SCRUM-422–426/431–439, com links entre as consultas. Links anteriores com `fonte` são redirecionados. A demonstração permanece identificada em `/maturidade/demonstracao`.
- Microtreinamentos usam o contrato de avaliação publicado no backend: cinco questões e aprovação em 80% (SCRUM-494–496). A abertura da avaliação cria/recupera a tentativa real.
- A tela de certificações da SCRUM-486 consulta as concessões persistidas das SCRUM-484/485. Finalizar a leitura não equivale a aprovação ou concessão. O usuário consultado vem da sessão; evidências internas não são enviadas ao navegador.
- Demais arquivos do stash foram conciliados com as alterações posteriores da develop. Nenhuma mudança posterior foi substituída por uma cópia integral do snapshot antigo.

## Validação

- Frontend: 14 testes aprovados, zero falhas. Incluem as duas suítes existentes e regressões de sessão compartilhada, identidade, login recusado, expiração, concessão de certificações e sintaxe dos scripts EJS.
- Java: 264 testes nos relatórios consolidados, 262 aprovados, zero falhas/erros e dois ignorados. A primeira execução completa foi complementada por 55 testes com PostgreSQL/PGVector, todos aprovados.
- Os dois testes ignorados exigem integração externa: chamada real ao provedor de embeddings e ponta a ponta do serviço de alertas. Não foi feita chamada paga ao provedor.
- As 23 migrações foram validadas em banco descartável, incluindo a atualização de V17 para V23. Nenhum banco operacional foi utilizado.
- Sintaxe do servidor, ausência de marcadores de conflito e verificação de espaços do diff conferidas.

## Limites de escopo

As consultas operacionais de confiabilidade e as apurações experimentais conservam seus próprios contratos. Esta conciliação não presume equivalência entre populações, fontes e fórmulas: o adaptador `ConfiancaIndicadoresPort` das apurações ainda informa `NAO_CLASSIFICADA` quando não há fornecedor específico. Configuração de fontes, publicação de questionários/requisitos e homologação acadêmica continuam seguindo os contratos documentados.

Os arquivos são recuperados como alterações locais na branch existente. Não há publicação, merge remoto ou fechamento de tasks por esta operação. A revisão independente exigida por `CONTRIBUTING.md` permanece obrigatória antes de um merge.

## Recuperação de segurança

O stash original é preservado pela referência Git local `refs/backup/stash-20261022-042896f`. Ela mantém o conteúdo original mesmo após a retirada da entrada ativa do GitHub Desktop. O hash original também está registrado acima. Não reaplique esse backup sobre a integração pronta; ele representa o estado anterior à conciliação.
