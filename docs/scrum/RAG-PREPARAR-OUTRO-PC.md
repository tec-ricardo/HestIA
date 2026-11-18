# RAG em outro computador — 13/11/2026

Atualizar o Git copia código e configuração; não copia fontes, aprovações, permissões e embeddings do banco local. Na instância validada, a política fictícia v1.1 (fonte 2) foi publicada e indexada separadamente, com cinco trechos. DemoDataInitializer apenas cria uma resposta curada de exemplo e suas permissões/versão; não chama o indexador. É possível ter o código novo e continuar sem conteúdo recuperável.

## Preparação

1. Conferir develop contendo b0b904c e reiniciar/recompilar o backend. Um processo Java, JAR ou imagem Docker antiga não muda só porque os arquivos foram atualizados.
2. Iniciar com perfil demo e a chave OpenAI no ambiente do mesmo processo Java, mantendo o banco local correto. O perfil demo usa similaridade mínima 0,35; sem ele, o padrão é 0,70. Não usar configuração demo em produção.
3. Publicar e reindexar a política no próprio banco, usando o script abaixo. Ele pede a senha demo de forma oculta, não a chave OpenAI. O ID é retornado pelo ambiente; não fixar fonte 2 ou empresa 1.

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
# Em outro terminal, depois de o backend iniciar:
.\docs\scrum\publicar-politica-demo.ps1 -BaseUrl 'http://localhost:8080'
.\docs\scrum\publicar-politica-demo.ps1 -BaseUrl 'http://localhost:8080' -SomenteVerificar
```

Substituir a porta pela porta real do Java. O script verifica CNPJ reservado/nome da empresa fictícia e perfil ADMIN, aprova a fonte, aplica os perfis e gera os embeddings/trechos pela API. A chave não é incluída no script ou no Git. Se a execução de scripts for bloqueada pelo Windows, executar o arquivo com powershell.exe -NoProfile -ExecutionPolicy Bypass -File, mantendo o mesmo caminho e argumentos; não alterar política global.

## Resultado e diagnóstico

SomenteVerificar confirma o contexto/empresa/fonte e pesquisa o exemplo de CPF pelo limite efetivo do servidor. Não publica nem altera a política. A busca pode registrar auditoria e consultar embeddings no provedor.

- Política não encontrada: fonte não publicada no banco/empresa usados por esse backend.
- HTTP 401/403: conta, perfil, sessão ou escopo incorretos.
- HTTP 503 na indexação/busca: verificar chave no processo Java, acesso/crédito do provedor e conexão.
- Fonte existe mas CPF não recupera: verificar perfil demo/variável que sobrescreve o corte, processo antigo, reindexação e permissões da fonte.
- Outro usuário/empresa: conferir o cadastro; o isolamento impede usar a fonte de outra empresa.

Não há acesso ao computador dos colegas nesta investigação. Esses são mecanismos comprovados no código e diferenças da instância local; o resultado do diagnóstico identifica qual se aplica a cada PC. Não enviar .env, chave, token de sessão ou log que os contenha.
