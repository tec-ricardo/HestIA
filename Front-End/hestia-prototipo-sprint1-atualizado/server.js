const express = require("express");
const axios = require("axios");
const app = express();

const api = axios.create({
    baseURL: process.env.API_URL || "http://localhost:8080",
    timeout: 5000
});


app.set("view engine", "ejs");

app.use(express.static("public"));

app.use(express.urlencoded({
    extended: true
}));


/* USUARIO */

let email = "";
let perfil = "";
const empresaAtualId = Number(process.env.HESTIA_EMPRESA_ID || 1);


/* LOGIN */

app.get("/acesso", (req, res) => {

    res.render("login", {
        erro: ""
    });

});


app.post("/acesso", (req, res) => {

    email = req.body.email;

    const senha = req.body.senha;


    if (senha == "") {

        return res.render("login", {
            erro: "Informe a senha."
        });

    }


    if (email.endsWith("@adm")) {

        perfil = "administrador";

    }

    else if (email.endsWith("@gestor")) {

        perfil = "gestor";

    }

    else if (email.endsWith("@colaborador")) {

        perfil = "colaborador";

    }

    else {

        return res.render("login", {
            erro: "Perfil não identificado."
        });

    }


    res.redirect("/");

});


/* VISAO GERAL */

app.get("/", async (req, res) => {

    if (perfil == "") {

        return res.redirect("/acesso");

    }


    const indicadores =
        await carregarIndicadores();


    const indicadoresMaturidade =
        await consultarObjetoComFallback(
            "/indicadores/maturidade",
            obterIndicadoresMaturidade()
        );


    const maturityScore =
        calcularMaturityScore(
            indicadoresMaturidade
        );


    const resumoPessoal = {

        maturityScore:
            maturityScore.toFixed(2),

        aiCredits: 680,

        ranking: "4º",

        usoResponsavel:
        indicadoresMaturidade.usoResponsavel

    };


    /*
        SCRUM-198 / SCRUM-200

        RESUMO PESSOAL DO COLABORADOR

        Esta área complementa a Visão Geral
        apresentando informações relacionadas
        à jornada individual do colaborador.

        Os dados abaixo são demonstrativos.

        Quando o backend disponibilizar
        o endpoint correspondente, os dados
        poderão ser carregados através do Axios.

        O endpoint /resumo-pessoal deverá ser
        confirmado com a equipe do backend.
    */

    const detalhesResumoPessoalMock = {

        progresso: {

            nivel: "Consciente",

            nivelNumero: 2,

            xpAtual: 235,

            xpProximoNivel: 300,

            xpFaltante: 65,

            proximoNivel: "Proficiente"

        },


        destaques: {

            sequenciaConforme: 7,

            utilizacoesConformes: 20,

            utilizacoesAvaliadas: 23

        },


        atividadeRecente: [

            {

                titulo:
                    "Resumo de documento",

                modelo:
                    "ChatGPT",

                status:
                    "Conforme"

            },

            {

                titulo:
                    "Apoio na elaboração de relatório",

                modelo:
                    "Copilot",

                status:
                    "Conforme"

            },

            {

                titulo:
                    "Análise de dados",

                modelo:
                    "ChatGPT",

                status:
                    "Não conforme"

            }

        ],


        conquistasRecentes: [

            {

                descricao:
                    "Utilização conforme",

                recompensa:
                    "+10 XP",

                data:
                    "Hoje"

            },

            {

                descricao:
                    "Utilização conforme",

                recompensa:
                    "+10 XP",

                data:
                    "Ontem"

            },

            {

                descricao:
                    "Reutilização eficiente",

                recompensa:
                    "XP conforme regra",

                data:
                    "20/09"

            }

        ]

    };


    /*
        AXIOS DO RESUMO PESSOAL

        Se o backend responder,
        utiliza os dados reais.

        Se o backend ainda estiver
        indisponível, utiliza os dados
        demonstrativos acima.
    */

    const detalhesResumoPessoal =
        perfil == "colaborador"
            ?
            await consultarObjetoComFallback(
                "/resumo-pessoal",
                detalhesResumoPessoalMock
            )
            :
            detalhesResumoPessoalMock;


    res.render("index", {

        perfil: perfil,

        email: email,

        active: "dashboard",

        indicadores: indicadores,

        resumoPessoal: resumoPessoal,

        detalhesResumoPessoal:
        detalhesResumoPessoal

    });

});



/* MATURIDADE */

function obterIndicadoresMaturidade() {

    return {

        aiLiteracy: 72,

        governanca: 65,

        usoResponsavel: 80,

        produtividade: 76,

        finOps: 58,

        seguranca: 85,

        colaboracao: 70,

        desenvolvimentoCompetencias: 68

    };

}


