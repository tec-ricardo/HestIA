const {test}=require('node:test');
const assert=require('node:assert/strict');
const dados=require('../dados/base-original-c01.json');
const {mediana,notaProdutividade}=require('../../../scripts/calcular-base-original.cjs');
test('agregados mensais e departamentais reconciliam sem misturar empresas',()=>{
    for(const campo of ['eventosUso','requisicoes','tokensEntrada','tokensSaida','empregadosNaBase','eventosSeguranca','treinamentosConcluidos'])
        assert.equal(dados.departamentos.reduce((s,d)=>s+d[campo],0),dados.total[campo],campo);
    assert.equal(dados.total.evolucao.reduce((s,m)=>s+m.eventos,0),dados.total.eventosUso);
    assert.equal(dados.total.evolucao.reduce((s,m)=>s+m.requisicoes,0),dados.total.requisicoes);
    assert.equal(dados.total.evolucao.reduce((s,m)=>s+m.tokens,0),dados.total.tokensEntrada+dados.total.tokensSaida);
    assert(dados.departamentos.every(d=>d.company_id===dados.empresa.company_id));
});
test('ausencia nao vira zero ou score global; hipotese nao altera moeda original',()=>{
    for(const r of [dados.total,...dados.departamentos]) {
        assert.equal(r.scoreGlobal,null);
        for(const codigo of ['usoResponsavel','seguranca','finOps'])assert.equal(r.fatores.find(f=>f.codigo===codigo).nota,null);
        assert(r.fatores.every(f=>f.nota==null||f.nota>=0&&f.nota<=100));
    }
    assert.equal(dados.total.simulacaoFinOps.natureza,'SIMULACAO_COM_PREMISSAS');
    assert.equal(dados.total.simulacaoFinOps.custoConvertido,
        Math.round((dados.total.custosPorMoeda.BRL+dados.total.custosPorMoeda.USD*dados.premissas.cambioBRLPorUSD)*100)/100);
});
test('duracoes preservam mediana por tipo e nao usam volume como produtividade',()=>{
    assert.equal(mediana([1,2,90,100]),46);assert.equal(mediana([]),null);
    assert.equal(notaProdutividade([{issue_type:'Bug',dias:10},{issue_type:'Feature',dias:20}],5),37.5);
    assert.equal(notaProdutividade([{issue_type:'Bug',dias:0}],5),100);
});
test('telas calculadas exigem perfil gerencial e nao consultam API operacional',async()=>{
    const rotas={};require('../demonstracao-base').registrarDemonstracaoBase({get:(p,h)=>rotas[p]=h});
    const negado={sendStatus:s=>assert.equal(s,403)};
    rotas['/maturidade/demonstracao']({session:{perfil:'colaborador'},query:{}},negado);
    const res={set:()=>res,render:(view,c)=>{assert.equal(view,'demonstracao-base');assert.equal(c.recorte.department_id,'C01-D01');},status:s=>{assert.equal(s,400);return res;},send:()=>{}};
    rotas['/maturidade/demonstracao']({session:{perfil:'gestor'},query:{departamento:'C01-D01'}},res);
    rotas['/maturidade/demonstracao']({session:{perfil:'gestor'},query:{departamento:'C02-D01'}},res);
});
test('reconciliacao independente com SQLite original',{skip:!process.env.HESTIA_ORIGINAL_DB},()=>{
    const {DatabaseSync}=require('node:sqlite');const fs=require('node:fs'),crypto=require('node:crypto');
    const origem=process.env.HESTIA_ORIGINAL_DB;
    assert.equal(crypto.createHash('sha256').update(fs.readFileSync(origem)).digest('hex'),dados.origem.sha256);
    const db=new DatabaseSync(origem,{readOnly:true});
    try {
        const r=db.prepare('SELECT count(*) eventos,sum(request_count) requisicoes,sum(input_tokens) entrada,sum(output_tokens) saida FROM ai_usage_logs WHERE company_id=? AND substr(timestamp,1,10) BETWEEN ? AND ?').get('C01',dados.periodo.inicio,dados.periodo.fim);
        assert.equal(r.eventos,dados.total.eventosUso);assert.equal(r.requisicoes,dados.total.requisicoes);
        assert.equal(r.entrada,dados.total.tokensEntrada);assert.equal(r.saida,dados.total.tokensSaida);
        assert.equal(dados.origem.tabelas.reduce((s,t)=>s+t.linhas,0),311556);
    } finally{db.close();}
});
