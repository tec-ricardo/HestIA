# Operação da Sprint 4

## Ambientes

O Compose padrão com override continua sendo demonstração local. Para publicação foi criado compose.release.yaml, sem perfil demo, com sessão persistida no PostgreSQL e portas do banco/API não publicadas. A interface fica ligada a 127.0.0.1:3000 e exige proxy HTTPS no host. TRUST_PROXY=1 pressupõe exatamente esse proxy e não deve ser usado com acesso direto externo à porta Node.

Definir DB_PASSWORD, SESSION_SECRET, SESSION_DATABASE_URL (URL PostgreSQL com senha devidamente codificada), PUBLIC_APP_URL e HESTIA_VCS_REF no ambiente. SMTP usa SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD e RECOVERY_FROM. Nunca colocar valores reais no Git. A configuração ainda não foi publicada em hospedagem externa.

## Instalação

Usar Java 21, Node 22 e PostgreSQL 17 com pgvector. A compilação deve usar uma pasta limpa: resíduos de migrações antigas em target podem provocar duplicidade de versões. Executar testes Java e Node antes de gerar imagens. Flyway aplica as migrações novas sem alterar arquivos históricos.

Na raiz do checkout, `docker compose -f compose.release.yaml build` e depois `docker compose -f compose.release.yaml up -d`. Esse procedimento depende das variáveis acima. Não usar `down -v` no ambiente com dados.

## Entrega e recuperação

O workflow Release verificável testa e gera imagens com o identificador do commit. A publicação no host externo continua dependente da escolha da hospedagem. Para recuperar uma versão, preservar imagens anteriores e confirmar compatibilidade de migrações; não fazer downgrade automático do banco. Restaurar backup em ambiente separado antes de qualquer recuperação operacional.

## Backup

scripts/validar-restauracao.ps1 recebe ContainerOrigem, Banco, Usuario e Destino. Gera dump, restaura em outro container sem rede, compara usuários, registros, catálogo, ciências e migrações e emite hash do backup. Nunca sobrescreve backup existente. O ensaio não restaura sobre o banco original. A retenção e o responsável pelo armazenamento externo precisam ser definidos pela equipe.

## Diagnóstico

API: GET /actuator/health retorna estado sem detalhes internos. Frontend: GET /health verifica o processo; não comprova a saúde de todas as dependências. Consulte os logs do serviço afetado sem divulgar segredos. Banco indisponível: conferir saúde do PostgreSQL, volume e conectividade interna. IA indisponível: preservar uso e consultar estado da avaliação antes de repetir. Falha de avaliação: usar a nova tentativa autorizada em Avaliações automáticas. SMTP indisponível: conferir remetente, TLS e credenciais; solicitar novo link após recuperação. Falha de alertas: conferir alert-service e ALERT_SERVICE_URL.

## Limitações e aceite

Configuração de hospedagem, domínio e envio real de e-mail não estão disponíveis. O catálogo operacional inicia vazio: a equipe deve cadastrar conteúdo autorizado, revisar a fonte e publicar. Dados demonstrativos continuam restritos ao perfil demo. Não declarar produção, envio real ou conteúdo homologado a partir dos testes locais.
