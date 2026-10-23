# Ambiente de demonstração da Sprint 3

Esta carga prepara um cenário navegável no PostgreSQL usando as estruturas reais do produto. Ela não substitui regras, serviços ou integrações por mocks. O perfil `demo` apenas insere dados iniciais identificáveis e pode ser executado mais de uma vez sem duplicá-los.

## Como iniciar

1. Inicie os serviços do `compose.yaml`.
2. Inicie o backend com o perfil `demo`: `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`.
3. Inicie o frontend normalmente.
4. Acesse a aplicação e use uma das contas abaixo.

| Perfil | E-mail | Senha |
| --- | --- | --- |
| Administrador | `admin.demo@hestia.local` | `Hestia@2026` |
| Gestor | `gestor.demo@hestia.local` | `Hestia@2026` |
| Funcionário | `colaborador.demo@hestia.local` | `Hestia@2026` |

## Cenário preparado

- empresa QSoft de demonstração, dois departamentos e três perfis de acesso;
- ferramenta aprovada e política ativa com histórico preservado;
- registros de uso, uma não conformidade em tratamento e responsável atribuído;
- resposta curada, aprovada, versionada e autorizada para os três perfis;
- catálogo real de microtreinamento, questionário e permissão de capacitação;
- progresso de gamificação e missões;
- apuração de maturidade para alimentar a apresentação.

## Limite conhecido do RAG

A resposta curada fica pronta para indexação. A busca semântica e a geração usam o provedor real configurado pelo projeto e, por isso, exigem `OPENAI_API_KEY`. A carga demo não grava vetores inventados e não simula uma resposta do provedor. Sem a chave, ainda é possível demonstrar governança, revisão, perfis e histórico do conteúdo.

O perfil `demo` deve ser usado somente na apresentação e em desenvolvimento. Sem esse perfil, nenhuma carga é executada.