function calcularMaturityScore(indicadores) {

    return (

        indicadores.aiLiteracy * 0.15 +

        indicadores.governanca * 0.15 +

        indicadores.usoResponsavel * 0.15 +

        indicadores.produtividade * 0.10 +

        indicadores.finOps * 0.10 +

        indicadores.seguranca * 0.20 +

        indicadores.colaboracao * 0.05 +

        indicadores.desenvolvimentoCompetencias * 0.10

    );

}


function calcularPontuacaoRankingDepartamento(
    maturityScore,
    eficiencia,
    usoResponsavel
) {

    return (

        maturityScore * 0.50 +

        eficiencia * 0.30 +

        usoResponsavel * 0.20

    );

}


function obterIndicadoresDepartamentais() {

    return [

        {

            nome: "Tecnologia",

            indicadores: {

                aiLiteracy: 88,

                governanca: 78,

                usoResponsavel: 90,

                produtividade: 85,

                finOps: 76,

                seguranca: 91,

                colaboracao: 79,

                desenvolvimentoCompetencias: 80

            }

        },


        {

            nome: "Marketing",

            indicadores: {

                aiLiteracy: 80,

                governanca: 72,

                usoResponsavel: 83,

                produtividade: 79,

                finOps: 68,

                seguranca: 86,

                colaboracao: 73,

                desenvolvimentoCompetencias: 71

            }

        },


        {

            nome: "Financeiro",

            indicadores: {

                aiLiteracy: 70,

                governanca: 65,

                usoResponsavel: 75,

                produtividade: 68,

                finOps: 61,

                seguranca: 78,

                colaboracao: 65,

                desenvolvimentoCompetencias: 64

            }

        },


        {

            nome: "RH",

            indicadores: {

                aiLiteracy: 66,

                governanca: 59,

                usoResponsavel: 69,

                produtividade: 63,

                finOps: 55,

                seguranca: 73,

                colaboracao: 60,

                desenvolvimentoCompetencias: 58

            }

        }

    ];

}


app.get("/maturidade", async (req, res) => {

    if (perfil == "") {

        return res.redirect("/acesso");

    }


    const indicadores =
        await consultarObjetoComFallback(
            "/indicadores/maturidade",
            obterIndicadoresMaturidade()
        );


    const maturityScore =
        calcularMaturityScore(
            indicadores
        );


    const departamentosCalculados =
        obterIndicadoresDepartamentais()
            .map(function(departamento) {

                return {

                    nome:
                    departamento.nome,

                    indicadores:
                    departamento.indicadores,

                    score:
                        calcularMaturityScore(
                            departamento.indicadores
                        )

                };

            });


    const departamentosMaturidade =
        await consultarListaComFallback(
            "/indicadores/maturidade/departamentos",
            departamentosCalculados
        );


    const eficienciaDepartamentos =
        await consultarListaComFallback(
            "/registros-uso-ia/eficiencia",
            []
        );


    const rankingDepartamentos =
        departamentosMaturidade
            .map(function(departamento) {

                const eficienciaEncontrada =
                    eficienciaDepartamentos.find(
                        function(item) {

                            return item.departamento ==
                                departamento.nome;

                        }
                    );


                const eficiencia =
                    eficienciaEncontrada
                        ?
                        eficienciaEncontrada.eficiencia
                        :
                        0;


                const departamentoBase =
                    departamentosCalculados.find(
                        function(item) {

                            return item.nome ==
                                departamento.nome;

                        }
                    );


                const usoResponsavel =
                    departamento.indicadores
                        ?
                        departamento.indicadores.usoResponsavel
                        :
                        departamentoBase.indicadores.usoResponsavel;


                const pontuacao =
                    calcularPontuacaoRankingDepartamento(
                        departamento.score,
                        eficiencia,
                        usoResponsavel
                    );


                return {

                    nome:
                    departamento.nome,

                    maturityScore:
                    departamento.score,

                    eficiencia:
                    eficiencia,

                    usoResponsavel:
                    usoResponsavel,

                    pontuacao:
                        Math.round(
                            pontuacao * 100
                        ) / 100

                };

            })
            .sort(function(a, b) {

                return b.pontuacao -
                    a.pontuacao;

            })
            .map(function(departamento, index) {

                return {

                    posicao:
                        index + 1,

                    nome:
                    departamento.nome,

                    maturityScore:
                    departamento.maturityScore,

                    eficiencia:
                    departamento.eficiencia,

                    usoResponsavel:
                    departamento.usoResponsavel,

                    pontuacao:
                    departamento.pontuacao

                };

            });


    const mediaDepartamental =
        departamentosMaturidade.reduce(
            function(total, departamento) {

                return total +
                    departamento.score;

            },

            0

        ) / departamentosMaturidade.length;


    const maiorMaturidade =
        departamentosMaturidade.reduce(
            function(maior, departamento) {

                if (departamento.score > maior.score) {

                    return departamento;

                }

                return maior;

            }
        );


    const menorMaturidade =
        departamentosMaturidade.reduce(
            function(menor, departamento) {

                if (departamento.score < menor.score) {

                    return departamento;

                }

                return menor;

            }
        );


    res.render("maturidade", {

        perfil: perfil,

        email: email,

        active: "maturidade",

        indicadores: indicadores,

        maturityScore:
            maturityScore.toFixed(2),

        departamentosMaturidade:
        departamentosMaturidade,

        rankingDepartamentos:
        rankingDepartamentos,

        mediaDepartamental:
            mediaDepartamental.toFixed(2),

        maiorMaturidade:
        maiorMaturidade,

        menorMaturidade:
        menorMaturidade

    });

});


