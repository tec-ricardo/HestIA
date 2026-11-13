const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const pagina=fs.readFileSync(path.join(__dirname,'../views/assistente-rag.ejs'),'utf8');
function carregar(linhas){
 const inicio=pagina.indexOf('function mensagemDoServidor(');
 const fim=pagina.indexOf('async function carregarConversa(',inicio);
 const grupo=pagina.indexOf('        const grupos = new Map();');
 const fimGrupo=pagina.indexOf('        renderHistory();',grupo);
 assert.ok(inicio>=0&&fim>inicio&&grupo>=0&&fimGrupo>grupo);
 const contexto=vm.createContext({linhas,conversations:null});
 vm.runInContext(pagina.slice(inicio,fim)+'\n'+pagina.slice(grupo,fimGrupo),contexto);
 return JSON.parse(JSON.stringify(contexto.conversations));
}
const mensagem=(id,conversaId)=>({id,conversaId,pergunta:'Pergunta '+id,resposta:'Resposta '+id,criadaEm:'2026-11-12T20:00:00',estado:'RECUPERADA',fontes:[{id:77,titulo:'Política',versao:1}],confianca:'NAO_CLASSIFICADA'});
test('recarregar agrupa mensagens pela conversa e mantém o ID exato para excluir uma interação',()=>{
 const grupos=carregar([mensagem(3,20),mensagem(2,10),mensagem(1,10)]);
 assert.equal(grupos.length,2);
 assert.equal(grupos[0].servidorId,10);
 assert.deepEqual(grupos[0].messages.map(m=>m.interacaoId),[1,1,2,2]);
 assert.ok(grupos[0].messages.every(m=>m.conversaId===10));
 assert.equal(grupos[1].messages[1].interacaoId,3);
});
test('resposta geral recarregada é identificada e não vira resposta reutilizável',()=>{
 const linha={...mensagem(1,10),estado:'GERADA_SEM_FONTES',fontes:[]};
 const resposta=carregar([linha])[0].messages[1];
 assert.match(resposta.policy,/Resposta geral sem fontes/);
 assert.equal(resposta.respostaId,null);
 assert.equal(resposta.source,null);
});
