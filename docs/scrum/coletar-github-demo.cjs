const fs=require('node:fs'),path=require('node:path');
const repo='tec-ricardo/HestIA',inicio='2026-11-03',fim='2026-11-12';
const de=new Date(inicio+'T00:00:00-03:00'),ate=new Date('2026-11-13T00:00:00-03:00');
async function get(rota){const r=await fetch('https://api.github.com/repos/'+repo+rota,{headers:{'User-Agent':'HestIA-demo-validacao','Accept':'application/vnd.github+json'},signal:AbortSignal.timeout(20000)});if(!r.ok)throw Error('GitHub HTTP '+r.status);return {dados:await r.json(),restante:Number(r.headers.get('x-ratelimit-remaining'))};}
async function main(){
 const dest=process.argv[2]||path.join(__dirname,'colaboracao-github-demo.json');if(fs.existsSync(dest))throw Error('Saída já existe; escolha um novo arquivo para preservar a coleta anterior.');
 let prs=[],completo=false,restante=0;
 for(let page=1;page<=5;page++){
  const r=await get('/pulls?state=closed&sort=updated&direction=desc&per_page=100&page='+page);restante=r.restante;
  prs.push(...r.dados.filter(p=>p.merged_at&&new Date(p.merged_at)>=de&&new Date(p.merged_at)<ate));
  if(r.dados.length<100||new Date(r.dados.at(-1).updated_at)<de){completo=true;break;}
 }
 const autores=new Map();const registros=[];
 for(const pr of prs){
  if(!pr.user?.id||!pr.user?.login)continue;
  autores.set(pr.user.id,{githubId:pr.user.id,login:pr.user.login,empresaOrigem:'HESTIA_EQUIPE_DEMO',departamentoOrigem:'DESENVOLVIMENTO_DEMO',naturezaVinculo:'CADASTRO_DEMONSTRATIVO',identidadeCivilConfirmada:false});
  let revisoes=null,motivo=null;
  if(restante>3){try{const r=await get('/pulls/'+pr.number+'/reviews?per_page=100');restante=r.restante;if(r.dados.length===100)motivo='Paginação de reviews não concluída';else revisoes=r.dados.filter(x=>x.user?.id&&x.user.id!==pr.user.id&&x.state==='APPROVED'&&x.submitted_at&&new Date(x.submitted_at)<=new Date(pr.merged_at)).map(x=>({id:x.id,revisorGithubId:x.user.id,estado:x.state,submetidaEm:x.submitted_at}));}catch(e){motivo=e.message;}}
  else motivo='Limite de consultas públicas; revisão não verificada';
  registros.push({repositorio:repo,numero:pr.number,url:pr.html_url,dataMerge:pr.merged_at,dataMergeLocal:new Intl.DateTimeFormat('en-CA',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(pr.merged_at)),autorGithubId:pr.user.id,autorLogin:pr.user.login,shaMerge:pr.merge_commit_sha,quantidadeRevisoes:revisoes===null?null:revisoes.length,revisoes,limitacao:motivo});
 }
 const out={versao:'colaboracao-demo-1.0',coletadoEm:new Date().toISOString(),repositorio:repo,inicio,fim,fuso:'America/Sao_Paulo',naturezaEventos:'OPERACIONAL_GITHUB',naturezaVinculos:'DEMONSTRATIVA_HIPOTESE_AUTORIZADA',populacaoPrsCompleta:completo,autores:[...autores.values()],prs:registros,avisos:['Eventos reais do projeto; vínculos de empresa e departamento são exclusivos da demonstração, sem associação à QSoft.','Review exige aprovação registrada por outra conta antes do merge. Identidades civis ou qualidade da revisão não foram comprovadas.','Não representa toda a Sprint 4: corte em 12/11/2026. Não prova colaboração causada pela HestIA.']};
 fs.writeFileSync(dest,JSON.stringify(out,null,2)+'\n');console.log(JSON.stringify({arquivo:dest,autores:autores.size,prs:registros.length,prsComRevisaoVerificada:registros.filter(x=>x.quantidadeRevisoes!==null).length,populacaoPrsCompleta:completo}));
}
if(require.main===module)main().catch(e=>{console.error(e.message);process.exit(1)});
