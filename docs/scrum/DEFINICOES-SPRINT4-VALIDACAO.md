# Validação das definições da Sprint 4

Data: 12/11/2026. Base 096a06a, branch correções. Não houve commit/push/merge automático nesta entrega. O aceite de produto está registrado em DECISOES-SPRINT4-INTEGRACAO.md. Esta validação não é aprovação acadêmica ou empresarial.

## Verificações executadas

- 32 testes Java aprovados, zero falhas/erros/ignorados, no recorte ClassificacaoRiscoDepartamentalTest, SelecaoAiLiteracyTest, CalculadorasMaturidadeTest, CatalogoControlesSegurancaTest e ResultadoMaturidadeServiceTest. Compilação das fontes e testes do projeto concluída. A suíte completa não foi repetida nesta etapa; a combinação anterior teve 361 aprovados e 2 externos ignorados, conforme correcoes-validacao.json.
- AI Literacy: última tentativa válida, reprovação posterior, versões distintas, desempate temporal por ID, limites inclusivos do período, peso igual por pessoa, descarte de tentativa sem evidência, deduplicação e rejeição de duplicata conflitante.
- Risco: limites 25/50, zero observado versus ausência, amostra frágil, versões/naturezas incompatíveis, duplicação de fatores e classificação antes de arredondar. Exemplos calculados pelo teste exportados para evidencias/risco-departamental-cenarios.json.
- Fonte GitHub real: paginação e 25 merges no recorte da coleta; 25 conjuntos de reviews consultados; quatro autores. Nenhuma aprovação formal elegível observada.
- PostgreSQL 17, stack descartável com carga demo: criação de empresa/departamento separados da QSoft e quatro autores históricos com login desabilitado. Segunda execução preservou os mesmos IDs. O script recusou alvo comum sem identificação demo/validação antes de qualquer acesso.

## O que fica para a integração de Knorre

Os helpers não substituem nem fazem merge das branches 573/576/579/580. Consumir a seleção da 573 após filtros autorizados; usar o JSON de vínculos gerado **no banco dele** na 576; anexar o classificador às linhas da 579 e ligar cada fator aos registros de origem da 580. Registrar validação dessas rotas após integração. Não afirmar que a classificação já aparece na tela só porque os testes do helper passaram.

FinOps continua sem fonte/moeda/orçamento; Produtividade sem meta/fonte operacional, com simulação separada; Segurança exige provas por controle/release. São decisões explícitas de tratamento, não convite para preencher valores.

## Relatórios do RAG já publicados

No commit 096a06a do repositório tec-ricardo/HestIA:

- docs/scrum/SCRUM-545-validacao-busca-real.md
- docs/scrum/SCRUM-545-evidencias-finais.json
- docs/scrum/SCRUM-546-continuidade-conversas.md
- docs/scrum/SCRUM-546-evidencias-reais.json
- docs/scrum/SCRUM-612-complemento-ricardo.md
- docs/scrum/evidencias/correcoes-validacao.json

Os relatórios 545/546 exercitaram o provedor real em suas execuções registradas. O ensaio de correções e os testes de fórmulas desta etapa não fazem chamadas reais a OpenAI. Corpus didático não é política oficial QSoft, e a amostra de oito casos não estabelece precisão universal.

Administrador opcional da empresa GitHub demo: criação e autenticação reais no backend do ensaio confirmaram perfil ADMIN e os mesmos IDs de empresa/departamento do cadastro. Os autores permaneceram desativados. Esse teste não prova que a rota da branch 576 já foi integrada.