/* CREDITOS */

app.get("/creditos", (req, res) => {

    if (perfil == "") {

        return res.redirect("/acesso");

    }


    res.render("creditos", {

        perfil: perfil,

        email: email,

        active: "creditos"

    });

});


/* UTILIZACOES */

app.get("/utilizacoes", async (req, res) => {

    if (perfil != "colaborador") {

        return res.redirect("/");

    }


    /*
        DADOS MOCKADOS DO HISTORICO

        Estes dados representam o retorno
        que futuramente poderá vir do backend.

        Quando a API de utilizações estiver
        pronta, os dados reais serão utilizados
        através do Axios.
    */

    const historicoMock = [

        {

            id: 1,

            modelo: "ChatGPT",

            data: "22/09/2026",

            dataFiltro: "2026-09-22",

            hora: "14:32",

            consumo: 1250,

            finalidade: "Resumo de documento",

            conformidade: "Conforme",

            prompt:
                "Resuma os principais pontos deste documento para apoiar minha análise.",

            resposta:
                "A IA apresentou um resumo dos principais pontos e organizou as informações solicitadas.",

            justificativa:
                "Nenhuma violação de política foi identificada nesta utilização."

        },

        {

            id: 2,

            modelo: "Copilot",

            data: "21/09/2026",

            dataFiltro: "2026-09-21",

            hora: "10:18",

            consumo: 980,

            finalidade:
                "Apoio na elaboração de relatório",

            conformidade: "Conforme",

            prompt:
                "Ajude a organizar a estrutura deste relatório em tópicos.",

            resposta:
                "A IA sugeriu uma estrutura com introdução, desenvolvimento, resultados e conclusão.",

            justificativa:
                "A utilização está de acordo com as políticas de uso de IA."

        },

        {

            id: 3,

            modelo: "ChatGPT",

            data: "20/09/2026",

            dataFiltro: "2026-09-20",

            hora: "16:45",

            consumo: 1540,

            finalidade: "Análise de dados",

            conformidade: "Não conforme",

            prompt:
                "Analise os dados informados e apresente os principais padrões encontrados.",

            resposta:
                "A IA realizou a análise solicitada e apresentou os principais padrões identificados.",

            justificativa:
                "A utilização foi classificada como não conforme após a avaliação das regras aplicáveis."

        },

        {

            id: 4,

            modelo: "Copilot",

            data: "23/09/2026",

            dataFiltro: "2026-09-23",

            hora: "09:12",

            consumo: 760,

            finalidade:
                "Geração de apresentação",

            conformidade:
                "Aguardando avaliação",

            prompt:
                "Organize estes tópicos em uma estrutura para apresentação.",

            resposta:
                "A IA sugeriu a divisão do conteúdo em uma sequência de slides.",

            justificativa:
                "A utilização ainda está aguardando avaliação de conformidade."

        }

    ];


    const historico =
        await consultarListaComFallback(
            "/utilizacoes",
            historicoMock
        );


    const utilizacoesAvaliadas =
        historico.filter(
            function(utilizacao) {

                return utilizacao.conformidade !=
                    "Aguardando avaliação";

            }
        );


    const utilizacoesConformes =
        historico.filter(
            function(utilizacao) {

                return utilizacao.conformidade ==
                    "Conforme";

            }
        );


    const consumoTotal =
        historico.reduce(

            function(total, utilizacao) {

                return total +
                    utilizacao.consumo;

            },

            0

        );


    const taxaConformidade =
        utilizacoesAvaliadas.length > 0
            ?
            Math.round(
                (
                    utilizacoesConformes.length /
                    utilizacoesAvaliadas.length
                ) * 100
            )
            :
            0;


    const resumoHistorico = {

        totalUtilizacoes:
        historico.length,

        taxaConformidade:
        taxaConformidade,

        consumoTotal:
        consumoTotal

    };


    res.render("utilizacoes", {

        perfil: perfil,

        email: email,

        active: "utilizacoes",

        historico: historico,

        respostasReutilizaveis:
            await consultarReutilizacoes(empresaAtualId),

        resumoHistorico:
        resumoHistorico

    });

});

