const {test}=require('node:test');
const assert=require('node:assert/strict');
const path=require('node:path');
const ejs=require('ejs');
const {aplicarCenario,calcularUsoResponsavel,calcularSeguranca}=require('../cenario-maturidade-demo');
const dados=require('../dados/base-original-c01.json');
const fixture=require('../dados/cenario-maturidade-demo.json');
const contexto={perfil:'gestor',email:'demo@example.test',active:'maturidade'};

test('cálculos do cenário distinguem revisão, reprovação e controle não verificado',()=>{
 assert.equal(calcularUsoResponsavel({conformes:8,naoConformes:2,revisao:20}).nota,80);
 assert.equal(calcularUsoResponsavel({conformes:0,naoConformes:2,revisao:0}).nota,0);
 assert.equal(calcularUsoResponsavel({conformes:0,naoConformes:0,revisao:20}).nota,null);
 assert.equal(calcularSeguranca(['ATENDIDO','ATENDIDO','NAO_ATENDIDO',...Array(5).fill('NAO_VERIFICADO')]).nota,200/3);
 assert.equal(calcularSeguranca(Array(8).fill('NAO_VERIFICADO')).nota,null);
 assert.throws(()=>calcularUsoResponsavel({conformes:-1,naoConformes:0,revisao:0}),RangeError);
 assert.throws(()=>calcularSeguranca(['ATENDIDO']),RangeError);
});

test('cenário preserva a base, os escores elegíveis e o isolamento da origem',()=>{
 const antes=JSON.stringify(dados),s=aplicarCenario(dados);
 assert.equal(JSON.stringify(dados),antes);
 assert.equal(dados.total.fatores.find(f=>f.codigo==='usoResponsavel').nota,null);
 assert.equal(s.total.scoreGlobal,dados.total.scoreGlobal);
 assert.equal(s.total.scoreParcial,dados.total.scoreParcial);
 for(const recorte of [s.total,...s.departamentos]){
  for(const codigo of ['usoResponsavel','seguranca']){
   const f=recorte.fatores.find(x=>x.codigo===codigo);
   assert.equal(f.naturezaDados,'SIMULADA');assert.equal(f.elegivelGlobal,false);
   assert.equal(typeof f.nota,'number');assert.match(f.motivo,/fictício/);
  }
 }
 const esperados=fixture.departamentos.reduce((a,d)=>[a[0]+d.conformes,a[1]+d.naoConformes],[0,0]);
 const fator=s.total.fatores.find(f=>f.codigo==='usoResponsavel');
 assert.equal(fator.numerador,esperados[0]);assert.equal(fator.denominador,esperados[0]+esperados[1]);
 assert.equal(s.total.fatores.find(f=>f.codigo==='seguranca').nota,75);
 assert.deepEqual(s.total.resumoDemonstrativo.ausentes,[]);
 assert.equal(s.total.resumoDemonstrativo.cobertura,1);
 assert.throws(()=>aplicarCenario({...dados,empresa:{company_id:'OUTRA'}}),RangeError);
 assert.throws(()=>aplicarCenario({...dados,periodo:{inicio:'2026-11-01',fim:'2026-11-18'}}),RangeError);
});

test('resumo fictício não redistribui pesos quando faltam notas de origem',()=>{
 const s=aplicarCenario(dados),rh=s.departamentos.find(d=>d.department_id==='C01-D09');
 assert.equal(rh.resumoDemonstrativo.nota,null);
 assert(rh.resumoDemonstrativo.ausentes.includes('aiLiteracy'));
 const manual=s.total.fatores.reduce((a,f)=>a+f.peso*(f.codigo==='finOps'?100:f.nota),0);
 assert.equal(s.total.resumoDemonstrativo.nota,Math.round((manual+Number.EPSILON)*100)/100);
});

test('tela do cenário identifica as simulações, o escopo e a navegação',async()=>{
 const s=aplicarCenario(dados);
 const html=await ejs.renderFile(path.join(__dirname,'../views/demonstracao-base.ejs'),{
  ...contexto,dados:s,recorte:s.total,departamento:'',formatar:String});
 assert.match(html,/Cenário fictício complementar/);
 assert.match(html,/Score do cenário fictício/);
 assert.match(html,/name="cenario" value="demo-v1"/);
 assert.match(html,/Não são avaliações realizadas nem evidências de segurança/);
 assert.match(html,/SEG08: ATENDIDO \(hipotético\)/);
 const serie=JSON.parse(html.match(/id="dados-radar"[^>]*>(.*?)<\/script>/s)[1]);
 assert.equal(serie.find(x=>x.nome==='Uso responsável').simulacao,true);
 assert.equal(serie.find(x=>x.nome==='Segurança').nota,75);
 assert.equal(serie.find(x=>x.nome==='Segurança').simulacao,true);
});

test('cenário é optativo e nunca intercepta a fonte operacional',()=>{
 const rotas={};require('../demonstracao-base').registrarDemonstracaoBase({get:(p,h)=>rotas[p]=h});
 assert.equal(rotas['/maturidade/resultados'],undefined);
 let renderizou=false;
 const req={session:contexto,query:{fonte:'SINTETICA',inicio:dados.periodo.inicio,fim:dados.periodo.fim,cenario:'demo-v1'}};
 const res={set:()=>res,render:(view,c)=>{assert.equal(c.dados.cenarioDemo.versao,'demo-v1');assert.equal(c.recorte.fatores.find(f=>f.codigo==='seguranca').nota,75);renderizou=true;}};
 rotas['/maturidade/demonstracao'](req,res);
 assert(renderizou);
});

test('risco fictício não classifica a empresa operacional nem reaproveita notas no filtro mensal',async()=>{
 const s=aplicarCenario(dados),linhas=s.departamentos.map(d=>({nome:d.department_name,eventos:0,requisicoes:0,governanca:null,eventosSeguranca:0}));
 const render=mes=>ejs.renderFile(path.join(__dirname,'../views/risco-base.ejs'),{...contexto,dados:s,linhas,mes,numero:String});
 const total=await render('');assert.match(total,/Risco do cenário fictício/);assert.match(total,/\(simulação\)/);assert.match(total,/Evidências insuficientes/);
 const mensal=await render(dados.financeiro.meses[0]);assert.match(mensal,/Não calculado para este recorte/);
 assert.doesNotMatch(mensal,/style="background:hsl/);
});
