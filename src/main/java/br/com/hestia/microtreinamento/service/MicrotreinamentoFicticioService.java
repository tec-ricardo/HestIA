package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.Microtreinamento;
import br.com.hestia.microtreinamento.model.TemaTreinamento;

import java.util.List;

public class MicrotreinamentoFicticioService {

    public List<Microtreinamento> criarTreinamentos() {

        Microtreinamento curso1 = new Microtreinamento(
                1L,
                "Proteção de Dados em IA",
                "Proteção de informações pessoais, dados sensíveis e segredos corporativos durante o uso de IA.",
                """
                1. Entenda o risco
                                Uma mensagem enviada a um assistente pode conter CPF, endereço, contratos, credenciais ou estratégias internas. Mesmo um trecho aparentemente inofensivo pode permitir identificar pessoas ou revelar operações da empresa.
                
                Exemplo detalhado: considere um relatório de clientes. Antes de executar qualquer ação, identifique dados identificáveis e verifique quem é responsável pela decisão. A equipe deve substituir os identificadores por exemplos sintéticos; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                2. Classifique antes de compartilhar
                                Identifique se a informação é pública, interna, confidencial ou restrita. Confirme se a ferramenta foi aprovada para aquela categoria. Nunca cole senhas, tokens, dados bancários ou prontuários em serviços não autorizados.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para um relatório de clientes. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: substituir os identificadores por exemplos sintéticos. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                3. Minimize e proteja
                                Compartilhe apenas o indispensável. Substitua nomes por identificadores fictícios e remova dados indiretos, como cargo e localização, quando puderem reidentificar alguém. Dados pseudonimizados ainda podem ser pessoais.
                
                Erro frequente: o envio de CPFs ou dados pessoais ao serviço não autorizado. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                4. Situação prática
                                Um analista quer resumir reclamações de clientes. Em vez de enviar a planilha com nomes e telefones, ele extrai temas recorrentes em ambiente aprovado e fornece exemplos sintéticos à IA.
                
                Exercício guiado: use um relatório de clientes como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar dados identificáveis e propor substituir os identificadores por exemplos sintéticos. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                5. Checklist de conclusão
                                Antes do envio, confirme finalidade, autorização, classificação, minimização e canal de reporte. Se perceber exposição indevida, interrompa o uso e avise a equipe responsável, sem reproduzir o segredo no chamado.
                
                Checklist de aplicação: confirme a finalidade legítima; revise dados identificáveis; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize substituir os identificadores por exemplos sintéticos. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.
                """,
                TemaTreinamento.PROTECAO_DE_DADOS,
                18,
                true
        );

        Microtreinamento curso2 = new Microtreinamento(
                2L,
                "Uso de Ferramentas de IA Autorizadas",
                "Critérios para escolher ferramentas aprovadas, controlar permissões e evitar uso indevido de contas pessoais.",
                """
                1. Por que existe uma lista aprovada
                                Ferramentas variam em armazenamento de prompts, retenção, treinamento de modelos, controles de acesso e tratamento de dados. Uma ferramenta popular não é necessariamente adequada ao ambiente corporativo.
                
                Exemplo detalhado: considere uma ferramenta de transcrição. Antes de executar qualquer ação, identifique permissões e retenção de gravações e verifique quem é responsável pela decisão. A equipe deve consultar o catálogo corporativo e registrar a aprovação; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                2. Como verificar a aprovação
                                Consulte o catálogo interno, o responsável pela governança ou a equipe de segurança. Verifique a finalidade aprovada, o tipo de dado permitido e eventuais restrições de departamento.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para uma ferramenta de transcrição. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: consultar o catálogo corporativo e registrar a aprovação. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                3. Contas e permissões
                                Prefira identidade corporativa autorizada, autenticação multifator e permissões mínimas. Evite extensões ou conectores desconhecidos e não transfira arquivos corporativos para contas pessoais.
                
                Erro frequente: a utilização de uma conta pessoal com arquivos internos. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                4. Situação prática
                                Uma equipe precisa transcrever uma reunião interna. Antes de usar um serviço externo, confirma se a ferramenta está autorizada para gravações e se todos os participantes e dados envolvidos foram considerados.
                
                Exercício guiado: use uma ferramenta de transcrição como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar permissões e retenção de gravações e propor consultar o catálogo corporativo e registrar a aprovação. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                5. O que fazer diante de uma ferramenta nova
                                Não cadastre dados reais para testar. Solicite avaliação de segurança e privacidade, documente a finalidade e aguarde aprovação. Se uma ferramenta perder autorização, interrompa o uso e siga a orientação de migração.
                
                Checklist de aplicação: confirme a finalidade legítima; revise permissões e retenção de gravações; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize consultar o catálogo corporativo e registrar a aprovação. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.
                """,
                TemaTreinamento.USO_DE_FERRAMENTAS_AUTORIZADAS,
                16,
                true
        );

        Microtreinamento curso3 = new Microtreinamento(
                3L,
                "Políticas Internas para Uso de IA",
                "Aplicação das regras de governança, responsabilidades e registros no trabalho diário.",
                """
                1. Conheça as regras aplicáveis
                                A política de IA define ferramentas permitidas, usos proibidos, responsáveis, limites de dados e exigências de revisão. As regras podem variar conforme área, cliente e processo.
                
                Exemplo detalhado: considere um pedido urgente da liderança. Antes de executar qualquer ação, identifique as regras e responsabilidades internas e verifique quem é responsável pela decisão. A equipe deve documentar a finalidade e consultar a governança; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                2. Identifique sua responsabilidade
                                Quem solicita uma resposta continua responsável por verificar sua adequação. Gestores definem aprovações, segurança avalia riscos e governança orienta exceções; nenhuma dessas funções substitui o cuidado individual.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para um pedido urgente da liderança. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: documentar a finalidade e consultar a governança. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                3. Uso permitido versus proibido
                                Um rascunho de comunicação interna com informações públicas pode ser permitido; enviar uma base de clientes para um chatbot não autorizado pode ser proibido. Consulte a política antes de automatizar decisões sensíveis.
                
                Erro frequente: a dispensa informal de uma política obrigatória. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                4. Registre o necessário
                                Quando exigido, informe ferramenta, finalidade, categoria de dados, data e revisão realizada. Registros claros permitem auditoria e correção de incidentes.
                
                Exercício guiado: use um pedido urgente da liderança como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar as regras e responsabilidades internas e propor documentar a finalidade e consultar a governança. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                5. Caso prático e decisão
                                Se uma liderança pedir uso urgente de uma ferramenta não aprovada, não ignore a regra. Explique o risco, proponha alternativa autorizada e encaminhe pedido formal de avaliação.
                
                Checklist de aplicação: confirme a finalidade legítima; revise as regras e responsabilidades internas; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize documentar a finalidade e consultar a governança. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.
                """,
                TemaTreinamento.POLITICAS_INTERNAS,
                17,
                true
        );

        Microtreinamento curso4 = new Microtreinamento(
                4L,
                "Uso Responsável da Inteligência Artificial",
                "Uso ético, seguro e proporcional de IA com supervisão e avaliação de impactos.",
                """
                1. Defina o problema antes da ferramenta
                                A IA é um meio, não um objetivo. Avalie se a tarefa precisa de IA, quais dados serão utilizados, quem será afetado e como medir a qualidade da resposta.
                
                Exemplo detalhado: considere uma recomendação automatizada. Antes de executar qualquer ação, identifique os impactos sobre pessoas e processos e verifique quem é responsável pela decisão. A equipe deve definir revisão humana e critérios de contestação; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                2. Reconheça limitações
                                Modelos podem inventar fatos, reproduzir vieses e gerar resultados inconsistentes. Uma resposta bem escrita não comprova precisão nem adequação jurídica ou técnica.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para uma recomendação automatizada. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: definir revisão humana e critérios de contestação. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                3. Proteja pessoas e processos
                                Evite conteúdo discriminatório, manipulação, exposição de informações e decisões automatizadas sem supervisão adequada. Estabeleça revisão humana proporcional ao risco.
                
                Erro frequente: a adoção automática de decisões de alto impacto. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                4. Exemplo no HestIA
                                Ao recomendar capacitações, avalie se o histórico de participação está completo e se os critérios não prejudicam colaboradores com menor acesso às atividades. Ofereça revisão de recomendações contestadas.
                
                Exercício guiado: use uma recomendação automatizada como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar os impactos sobre pessoas e processos e propor definir revisão humana e critérios de contestação. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                5. Checklist responsável
                                Verifique finalidade legítima, ferramenta autorizada, dados adequados, precisão, justiça, transparência e responsabilidade pelo resultado. Documente limites e corrija problemas identificados.
                
                Checklist de aplicação: confirme a finalidade legítima; revise os impactos sobre pessoas e processos; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize definir revisão humana e critérios de contestação. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.
                """,
                TemaTreinamento.BOAS_PRATICAS_DE_USO,
                18,
                true
        );

        Microtreinamento curso5 = new Microtreinamento(
                5L,
                "Validação Humana de Respostas de IA",
                "Método de conferência de fatos, cálculos e recomendações antes de decisões profissionais.",
                """
                1. Por que revisar
                                Sistemas generativos podem apresentar números incorretos, referências inexistentes e conclusões não sustentadas. Confiança na escrita não substitui evidência.
                
                Exemplo detalhado: considere um indicador de produtividade. Antes de executar qualquer ação, identifique a fonte, o período e o cálculo e verifique quem é responsável pela decisão. A equipe deve conferir o número em registros oficiais; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                2. Verificação em camadas
                                Confira nomes, datas, fórmulas e valores em fontes oficiais ou documentos autorizados. Diferencie informação confirmada, estimativa e hipótese; marque pontos ainda não verificados.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para um indicador de produtividade. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: conferir o número em registros oficiais. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                3. Revisão proporcional ao risco
                                Para um rascunho informal, uma checagem simples pode bastar. Para decisões financeiras, jurídicas, de saúde ou de pessoas, use validação especializada e critérios definidos pela organização.
                
                Erro frequente: a publicação de números gerados sem validação. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                4. Situação prática
                                Uma IA afirma que um treinamento elevou a produtividade em 35%. O analista consulta os dados originais, revisa a fórmula e verifica o período e a amostra antes de apresentar o resultado.
                
                Exercício guiado: use um indicador de produtividade como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar a fonte, o período e o cálculo e propor conferir o número em registros oficiais. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                5. Registro e correção
                                Anote fonte, data, revisor e alterações relevantes. Se houver erro, corrija a saída, informe quem recebeu a versão anterior e reavalie o processo de revisão.
                
                Checklist de aplicação: confirme a finalidade legítima; revise a fonte, o período e o cálculo; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize conferir o número em registros oficiais. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.
                """,
                TemaTreinamento.VALIDACAO_HUMANA,
                20,
                true
        );

        Microtreinamento curso6 = new Microtreinamento(
                6L,
                "Registro e Auditoria do Uso de IA",
                "Rastreabilidade das interações com IA e documentação de evidências para governança.",
                """
                1. Por que registrar
                                O registro permite compreender onde a IA foi usada, identificar riscos, investigar falhas e melhorar controles. Ele não é apenas burocracia: sustenta responsabilidade e aprendizado.
                
                Exemplo detalhado: considere um resumo corporativo produzido por IA. Antes de executar qualquer ação, identifique a rastreabilidade da ferramenta e das fontes e verifique quem é responsável pela decisão. A equipe deve registrar finalidade, revisão e responsável; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                2. Campos essenciais
                                Conforme a política interna, registre data, ferramenta, finalidade, responsável, categoria de dados, resultado e validação humana. Evite guardar prompts com dados sigilosos sem necessidade.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para um resumo corporativo produzido por IA. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: registrar finalidade, revisão e responsável. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                3. Rastreabilidade sem exposição
                                Não copie senhas, dados pessoais ou conteúdo confidencial para justificar uma atividade. Use referências internas autorizadas, versões e identificadores apropriados.
                
                Erro frequente: o armazenamento de senhas ou segredos nos logs. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                4. Exemplo prático
                                Uma equipe usa IA para resumir um relatório público. O registro informa a ferramenta, o documento de origem, a finalidade e a pessoa que conferiu o resumo antes da publicação.
                
                Exercício guiado: use um resumo corporativo produzido por IA como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar a rastreabilidade da ferramenta e das fontes e propor registrar finalidade, revisão e responsável. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                5. Preparação para auditoria
                                Mantenha registros consistentes, completos e acessíveis apenas a quem precisa. Corrija informações incorretas de forma rastreável e siga prazos de retenção definidos pela organização.
                
                Checklist de aplicação: confirme a finalidade legítima; revise a rastreabilidade da ferramenta e das fontes; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize registrar finalidade, revisão e responsável. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.
                """,
                TemaTreinamento.REGISTRO_E_AUDITORIA,
                18,
                true
        );

        Microtreinamento curso7 = new Microtreinamento(
                7L,
                "LGPD Aplicada ao Uso de Inteligência Artificial",
                "Princípios da LGPD e cuidados práticos com dados pessoais em soluções de IA.",
                """
                1. O que a LGPD protege
                                Dados pessoais se relacionam a pessoa identificada ou identificável; dados sensíveis incluem, por exemplo, informações de saúde e biometria. O uso de IA não elimina as obrigações de proteção.
                
                Exemplo detalhado: considere um projeto que reutiliza dados pessoais. Antes de executar qualquer ação, identifique a finalidade e a base legal do tratamento e verifique quem é responsável pela decisão. A equipe deve validar minimização e orientação de privacidade; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                2. Princípios na prática
                                Finalidade, adequação, necessidade, transparência, segurança e prevenção orientam o tratamento. Antes de enviar dados, pergunte por que são necessários, qual a base legal e quem poderá acessá-los.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para um projeto que reutiliza dados pessoais. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: validar minimização e orientação de privacidade. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                3. Minimização e anonimização
                                Prefira estatísticas agregadas ou dados sintéticos. Retirar somente o nome pode ser insuficiente se outras informações permitirem reidentificação. A anonimização efetiva depende do contexto e dos meios razoavelmente disponíveis.
                
                Erro frequente: o reaproveitamento indiscriminado de dados. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                4. Exemplo corporativo
                                Para estudar dúvidas frequentes de clientes, produza categorias agregadas em ambiente autorizado em vez de enviar conversas completas com identificadores para uma ferramenta externa.
                
                Exercício guiado: use um projeto que reutiliza dados pessoais como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar a finalidade e a base legal do tratamento e propor validar minimização e orientação de privacidade. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                5. Incidentes e direitos
                                Respeite os processos internos para solicitações dos titulares e incidentes. Em caso de exposição, comunique rapidamente o encarregado ou canal definido, preservando evidências sem ampliar o vazamento.
                
                Checklist de aplicação: confirme a finalidade legítima; revise a finalidade e a base legal do tratamento; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize validar minimização e orientação de privacidade. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.
                """,
                TemaTreinamento.LGPD,
                22,
                true
        );

        Microtreinamento curso8 = new Microtreinamento(
                8L,
                "Dados sensíveis em prompts",
                "Identificar dados pessoais e empresariais antes de enviar informações a ferramentas de IA.",
                """
                O que são dados sensíveis e confidenciais
                                Dados pessoais identificam indivíduos; dados sensíveis exigem proteção reforçada. Segredos comerciais e credenciais também não devem ser enviados a ferramentas sem autorização. No HestIA, a classificação do dado precede o uso da IA.
                
                Exemplo detalhado: considere um prompt com informações de saúde. Antes de executar qualquer ação, identifique a possibilidade de identificar uma pessoa e verifique quem é responsável pela decisão. A equipe deve remover dados reais e usar cenário fictício; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                Exemplo: um analista pretende colar uma planilha de clientes com CPF e histórico de atendimento em um chatbot público. A ação adequada é interromper o envio e usar apenas dados autorizados, minimizados ou sintéticos.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para um prompt com informações de saúde. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: remover dados reais e usar cenário fictício. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                Minimização e anonimização
                                Minimizar significa compartilhar somente os campos necessários. Remover nomes nem sempre anonimiza: combinações de cidade, idade e cargo podem reidentificar pessoas. Avalie o contexto e a política da empresa.
                
                Erro frequente: o compartilhamento de dados sensíveis sem autorização. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                Prática: transforme um relato de cliente em exemplo fictício sem nome, telefone, CPF, endereço ou identificadores indiretos.
                
                Exercício guiado: use um prompt com informações de saúde como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar a possibilidade de identificar uma pessoa e propor remover dados reais e usar cenário fictício. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                Checklist antes de enviar
                                Verifique a classificação da informação, a ferramenta aprovada, a finalidade legítima e a necessidade de cada campo. Se houver dúvida, consulte a área responsável.
                
                Checklist de aplicação: confirme a finalidade legítima; revise a possibilidade de identificar uma pessoa; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize remover dados reais e usar cenário fictício. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.

                Desafio: reescreva um prompt para pedir uma análise estatística sem anexar dados identificáveis.
                
                Exemplo detalhado: considere um prompt com informações de saúde. Antes de executar qualquer ação, identifique a possibilidade de identificar uma pessoa e verifique quem é responsável pela decisão. A equipe deve remover dados reais e usar cenário fictício; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.
                """,
                TemaTreinamento.PROTECAO_DE_DADOS,
                20,
                true
        );

        Microtreinamento curso9 = new Microtreinamento(
                9L,
                "Alucinações e verificação de respostas",
                "Reconhecer respostas incorretas e validar informações antes de utilizá-las.",
                """
                Por que a IA pode inventar
                                Modelos geram respostas plausíveis, mas podem produzir fatos, números, referências e links inexistentes. Fluência não é garantia de veracidade.
                
                Exemplo detalhado: considere uma referência normativa sugerida pela IA. Antes de executar qualquer ação, identifique a existência da fonte citada e verifique quem é responsável pela decisão. A equipe deve abrir o documento oficial e comparar a afirmação; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                Exemplo: a IA apresenta uma norma interna com número de versão que não existe. Verifique no repositório oficial antes de citar.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para uma referência normativa sugerida pela IA. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: abrir o documento oficial e comparar a afirmação. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                Triangulação de fontes
                                Compare afirmações com documentos internos autorizados, fontes oficiais e registros verificáveis. Diferencie hipótese de evidência e registre incertezas.
                
                Erro frequente: a citação de links inventados. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                Prática: destaque três afirmações verificáveis em uma resposta e identifique a fonte adequada para cada uma.
                
                Exercício guiado: use uma referência normativa sugerida pela IA como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar a existência da fonte citada e propor abrir o documento oficial e comparar a afirmação. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                Revisão humana
                                Decisões de impacto precisam de revisão de alguém responsável pelo processo. Documente o que foi validado, por quem e quando.
                
                Checklist de aplicação: confirme a finalidade legítima; revise a existência da fonte citada; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize abrir o documento oficial e comparar a afirmação. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.

                Desafio: redija uma resposta que indique claramente quais dados ainda não foram confirmados.
                
                Exemplo detalhado: considere uma referência normativa sugerida pela IA. Antes de executar qualquer ação, identifique a existência da fonte citada e verifique quem é responsável pela decisão. A equipe deve abrir o documento oficial e comparar a afirmação; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.
                """,
                TemaTreinamento.VALIDACAO_HUMANA,
                25,
                true
        );

        Microtreinamento curso10 = new Microtreinamento(
                10L,
                "Engenharia de prompts responsável",
                "Criar instruções claras com limites, contexto e critérios de verificação.",
                """
                Estrutura de um bom prompt
                                Descreva objetivo, público, contexto permitido, formato de saída e critérios de qualidade. Evite inserir informações sigilosas desnecessárias.
                
                Exemplo detalhado: considere um pedido para criar um relatório. Antes de executar qualquer ação, identifique o objetivo, o público e o formato esperado e verifique quem é responsável pela decisão. A equipe deve especificar critérios de qualidade e limites; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                Exemplo: peça um resumo de política pública fornecida, com tópicos e indicação de trechos incertos.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para um pedido para criar um relatório. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: especificar critérios de qualidade e limites. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                Restrições e fontes
                                Instrua o modelo a não inventar dados e a sinalizar lacunas. Peça referências apenas quando houver fontes acessíveis e autorizadas.
                
                Erro frequente: a inclusão desnecessária de informações confidenciais. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                Prática: compare um prompt genérico com outro que explicite escopo, formato e limitações.
                
                Exercício guiado: use um pedido para criar um relatório como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar o objetivo, o público e o formato esperado e propor especificar critérios de qualidade e limites. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                Teste e melhoria
                                Avalie respostas com casos normais, ambíguos e adversos. Ajuste o prompt sem usar dados reais de clientes no teste.
                
                Checklist de aplicação: confirme a finalidade legítima; revise o objetivo, o público e o formato esperado; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize especificar critérios de qualidade e limites. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.

                Desafio: crie três critérios para aceitar ou rejeitar uma resposta.
                
                Exemplo detalhado: considere um pedido para criar um relatório. Antes de executar qualquer ação, identifique o objetivo, o público e o formato esperado e verifique quem é responsável pela decisão. A equipe deve especificar critérios de qualidade e limites; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.
                """,
                TemaTreinamento.BOAS_PRATICAS_DE_USO,
                15,
                true
        );

        Microtreinamento curso11 = new Microtreinamento(
                11L,
                "Prevenção de vazamento de informações",
                "Evitar exposição de segredos, credenciais e documentos corporativos.",
                """
                Principais vetores
                                Uploads, prompts, extensões e compartilhamentos podem expor informações. Nunca envie senhas, tokens, chaves de API ou dados restritos a ferramentas não aprovadas.
                
                Exemplo detalhado: considere uma chave de API exposta em um prompt. Antes de executar qualquer ação, identifique o alcance da exposição e dos acessos e verifique quem é responsável pela decisão. A equipe deve revogar o segredo e comunicar o incidente; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                Exemplo: um colaborador cola uma chave de acesso em um assistente para depurar código. Deve revogar a chave e comunicar o incidente.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para uma chave de API exposta em um prompt. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: revogar o segredo e comunicar o incidente. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                Ferramentas e permissões
                                Use somente serviços autorizados e conceda o menor acesso necessário. Revise permissões de plugins e conectores antes de habilitá-los.
                
                Erro frequente: a tentativa de ocultar a exposição. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                Prática: identifique quais campos de um log podem conter segredos.
                
                Exercício guiado: use uma chave de API exposta em um prompt como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar o alcance da exposição e dos acessos e propor revogar o segredo e comunicar o incidente. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                Resposta a incidentes
                                Interrompa o compartilhamento, preserve evidências relevantes e siga o canal interno de segurança. Não tente ocultar o ocorrido.
                
                Checklist de aplicação: confirme a finalidade legítima; revise o alcance da exposição e dos acessos; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize revogar o segredo e comunicar o incidente. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.

                Desafio: escreva um registro objetivo do incidente sem repetir o segredo vazado.
                
                Exemplo detalhado: considere uma chave de API exposta em um prompt. Antes de executar qualquer ação, identifique o alcance da exposição e dos acessos e verifique quem é responsável pela decisão. A equipe deve revogar o segredo e comunicar o incidente; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.
                """,
                TemaTreinamento.PROTECAO_DE_DADOS,
                20,
                true
        );

        Microtreinamento curso12 = new Microtreinamento(
                12L,
                "Viés algorítmico e decisões justas",
                "Avaliar riscos de discriminação e desigualdade em decisões apoiadas por IA.",
                """
                Como surge o viés
                                Dados históricos, amostras desequilibradas e proxies podem produzir resultados injustos. A ausência de atributos sensíveis não garante neutralidade.
                
                Exemplo detalhado: considere um recomendador de cursos. Antes de executar qualquer ação, identifique a cobertura e o efeito sobre diferentes grupos e verifique quem é responsável pela decisão. A equipe deve investigar métricas e permitir revisão humana; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                Exemplo: um sistema recomenda treinamentos com base em histórico incompleto e exclui parte dos colaboradores.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para um recomendador de cursos. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: investigar métricas e permitir revisão humana. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                Avaliação por grupos
                                Compare cobertura, erros e oportunidades entre grupos relevantes, com cuidado à privacidade e às políticas aplicáveis. Analise também explicações qualitativas.
                
                Erro frequente: a suposição de neutralidade automática do algoritmo. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                Prática: identifique duas hipóteses para uma taxa de recomendação desigual.
                
                Exercício guiado: use um recomendador de cursos como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar a cobertura e o efeito sobre diferentes grupos e propor investigar métricas e permitir revisão humana. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                Mitigação e revisão
                                Ajuste dados e critérios, documente limitações e permita contestação humana. Monitore resultados depois da implantação.
                
                Checklist de aplicação: confirme a finalidade legítima; revise a cobertura e o efeito sobre diferentes grupos; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize investigar métricas e permitir revisão humana. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.

                Desafio: proponha um procedimento de revisão de recomendações contestadas.
                
                Exemplo detalhado: considere um recomendador de cursos. Antes de executar qualquer ação, identifique a cobertura e o efeito sobre diferentes grupos e verifique quem é responsável pela decisão. A equipe deve investigar métricas e permitir revisão humana; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.
                """,
                TemaTreinamento.VALIDACAO_HUMANA,
                25,
                true
        );

        Microtreinamento curso13 = new Microtreinamento(
                13L,
                "Resposta a incidentes de IA",
                "Registrar, classificar e encaminhar incidentes ligados ao uso de IA.",
                """
                Detecção e classificação
                                Incidentes podem envolver vazamento, resposta enganosa, uso indevido ou falha de controle. Registre impacto, alcance, sistemas e horário aproximado.
                
                Exemplo detalhado: considere um vazamento em resposta de IA. Antes de executar qualquer ação, identifique a gravidade, o alcance e as evidências e verifique quem é responsável pela decisão. A equipe deve conter a exposição e acionar o processo de incidentes; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                Exemplo: uma recomendação automatizada contém informações restritas.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para um vazamento em resposta de IA. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: conter a exposição e acionar o processo de incidentes. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                Contenção e comunicação
                                Interrompa o fluxo afetado quando autorizado, avise os responsáveis e preserve evidências sem ampliar a exposição. Siga o plano interno de resposta.
                
                Erro frequente: a divulgação do conteúdo restrito. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                Prática: organize um registro com fato, impacto, evidência e responsável.
                
                Exercício guiado: use um vazamento em resposta de IA como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar a gravidade, o alcance e as evidências e propor conter a exposição e acionar o processo de incidentes. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                Aprendizado e prevenção
                                Após a contenção, identifique causas, ações corretivas, donos e prazos. Valide se a correção funciona e monitore recorrência.
                
                Checklist de aplicação: confirme a finalidade legítima; revise a gravidade, o alcance e as evidências; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize conter a exposição e acionar o processo de incidentes. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.

                Desafio: proponha duas ações preventivas mensuráveis.
                
                Exemplo detalhado: considere um vazamento em resposta de IA. Antes de executar qualquer ação, identifique a gravidade, o alcance e as evidências e verifique quem é responsável pela decisão. A equipe deve conter a exposição e acionar o processo de incidentes; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.
                """,
                TemaTreinamento.POLITICAS_INTERNAS,
                15,
                true
        );

        Microtreinamento curso14 = new Microtreinamento(
                14L,
                "Rastreabilidade e evidências de auditoria",
                "Documentar decisões, fontes e controles de sistemas com IA.",
                """
                O que registrar
                                Guarde identificador do processo, versão da ferramenta, data, objetivo, entradas permitidas, resultado e revisão humana conforme a política interna.
                
                Exemplo detalhado: considere uma decisão baseada em IA. Antes de executar qualquer ação, identifique a origem, a versão e a revisão das evidências e verifique quem é responsável pela decisão. A equipe deve registrar fontes e critérios de decisão; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                Exemplo: uma recomendação de treinamento precisa ter origem e critério identificáveis.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para uma decisão baseada em IA. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: registrar fontes e critérios de decisão. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                Evidência de qualidade
                                Registros devem ser íntegros, acessíveis a responsáveis autorizados e suficientes para reconstruir uma decisão, sem armazenar dados excessivos.
                
                Erro frequente: a criação de justificativas retroativas fictícias. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                Prática: desenhe uma ficha de evidência com campos mínimos.
                
                Exercício guiado: use uma decisão baseada em IA como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar a origem, a versão e a revisão das evidências e propor registrar fontes e critérios de decisão. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                Preparação para auditoria
                                Organize responsáveis, políticas aplicáveis, histórico de mudanças e tratamento de desvios. Diferencie controles planejados de controles efetivamente executados.
                
                Checklist de aplicação: confirme a finalidade legítima; revise a origem, a versão e a revisão das evidências; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize registrar fontes e critérios de decisão. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.

                Desafio: descreva como provar que uma recomendação foi revisada.
                
                Exemplo detalhado: considere uma decisão baseada em IA. Antes de executar qualquer ação, identifique a origem, a versão e a revisão das evidências e verifique quem é responsável pela decisão. A equipe deve registrar fontes e critérios de decisão; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.
                """,
                TemaTreinamento.REGISTRO_E_AUDITORIA,
                20,
                true
        );

        Microtreinamento curso15 = new Microtreinamento(
                15L,
                "Avaliação de riscos de ferramentas de IA",
                "Priorizar riscos antes da adoção de uma nova ferramenta.",
                """
                Mapeamento do uso
                                Defina finalidade, usuários, dados tratados, integrações e impacto de erros. Identifique responsáveis e dependências.
                
                Exemplo detalhado: considere a adoção de uma nova ferramenta. Antes de executar qualquer ação, identifique a probabilidade e o impacto dos riscos e verifique quem é responsável pela decisão. A equipe deve designar responsáveis e controles mensuráveis; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                Exemplo: um plugin solicita acesso a arquivos corporativos sem justificativa clara.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para a adoção de uma nova ferramenta. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: designar responsáveis e controles mensuráveis. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                Probabilidade e impacto
                                Classifique riscos como exposição de dados, erro decisório, indisponibilidade e viés. Use critérios consistentes e explicite incertezas.
                
                Erro frequente: a aprovação sem avaliação de risco. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                Prática: crie uma matriz simples para três riscos.
                
                Exercício guiado: use a adoção de uma nova ferramenta como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar a probabilidade e o impacto dos riscos e propor designar responsáveis e controles mensuráveis. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                Tratamento e aprovação
                                Escolha mitigar, evitar, transferir ou aceitar formalmente conforme as regras internas. Registre controles, dono e reavaliação.
                
                Checklist de aplicação: confirme a finalidade legítima; revise a probabilidade e o impacto dos riscos; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize designar responsáveis e controles mensuráveis. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.

                Desafio: justifique a decisão de aprovar ou não uma ferramenta.
                
                Exemplo detalhado: considere a adoção de uma nova ferramenta. Antes de executar qualquer ação, identifique a probabilidade e o impacto dos riscos e verifique quem é responsável pela decisão. A equipe deve designar responsáveis e controles mensuráveis; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.
                """,
                TemaTreinamento.USO_DE_FERRAMENTAS_AUTORIZADAS,
                25,
                true
        );

        Microtreinamento curso16 = new Microtreinamento(
                16L,
                "Liderança em IA responsável",
                "Promover cultura de aprendizagem, transparência e responsabilidade.",
                """
                Exemplo da liderança
                                Líderes devem demonstrar uso autorizado, revisão crítica e transparência sobre limites da IA. Incentive perguntas e reporte de problemas.
                
                Exemplo detalhado: considere uma equipe que automatiza atendimento. Antes de executar qualquer ação, identifique os controles, o impacto e o reporte de falhas e verifique quem é responsável pela decisão. A equipe deve definir supervisão e indicadores de qualidade; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.

                Exemplo: uma equipe quer automatizar respostas a clientes sem revisão.
                
                Como aplicar no trabalho: imagine que um colega solicite uma solução imediata para uma equipe que automatiza atendimento. Primeiro, confirme a finalidade e quais dados são realmente necessários. Em seguida, documente a decisão e execute o controle recomendado: definir supervisão e indicadores de qualidade. Um registro curto com data, responsável, ferramenta e resultado ajuda a demonstrar que o processo foi seguido sem expor informações desnecessárias.

                Mentoria e aprendizagem
                                Crie espaços para compartilhar casos reais anonimizados, boas práticas e lições aprendidas. Reconheça comportamentos responsáveis, não apenas velocidade.
                
                Erro frequente: a punição do relato de problemas. Esse comportamento pode gerar riscos que não são evidentes na resposta inicial da IA. Para prevenir o problema, faça uma revisão antes de compartilhar ou publicar, compare o resultado com fontes autorizadas e registre o que foi conferido. Se a revisão identificar uma inconsistência, corrija a informação e reavalie o processo antes de prosseguir.

                Prática: planeje uma conversa de 15 minutos sobre um caso de uso.
                
                Exercício guiado: use uma equipe que automatiza atendimento como caso fictício e escreva três decisões: o que deve ser verificado, qual ação será executada e como demonstrar que o procedimento foi cumprido. Uma resposta adequada deve mencionar os controles, o impacto e o reporte de falhas e propor definir supervisão e indicadores de qualidade. Compare as decisões com a política da organização; se faltar evidência, identifique exatamente o que precisa ser obtido.

                Indicadores e melhoria contínua
                                Acompanhe adoção responsável, incidentes, qualidade das revisões e participação em treinamentos. Ajuste práticas com base em evidências.
                
                Checklist de aplicação: confirme a finalidade legítima; revise os controles, o impacto e o reporte de falhas; verifique a ferramenta e as permissões; execute o controle definido; registre responsável, data e evidência mínima; e determine quando reavaliar. Na dúvida, priorize definir supervisão e indicadores de qualidade. A conclusão desta aula indica estudo do conteúdo, mas a aprovação no curso depende também da avaliação objetiva e das regras de certificação da empresa.

                Desafio: proponha uma ação trimestral e um indicador de resultado.
                
                Exemplo detalhado: considere uma equipe que automatiza atendimento. Antes de executar qualquer ação, identifique os controles, o impacto e o reporte de falhas e verifique quem é responsável pela decisão. A equipe deve definir supervisão e indicadores de qualidade; isso permite comparar a prática com a política interna e explicar a escolha em uma auditoria. Se houver dúvida sobre a autorização, interrompa a etapa e consulte o responsável, em vez de presumir que a ferramenta é segura.
                """,
                TemaTreinamento.POLITICAS_INTERNAS,
                15,
                true
        );

        return List.of(
                curso1,
                curso2,
                curso3,
                curso4,
                curso5,
                curso6,
                curso7,
                curso8,
                curso9,
                curso10,
                curso11,
                curso12,
                curso13,
                curso14,
                curso15,
                curso16
        );
    }
}