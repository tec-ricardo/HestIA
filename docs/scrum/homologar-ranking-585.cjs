const {Client}=require('/app/node_modules/pg');
const checks=[];function check(nome,ok){checks.push({nome,aprovado:!!ok});if(!ok)throw Error(nome);}
const base='http://backend:8080',inicio='2026-11-03',fim='2026-11-24';
async function login(email){const r=await fetch(base+'/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,senha:'Hestia@2026'})});if(!r.ok)throw Error('Login indisponível');return(await r.json()).token;}
async function main(){
 const db=new Client({connectionString:process.env.SESSION_DATABASE_URL});await db.connect();
 try{
 const admin=await login('admin.demo@hestia.local'),gestor=await login('gestor.demo@hestia.local'),colab=await login('colaborador.demo@hestia.local');
 const get=(url,t)=>fetch(base+url,{headers:{Authorization:'Bearer '+t},signal:AbortSignal.timeout(20000)});
 const q='?inicio='+inicio+'&fim='+fim;
 const route='/indicadores/maturidade/ranking-departamental';
 const r=await get(route+q,admin);check('admin-consulta-ranking',r.status===200);const ranking=await r.json();
 await db.query("INSERT INTO departamentos (empresa_id,nome) SELECT $1,'Departamento isolado de homologação' WHERE NOT EXISTS (SELECT 1 FROM departamentos WHERE empresa_id=$1 AND nome='Departamento isolado de homologação')",[ranking.empresaId]);
 const refreshed=await get(route+q,admin);Object.assign(ranking,await refreshed.json());
 const departamentos=(await db.query('SELECT id,nome FROM departamentos WHERE empresa_id=$1 ORDER BY nome,id',[ranking.empresaId])).rows;
 check('ranking-restrito-a-empresa',ranking.departamentos.length===departamentos.length&&ranking.departamentos.every(d=>departamentos.some(x=>Number(x.id)===d.departamentoId)));
 check('fonte-periodo-versao-preservados',ranking.fonte==='OPERACIONAL'&&ranking.inicio===inicio&&ranking.fim===fim&&ranking.situacaoRisco==='risco-departamental-1.0');
 check('score-ausente-sem-posicao',ranking.departamentos.every(d=>d.apuracao.scoreGlobal!==null||d.posicao===null));
 const rg=await get(route+q,gestor);check('gestor-consulta-ranking',rg.status===200);const g=await rg.json();
 const dep=(await db.query("SELECT departamento_id FROM usuarios WHERE email='gestor.demo@hestia.local'")).rows[0].departamento_id;
 check('gestor-ve-apenas-seu-departamento-sem-posicao',g.departamentos.length===1&&g.departamentos[0].departamentoId===Number(dep)&&g.departamentos[0].posicao===null);
 check('funcionario-nao-consulta-ranking',(await get(route+q,colab)).status===403);
 check('periodo-invertido-rejeitado',(await get(route+'?inicio='+fim+'&fim='+inicio,admin)).status===400);
 for(const linha of ranking.departamentos){
  const ir=await get('/indicadores/maturidade/investigacao'+q+'&departamentoId='+linha.departamentoId,admin);check('investigacao-autorizada-departamento-'+linha.departamentoId,ir.status===200);const i=await ir.json();
  check('risco-investigacao-igual-ranking-'+linha.departamentoId,JSON.stringify(i.risco)===JSON.stringify(linha.risco));
  check('fatores-investigacao-iguais-ranking-'+linha.departamentoId,JSON.stringify(i.maturidade.fatores)===JSON.stringify(linha.apuracao.fatores));
  const esperado=(await db.query(`SELECT t.id,t.avaliacao_id,t.status,t.responsavel_id FROM tratamentos_nao_conformidade t JOIN avaliacoes_conformidade a ON a.id=t.avaliacao_id JOIN registros_uso_ia r ON r.id=a.registro_uso_id WHERE t.empresa_id=$1 AND t.departamento_id=$2 AND r.data_hora >= $3::date AND r.data_hora < $4::date+1 AND a.data_avaliacao >= $3::date AND a.data_avaliacao < $4::date+1 AND t.status IN ('ABERTA','EM_TRATAMENTO') ORDER BY t.id`,[ranking.empresaId,linha.departamentoId,inicio,fim])).rows;
  check('pendencias-conferidas-no-banco-'+linha.departamentoId,JSON.stringify(i.pendencias)===JSON.stringify(esperado.map(p=>({tratamentoId:Number(p.id),avaliacaoId:Number(p.avaliacao_id),status:p.status,responsavelId:p.responsavel_id===null?null:Number(p.responsavel_id)}))));
  check('sem-fatores-nao-inventa-classificacao-'+linha.departamentoId,linha.risco.estado!=='SEM_CLASSIFICACAO'||(linha.risco.indice===null&&linha.risco.classificacao===null&&linha.risco.fragil));
 }
 const other=departamentos.find(d=>Number(d.id)!==Number(dep));
 if(other)check('gestor-nao-investiga-outro-departamento',[403,404].includes((await get('/indicadores/maturidade/investigacao'+q+'&departamentoId='+other.id,gestor)).status));
 check('funcionario-nao-investiga',[403,404].includes((await get('/indicadores/maturidade/investigacao'+q+'&departamentoId='+dep,colab)).status));
 console.log(JSON.stringify({base:'9f06150',data:new Date().toISOString(),natureza:'ENSAIO_DIDATICO_LOCAL_DOCKER',checks,departamentos:ranking.departamentos.map(d=>({id:d.departamentoId,score:d.apuracao.scoreGlobal,posicao:d.posicao,risco:d.risco.estado})),aceiteFinal585:false},null,2));
 }finally{await db.end();}
}
main().catch(e=>{console.error(JSON.stringify({erro:e.message,checks}));process.exitCode=1});
