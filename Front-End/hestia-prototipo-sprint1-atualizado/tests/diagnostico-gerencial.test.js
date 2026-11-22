const {test}=require('node:test'),assert=require('node:assert/strict');
const {gerarDiagnostico,analisarColaborador}=require('../diagnostico-gerencial');
test('indicador válido crítico priorizado',()=>{const r=gerarDiagnostico({fatores:[{codigo:'colaboracao',nota:40,elegivelGlobal:true}]});assert.equal(r.prioridades[0].codigo,'colaboracao');});
test('ausência e amostra frágil não são desempenho baixo',()=>{const r=gerarDiagnostico({fatores:[{codigo:'governanca',nota:20,elegivelGlobal:false}]});assert.equal(r.prioridades.length,0);assert.equal(r.indicadores.find(f=>f.codigo==='governanca').status,'Evidência insuficiente');assert.equal(gerarDiagnostico(null).indicadores.length,8);});
test('alerta de consumo só com referência positiva',()=>{const c={aiLiteracy:90,tokens:150000,nao_conformes:0,revisoes:0};assert.equal(analisarColaborador(c,0).alertas.length,0);assert.equal(analisarColaborador(c,100000).alertas[0].dimensao,'FinOps');});
test('ausência de fonte não é violação',()=>{const c=analisarColaborador({aiLiteracy:90,semFonte:1},0);assert.equal(c.alertas[0].dimensao,'Base de conhecimento');});

test('templates de acompanhamento e XP renderizam evidências e contexto',async()=>{
 const ejs=require('ejs'),path=require('node:path');
 const base={perfil:'gestor',email:'gestor@example.test',active:'gestao-colaboradores'};
 const painel={empresa:'Empresa A',departamento:'Equipe 1',inicio:'2026-11-01',fim:'2026-11-30',comAlertas:1,usos:1,tokens:20,limite:10,
 colaboradores:[{id:1,nome:'Ana',departamento:'Equipe 1',xp:25,xp_periodo:25,utilizacoes:1,usos_com_tokens:1,treinamentos:0,reutilizacoes:1,
 aiLiteracy:70,amostraLiteracy:1,avaliacoesQualificadas:0,taxaConformidade:100,totalAvaliado:1,conformes:1,revisoes:0,tokens:20,
 alertas:[{dimensao:'FinOps',motivo:'Consumo',acao:'Revisar',link:'/custos'}]}]};
 const html=await ejs.renderFile(path.join(__dirname,'../views/gestao-colaboradores.ejs'),{...base,painel,custos:false});
 assert.match(html,/Empresa A/);assert.match(html,/Ana/);assert.match(html,/FinOps/);assert.match(html,/ranking\/1\/xp/);
 const xp=await ejs.renderFile(path.join(__dirname,'../views/detalhamento-xp.ejs'),{...base,detalhe:{nome:'Ana',xpTotal:25,xpMovimentos:15,conciliado:false,itens:[{acao:'REUTILIZACAO_RESPOSTA',quantidade:1,xp:15}]}});
 assert.match(xp,/divergência entre saldo e movimentos/);
});
