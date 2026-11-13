const test=require('node:test');const assert=require('node:assert/strict');const express=require('express');
async function ambiente(t,modulo,api,perfil='administrador'){
 const app=express();app.use(express.urlencoded({extended:false}));app.set('view engine','ejs');app.set('views',require('node:path').join(__dirname,'../views'));
 const sessao={perfil,email:'teste@example.invalid',catalogoCsrf:'protecao',recuperacaoCsrf:'protecao'};
 app.use((req,res,next)=>{req.session=sessao;next();});require('../'+modulo)(app,api);
 const server=app.listen(0,'127.0.0.1');await new Promise(r=>server.once('listening',r));t.after(()=>new Promise(r=>server.close(r)));
 return {base:`http://127.0.0.1:${server.address().port}`,sessao};
}
test('curadoria bloqueia gestor e valida CSRF antes de gravar',async t=>{
 let chamadas=0;const api={post:async()=>{chamadas++;return{data:{id:1000}};}};
 const {base}=await ambiente(t,'catalogo-real',api,'gestor');assert.equal((await fetch(base+'/catalogo-real')).status,403);
 const admin=await ambiente(t,'catalogo-real',api);const r=await fetch(admin.base+'/api/catalogo-real',{method:'POST',headers:{'content-type':'application/json'},body:'{}'});assert.equal(r.status,403);assert.equal(chamadas,0);
});
test('detalhe da curadoria escapa conteudo e apresenta revisao preservada',async t=>{
 const item={id:1000,titulo:'<script>ataque</script>',estado:'RETIRADO',conteudo:'<img onerror=alert(1)>',questionario:'[]',evidencia_revisao:'Fonte conferida'};
 const {base}=await ambiente(t,'catalogo-real',{get:async()=>({data:item})});const r=await fetch(base+'/catalogo-real/1000');assert.equal(r.status,200);const html=await r.text();assert.match(html,/Fonte conferida/);assert.ok(!html.includes('<script>ataque'));
});
test('CSV e consulta encaminham mesmos filtros sem empresa injetada',async t=>{
 const chamadas=[];const {base}=await ambiente(t,'relatorios-uso',{get:async(url,op)=>{chamadas.push([url,op.params]);return{data:url.endsWith('/exportar')?'id,data\r\n':[]};}});
 const query='?inicio=2026-11-01&fim=2026-11-12&finalidade=PESQUISA&empresaId=999';
 assert.equal((await fetch(base+'/relatorios-uso'+query)).status,200);const csv=await fetch(base+'/relatorios-uso/exportar'+query);assert.equal(csv.status,200);assert.match(csv.headers.get('content-type'),/text\/csv/);assert.deepEqual(chamadas[0][1],chamadas[1][1]);assert.equal(chamadas[1][1].empresaId,undefined);
});
test('recuperacao retira token do endereco e envia somente no corpo protegido',async t=>{
 let enviada;const {base,sessao}=await ambiente(t,'recuperacao-senha',{post:async(url,body)=>{enviada={url,body};}});
 const link=await fetch(base+'/redefinir-senha?token=temporario',{redirect:'manual'});assert.equal(link.headers.get('location'),'/redefinir-senha');assert.equal(sessao.tokenRecuperacao,'temporario');
 const r=await fetch(base+'/redefinir-senha',{method:'POST',headers:{'content-type':'application/x-www-form-urlencoded'},body:new URLSearchParams({csrf:'protecao',senha:'NovaSenha123!'})});assert.equal(r.status,200);assert.equal(enviada.url,'/auth/recuperacao/redefinir');assert.equal(enviada.body.get('token'),'temporario');assert.equal(sessao.tokenRecuperacao,undefined);assert.equal(r.headers.get('cache-control'),'no-store');
});

test('maturidade exporta os mesmos filtros e preserva ausencia de dados',async t=>{
 const chamadas=[];const {base}=await ambiente(t,'relatorios-maturidade',{get:async(url,op)=>{chamadas.push([url,op.params]);return{data:url.endsWith('.csv')?'campo,valor\r\n':{scoreGlobal:null,avisos:['Dados insuficientes']}};}});
 const query='?tipo=resultados&inicio=2026-11-01&fim=2026-11-12&fonte=OPERACIONAL&empresaId=999';
 const r=await fetch(base+'/relatorios-maturidade'+query);assert.equal(r.status,200);assert.match(await r.text(),/Não disponível/);
 assert.equal((await fetch(base+'/relatorios-maturidade/exportar'+query)).status,200);
 assert.deepEqual(chamadas[0][1],chamadas[1][1]);assert.equal(chamadas[1][1].empresaId,undefined);
});

test('economia acumulada preserva valor indisponivel e recusa filtros nao suportados',async t=>{
 let chamadas=0;const {base}=await ambiente(t,'relatorios-economia',{get:async()=>{chamadas++;return{data:{chamadasEvitadas:5,tokensEconomizados:100,custoEstimadoEvitado:null,percentualReutilizacao:50,limitacao:'Preços não aprovados'}};}});
 const r=await fetch(base+'/relatorios-economia');assert.equal(r.status,200);assert.match(await r.text(),/Não disponível/);
 assert.equal((await fetch(base+'/relatorios-economia/exportar?inicio=2026-11-01')).status,400);assert.equal(chamadas,1);
});
