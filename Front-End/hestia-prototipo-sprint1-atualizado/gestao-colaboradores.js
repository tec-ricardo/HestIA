const {analisarColaborador}=require('./diagnostico-gerencial');
const {filtrosPainel}=require('./dashboard-executivo');
function limiteAcompanhamento(v){
 const n=Number(v??process.env.LIMITE_TOKENS_ACOMPANHAMENTO??0);
 return Number.isSafeInteger(n)&&n>=0?n:0;
}
async function carregarPainel(api,limite=limiteAcompanhamento(),filtros={dias:30,departamentoId:null}){
 const {data}=await api.get('/gestao/colaboradores',{params:{dias:filtros.dias,...(filtros.departamentoId?{departamentoId:filtros.departamentoId}:{})}});
 if(!data||!Array.isArray(data.colaboradores))throw Error('Resposta inválida');
 const colaboradores=data.colaboradores.map(c=>analisarColaborador(c,limite)).sort((a,b)=>b.alertas.length-a.alertas.length);
 return {...data,colaboradores,limite,comAlertas:colaboradores.filter(c=>c.alertas.length).length,
  tokens:colaboradores.reduce((n,c)=>n+Number(c.tokens||0),0),
  usos:colaboradores.reduce((n,c)=>n+Number(c.utilizacoes||0),0)};
}
function registrarRotasGestao(app,api){
 const gerencial=req=>['gestor','administrador'].includes(req.session?.perfil);
 const renderizar=async(req,res,custos=false)=>{
  if(!gerencial(req))return res.sendStatus(403);
  try{
   if(req.query.limite!==undefined) req.session.limiteAcompanhamento=limiteAcompanhamento(req.query.limite);
   const painel=await carregarPainel(api,limiteAcompanhamento(req.session.limiteAcompanhamento),filtrosPainel(req.query));
   return res.set('Cache-Control','no-store').render('gestao-colaboradores',{
    perfil:req.session.perfil,email:req.session.email,active:custos?'custos':'gestao-colaboradores',painel,custos});
  }catch(e){return res.status(e.response?.status||503).send('Não foi possível consultar o escopo autorizado.');}
 };
 app.get('/gestao-colaboradores',(req,res)=>renderizar(req,res));
 app.get('/custos',(req,res)=>renderizar(req,res,true));
 app.get('/ranking/:id/xp',async(req,res)=>{
  const id=Number(req.params.id);
  if(!Number.isSafeInteger(id)||id<=0)return res.sendStatus(400);
  try{
   const {data}=await api.get('/gamificacao/usuarios/'+id+'/detalhamento-xp');
   return res.set('Cache-Control','no-store').render('detalhamento-xp',{
    perfil:req.session.perfil,email:req.session.email,active:req.session.perfil==='colaborador'?'ranking':'gestao-colaboradores',detalhe:data});
  }catch(e){return res.status(e.response?.status||503).send('Detalhamento indisponível neste escopo.');}
 });
}
module.exports={registrarRotasGestao,carregarPainel,limiteAcompanhamento};
