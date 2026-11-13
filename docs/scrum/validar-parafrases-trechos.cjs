const fs=require('fs');const base='http://127.0.0.1:18080';
async function api(rota,t,b,method=b?'POST':'GET'){const r=await fetch(base+rota,{method,headers:{'Content-Type':'application/json',...(t?{Authorization:'Bearer '+t}:{})},body:b?JSON.stringify(b):undefined,signal:AbortSignal.timeout(90000)});if(!r.ok)throw Error(rota+' HTTP '+r.status);const s=await r.text();return s?JSON.parse(s):null;}
(async()=>{
 const admin=(await api('/auth/login',null,{email:'admin.demo@hestia.local',senha:'Hestia@2026'})).token;
 await api('/api/respostas-reutilizaveis/2/indexar',admin,null,'POST');
 const token=(await api('/auth/login',null,{email:'colaborador.demo@hestia.local',senha:'Hestia@2026'})).token;
 const qs=[
 ['Posso colocar o CPF do cliente no ChatGPT?',true],
 ['Posso mandar o nome e telefone do cliente para uma IA?',true],
 ['É permitido colar a senha do sistema no assistente?',true],
 ['Preciso revisar os números que a inteligência artificial produz?',true],
 ['Posso usar qualquer ferramenta de inteligência artificial no trabalho?',true],
 ['Posso confiar numa política que já foi retirada?',true],
 ['Se eu apagar os nomes, posso compartilhar um exemplo com IA?',true],
 ['Quem é responsável pela decisão tomada com uma resposta de IA?',true],
 ['O ChatGPT pode receber documentos confidenciais da empresa?',true],
 ['Preciso checar as citações geradas pelo assistente?',true],
 ['Como fazer bolo de cenoura?',false],['Qual é a capital da França?',false],['Como solicitar férias?',false],
 ['Qual é o resultado do jogo de futebol de ontem?',false],['Me ensine uma receita de lasanha.',false]
 ];
 const casos=[];for(const [pergunta,esperada] of qs){const r=await api('/api/rag/perguntas',token,{chave:'parafrase-'+crypto.randomUUID(),pergunta,modo:'CONSULTAR'});casos.push({pergunta,esperada,recuperou:r.fontes.some(f=>f.id===2),estado:r.estado,similaridade:r.similaridade,correto:r.fontes.some(f=>f.id===2)===esperada});}
 const gerada=await api('/api/rag/perguntas',token,{chave:'cpf-geracao-'+crypto.randomUUID(),pergunta:qs[0][0],modo:'GERAR_NOVA'});
 const resultado={data:new Date().toISOString(),natureza:'DIDATICO_COM_PROVEDOR_REAL',estrategia:'documento e trechos com mesma fonte, permissão e snapshot',limiarDemo:0.35,casos,geracaoCPF:{estado:gerada.estado,fontes:gerada.fontes.map(f=>f.id),resposta:gerada.resposta},aprovado:casos.every(c=>c.correto)&&gerada.estado==='GERADA_COM_FONTES'};
 fs.writeFileSync(__dirname+'/rag-trechos-parafrases.json',JSON.stringify(resultado,null,2)+'\n');console.log(JSON.stringify({acertos:casos.filter(c=>c.correto).length,total:casos.length,geracao:gerada.estado}));if(!resultado.aprovado)process.exitCode=2;
})().catch(e=>{console.error(e.message);process.exitCode=1});
