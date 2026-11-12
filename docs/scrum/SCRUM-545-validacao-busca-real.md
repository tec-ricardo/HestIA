# SCRUM-545 — Busca com embeddings reais e política didática

Data: 12/11/2026. Responsável: Luccas Toffano. Branch: features/SCRUM-545-546-busca-contexto, a partir de develop 85a3e82.

## Escopo e ambiente

Por decisão explícita do usuário, o corpus é a política fictícia da SCRUM-543, versão 1.1. Não se trata de política oficial da QSoft. O antigo dossiê SCRUM-542 registrava a falta de política empresarial oficial; isso não é apresentado como documento obtido. A validação abaixo usa a política didática aprovada para a demonstração.

PostgreSQL/pgvector local em banco separado hestia_demo_545, perfil demo, fonte 2. Esse ID é exclusivo deste banco. O banco de Knorre, hestia_demo_543, não estava neste computador. A carga foi reproduzida com publicar-politica-demo.ps1; o ID retornado foi conferido, sem assumir que seria igual.

Provedor real: OpenAI, embeddings text-embedding-3-small (1536 dimensões). Credencial fornecida pelo usuário somente ao processo local, sem arquivo de ambiente versionado. O banco normal da aplicação não foi usado para a carga/testes.

## Problemas comprovados e ajustes

1. O índice vetorial representava só o título do conteúdo. A indexação passou a incluir título e corpo, preservando os filtros existentes, a conferência de mudanças concorrentes e a infraestrutura de índice.
2. Após reindexar, o limiar de 0,70 ainda recusava as cinco perguntas pertinentes. A comparação de limiares está nas evidências. O maior limiar ensaiado que preservou os cinco positivos e rejeitou os três negativos foi 0,40.
3. O perfil demo usa hestia.rag.similaridade-minima=0.40. Fora dele, o padrão permanece 0,70. A API de busca e a interação utilizam o mesmo padrão configurado; um limiar explícito na API continua sujeito às validações existentes.
4. O script da SCRUM-543 confundia uma página de resultados com um único registro no PowerShell, produzindo IDs como “2 1” ao ser executado novamente. A enumeração foi corrigida e a reindexação da fonte existente foi executada.

Fontes já indexadas precisam ser reindexadas para incorporar o corpo; a alteração não reescreve vetores históricos automaticamente. No banco de validação isso foi feito pela API.

## Evidência

Corpus reproduzível: SCRUM-545-corpus.json. Resultado inicial: 3/8 acertos (os três negativos), com cinco falsos negativos. Resultado final: 8/8, cinco positivos recuperados e três negativos sem fonte. Consulta não incrementou contadores; confirmação explícita seguida de reenvio incrementou uma única vez.

Arquivos: SCRUM-545-evidencias-iniciais.json, SCRUM-545-evidencias-reindexacao.json e SCRUM-545-evidencias-finais.json. Contêm perguntas, similaridades observadas, limiares comparados e verificações de contagem; não contêm tokens de autenticação nem chave do provedor.

**Limitação:** oito casos de uma política fictícia são uma amostra de demonstração. O limiar não foi validado para outros documentos, populações, perguntas ou provedores. Similaridade não é probabilidade de acerto nem prova de que o documento contém a resposta. A tela identifica a fonte fictícia e a calibração em amostra pequena; confiança não é fabricada.

## Reproduzir

1. Iniciar backend com perfil demo em banco separado, migrações aplicadas e OPENAI_API_KEY disponível apenas no servidor.
2. Executar docs/scrum/publicar-politica-demo.ps1 e anotar o ID retornado.
3. Definir HESTIA_RAG_FONTE_ID e, opcionalmente, HESTIA_RAG_BASE_URL e HESTIA_RAG_RELATORIO.
4. Executar node docs/scrum/validar-busca-rag.cjs. A senha da conta demo pode ser fornecida por HESTIA_DEMO_PASSWORD; o padrão é o da carga didática existente.

O roteiro cria consultas e uma confirmação no ambiente de demonstração. Código de saída diferente de zero significa que há caso a analisar; não converter falha em resultado aprovado.

## Verificação técnica

Suíte Java final: 331 testes, 329 aprovados, zero falhas/erros e 2 externos não executados (embedding isolado e alert-service E2E). O embedding real foi exercitado separadamente pelos roteiros desta tarefa. Há regressões de autorização, concorrência e indexação, incluindo a garantia de que o corpo chega ao provedor. A qualidade da geração contextual é registrada na SCRUM-546 e o aceite final segue na SCRUM-556.

Implementação e validação locais prontas para revisão; commit, CI e merge não foram executados pelo assistente.
