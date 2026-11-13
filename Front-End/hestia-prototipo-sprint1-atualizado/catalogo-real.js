const express=require('express');
const {randomBytes}=require('node:crypto');
module.exports=(app,api)=>{
 const admin=(req,res,next)=>req.session?.perfil==='administrador'?next():res.sendStatus(403);
 app.get('/catalogo-real',admin,async(req,res)=>{
  req.session.catalogoCsrf ||= randomBytes(32).toString('hex');
  try {res.render('catalogo-real',{perfil:req.session.perfil,email:req.session.email,active:'catalogo-real',itens:(await api.get('/catalogo-treinamentos')).data,csrf:req.session.catalogoCsrf});}
  catch{res.status(503).send('Catálogo indisponível. Tente novamente.');}
 });
 app.get('/catalogo-real/:id',admin,async(req,res)=>{
  if(!/^\d+$/.test(req.params.id))return res.sendStatus(400);
  try{res.render('catalogo-detalhe',{perfil:req.session.perfil,email:req.session.email,active:'catalogo-real',item:(await api.get('/catalogo-treinamentos/'+req.params.id)).data});}
  catch(e){res.status(e.response?.status||503).send('Conteúdo indisponível para consulta.');}
 });
 const seguro=(req,res,next)=>req.session.catalogoCsrf&&req.get('X-CSRF-Token')===req.session.catalogoCsrf?next():res.sendStatus(403);
 app.post('/api/catalogo-real',admin,express.json({limit:'512kb'}),seguro,async(req,res)=>{
  try{res.status(201).json((await api.post('/catalogo-treinamentos',req.body)).data);}
  catch(e){res.status(e.response?.status||503).json({erro:e.response?.data?.detail||'Não foi possível salvar. Confira os campos e tente novamente.'});}
 });
 app.post('/api/catalogo-real/:id/estado',admin,express.json({limit:'8kb'}),seguro,async(req,res)=>{
  if(!/^\d+$/.test(req.params.id))return res.sendStatus(400);
  try{res.json((await api.post('/catalogo-treinamentos/'+req.params.id+'/estado',req.body)).data);}
  catch(e){res.status(e.response?.status||503).json({erro:e.response?.data?.detail||'Não foi possível alterar o estado.'});}
 });
};
