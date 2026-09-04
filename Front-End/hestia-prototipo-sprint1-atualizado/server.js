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

    const indicadores = await carregarIndicadores();

    res.render("index", {

        perfil: perfil,

        email: email,

        active: "dashboard",
        indicadores: indicadores

    });

});


/* MATURIDADE */

app.get("/maturidade", (req, res) => {

    if (perfil == "") {

        return res.redirect("/acesso");

    }

    res.render("maturidade", {

        perfil: perfil,

        email: email,

        active: "maturidade"

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

app.get("/utilizacoes", (req, res) => {

    if (perfil != "colaborador") {

        return res.redirect("/");

    }

    res.render("utilizacoes", {

        perfil: perfil,

        email: email,

        active: "utilizacoes"

    });

});


/* COMPETENCIAS */

app.get("/competencias", (req, res) => {

    if (perfil != "colaborador" && perfil != "administrador") {

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

    const [politicas, empresas] = await Promise.all([
        consultar("/politicas"),
        consultar("/empresas")
    ]);

    res.render("governanca", {

        perfil: perfil,

        email: email,

        active: "governanca",
        politicas: politicas,
        empresas: empresas,
        mensagem: req.query.mensagem || "",
        erro: req.query.erro || ""

    });

});


/* CUSTOS */

app.get("/custos", (req, res) => {

    if (perfil != "gestor") {

        return res.redirect("/");

    }

    res.render("custos", {

        perfil: perfil,

        email: email,

        active: "custos"

    });

});


/* ADMINISTRACAO */

app.get("/administracao", async (req, res) => {

    if (perfil != "administrador") {

        return res.redirect("/");

    }

    const [empresas, departamentos, usuarios, ferramentas] = await Promise.all([
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
        mensagem: req.query.mensagem || "",
        erro: req.query.erro || ""

    });

});


/* SAIR */

app.get("/sair", (req, res) => {

    email = "";
    perfil = "";

    res.redirect("/acesso");

});


/* INTEGRACAO COM O BACKEND */

app.post("/administracao/empresas", exigirAdministrador, async (req, res) => {
    await executarCadastro(res, "/empresas", {
        nome: req.body.nome,
        cnpj: req.body.cnpj,
        configuracoesGerais: req.body.configuracoesGerais || null,
        orcamento: numeroOpcional(req.body.orcamento)
    }, "/administracao", "Empresa cadastrada com sucesso.");
});

app.post("/administracao/departamentos", exigirAdministrador, async (req, res) => {
    await executarCadastro(res, "/departamentos", {
        nome: req.body.nome,
        responsavel: req.body.responsavel || null,
        estruturaHierarquica: req.body.estruturaHierarquica || null,
        empresaId: Number(req.body.empresaId)
    }, "/administracao", "Departamento cadastrado com sucesso.");
});

app.post("/administracao/usuarios", exigirAdministrador, async (req, res) => {
    await executarCadastro(res, "/usuarios", {
        nome: req.body.nome,
        email: req.body.email,
        senha: req.body.senha,
        cargo: req.body.cargo,
        perfil: req.body.perfil,
        ativo: true,
        empresaId: Number(req.body.empresaId),
        departamentoId: Number(req.body.departamentoId)
    }, "/administracao", "Usuário cadastrado com sucesso.");
});

app.post("/administracao/ferramentas", exigirAdministrador, async (req, res) => {
    await executarCadastro(res, "/ferramentas-ia", {
        nome: req.body.nome,
        fornecedor: req.body.fornecedor,
        descricao: req.body.descricao || null,
        tipo: req.body.tipo,
        finalidadeUso: req.body.finalidadeUso,
        urlAcesso: req.body.urlAcesso || null,
        trataDadosPessoais: req.body.trataDadosPessoais === "true",
        empresaId: Number(req.body.empresaId)
    }, "/administracao", "Ferramenta de IA cadastrada com sucesso.");
});

app.post("/governanca/politicas", exigirAdministrador, async (req, res) => {
    await executarCadastro(res, "/politicas", {
        titulo: req.body.titulo,
        descricao: req.body.descricao,
        conteudo: req.body.conteudo,
        versao: req.body.versao,
        ativa: true,
        empresaId: Number(req.body.empresaId)
    }, "/governanca", "Política cadastrada com sucesso.");
});

function exigirAdministrador(req, res, next) {
    if (perfil !== "administrador") {
        return res.redirect("/acesso");
    }
    next();
}

async function executarCadastro(res, endpoint, dados, retorno, sucesso) {
    try {
        await api.post(endpoint, dados);
        res.redirect(`${retorno}?mensagem=${encodeURIComponent(sucesso)}`);
    } catch (error) {
        const detalhe = mensagemDaApi(error);
        res.redirect(`${retorno}?erro=${encodeURIComponent(detalhe)}`);
    }
}

async function consultar(endpoint) {
    try {
        const response = await api.get(endpoint);
        return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
        console.error(`Falha ao consultar ${endpoint}: ${mensagemDaApi(error)}`);
        return [];
    }
}

async function carregarIndicadores() {
    const [empresas, departamentos, usuarios, politicas, ferramentas] = await Promise.all([
        consultar("/empresas"),
        consultar("/departamentos"),
        consultar("/usuarios"),
        consultar("/politicas"),
        consultar("/ferramentas-ia")
    ]);
    return {
        empresas: empresas.length,
        departamentos: departamentos.length,
        usuarios: usuarios.length,
        politicas: politicas.length,
        ferramentas: ferramentas.length
    };
}

function mensagemDaApi(error) {
    const data = error.response && error.response.data;
    if (data && Array.isArray(data.errors)) {
        return data.errors.map(item => item.message || item).join(" ");
    }
    return (data && (data.message || data.error)) ||
        (error.code === "ECONNREFUSED" ? "Backend indisponível." : error.message);
}

function numeroOpcional(valor) {
    return valor === undefined || valor === "" ? null : Number(valor);
}


/* SERVIDOR */

app.listen(3000, () => {
    console.log("Servidor rodando");
});
