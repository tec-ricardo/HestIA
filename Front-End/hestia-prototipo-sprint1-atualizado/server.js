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


    res.render("index", {

        perfil: perfil,

        email: email,

        active: "dashboard",

        indicadores: indicadores,

        resumoPessoal: resumoPessoal

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


    res.render("maturidade", {

        perfil: perfil,

        email: email,

        active: "maturidade",

        indicadores: indicadores,

        maturityScore:
            maturityScore.toFixed(2)

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

        resumoHistorico:
            resumoHistorico

    });

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


        const mapaRisco =
            await consultarListaComFallback(
                "/indicadores/mapa-risco",
                mapaRiscoMock
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

        const passe =
            await consultarObjetoComFallback(
                "/passe-hestia",
                passeMock
            );


        res.render("passe-hestia", {

            perfil: perfil,

            email: email,

            active: "passe-hestia",

            passe: passe

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

async function consultarReutilizacoes() {

    /*
        SCRUM-303

        Integração preparada para o backend.

        Quando o endpoint de reutilização estiver definido
        pela equipe de backend, esta função poderá utilizar:

        return await consultar("/endpoint-definido-pelo-backend");

        Por enquanto, retornamos os dados demonstrativos
        utilizados pelo front.
    */

    return {
        totalReutilizacoes: 0,
        reutilizacoesValidas: 0,
        xpRecebido: 0
    };
}