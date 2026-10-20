# SCRUM-417 — Estrutura de textos, metadados e embeddings

## Organização

A base de conhecimento utiliza respostas_reutilizaveis como registro
operacional e resposta_versoes como histórico do conteúdo.

| Informação | Armazenamento |
|---|---|
| Pergunta original | respostas_reutilizaveis.prompt_original |
| Resposta | respostas_reutilizaveis.resposta_gerada |
| Empresa | respostas_reutilizaveis.empresa_id |
| Perfis autorizados | resposta_perfis |
| Disponibilidade | respostas_reutilizaveis.reutilizavel |
| Indicação de conteúdo sensível | respostas_reutilizaveis.possui_dados_sensiveis |
| Origem e tipo | resposta_versoes.origem e tipo_conteudo |
| Número da versão | resposta_versoes.numero_versao |
| Integridade do conteúdo | resposta_versoes.conteudo_sha256 |
| Data da versão | resposta_versoes.criada_em |
| Cópia histórica dos textos | resposta_versoes.prompt_original e resposta_gerada |
| Embedding serializado | respostas_reutilizaveis.embedding |
| Vetor para recuperação | respostas_reutilizaveis.embedding_vector |

## Integridade e recuperação

A combinação resposta_id e numero_versao é única.
Cada versão referencia a resposta operacional por chave estrangeira.

O vetor possui 1.536 dimensões e índice HNSW com distância cosseno.
A busca restringe os resultados por empresa, perfil autorizado,
disponibilidade e ausência de dados sensíveis.

Atualizações de conteúdo preservam o histórico e removem o embedding
anterior. A retirada desabilita a recuperação e preserva os registros.

## Migrations

- V8: textos e registro operacional da resposta.
- V11: extensão pgvector, coluna vetorial e índice HNSW.
- V14: permissões de recuperação por perfil.
- V15: metadados e histórico de versões.

## Evidências existentes

- RespostaVersaoRepositoryTest: histórico, hash, duplicidade e consulta por empresa.
- BuscaVetorialPostgresTest: migrations, extensão, índice e filtros de recuperação.
- ManutencaoConteudoPostgresTest: cadastro, atualização, histórico e retirada.

## Limites da estrutura

Os embeddings estão associados à resposta operacional.
Não existe associação explícita entre vetor e número da versão.

O serviço de limpeza e divisão existe, mas os trechos ainda não possuem
armazenamento próprio nem participam da busca atual.

A base sintética de uso de IA não fornece conteúdo textual para indexação.