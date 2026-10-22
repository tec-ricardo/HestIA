const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');

function lerJson(req) {
    return new Promise((resolve, reject) => {
        let corpo = '';
        req.setEncoding('utf8');
        req.on('data', trecho => corpo += trecho);
        req.on('end', () => {
            try { resolve(corpo ? JSON.parse(corpo) : {}); }
            catch (erro) { reject(erro); }
        });
        req.on('error', reject);
    });
}

test('SCRUM-389: consulta e trata ocorrências pela API no escopo da sessão', async () => {
    const chamadas = [];
    let tratamento = {id:42,avaliacaoId:71,departamentoId:3,responsavelId:null,
        criadoEm:'2026-10-22T10:30:00',status:'ABERTA',versaoRegistro:0,acaoCorretiva:null};
    const detalhe = () => ({
        tratamento,
        ocorrencia:{id:9,ferramentaNome:'<script>erro</script>',
            usuarioNome:'Pessoa de teste',modeloIa:'Modelo A',finalidade:'PESQUISA'},
        avaliacao:{status:'NAO_CONFORME',motivo:'Uso sem autorização',
            regraAplicada:'Ferramenta não autorizada',limitacoes:'Revisão humana necessária'},
        politicaAplicada:null
    });
    const backend = http.createServer(async (req, res) => {
        res.setHeader('Content-Type', 'application/json');
        if (req.url === '/auth/login') return res.end(JSON.stringify({token:'token-teste'}));
        if (req.url === '/api/contexto') {
            assert.equal(req.headers.authorization, 'Bearer token-teste');
            return res.end(JSON.stringify({usuarioId:7, empresaId:3, departamentoId:null, perfil:'ADMIN'}));
        }
        if (req.url === '/usuarios') {
            chamadas.push({metodo:req.method,url:req.url,token:req.headers.authorization});
            return res.end(JSON.stringify([
                {id:7,nome:'Gestora Teste',email:'gestora@example.test',ativo:true},
                {id:8,nome:'Inativa',email:'inativa@example.test',ativo:false}
            ]));
        }
        if (req.url.startsWith('/tratamentos-nao-conformidade')) {
            chamadas.push({metodo:req.method,url:req.url,token:req.headers.authorization});
            if (req.headers.authorization !== 'Bearer token-teste') {
                res.writeHead(401); return res.end('{}');
            }
            if (req.url === '/tratamentos-nao-conformidade/42' && req.method === 'PUT') {
                const corpo = await lerJson(req);
                assert.deepEqual(corpo, {
                    responsavelId:7,
                    acaoCorretiva:'Orientar a equipe',
                    status:'EM_TRATAMENTO',
                    versaoRegistro:0
                });
                tratamento = {...tratamento,...corpo,versaoRegistro:1};
                return res.end(JSON.stringify(detalhe()));
            }
            if (req.url === '/tratamentos-nao-conformidade/43' && req.method === 'PUT') {
                res.writeHead(409); return res.end(JSON.stringify({erro:'versão desatualizada'}));
            }
            if (req.url === '/tratamentos-nao-conformidade/42') return res.end(JSON.stringify(detalhe()));
            if (req.url === '/tratamentos-nao-conformidade/43') return res.end(JSON.stringify({
                ...detalhe(), tratamento:{...tratamento,id:43,versaoRegistro:2}
            }));
            if (req.url === '/tratamentos-nao-conformidade/99') {
                res.writeHead(404); return res.end('{}');
            }
            const url = new URL(req.url, 'http://localhost');
            if (url.searchParams.get('status') === 'RESOLVIDA') {
                return res.end(JSON.stringify({itens:[],pagina:0,tamanho:20,totalItens:0,totalPaginas:0}));
            }
            return res.end(JSON.stringify({
                itens:[tratamento],
                pagina:0,tamanho:20,totalItens:1,totalPaginas:1
            }));
        }
        res.writeHead(404); res.end('{}');
    });
    await new Promise(resolve => backend.listen(0,'127.0.0.1',resolve));
    process.env.API_URL = `http://127.0.0.1:${backend.address().port}`;
    const frontend = require('../server').listen(0,'127.0.0.1');
    await new Promise(resolve => frontend.once('listening',resolve));
    const base = `http://127.0.0.1:${frontend.address().port}`;
    try {
        const login = await fetch(base+'/acesso',{method:'POST',redirect:'manual',
            headers:{'Content-Type':'application/x-www-form-urlencoded'},
            body:new URLSearchParams({email:'admin@example.test',senha:'teste'})});
        assert.equal(login.status,302);
        const cookie = login.headers.get('set-cookie').split(';')[0];
        const consultar = rota => fetch(base+rota,{headers:{Cookie:cookie}});

        const lista = await consultar('/ocorrencias?status=ABERTA');
        assert.equal(lista.status,200);
        const htmlLista = await lista.text();
        assert.match(htmlLista, /#42/);
        assert.match(htmlLista, /href="\/ocorrencias\/42"/);
        assert.match(htmlLista, /1 tratamento\(s\)/);
        assert.doesNotMatch(htmlLista, /localStorage|Uso de ferramenta de IA não autorizada/);

        const detalheInicial = await consultar('/ocorrencias/42');
        assert.equal(detalheInicial.status,200);
        const htmlDetalhe = await detalheInicial.text();
        assert.match(htmlDetalhe, /Tratamento #42/);
        assert.match(htmlDetalhe, /Uso sem autorização/);
        assert.match(htmlDetalhe, /Pessoa de teste/);
        assert.match(htmlDetalhe, /Gestora Teste/);
        assert.match(htmlDetalhe, /name="versaoRegistro" value="0"/);
        assert.match(htmlDetalhe, /&lt;script&gt;erro&lt;\/script&gt;/);
        assert.doesNotMatch(htmlDetalhe, /<script>erro<\/script>/);

        const atualizar = await fetch(base+'/ocorrencias/42/tratamento',{
            method:'POST',redirect:'manual',
            headers:{Cookie:cookie,'Content-Type':'application/x-www-form-urlencoded'},
            body:new URLSearchParams({responsavelId:'7',acaoCorretiva:' Orientar a equipe ',
                status:'EM_TRATAMENTO',versaoRegistro:'0'})
        });
        assert.equal(atualizar.status,303);
        assert.equal(atualizar.headers.get('location'),'/ocorrencias/42?atualizada=1');
        const atualizado = await consultar('/ocorrencias/42?atualizada=1');
        assert.equal(atualizado.status,200);
        const htmlAtualizado = await atualizado.text();
        assert.match(htmlAtualizado, /Tratamento atualizado com sucesso/);
        assert.match(htmlAtualizado, /Em tratamento/);
        assert.match(htmlAtualizado, /Orientar a equipe/);
        assert.match(htmlAtualizado, /name="versaoRegistro" value="1"/);

        const invalido = await fetch(base+'/ocorrencias/42/tratamento',{
            method:'POST',headers:{Cookie:cookie,'Content-Type':'application/x-www-form-urlencoded'},
            body:new URLSearchParams({responsavelId:'',acaoCorretiva:'',status:'RESOLVIDA',versaoRegistro:'1'})
        });
        assert.equal(invalido.status,400);
        assert.match(await invalido.text(), /Confira os dados informados/);

        const conflito = await fetch(base+'/ocorrencias/43/tratamento',{
            method:'POST',headers:{Cookie:cookie,'Content-Type':'application/x-www-form-urlencoded'},
            body:new URLSearchParams({responsavelId:'7',acaoCorretiva:'Concluída',
                status:'RESOLVIDA',versaoRegistro:'0'})
        });
        assert.equal(conflito.status,409);
        const htmlConflito = await conflito.text();
        assert.match(htmlConflito, /A ocorrência foi alterada/);
        assert.match(htmlConflito, /name="versaoRegistro" value="2"/);

        const vazia = await consultar('/ocorrencias?status=RESOLVIDA');
        assert.equal(vazia.status,200);
        assert.match(await vazia.text(), /Nenhuma ocorrência encontrada/);
        const ausente = await consultar('/ocorrencias/99');
        assert.equal(ausente.status,404);
        assert.match(await ausente.text(), /não encontrada no seu escopo/);
        const idInvalido = await consultar('/ocorrencias/abc');
        assert.notEqual(idInvalido.status,200);

        assert(chamadas.every(chamada => chamada.token === 'Bearer token-teste'));
        assert(chamadas.some(chamada => chamada.metodo === 'PUT' &&
            chamada.url === '/tratamentos-nao-conformidade/42'));
        assert.match(chamadas[0].url, /status=ABERTA/);
    } finally {
        frontend.closeAllConnections();
        backend.closeAllConnections();
        await Promise.all([
            new Promise(resolve => frontend.close(resolve)),
            new Promise(resolve => backend.close(resolve))
        ]);
    }
});