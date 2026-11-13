const checks=[];const base='http://backend:8080';
function check(nome,ok){checks.push({nome,aprovado:!!ok});if(!ok)throw Error(nome);}
async function login(email){const r=await fetch(base+'/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,senha:'Hestia@2026'})});if(!r.ok)throw Error('Login de ensaio indisponível');return(await r.json()).token;}
function csv(texto){const rows=[];let row=[],v='',q=false;for(let i=0;i<texto.length;i++){const c=texto[i];if(c==='"'){if(q&&texto[i+1]==='"'){v+='"';i++;}else q=!q;}else if(c===','&&!q){row.push(v);v='';}else if(c==='\n'&&!q){row.push(v.replace(/\r$/,''));rows.push(row);row=[];v='';}else v+=c;}return new Map(rows.slice(1).map(r=>[r[0],r[1]]));}
(async()=>{
 const admin=await login('admin.demo@hestia.local'),colab=await login('colaborador.demo@hestia.local');
 const query='?inicio=2026-11-03&fim=2026-11-24&fonte=OPERACIONAL';
 const get=(rota,token)=>fetch(base+rota,{headers:{Authorization:'Bearer '+token}});
 const r=await get('/relatorios-maturidade/resultados'+query,admin);check('consulta-json-autorizada',r.status===200);const dados=await r.json();
 check('oito-fatores-explicitos',dados.fatores.length===8&&new Set(dados.fatores.map(f=>f.codigo)).size===8);
 check('global-nao-preenche-fatores-ausentes',dados.scoreGlobal===null&&dados.dimensoesAusentes.length>0);
 check('finops-sem-dados-nao-vira-zero',dados.fatores.find(f=>f.codigo==='finOps').nota===null);
 const uso=dados.fatores.find(f=>f.codigo==='usoResponsavel'),competencias=dados.fatores.find(f=>f.codigo==='desenvolvimentoCompetencias');
 check('notas-conferidas-contra-massa-controlada',uso.nota===33.33&&uso.amostra===4&&uso.estado==='PARCIAL'&&competencias.nota===100&&competencias.amostra===1&&competencias.estado==='PARCIAL');
 check('fonte-operacional-preservada',dados.naturezaDados==='OPERACIONAL'&&dados.fatores.every(f=>f.naturezaDados==='OPERACIONAL'));
 const rcsv=await get('/relatorios-maturidade/resultados.csv'+query,admin);check('exportacao-csv-autorizada',rcsv.status===200&&rcsv.headers.get('content-type').includes('text/csv'));
 const exportado=csv(await rcsv.text());
 check('csv-preserva-notas-estados-e-amostras',dados.fatores.every((f,i)=>{const nota=exportado.get('fatores['+i+'].nota');return exportado.get('fatores['+i+'].codigo')===f.codigo&&(f.nota==null?nota==='':nota!==''&&Number(nota)===f.nota)&&exportado.get('fatores['+i+'].estado')===f.estado&&exportado.get('fatores['+i+'].amostra')===String(f.amostra);}));
 check('csv-preserva-natureza-e-global-ausente',exportado.get('naturezaDados')==='OPERACIONAL'&&exportado.get('scoreGlobal')==='');
 check('colaborador-nao-exporta-relatorio-gerencial',(await get('/relatorios-maturidade/resultados.csv'+query,colab)).status===403);
 console.log(JSON.stringify({base:'f71021d',natureza:'ENSAIO_LOCAL_PARCIAL',periodo:{inicio:dados.inicio,fim:dados.fim,fuso:dados.fusoOrigem},checks,aceiteFinal585:false,pendencias:['Integração da 578 e demais branches analíticas do Knorre','Ranking/risco integrado 579/580/582','Comparação final dos oito fatores com a release aceita'],fatores:dados.fatores.map(f=>({codigo:f.codigo,nota:f.nota,estado:f.estado,amostra:f.amostra})),scoreGlobal:dados.scoreGlobal},null,2));
})().catch(e=>{console.error(JSON.stringify({erro:e.message,checks}));process.exit(1)});
