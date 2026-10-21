const express = require("express");
const axios = require("axios");
const app = express();
const session = require("express-session");

const api = axios.create({
    baseURL: process.env.API_URL || "http://localhost:8080",
    timeout: 5000
});


app.set("view engine", "ejs");

app.use(express.static("public"));

app.use(express.urlencoded({
    extended: true
}));

/* SCRUM-376 - SESSAO INDIVIDUAL */

app.use(session({
    secret: process.env.SESSION_SECRET || "hestia-prototipo-alterar-esta-chave",
    resave: false,
    saveUninitialized: false,
    rolling: true,
    cookie: {
        httpOnly: true,
        sameSite: "lax",
        secure: process.env.NODE_ENV === "production",
        maxAge: 30 * 60 * 1000
    }
}));

/* PROTECAO DE ROTAS E EXPIRACAO */

app.use(function(req, res, next) {

    if (req.path == "/acesso") {
        return next();
    }

    if (!req.session || !req.session.perfil) {
        return res.redirect("/acesso?expirada=1");
    }

    next();

});



/* USUARIO */

let email = "";
let perfil = "";
/* SCRUM-376: variaveis antigas preservadas; autenticacao agora usa req.session. */
const empresaAtualId = Number(process.env.HESTIA_EMPRESA_ID || 1);


/* LOGIN */

app.get("/acesso", (req, res) => {

    let erro = "";

    if (req.query.expirada == "1") {
        erro = "Sua sessão expirou. Entre novamente para continuar.";
    }

    if (req.query.saiu == "1") {
        erro = "Você saiu da plataforma. Entre novamente quando quiser.";
    }

    res.render("login", {
        erro: erro
    });

});


app.post("/acesso", (req, res) => {

    req.session.email = String(req.body.email || "").trim().toLowerCase();

    const senha = String(req.body.senha || "");

    req.session.perfil = "";


    if (senha == "") {

        return res.render("login", {
            erro: "Informe a senha."
        });

    }


    if (req.session.email.endsWith("@adm")) {

        req.session.perfil = "administrador";

    }

    else if (req.session.email.endsWith("@gestor")) {

        req.session.perfil = "gestor";

    }

    else if (req.session.email.endsWith("@colaborador")) {

        req.session.perfil = "colaborador";

    }

    else {

        return res.render("login", {
            erro: "Perfil não identificado."
        });

    }


    req.session.save(function(erro) {

        if (erro) {
            return res.render("login", {
                erro: "Não foi possível iniciar sua sessão. Tente novamente."
            });
        }

        res.redirect("/");

    });

});


/* VISAO GERAL */

