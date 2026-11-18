function sequenciaAtual(registros,processamentos,usuarioId) {
    if(!Array.isArray(registros)||!Array.isArray(processamentos))return null;
    if(registros.some(r=>String(r.usuarioId)!==String(usuarioId)))throw Error('Escopo pessoal inconsistente');
    const indice=new Map(processamentos.map(p=>[String(p.registro_id),p]));
    const ordenados=[...registros].sort((a,b)=>String(b.dataHora).localeCompare(String(a.dataHora))||Number(b.id)-Number(a.id));
    let n=0;
    for(const r of ordenados){const p=indice.get(String(r.id));if(!p)return null;
        if(p.estado!=='CONCLUIDA'||p.resultado!=='CONFORME')return n;n++;}
    return n;
}
async function carregarPasse(api,usuarioId,adaptar) {
    const [core,resumo,registros,processamentos]=await Promise.allSettled([
        api.get(`/gamificacao/usuarios/${usuarioId}/passe`),api.get('/avaliacoes-conformidade/resumo'),
        api.get('/registros-uso-ia',{params:{usuarioId}}),api.get('/avaliacoes-automaticas')]);
    if(core.status!=='fulfilled'||!Number.isFinite(core.value.data?.xpTotal)||!Array.isArray(core.value.data?.missoes))throw Error('Passe indisponível');
    if(core.value.data.usuarioId!=null&&String(core.value.data.usuarioId)!==String(usuarioId))throw Error('Passe de outro usuário');
    const passe=adaptar(core.value.data,{reconhecimentoMaximo:{titulo:'Embaixador HestIA',requisitos:[
        'Alcançar 2.500 XP no passe.','Certificações de aprendizagem dependem dos requisitos publicados e de avaliações aprovadas.']}});
    if(resumo.status==='fulfilled'){
        const r=resumo.value.data;
        if(['avaliadas','conformes','nao_conformes','requerem_revisao'].every(k=>Number.isSafeInteger(Number(r?.[k]))&&Number(r[k])>=0)){
            passe.totalUtilizacoesAvaliadas=Number(r.avaliadas);passe.totalUtilizacoesConformes=Number(r.conformes);
            passe.taxaConformidade=r.percentualConformidade==null?null:Number(r.percentualConformidade);
            passe.requeremRevisao=Number(r.requerem_revisao);
        }
    }
    passe.sequenciaConforme=sequenciaAtual(registros.status==='fulfilled'?registros.value.data:null,
        processamentos.status==='fulfilled'?processamentos.value.data:null,usuarioId);
    // Nenhuma avaliacao conforme implica sequencia zero, inclusive no historico legado.
    // Se existirem conformes, exigir a ordem completa; nao inventar sequencia para lacunas.
    if(passe.sequenciaConforme==null&&passe.totalUtilizacoesConformes===0)passe.sequenciaConforme=0;
    passe.fonteIndicadores='Avaliações e processamentos operacionais do usuário autenticado';
    return passe;
}
function registrarPasseIntegrado(app,api,adaptar) {
    const permitido=(req,res)=>{if(req.session.perfil!=='colaborador'){res.status(403).send('Passe pessoal restrito ao colaborador.');return false;}
        if(!Number.isSafeInteger(Number(req.session.usuarioId))||Number(req.session.usuarioId)<=0){res.status(401).send('Sessão inválida.');return false;}return true;};
    app.get('/passe-hestia',async(req,res)=>{
        if(!permitido(req,res))return;
        try {const passe=await carregarPasse(api,Number(req.session.usuarioId),adaptar);
            return res.set('Cache-Control','no-store').render('passe-hestia',{perfil:req.session.perfil,email:req.session.email,active:'passe-hestia',passe});
        }catch(_){return res.set('Cache-Control','no-store').status(503).send('Não foi possível consultar seu Passe HestIA. Tente novamente; nenhum XP demonstrativo foi utilizado.');}
    });
    app.get('/api/passe-hestia/status',async(req,res)=>{
        if(!permitido(req,res))return;
        try {const p=await carregarPasse(api,Number(req.session.usuarioId),adaptar);
            return res.set('Cache-Control','no-store').json({xpTotal:p.xpAtual,nivel:p.nomeNivel,
                taxaConformidade:p.taxaConformidade,totalUtilizacoesAvaliadas:p.totalUtilizacoesAvaliadas,
                totalUtilizacoesConformes:p.totalUtilizacoesConformes,sequenciaConforme:p.sequenciaConforme});
        }catch(_){return res.status(503).json({erro:'Passe indisponível'});}
    });
}
module.exports={sequenciaAtual,carregarPasse,registrarPasseIntegrado};
