const {randomBytes}=require('node:crypto');
module.exports=(app,api)=>{
 const tela=(req,res,erro='',ok=false,status=200)=>{req.session.recuperacaoCsrf ||= randomBytes(32).toString('hex');res.set('Cache-Control','no-store');res.set('Referrer-Policy','no-referrer');res.status(status).render('recuperacao-senha',{redefinir:req.path==='/redefinir-senha',csrf:req.session.recuperacaoCsrf,erro,ok});};
 app.get('/recuperar-senha',(req,res)=>tela(req,res));
 app.get('/redefinir-senha',(req,res)=>{if(req.query.token){req.session.tokenRecuperacao=String(req.query.token);return res.redirect('/redefinir-senha');}tela(req,res);});
 for(const rota of ['/recuperar-senha','/redefinir-senha'])app.post(rota,async(req,res)=>{
  if(!req.session.recuperacaoCsrf||req.body.csrf!==req.session.recuperacaoCsrf)return res.sendStatus(403);
  try{
   if(rota==='/recuperar-senha')await api.post('/auth/recuperacao',{email:req.body.email});
   else {if(!req.session.tokenRecuperacao||String(req.body.senha||'').length<8||String(req.body.senha).length>100)return tela(req,res,'Link ou senha inválidos.',false,400);
    await api.post('/auth/recuperacao/redefinir',new URLSearchParams({token:req.session.tokenRecuperacao,novaSenha:req.body.senha}),{headers:{'Content-Type':'application/x-www-form-urlencoded'}});delete req.session.tokenRecuperacao;}
   tela(req,res,'',true);
  }catch{tela(req,res,'Não foi possível concluir. Verifique o link ou tente novamente mais tarde.',false,503);}
 });
};
