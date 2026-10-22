const { loginApi } = require('./sessao-api');
const dataLocal = d => [d.getFullYear(), String(d.getMonth()+1).padStart(2,'0'), String(d.getDate()).padStart(2,'0')].join('-');

async function carregarConfianca(api, sessao, parametros) {
    if (!sessao.apiToken) return {status:200, dados:null, erro:'Acesso demonstrativo. Entre com uma conta cadastrada para consultar indicadores reais.'};
    const params = Object.fromEntries(['inicio','fim','visao','versao'].filter(k => parametros[k] !== undefined).map(k => [k,parametros[k]]));
    try {
        const {data} = await api.get('/indicadores/confiabilidade', {params, headers:{Authorization:`Bearer ${sessao.apiToken}`}});
        if (!data?.periodo || !Array.isArray(data.indicadores) || !data.versaoConfianca) throw new Error('Contrato de indicadores inválido');
        return {status:200,dados:data,erro:null};
    } catch (e) {
        const status=e.response?.status;
        if (status===401) delete sessao.apiToken;
        const mensagens={400:'Confira as datas e a visão escolhida.',401:'Sua sessão da API expirou. Entre novamente.',403:'Seu perfil não permite consultar esse escopo.',404:'A versão ou o serviço de indicadores não está disponível.'};
        return {status:mensagens[status]?status:503,dados:null,erro:mensagens[status] || 'Indicadores indisponíveis. Tente novamente; nenhum valor demonstrativo foi usado.'};
    }
}

function criarRotaMaturidade(api) {
    return async (req,res,next) => {
        try {
            const hoje=new Date(), primeiro=new Date(hoje);
            primeiro.setDate(primeiro.getDate()-29);
            const gerencial=['administrador','gestor'].includes(req.session.perfil);
            const inicio=String(req.query.inicio || dataLocal(primeiro)), fim=String(req.query.fim || dataLocal(hoje));
            const visao=String(req.query.visao || (gerencial?'GERENCIAL':'PESSOAL'));
            const resultado=await carregarConfianca(api,req.session,{inicio,fim,visao});
            res.set('Cache-Control','no-store').status(resultado.status).render('maturidade', {
                perfil:req.session.perfil,email:req.session.email,active:'maturidade',inicio,fim,visao,gerencial,dados:resultado.dados,erro:resultado.erro
            });
        } catch(e) { next(e); }
    };
}
module.exports={autenticarContaReal:loginApi,carregarConfianca,criarRotaMaturidade};
