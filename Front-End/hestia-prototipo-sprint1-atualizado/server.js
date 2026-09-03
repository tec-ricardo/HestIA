const express = require("express");
const app = express();

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

app.get("/", (req, res) => {

    if (perfil == "") {

        return res.redirect("/acesso");

    }

    res.render("index", {

        perfil: perfil,

        email: email,

        active: "dashboard"

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

app.get("/governanca", (req, res) => {

    if (perfil == "") {

        return res.redirect("/acesso");

    }

    res.render("governanca", {

        perfil: perfil,

        email: email,

        active: "governanca"

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

app.get("/administracao", (req, res) => {

    if (perfil != "administrador") {

        return res.redirect("/");

    }

    res.render("administracao", {

        perfil: perfil,

        email: email,

        active: "administracao"

    });

});


/* SAIR */

app.get("/sair", (req, res) => {

    email = "";
    perfil = "";

    res.redirect("/acesso");

});


/* SERVIDOR */

app.listen(3000, () => {

    console.log("Servidor rodando");

});