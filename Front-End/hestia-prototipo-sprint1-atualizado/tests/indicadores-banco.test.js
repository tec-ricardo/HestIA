const {test}=require('node:test');
const assert=require('node:assert/strict');
const http=require('node:http');
const {periodoDemoBanco}=require('../periodo-demo-banco');

test('período da demonstração é o último mês fechado, inclusive na virada do ano',()=>{
 assert.deepEqual(periodoDemoBanco(new Date(2026,0,2)),{inicio:'2025-12-01',fim:'2025-12-31'});
 assert.deepEqual(periodoDemoBanco(new Date(2026,10,18)),{inicio:'2026-10-01',fim:'2026-10-31'});
});

test('painel e mapa originais exibem os resultados da API DEMO sem cenário local',async t=>{
 const chamadas=[];let nota=82;
 const fatores=['aiLiteracy','governanca','usoResponsavel','produtividade','finOps','seguranca','colaboracao','desenvolvimentoCompetencias'].map(codigo=>({codigo,nota:75,elegivelGlobal:true,naturezaDados:'DEMO',fonte:'PostgreSQL',estado:'SUFICIENTE'}));
 const backend=http.createServer(async(req,res)=>{
  chamadas.push(req.url);res.setHeader('content-type','application/json');
  if(req.url==='/auth/login')return res.end('{"token":"demo-token-teste"}');
  if(req.url==='/api/contexto')return res.end('{"usuarioId":1,"empresaId":1,"perfil":"ADMIN"}');
  if(req.url.startsWith('/indicadores/maturidade/resultados?')){
   const u=new URL(req.url,'http://teste');assert.equal(u.searchParams.get('fonte'),'DEMO');
   assert.equal(req.headers.authorization,'Bearer demo-token-teste');
   return res.end(JSON.stringify({naturezaDados:'DEMO',scoreGlobal:nota,scoreParcial:nota,coberturaPesos:1,dimensoesAusentes:[],fatores,avisos:[],versaoCalculo:'proposta-0.1'}));
  }
  if(req.url.startsWith('/indicadores/maturidade/ranking-departamental?')){
   assert.equal(new URL(req.url,'http://teste').searchParams.get('fonte'),'DEMO');
   return res.end(JSON.stringify({fonte:'DEMO',departamentos:[{departamento:'Tecnologia',posicao:1,
    apuracao:{scoreGlobal:nota,coberturaPesos:1,dimensoesAusentes:[],fatores},risco:{estado:'CLASSIFICADO',classificacao:'BAIXO',indice:20}}]}));
  }
  res.statusCode=404;res.end('{}');
 });
 await new Promise(r=>backend.listen(0,'127.0.0.1',r));
 process.env.API_URL=`http://127.0.0.1:${backend.address().port}`;
 const frontend=require('../server').listen(0,'127.0.0.1');await new Promise(r=>frontend.once('listening',r));
 t.after(async()=>{frontend.closeAllConnections();backend.closeAllConnections();await Promise.all([new Promise(r=>frontend.close(r)),new Promise(r=>backend.close(r))]);});
 const url=`http://127.0.0.1:${frontend.address().port}`;
 const login=await fetch(url+'/acesso',{method:'POST',body:new URLSearchParams({email:'admin.demo@hestia.local',senha:'teste'}),redirect:'manual'});
 const cookie=login.headers.get('set-cookie').split(';')[0];
 const painel=await fetch(url+'/maturidade',{headers:{cookie}});assert.equal(painel.status,200);const html=await painel.text();
 assert.match(html,/AI Maturity Score completo/);assert.match(html,/82\.00/);assert.match(html,/Fonte DEMO — PostgreSQL/);
 assert.doesNotMatch(html,/Score do cenário fictício|Evidências insuficientes/);
 nota=83.5;
 const mudou=await(await fetch(url+'/maturidade',{headers:{cookie}})).text();assert.match(mudou,/83\.50/);
 const mapa=await fetch(url+'/mapa-risco?fonte=DEMO',{headers:{cookie}});assert.equal(mapa.status,200);const risco=await mapa.text();
 assert.match(risco,/BAIXO/);assert.match(risco,/Tecnologia/);assert.match(risco,/Score global/);
 assert.doesNotMatch(risco,/Evidências insuficientes|Risco do cenário fictício/);
 assert(!chamadas.some(u=>u.includes('fonte=SINTETICA')||u.includes('cenario=demo-v1')));
});
