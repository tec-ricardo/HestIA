/* Node >= 22.13. Somente leitura; nunca importa pessoas em usuarios operacionais. */
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { DatabaseSync } = require('node:sqlite');
const PESOS = {aiLiteracy:.15, governanca:.15, usoResponsavel:.15, produtividade:.10,
    finOps:.10, seguranca:.20, colaboracao:.05, desenvolvimentoCompetencias:.10};
const media = xs => xs.length ? xs.reduce((a,b)=>a+b,0)/xs.length : null;
const mediana = xs => { const s=[...xs].sort((a,b)=>a-b), n=s.length; return n ? (s[Math.floor(n/2)]+s[Math.floor((n-1)/2)])/2 : null; };
const pct = (n,d) => d ? 100*n/d : null;
const arred = n => n == null ? null : Math.round((n+Number.EPSILON)*100)/100;
const numero = v => v != null && v !== '' && Number.isFinite(Number(v)) ? Number(v) : null;
const emPeriodo = (v,inicio,fim) => typeof v === 'string' && v.slice(0,10)>=inicio && v.slice(0,10)<=fim;
function gerar(db, op) {
    const {empresa='C01',inicio='2025-06-01',fim='2026-08-10',cambio=null,moedaOrcamento=null,metaDias=null}=op;
    if(!/^\d{4}-\d{2}-\d{2}$/.test(inicio)||!/^\d{4}-\d{2}-\d{2}$/.test(fim)||inicio>fim) throw Error('Periodo invalido');
    if(cambio!=null&&!(cambio>0)||metaDias!=null&&!(metaDias>0)) throw Error('Premissa numerica invalida');
    if(moedaOrcamento!=null&&moedaOrcamento!=='BRL') throw Error('A simulacao de orcamento suporta BRL explicito');
    // Billing mensal nao permite rateio diario sem outra hipotese.
    const primeiroMes=inicio.slice(0,7), ultimoMes=fim.slice(0,7);
    const mesesCompletos=m=>m>=primeiroMes&&m<=ultimoMes&&m+'-01'>=inicio&&
        new Date(Date.UTC(Number(m.slice(0,4)),Number(m.slice(5,7)),0)).toISOString().slice(0,10)<=fim;
    const company=db.prepare('SELECT * FROM companies WHERE company_id=?').get(empresa);
    if(!company) throw Error('Empresa ausente');
    const ler=t=>db.prepare('SELECT * FROM '+t+' WHERE company_id=?').all(empresa);
    const departamentos=ler('departments'), pessoas=ler('employees'), pessoasPorId=new Map(pessoas.map(p=>[p.employee_id,p]));
    const projetos=new Set(ler('projects').map(p=>p.project_id));
    const usos=ler('ai_usage_logs').filter(r=>emPeriodo(r.timestamp,inicio,fim));
    const treinos=ler('training_records');
    const eventos=ler('security_events').filter(r=>emPeriodo(r.timestamp,inicio,fim));
    const faturas=ler('ai_billing').filter(r=>mesesCompletos(r.billing_month));
    const orcamentos=ler('ai_budgets').filter(r=>mesesCompletos(`${r.year}-${String(r.month).padStart(2,'0')}`));
    const prs=db.prepare('SELECT * FROM pull_requests').all().filter(r=>projetos.has(r.project_id)&&emPeriodo(r.merged_at,inicio,fim));
    const issues=db.prepare('SELECT * FROM issues').all().filter(r=>projetos.has(r.project_id)&&emPeriodo(r.closed_at,inicio,fim));
    const aprovacoes=ler('approved_ai_tools'), nomes=new Set(departamentos.map(d=>d.department_name));
    const porPessoa=(rows,id)=>id==null?rows:rows.filter(r=>pessoasPorId.get(r.employee_id)?.department_id===id);
    const duracoes=rows=>rows.filter(r=>r.created_at&&r.closed_at>=r.created_at&&pessoasPorId.has(r.employee_id))
        .map(r=>({...r,dias:(Date.parse(r.closed_at.replace(' ','T')+'Z')-Date.parse(r.created_at.replace(' ','T')+'Z'))/86400000})).filter(r=>Number.isFinite(r.dias)&&r.dias>=0);
    function resumo(id=null) {
        const u=id==null?usos:usos.filter(r=>r.department_id===id);
        const empregados=id==null?pessoas:pessoas.filter(p=>p.department_id===id);
        const tr=porPessoa(treinos,id), pr=porPessoa(prs,id), is=duracoes(porPessoa(issues,id));
        const ev=porPessoa(eventos,id);
        const centros=new Set(empregados.map(p=>p.cost_center).filter(Boolean));
        // Centro ambiguo nao e repartido por numero de pessoas.
        const centrosAmbiguos=new Set(pessoas.filter(p=>centros.has(p.cost_center)&&id!=null&&p.department_id!==id).map(p=>p.cost_center));
        const bill=id==null?faturas:faturas.filter(r=>centros.has(r.cost_center)&&!centrosAmbiguos.has(r.cost_center));
        const bud=id==null?orcamentos:orcamentos.filter(r=>centros.has(r.cost_center)&&!centrosAmbiguos.has(r.cost_center));
        const valores={}; for(const r of bill) valores[r.currency]=(valores[r.currency]||0)+Number(r.total_cost);
        const concluidos=tr.filter(r=>r.completion_status==='Concluido'&&emPeriodo(r.completion_date,inicio,fim)&&r.enrollment_date&&r.completion_date>=r.enrollment_date&&numero(r.score)!=null&&numero(r.score)>=0&&numero(r.score)<=100&&pessoasPorId.has(r.employee_id));
        const literacy=new Map();for(const r of concluidos.filter(r=>r.course_category==='IA Generativa')) {
            if(!literacy.has(r.employee_id)) literacy.set(r.employee_id,[]); literacy.get(r.employee_id).push(Number(r.score));
        }
        const coorte=new Map();let candidatas=0,validas=0;
        for(const r of tr.filter(r=>emPeriodo(r.enrollment_date,inicio,fim))) {
            candidatas++; if(!pessoasPorId.has(r.employee_id))continue; validas++;
            let estado=1;
            if(['Em Andamento','Nao Iniciado','Abandonado'].includes(r.completion_status))estado=0;
            else if(r.completion_status==='Concluido'&&r.completion_date&&r.completion_date>=r.enrollment_date){
                if(r.completion_date>fim)estado=0;
                else if(numero(r.score)!=null&&numero(r.score)>=0&&numero(r.score)<=100)estado=numero(r.score)>=80?2:0;
            }
            coorte.set(r.employee_id,Math.max(coorte.get(r.employee_id)??0,estado));
        }
        let requests=0,classificados=0,aprovados=0,invalidos=0;
        for(const r of u) {
            const n=numero(r.request_count);if(!Number.isInteger(n)||n<=0){invalidos++;continue;} requests+=n;
            const p=pessoasPorId.get(r.employee_id);
            if(!p||p.department_id!==r.department_id)continue;
            const a=aprovacoes.filter(a=>a.ai_tool_id===r.ai_tool_id);if(a.length!==1)continue;
            const c=a[0];if(!c.approval_date||c.approval_date>r.timestamp.slice(0,10)||!c.allowed_departments)continue;
            if(!['Aprovado','Bloqueado','Em Avaliacao'].includes(c.approval_status))continue;
            const lista=c.allowed_departments.split(/[,;]/).map(x=>x.trim());
            if(c.allowed_departments!=='Todos os departamentos'&&lista.some(x=>!nomes.has(x)))continue;
            classificados+=n;
            const nome=departamentos.find(d=>d.department_id===r.department_id)?.department_name;
            if(c.approval_status==='Aprovado'&&(c.allowed_departments==='Todos os departamentos'||lista.includes(nome)))aprovados+=n;
        }
        const prValidos=pr.filter(r=>r.created_at&&r.merged_at>=r.created_at&&pessoasPorId.has(r.employee_id)&&Number.isInteger(numero(r.review_count))&&numero(r.review_count)>=0);
        const conhecidos=[...coorte.values()].filter(v=>v!==1).length,qualificados=[...coorte.values()].filter(v=>v===2).length;
        function fator(codigo,nota,numerador,denominador,fonte,formula,motivo,elegivel=false) {
            return {codigo,peso:PESOS[codigo],nota:arred(nota),numerador,denominador,fonte,formula,
                estado:nota==null?'SEM_DADOS':'DESCRITIVO_SINTETICO',elegivelGlobal:elegivel,motivo};
        }
        const fatores=[
            fator('aiLiteracy',media([...literacy.values()].map(media)),null,literacy.size,'training_records','Media das medias por pessoa (IA Generativa concluida)','Media das pessoas observadas; nao representa todo o headcount'),
            fator('governanca',invalidos?null:pct(aprovados,classificados),aprovados,classificados,'ai_usage_logs + approved_ai_tools','100 * requests aprovados / classificados','Cadastro sem historico de revogacoes; cobertura: '+arred(pct(classificados,requests))+'%'),
            fator('usoResponsavel',null,null,null,'Ausente','100 * C / (C + N)','Logs tecnicos nao contem julgamento de conformidade fundamentado'),
            fator('produtividade',metaDias==null?null:notaProdutividade(is,metaDias),null,is.length,'issues + projects','Media das notas por tipo, ponderada por issues; cada nota=min(100,100*meta/medianaDias)','Meta simulada explicita; nao mede ganho causado pela IA'),
            fator('finOps',null,null,null,'ai_billing + ai_budgets','limitar(100*(2-custo/orcamento),0,100)','Moeda de orcamento e cambio nao existem na origem'),
            fator('seguranca',null,null,null,'Ausente','100 * controles atendidos / avaliados','security_events nao comprova controles aplicaveis/atendidos'),
            fator('colaboracao',pct(prValidos.filter(r=>Number(r.review_count)>=1).length,prValidos.length),prValidos.filter(r=>Number(r.review_count)>=1).length,prValidos.length,'pull_requests + projects','100 * PRs integrados com revisao / PRs integrados validos','Identidade do revisor ausente; nao comprova independencia'),
            fator('desenvolvimentoCompetencias',pct(qualificados,conhecidos),qualificados,conhecidos,'training_records + employees','100 * pessoas com nota >=80 / resultado determinavel','Limiar 80 da proposta; cobertura tecnica: '+arred(pct(validas,candidatas))+'%')
        ];
        const orcamento=bud.reduce((s,r)=>s+Number(r.budget_amount),0);
        const simulacaoFinOps=cambio&&moedaOrcamento==='BRL'&&orcamento>0&&!Object.keys(valores).some(k=>!['BRL','USD'].includes(k))&&bill.length
            ? {natureza:'SIMULACAO_COM_PREMISSAS',cambioBRLPorUSD:cambio,moedaOrcamento:'BRL',orcamento:arred(orcamento),
                custoConvertido:arred((valores.BRL||0)+(valores.USD||0)*cambio),
                nota:arred(Math.max(0,Math.min(100,100*(2-((valores.BRL||0)+(valores.USD||0)*cambio)/orcamento)))),
                limite:'Lancamentos observados; origem admite faturas faltantes. Nao contribui ao score global.'} : null;
        const mensal=new Map();for(const r of u){const m=r.timestamp.slice(0,7);if(!mensal.has(m))mensal.set(m,{mes:m,eventos:0,sucessos:0,requisicoes:0,tokens:0});const x=mensal.get(m);x.eventos++;if(r.request_status==='success')x.sucessos++;x.requisicoes+=Number(r.request_count)||0;x.tokens+=(Number(r.input_tokens)||0)+(Number(r.output_tokens)||0);}
        const tipos=[...new Set(is.map(r=>r.issue_type))].sort().map(tipo=>{const xs=is.filter(r=>r.issue_type===tipo);return {tipo,quantidade:xs.length,medianaDias:arred(mediana(xs.map(r=>r.dias))),metaDiasSimulada:metaDias,notaSimulada:metaDias?arred(notaProdutividade(xs,metaDias)):null};});
        return {departamentoId:id,natureza:'CALCULADO_DA_BASE_SINTETICA',scoreGlobal:null,scoreParcial:null,
            motivoScore:'Nao ha oito fatores elegiveis; medias descritivas nao sao indice global',
            empregadosNaBase:empregados.length,eventosUso:u.length,requisicoes:requests,
            tokensEntrada:u.reduce((s,r)=>s+(Number(r.input_tokens)||0),0),tokensSaida:u.reduce((s,r)=>s+(Number(r.output_tokens)||0),0),
            eventosSucesso:u.filter(r=>r.request_status==='success').length,taxaSucessoEventos:arred(pct(u.filter(r=>r.request_status==='success').length,u.length)),
            treinamentosConcluidos:concluidos.length,eventosSeguranca:ev.length,
            custosPorMoeda:Object.fromEntries(Object.entries(valores).map(([k,v])=>[k,arred(v)])),
            centrosAmbiguosExcluidos:[...centrosAmbiguos],faturasObservadas:bill.length,simulacaoFinOps,
            evolucao:[...mensal.values()].sort((a,b)=>a.mes.localeCompare(b.mes)),produtividadePorTipo:tipos,fatores};
    }
    // Agregados financeiros conservam mes, moeda, ferramenta e atribuicao de centro.
    // Uma fatura nao e custo de um evento individual nem custo por token.
    const donosCentro=new Map();
    for(const p of pessoas.filter(p=>p.cost_center)) {
        if(!donosCentro.has(p.cost_center))donosCentro.set(p.cost_center,new Set());
        donosCentro.get(p.cost_center).add(p.department_id);
    }
    const departamentoCentro=cc=>donosCentro.get(cc)?.size===1?[...donosCentro.get(cc)][0]:null;
    const financeiros=new Map();
    for(const r of faturas) {
        const departamentoId=departamentoCentro(r.cost_center),chave=JSON.stringify([r.billing_month,departamentoId,r.currency,r.ai_tool_id]);
        if(!financeiros.has(chave))financeiros.set(chave,{mes:r.billing_month,departamentoId,moeda:r.currency,ferramentaId:r.ai_tool_id,custoCentavos:0,faturas:0});
        const grupo=financeiros.get(chave);grupo.custoCentavos+=Math.round(Number(r.total_cost)*100);grupo.faturas++;
    }
    const ferramentas=db.prepare('SELECT ai_tool_id,tool_name FROM ai_tools').all();
    const financeiro={meses:[...new Set([...faturas.map(r=>r.billing_month),...orcamentos.map(r=>`${r.year}-${String(r.month).padStart(2,'0')}`)])].sort(),
        custos:[...financeiros.values()].map(r=>({...r,custo:r.custoCentavos/100})),
        orcamentos:orcamentos.map(r=>({mes:`${r.year}-${String(r.month).padStart(2,'0')}`,departamentoId:departamentoCentro(r.cost_center),centro:r.cost_center,valor:Number(r.budget_amount),moeda:null})),
        uso:departamentos.flatMap(d=>resumo(d.department_id).evolucao.map(r=>({...r,departamentoId:d.department_id}))),
        ferramentas};
    return {versao:'base-original-1.1',natureza:'BASE_ORIGINAL_SINTETICA',empresa:company,periodo:{inicio,fim},financeiro,
        premissas:{cambioBRLPorUSD:cambio,moedaOrcamento,metaDiasSimulada:metaDias,finops:'Apenas meses completos dentro do periodo; moedas separadas por padrao'},
        total:resumo(),departamentos:departamentos.map(d=>({...d,...resumo(d.department_id)}))};
}
function notaProdutividade(rows,meta) {
    if(!rows.length)return null;
    const tipos=[...new Set(rows.map(r=>r.issue_type))];
    return tipos.reduce((s,t)=>{const xs=rows.filter(r=>r.issue_type===t),m=mediana(xs.map(r=>r.dias));return s+(m===0?100:Math.min(100,100*meta/m))*xs.length;},0)/rows.length;
}
if(require.main===module) {
    const args=process.argv.slice(2), dbPath=args[0],saida=args[1];
    if(!dbPath||!saida)throw Error('Uso: node scripts/calcular-base-original.cjs origem.db saida.json [--cambio=5 --moeda-orcamento=BRL --meta-dias=5]');
    const opc=Object.fromEntries(args.slice(2).map(a=>a.replace(/^--/,'').split('=')));
    const db=new DatabaseSync(dbPath,{readOnly:true});
    try {
        const dados=gerar(db,{empresa:opc.empresa||'C01',inicio:opc.inicio||'2025-06-01',fim:opc.fim||'2026-08-10',
            cambio:opc.cambio?Number(opc.cambio):null,moedaOrcamento:opc['moeda-orcamento']||null,metaDias:opc['meta-dias']?Number(opc['meta-dias']):null});
        dados.origem={arquivo:path.basename(dbPath),sha256:crypto.createHash('sha256').update(fs.readFileSync(dbPath)).digest('hex'),
            tabelas:db.prepare("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").all().map(r=>({nome:r.name,linhas:db.prepare('SELECT COUNT(*) n FROM '+r.name).get().n}))};
        fs.mkdirSync(path.dirname(saida),{recursive:true}); fs.writeFileSync(saida,JSON.stringify(dados,null,2)+'\n');
        console.log(JSON.stringify({saida,empresa:dados.empresa.company_id,departamentos:dados.departamentos.length,eventos:dados.total.eventosUso,requisicoes:dados.total.requisicoes,hash:dados.origem.sha256}));
    } finally {db.close();}
}
module.exports={gerar,mediana,notaProdutividade};
