# HestIA — versão integrada para apresentação

13/11/2026. Código no repositório do IntelliJ, branch finalizacao. Base 85bb4b5; integração adicional da branch Segurança/571 aplicada sem commit automático. Colaboração, composição dos fatores/578, ranking/579, investigação/580, comparação temporal/582, Produtividade/572 e documentação/594 presentes. Nenhuma recuperação de senha foi adicionada ao escopo.

## Como iniciar

Mantenha o banco e o Docker que o grupo já usa. No PowerShell do IntelliJ, na raiz HestIA, configure a chave da OpenAI de forma oculta conforme as instruções do grupo e execute o backend no mesmo terminal:

```powershell
$chave = Read-Host 'Cole a chave da OpenAI' -AsSecureString
$env:OPENAI_API_KEY = [pscredential]::new('openai', $chave).GetNetworkCredential().Password
Remove-Variable chave
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Mantenha as variáveis de banco já usadas. Inicie o site com npm start na pasta Front-End/hestia-prototipo-sprint1-atualizado, mantendo a configuração local existente. A chave deve ficar no backend Java. Não incluí-la no código, Git, slides ou evidências. Perfil demo é para apresentação local.

## Contas da demonstração

Administrador: admin.demo@hestia.local. Gestor: gestor.demo@hestia.local. Colaborador: colaborador.demo@hestia.local. Senha pública das contas fictícias: Hestia@2026. Não usar credenciais de usuários reais na banca.

## Sequência sugerida

1. Colaborador: registrar utilização e mostrar como falta de evidência gera revisão, sem aprovação automática.
2. Gestor: consultar ocorrência, fundamentos e capacitação do próprio departamento.
3. Colaborador: concluir curso/avaliação; verificar trilha e recompensa única.
4. Administrador: catálogo e revisão/publicação; mostrar histórico preservado.
5. Gestor/administrador: resultados, CSV, ranking e investigação. Mostrar notas ausentes e amostra insuficiente; não apresentar ausência como zero nem calcular score global incompleto.
6. RAG: usar política fictícia v1.1 e corpus SCRUM-545-corpus.json. Perguntar ferramentas permitidas, dados pessoais, senhas em prompts, validação de fatos e política retirada; continuar uma pergunta na mesma conversa e abrir uma nova para demonstrar isolamento. Pergunta sobre bolo, férias ou astronomia deve mostrar ausência de fonte. Validar geração real após configurar a chave.
7. Encerrar com limites e evidências. Apresentação editável: preparacao/HestIA-DemoDay-preparacao.pptx; roteiro e falas: ../scrum/SCRUM-597-roteiro-demonstracao.md.

## Evidências da integração

397 testes Java: 395 aprovados, 2 externos ignorados, zero falhas/erros. 52 testes do frontend aprovados. Mais 51 verificações HTTP locais: 10 relatórios, 25 ranking/investigação e 16 perfis. Relatório ../scrum/evidencias/INTEGRACAO-FINAL-PROFESSORA.json. Os testes anteriores de jornada e RAG estão identificados por suas bases históricas.

## Passos que exigem confirmação real

RAG ao vivo validado em 13/11: política publicada/indexada com OpenAI; 8/8 casos de busca e 7/7 cenários de conversa aprovados, incluindo resposta gerada pela tela. Nesta sessão, site http://localhost:3300 e backend Java http://127.0.0.1:18080, com banco demonstrativo separado hestia_demo_final_rag e fonte 2. A chave foi fornecida pelo canal de entrada ao processo Java; não foi gravada em arquivo. Se encerrar o backend, configure a variável de ambiente novamente. Usar o roteiro e script de publicação da política já existentes; não assumir que IDs e embeddings de outro banco estão disponíveis neste.

Cloud: nenhum endereço público informado. Esta versão é integrada e testada localmente; não representa homologação cloud. Regras de prazo/submissão e ensaio dos quatro integrantes ainda precisam de confirmação/execução. Nenhum comprovante de submissão foi produzido. Custos/metas não aprovados permanecem ausentes; produtividade simulada e política fictícia devem ser anunciadas como tais.

## Correção encontrada no ensaio real

A integração havia fixado o limiar em 0,70, ignorando a configuração demo já existente (0,40); o primeiro ensaio acertou 3/8. Restaurado o valor configurável sem mudar o padrão de produção, 8 testes direcionados passaram e o corpus real passou 8/8. O verify completo de 395 aprovados foi executado antes desta pequena correção; o recorte direcionado e a execução real foram executados depois. Relatórios RAG-FINAL-BUSCA.json e RAG-FINAL-CONVERSA.json. A amostra é didática e não prova qualidade em outros documentos.
