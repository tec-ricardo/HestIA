const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const tela=fs.readFileSync(path.join(__dirname,'../views/assistente-rag.ejs'),'utf8');
function montar(fetch,copyText=async()=>{}){
  const avisos=[];
  const contexto={fetch,copyText,crypto:{randomUUID:()=> 'chave-estavel'},persist:()=>{},persistBank:()=>{},registrarReutilizacao:()=>avisos.push('confirmada'),notify:m=>avisos.push(m)};
  vm.createContext(contexto);
  vm.runInContext(tela.slice(tela.indexOf('      const confirmacoesPendentes'),tela.indexOf('      const bankBody'))+';globalThis.confirmar=confirmarNoServidor;',contexto);
  return {confirmar:contexto.confirmar,avisos};
}
test('falha de rede mantém a chave no reenvio e só sucesso registra reuso',async()=>{
  const chamadas=[];let primeira=true;
  const {confirmar,avisos}=montar(async(url,opcoes)=>{chamadas.push(JSON.parse(opcoes.body));if(primeira){primeira=false;throw Error('Rede');}return {ok:true,json:async()=>({estado:'REUTILIZADA',resposta:'Texto autorizado'})};});
  const item={question:'Qual regra?',respostaId:77};
  await assert.rejects(confirmar(item),/Rede/);assert.equal(item.reutilizada,undefined);assert.equal(avisos.length,0);
  await confirmar(item);await confirmar(item);
  assert.ok(chamadas.every(c=>c.chave==='chave-estavel'&&c.modo==='REUTILIZAR'&&c.respostaId===77));
  assert.equal(avisos.filter(a=>a==='confirmada').length,1);assert.equal(item.reutilizada,true);
});
test('duplo clique aguarda a mesma confirmação',async()=>{
  let liberar;let chamadas=0;const espera=new Promise(r=>liberar=r);
  const {confirmar}=montar(async()=>{chamadas++;await espera;return {ok:true,json:async()=>({estado:'REUTILIZADA',resposta:'Texto'})};});
  const item={question:'Regra',respostaId:77};const primeira=confirmar(item);
  await assert.rejects(confirmar(item),/Aguarde/);liberar();await primeira;assert.equal(chamadas,1);
});
test('fonte indisponível não copia texto antigo nem registra sucesso',async()=>{
  let copias=0;const {confirmar,avisos}=montar(async()=>({ok:true,json:async()=>({estado:'FONTES_INDISPONIVEIS',resposta:null})}),async()=>copias++);
  await assert.rejects(confirmar({question:'Regra',respostaId:77}),/indisponível/);assert.equal(copias,0);assert.equal(avisos.length,0);
});
test('falha ao copiar depois do sucesso não desfaz confirmação nem gera novo crédito',async()=>{
  const {confirmar,avisos}=montar(async()=>({ok:true,json:async()=>({estado:'REUTILIZADA',resposta:'Texto'})}),async()=>{throw Error('Clipboard');});
  const item={question:'Regra',respostaId:77};await confirmar(item);assert.equal(item.reutilizada,true);
  assert.equal(avisos[0],'confirmada');assert.match(avisos[1],/copiar/);
});

test('retomar a consulta mantém a mesma chave de confirmação após recarregar',async()=>{
  const chaves=[];const {confirmar}=montar(async(url,opcoes)=>{chaves.push(JSON.parse(opcoes.body).chave);return {ok:true,json:async()=>({estado:'REUTILIZADA',resposta:'Texto'})};});
  await confirmar({question:'Regra',respostaId:77,interacaoId:41});
  await confirmar({question:'Regra',respostaId:77,interacaoId:41});
  await confirmar({question:'Regra',respostaId:77,interacaoId:42});
  assert.equal(chaves[0],chaves[1]);assert.notEqual(chaves[0],chaves[2]);
});

test('bloqueio do armazenamento local não transforma confirmação real em falha',()=>{
  const avisos=[],reutilizacoes=[];let atualizacoes=0;
  const contexto={avisos,reutilizacoes,window:{},localStorage:{setItem:()=>{throw Error('Armazenamento bloqueado');}},atualizarEconomia:()=>atualizacoes++,notify:m=>avisos.push(m)};
  vm.createContext(contexto);
  vm.runInContext(tela.slice(tela.indexOf('      function registrarReutilizacao(item)'),tela.indexOf('      const confirmacoesPendentes'))+';registrarReutilizacao({question:"Regra"});',contexto);
  assert.equal(reutilizacoes.length,1);assert.equal(atualizacoes,1);assert.match(avisos[0],/confirmada no servidor/);
});
