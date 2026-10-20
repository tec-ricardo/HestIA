# SCRUM-415 — Validação com perguntas conhecidas

## Escopo

Validação da recuperação vetorial no PostgreSQL com pgvector,
usando conteúdo fictício e embeddings determinísticos de 1.536 dimensões.

O conteúdo pertence exclusivamente aos testes. Não representa políticas
da QSoft nem foi extraído da base sintética de uso de IA.

## Perguntas e respostas de referência

| Pergunta | Resposta esperada |
|---|---|
| Posso enviar dados pessoais para uma IA? | Neste cenário de teste, dados pessoais não devem ser enviados. |
| Quais ferramentas de IA posso utilizar? | Neste cenário de teste, utilize somente ferramentas aprovadas. |
| Preciso revisar uma resposta gerada por IA? | Neste cenário de teste, revise o conteúdo antes de utilizá-lo. |

A reformulação “É permitido colocar informações pessoais em ferramentas
de IA?” utiliza um vetor equivalente ao da primeira pergunta.

## Cenários validados

- Recuperação da resposta esperada como primeiro resultado para cada pergunta.
- Recuperação da mesma resposta para a reformulação controlada.
- Exclusão de conteúdo retirado, mesmo quando o vetor permanece armazenado.
- Exclusão de conteúdo de outra empresa, de perfil não autorizado ou sensível.
- Retorno vazio quando a base não contém respostas.

## Execução e resultado

Arquivo: `src/test/java/br/com/hestia/rag/service/PerguntasConhecidasPostgresTest.java`.

Comando: `.\mvnw.cmd "-Dtest=PerguntasConhecidasPostgresTest" test`.

Resultado local: 5 testes executados, 0 falhas, 0 erros e 0 ignorados.
As 15 migrations foram aplicadas nos schemas de teste.

## Limites da evidência

O teste utiliza busca SQL e cálculo de distância reais no PostgreSQL.
A geração dos embeddings e a leitura das entidades pelo repositório
são simuladas.

A equivalência da reformulação é definida pelo teste. Não foi medida
a qualidade semântica de embeddings produzidos pela OpenAI.

A busca atual não possui limite mínimo de similaridade. Não foi validada
a rejeição de perguntas sem relação com os conteúdos disponíveis.