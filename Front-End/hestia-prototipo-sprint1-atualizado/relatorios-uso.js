module.exports=(app,api)=>{
 const autorizado=(req,res,next)=>['administrador','gestor'].includes(req.session?.perfil)?next():res.sendStatus(403);
 const filtros=q=>Object.fromEntries(['inicio','fim','departamentoId','ferramentaId','finalidade'].filter(k=>q[k]).map(k=>[k,q[k]]));
 app.get('/relatorios-uso',autorizado,async(req,res)=>{const params=filtros(req.query);try{res.render('relatorios-uso',{perfil:req.session.perfil,email:req.session.email,active:'relatorios-uso',itens:(await api.get('/relatorios-uso',{params})).data,filtros:params,consulta:new URLSearchParams(params).toString()});}catch(e){res.status(e.response?.status||503).send('Relatório indisponível. Verifique período e permissões.');}});
 app.get('/relatorios-uso/exportar',autorizado,async(req,res)=>{try{const r=await api.get('/relatorios-uso/exportar',{params:filtros(req.query),responseType:'text'});res.set('Content-Type','text/csv;charset=UTF-8');res.set('Content-Disposition','attachment; filename=relatorio-usos.csv');res.send(r.data);}catch(e){res.status(e.response?.status||503).send('Exportação indisponível.');}});
};
