const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const path = require('node:path');
const vm = require('node:vm');
const ejs = require('ejs');

test('a mesma sessão atende confiabilidade, resultados e registros; login recusado elimina acesso anterior', async t => {
    const chamadas = [];
    const backend = http.createServer(async (req, res) => {
        res.setHeader('Content-Type', 'application/json');
        chamadas.push({ url: req.url, token: req.headers.authorization });
        if (req.url === '/auth/login') {
            let body = ''; for await (const chunk of req) body += chunk;
            const login = JSON.parse(body);
            assert.equal(req.headers.authorization, undefined);
            if (login.senha === 'errada') { res.writeHead(401); return res.end('{}'); }
            return res.end(JSON.stringify({ token: 'token-integracao' }));
        }
        assert.equal(req.headers.authorization, 'Bearer token-integracao');
        if (req.url === '/api/contexto') return res.end(JSON.stringify({ usuarioId: 17, empresaId: 23, perfil: 'ADMIN' }));
        if (req.url.startsWith('/indicadores/confiabilidade')) return res.end(JSON.stringify({
            periodo: { inicio: '2026-01-01', fim: '2026-01-31' }, indicadores: [], versaoConfianca: 'CONFIANCA-1.0',
            versaoComposicao: '1.0', scoreGlobal: null, scoreParcial: null, coberturaPesos: 0, limitacoes: []
        }));
        if (req.url.startsWith('/indicadores/maturidade/resultados')) return res.end(JSON.stringify({
            naturezaDados: 'OPERACIONAL', inicio: '2026-01-01', fim: '2026-01-31', scoreGlobal: null,
            scoreParcial: 0, coberturaPesos: 0.15, fatores: [], versaoPesos: '1.0', versaoCalculo: 'proposta-0.1', versaoParametros: 'teste'
        }));
        if (req.url === '/registros-uso-ia') {
            let body = ''; for await (const chunk of req) body += chunk;
            return res.end(body);
        }
        if (req.url.startsWith('/indicadores/maturidade/apuracoes')) { res.writeHead(401); return res.end('{}'); }
        res.writeHead(404); res.end('{}');
    });
    await new Promise(resolve => backend.listen(0, '127.0.0.1', resolve));
    process.env.API_URL = `http://127.0.0.1:${backend.address().port}`;
    const frontend = require('../server').listen(0, '127.0.0.1');
    await new Promise(resolve => frontend.once('listening', resolve));
    t.after(async () => {
        frontend.closeAllConnections(); backend.closeAllConnections();
        await Promise.all([new Promise(resolve => frontend.close(resolve)), new Promise(resolve => backend.close(resolve))]);
    });
    const base = `http://127.0.0.1:${frontend.address().port}`;
    const login = async (senha, cookie) => fetch(base + '/acesso', {
        method: 'POST', redirect: 'manual', headers: cookie ? { cookie } : {},
        body: new URLSearchParams({ email: 'pessoa@example.test', senha })
    });
    const entrada = await login('certa');
    const cookie = entrada.headers.get('set-cookie').split(';')[0];
    for (const rota of ['/maturidade', '/maturidade/resultados?inicio=2026-01-01&fim=2026-01-31&fonte=OPERACIONAL']) {
        const pagina = await fetch(base + rota, { headers: { cookie } });
        assert.equal(pagina.status, 200);
        assert.equal(pagina.headers.get('cache-control'), 'no-store');
        assert.doesNotMatch(await pagina.text(), /token-integracao/);
    }
    const registro = await fetch(base + '/api/registros', { method: 'POST', headers: { cookie, 'Content-Type': 'application/json' }, body: JSON.stringify({ usuarioId: 999, empresaId: 999, origem: 'PLATAFORMA', finalidade: 'Teste' }) });
    assert.equal(registro.status, 201);
    assert.deepEqual(await registro.json(), { usuarioId: 17, finalidade: 'Teste' });
    const recusado = await login('errada', cookie);
    assert.equal(recusado.status, 401);
    const bloqueado = await fetch(base + '/api/registros', { headers: { cookie }, redirect: 'manual' });
    assert.equal(bloqueado.status, 302);
    const nova = await login('certa');
    const novoCookie = nova.headers.get('set-cookie').split(';')[0];
    assert.equal((await fetch(base + '/api/maturidade/apuracoes', { headers: { cookie: novoCookie } })).status, 401);
    assert.equal((await fetch(base + '/maturidade', { headers: { cookie: novoCookie }, redirect: 'manual' })).status, 302);
    assert.ok(chamadas.some(c => c.url.startsWith('/indicadores/confiabilidade')));
    assert.ok(chamadas.some(c => c.url.startsWith('/indicadores/maturidade/resultados')));
});

test('certificação na tela depende de concessão, não da conclusão da leitura', async () => {
    const html = await ejs.renderFile(path.join(__dirname, '../views/microtreinamento.ejs'), { perfil: 'colaborador', email: 'u@example.test', active: 'microtreinamentos' });
    const lista = { children: [], textContent: '', replaceChildren() { this.children = []; }, append(c) { this.children.push(c); } };
    let concessoes = [];
    const contexto = vm.createContext({
        document: {
            getElementById: id => id === 'mt-cert-list' ? lista : null,
            createElement: () => ({ append() {}, addEventListener() {} })
        },
        fetch: async url => {
            assert.equal(url, '/api/certificacoes');
            return { ok: true, json: async () => ({ certificacoes: concessoes }) };
        }
    });
    for (const script of html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g)) {
        if (script[1].trim()) vm.runInContext(script[1], contexto);
    }
    await vm.runInContext('cursos = [{ id: 1, concluido: true }]; mostrarCertificacoes()', contexto);
    assert.equal(lista.children.length, 0);
    assert.match(lista.textContent, /Nenhuma certificação concedida/);
    concessoes = [{ id: 5, nome: 'Fundamentos', nivel: 'Inicial', versao: 1, concedidaEm: '2026-10-22' }];
    await vm.runInContext('mostrarCertificacoes()', contexto);
    assert.equal(lista.children.length, 1);
});
