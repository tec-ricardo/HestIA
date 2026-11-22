const DIMENSOES = {
 aiLiteracy:['AI Literacy','Resultados baixos nas avaliações válidas.','Revisar avaliações e recomendar capacitação.','/gestao-colaboradores'],
 governanca:['Governança','Baixa proporção de usos classificados como autorizados.','Revisar ferramentas e políticas vigentes.','/governanca'],
 usoResponsavel:['Uso responsável','Baixa proporção de avaliações conformes.','Avaliar ocorrências e orientar uso responsável.','/ocorrencias'],
 produtividade:['Produtividade','Resultados abaixo da meta do processo.','Revisar processos e oportunidades de automação.','/maturidade'],
 finOps:['FinOps','Baixa aderência ao orçamento.','Revisar consumo e orçamento.','/custos'],
 seguranca:['Segurança','Controles avaliados não atendidos.','Revisar as evidências e controles.','/maturidade'],
 colaboracao:['Colaboração','Baixa proporção de PRs válidos com revisão.','Estimular revisão e compartilhamento de conhecimento.','/maturidade'],
 desenvolvimentoCompetencias:['Competências','Baixa proporção de pessoas qualificadas entre as avaliadas.','Identificar necessidades de capacitação.','/gestao-colaboradores']
};
function gerarDiagnostico(resultado) {
 const fatores=Array.isArray(resultado?.fatores)?resultado.fatores:[];
 const indicadores=Object.entries(DIMENSOES).map(([codigo,[nome,problema,acao,link]])=>{
  const f=fatores.find(x=>x.codigo===codigo);
  const nota=typeof f?.nota==='number'&&Number.isFinite(f.nota)&&f.nota>=0&&f.nota<=100?f.nota:null;
  const elegivel=nota!==null&&f.elegivelGlobal===true;
  const status=nota===null?'Sem dados':!elegivel?'Evidência insuficiente':nota<60?'Crítico':nota<80?'Atenção':'Adequado';
  return {codigo,nome,nota,status,significado:!elegivel?(f?.motivo||'Sem evidência para o recorte.'):nota<80?problema:'Dentro da faixa gerencial proposta.',
   recomendacao:!elegivel?'Coletar ou validar evidências.':nota<80?acao:'Manter acompanhamento.',
   link,formula:f?.formula,fonte:f?.fonte,amostra:f?.amostra,numerador:f?.numerador,denominador:f?.denominador};
 });
 const prioridades=indicadores.filter(i=>['Crítico','Atenção'].includes(i.status)).sort((a,b)=>a.nota-b.nota);
 const ausentes=indicadores.filter(i=>['Sem dados','Evidência insuficiente'].includes(i.status)).length;
 let conclusao=prioridades.length?'Priorize '+prioridades.slice(0,3).map(i=>i.nome).join(', ')+'.':ausentes===8?'Não há evidências suficientes para concluir.':'Os indicadores elegíveis estão na faixa adequada proposta.';
 if(ausentes)conclusao+=' '+ausentes+' dimensão(ões) precisam de evidências adicionais.';
 return {indicadores,prioridades,conclusao,fonte:resultado?.naturezaDados??null,inicio:resultado?.inicio,fim:resultado?.fim};
}
function analisarColaborador(c,limite=0){
 const alertas=[];
 const add=(dimensao,motivo,acao,link)=>alertas.push({dimensao,motivo,acao,link});
 if(c.aiLiteracy==null)add('Competências','Sem avaliações elegíveis no período.','Orientar avaliação e capacitação.','/gestao-colaboradores');
 else if(c.aiLiteracy<80)add('AI Literacy','Média observada abaixo de 80; não é uma nota global individual.','Revisar resultados e recomendar capacitação.','/gestao-colaboradores');
 if(Number(c.nao_conformes)>0)add('Uso responsável',c.nao_conformes+' avaliação(ões) não conforme(s).','Consultar as ocorrências.','/ocorrencias');
 if(Number(c.revisoes)>0)add('Governança',c.revisoes+' avaliação(ões) requerem revisão.','Validar evidências antes de concluir.','/ocorrencias');
 if(limite>0&&Number(c.tokens)>limite)add('FinOps','Consumo registrado acima da referência de '+limite+' tokens.','Verificar necessidade operacional; não é penalidade.','/custos');
 if(Number(c.semFonte)>0)add('Base de conhecimento',c.semFonte+' consulta(s) sem conteúdo autorizado.','Verificar cobertura da base ou finalidade; não classificar como violação automaticamente.','/curadoria');
 return {...c,alertas};
}
module.exports={gerarDiagnostico,analisarColaborador};
