const {carregarOrigem}=require('./origem-local');
const {aplicarCenario}=require('./cenario-maturidade-demo');
function selecionarCenario(req,dados) {
    if(!req.query.cenario)return dados;
    if(req.query.cenario!=='demo-v1')throw new RangeError('Cenário desconhecido.');
    return aplicarCenario(dados);
}

function registrarDemonstracaoBase(app) {
    if(app.locals)app.locals.resumoBaseOriginal=()=>{
        try {const d=carregarOrigem();return {empresa:d.empresa.company_name,periodo:d.periodo,
            literacy:d.total.fatores.find(f=>f.codigo==='aiLiteracy'),
            governanca:d.total.fatores.find(f=>f.codigo==='governanca')};}catch(_){return null;}
    };
    app.get('/maturidade/resultados',(req,res,next)=>{
        if(req.query.fonte!=='SINTETICA')return next();
        if(!['administrador','gestor'].includes(req.session?.perfil))return res.sendStatus(403);
        try {
            const dados=selecionarCenario(req,carregarOrigem());
            if(req.query.inicio!==dados.periodo.inicio||req.query.fim!==dados.periodo.fim)return next();
            if(req.query.snapshot&&req.query.snapshot!==dados.origem.sha256)return next();
            // IDs numericos operacionais nao sao IDs de origem; nao assumir equivalencias.
            if(req.query.departamentoId||req.query.tipoIssue)return next();
            const dep=req.query.departamentoOrigem;
            const recorte=dep?dados.departamentos.find(d=>d.department_id===dep):dados.total;
            if(!recorte)return res.status(400).send('Departamento ausente na fonte original.');
            return res.set('Cache-Control','no-store').render('demonstracao-base',{
                perfil:req.session.perfil,email:req.session.email,active:'maturidade',dados,recorte,
                departamento:dep||'',formatar:n=>n==null?'Não disponível':Number(n).toLocaleString('pt-BR',{maximumFractionDigits:2})});
        }catch(_){return res.status(503).send('Base original indisponível; nenhuma nota foi estimada.');}
    });
    app.get('/maturidade/demonstracao', (req,res) => {
        if(!['administrador','gestor'].includes(req.session?.perfil))return res.sendStatus(403);
        let dados;try{dados=selecionarCenario(req,carregarOrigem());}catch(_){return res.status(503).send('Base ou cenário demonstrativo indisponível.');}
        const departamento = req.query.departamento;
        const recorte = departamento ? dados.departamentos.find(d=>d.department_id===departamento) : dados.total;
        if(!recorte)return res.status(400).send('Departamento ausente na base demonstrativa C01.');
        // Fonte didatica fixa e explicita: nunca apresentada como dados do tenant autenticado.
        return res.set('Cache-Control','no-store').render('demonstracao-base', {
            perfil:req.session.perfil,email:req.session.email,active:'maturidade',dados,recorte,
            departamento:departamento||'',formatar:n=>n==null?'Não disponível':Number(n).toLocaleString('pt-BR',{maximumFractionDigits:2})
        });
    });
    app.get('/custos',(req,res)=>{
        if(!['administrador','gestor'].includes(req.session?.perfil))return res.sendStatus(403);
        try {
            const painel=require('./custos-base').calcularPainel(carregarOrigem(),req.query);
            return res.set('Cache-Control','no-store').render('custos',{
                perfil:req.session.perfil,email:req.session.email,active:'custos',painel,
                dinheiro:n=>n==null?'Não disponível':n.toLocaleString('pt-BR',{style:'currency',currency:painel.moedaExibicao}),
                numero:n=>n==null?'Não disponível':n.toLocaleString('pt-BR',{maximumFractionDigits:2}),
                jsonSeguro:obj=>JSON.stringify(obj).replace(/</g,'\\u003c')
            });
        } catch(erro) {
 res.set('Cache-Control','no-store');
 if(erro instanceof RangeError)return res.status(400).send('Filtros de custos ou premissas invalidos. Confira mes, moeda e departamento.');
 return res.status(503).send('Nao foi possivel consultar os custos. Tente novamente mais tarde.');
 }
    });
    app.get('/mapa-risco',(req,res,next)=>{
        if(['OPERACIONAL','DEMO'].includes(req.query.fonte))return next();
        if(!['administrador','gestor'].includes(req.session?.perfil))return res.sendStatus(403);
        try {
            const dados=selecionarCenario(req,carregarOrigem()),mes=String(req.query.mes||'');
            if(mes&&!dados.financeiro.meses.includes(mes))return res.status(400).send('Mês ausente na base original.');
            const linhas=dados.departamentos.map(d=>{
                const mensal=d.evolucao.filter(m=>!mes||m.mes===mes);
                return {nome:d.department_name,eventos:mensal.reduce((s,m)=>s+m.eventos,0),
                    requisicoes:mensal.reduce((s,m)=>s+m.requisicoes,0),
                    governanca:mes?null:d.fatores.find(f=>f.codigo==='governanca').nota,
                    eventosSeguranca:mes?null:d.eventosSeguranca};
            });
            return res.set('Cache-Control','no-store').render('risco-base',{perfil:req.session.perfil,email:req.session.email,
                active:'mapa-risco',dados,linhas,mes,numero:n=>n==null?'Não disponível':Number(n).toLocaleString('pt-BR',{maximumFractionDigits:2})});
        }catch(_){return res.status(503).send('Base original indisponível; nenhum risco foi estimado.');}
    });
}
module.exports={registrarDemonstracaoBase};
