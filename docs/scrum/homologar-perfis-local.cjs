const checks=[];function check(nome,ok){checks.push({nome,aprovado:!!ok});if(!ok)throw Error(nome);}
const base='http://127.0.0.1:3301',headers={'X-Forwarded-Proto':'https'};
async function login(email){const r=await fetch(base+'/acesso',{method:'POST',headers:{...headers,'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({email,senha:'Hestia@2026'}),redirect:'manual'});check('login-'+email.split('@')[0],r.status===302&&!!r.headers.get('set-cookie'));return r.headers.get('set-cookie').split(';')[0];}
async function get(route,cookie){return fetch(base+route,{headers:{...headers,...(cookie?{Cookie:cookie}:{})},redirect:'manual',signal:AbortSignal.timeout(20000)});}
(async()=>{
 check('prontidao',(await get('/ready')).status===200);
 check('visitante-nao-abre-mapa',(await get('/mapa-risco')).status===302);
 const admin=await login('admin.demo@hestia.local'),gestor=await login('gestor.demo@hestia.local'),colab=await login('colaborador.demo@hestia.local');
 for(const [perfil,cookie] of [['admin',admin],['gestor',gestor]]){
  const r=await get('/mapa-risco?inicio=2026-11-03&fim=2026-11-24',cookie);const html=await r.text();
  check('mapa-renderiza-'+perfil,r.status===200&&html.includes('Fonte operacional.'));
  check('ausencia-explicita-'+perfil,html.includes('Sem classificação')&&html.includes('Sem dados para score global'));
  check('cache-privado-'+perfil,r.headers.get('cache-control').includes('no-store'));
  check('posicao-so-admin-'+perfil,perfil==='admin'?html.includes('scope="col">Posição'):!html.includes('scope="col">Posição'));
 }
 check('colaborador-nao-abre-mapa',(await get('/mapa-risco',colab)).status===302);
 const invalid=await get('/mapa-risco?inicio=2026-11-24&fim=2026-11-03',admin);check('periodo-invalido-exibe-alerta',invalid.status===400&&(await invalid.text()).includes('role="alert"'));
 await get('/sair',admin);check('logout-invalida-sessao',(await get('/mapa-risco',admin)).status===302);
 console.log(JSON.stringify({base:'9f06150',data:new Date().toISOString(),natureza:'HTTP_FRONTEND_LOCAL_SEM_VALIDACAO_VISUAL_OU_HTTPS_PUBLICO',checks,aceiteFinal590:false},null,2));
})().catch(e=>{console.error(JSON.stringify({erro:e.message,checks}));process.exitCode=1});
