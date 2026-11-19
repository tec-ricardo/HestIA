const padrao = require('./dados/cenario-maturidade-demo.json');
const pesos = {aiLiteracy:.15,governanca:.15,usoResponsavel:.15,produtividade:.10,
  finOps:.10,seguranca:.20,colaboracao:.05,desenvolvimentoCompetencias:.10};
const valido = n => typeof n === 'number' && Number.isFinite(n) && n>=0 && n<=100;
const arred = n => n==null ? null : Math.round((n+Number.EPSILON)*100)/100;

function calcularUsoResponsavel({conformes,naoConformes,revisao}) {
  if(![conformes,naoConformes,revisao].every(n=>Number.isSafeInteger(n)&&n>=0)) throw new RangeError('Contagens fictícias inválidas.');
  const avaliados=conformes+naoConformes;
  return {nota:avaliados ? 100*conformes/avaliados : null,numerador:conformes,denominador:avaliados,
    motivo:`Exemplo fictício: ${conformes} conformes, ${naoConformes} não conformes e ${revisao} em revisão. Revisões não entram no denominador.`,
    formula:'100 × conformes / (conformes + não conformes)'};
}
function calcularSeguranca(resultados) {
  if(!Array.isArray(resultados)||resultados.length!==8||resultados.some(s=>!['ATENDIDO','NAO_ATENDIDO','NAO_VERIFICADO'].includes(s)))
    throw new RangeError('Informe os oito resultados fictícios dos controles.');
  const atendidos=resultados.filter(s=>s==='ATENDIDO').length;
  const avaliados=resultados.filter(s=>s!=='NAO_VERIFICADO').length;
  return {nota:avaliados ? 100*atendidos/avaliados : null,numerador:atendidos,denominador:avaliados,
    motivo:`Exemplo fictício: ${atendidos} controles atendidos em ${avaliados} avaliados de oito. Não comprova segurança da aplicação.`,
    formula:'100 × controles atendidos / controles avaliados conclusivamente'};
}
function estimarFinOps(sim) {
  if(sim?.natureza!=='SIMULACAO_COM_PREMISSAS'||sim.moedaOrcamento!=='BRL'
      ||![sim.cambioBRLPorUSD,sim.orcamento].every(n=>typeof n==='number'&&Number.isFinite(n)&&n>0)
      ||typeof sim.custoConvertido!=='number'||!Number.isFinite(sim.custoConvertido)||sim.custoConvertido<0)return null;
  return Math.max(0,Math.min(100,100*(2-sim.custoConvertido/sim.orcamento)));
}
function complementar(recorte,uso,seguranca,versao) {
  const fatores=recorte.fatores.map(f=>{
    const complemento=f.codigo==='usoResponsavel'?uso:f.codigo==='seguranca'?seguranca:null;
    if(!complemento||f.nota!=null)return {...f};
    return {...f,...complemento,nota:arred(complemento.nota),naturezaDados:'SIMULADA',estado:'SIMULACAO',
      elegivelGlobal:false,fonte:`Cenário fictício ${versao}; dados de exemplo, sem evidência operacional`};
  });
  const notas=new Map(fatores.map(f=>[f.codigo,f.nota]));
  if(notas.get('finOps')==null)notas.set('finOps',estimarFinOps(recorte.simulacaoFinOps));
  const ausentes=Object.keys(pesos).filter(c=>!valido(notas.get(c)));
  const cobertura=Object.keys(pesos).filter(c=>valido(notas.get(c))).reduce((s,c)=>s+pesos[c],0);
  const nota=ausentes.length?null:arred(Object.entries(pesos).reduce((s,[c,p])=>s+notas.get(c)*p,0));
  const aderencia=['governanca','usoResponsavel','seguranca'].every(c=>valido(notas.get(c)))
    ? .3*notas.get('governanca')+.3*notas.get('usoResponsavel')+.4*notas.get('seguranca') : null;
  const indice=aderencia==null?null:100-aderencia;
  return {...recorte,fatores,resumoDemonstrativo:{nota,cobertura:arred(cobertura),ausentes,
    formula:'Soma das oito notas × pesos da composição; disponível somente com as oito notas.',
    natureza:'CENARIO_FICTICIO_COM_PREMISSAS'},
    riscoDemonstrativo:indice==null?null:{indice:arred(indice),faixa:indice<25?'BAIXO':indice<50?'MEDIO':'ALTO',
      natureza:'CENARIO_FICTICIO_COM_PREMISSAS'}};
}
function aplicarCenario(dados,cenario=padrao) {
  if(cenario.versao!=='demo-v1'||cenario.natureza!=='CENARIO_FICTICIO_COM_PREMISSAS'
      ||dados.empresa.company_id!==cenario.empresaOrigem||dados.periodo.inicio!==cenario.inicio||dados.periodo.fim!==cenario.fim)
    throw new RangeError('O cenário fictício não corresponde à empresa e ao período desta base.');
  if(JSON.stringify(cenario.controles)!==JSON.stringify(['SEG01','SEG02','SEG03','SEG04','SEG05','SEG06','SEG07','SEG08']))
    throw new RangeError('Catálogo de controles fictícios inválido.');
  const porDepartamento=new Map();
  for(const d of cenario.departamentos){
    if(porDepartamento.has(d.id)||!dados.departamentos.some(x=>x.department_id===d.id))throw new RangeError('Departamento fictício inválido ou duplicado.');
    porDepartamento.set(d.id,d);
  }
  if(porDepartamento.size!==dados.departamentos.length)throw new RangeError('O cenário deve declarar todos os departamentos.');
  const total=cenario.departamentos.reduce((s,d)=>({conformes:s.conformes+d.conformes,
    naoConformes:s.naoConformes+d.naoConformes,revisao:s.revisao+d.revisao}),{conformes:0,naoConformes:0,revisao:0});
  return {...dados,cenarioDemo:cenario,
    total:complementar(dados.total,calcularUsoResponsavel(total),calcularSeguranca(cenario.segurancaEmpresa),cenario.versao),
    departamentos:dados.departamentos.map(d=>{const exemplo=porDepartamento.get(d.department_id);
      return complementar(d,calcularUsoResponsavel(exemplo),calcularSeguranca(exemplo.seguranca),cenario.versao);})};
}
module.exports={aplicarCenario,calcularUsoResponsavel,calcularSeguranca};