app.get("/reutilizacoes/buscar", async (req, res) => {
    const prompt = String(req.query.prompt || "").trim();
    if (!prompt) {
        return res.status(400).json({ erro: "Informe o prompt para buscar uma resposta anterior." });
    }

    try {
        const response = await api.get(
            `/api/respostas-reutilizaveis/empresa/${empresaAtualId}/buscar`,
            { params: { prompt } }
        );
        return res.json(response.data);
    } catch (error) {
        console.error(`Falha ao buscar respostas anteriores: ${mensagemDaApi(error)}`);
        return res.status(502).json({ erro: "Não foi possível consultar respostas anteriores." });
    }
});

app.post("/reutilizacoes/:id/reutilizar", async (req, res) => {
    try {
        const response = await api.patch(
            `/api/respostas-reutilizaveis/${encodeURIComponent(req.params.id)}/reutilizar`
        );
        return res.json(response.data);
    } catch (error) {
        console.error(`Falha ao registrar reutilização: ${mensagemDaApi(error)}`);
        return res.status(502).json({ erro: "Não foi possível registrar a reutilização." });
    }
});


/* COMPETENCIAS */

app.get("/competencias", (req, res) => {

    if (
        perfil != "colaborador" &&
        perfil != "administrador"
    ) {

        return res.redirect("/");

    }


    res.render("competencias", {

        perfil: perfil,

        email: email,

        active: "competencias"

    });

});


/* GOVERNANCA */

app.get("/governanca", async (req, res) => {

    if (perfil == "") {

        return res.redirect("/acesso");

    }


    const [politicas, empresas] =
        await Promise.all([

            consultar("/politicas"),

            consultar("/empresas")

        ]);


    res.render("governanca", {

        perfil: perfil,

        email: email,

        active: "governanca",

        politicas: politicas,

        empresas: empresas,

        mensagem:
            req.query.mensagem || "",

        erro:
            req.query.erro || ""

    });

});


/* CUSTOS */

app.get("/custos", async (req, res) => {

    if (perfil != "gestor") {

        return res.redirect("/");

    }


    /*
        DADOS MOCKADOS DE EFICIENCIA

        O Axios tentará utilizar os dados
        reais quando o backend disponibilizar
        o endpoint correspondente.
    */

    const dadosEficienciaMock = [

        {

            departamento:
                "Tecnologia",

            utilizacoes: 320,

            usoAdequado: 91,

            produtividade: 86,

            custoIA: 4200

        },

        {

            departamento:
                "Marketing",

            utilizacoes: 245,

            usoAdequado: 84,

            produtividade: 78,

            custoIA: 2800

        },

        {

            departamento:
                "Financeiro",

            utilizacoes: 180,

            usoAdequado: 79,

            produtividade: 72,

            custoIA: 1900

        },

        {

            departamento:
                "RH",

            utilizacoes: 120,

            usoAdequado: 82,

            produtividade: 69,

            custoIA: 1100

        }

    ];


    const dadosEficiencia =
        await consultarListaComFallback(
            "/indicadores/eficiencia",
            dadosEficienciaMock
        );


    res.render("custos", {

        perfil: perfil,

        email: email,

        active: "custos",

        dadosEficiencia:
        dadosEficiencia

    });

});


/* ADMINISTRACAO */

app.get(
    "/administracao",
    async (req, res) => {

        if (perfil != "administrador") {

            return res.redirect("/");

        }


        const [
            empresas,
            departamentos,
            usuarios,
            ferramentas
        ] =
            await Promise.all([

                consultar("/empresas"),

                consultar("/departamentos"),

                consultar("/usuarios"),

                consultar("/ferramentas-ia")

            ]);


        res.render("administracao", {

            perfil: perfil,

            email: email,

            active: "administracao",

            empresas: empresas,

            departamentos: departamentos,

            usuarios: usuarios,

            ferramentas: ferramentas,

            mensagem:
                req.query.mensagem || "",

            erro:
                req.query.erro || ""

        });

    }
);


/* CONFIGURACOES */

app.get(
    "/configuracoes",
    async (req, res) => {

        if (perfil != "administrador") {

            return res.redirect("/");

        }


        const ferramentas =
            await consultar(
                "/ferramentas-ia"
            );


        res.render("configuracoes", {

            perfil: perfil,

            email: email,

            active: "configuracoes",

            ferramentas:
            ferramentas

        });

    }
);


/* MAPA DE RISCO */

function calcularRiscoDepartamental(fatores) {

    if (
        fatores == null ||
        typeof fatores.conformidade != "number" ||
        typeof fatores.nivelRiscoFerramenta != "number" ||
        typeof fatores.tratamentoDadosPessoais != "number" ||
        typeof fatores.statusFerramenta != "number"
    ) {

        return null;

    }

    return (
        fatores.conformidade * 0.35 +
        fatores.nivelRiscoFerramenta * 0.30 +
        fatores.tratamentoDadosPessoais * 0.20 +
        fatores.statusFerramenta * 0.15
    );

}


