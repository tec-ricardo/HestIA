const {test}=require('node:test');
const assert=require('node:assert/strict');
const {montarDashboard,filtrosPainel,serieDias}=require('../dashboard-executivo');
const base={empresa:'Empresa A',departamento:'Empresa inteira',departamentoId:null,departamentosDisponiveis:[{id:1,nome:'Equipe A'}],
 inicio:'2026-11-01',fim:'2026-11-07',anterior:{usos:2},evolucao:[{dia:'2026-11-01',usos:5,tokens:90}],ferramentasUso:[{nome:'Assistente',usos:5}],finalidades:[{nome:'PESQUISA',usos:5}],
 colaboradores:[{id:1,nome:'Ana',departamento:'Equipe A',departamento_id:1,utilizacoes:5,tokens:90,usos_com_tokens:3,conformes:3,nao_conformes:1,revisoes:2,aiLiteracy:90,xp_periodo:40,treinamentos:1,reutilizacoes:2,alertas:[{dimensao:'Uso responsável',motivo:'1 não conforme',acao:'Consultar',link:'/ocorrencias'}]},
 {id:2,nome:'Bia',departamento:'Equipe A',departamento_id:1,utilizacoes:0,tokens:0,aiLiteracy:null,alertas:[]}]};
test('calcula KPIs operacionais com denominadores explícitos',()=>{
 const d=montarDashboard(base,null,{dias:7,departamentoId:null});
 assert.equal(d.adocao,50);assert.equal(d.conformidade,75);assert.equal(d.literacy,90);assert.equal(d.avaliados,1);
 assert.equal(d.tokens,90);assert.equal(d.reutilizacoes,2);assert.equal(d.atencao,1);assert.equal(d.variacao,150);
 assert.equal(d.departamentos[0].taxa,50);assert.equal(d.serie.length,7);assert.equal(d.serie[1].usos,0);
});
test('ausência não equivale a desempenho zero',()=>{
 const d=montarDashboard({...base,colaboradores:[],anterior:{usos:0}},null);
 assert.equal(d.adocao,null);assert.equal(d.conformidade,null);assert.equal(d.literacy,null);assert.equal(d.variacao,null);
 assert.equal(montarDashboard(null,null).disponivel,false);
});
test('filtros são limitados aos períodos previstos',()=>{
 assert.deepEqual(filtrosPainel({dias:'90',departamentoId:'4'}),{dias:90,departamentoId:4});
 assert.deepEqual(filtrosPainel({dias:'5',departamentoId:'-1'}),{dias:30,departamentoId:null});
});
test('template executivo renderiza os seis KPIs, gráficos e decisões',async()=>{
 const ejs=require('ejs'),path=require('node:path');
 const d=montarDashboard(base,{scoreGlobal:null,scoreParcial:78,coberturaPesos:.6,fatores:[],naturezaDados:'OPERACIONAL'},{dias:7,departamentoId:null});
 const html=await ejs.renderFile(path.join(__dirname,'../views/gestor-dashboard.ejs'),{perfil:'administrador',email:'teste@example.test',active:'dashboard',dashboard:d,erro:null,demonstracao:false});
 for(const titulo of ['Adoção de IA','Taxa de conformidade','AI Literacy médio','Tokens registrados','Reutilizações pontuadas','Colaboradores em atenção','Central de decisões','Assistente','Score parcial exploratório'])assert.ok(html.includes(titulo),titulo);
 assert.match(html,/<svg class="ex-line"/);assert.match(html,/Ana/);assert.match(html,/50%/);assert.match(html,/75%/);
 assert.doesNotMatch(html,/QSoft/);
 const indisponivel=await ejs.renderFile(path.join(__dirname,'../views/gestor-dashboard.ejs'),{perfil:'gestor',email:'teste@example.test',active:'dashboard',dashboard:montarDashboard(null,null,{dias:30}),erro:'Sem dados',demonstracao:false});
 assert.match(indisponivel,/Dados indisponíveis/);assert.doesNotMatch(indisponivel,/ex-kpi-grid/);
});
