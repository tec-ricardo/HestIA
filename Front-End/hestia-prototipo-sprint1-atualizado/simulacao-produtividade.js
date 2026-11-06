function simularProdutividade(entrada) {
  const numero = valor => typeof valor === 'number' || (typeof valor === 'string' && valor.trim() !== '') ? Number(valor) : NaN;
  const antes = numero(entrada?.minutosAntes);
  const depois = numero(entrada?.minutosDepois);
  const tarefas = numero(entrada?.tarefas);
  if (![antes, depois].every(n => Number.isFinite(n) && n >= 0.01 && n <= 1000000) || !Number.isInteger(tarefas) || tarefas < 1 || tarefas > 10000) {
    throw new Error('Informe tempos de pelo menos 0,01 minuto e de 1 a 10.000 tarefas comparáveis.');
  }
  const diferenca = antes - depois;
  return {
    naturezaDados: 'SIMULADA', fonte: 'Hipóteses informadas no formulário; sem dados de funcionários reais',
    minutosAntes: antes, minutosDepois: depois, tarefas,
    minutosPorTarefaEconomizados: diferenca, minutosTotaisEconomizados: diferenca * tarefas,
    reducaoTempoPercentual: 100 * diferenca / antes,
    variacaoCapacidadePercentual: 100 * (antes / depois - 1),
    custos: null,
    aviso: 'Simulação de tarefas equivalentes, com mesma qualidade, complexidade e jornada. Os tempos são hipóteses, não médias medidas. Não comprova efeito real ou causal da HestIA. Repetir tarefas simuladas não torna a evidência real nem libera o score global.'
  };
}
function registrarRotasSimulacao(app) {
  app.get('/maturidade/simulacao-produtividade', (req, res) => {
    const filtros = {};
    for (const campo of ['minutosAntes', 'minutosDepois', 'tarefas']) {
      if (typeof req.query[campo] === 'string') filtros[campo] = req.query[campo];
    }
    let resultado = null, erro = null;
    if (Object.keys(req.query).length) {
      try { resultado = simularProdutividade(filtros); }
      catch (falha) { erro = falha.message; }
    }
    res.status(erro ? 400 : 200).render('simulacao-produtividade', {
      perfil: req.session.perfil, email: req.session.email, active: 'maturidade', filtros, resultado, erro
    });
  });
}
module.exports = { simularProdutividade, registrarRotasSimulacao };
