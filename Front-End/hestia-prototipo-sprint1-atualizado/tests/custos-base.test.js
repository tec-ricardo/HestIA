const {test}=require('node:test');
const assert=require('node:assert/strict');
const {calcularPainel}=require('../custos-base');
const dados=require('../dados/base-original-c01.json');
test('custos preservam moedas e conciliam com os totais da origem',()=>{
    for(const moeda of ['BRL','USD']) {
        const p=calcularPainel(dados,{moeda});
        assert.equal(p.total,dados.total.custosPorMoeda[moeda]);
        assert.equal(p.orcamento,null);
        assert(Math.abs(p.departamentos.reduce((s,d)=>s+(d.custo||0),0)+p.semDepartamento-p.total)<.02);
        assert(Math.abs(p.evolucao.reduce((s,d)=>s+(d.custo||0),0)-p.total)<.02);
        assert(Math.abs(p.porFerramenta.reduce((s,d)=>s+d.custo,0)-p.total)<.02);
    }
});
test('simulacao usa cambio explicito e nao altera moeda original',()=>{
    const p=calcularPainel(dados,{moeda:'SIMULADO'});
    assert.equal(p.total,dados.total.simulacaoFinOps.custoConvertido);
    assert.equal(p.orcamento,dados.total.simulacaoFinOps.orcamento);
    assert.equal(p.eventos,99604);
    assert.equal(p.sucesso,94.96);
});
test('filtros de mes e departamento atingem faturas e denominadores',()=>{
    const p=calcularPainel(dados,{mes:'2025-06',departamento:'C01-D01',moeda:'USD'});
    const raw=dados.financeiro.custos.filter(r=>r.mes==='2025-06'&&r.departamentoId==='C01-D01'&&r.moeda==='USD');
    assert.equal(p.total,Math.round(raw.reduce((s,r)=>s+r.custo,0)*100)/100);
    assert.equal(p.departamentos.length,1);assert.equal(p.evolucao.length,1);
    assert.equal(p.eventos,dados.financeiro.uso.find(r=>r.mes==='2025-06'&&r.departamentoId==='C01-D01').eventos);
    assert.throws(()=>calcularPainel(dados,{departamento:'C02-D01'}));
    assert.throws(()=>calcularPainel(dados,{mes:'2030-01'}));
});
test('mes sem faturamento nao transforma ausencia em custo zero',()=>{
    const copia=structuredClone(dados);copia.financeiro.custos=[];
    const p=calcularPainel(copia,{moeda:'BRL'});
    assert.equal(p.total,null);
    assert(p.departamentos.every(d=>d.custo===null));
    assert(p.evolucao.every(d=>d.custo===null));
});
test('template usa dados calculados e preserva dashboard, graficos e filtros',async()=>{
    const ejs=require('ejs'),path=require('node:path'),painel=calcularPainel(dados,{});
    const html=await ejs.renderFile(path.join(__dirname,'../views/custos.ejs'),{
        perfil:'gestor',email:'',active:'custos',painel,
        dinheiro:n=>n==null?'Não disponível':String(n),numero:n=>n==null?'Não disponível':String(n),
        jsonSeguro:obj=>JSON.stringify(obj).replace(/</g,'\\u003c')});
    assert(html.includes('graficoCustos'));assert(html.includes('graficoEvolucao'));
    assert(html.includes('name="mes"'));assert(html.includes('name="moeda"'));
    assert(html.includes('1219396.34'));assert(!html.includes('const dadosDepartamentos = ['));
});

function consultarCustos(query={}) {const rotas={};require('../demonstracao-base').registrarDemonstracaoBase({locals:{},get:(p,h)=>rotas[p]=h});const res={codigo:200,corpo:null,set(){return this;},status(n){this.codigo=n;return this;},send(s){this.corpo=s;return this;},render(){assert.fail('Render inesperado');}};rotas['/custos/demonstracao']({session:{perfil:'gestor'},query},res);return res;}
test('custos retornam 400 para filtros invalidos',()=>{assert.equal(consultarCustos({moeda:'INVALIDA'}).codigo,400);});
test('origem indisponivel retorna 503 sem detalhes internos',()=>{const anterior=process.env.HESTIA_BASE_ORIGINAL_DB,caminho=require('node:path').join(__dirname,'origem-privada-inexistente.db');process.env.HESTIA_BASE_ORIGINAL_DB=caminho;try{const r=consultarCustos();assert.equal(r.codigo,503);assert(!r.corpo.includes(caminho));assert(!r.corpo.includes('ENOENT'));}finally{if(anterior===undefined)delete process.env.HESTIA_BASE_ORIGINAL_DB;else process.env.HESTIA_BASE_ORIGINAL_DB=anterior;}});
