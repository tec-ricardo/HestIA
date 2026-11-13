// Cenários controlados: executado dentro do container de frontend da validação.
const fs=require('node:fs');
const {Client}=require('/app/node_modules/pg');
const base='http://backend:8080';const checks=[];
function check(nome,cond){checks.push({nome,aprovado:!!cond});if(!cond)throw Error('Cenário não aprovado: '+nome);}
async function api(rota,token,body,metodo=body?'POST':'GET',esperado=200){
 const r=await fetch(base+rota,{method:metodo,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(15000)});
 const raw=await r.text();if(r.status!==esperado)throw Error(rota+': HTTP '+r.status+' (esperado '+esperado+')');
 if(!raw)return null;return JSON.parse(raw);
}
async function main(){
 const db=new Client({connectionString:process.env.SESSION_DATABASE_URL});await db.connect();
 try{
 const admin=await api('/auth/login',null,{email:'admin.demo@hestia.local',senha:'Hestia@2026'});
 const aluno=await api('/auth/login',null,{email:'colaborador.demo@hestia.local',senha:'Hestia@2026'});
 const gestor=await api('/auth/login',null,{email:'gestor.demo@hestia.local',senha:'Hestia@2026'});
 const user=(await db.query("SELECT id,empresa_id FROM usuarios WHERE email='colaborador.demo@hestia.local'")).rows[0];
 const ferramenta=(await db.query("SELECT id FROM ferramentas_ia WHERE empresa_id=$1 AND status='APROVADA' ORDER BY id LIMIT 1",[user.empresa_id])).rows[0].id;
 check('saude-api-inclui-banco-e-alertas',(await api('/actuator/health')).status==='UP');
 check('catalogo-sem-fallback-ficticio',(await api('/aprendizagem/conteudos',aluno.token)).length===0);
 await api('/catalogo-treinamentos',aluno.token,null,'GET',403);check('curadoria-restrita-ao-admin',true);
 const conteudo='Política didática HestIA v1.1: não enviar dados pessoais, senhas ou informações confidenciais à IA. Para demonstrações, usar dados fictícios. Conferir fatos e números e procurar o responsável em caso de dúvida. Material de ensaio, não política oficial da QSoft.';
 const questoes=Array.from({length:5},(_,i)=>({id:'Q'+(i+1),enunciado:['Que dados usar na demonstração?','O que fazer com senhas?','O que fazer antes de usar a resposta?','O que fazer em dúvida?','Este curso é uma política real da QSoft?'][i],alternativas:{A:['Dados fictícios','Não enviar à IA','Conferir fatos e números','Consultar o responsável','Não; é material didático'][i],B:'Enviar dados pessoais sem autorização',C:'Ignorar a política'},correta:'A',explicacao:'Aplicação do texto didático aprovado para a demonstração.'}));
 const curso=await api('/catalogo-treinamentos',admin.token,{anteriorId:null,titulo:'Demonstração HestIA — cuidados com dados — SCRUM-568',descricao:'Curso de ensaio da política fictícia v1.1, sem dados empresariais reais.',conteudo,tema:'LGPD',duracaoMinutos:5,fonte:'docs/scrum/SCRUM-543-politica-demo-v1.1.md',autorizacaoUso:'Uso didático da política fictícia autorizado pelo usuário em 12/11/2026; sem atribuição à QSoft real.',objetivo:'Identificar dados proibidos e aplicar validação humana no cenário de demonstração.',publico:'Contas de demonstração HestIA',atividade:'Revisar um prompt fictício, remover dados pessoais e justificar a decisão.',criterioAceite:'Usar dados fictícios e acertar pelo menos quatro das cinco questões alinhadas ao texto.',questoes});
 check('rascunho-nao-entra-na-listagem',(await api('/aprendizagem/conteudos',aluno.token)).length===0);
 await api('/catalogo-treinamentos/'+curso.id+'/estado',admin.token,{estado:'EM_REVISAO',motivo:'Ensaio controlado'});
 await api('/catalogo-treinamentos/'+curso.id+'/estado',admin.token,{estado:'PUBLICADO',motivo:'Conferido neste ensaio: texto didático próprio, fonte v1.1, cinco questões coerentes e autorização explícita do usuário para demonstração.'});
 await api('/catalogo-treinamentos/'+curso.id+'/estado',admin.token,{estado:'PUBLICADO',motivo:'Reenvio controlado'});
 const lista=await api('/aprendizagem/conteudos',aluno.token);const publicado=lista.find(c=>c.id===curso.id);
 check('publicacao-usa-catalogo-persistido-e-uma-prova',!!publicado&&publicado.catalogoPersistido===true&&Number((await db.query('SELECT count(*) FROM aprendizagem_catalogo WHERE empresa_id=$1 AND treinamento_id=$2',[user.empresa_id,curso.id])).rows[0].count)===1);
 const registro={usuarioId:Number(user.id),ferramentaId:Number(ferramenta),modeloIa:'cenario-controlado-SCRUM-568',dataHora:null,finalidade:'PESQUISA',tokensEntrada:10,tokensSaida:5,custoEstimado:null,statusExecucao:'SUCESSO',tempoRespostaMs:1,evidencias:['FINALIDADE_ADEQUADA','DADOS_SENSIVEIS','VALIDACAO_HUMANA','LGPD'].map(criterio=>({criterio,estado:'ATENDIDO',referencia:'SIMULAÇÃO SCRUM-568: aplicação conferida da política didática v1.1; dado fictício, sem envio a provedor externo.'}))};
 await api('/registros-uso-ia',aluno.token,registro,'POST',403);check('colaborador-nao-forja-conferencia-conclusiva',true);
 const semEvidencia=await api('/registros-uso-ia',aluno.token,{...registro,evidencias:[]},'POST',201);
 const pendente=(await api('/avaliacoes-automaticas',aluno.token)).find(r=>Number(r.registro_id)===semEvidencia.id);
 check('ausencia-de-evidencia-vira-revisao-sem-reprovar',pendente?.estado==='CONCLUIDA'&&pendente.resultado==='REQUER_REVISAO');
 const fundamentos=await api('/avaliacoes-automaticas/'+semEvidencia.id+'/fundamentos',aluno.token);
 check('revisao-preserva-criterios-nao-verificados',fundamentos.criterios.some(x=>x.estado==='NAO_VERIFICADO'));
 await api('/avaliacoes-automaticas/'+semEvidencia.id+'/tentar-novamente',aluno.token,{},'POST',403);check('retentativa-exige-perfil-autorizado',true);
 check('revisao-nao-gera-xp-de-conformidade',Number((await db.query("SELECT count(*) FROM movimentos_experiencia WHERE usuario_id=$1 AND referencia LIKE 'AVALIACAO_CONFORME:%'",[user.id])).rows[0].count)===0);
 const uso=await api('/registros-uso-ia',admin.token,registro,'POST',201);
 let avaliacao=(await api('/avaliacoes-automaticas',admin.token)).find(r=>Number(r.registro_id)===uso.id);
 check('registro-dispara-avaliacao-conforme-com-evidencias',avaliacao?.estado==='CONCLUIDA'&&avaliacao.resultado==='CONFORME');
 await api('/avaliacoes-automaticas/'+uso.id+'/tentar-novamente',gestor.token,{});
 check('retentativa-nao-duplica-avaliacao-ou-recompensa',Number((await db.query('SELECT count(*) FROM avaliacoes_conformidade WHERE registro_uso_id=$1',[uso.id])).rows[0].count)===1&&Number((await db.query("SELECT count(*) FROM movimentos_experiencia WHERE usuario_id=$1 AND referencia LIKE 'AVALIACAO_CONFORME:%'",[user.id])).rows[0].count)===1);
 const violacao=await api('/registros-uso-ia',admin.token,{...registro,evidencias:[{criterio:'LGPD',estado:'NAO_ATENDIDO',referencia:'SIMULAÇÃO: prompt de ensaio contém dado pessoal fictício e contraria a política didática; cenário controlado, não incidente real.'}]},'POST',201);
 avaliacao=(await api('/avaliacoes-automaticas',admin.token)).find(r=>Number(r.registro_id)===violacao.id);
 check('violacao-comprovada-cria-tratamento',avaliacao?.resultado==='NAO_CONFORME'&&Number((await db.query('SELECT count(*) FROM tratamentos_nao_conformidade WHERE avaliacao_id=$1',[avaliacao.avaliacao_id])).rows[0].count)===1);
 check('trilha-usa-o-curso-publicado-do-tema',Number((await db.query("SELECT count(*) FROM trilha_ciclos_automaticos c JOIN trilha_aprendizagem_requisitos r ON r.trilha_id=(SELECT trilha_id FROM participacoes_trilha p WHERE p.id=c.participacao_id) WHERE c.usuario_id=$1 AND c.tema='LGPD' AND r.treinamento_id=$2",[user.id,curso.id])).rows[0].count)>=1);
 for(let i=1;i<=publicado.totalEtapas;i++)await api('/aprendizagem/conteudos/'+curso.id+'/progresso',aluno.token,{etapa:i,chave:'568-'+i});
 const tentativa=await api('/aprendizagem/catalogo/'+publicado.catalogoId+'/tentativas',aluno.token,{});
 const final=await api('/aprendizagem/tentativas/'+tentativa.id+'/respostas',aluno.token,{respostas:questoes.map(q=>({questaoId:q.id,alternativa:'A'}))});
 check('aprendizagem-corrigida-no-servidor',final.status==='APROVADA'&&final.nota===100);
 const ciclo=(await api('/trilhas/usuarios/'+user.id+'/ciclos',aluno.token)).find(x=>x.tema==='LGPD');
 const participacao=await api('/trilhas/participacoes/'+ciclo.participacao_id,aluno.token);
 check('aprovacao-conclui-trilha-da-ocorrencia',participacao.status==='CONCLUIDA');
 const movimentosAntes=Number((await db.query('SELECT count(*) FROM movimentos_experiencia WHERE usuario_id=$1',[user.id])).rows[0].count);
 await api('/aprendizagem/tentativas/'+tentativa.id+'/respostas',aluno.token,{respostas:questoes.map(q=>({questaoId:q.id,alternativa:'A'}))});
 check('reenvio-da-prova-nao-duplica-xp',Number((await db.query('SELECT count(*) FROM movimentos_experiencia WHERE usuario_id=$1',[user.id])).rows[0].count)===movimentosAntes);

 const fonte=(await db.query('SELECT r.*,v.numero_versao,v.conteudo_sha256 FROM respostas_reutilizaveis r JOIN resposta_versoes v ON v.resposta_id=r.id WHERE r.empresa_id=$1 ORDER BY r.id,v.numero_versao DESC LIMIT 1',[user.empresa_id])).rows[0];
 const conversa=await api('/api/rag/conversas',aluno.token,{titulo:'Ensaio longo SCRUM-568'});
 const fontes=[{id:Number(fonte.id),titulo:fonte.prompt_original,trecho:fonte.resposta_gerada,versao:fonte.numero_versao,conteudoSha256:fonte.conteudo_sha256,criadaEm:null,origem:'DEMO',similaridade:null}];
 for(let i=0;i<18;i++)await db.query("INSERT INTO interacoes_rag(empresa_id,usuario_id,chave,pergunta,modo,estado,resposta,fontes,conversa_id,criada_em) VALUES($1,$2,$3,$4,'CONSULTAR','RECUPERADA',$5,$6,$7,CURRENT_TIMESTAMP)",[user.empresa_id,user.id,'568-fixture-historico-'+i,'Pergunta controlada '+i,fonte.resposta_gerada,JSON.stringify(fontes),conversa.id]);
 const limites=await api('/api/rag/conversas/'+conversa.id+'/contexto',aluno.token);
 check('historico-longo-com-resumo-dentro-do-orcamento',limites.caracteresSelecionados<=limites.maxCaracteres&&limites.turnosResumidos>0);
 await api('/api/rag/conversas/'+conversa.id+'/contexto',admin.token,null,'GET',404);check('limites-da-conversa-isolados-por-autor',true);
 await api('/api/respostas-reutilizaveis/'+fonte.id+'/perfis',admin.token,['ADMIN'],'PUT',204);
 const revogado=await api('/api/rag/conversas/'+conversa.id+'/contexto',aluno.token);
 check('fonte-revogada-nao-volta-em-resumo',revogado.turnosCompletos===0&&revogado.turnosTruncados===0&&revogado.turnosResumidos===0);
 await api('/api/respostas-reutilizaveis/'+fonte.id+'/perfis',admin.token,['ADMIN','GESTOR','FUNCIONARIO'],'PUT',204);
 await api('/catalogo-treinamentos/'+curso.id+'/estado',admin.token,{estado:'RETIRADO',motivo:'Fim do cenário controlado de homologação'});
 check('retirada-preserva-avaliacao-e-historico',(await api('/aprendizagem/conteudos',aluno.token)).length===0&&Number((await db.query("SELECT count(*) FROM aprendizagem_tentativas WHERE id=$1 AND status='APROVADA'",[tentativa.id])).rows[0].count)===1);
 console.log(JSON.stringify({base:'f71021d',tarefa:'SCRUM-568',data:new Date().toISOString(),natureza:'ENSAIO_DIDATICO_LOCAL_DOCKER',checks,cursoId:curso.id,registros:[uso.id,violacao.id],conversaId:conversa.id,limites},null,2));
 }finally{await db.end();}
}
main().catch(e=>{console.error(JSON.stringify({erro:e.message,checks}));process.exitCode=1;});