function classificarNivelRisco(pontuacao) {

    if (
        pontuacao == null ||
        typeof pontuacao != "number"
    ) {

        return {
            nivel: "Não avaliado",
            classe: "not-evaluated",
            descricao:
                "Não existem dados suficientes para avaliar o risco."
        };

    }


    if (pontuacao <= 25) {

        return {
            nivel: "Baixo",
            classe: "low",
            descricao:
                "Uso de IA com menor nível de atenção."
        };

    }


    if (pontuacao <= 50) {

        return {
            nivel: "Médio",
            classe: "medium",
            descricao:
                "Uso de IA que requer acompanhamento."
        };

    }


    if (pontuacao <= 75) {

        return {
            nivel: "Alto",
            classe: "high",
            descricao:
                "Uso de IA que requer maior atenção."
        };

    }


    return {
        nivel: "Crítico",
        classe: "critical",
        descricao:
            "Uso de IA que requer prioridade de análise e tratamento."
    };

}


function calcularNivelRiscoDepartamento(item) {

    let pontuacao = null;


    if (typeof item.pontuacao == "number") {

        pontuacao =
            item.pontuacao;

    } else if (item.fatores) {

        pontuacao =
            calcularRiscoDepartamental(
                item.fatores
            );

    }


    if (pontuacao == null) {

        return item;

    }


    const classificacao =
        classificarNivelRisco(
            pontuacao
        );


    return {
        ...item,

        pontuacao:
            Math.round(
                pontuacao * 100
            ) / 100,

        nivel:
        classificacao.nivel,

        classe:
        classificacao.classe,

        descricao:
        classificacao.descricao
    };

}


app.get(
    "/mapa-risco",
    async (req, res) => {

        if (
            perfil != "gestor" &&
            perfil != "administrador"
        ) {

            return res.redirect("/");

        }


        const mapaRiscoMock = [

            {

                departamento:
                    "Tecnologia",

                nivel: "Baixo",

                classe: "low",

                descricao:
                    "Uso de IA com menor nível de atenção."

            },

            {

                departamento:
                    "Marketing",

                nivel: "Médio",

                classe: "medium",

                descricao:
                    "Uso de IA que requer acompanhamento."

            },

            {

                departamento:
                    "Financeiro",

                nivel: "Alto",

                classe: "high",

                descricao:
                    "Uso de IA que requer maior atenção."

            },

            {

                departamento:
                    "RH",

                nivel: "Médio",

                classe: "medium",

                descricao:
                    "Uso de IA que requer acompanhamento."

            }

        ];


        const mapaRiscoRecebido =
            await consultarListaComFallback(
                "/indicadores/mapa-risco",
                mapaRiscoMock
            );


        const mapaRisco =
            mapaRiscoRecebido.map(
                calcularNivelRiscoDepartamento
            );


        const resumoRisco = {

            baixo:
            mapaRisco.filter(
                function(item) {

                    return item.classe ==
                        "low";

                }
            ).length,

            medio:
            mapaRisco.filter(
                function(item) {

                    return item.classe ==
                        "medium";

                }
            ).length,

            alto:
            mapaRisco.filter(
                function(item) {

                    return item.classe ==
                        "high";

                }
            ).length,

            critico:
            mapaRisco.filter(
                function(item) {

                    return item.classe ==
                        "critical";

                }
            ).length

        };


        res.render("mapa-risco", {

            perfil: perfil,

            email: email,

            active: "mapa-risco",

            mapaRisco:
            mapaRisco,

            resumoRisco:
            resumoRisco

        });

    }
);


/* HESTIA PASS */

