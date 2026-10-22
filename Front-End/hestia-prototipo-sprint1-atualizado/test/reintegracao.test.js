const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { carregarConfianca } = require('../confianca-maturidade');

test('consulta descarta IDs de empresa e usuário enviados pelo navegador', async () => {
    const api = { get: async (_, config) => {
        assert.deepEqual(config.params, {inicio:'2026-10-01',fim:'2026-10-22',visao:'PESSOAL'});
        return {data:{periodo:{},indicadores:[],versaoConfianca:'CONFIANCA-1.0'}};
    }};
    await carregarConfianca(api,{apiToken:'privado'}, {
        inicio:'2026-10-01',fim:'2026-10-22',visao:'PESSOAL',empresaId:999,usuarioId:888
    });
});

test('HTTP: demonstração identificada, sessão expirada e logout com API indisponível', async () => {
    let logoutChamado = false;
    const backend = http.createServer((req,res) => {
        res.setHeader('Content-Type','application/json');
        if(req.url==='/auth/login') return res.end(JSON.stringify({token:'secreto',perfil:'ADMIN'}));
        if(req.url==='/api/contexto') return res.end(JSON.stringify({usuarioId:1,empresaId:10,perfil:'ADMIN'}));
        if(req.url==='/auth/logout') { logoutChamado=true; res.writeHead(503); return res.end('{}'); }
        res.writeHead(401); res.end('{}');
    });
    await new Promise(resolve=>backend.listen(0,'127.0.0.1',resolve));
    process.env.API_URL=`http://127.0.0.1:${backend.address().port}`;
    const frontend = require('../server').listen(0,'127.0.0.1');
    await new Promise(resolve=>frontend.once('listening',resolve));
    const base=`http://127.0.0.1:${frontend.address().port}`;
    const login=async email=>{
        const resposta=await fetch(base+'/acesso',{method:'POST',redirect:'manual',
            headers:{'Content-Type':'application/x-www-form-urlencoded'},
            body:new URLSearchParams({email,senha:'teste'})});
        assert.equal(resposta.status,302);
        return resposta.headers.get('set-cookie').split(';')[0];
    };
    try {
        const demo=await login('pessoa@colaborador');
        const tela=await fetch(base+'/maturidade',{headers:{Cookie:demo}});
        const html=await tela.text();
        assert.match(html,/Acesso demonstrativo/);
        assert.doesNotMatch(html,/11150|11\.150|Confiável/);
        const demonstracao=await fetch(base+'/maturidade/demonstracao',{headers:{Cookie:demo}});
        assert.equal(demonstracao.status,200);
        assert.match(await demonstracao.text(),/Demonstração com dados fictícios/);
        const cookie=await login('admin@example.test');
        const expirada=await fetch(base+'/maturidade',{headers:{Cookie:cookie}});
        assert.equal(expirada.status,401);
        assert.match(await expirada.text(),/sessão da API expirou/);
        // Novo login para comprovar que logout tenta revogar o token e sempre encerra localmente.
        const novaSessao=await login('admin@example.test');
        const logout=await fetch(base+'/sair',{headers:{Cookie:novaSessao},redirect:'manual'});
        assert.equal(logout.status,302);
        assert.equal(logoutChamado,true);
        const protegida=await fetch(base+'/maturidade',{headers:{Cookie:novaSessao},redirect:'manual'});
        assert.equal(protegida.status,302);
        assert.match(protegida.headers.get('location'),/^\/acesso/);
    } finally {
        await new Promise(resolve=>frontend.close(resolve));
        await new Promise(resolve=>backend.close(resolve));
    }
});
