const checks=[];
const base='http://127.0.0.1:3000';
const headers={'X-Forwarded-Proto':'https'};
async function login(email){const r=await fetch(base+'/acesso',{method:'POST',headers:{...headers,'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({email,senha:'Hestia@2026'}),redirect:'manual'});if(r.status!==302||!r.headers.get('set-cookie'))throw Error('Login de ensaio não confirmado');return r.headers.get('set-cookie').split(';')[0];}
async function req(nome,rota,cookie,status=200,method='GET'){const r=await fetch(base+rota,{headers:{...headers,Cookie:cookie},method,redirect:'manual'});if(r.status!==status)throw Error(nome+': HTTP '+r.status+' esperado '+status);checks.push({nome,aprovado:true});return r;}
(async()=>{
 const colab=await login('colaborador.demo@hestia.local');const admin=await login('admin.demo@hestia.local');const gestor=await login('gestor.demo@hestia.local');
 await req('prontidao-frontend','/ready',colab);
 const html=await (await req('template-rag-renderiza','/assistente-rag',colab)).text();if(!html.includes('Ver limites da conversa'))throw Error('Controle de contexto ausente');
 const hist=await (await req('historico-com-conversa','/api/rag/historico',colab)).json();if(!hist.length||hist.some(x=>x.conversaId!==1))throw Error('Vínculo de conversa ausente');
 await req('limites-via-bff','/api/rag/conversas/1/contexto',colab);
 await req('contexto-restrito-ao-autor','/api/rag/conversas/1/contexto',admin,404);
 await req('curadoria-leitura-gestor','/api/admin/respostas',gestor);
 await req('curadoria-restrita-colaborador','/api/admin/respostas',colab,403);
 await req('exclusao-nao-alcanca-outro-autor','/api/rag/historico/'+hist[0].id,admin,404,'DELETE');
 await req('exclusao-da-interacao-correta','/api/rag/historico/'+hist[0].id,colab,204,'DELETE');
 const depois=await (await fetch(base+'/api/rag/historico',{headers:{...headers,Cookie:colab}})).json();if(depois.length!==hist.length-1||depois.some(x=>x.id===hist[0].id))throw Error('Exclusão incorreta');
 console.log(JSON.stringify({natureza:'ENSAIO_DIDATICO_LOCAL_DOCKER',checks},null,2));
})().catch(e=>{console.error(JSON.stringify({erro:e.message,checks}));process.exit(1)});
