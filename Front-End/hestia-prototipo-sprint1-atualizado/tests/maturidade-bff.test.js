const { test } = require("node:test");
const assert = require("node:assert/strict");
const http = require("node:http");

test("login real, isolamento e resultado parcial sem fallback demonstrativo", async t => {
    const recebidos = [];
    let indisponivel = false;
    const backend = http.createServer(async (req, res) => {
        res.setHeader("content-type", "application/json");
        if (req.url === "/auth/login") {
            let body = ""; for await (const chunk of req) body += chunk;
            const login = JSON.parse(body);
            if (login.senha !== "teste") { res.statusCode = 401; return res.end('{}'); }
            return res.end(JSON.stringify({ token: login.email.startsWith("a") ? "token-a" : "token-b" }));
        }
        if (req.url === "/api/contexto") {
            const a = req.headers.authorization === "Bearer token-a";
            return res.end(JSON.stringify({ usuarioId: a ? 1 : 2, empresaId: a ? 10 : 20, departamentoId: null, perfil: "ADMIN" }));
        }
        if (req.url.startsWith("/indicadores/maturidade/resultados")) {
            recebidos.push(req.headers.authorization);
            if (indisponivel) { res.statusCode = 503; return res.end('{}'); }
            return res.end(JSON.stringify({ naturezaDados: "SINTETICA", inicio: "2026-01-01", fim: "2026-07-31",
                scoreGlobal: null, scoreParcial: 80, coberturaPesos: 0.15, versaoPesos: "1.0", versaoCalculo: "proposta-0.1", versaoParametros: "teste",
                fatores: [{ codigo: "aiLiteracy", nota: 80, estado: "PARCIAL", amostra: 2, coberturaTecnica: 1, elegivelGlobal: false, motivo: "Amostra insuficiente" }], avisos: ["Proposta em validação"] }));
        }
        res.statusCode = 404; res.end('{}');
    });
    await new Promise(resolve => backend.listen(0, "127.0.0.1", resolve));
    process.env.API_URL = `http://127.0.0.1:${backend.address().port}`;
    const app = require("../server");
    const frontend = app.listen(0, "127.0.0.1");
    await new Promise(resolve => frontend.once("listening", resolve));
    t.after(async () => {
        frontend.closeAllConnections(); backend.closeAllConnections();
        await Promise.all([new Promise(resolve => frontend.close(resolve)),new Promise(resolve => backend.close(resolve))]);
    });
    const url = `http://127.0.0.1:${frontend.address().port}`;
    const negado = await fetch(`${url}/maturidade`, { redirect: "manual" });
    assert.equal(negado.status, 302);
    async function entrar(email) {
        const response = await fetch(`${url}/acesso`, { method: "POST", body: new URLSearchParams({ email, senha: "teste" }), redirect: "manual" });
        assert.equal(response.status, 302);
        return response.headers.get("set-cookie").split(";")[0];
    }
    const [a, b] = await Promise.all([entrar("a@teste.invalid"), entrar("b@teste.invalid")]);
    const caminho = "/maturidade?inicio=2026-01-01&fim=2026-07-31&fonte=SINTETICA&snapshot=" + "A".repeat(64);
    const paginas = await Promise.all([fetch(url + caminho, { headers: { cookie: a } }), fetch(url + caminho, { headers: { cookie: b } })]);
    for (const pagina of paginas) {
        assert.equal(pagina.status, 200); const html = await pagina.text();
        assert.match(html, /Score parcial exploratório/); assert.match(html, /15\.00/); assert.match(html, /Indisponível/);
        assert.doesNotMatch(html, /token-a|token-b/);
    }
    assert.deepEqual(recebidos.sort(), ["Bearer token-a", "Bearer token-b"]);
    indisponivel = true;
    const falha = await (await fetch(url + caminho, { headers: { cookie: a } })).text();
    assert.match(falha, /Nenhuma nota foi estimada/); assert.doesNotMatch(falha, /73[,.]25/);
    const loginInvalido = await fetch(`${url}/acesso`, { method: "POST", body: new URLSearchParams({ email: "a@teste.invalid", senha: "errada" }) });
    assert.equal(loginInvalido.status, 401);
});
