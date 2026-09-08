package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.model.Microtreinamento;
import br.com.hestia.microtreinamento.model.TemaTreinamento;

import java.util.List;

public class MicrotreinamentoFicticioService {

    public List<Microtreinamento> criarTreinamentos() {

        Microtreinamento protecaoDados = new Microtreinamento(
                1L,
                "Proteção de Dados em IA",
                "Boas práticas para evitar o uso indevido de dados pessoais e sensíveis em ferramentas de IA.",
                """
                1. Não insira senhas, documentos pessoais ou dados bancários em ferramentas de IA.

                2. Evite compartilhar informações confidenciais de clientes, funcionários ou parceiros.

                3. Antes de utilizar qualquer informação, avalie se o dado é realmente necessário.

                4. Sempre que possível, anonimize os dados.

                5. Em caso de dúvida, consulte as políticas internas da empresa.
                """,
                TemaTreinamento.PROTECAO_DE_DADOS,
                5,
                true
        );

        Microtreinamento ferramentasAutorizadas = new Microtreinamento(
                2L,
                "Uso de Ferramentas de IA Autorizadas",
                "Como identificar e utilizar somente ferramentas de IA aprovadas pela empresa.",
                """
                1. Utilize apenas ferramentas de IA autorizadas pela organização.

                2. Não crie contas corporativas em serviços não aprovados.

                3. Verifique se a ferramenta atende aos requisitos de segurança da empresa.

                4. Evite utilizar contas pessoais para atividades profissionais.

                5. Consulte a lista interna de ferramentas permitidas antes de iniciar o uso.
                """,
                TemaTreinamento.USO_DE_FERRAMENTAS_AUTORIZADAS,
                4,
                true
        );

        Microtreinamento politicasInternas = new Microtreinamento(
                3L,
                "Políticas Internas para Uso de IA",
                "Orientações para utilizar inteligência artificial de acordo com as regras internas da empresa.",
                """
                1. Conheça as políticas internas relacionadas ao uso de inteligência artificial.

                2. Respeite as restrições definidas para cada área ou departamento.

                3. Não utilize IA em atividades que tenham sido expressamente proibidas.

                4. Mantenha os registros exigidos pela empresa.

                5. Em caso de dúvida, procure o responsável pela governança de IA.
                """,
                TemaTreinamento.POLITICAS_INTERNAS,
                5,
                true
        );

        Microtreinamento boasPraticas = new Microtreinamento(
                4L,
                "Uso Responsável da Inteligência Artificial",
                "Boas práticas para utilizar IA de forma adequada às atividades profissionais.",
                """
                1. Utilize IA apenas para finalidades relacionadas às suas atividades profissionais.

                2. Não utilize ferramentas corporativas para gerar conteúdos inadequados ou não autorizados.

                3. Avalie se a IA é realmente necessária para a atividade.

                4. Evite depender exclusivamente da IA para decisões importantes.

                5. Sempre considere os riscos envolvidos antes de utilizar a ferramenta.
                """,
                TemaTreinamento.BOAS_PRATICAS_DE_USO,
                5,
                true
        );

        Microtreinamento validacaoHumana = new Microtreinamento(
                5L,
                "Validação Humana de Respostas de IA",
                "Como revisar e validar conteúdos produzidos por ferramentas de inteligência artificial.",
                """
                1. Nunca considere automaticamente correta uma resposta produzida por IA.

                2. Confira informações importantes em fontes confiáveis.

                3. Revise cálculos, nomes, datas e informações técnicas.

                4. A responsabilidade final permanece com o usuário.

                5. Para decisões críticas, solicite revisão de outra pessoa quando necessário.
                """,
                TemaTreinamento.VALIDACAO_HUMANA,
                4,
                true
        );

        Microtreinamento registroAuditoria = new Microtreinamento(
                6L,
                "Registro e Auditoria do Uso de IA",
                "Importância de registrar corretamente o uso de inteligência artificial no ambiente corporativo.",
                """
                1. Registre os usos de IA exigidos pelas políticas da empresa.

                2. Informe a ferramenta utilizada e a finalidade do uso.

                3. Não omita atividades realizadas com auxílio de inteligência artificial.

                4. O registro permite auditoria, controle e melhoria das práticas internas.

                5. Informações corretas ajudam a organização a identificar riscos e oportunidades.
                """,
                TemaTreinamento.REGISTRO_E_AUDITORIA,
                4,
                true
        );

        Microtreinamento lgpd = new Microtreinamento(
                7L,
                "LGPD Aplicada ao Uso de Inteligência Artificial",
                "Conceitos básicos da LGPD aplicados ao uso corporativo de ferramentas de IA.",
                """
                1. Dados pessoais são informações relacionadas a uma pessoa identificada ou identificável.

                2. Dados sensíveis exigem cuidados ainda maiores.

                3. Não compartilhe dados pessoais com ferramentas de IA sem autorização adequada.

                4. Utilize apenas os dados necessários para a finalidade pretendida.

                5. O tratamento de dados deve respeitar as regras da LGPD e as políticas da empresa.
                """,
                TemaTreinamento.LGPD,
                6,
                true
        );

        return List.of(
                protecaoDados,
                ferramentasAutorizadas,
                politicasInternas,
                boasPraticas,
                validacaoHumana,
                registroAuditoria,
                lgpd
        );
    }
}