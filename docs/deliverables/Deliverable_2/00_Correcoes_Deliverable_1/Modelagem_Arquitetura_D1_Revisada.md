# Revisão da arquitetura inicial - Deliverable 1

Revisão conceitual de 20/10/2026. Não há feedback docente específico fornecido.
Base: SCRUM-102, SCRUM-105 e SCRUM-106.

O estilo escolhido para o núcleo da HestIA é monólito modular com camadas
internas por domínio. O Alert Service é um processo auxiliar Node/Express
acessado por REST; sua existência não caracteriza todo o sistema como
microsserviços. A interface web e a API são contêineres distintos.

O C4 inicial representa arquitetura-alvo. Fachadas entre módulos, limites
estritos de dependência e metas de desempenho são objetivos de projeto;
não devem ser apresentados como propriedades comprovadas por aquele artefato.
O acesso direto de services a repositories de outro módulo exige identificação
como acoplamento e futura decisão de refatoração.

A descrição inicial de componentes planejados deve ser lida com a data da
Sprint 1. A implementação evoluiu posteriormente. A visão atual está no relatório
de Modelagem D2 e nos diagramas `docs/architecture/c4-*-d2.mmd`.

Correção: separar decisão, arquitetura-alvo, implementação e evidência de teste.
Justificativa: evitar que a representação planejada seja confundida com
comprovação do funcionamento ou do isolamento dos módulos.
