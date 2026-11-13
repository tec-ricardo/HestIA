const { randomBytes } = require('node:crypto');
module.exports = (app,api) => {
 app.get('/ciencia-politica',async(req,res)=>{
  req.session.cienciaCsrf ||= randomBytes(32).toString('hex');
  try { const [atuais,historico]=await Promise.all([api.get('/ciencias-politica'),api.get('/ciencias-politica/historico')]);
   res.render('ciencia-politica',{perfil:req.session.perfil,email:req.session.email,active:'ciencia-politica',atuais:atuais.data,historico:historico.data,csrf:req.session.cienciaCsrf});
  } catch {res.status(503).send('Não foi possível consultar as políticas. Tente novamente.');}
 });
 app.post('/ciencia-politica/:id',async(req,res)=>{
  if(!req.session.cienciaCsrf || req.body.csrf!==req.session.cienciaCsrf)return res.sendStatus(403);
  if(!/^\d+$/.test(req.params.id))return res.sendStatus(400);
  try{await api.post('/ciencias-politica/'+req.params.id);res.redirect('/ciencia-politica');}
  catch{res.status(409).send('Não foi possível confirmar a ciência. Consulte novamente as políticas vigentes.');}
 });
};
