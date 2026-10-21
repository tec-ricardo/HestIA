const test = require("node:test");
const assert = require("node:assert/strict");
const http = require("node:http");
const path = require("node:path");
const ejs = require("ejs");
const { carregarConfianca } = require("../confianca-maturidade");

const resultado = (valor, classe, situacao = "CALCULADO") => ({
    periodo: { inicio: "2026-01-01", fim: "2026-01-31" },
    visao: "PESSOAL", apuradoEm: "2026-02-01T12:00:00",
    versaoComposicao: "1.0", versaoConfianca: "CONFIANCA-1.0",
    scoreGlobal: null, scoreParcial: null, coberturaPesos: 0,
    limitacoes: ["Somente registros cadastrados."],
    indicadores: [{ codigo: "usoResponsavel", nome: "Uso Responsável", peso: 0.15,
        valor, fonte: "OPERACIONAL", formula: "conformes / conclusivos",
        confianca: { classificacao: classe, situacao, motivos: ["Motivo verificável"],
            cobertura: valor == null ? null : 0.95, completude: 1, amostra: 30,
            universoObservado: 32, amostraMinima: 30, limiteCobertura: 0.95, versaoRegra: "CONFIANCA-1.0" }
    }]
});

test("BFF encaminha sessão e período sem aceitar empresa do navegador", async () => {
    const api = { get: async (url, config) => {
        assert.equal(url, "/indicadores/confiabilidade");
        assert.equal(config.headers.Authorization, "Bearer privado");
        assert.deepEqual(config.params, { inicio: "2026-01-01", fim: "2026-01-31" });
        return { data: resultado(0, "CONFIAVEL") };
    }};
    const r = await carregarConfianca(api, { apiToken: "privado" }, { inicio: "2026-01-01", fim: "2026-01-31" });
    assert.equal(r.dados.indicadores[0].valor, 0);
});

test("demonstração, falha e sessão expirada não geram notas fictícias", async () => {
    let chamadas = 0;
    const api = { get: async () => { chamadas++; throw new Error("offline"); }};
    assert.equal((await carregarConfianca(api, {}, {})).dados, null);
    assert.equal(chamadas, 0);
    assert.equal((await carregarConfianca(api, { apiToken: "token" }, {})).status, 503);
    const sessao = { apiToken: "expirado" };
    const r = await carregarConfianca({ get: async () => { throw { response: { status: 401 }}; }}, sessao, {});
    assert.equal(r.status, 401);
    assert.equal(sessao.apiToken, undefined);
    assert.equal(r.dados, null);
});

test("template mostra nulo, zero medido, parcial e estimado corretamente e escapa conteúdo", async () => {
    for (const [valor, classe, situacao, rotulo] of [
        [null, "ESTIMADO", "SEM_DADOS", "Sem dados"],
        [0, "CONFIAVEL", "CALCULADO", "Confiável"],
        [75, "PARCIAL", "CALCULADO", "Parcial"],
        [75, "ESTIMADO", "CALCULADO", "Estimado"]
    ]) {
        const dados = resultado(valor, classe, situacao);
        dados.indicadores[0].confianca.motivos.push("<script>alert(1)</script>");
        for (const perfil of ["colaborador", "gestor", "administrador"]) {
            const html = await ejs.renderFile(path.join(__dirname, "../views/maturidade.ejs"), {
                perfil, email: "teste@example.test", active: "maturidade",
                inicio: "2026-01-01", fim: "2026-01-31", visao: "PESSOAL",
                gerencial: perfil !== "colaborador", dados, erro: null
            });
            assert.ok(html.includes(rotulo));
            assert.ok(html.includes("CONFIANCA-1.0"));
            assert.ok(html.includes("2026-01-31"));
            assert.ok(!html.includes("<script>alert(1)</script>"));
            assert.ok(html.includes("&lt;script&gt;"));
            if (valor === 0) assert.ok(html.includes("0 / 100"));
            if (valor == null) assert.ok(!html.includes("null / 100"));
        }
    }
});

test("login real, sessão BFF, consulta, expiração e logout funcionam por HTTP", async () => {
    let revogado = false;
    const mock = http.createServer(async (req, res) => {
        res.setHeader("Content-Type", "application/json");
        if (req.url === "/auth/login") {
            let body = ""; for await (const parte of req) body += parte;
            const credenciais = JSON.parse(body);
            if (credenciais.senha !== "senha-de-teste") { res.writeHead(401); return res.end("{}"); }
            return res.end(JSON.stringify({ token: "token-teste", perfil: "GESTOR" }));
        }
        if (req.url === "/auth/logout") { revogado = true; res.writeHead(204); return res.end(); }
        if (req.url.startsWith("/indicadores/confiabilidade")) {
            assert.equal(req.headers.authorization, "Bearer token-teste");
            if (req.url.includes("inicio=2000")) { res.writeHead(401); return res.end("{}"); }
            return res.end(JSON.stringify(resultado(0, "CONFIAVEL")));
        }
        res.writeHead(404); res.end("{}");
    });
    await new Promise(resolve => mock.listen(0, "127.0.0.1", resolve));
    process.env.API_URL = "http://127.0.0.1:" + mock.address().port;
    const app = require("../server");
    const frontend = app.listen(0, "127.0.0.1");
    await new Promise(resolve => frontend.once("listening", resolve));
    const base = "http://127.0.0.1:" + frontend.address().port;
    try {
        const login = await fetch(base + "/acesso", { method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: "email=gestor%40example.test&senha=senha-de-teste", redirect: "manual" });
        assert.equal(login.status, 302);
        assert.equal(login.headers.get("location"), "/maturidade");
        const cookie = login.headers.get("set-cookie").split(";")[0];
        const pagina = await fetch(base + "/maturidade", { headers: { Cookie: cookie }});
        assert.equal(pagina.status, 200);
        assert.equal(pagina.headers.get("cache-control"), "no-store");
        const html = await pagina.text();
        assert.ok(html.includes("Confiável"));
        assert.ok(!html.includes("token-teste"));
        const logout = await fetch(base + "/sair", { headers: { Cookie: cookie }, redirect: "manual" });
        assert.equal(logout.status, 302);
        assert.ok(revogado);
        const negado = await fetch(base + "/acesso", { method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: "email=gestor%40example.test&senha=errada" });
        assert.equal(negado.status, 401);
    } finally {
        await new Promise(resolve => frontend.close(resolve));
        await new Promise(resolve => mock.close(resolve));
    }
});
