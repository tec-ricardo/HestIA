const {test}=require('node:test'),assert=require('node:assert/strict');
function app(){const rotas={},locals={};require('../demonstracao-base').registrarDemonstracaoBase({locals,get:(p,h)=>rotas[p]=h});return {rotas,locals};}
test('AI Literacy historica usa notas da origem e nao tentativas operacionais ausentes',()=>{
    const r=app().locals.resumoBaseOriginal();assert.equal(r.literacy.nota,76.87);assert.equal(r.literacy.denominador,38);
    assert.equal(r.governanca.nota,97.21);assert(!Object.hasOwn(r,'conformidade'));
});
test('consulta historica padrao integra AI Literacy sem fabricar uso responsavel',()=>{
    const {rotas}=app();let renderizou=false;
    const res={set:()=>res,render:(view,c)=>{assert.equal(view,'demonstracao-base');assert.equal(c.recorte.fatores.find(f=>f.codigo==='aiLiteracy').nota,76.87);
        assert.equal(c.recorte.fatores.find(f=>f.codigo==='usoResponsavel').nota,null);renderizou=true;}};
    rotas['/maturidade/resultados']({session:{perfil:'gestor'},query:{fonte:'SINTETICA',inicio:'2025-06-01',fim:'2026-08-10'}},res,()=>assert.fail('Consulta caiu na fonte operacional'));
    assert(renderizou);
});
test('filtros operacionais e periodos diferentes preservam o contrato original',()=>{
    const {rotas}=app();let chamadas=0;
    for(const query of [{fonte:'OPERACIONAL'},{fonte:'SINTETICA',inicio:'2030-01-01',fim:'2030-02-01'},
        {fonte:'SINTETICA',inicio:'2025-06-01',fim:'2026-08-10',departamentoId:'1'}])
        rotas['/maturidade/resultados']({session:{perfil:'gestor'},query},{},()=>chamadas++);
    assert.equal(chamadas,3);
});