app.get("/", async (req, res) => {

    if (req.session.perfil == "") {

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
        req.session.perfil == "colaborador"
            ?
            await consultarObjetoComFallback(
                "/resumo-pessoal",
                detalhesResumoPessoalMock
            )
            :
            detalhesResumoPessoalMock;


    res.render("index", {

        perfil: req.session.perfil,

        email: req.session.email,

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

    if (req.session.perfil == "") {

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

        perfil: req.session.perfil,

        email: req.session.email,

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
        menorMaturidade,

        // SCRUM-449: contrato preparado; sem backend de apuração disponível.
        // O template interpreta propriedades ausentes como "não apurado".
        // SCRUM-447/448: dados inteiramente fictícios para demonstrar o layout.
        // SCRUM-449: substituir por resposta do backend Java quando disponível.
        narrativaBanco: {
            origem: "Mock demonstrativo no BFF (não é dado do banco)",
            demonstrativo: true,
            inicio: "2026-01",
            fim: "2026-06",
            totalUtilizacoes: 11150,
            treinamentos: 94,
            eventosSeguranca: 7,
            evolucao: [
                { mes: "2026-01", registros: 1200 },
                { mes: "2026-02", registros: 1450 },
                { mes: "2026-03", registros: 1680 },
                { mes: "2026-04", registros: 1920 },
                { mes: "2026-05", registros: 2150 },
                { mes: "2026-06", registros: 2750 }
            ]
        },

        maturidadeMeta: {
            periodoInicio: null,
            periodoFim: null,
            coberturaIndicadores: null,
            versaoRegra: "1.0",
            versaoCalculo: null,
            status: "Aguardando apuração validada",
            indicadores: []
        }

    });

});


/* CREDITOS */

app.get("/creditos", (req, res) => {

    if (req.session.perfil == "") {

        return res.redirect("/acesso");

    }


    res.render("creditos", {

        perfil: req.session.perfil,

        email: req.session.email,

        active: "creditos"

    });

});


/* UTILIZACOES */

app.get("/utilizacoes", async (req, res) => {

    if (req.session.perfil != "colaborador") {

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

        perfil: req.session.perfil,

        email: req.session.email,

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


/* SCRUM-378 / SCRUM-379 - REGISTRO DE UTILIZACOES */

app.post("/utilizacoes/registrar", async (req, res) => {

    if (perfil != "colaborador") {
        return res.redirect("/");
    }

    const usuarioId = Number(req.body.usuarioId);
    const ferramentaId = Number(req.body.ferramentaId);
    const tokensEntrada = Number(req.body.tokensEntrada);
    const tokensSaida = Number(req.body.tokensSaida);
    const modeloIa = String(req.body.modeloIa || "").trim();
    const finalidade = String(req.body.finalidade || "").trim();
    const statusExecucao = String(req.body.statusExecucao || "").trim();
    const dataHora = String(req.body.dataHora || "").trim();

    const finalidadesPermitidas = [
        "GERACAO_CODIGO", "GERACAO_CONTEUDO", "PESQUISA", "ANALISE_DADOS",
        "RESUMO", "AUTOMACAO", "SUPORTE_DECISAO", "OUTROS"
    ];

    const statusPermitidos = ["SUCESSO", "ERRO", "INTERROMPIDA"];

    if (
        !Number.isInteger(usuarioId) || usuarioId <= 0 ||
        !Number.isInteger(ferramentaId) || ferramentaId <= 0 ||
        req.body.tokensEntrada === undefined ||
        req.body.tokensSaida === undefined ||
        !Number.isInteger(tokensEntrada) || tokensEntrada < 0 ||
        !Number.isInteger(tokensSaida) || tokensSaida < 0 ||
        !modeloIa || !finalidadesPermitidas.includes(finalidade) ||
        !statusPermitidos.includes(statusExecucao) ||
        !dataHora || Number.isNaN(Date.parse(dataHora))
    ) {
        return res.redirect("/utilizacoes?registro=erro");
    }

    try {
        await api.post("/utilizacoes", {
            usuarioId: usuarioId,
            ferramentaId: ferramentaId,
            tokensEntrada: tokensEntrada,
            tokensSaida: tokensSaida,
            modeloIa: modeloIa,
            finalidade: finalidade,
            statusExecucao: statusExecucao,
            dataHora: dataHora
        });

        return res.redirect("/utilizacoes?registro=ok");
    } catch (error) {
        console.error("Falha ao registrar utilizacao:", mensagemDaApi(error));
        return res.redirect("/utilizacoes?registro=erro");
    }

});


/* COMPETENCIAS */

app.get("/competencias", (req, res) => {

    if (
        req.session.perfil != "colaborador" &&
        req.session.perfil != "administrador"
    ) {

        return res.redirect("/");

    }


    res.render("competencias", {

        perfil: req.session.perfil,

        email: req.session.email,

        active: "competencias"

    });

});




/* SCRUM-386, 387, 388: frontend demonstrativo de ocorrências.
   SCRUM-389 (integração com API) será implementada depois. */
app.get("/ocorrencias", (req, res) => {
    if (!["administrador", "gestor"].includes(req.session.perfil)) {
        return res.redirect("/");
    }
    res.render("ocorrencias", {
        perfil: req.session.perfil,
        email: req.session.email,
        active: "ocorrencias"
    });
});

/* GOVERNANCA */

app.get("/governanca", async (req, res) => {

    if (req.session.perfil == "") {

        return res.redirect("/acesso");

    }


    const [politicas, empresas] =
        await Promise.all([

            consultar("/politicas"),

            consultar("/empresas")

        ]);


    res.render("governanca", {

        perfil: req.session.perfil,

        email: req.session.email,

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

    if (req.session.perfil != "gestor") {

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

        perfil: req.session.perfil,

        email: req.session.email,

        active: "custos",

        dadosEficiencia:
            dadosEficiencia

    });

});


/* ADMINISTRACAO */

app.get(
    "/administracao",
    async (req, res) => {

        if (req.session.perfil != "administrador") {

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

            perfil: req.session.perfil,

            email: req.session.email,

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

        if (req.session.perfil != "administrador") {

            return res.redirect("/");

        }


        const ferramentas =
            await consultar(
                "/ferramentas-ia"
            );


        res.render("configuracoes", {

            perfil: req.session.perfil,

            email: req.session.email,

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
            req.session.perfil != "gestor" &&
            req.session.perfil != "administrador"
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

            perfil: req.session.perfil,

            email: req.session.email,

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

        if (req.session.perfil != "colaborador") {

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
            usuario => String(usuario.email || "").toLowerCase() === String(req.session.email).toLowerCase()
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

            perfil: req.session.perfil,

            email: req.session.email,

            active: "passe-hestia",

            passe: passe

        });

    }
);

/* RANKING HESTIA */

app.get(
    "/ranking",
    async (req, res) => {

        if (req.session.perfil != "colaborador") {

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

            perfil: req.session.perfil,

            email: req.session.email,

            active: "ranking",

            ranking: ranking

        });

    }
);


/* SCRUM-514 / 515 / 516 / 517 - CURADORIA (FRONTEND) */
app.get("/curadoria", (req, res) => {
    if (!["administrador", "gestor"].includes(req.session.perfil)) {
        return res.redirect("/");
    }

    res.render("curadoria", {
        perfil: req.session.perfil,
        email: req.session.email,
        active: "curadoria"
    });
});

/* ASSISTENTE RAG - SCRUM-457 / 458 / 459 / 460 / 461 / 464 */

app.get("/assistente-rag", (req, res) => {

    res.render("assistente-rag", {
        perfil: req.session.perfil,
        email: req.session.email,
        active: "assistente-rag"
    });

});


/* SAIR */

app.get("/sair", (req, res) => {

    req.session.destroy(function(erro) {

        if (erro) {
            return res.redirect("/");
        }

        res.clearCookie("connect.sid");
        res.redirect("/acesso?saiu=1");

    });

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
        req.session.perfil !== "administrador"
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



app.use(express.json());

/* MICROtreinamentos — SCRUM-504 a 508
   API de demonstração: dados em memória. Substituir por integração backend
   quando os contratos reais estiverem disponíveis. */
const microCatalogo = [
    {
        "id": "dados",
        "titulo": "Proteção de Dados e IA",
        "categoria": "Aprendizagem em IA",
        "duracao": 21,
        "nivel": "Bronze",
        "descricao": "Identifique informações sensíveis e aprenda a trabalhar com IA sem expor dados da empresa.",
        "recomendado": true,
        "motivo": "Trilha introdutória recomendada para todos.",
        "prerequisito": null,
        "licoes": [
            {
                "titulo": "Por que proteger dados?",
                "texto": "Ferramentas de inteligência artificial processam as informações que recebem. Por isso, antes de escrever um comando, o colaborador precisa avaliar se o conteúdo contém dados pessoais, financeiros, estratégicos ou sigilosos. Mesmo uma informação aparentemente simples pode identificar uma pessoa quando combinada com outras.\n\nExemplo prático: um funcionário deseja resumir uma planilha de clientes. Em vez de enviar nomes, CPFs, telefones e contratos completos, ele deve usar apenas informações necessárias, devidamente anonimizadas e em uma ferramenta aprovada pela organização.\n\nBoa prática: confira a política interna, a classificação do documento e as permissões da ferramenta. Na dúvida, consulte a equipe de governança ou segurança antes de compartilhar."
            },
            {
                "titulo": "Como preparar um prompt seguro",
                "texto": "Um prompt seguro descreve a tarefa sem expor informações desnecessárias. Substitua dados reais por exemplos fictícios e remova nomes, credenciais, endereços e números de documentos. A anonimização precisa impedir que a pessoa seja identificada por outros detalhes.\n\nExemplo inadequado: “Analise o desempenho de Maria Silva, CPF 000..., e recomende uma advertência”. Exemplo melhor: “Explique critérios gerais para avaliar desempenho profissional, sem tomar decisões sobre pessoas específicas”.\n\nChecklist: preciso mesmo desses dados? A ferramenta é autorizada? Existe risco de identificar alguém? Um colega autorizado poderia revisar o pedido antes do envio?"
            },
            {
                "titulo": "Como agir diante de um risco",
                "texto": "Se perceber que compartilhou uma informação confidencial por engano, não tente esconder o ocorrido. Interrompa novos envios, registre o que foi compartilhado e comunique o incidente pelo canal definido pela empresa.\n\nExemplo: um relatório interno foi anexado à ferramenta errada. O caminho correto é seguir o procedimento de resposta a incidentes, preservando informações úteis para a análise e evitando espalhar ainda mais o arquivo.\n\nLembrete: proteção de dados é uma responsabilidade contínua, desde a elaboração do prompt até a revisão, o armazenamento e o compartilhamento da resposta."
            }
        ],
        "questoes": [
            {
                "pergunta": "Antes de enviar dados de clientes para uma IA, o correto é:",
                "opcoes": [
                    "Enviar a planilha completa",
                    "Minimizar os dados e verificar as regras internas",
                    "Publicar os dados para facilitar o acesso"
                ],
                "correta": 1
            },
            {
                "pergunta": "Qual informação nunca deve ser colocada em um prompt comum?",
                "opcoes": [
                    "Senha corporativa",
                    "Exemplo fictício",
                    "Pergunta genérica"
                ],
                "correta": 0
            },
            {
                "pergunta": "Ao enviar um arquivo confidencial por engano, você deve:",
                "opcoes": [
                    "Esconder o fato",
                    "Continuar enviando arquivos",
                    "Seguir o procedimento de incidente da empresa"
                ],
                "correta": 2
            }
        ]
    },
    {
        "id": "responsavel",
        "titulo": "Uso Responsável de IA",
        "categoria": "Aprendizagem em IA",
        "duracao": 21,
        "nivel": "Bronze",
        "descricao": "Saiba quando confiar, revisar e usar respostas geradas por inteligência artificial.",
        "recomendado": true,
        "motivo": "Trilha introdutória recomendada para todos.",
        "prerequisito": null,
        "licoes": [
            {
                "titulo": "IA ajuda, mas não substitui julgamento",
                "texto": "Modelos de IA podem apoiar a redação, organização de ideias e análise inicial de informações. Entretanto, também podem produzir respostas incorretas, incompletas ou inventadas. A responsabilidade pelo uso do resultado permanece com a pessoa e com os processos da organização.\n\nExemplo: a IA sugere uma regra trabalhista. Antes de colocá-la em uma apresentação, o colaborador deve consultar a legislação atual e as orientações do setor jurídico. Uma resposta bem escrita não é prova de que esteja correta.\n\nBoa prática: trate a resposta como rascunho, confirme fatos importantes e identifique quais decisões exigem aprovação humana."
            },
            {
                "titulo": "Vieses e decisões sobre pessoas",
                "texto": "Um sistema pode reproduzir estereótipos ou favorecer determinados grupos, dependendo dos dados e da forma como foi desenvolvido. Por isso, decisões de contratação, promoção, avaliação ou concessão de benefícios não devem ser delegadas cegamente à IA.\n\nExemplo: ao pedir à IA uma lista de candidatos “ideais”, a resposta pode valorizar critérios sem relação com a vaga. O colaborador deve revisar a pertinência dos critérios e seguir as políticas de diversidade e seleção.\n\nPergunte: existe algum grupo sendo prejudicado? Quais evidências sustentam a recomendação? Uma pessoa responsável revisou a decisão?"
            },
            {
                "titulo": "Transparência e revisão final",
                "texto": "É importante informar o uso de IA quando as regras da empresa, do cliente ou da atividade exigirem. Antes de compartilhar uma resposta, revise linguagem, precisão, confidencialidade e possíveis consequências.\n\nExemplo: um texto criado com IA para um cliente pode conter uma promessa que a empresa não oferece. A revisão humana precisa corrigir isso antes do envio.\n\nResumo da aula: use IA para apoiar o trabalho, nunca para dispensar responsabilidade, verificação ou cuidado com as pessoas."
            }
        ],
        "questoes": [
            {
                "pergunta": "Quem deve validar uma resposta de IA antes do uso profissional?",
                "opcoes": [
                    "O colaborador responsável",
                    "A própria IA, sem revisão",
                    "Ninguém"
                ],
                "correta": 0
            },
            {
                "pergunta": "Em decisões que afetam pessoas, é importante:",
                "opcoes": [
                    "Aceitar qualquer resposta",
                    "Verificar vieses e realizar revisão humana",
                    "Ignorar critérios"
                ],
                "correta": 1
            },
            {
                "pergunta": "Uma resposta bem escrita é necessariamente verdadeira?",
                "opcoes": [
                    "Sim",
                    "Apenas quando é longa",
                    "Não, precisa ser verificada"
                ],
                "correta": 2
            }
        ]
    },
    {
        "id": "verificacao",
        "titulo": "Verificação de Respostas",
        "categoria": "Aprendizagem em IA",
        "duracao": 21,
        "nivel": "Prata",
        "descricao": "Aprenda a checar fatos, fontes e números antes de usar respostas de IA.",
        "recomendado": false,
        "motivo": "Exige medalha Prata.",
        "prerequisito": null,
        "licoes": [
            {
                "titulo": "O que são alucinações?",
                "texto": "Uma alucinação ocorre quando a IA apresenta informações falsas ou sem base confiável como se fossem fatos. Ela pode inventar nomes de estudos, datas, estatísticas e referências. Isso acontece mesmo quando o texto parece muito convincente.\n\nExemplo: ao solicitar “três pesquisas recentes sobre produtividade”, a IA pode listar artigos inexistentes. O colaborador precisa localizar cada estudo em uma fonte confiável antes de citá-lo.\n\nSinal de alerta: respostas muito específicas sem referência verificável merecem atenção redobrada."
            },
            {
                "titulo": "Verifique fontes e contexto",
                "texto": "Comece separando afirmações que podem ser verificadas: números, leis, datas, nomes, citações e recomendações técnicas. Consulte documentos oficiais, bases institucionais ou publicações confiáveis e confira se a fonte realmente sustenta o que foi dito.\n\nExemplo: se a IA disser que uma norma foi alterada em 2026, procure a publicação oficial e confira a data de vigência. Não basta encontrar uma página que apenas repita a mesma afirmação.\n\nTambém verifique o contexto: um dado pode ser verdadeiro para outro país, período ou público e não servir para a situação da sua empresa."
            },
            {
                "titulo": "Monte uma rotina de validação",
                "texto": "Uma rotina simples possui quatro etapas: identificar as afirmações importantes, buscar evidências, comparar os resultados e registrar eventuais correções. Quando houver divergência, priorize fontes primárias e consulte um especialista.\n\nExemplo: para um relatório financeiro, compare cada valor produzido pela IA com a planilha ou sistema de origem. Refaça cálculos críticos e documente a conferência.\n\nAo final, pergunte: eu conseguiria explicar de onde veio cada informação relevante? Se a resposta for não, o material ainda não está pronto para uso."
            }
        ],
        "questoes": [
            {
                "pergunta": "O que é uma alucinação de IA?",
                "opcoes": [
                    "Um erro ou informação inventada apresentada como fato",
                    "Um tipo de gráfico",
                    "Uma ferramenta de armazenamento"
                ],
                "correta": 0
            },
            {
                "pergunta": "Qual é a melhor forma de confirmar uma norma?",
                "opcoes": [
                    "Confiar no texto gerado",
                    "Consultar a publicação oficial",
                    "Usar qualquer postagem"
                ],
                "correta": 1
            },
            {
                "pergunta": "Antes de citar uma estatística, você deve:",
                "opcoes": [
                    "Conferir fonte, período e contexto",
                    "Arredondar sem conferir",
                    "Copiar sem revisão"
                ],
                "correta": 0
            }
        ]
    },
    {
        "id": "prompts",
        "titulo": "Prompts Seguros e Eficientes",
        "categoria": "Aprendizagem em IA",
        "duracao": 21,
        "nivel": "Prata",
        "descricao": "Escreva comandos claros para obter respostas úteis sem comprometer a segurança.",
        "recomendado": false,
        "motivo": "Exige medalha Prata.",
        "prerequisito": null,
        "licoes": [
            {
                "titulo": "A estrutura de um bom prompt",
                "texto": "Um comando bem formulado explica o objetivo, o contexto permitido, o formato esperado e os critérios de qualidade. Pedidos vagos geram respostas vagas; instruções claras facilitam a revisão.\n\nExemplo vago: “Faça um relatório”. Exemplo melhor: “Organize os resultados fictícios abaixo em um resumo executivo com introdução, três achados e recomendações, sem inventar números”.\n\nSempre delimite o que a IA pode usar. Se faltar informação, peça que ela identifique as lacunas em vez de inventar dados."
            },
            {
                "titulo": "Contexto sem exposição de dados",
                "texto": "Contexto é útil, mas deve ser suficiente e não excessivo. Descreva a situação de forma genérica ou use informações autorizadas. Evite copiar conversas internas, listas de clientes e dados pessoais para ferramentas não aprovadas.\n\nExemplo: para criar uma mensagem de atendimento, descreva o tipo de problema e o tom desejado, sem informar o nome, telefone ou histórico privado de um cliente real.\n\nUma técnica útil é criar modelos de prompt reutilizáveis com campos fictícios, que depois podem ser preenchidos em ambientes autorizados."
            },
            {
                "titulo": "Iteração e avaliação do resultado",
                "texto": "A primeira resposta raramente é perfeita. Você pode pedir que a IA reorganize a estrutura, explique premissas ou destaque dúvidas. Cada revisão deve aproximar o texto dos critérios definidos no início.\n\nExemplo: após gerar um resumo, peça “separe fatos de sugestões e indique quais afirmações precisam de verificação”. Em seguida, confira os pontos importantes em fontes externas.\n\nEvite confiar em uma autoavaliação da IA como única prova de qualidade. A revisão final continua sendo humana."
            }
        ],
        "questoes": [
            {
                "pergunta": "Qual prompt tende a produzir um resultado mais útil?",
                "opcoes": [
                    "Faça qualquer coisa",
                    "Explique objetivo, contexto e formato desejado",
                    "Não dê nenhuma instrução"
                ],
                "correta": 1
            },
            {
                "pergunta": "Se faltarem dados, o melhor é pedir à IA para:",
                "opcoes": [
                    "Inventar valores",
                    "Ocultar a dúvida",
                    "Indicar as informações ausentes"
                ],
                "correta": 2
            },
            {
                "pergunta": "Como fornecer contexto com segurança?",
                "opcoes": [
                    "Usar dados mínimos e autorizados",
                    "Copiar senhas",
                    "Enviar documentos confidenciais sem avaliação"
                ],
                "correta": 0
            }
        ]
    },
    {
        "id": "etica",
        "titulo": "Ética e Governança de IA",
        "categoria": "Aprendizagem em IA",
        "duracao": 28,
        "nivel": "Ouro",
        "descricao": "Aplique princípios éticos, gestão de riscos e supervisão em situações reais.",
        "recomendado": false,
        "motivo": "Exige medalha Ouro.",
        "prerequisito": null,
        "licoes": [
            {
                "titulo": "Princípios de uma IA confiável",
                "texto": "O uso responsável de IA envolve transparência, segurança, privacidade, equidade, prestação de contas e supervisão humana. Esses princípios precisam orientar decisões práticas, não apenas documentos institucionais.\n\nExemplo: uma ferramenta que classifica solicitações de clientes deve ter critérios claros, possibilidade de revisão e controles para evitar tratamentos injustos.\n\nA governança define quem aprova o uso da ferramenta, quais dados são permitidos, como os resultados são monitorados e como incidentes devem ser reportados."
            },
            {
                "titulo": "Avaliação de riscos antes da adoção",
                "texto": "Antes de implementar uma solução, avalie a finalidade, os dados envolvidos, os possíveis impactos sobre pessoas, a criticidade da decisão e as formas de controle. Quanto maior o risco, mais rigorosa deve ser a validação.\n\nExemplo: usar IA para organizar ideias de uma apresentação costuma ter riscos diferentes de utilizá-la para recomendar crédito ou avaliar candidatos. O segundo caso exige cuidados adicionais, testes e supervisão adequada.\n\nDocumente o objetivo, as limitações conhecidas, os responsáveis e os critérios para interromper ou revisar o uso."
            },
            {
                "titulo": "Transparência e responsabilidade",
                "texto": "Governança também significa manter rastreabilidade: saber quando a IA foi utilizada, quais dados foram considerados e quem revisou o resultado. Isso facilita auditorias, correções e melhoria contínua.\n\nExemplo: ao usar IA para apoiar uma recomendação de negócio, registre as fontes verificadas, os limites do modelo e a decisão final tomada pela equipe.\n\nSe houver comportamento inesperado ou impacto negativo, o correto é comunicar pelos canais definidos e revisar o processo, não ignorar o problema."
            },
            {
                "titulo": "Estudo de caso: uso corporativo",
                "texto": "Imagine que uma área deseja usar IA para resumir manifestações de clientes. Primeiro, ela define o objetivo e avalia quais dados pessoais podem ser removidos. Depois, seleciona uma ferramenta autorizada e estabelece critérios de qualidade.\n\nDurante o teste, a equipe identifica resumos com omissões relevantes. Em vez de publicar os resultados automaticamente, cria uma etapa de revisão humana e acompanha os erros mais frequentes.\n\nResultado esperado: uma solução que gera eficiência, mas mantém proteção de dados, controle de qualidade e responsabilidade. Esses cuidados são parte do valor entregue pela governança."
            }
        ],
        "questoes": [
            {
                "pergunta": "Qual é um princípio importante da governança de IA?",
                "opcoes": [
                    "Transparência e supervisão humana",
                    "Ausência de controles",
                    "Compartilhamento irrestrito"
                ],
                "correta": 0
            },
            {
                "pergunta": "O nível de controle deve ser maior quando:",
                "opcoes": [
                    "A decisão pode afetar pessoas significativamente",
                    "A tarefa é apenas organizar ideias",
                    "O texto é curto"
                ],
                "correta": 0
            },
            {
                "pergunta": "O que fazer ao identificar um incidente?",
                "opcoes": [
                    "Ocultar o ocorrido",
                    "Comunicar pelo canal adequado e revisar o processo",
                    "Ignorar se a IA funcionar"
                ],
                "correta": 1
            }
        ]
    }
];

const microProgresso = new Map();
function chaveMicro(req) { return String(req.session.email || req.sessionID || "demo"); }
function progressoMicro(req) {
 const chave=chaveMicro(req);
 if (!microProgresso.has(chave)) microProgresso.set(chave,{});
 return microProgresso.get(chave);
}
/* Medalhas demonstrativas do HestIA Pass: Bronze 10 XP, Prata 100 XP, Ouro 200 XP.
   Até a integração com o backend, utiliza os 235 XP do mock existente. */
function medalhaMicro(xp) {
    if (xp >= 200) return "Ouro";
    if (xp >= 100) return "Prata";
    if (xp >= 10) return "Bronze";
    return "Sem medalha";
}
function listarMicro(req) {
    const progresso = progressoMicro(req);
    const xp = 235;
    const ordem = { "Sem medalha": 0, "Bronze": 1, "Prata": 2, "Ouro": 3 };
    const medalhaAtual = medalhaMicro(xp);
    return microCatalogo.map(function(curso) {
        const p = progresso[curso.id] || { etapa: 0, concluido: false };
        const faltaMedalha = ordem[medalhaAtual] < ordem[curso.nivel];
        const faltaPrerequisito = curso.prerequisito && !progresso[curso.prerequisito]?.concluido;
        return {
            id: curso.id,
            titulo: curso.titulo,
            categoria: curso.categoria,
            duracao: curso.duracao,
            nivel: curso.nivel,
            descricao: curso.descricao,
            recomendado: curso.recomendado,
            motivo: faltaMedalha ? "Requer medalha " + curso.nivel + " do HestIA Pass." :
                faltaPrerequisito ? "Conclua o treinamento anterior." : curso.motivo,
            prerequisito: curso.prerequisito,
            totalEtapas: curso.licoes.length,
            etapa: p.etapa,
            concluido: p.concluido,
            bloqueado: Boolean(faltaMedalha || faltaPrerequisito),
            medalhaAtual: medalhaAtual,
            xpAtual: xp
        };
    });
}
function apenasColaboradorMicro(req,res,next) {
 if (req.session?.perfil !== "colaborador") return res.status(403).json({erro:"Acesso exclusivo do colaborador."});
 next();
}
app.get("/microtreinamentos",(req,res)=>{
 if(req.session?.perfil!=="colaborador") return res.redirect("/");
 res.render("microtreinamento",{perfil:req.session.perfil,email:req.session.email,active:"microtreinamentos"});
});
app.get("/microtreinamentos/:id/avaliacao",(req,res)=>{
 if(req.session?.perfil!=="colaborador") return res.redirect("/");
 const t=microCatalogo.find(x=>x.id===req.params.id);
 if(!t) return res.redirect("/microtreinamentos");
 const p=progressoMicro(req)[t.id];
 if(listarMicro(req).find(x=>x.id===t.id).bloqueado || !p || p.etapa<t.licoes.length) return res.redirect("/microtreinamentos/"+t.id);
 res.render("microtreinamento-avaliacao",{perfil:req.session.perfil,active:"microtreinamentos",cursoId:t.id});
});
app.get("/microtreinamentos/:id",(req,res)=>{
 if(req.session?.perfil!=="colaborador") return res.redirect("/");
 const t=microCatalogo.find(x=>x.id===req.params.id);
 if(!t || listarMicro(req).find(x=>x.id===t.id).bloqueado) return res.redirect("/microtreinamentos");
 res.render("microtreinamento-curso",{perfil:req.session.perfil,active:"microtreinamentos",cursoId:t.id});
});
app.get("/api/microtreinamentos",apenasColaboradorMicro,(req,res)=>{
 res.json({treinamentos:listarMicro(req)});
});
app.get("/api/microtreinamentos/:id",apenasColaboradorMicro,(req,res)=>{
 const t=microCatalogo.find(x=>x.id===req.params.id);
 if(!t) return res.status(404).json({erro:"Treinamento não encontrado."});
 const resumo=listarMicro(req).find(x=>x.id===t.id);
 if(resumo.bloqueado) return res.status(403).json({erro:"Conclua o pré-requisito para desbloquear."});
 res.json({treinamento:{...resumo,licoes:t.licoes,questoes:t.questoes.map(({pergunta,opcoes})=>({pergunta,opcoes}))}});
});
app.post("/api/microtreinamentos/:id/progresso",apenasColaboradorMicro,(req,res)=>{
 const t=microCatalogo.find(x=>x.id===req.params.id);
 if(!t) return res.status(404).json({erro:"Treinamento não encontrado."});
 if(listarMicro(req).find(x=>x.id===t.id).bloqueado) return res.status(403).json({erro:"Treinamento bloqueado."});
 const progresso=progressoMicro(req), p=progresso[t.id]||{etapa:0,concluido:false};
 const etapa=Number(req.body.etapa);
 if(!Number.isInteger(etapa)||etapa<0||etapa>t.licoes.length||etapa>p.etapa+1) return res.status(400).json({erro:"Etapa inválida."});
 p.etapa=Math.max(p.etapa,etapa); progresso[t.id]=p;
 res.json({etapa:p.etapa,totalEtapas:t.licoes.length});
});
app.post("/api/microtreinamentos/:id/avaliacao",apenasColaboradorMicro,(req,res)=>{
 const t=microCatalogo.find(x=>x.id===req.params.id);
 if(!t) return res.status(404).json({erro:"Treinamento não encontrado."});
 const progresso=progressoMicro(req), p=progresso[t.id]||{etapa:0,concluido:false};
 if(p.etapa<t.licoes.length) return res.status(403).json({erro:"Conclua o conteúdo antes da avaliação."});
 const respostas=req.body.respostas;
 if(!Array.isArray(respostas)||respostas.length!==t.questoes.length||!respostas.every(Number.isInteger))
  return res.status(400).json({erro:"Responda todas as perguntas."});
 const acertos=t.questoes.filter((q,i)=>q.correta===respostas[i]).length;
 const nota=Math.round(acertos/t.questoes.length*100);
 const aprovado=nota>=70;
 if(aprovado) {p.concluido=true; progresso[t.id]=p;}
 res.json({nota,acertos,total:t.questoes.length,aprovado,mensagem:aprovado?"Treinamento concluído!":"Você precisa de pelo menos 70%. Tente novamente."});
});

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