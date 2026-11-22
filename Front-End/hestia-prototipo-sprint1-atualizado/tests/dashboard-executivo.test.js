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

test('a consulta gerencial encaminha somente período e departamento selecionados',async()=>{
 const {carregarPainel}=require('../gestao-colaboradores');
 let args;
 const api={get:async(...a)=>{args=a;return {data:base}}};
 const painel=await carregarPainel(api,0,{dias:7,departamentoId:1,empresaId:999});
 assert.deepEqual(args,['/gestao/colaboradores',{params:{dias:7,departamentoId:1}}]);
 assert.equal(painel.colaboradores.length,2);
});

test('conta demonstrativa abre no cenário DEMO e pode alternar para operacional',()=>{
 const {fonteMaturidade,consultaDaMaturidade}=require('../dashboard-executivo');
 assert.equal(fonteMaturidade({},true),'DEMO');
 assert.equal(fonteMaturidade({fonteMaturidade:'OPERACIONAL'},true),'OPERACIONAL');
 assert.equal(fonteMaturidade({fonteMaturidade:'DEMO'},false),'OPERACIONAL');
 const painel={...base,departamentoId:1};
 assert.deepEqual(consultaDaMaturidade(painel,'DEMO',new Date(2026,10,22)),{inicio:'2026-10-01',fim:'2026-10-31',fonte:'DEMO',departamentoId:1});
 assert.deepEqual(consultaDaMaturidade(painel,'OPERACIONAL'),{inicio:base.inicio,fim:base.fim,fonte:'OPERACIONAL',departamentoId:1});
});
test('exibe as oito notas demonstrativas e mantém os KPIs da operação separados',async()=>{
 const ejs=require('ejs'),path=require('node:path');
 const fatores=['aiLiteracy','governanca','usoResponsavel','produtividade','finOps','seguranca','colaboracao','desenvolvimentoCompetencias'].map((codigo,i)=>({codigo,nota:80+i,elegivelGlobal:true}));
 const consulta={fonte:'DEMO',inicio:'2026-10-01',fim:'2026-10-31'};
 const d=montarDashboard(base,{scoreGlobal:83.5,coberturaPesos:1,naturezaDados:'DEMO',fatores,inicio:consulta.inicio,fim:consulta.fim},{dias:7},consulta);
 const html=await ejs.renderFile(path.join(__dirname,'../views/gestor-dashboard.ejs'),{perfil:'gestor',email:'gestor.demo@hestia.local',active:'dashboard',dashboard:d,erro:null,demonstracao:true});
 assert.match(html,/name="fonteMaturidade"/);
 assert.match(html,/value="DEMO" selected/);
 assert.match(html,/83,50/);
 for(let i=80;i<88;i++)assert.match(html,new RegExp(i+',00'));
 assert.match(html,/DEMONSTRAÇÃO NO BANCO/);
 assert.match(html,/01\/10\/2026/);
 assert.match(html,/KPIs e alertas abaixo permanecem operacionais/);
 assert.match(html,/50%/);
 assert.match(html,/75%/);
});
