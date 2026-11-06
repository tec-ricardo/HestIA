const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const ejs = require('ejs');
const { qualidadeIndicador } = require('../qualidade-indicadores');
const { simularProdutividade, registrarRotasSimulacao } = require('../simulacao-produtividade');

test('dados suficientes não significam desempenho alto; fragilidade não desaparece com nota alta', () => {
  assert.equal(qualidadeIndicador({ nota: 0, elegivelGlobal: true }).classe, 'suficiente');
  assert.equal(qualidadeIndicador({ nota: 100, elegivelGlobal: false }).classe, 'fragil');
  assert.equal(qualidadeIndicador({ nota: null, elegivelGlobal: true }).classe, 'indisponivel');
  assert.equal(qualidadeIndicador({ nota: 100, elegivelGlobal: true, naturezaDados: 'SIMULADA' }).rotulo, 'Simulação');
});
test('simulação separa redução de tempo de aumento de capacidade e não cria valor financeiro', () => {
  const r = simularProdutividade({ minutosAntes: 60, minutosDepois: 45, tarefas: 30 });
  assert.equal(r.reducaoTempoPercentual, 25);
  assert.ok(Math.abs(r.variacaoCapacidadePercentual - 100 / 3) < 1e-10);
  assert.equal(r.minutosTotaisEconomizados, 450);
  assert.equal(r.naturezaDados, 'SIMULADA');
  assert.equal(r.custos, null);
  assert.match(r.aviso, /não comprova|Não comprova/);
});
test('simulação preserva piora e ausência de melhoria em vez de forçar ganho', () => {
  assert.equal(simularProdutividade({ minutosAntes: 30, minutosDepois: 60, tarefas: 2 }).reducaoTempoPercentual, -100);
  assert.equal(simularProdutividade({ minutosAntes: 30, minutosDepois: 30, tarefas: 2 }).minutosTotaisEconomizados, 0);
});
test('não presume tempos ou quantidade e rejeita valores inválidos', () => {
  for (const entrada of [{}, { minutosAntes: '', minutosDepois: 1, tarefas: 30 },
    { minutosAntes: 0, minutosDepois: 1, tarefas: 30 }, { minutosAntes: 1, minutosDepois: 1e-308, tarefas: 30 }, { minutosAntes: 1, minutosDepois: -1, tarefas: 30 },
    { minutosAntes: Infinity, minutosDepois: 1, tarefas: 30 }, { minutosAntes: 1, minutosDepois: 1, tarefas: 1.5 },
    { minutosAntes: 1, minutosDepois: 1, tarefas: 10001 }]) assert.throws(() => simularProdutividade(entrada));
});
test('rota inicia vazia e retorna 400 para hipóteses incompletas', () => {
  let rota; registrarRotasSimulacao({ get: (_, handler) => { rota = handler; } });
  let codigo, render;
  const res = { status(n) { codigo = n; return this; }, render(_, locals) { render = locals; } };
  rota({ query: {}, session: { perfil: 'gestor', email: 'teste' } }, res);
  assert.equal(codigo, 200); assert.equal(render.resultado, null);
  rota({ query: { minutosAntes: '60' }, session: { perfil: 'gestor' } }, res);
  assert.equal(codigo, 400); assert.ok(render.erro);
});
test('tela de simulação mantém aviso e campos vazios antes da escolha de hipóteses', async () => {
  const html = await ejs.renderFile(path.join(__dirname, '../views/simulacao-produtividade.ejs'), {
    perfil: 'gestor', email: 'teste', active: 'maturidade', filtros: {}, resultado: null, erro: null
  });
  assert.match(html, /Dados simulados/);
  assert.match(html, /name="minutosAntes"[^>]*value=""/);
  assert.doesNotMatch(html, /Redução simulada do tempo:/);
});
test('tela mantém nota frágil visível com aviso e não converte ausência em zero', async () => {
  const html = await ejs.renderFile(path.join(__dirname, '../views/maturidade-resultados.ejs'), {
    perfil: 'gestor', email: 'teste', active: 'maturidade', filtros: {}, erro: null, qualidadeIndicador,
    resultado: { naturezaDados: 'SINTETICA', coberturaPesos: 0, scoreGlobal: null, scoreParcial: null,
      fatores: [{ codigo: 'colaboracao', nota: 100, estado: 'PARCIAL', amostra: 2, coberturaTecnica: 1,
        elegivelGlobal: false, motivo: 'Amostra abaixo do mínimo' },
        { codigo: 'finOps', nota: null, estado: 'CONFIGURACAO_PENDENTE', amostra: 0,
          elegivelGlobal: false, motivo: 'Valores não definidos' }], avisos: [] }
  });
  assert.match(html, /Informação frágil/); assert.match(html, /100.00/);
  assert.match(html, /Amostra abaixo do mínimo/); assert.match(html, /Indisponível/);
  assert.match(html, /aria-label="Legenda da qualidade dos dados"/);
});
