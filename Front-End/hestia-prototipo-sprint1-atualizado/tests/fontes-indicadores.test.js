const {test}=require('node:test'),assert=require('node:assert/strict');
function app(){const rotas={},locals={};require('../demonstracao-base').registrarDemonstracaoBase({locals,get:(p,h)=>rotas[p]=h});return {rotas,locals};}
test('AI Literacy historica usa notas da origem e nao tentativas operacionais ausentes',()=>{
    const r=app().locals.resumoBaseOriginal();assert.equal(r.literacy.nota,76.87);assert.equal(r.literacy.denominador,38);
    assert.equal(r.governanca.nota,97.21);assert(!Object.hasOwn(r,'conformidade'));
});
test('consulta histórica é explícita e o resultado autenticado não é interceptado',()=>{
 const {rotas}=app();assert.equal(rotas['/maturidade/resultados'],undefined);
 let renderizou=false;const res={set:()=>res,render:(view,c)=>{assert.equal(view,'demonstracao-base');assert.equal(c.recorte.fatores.find(f=>f.codigo==='aiLiteracy').nota,76.87);renderizou=true;}};
 rotas['/maturidade/demonstracao']({session:{perfil:'gestor'},query:{}},res);
 assert(renderizou);
});
