const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const { contextoApi } = require('../api-contexto');
const montar = require('../configuracao-inicial');
async function ambiente(t, api, perfil = 'administrador') {
 const app = express(); app.use(express.urlencoded({extended:false}));
 app.set('view engine','ejs'); app.set('views',require('node:path').join(__dirname,'../views'));
 const session = {perfil,email:'admin@example.test', setupCsrf:'segredo'};
 app.use((req,res,next) => {req.session=session; contextoApi.run({empresaId:10},next);});
 montar(app,api); const server=app.listen(0,'127.0.0.1');
 await new Promise(resolve => server.once('listening',resolve));
 t.after(() => new Promise(resolve => server.close(resolve)));
 return `http://127.0.0.1:${server.address().port}`;
}
test('retomada apresenta valores persistidos sem gravar cadastros',async t => {
 let gravacoes=0;
 const base=await ambiente(t,{get:async url => {assert.match(url,/empresas\/10/);return {data:{etapas:[],configuracao:{diasRetencaoAuditoria:42,limiteCustoMensal:125,registrarUsosAutomaticamente:false,bloquearFerramentasNaoAprovadas:true}}};},put:async()=>gravacoes++});
 const resposta=await fetch(base+'/configuracao-inicial'); assert.equal(resposta.status,200);
 const html=await resposta.text();assert.match(html,/value="42"/);assert.match(html,/value="125"/);assert.equal(gravacoes,0);
});
test('salvamento usa empresa autenticada e rejeita repetição sem proteção de formulário',async t => {
 let gravacoes=0;
 const base=await ambiente(t,{put:async(url,dto)=>{assert.match(url,/empresas\/10/);assert.equal(dto.diasRetencaoAuditoria,42);gravacoes++;}});
 const enviar=csrf=>fetch(base+'/configuracao-inicial',{method:'POST',redirect:'manual',headers:{'content-type':'application/x-www-form-urlencoded'},body:new URLSearchParams({csrf,empresaId:'999',diasRetencaoAuditoria:'42',limiteCustoMensal:''})});
 assert.equal((await enviar('errado')).status,403);assert.equal(gravacoes,0);
 assert.equal((await enviar('segredo')).status,302);assert.equal(gravacoes,1);
});
test('falha de consulta não vira lista de pendências fictícias',async t=>{
 const base=await ambiente(t,{get:async()=>{throw Error('offline');}});
 const resposta=await fetch(base+'/configuracao-inicial');assert.equal(resposta.status,503);
 assert.match(await resposta.text(),/Não foi possível consultar/);
});
test('colaborador não acessa configuração',async t=>{
 const base=await ambiente(t,{get:()=>{throw Error('não chamar');}},'colaborador');
 assert.equal((await fetch(base+'/configuracao-inicial')).status,403);
});
