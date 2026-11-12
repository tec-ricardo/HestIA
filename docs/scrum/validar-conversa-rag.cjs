// Somente demonstração isolada: altera temporariamente os perfis da fonte e restaura no finally.
const fs=require('node:fs');
const base=process.env.HESTIA_RAG_BASE_URL||'http://127.0.0.1:8080';
const fonteId=Number(process.env.HESTIA_RAG_FONTE_ID);
const saida=process.env.HESTIA_RAG_RELATORIO||require('node:path').join(process.cwd(),'SCRUM-546-resultado.json');
async function api(rota,token,body,metodo=body?'POST':'GET'){
 const r=await fetch(base+rota,{method:metodo,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(120000)});
 const txt=await r.text();if(!r.ok){let e=Error(rota+': HTTP '+r.status);e.status=r.status;throw e;}return txt?JSON.parse(txt):null;
}
async function main(){
 if(!Number.isSafeInteger(fonteId)||fonteId<=0)throw Error("Defina HESTIA_RAG_FONTE_ID com o ID retornado pelo script de publicação.");
 const sessao=await api('/auth/login',null,{email:'colaborador.demo@hestia.local',senha:process.env.HESTIA_DEMO_PASSWORD||'Hestia@2026'}),token=sessao.token;
 const admin=await api('/auth/login',null,{email:'admin.demo@hestia.local',senha:process.env.HESTIA_DEMO_PASSWORD||'Hestia@2026'});
 const fonte=await api('/api/respostas-reutilizaveis/'+fonteId,token);
 if(!fonte.promptOriginal.includes('Demonstração HestIA'))throw Error('Fonte inesperada.');
 const perfisOriginais=fonte.perfisPermitidos;if(!Array.isArray(perfisOriginais)||!perfisOriginais.includes('ADMIN')||!perfisOriginais.includes('FUNCIONARIO'))throw Error('Perfis demo inesperados.');
 const conversa=await api('/api/rag/conversas',token,{titulo:'Validação 546 — proteção de dados'});
 const outra=await api('/api/rag/conversas',token,{titulo:'Validação 546 — conversa separada'});
 const perguntar=(pergunta,conversaId)=>api('/api/rag/perguntas',token,{chave:'546-'+crypto.randomUUID(),pergunta,conversaId,modo:'GERAR_NOVA'});
 const inicial=await perguntar('Posso enviar dados pessoais de clientes a uma ferramenta de IA?',conversa.id);
 const continuacao=await perguntar('E se eu remover os nomes e identificadores?',conversa.id);
 const separada=await perguntar('E se eu remover os nomes e identificadores?',outra.id);
 const historico=await api('/api/rag/conversas/'+conversa.id+'/historico',token);
 let isolamento=false;try{await api('/api/rag/conversas/'+conversa.id+'/historico',admin.token);}catch(e){isolamento=e.status===404;}
 let revogacao;
 try{
  await api('/api/respostas-reutilizaveis/'+fonteId+'/perfis',admin.token,['ADMIN'],'PUT');
  const antes=await api('/api/rag/conversas/'+conversa.id+'/historico',token);
  const depois=await perguntar('E isso?',conversa.id);
  revogacao={historicoOculto:antes.every(r=>r.estado==='FONTES_INDISPONIVEIS'),estado:depois.estado};
 }finally{await api('/api/respostas-reutilizaveis/'+fonteId+'/perfis',admin.token,perfisOriginais,'PUT');}
 const final=await api('/api/respostas-reutilizaveis/'+fonteId,token);
 const checks={inicialComFontes:inicial.estado==='GERADA_COM_FONTES',continuacaoComFontes:continuacao.estado==='GERADA_COM_FONTES',novaConversaPedeEsclarecimento:separada.estado==='PRECISA_ESCLARECIMENTO',historicoDaMesmaConversa:historico.length===2,isolamento,revogacao:revogacao.historicoOculto&&revogacao.estado==='PRECISA_ESCLARECIMENTO',semCreditoPorGeracao:fonte.quantidadeReutilizacoes===final.quantidadeReutilizacoes};
 const resultado={data:new Date().toISOString(),modelo:process.env.HESTIA_RAG_MODELO||'configurado no servidor',natureza:'DIDATICO_COM_PROVEDOR_REAL',banco:process.env.HESTIA_RAG_BANCO||'ambiente demo informado pelo operador',checks,inicial,continuacao,separada,revogacao};
 fs.writeFileSync(saida,JSON.stringify(resultado,null,2));
 console.log(JSON.stringify(checks));if(Object.values(checks).some(x=>!x))process.exitCode=2;
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
