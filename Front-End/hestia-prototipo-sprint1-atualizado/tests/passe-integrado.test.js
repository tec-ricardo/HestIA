const {test}=require('node:test'),assert=require('node:assert/strict');
const {sequenciaAtual,carregarPasse}=require('../passe-integrado');
const registros=[3,2,1].map(id=>({id,usuarioId:7,dataHora:`2026-11-${String(id).padStart(2,'0')}T12:00:00`}));
test('sequencia usa ordem de uso e resultados efetivamente concluidos',()=>{
    const p=registros.map(r=>({registro_id:r.id,estado:'CONCLUIDA',resultado:r.id===1?'NAO_CONFORME':'CONFORME'}));
    assert.equal(sequenciaAtual(registros,p,7),2);
    p[0].resultado='REQUER_REVISAO';assert.equal(sequenciaAtual(registros,p,7),0);
    assert.equal(sequenciaAtual(registros,[],7),null);
    assert.equal(sequenciaAtual([],[],7),0);
    assert.throws(()=>sequenciaAtual(registros,p,8));
});
test('passe combina XP e resumo pessoal sem usar mock',async()=>{
    const api={get:async url=>({data:url.includes('/passe')?{usuarioId:7,xpTotal:445,missoes:[]}:
        url.includes('/resumo')?{avaliadas:2,conformes:1,nao_conformes:1,requerem_revisao:0,percentualConformidade:50}:
        url==='/registros-uso-ia'?registros:registros.map(r=>({registro_id:r.id,estado:'CONCLUIDA',resultado:'CONFORME'}))})};
    const p=await carregarPasse(api,7,(d,f)=>({...f,xpAtual:d.xpTotal}));
    assert.equal(p.xpAtual,445);assert.equal(p.taxaConformidade,50);assert.equal(p.totalUtilizacoesAvaliadas,2);
    assert.equal(p.totalUtilizacoesConformes,1);assert.equal(p.sequenciaConforme,3);
});
test('falha do passe nao vira XP zero; falha suplementar nao inventa contagens',async()=>{
    await assert.rejects(()=>carregarPasse({get:async()=>{throw Error('offline');}},7,()=>({})));
    const p=await carregarPasse({get:async url=>{if(!url.includes('/passe'))throw Error('offline');return {data:{usuarioId:7,xpTotal:40,missoes:[]}};}},7,
        d=>({xpAtual:d.xpTotal,taxaConformidade:null,totalUtilizacoesAvaliadas:null,totalUtilizacoesConformes:null}));
    assert.equal(p.xpAtual,40);assert.equal(p.taxaConformidade,null);assert.equal(p.sequenciaConforme,null);
});
