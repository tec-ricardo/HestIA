const {test}=require('node:test');const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const tela=fs.readFileSync(path.join(__dirname,'../views/assistente-rag.ejs'),'utf8');
function montar(fetch){const c={fetch,crypto:{randomUUID:()=>Math.random().toString()},proximoModo:'CONSULTAR'};vm.createContext(c);vm.runInContext(tela.slice(tela.indexOf('      function mensagemDoServidor'),tela.indexOf("      form.addEventListener('submit'"))+';globalThis.answer=answerFor;globalThis.carregar=carregarConversa;',c);return c;}
test('continuação usa a mesma conversa e uma conversa nova recebe outro ID',async()=>{
 const chamadas=[];let id=40;
 const c=montar(async(url,opcoes)=>{const body=opcoes?.body?JSON.parse(opcoes.body):null;chamadas.push({url,body});return {ok:true,json:async()=>url==='/api/rag/conversas'?{id:++id}:{id:1,estado:'RECUPERADA',resposta:'Política',fontes:[]}};});
 const uma={title:'Uma'},outra={title:'Outra'};await c.answer('Pergunta',uma);await c.answer('E isso?',uma);await c.answer('Pergunta',outra);
 assert.deepEqual(chamadas.filter(x=>x.url==='/api/rag/perguntas').map(x=>x.body.conversaId),[41,41,42]);assert.equal(chamadas.filter(x=>x.url==='/api/rag/conversas').length,2);
});
test('retomada carrega só o histórico do ID selecionado e preserva o pedido de esclarecimento',async()=>{
 const urls=[];const c=montar(async url=>{urls.push(url);return {ok:true,json:async()=>[{id:7,pergunta:'E isso?',estado:'PRECISA_ESCLARECIMENTO',resposta:'Qual ferramenta?',fontes:[]}]};});
 const conversa={servidorId:41,messages:[]};await c.carregar(conversa);await c.carregar(conversa);
 assert.deepEqual(urls,['/api/rag/conversas/41/historico']);assert.equal(conversa.messages[1].text,'Qual ferramenta?');assert.equal(conversa.messages[1].conversaId,41);
});

test('BFF preserva ID da conversa e valida IDs antes de chamar a API',async t=>{
 const express=require('express'),montar=require('../integracoes-sprint');const chamadas=[];
 const api={post:async(url,body)=>{chamadas.push({url,body});return {data:url.endsWith('/conversas')?{id:41}:body};},get:async url=>{chamadas.push({url});return {data:[]};}};
 const app=express();app.use(express.json());app.use((req,res,next)=>{req.session={perfil:'colaborador',usuarioId:10};next();});montar(app,api);
 const servidor=app.listen(0,'127.0.0.1');await new Promise(r=>servidor.once('listening',r));t.after(()=>new Promise(r=>servidor.close(r)));
 const base='http://127.0.0.1:'+servidor.address().port;
 let r=await fetch(base+'/api/rag/conversas',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({titulo:'Privada'})});assert.equal(r.status,201);
 r=await fetch(base+'/api/rag/perguntas',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({chave:'turno',pergunta:'E isso?',modo:'CONSULTAR',conversaId:41})});assert.equal(r.status,200);assert.equal((await r.json()).conversaId,41);
 r=await fetch(base+'/api/rag/conversas/41/historico');assert.equal(r.status,200);
 const antes=chamadas.length;
 r=await fetch(base+'/api/rag/perguntas',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({chave:'turno',pergunta:'E isso?',conversaId:'invalida'})});assert.equal(r.status,400);assert.equal(chamadas.length,antes);
 assert.equal(chamadas[2].url,'/api/rag/conversas/41/historico');
});
