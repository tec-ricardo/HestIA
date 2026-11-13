// Executar somente no ambiente didático preparado pela SCRUM-543.
const fs=require('node:fs'),path=require('node:path');
const base=process.env.HESTIA_RAG_BASE_URL||'http://127.0.0.1:8080';
const fonteId=Number(process.env.HESTIA_RAG_FONTE_ID);
const saida=process.env.HESTIA_RAG_RELATORIO||path.join(process.cwd(),'SCRUM-545-resultado.json');
const corpus=JSON.parse(fs.readFileSync(path.join(__dirname,'SCRUM-545-corpus.json'),'utf8'));
async function api(rota,token,body,metodo=body?'POST':'GET'){
 const r=await fetch(base+rota,{method:metodo,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(90000)});
 const raw=await r.text();if(!r.ok)throw new Error(rota+': HTTP '+r.status);return raw?JSON.parse(raw):null;
}
async function main(){
 if(!Number.isSafeInteger(fonteId)||fonteId<=0)throw Error('Defina HESTIA_RAG_FONTE_ID com o ID retornado por publicar-politica-demo.ps1.');
 const sessao=await api('/auth/login',null,{email:'colaborador.demo@hestia.local',senha:process.env.HESTIA_DEMO_PASSWORD||'Hestia@2026'});
 const token=sessao.token,ctx=await api('/api/contexto',token);
 const fonte=await api('/api/respostas-reutilizaveis/'+fonteId,token);
 if(!fonte.promptOriginal.includes('Demonstração HestIA')||!fonte.respostaGerada.includes('não representa uma política oficial da QSoft'))throw Error('A fonte não é a política didática v1.1 esperada.');
 const inicio=fonte.quantidadeReutilizacoes;
 const casos=[];
 for(const c of corpus.casos){
  const encontradas=await api('/api/respostas-reutilizaveis/buscar-semelhantes',token,{pergunta:c.pergunta,limite:20,similaridadeMinima:0});
  const alvo=encontradas.find(r=>r.id===fonteId);
  const consulta=await api('/api/rag/perguntas',token,{chave:'545-'+crypto.randomUUID(),pergunta:c.pergunta,modo:'CONSULTAR'});
  const recuperou=consulta.fontes.some(f=>f.id===fonteId);
  casos.push({...c,similaridade:alvo?.similaridade??null,estado:consulta.estado,recuperou,correto:recuperou===c.esperada,ids:consulta.fontes.map(f=>f.id)});
 }
 const fim=await api('/api/respostas-reutilizaveis/'+fonteId,token);
 const chave='545-confirmacao-'+crypto.randomUUID();
 const confirmar=()=>api('/api/rag/perguntas',token,{chave,pergunta:corpus.casos[0].pergunta,modo:'REUTILIZAR',respostaId:fonteId});
 const primeira=await confirmar(),repetida=await confirmar();
 const depois=await api('/api/respostas-reutilizaveis/'+fonteId,token);
 const limiares=[0.2,0.25,0.3,0.35,0.4,0.45,0.5,0.55,0.6,0.65,0.7].map(limiar=>({limiar,acertos:casos.filter(c=>(c.similaridade!==null&&c.similaridade>=limiar)===c.esperada).length,falsosPositivos:casos.filter(c=>!c.esperada&&c.similaridade!==null&&c.similaridade>=limiar).length,falsosNegativos:casos.filter(c=>c.esperada&&(c.similaridade===null||c.similaridade<limiar)).length}));
 const resultado={executadoEm:new Date().toISOString(),corpus:corpus.versao,natureza:corpus.natureza,empresaId:ctx.empresaId,fonteId,casos,limiares,consultaSemContagem:fim.quantidadeReutilizacoes===inicio,confirmacaoUnica:primeira.id===repetida.id&&depois.quantidadeReutilizacoes===inicio+1,observacao:'Amostra didática pequena: não demonstra qualidade em documentos ou perguntas fora deste corpus.'};
 fs.writeFileSync(saida,JSON.stringify(resultado,null,2)+'\n');
 console.log(JSON.stringify({arquivo:saida,acertos:casos.filter(c=>c.correto).length,total:casos.length,consultaSemContagem:resultado.consultaSemContagem,confirmacaoUnica:resultado.confirmacaoUnica,limiares}));
 if(casos.some(c=>!c.correto)||!resultado.consultaSemContagem||!resultado.confirmacaoUnica)process.exitCode=2;
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
