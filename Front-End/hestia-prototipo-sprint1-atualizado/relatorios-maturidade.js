module.exports=(app,api)=>{
 const autorizado=(req,res,next)=>['administrador','gestor'].includes(req.session?.perfil)?next():res.sendStatus(403);
 const permitidos={resultados:['inicio','fim','fonte','snapshot','departamentoId','departamentoOrigem','tipoIssue'],evolucao:['anteriorId','atualId']};
 const filtros=(q,tipo)=>Object.fromEntries(permitidos[tipo].filter(k=>q[k]).map(k=>[k,String(q[k])]));
 const linhas=(valor,caminho='',saida=[])=>{if(valor!==null&&typeof valor==='object'){for(const [k,v] of Object.entries(valor))linhas(v,caminho?caminho+'.'+k:k,saida);}else saida.push({campo:caminho,valor:valor??'Não disponível'});return saida;};
 app.get('/relatorios-maturidade',autorizado,async(req,res)=>{
  const tipo=req.query.tipo==='evolucao'?'evolucao':'resultados',params=filtros(req.query,tipo);let itens=[],erro='';
  if(Object.keys(params).length)try{itens=linhas((await api.get('/relatorios-maturidade/'+tipo,{params})).data);}catch(e){return res.status(e.response?.status||503).send('Consulta indisponível. Confira os filtros e seu escopo de acesso.');}
  res.render('relatorios-maturidade',{perfil:req.session.perfil,email:req.session.email,active:'relatorios-uso',tipo,filtros:params,itens,consulta:new URLSearchParams({tipo,...params}).toString()});
 });
 app.get('/relatorios-maturidade/exportar',autorizado,async(req,res)=>{
  const tipo=req.query.tipo==='evolucao'?'evolucao':'resultados';try{const r=await api.get('/relatorios-maturidade/'+tipo+'.csv',{params:filtros(req.query,tipo),responseType:'text'});res.set('Content-Type','text/csv;charset=UTF-8');res.set('Content-Disposition','attachment; filename='+tipo+'.csv');res.set('Cache-Control','no-store');res.send(r.data);}catch(e){res.status(e.response?.status||503).send('Exportação indisponível. Confira filtros e permissões.');}
 });
};