app.get(
    "/passe-hestia",
    async (req, res) => {

        if (perfil != "colaborador") {

            return res.redirect("/");

        }


        /*
            DADOS MOCKADOS DO HESTIA PASS

            Estes dados representam o retorno
            que futuramente deverá vir do backend.

            A avaliação de conformidade acontece
            no fluxo de Governança.

            O HestIA utiliza essas informações
            para calcular a progressão do usuário.

            O endpoint /passe-hestia está
            preparado para integração.

            O caminho definitivo deverá ser
            confirmado com a equipe do backend.
        */

        const passeMock = {

            nivelAtual: 2,

            nomeNivel: "Consciente",

            xpAtual: 235,

            xpProximoNivel: 300,

            taxaConformidade: 87,

            totalUtilizacoesAvaliadas: 23,

            totalUtilizacoesConformes: 20,

            sequenciaConforme: 7,


            regrasXp: [
                {
                    titulo: "Utilização conforme",
                    descricao: "Após avaliação de conformidade.",
                    xp: "+10 XP",
                    tipo: "positivo"
                },
                {
                    titulo: "Utilização não conforme",
                    descricao: "Após avaliação de conformidade.",
                    xp: "-5 XP",
                    tipo: "negativo"
                },
                {
                    titulo: "Desafio concluído",
                    descricao: "Receba o XP indicado no desafio.",
                    xp: "Bônus XP",
                    tipo: "bonus"
                },
                {
                    titulo: "Reutilização eficiente",
                    descricao: "Reutilização válida de uma resposta de IA já existente.",
                    xp: "XP conforme regra",
                    tipo: "reutilizacao"
                }
            ],


            proximoNivel: {

                nivel: 3,

                nome: "Proficiente",

                xpNecessario: 300,

                descricao:
                    "Amplie sua experiência mantendo práticas responsáveis no uso de IA."

            },


            niveis: [

                {
                    nivel: 1,
                    nome: "Iniciante",
                    xpNecessario: 0,
                    status: "concluido"
                },

                {
                    nivel: 2,
                    nome: "Consciente",
                    xpNecessario: 100,
                    status: "atual"
                },

                {
                    nivel: 3,
                    nome: "Proficiente",
                    xpNecessario: 300,
                    status: "bloqueado"
                },

                {
                    nivel: 4,
                    nome: "Especialista",
                    xpNecessario: 700,
                    status: "bloqueado"
                },

                {
                    nivel: 5,
                    nome: "Embaixador HestIA",
                    xpNecessario: 1200,
                    status: "bloqueado"
                }

            ],


            desafios: [

                {
                    id: 1,
                    titulo: "Uso Responsável",
                    descricao:
                        "Alcance 25 utilizações avaliadas como conformes.",
                    progressoAtual: 20,
                    meta: 25,
                    recompensaXp: 25,
                    concluido: false
                },

                {
                    id: 2,
                    titulo: "Consistência",
                    descricao:
                        "Alcance 10 utilizações conformes consecutivas.",
                    progressoAtual: 7,
                    meta: 10,
                    recompensaXp: 30,
                    concluido: false
                },

                {
                    id: 3,
                    titulo: "Experiência em IA",
                    descricao:
                        "Complete 20 utilizações avaliadas.",
                    progressoAtual: 20,
                    meta: 20,
                    recompensaXp: 25,
                    concluido: true
                }

            ],


            reconhecimentoMaximo: {

                titulo: "Embaixador HestIA",

                descricao:
                    "Reconhecimento institucional pela evolução e pelo uso responsável de IA.",

                reconhecimento:
                    "Reconhecimento institucional HestIA",

                requisitos: [

                    "Alcançar o nível máximo do HestIA Pass",

                    "Cumprir os requisitos de experiência do programa",

                    "Manter o nível de conformidade definido para o programa"

                ]

            }

        };

        /*
            AXIOS DO HESTIA PASS

            Se o backend responder,
            utiliza os dados reais.

            Se ainda não existir ou estiver
            indisponível, utiliza passeMock.
        */

        const usuarios = await consultar("/usuarios");
        const usuarioAtual = usuarios.find(
            usuario => String(usuario.email || "").toLowerCase() === String(email).toLowerCase()
        );
        const passeApi = usuarioAtual
            ? await consultarObjetoComFallback(
                `/gamificacao/usuarios/${usuarioAtual.id}/passe`,
                null
            )
            : null;
        const passe = passeApi
            ? adaptarPasseHestIA(passeApi, passeMock)
            : passeMock;


        res.render("passe-hestia", {

            perfil: perfil,

            email: email,

            active: "passe-hestia",

            passe: passe

        });

    }
);

/* RANKING HESTIA */

app.get(
    "/ranking",
    async (req, res) => {

        if (perfil != "colaborador") {

            return res.redirect("/");

        }


        /*
            SCRUM-275 / SCRUM-277

            DADOS MOCKADOS DO RANKING

            O ranking utiliza a progressão
            do HestIA Pass como referência.

            Quando o backend disponibilizar
            o endpoint definitivo, os dados
            poderão ser carregados pelo Axios.

            O endpoint /ranking deverá ser
            confirmado com a equipe do backend.
        */

        const rankingMock = {

            posicaoUsuario: 4,

            totalParticipantes: 8,

            usuario: {

                nome: "Você",

                nivel: "Consciente",

                xp: 235

            },


            classificacao: [

                {
                    posicao: 1,
                    nome: "Ana Martins",
                    iniciais: "AM",
                    nivel: "Proficiente",
                    xp: 480,
                    usuarioAtual: false
                },

                {
                    posicao: 2,
                    nome: "Lucas Ferreira",
                    iniciais: "LF",
                    nivel: "Proficiente",
                    xp: 390,
                    usuarioAtual: false
                },

                {
                    posicao: 3,
                    nome: "Mariana Costa",
                    iniciais: "MC",
                    nivel: "Proficiente",
                    xp: 315,
                    usuarioAtual: false
                },

                {
                    posicao: 4,
                    nome: "Você",
                    iniciais: "KL",
                    nivel: "Consciente",
                    xp: 235,
                    usuarioAtual: true
                },

                {
                    posicao: 5,
                    nome: "Rafael Souza",
                    iniciais: "RS",
                    nivel: "Consciente",
                    xp: 210,
                    usuarioAtual: false
                },

                {
                    posicao: 6,
                    nome: "Beatriz Lima",
                    iniciais: "BL",
                    nivel: "Consciente",
                    xp: 180,
                    usuarioAtual: false
                },

                {
                    posicao: 7,
                    nome: "Gabriel Rocha",
                    iniciais: "GR",
                    nivel: "Consciente",
                    xp: 145,
                    usuarioAtual: false
                },

                {
                    posicao: 8,
                    nome: "Camila Alves",
                    iniciais: "CA",
                    nivel: "Iniciante",
                    xp: 75,
                    usuarioAtual: false
                }

            ]

        };


        /*
            AXIOS DO RANKING

            Se o backend responder,
            utiliza os dados reais.

            Se o backend ainda estiver
            indisponível, utiliza rankingMock.
        */

        const ranking =
            await consultarObjetoComFallback(
                "/ranking",
                rankingMock
            );


        res.render("ranking", {

            perfil: perfil,

            email: email,

            active: "ranking",

            ranking: ranking

        });

    }
);


