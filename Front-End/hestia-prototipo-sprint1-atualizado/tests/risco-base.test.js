const {test}=require('node:test'),assert=require('node:assert/strict');
function rota(){const rotas={};require('../demonstracao-base').registrarDemonstracaoBase({get:(p,h)=>rotas[p]=h});return rotas['/mapa-risco'];}
test('historico sintetico nao substitui a consulta operacional',()=>{
    let seguiu=false;rota()({query:{fonte:'OPERACIONAL'}},{},()=>seguiu=true);assert(seguiu);
});
test('historico de risco exige perfil gerencial e rejeita mes ausente',()=>{
    rota()({query:{fonte:'SINTETICA'},session:{perfil:'colaborador'}},{sendStatus:s=>assert.equal(s,403)});
    const res={status:s=>{assert.equal(s,400);return res;},send:()=>{}};
    rota()({query:{fonte:'SINTETICA',mes:'2030-01'},session:{perfil:'gestor'}},res);
});
test('historico mostra os departamentos sem fabricar classificacao de risco',()=>{
    let exibiu=false;
    const res={set:()=>res,render:(view,c)=>{assert.equal(view,'risco-base');assert.equal(c.linhas.length,11);
        assert.equal(c.linhas.reduce((s,d)=>s+d.eventos,0),99604);assert(c.linhas.every(d=>d.risco===undefined));exibiu=true;}};
    rota()({query:{fonte:'SINTETICA'},session:{perfil:'gestor',email:''}},res);assert(exibiu);
});
