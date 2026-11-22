const { gerarDiagnostico } = require('./diagnostico-gerencial');
const inteiro = n => Number.isSafeInteger(Number(n)) && Number(n)>=0 ? Number(n) : 0;
const proporcao = (n,d) => d>0 ? n*100/d : null;
const media = xs => xs.length ? xs.reduce((a,b)=>a+b,0)/xs.length : null;
function filtrosPainel(query={}) {
 const dias=[7,30,90].includes(Number(query.dias))?Number(query.dias):30;
 const departamentoId=typeof query.departamentoId==='string'&&/^\d+$/.test(query.departamentoId)&&Number.isSafeInteger(Number(query.departamentoId))&&Number(query.departamentoId)>0?Number(query.departamentoId):null;
 return {dias,departamentoId};
}
function queryPainel(filtros={}) {
 return new URLSearchParams({dias:String(filtros.dias??30),...(filtros.departamentoId?{departamentoId:String(filtros.departamentoId)}:{})}).toString();
}
function serieDias(inicio,fim,linhas){
 if(!/^\d{4}-\d{2}-\d{2}$/.test(inicio||'')||!/^\d{4}-\d{2}-\d{2}$/.test(fim||''))return [];
 const mapa=new Map((linhas||[]).map(l=>[String(l.dia).slice(0,10),l]));
 const serie=[];
 for(let d=new Date(inicio+'T00:00:00Z');d<=new Date(fim+'T00:00:00Z')&&serie.length<90;d.setUTCDate(d.getUTCDate()+1)){
  const dia=d.toISOString().slice(0,10);const l=mapa.get(dia);serie.push({dia,usos:inteiro(l?.usos),tokens:l?inteiro(l.tokens):0});
 }
 return serie;
}
function montarDashboard(painel,maturidade,filtros={}){
 const diagnostico=gerarDiagnostico(maturidade);
 const base={disponivel:!!painel,diagnostico,filtros,query:queryPainel(filtros),maturidade:maturidade??null};
 if(!painel)return base;
 const pessoas=painel.colaboradores;
 const somar=k=>pessoas.reduce((n,c)=>n+inteiro(c[k]),0);
 const ativos=pessoas.filter(c=>inteiro(c.utilizacoes)>0).length;
 const literacy=pessoas.map(c=>c.aiLiteracy).filter(n=>typeof n==='number'&&Number.isFinite(n)&&n>=0&&n<=100);
 const conformes=somar('conformes'),nao=somar('nao_conformes'),revisoes=somar('revisoes');
 const usos=somar('utilizacoes'),tokens=somar('tokens'),medidos=somar('usos_com_tokens');
 const anterior=painel.anterior&&Number.isSafeInteger(Number(painel.anterior.usos))?inteiro(painel.anterior.usos):null;
 const deps=new Map();
 for(const c of pessoas){const k=String(c.departamento_id??c.departamento);const dep=deps.get(k)||{nome:c.departamento,total:0,participantes:0,usos:0};dep.total++;dep.participantes+=inteiro(c.utilizacoes)>0?1:0;dep.usos+=inteiro(c.utilizacoes);deps.set(k,dep);}
 const departamentos=[...deps.values()].map(d=>({...d,taxa:proporcao(d.participantes,d.total)})).sort((a,b)=>b.taxa-a.taxa);
 const ordem={'Uso responsável':0,'Governança':1,'FinOps':2,'AI Literacy':3,'Base de conhecimento':4,'Competências':5};
 const acoes=pessoas.flatMap(c=>(c.alertas||[]).map(a=>({...a,id:c.id,nome:c.nome,departamento:c.departamento}))).sort((a,b)=>(ordem[a.dimensao]??9)-(ordem[b.dimensao]??9));
 return {...base,painel,ativos,total:pessoas.length,adocao:proporcao(ativos,pessoas.length),conformes,nao,revisoes,
  conformidade:proporcao(conformes,conformes+nao),literacy:media(literacy),avaliados:literacy.length,
  usos,tokens,medidos,treinamentos:somar('treinamentos'),reutilizacoes:somar('reutilizacoes'),xp:somar('xp_periodo'),
  sucessos:somar('sucessos'),semFonte:somar('semFonte'),atencao:pessoas.filter(c=>(c.alertas||[]).length>0).length,
  anterior,variacao:anterior>0?(usos-anterior)*100/anterior:null,
  serie:serieDias(painel.inicio,painel.fim,painel.evolucao),
  ferramentas:(painel.ferramentasUso||[]).slice(0,6),
  finalidades:(painel.finalidades||[]).slice(0,6),
  departamentos,acoes};
}
module.exports={filtrosPainel,queryPainel,serieDias,montarDashboard};