/* SAIR */

app.get("/sair", (req, res) => {

    email = "";

    perfil = "";

    res.redirect("/acesso");

});


/* INTEGRACAO COM O BACKEND */

app.post(
    "/administracao/empresas",
    exigirAdministrador,
    async (req, res) => {

        await executarCadastro(
            res,
            "/empresas",
            {

                nome:
                req.body.nome,

                cnpj:
                req.body.cnpj,

                configuracoesGerais:
                    req.body.configuracoesGerais ||
                    null,

                orcamento:
                    numeroOpcional(
                        req.body.orcamento
                    )

            },
            "/administracao",
            "Empresa cadastrada com sucesso."
        );

    }
);


app.post(
    "/administracao/departamentos",
    exigirAdministrador,
    async (req, res) => {

        await executarCadastro(
            res,
            "/departamentos",
            {

                nome:
                req.body.nome,

                responsavel:
                    req.body.responsavel ||
                    null,

                estruturaHierarquica:
                    req.body.estruturaHierarquica ||
                    null,

                empresaId:
                    Number(
                        req.body.empresaId
                    )

            },
            "/administracao",
            "Departamento cadastrado com sucesso."
        );

    }
);


app.post(
    "/administracao/usuarios",
    exigirAdministrador,
    async (req, res) => {

        await executarCadastro(
            res,
            "/usuarios",
            {

                nome:
                req.body.nome,

                email:
                req.body.email,

                senha:
                req.body.senha,

                cargo:
                req.body.cargo,

                perfil:
                req.body.perfil,

                ativo: true,

                empresaId:
                    Number(
                        req.body.empresaId
                    ),

                departamentoId:
                    Number(
                        req.body.departamentoId
                    )

            },
            "/administracao",
            "Usuário cadastrado com sucesso."
        );

    }
);


app.post(
    "/administracao/ferramentas",
    exigirAdministrador,
    async (req, res) => {

        await executarCadastro(
            res,
            "/ferramentas-ia",
            {

                nome:
                req.body.nome,

                fornecedor:
                req.body.fornecedor,

                descricao:
                    req.body.descricao ||
                    null,

                tipo:
                req.body.tipo,

                finalidadeUso:
                req.body.finalidadeUso,

                urlAcesso:
                    req.body.urlAcesso ||
                    null,

                trataDadosPessoais:
                    req.body.trataDadosPessoais ===
                    "true",

                empresaId:
                    Number(
                        req.body.empresaId
                    )

            },
            "/administracao",
            "Ferramenta de IA cadastrada com sucesso."
        );

    }
);


app.post(
    "/governanca/politicas",
    exigirAdministrador,
    async (req, res) => {

        await executarCadastro(
            res,
            "/politicas",
            {

                titulo:
                req.body.titulo,

                descricao:
                req.body.descricao,

                conteudo:
                req.body.conteudo,

                versao:
                req.body.versao,

                ativa: true,

                empresaId:
                    Number(
                        req.body.empresaId
                    )

            },
            "/governanca",
            "Política cadastrada com sucesso."
        );

    }
);


/* PROTECAO ADMIN */

function exigirAdministrador(
    req,
    res,
    next
) {

    if (
        perfil !== "administrador"
    ) {

        return res.redirect(
            "/acesso"
        );

    }


    next();

}


/* POST COM AXIOS */

async function executarCadastro(
    res,
    endpoint,
    dados,
    retorno,
    sucesso
) {

    try {

        await api.post(
            endpoint,
            dados
        );


        res.redirect(
            `${retorno}?mensagem=${encodeURIComponent(
                sucesso
            )}`
        );

    }

    catch (error) {

        const detalhe =
            mensagemDaApi(error);


        res.redirect(
            `${retorno}?erro=${encodeURIComponent(
                detalhe
            )}`
        );

    }

}


