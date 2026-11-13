const {test}=require('node:test');const assert=require('node:assert/strict');const {verificarProntidao}=require('../prontidao');
const api={get:async()=>({data:{status:'UP'}})};
test('prontidão confere API e armazenamento da sessão sem expor segredos',async()=>{const r=await verificarProntidao(api,{get:(id,cb)=>cb(null,null)});assert.equal(r.status,'UP');assert.equal(r.dependencias.sessao,'UP');});
test('API fora do ar deixa a prontidão indisponível',async()=>{const r=await verificarProntidao({get:async()=>{throw Error('credencial interna');}},{get:(id,cb)=>cb(null,null)});assert.equal(r.status,'DOWN');assert.ok(!JSON.stringify(r).includes('credencial'));});
test('erro ou timeout do armazenamento de sessão não é sucesso',async()=>{assert.equal((await verificarProntidao(api,{get:(id,cb)=>cb(Error('sigilo'))})).status,'DOWN');assert.equal((await verificarProntidao(api,{get:()=>{}},10)).status,'DOWN');});
test('desenvolvimento explicita a sessão em memória',async()=>{const r=await verificarProntidao(api);assert.equal(r.status,'UP');assert.equal(r.dependencias.sessao,'MEMORIA_LOCAL');});
