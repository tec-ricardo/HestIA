const {test}=require('node:test'),assert=require('node:assert/strict');
const {gerarDiagnostico,analisarColaborador}=require('../diagnostico-gerencial');
test('indicador válido crítico priorizado',()=>{const r=gerarDiagnostico({fatores:[{codigo:'colaboracao',nota:40,elegivelGlobal:true}]});assert.equal(r.prioridades[0].codigo,'colaboracao');});
test('ausência e amostra frágil não são desempenho baixo',()=>{const r=gerarDiagnostico({fatores:[{codigo:'governanca',nota:20,elegivelGlobal:false}]});assert.equal(r.prioridades.length,0);assert.equal(r.indicadores.find(f=>f.codigo==='governanca').status,'Evidência insuficiente');assert.equal(gerarDiagnostico(null).indicadores.length,8);});
test('alerta de consumo só com referência positiva',()=>{const c={aiLiteracy:90,tokens:150000,nao_conformes:0,revisoes:0};assert.equal(analisarColaborador(c,0).alertas.length,0);assert.equal(analisarColaborador(c,100000).alertas[0].dimensao,'FinOps');});
test('ausência de fonte não é violação',()=>{const c=analisarColaborador({aiLiteracy:90,semFonte:1},0);assert.equal(c.alertas[0].dimensao,'Base de conhecimento');});
