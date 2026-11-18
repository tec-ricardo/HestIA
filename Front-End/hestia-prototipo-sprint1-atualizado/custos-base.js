const arred=n=>Math.round((n+Number.EPSILON)*100)/100;
function calcularPainel(dados,query={}) {
    const f=dados.financeiro,mes=String(query.mes||''),moeda=String(query.moeda||'BRL'),departamento=String(query.departamento||'');
    if(!['BRL','USD','SIMULADO'].includes(moeda))throw new RangeError('Moeda inválida.');
    if(mes&&!f.meses.includes(mes))throw new RangeError('Mês ausente na base original.');
    if(departamento&&!dados.departamentos.some(d=>d.department_id===departamento))throw new RangeError('Departamento ausente na base original.');
    const cambio=dados.premissas.cambioBRLPorUSD;
    const simulado=moeda==='SIMULADO';
    if(simulado&&(!(cambio>0)||dados.premissas.moedaOrcamento!=='BRL'))throw new RangeError('Configure as premissas da simulação.');
    const noEscopo=r=>(!mes||r.mes===mes)&&(!departamento||r.departamentoId===departamento);
    const custos=f.custos.filter(noEscopo).filter(r=>simulado?['BRL','USD'].includes(r.moeda):r.moeda===moeda);
    const valor=r=>r.custo*(simulado&&r.moeda==='USD'?cambio:1);
    const usos=f.uso.filter(noEscopo).filter(r=>f.meses.includes(r.mes));
    const soma=(xs,fn)=>xs.reduce((s,r)=>s+fn(r),0);
    const total=soma(custos,valor),eventos=soma(usos,r=>r.eventos),sucessos=soma(usos,r=>r.sucessos);
    const departamentos=dados.departamentos.filter(d=>!departamento||d.department_id===departamento).map(d=>{
        const cs=custos.filter(r=>r.departamentoId===d.department_id),us=usos.filter(r=>r.departamentoId===d.department_id);
        const n=soma(us,r=>r.eventos),ok=soma(us,r=>r.sucessos);
        return {id:d.department_id,nome:d.department_name,custo:cs.length?arred(soma(cs,valor)):null,eventos:n,
            requisicoes:soma(us,r=>r.requisicoes),tokens:soma(us,r=>r.tokens),sucesso:n?arred(100*ok/n):null,
            participacao:total>0?arred(100*soma(cs,valor)/total):null};
    });
    const meses=mes?[mes]:f.meses;
    const evolucao=meses.map(m=>{const cs=custos.filter(r=>r.mes===m),bud=f.orcamentos.filter(noEscopo).filter(r=>r.mes===m);
        return {mes:m,custo:cs.length?arred(soma(cs,valor)):null,orcamento:simulado&&bud.length?arred(soma(bud,r=>r.valor)):null};});
    const orcamentos=f.orcamentos.filter(noEscopo);
    const orcamento=simulado&&orcamentos.length?soma(orcamentos,r=>r.valor):null;
    const porFerramenta=f.ferramentas.map(t=>{const cs=custos.filter(r=>r.ferramentaId===t.ai_tool_id);return {nome:t.tool_name,custo:cs.length?arred(soma(cs,valor)):null};}).filter(r=>r.custo!=null).sort((a,b)=>b.custo-a.custo);
    return {empresa:dados.empresa,hash:dados.origem.sha256,mes,moeda,departamento,moedaExibicao:moeda==='USD'?'USD':'BRL',
        simulado,cambio,meses:f.meses,opcoesDepartamentos:dados.departamentos,total:custos.length?arred(total):null,eventos,
        requisicoes:soma(usos,r=>r.requisicoes),tokens:soma(usos,r=>r.tokens),sucesso:eventos?arred(100*sucessos/eventos):null,
        departamentos,evolucao,porFerramenta,orcamento:orcamento==null?null:arred(orcamento),
        percentualOrcamento:orcamento>0&&custos.length?arred(100*total/orcamento):null,
        custoMedioEvento:eventos&&custos.length?total/eventos:null,
        semDepartamento:arred(soma(custos.filter(r=>r.departamentoId==null),valor)),
        faturas:soma(custos,r=>r.faturas)};
}
module.exports={calcularPainel};
