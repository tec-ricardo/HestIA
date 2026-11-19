function qualidadeIndicador(fator) {
  if (fator.nota == null) return { classe: 'indisponivel', rotulo: 'Indisponível', detalhe: fator.motivo || 'Não há dados ou configuração suficientes para calcular.' };
  if (fator.naturezaDados === 'DEMO') return {classe:'fragil',rotulo:'Demonstração no banco',detalhe:'Cálculo com observações fictícias persistidas no PostgreSQL.'};
  if (fator.naturezaDados === 'SIMULADA') return { classe: 'fragil', rotulo: 'Simulação', detalhe: 'Hipóteses de demonstração; não representam resultados reais.' };
  if (fator.elegivelGlobal === true) return { classe: 'suficiente', rotulo: 'Dados suficientes', detalhe: 'Atende aos critérios do indicador; não significa bom desempenho nem garantia estatística.' };
  return { classe: 'fragil', rotulo: 'Informação frágil', detalhe: fator.motivo || 'Amostra, cobertura ou evidência insuficiente. Interprete esta nota com cautela.' };
}
module.exports = { qualidadeIndicador };
