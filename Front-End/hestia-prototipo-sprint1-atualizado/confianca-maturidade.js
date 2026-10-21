const PERFIS = { ADMIN: "administrador", GESTOR: "gestor", FUNCIONARIO: "colaborador" };

async function autenticarContaReal(req, res, api) {
    const email = String(req.body.email || "").trim().toLowerCase();
    if (/@(adm|gestor|colaborador)$/.test(email)) {
        delete req.session.apiToken;
        return false; // Os acessos de demonstração existentes nunca recebem acesso à API.
    }
    delete req.session.apiToken;
    req.session.perfil = "";
    try {
        const { data } = await api.post("/auth/login", { email, senha: String(req.body.senha || "") });
        if (!data.token || !PERFIS[data.perfil]) throw new Error("Sessão inválida.");
        await new Promise((resolve, reject) => req.session.regenerate(e => e ? reject(e) : resolve()));
        req.session.apiToken = data.token;
        req.session.email = email;
        req.session.perfil = PERFIS[data.perfil];
        await new Promise((resolve, reject) => req.session.save(e => e ? reject(e) : resolve()));
        res.redirect("/maturidade");
    } catch (e) {
        const negado = [400, 401, 403].includes(e.response?.status);
        res.status(negado ? 401 : 503).render("login", {
            erro: negado ? "E-mail ou senha inválidos, ou conta sem acesso."
                : "Não foi possível acessar o serviço de autenticação. Tente novamente."
        });
    }
    return true;
}

function dataLocal(data) {
    return [data.getFullYear(), String(data.getMonth() + 1).padStart(2, "0"),
        String(data.getDate()).padStart(2, "0")].join("-");
}

async function carregarConfianca(api, sessao, parametros) {
    if (!sessao.apiToken) return { status: 200, dados: null,
        erro: "Este acesso é demonstrativo. Entre com uma conta cadastrada no HestIA para consultar indicadores reais." };
    try {
        const { data } = await api.get("/indicadores/confiabilidade", {
            params: parametros, headers: { Authorization: "Bearer " + sessao.apiToken }
        });
        if (!data || !Array.isArray(data.indicadores) || !data.periodo || !data.versaoConfianca)
            throw new Error("Resposta de indicadores inválida.");
        return { status: 200, dados: data, erro: null };
    } catch (e) {
        const status = e.response?.status;
        if (status === 401) {
            delete sessao.apiToken;
            return { status: 401, dados: null, erro: "Sua sessão da API expirou. Entre novamente." };
        }
        return { status: [400, 403, 404].includes(status) ? status : 503, dados: null,
            erro: status === 403 ? "Seu perfil não permite consultar esse escopo."
                : status === 400 ? "Confira as datas e o escopo do período informado."
                : status === 404 ? "A versão ou o serviço de indicadores não está disponível."
                : "Indicadores indisponíveis no momento. Nenhuma nota foi substituída por dados demonstrativos." };
    }
}

function criarRotaMaturidade(api) {
    return async (req, res) => {
        const hoje = new Date();
        const inicioPadrao = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() - 29);
        const inicio = String(req.query.inicio || dataLocal(inicioPadrao));
        const fim = String(req.query.fim || dataLocal(hoje));
        const gerencial = ["gestor", "administrador"].includes(req.session.perfil);
        const visao = String(req.query.visao || (gerencial ? "GERENCIAL" : "PESSOAL"));
        const resultado = await carregarConfianca(api, req.session, { inicio, fim, visao });
        res.set("Cache-Control", "no-store").status(resultado.status).render("maturidade", {
            perfil: req.session.perfil, email: req.session.email, active: "maturidade",
            inicio, fim, visao, gerencial, dados: resultado.dados, erro: resultado.erro
        });
    };
}

module.exports = { autenticarContaReal, carregarConfianca, criarRotaMaturidade };
