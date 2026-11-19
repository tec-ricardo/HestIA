const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const ejs = require('ejs');
const { Script } = require('node:vm');
const dados = require('../dados/base-original-c01.json');

const contexto = { perfil:'gestor', email:'demo@example.test', active:'maturidade' };
const render = (nome, valores) => ejs.renderFile(path.join(__dirname, '../views', nome), { ...contexto, ...valores });

test('painel original recebe a base calculada e preserva notas ausentes no radar', async () => {
    const html = await render('demonstracao-base.ejs', {
        dados, recorte:dados.total, departamento:'', formatar:String
    });
    assert.match(html, /app-shell/);
    assert.match(html, /AI Maturity Score/);
    assert.match(html, /graficoMaturidade/);
    assert.match(html, /76.87/);
    assert.match(html, /Mapa de calor dos indicadores/);
    assert.match(html, /Engenharia de Software/);
    const json = html.match(/id="dados-radar"[^>]*>(.*?)<\/script>/s)[1];
    const series = JSON.parse(json);
    assert.equal(series.length, 8);
    assert.equal(series.find(s => s.nome === 'Segurança').nota, null);
    for (const script of html.matchAll(/<script>([\s\S]*?)<\/script>/g)) new Script(script[1]);
});

test('heatmap distingue nota zero, nota ausente e conteúdo escapado', async () => {
    const html = await render('heatmap-indicadores.ejs', { linhasHeatmap:[{
        nome:'<script>injetado</script>', fatores:[
            {codigo:'aiLiteracy',nota:0,elegivelGlobal:true},
            {codigo:'seguranca',nota:null}
        ]
    }] });
    assert.match(html, /hsl\(0, 46%, 86%\)/);
    assert.match(html, /hm-sem-dados/);
    assert.match(html, /&lt;script&gt;injetado/);
    assert.doesNotMatch(html, /<script>injetado/);
});

test('heatmap histórico mensal deixa indicadores sem apuração mensal ausentes', async () => {
    const html = await render('risco-base.ejs', {
        dados, linhas:[{nome:dados.departamentos[0].department_name,eventos:5,requisicoes:8,governanca:null,eventosSeguranca:null}],
        mes:dados.financeiro.meses[0], numero:n => n ?? 'Não disponível'
    });
    assert.match(html, /Indicador mensal ausente neste recorte/);
    assert.doesNotMatch(html, /style="background:hsl/);
});

test('mapa operacional usa os fatores retornados pela API e mantém a faixa de risco', async () => {
    const html = await render('mapa-risco.ejs', {
        inicio:'2026-11-01', fim:'2026-11-30', erro:null, formatar:String,
        resultado:{departamentos:[{departamento:'Dados',posicao:null,
            apuracao:{scoreGlobal:null,coberturaPesos:0.15,dimensoesAusentes:['seguranca'],
                fatores:[{codigo:'governanca',nota:80,elegivelGlobal:true}]},
            risco:{estado:'SEM_CLASSIFICACAO'}}]}
    });
    assert.match(html, /hsl\(96, 46%, 86%\)/);
    assert.match(html, /Sem classificação/);
});

test('FinOps mostra a estimativa da 570 e mantém o score indisponível', async () => {
    const recorte = JSON.parse(JSON.stringify(dados.total));
    recorte.simulacaoFinOps = {natureza:'SIMULACAO_COM_PREMISSAS', moedaOrcamento:'BRL',
        cambioBRLPorUSD:5, orcamento:6357.74, custoConvertido:7164.56+17.53*5,
        nota:100, limite:'Somente simulação.'};
    const antes = JSON.stringify(recorte);
    const html = await render('demonstracao-base.ejs', {dados,recorte,departamento:'',formatar:String});
    const series = JSON.parse(html.match(/id="dados-radar"[^>]*>(.*?)<\/script>/s)[1]);
    const finOps = series.find(s=>s.nome==='FinOps');
    assert.equal(finOps.simulacao,true);
    assert.equal(finOps.nota.toFixed(2),'85.93');
    assert.match(html,/Simulação com premissas/);
    assert.match(html,/Não compõe o score global ou parcial/);
    assert.match(html,/AI Maturity Score completo<\/span><strong>Indisponível/);
    assert.equal(JSON.stringify(recorte),antes);
    assert.equal(recorte.fatores.find(f=>f.codigo==='finOps').nota,null);
});

test('simulação de FinOps sem orçamento válido permanece ausente', async () => {
    const recorte = {...dados.total,simulacaoFinOps:{...dados.total.simulacaoFinOps,orcamento:0}};
    const html = await render('demonstracao-base.ejs',{dados,recorte,departamento:'',formatar:String});
    const series = JSON.parse(html.match(/id="dados-radar"[^>]*>(.*?)<\/script>/s)[1]);
    assert.equal(series.find(s=>s.nome==='FinOps').nota,null);
    assert.equal(series.find(s=>s.nome==='FinOps').simulacao,false);
});

test('heatmap identifica estimativa zero e exige permissão explícita para simulação', async () => {
    const linhasHeatmap=[{nome:'Demo',fatores:[{codigo:'finOps',nota:null}],
        simulacaoFinOps:{natureza:'SIMULACAO_COM_PREMISSAS',moedaOrcamento:'BRL',
            cambioBRLPorUSD:5,orcamento:100,custoConvertido:300}}];
    const html = await render('heatmap-indicadores.ejs',{linhasHeatmap,permitirSimulacaoHeatmap:true});
    assert.match(html,/hsl\(0, 46%, 86%\)/);
    assert.match(html,/<small>Sim\.<\/small>/);
    const operacional = await render('heatmap-indicadores.ejs',{linhasHeatmap});
    assert.doesNotMatch(operacional,/<small>Sim\.<\/small>/);
    assert.doesNotMatch(operacional,/style="background:hsl/);
});

test('fonte operacional não recebe estimativa da base sintética', async () => {
    const html=await render('painel-maturidade.ejs',{painel:{sintetica:false,
        resultado:{fatores:[{codigo:'finOps',nota:null}],scoreGlobal:null,scoreParcial:null},
        filtros:{fonte:'OPERACIONAL'},recorte:dados.total,departamentos:[],erro:null}});
    const series = JSON.parse(html.match(/id="dados-radar"[^>]*>(.*?)<\/script>/s)[1]);
    assert.equal(series.find(s=>s.nome==='FinOps').nota,null);
    assert.equal(series.find(s=>s.nome==='FinOps').simulacao,false);
    assert.match(html,/Esta consulta usa somente registros da empresa autenticada/);
});