/* GET COM AXIOS */

async function consultar(endpoint) {

    try {

        const response =
            await api.get(endpoint);


        return Array.isArray(
            response.data
        )
            ?
            response.data
            :
            [];

    }

    catch (error) {

        console.error(
            `Falha ao consultar ${endpoint}: ${mensagemDaApi(error)}`
        );


        return [];

    }

}


/*
    CONSULTAS COM FALLBACK

    Estas funções deixam as telas
    preparadas para dados reais.

    Se o backend responder:
    -> usa response.data

    Se o backend não responder:
    -> mantém os mocks atuais.

    Os endpoints dos indicadores
    precisam ser confirmados com
    a equipe responsável pelo backend.
*/

async function consultarListaComFallback(
    endpoint,
    fallback
) {

    try {

        const response =
            await api.get(endpoint);


        return Array.isArray(
            response.data
        )
            ?
            response.data
            :
            fallback;

    }

    catch (error) {

        console.error(
            `Falha ao consultar ${endpoint}: ${mensagemDaApi(error)}. Utilizando dados locais.`
        );


        return fallback;

    }

}


async function consultarObjetoComFallback(
    endpoint,
    fallback
) {

    try {

        const response =
            await api.get(endpoint);


        return (
            response.data &&
            typeof response.data ===
            "object" &&
            !Array.isArray(
                response.data
            )
        )
            ?
            response.data
            :
            fallback;

    }

    catch (error) {

        console.error(
            `Falha ao consultar ${endpoint}: ${mensagemDaApi(error)}. Utilizando dados locais.`
        );


        return fallback;

    }

}


/* INDICADORES GERAIS */

async function carregarIndicadores() {

    const [
        empresas,
        departamentos,
        usuarios,
        politicas,
        ferramentas
    ] =
        await Promise.all([

            consultar("/empresas"),

            consultar("/departamentos"),

            consultar("/usuarios"),

            consultar("/politicas"),

            consultar("/ferramentas-ia")

        ]);


    return {

        empresas:
        empresas.length,

        departamentos:
        departamentos.length,

        usuarios:
        usuarios.length,

        politicas:
        politicas.length,

        ferramentas:
        ferramentas.length

    };

}


/* MENSAGEM DA API */

function mensagemDaApi(error) {

    const data =
        error.response &&
        error.response.data;


    if (
        data &&
        Array.isArray(
            data.errors
        )
    ) {

        return data.errors
            .map(
                item =>
                    item.message ||
                    item
            )
            .join(" ");

    }


    return (
            data &&
            (
                data.message ||
                data.error
            )
        )
        ||
        (
            error.code ===
            "ECONNREFUSED"
                ?
                "Backend indisponível."
                :
                error.message
        );

}


/* NUMERO OPCIONAL */

function numeroOpcional(valor) {

    return (
        valor === undefined ||
        valor === ""
    )
        ?
        null
        :
        Number(valor);

}


/* SERVIDOR */

app.listen(3000, () => {

    console.log(
        "Servidor rodando"
    );

});

async function consultarReutilizacoes(empresaId) {
    try {
        const response = await api.get(
            `/api/respostas-reutilizaveis/empresa/${empresaId}/reutilizaveis`
        );
        return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
        console.error(`Falha ao consultar respostas reutilizáveis: ${mensagemDaApi(error)}`);
        return [];
    }
}

function adaptarPasseHestIA(passeApi, fallback) {
    const nomesNiveis = {
        APRENDIZ: { nivel: 1, nome: "Aprendiz" },
        CONSCIENTE: { nivel: 2, nome: "Consciente" },
        GUARDIAO: { nivel: 3, nome: "Guardião" },
        EMBAIXADOR: { nivel: 4, nome: "Embaixador" }
    };
    const nivelApi = nomesNiveis[String(passeApi.nivel || "").toUpperCase()];
    const nivelAtual = nivelApi ? nivelApi.nivel : fallback.nivelAtual;
    const xpAtual = Number(passeApi.xpTotal || 0);
    const xpProximoNivel = passeApi.xpProximoNivel || fallback.xpProximoNivel;
    const missoes = Array.isArray(passeApi.missoes) ? passeApi.missoes : [];

    return {
        ...fallback,
        nivelAtual,
        nomeNivel: nivelApi ? nivelApi.nome : fallback.nomeNivel,
        xpAtual,
        xpProximoNivel,
        desafios: missoes.map(missao => ({
            titulo: missao.titulo,
            descricao: missao.descricao,
            progressoAtual: missao.progresso,
            meta: missao.meta,
            recompensaXp: missao.recompensaXp,
            concluido: missao.concluida
        })),
        niveis: fallback.niveis.map(nivel => ({
            ...nivel,
            status: nivel.nivel < nivelAtual
                ? "concluido"
                : nivel.nivel === nivelAtual
                    ? "atual"
                    : "bloqueado"
        }))
    };
}
