const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const path = require('node:path');
const fs = require('node:fs');
const vm = require('node:vm');
const ejs = require('ejs');
const montar = require('../integracoes-sprint');

test('adapta avaliações reais, mantém tentativa e não expõe gabarito', async t => {
  const chamadas = [], sessoes = new Map();
  const questoes = Array.from({ length: 5 }, (_, i) => ({ id: `q${i + 1}`, enunciado: `Questão ${i + 1}`, alternativas: { A: 'Uma', B: 'Duas', C: 'Três' }, correta: 'A' }));
  const api = {
    async get(url) {
      chamadas.push(['GET', url]);
      if (url === '/gamificacao/usuarios/1/passe') return { data: { xpTotal: 150, nivel: 'CONSCIENTE' } };
      if (url === '/gamificacao/usuarios/2/passe') return { data: { xpTotal: 0, nivel: 'APRENDIZ' } };
      if (url === '/aprendizagem/conteudos') return { data: Array.from({ length: 7 }, (_, i) => ({ id: i + 1, titulo: `Curso ${i + 1}` })) };
      if (url.startsWith('/aprendizagem/conteudos/')) return { data: { id: 1, titulo: 'Dados', licoes: [], etapa: 1, totalEtapas: 1, catalogoId: 11, notaMinima: 80 } };
      if (url === '/api/admin/respostas') return { data: [] };
      if (url === '/certificacoes/usuarios/1') return { data: [{ id: 9, nome: 'Fundamentos', nivel: 'Inicial', codigo: 'FUND', versao: 1, concedida_em: '2026-10-22', evidencias: 'privadas' }] };
      throw { response: { status: 503, data: { message: 'Fonte indisponível' } } };
    },
    async post(url, body) {
      chamadas.push(['POST', url, body]);
      if (url === '/certificacoes/usuarios/1/verificar') return { data: [{ definicaoId: 7, nome: 'Fundamentos', nivel: 'Inicial', codigo: 'FUND', versao: 1, concedida: true, requisitosAtendidos: 4, totalRequisitos: 4, avaliacoesPendentes: [] }] };
      if (url === '/aprendizagem/catalogo/11/tentativas') return { data: { id: 123, questoes } };
      if (url === '/aprendizagem/tentativas/123/respostas') return { data: { id: 123, nota: 80, status: 'APROVADA', acertos: 4, questoes } };
      if (url === '/registros-uso-ia') return { data: body };
      throw { response: { status: 503, data: { message: 'Fonte indisponível' } } };
    }
  };
  const app = express();
  app.set('view engine', 'ejs'); app.set('views', path.join(__dirname, '../views'));
  app.use((req, res, next) => {
    const usuarioId = Number(req.headers['x-usuario'] || 1);
    if (!sessoes.has(usuarioId)) sessoes.set(usuarioId, { usuarioId, perfil: req.headers['x-perfil'] || 'colaborador', email: `u${usuarioId}@example.test`, destroy: cb => cb() });
    req.session = sessoes.get(usuarioId); next();
  });
  montar(app, api);
  const server = app.listen(0, '127.0.0.1'); await new Promise(resolve => server.once('listening', resolve));
  t.after(() => new Promise(resolve => server.close(resolve)));
  const base = `http://127.0.0.1:${server.address().port}`;
  const request = async (url, body, headers = {}) => {
    const r = await fetch(base + url, { method: body ? 'POST' : 'GET', headers: { 'content-type': 'application/json', ...headers }, ...(body ? { body: JSON.stringify(body) } : {}) });
    return { status: r.status, text: await r.text() };
  };
  const abertura = await request('/api/microtreinamentos/1?avaliacao=1');
  const catalogo = await request('/api/microtreinamentos');
  const cursos = JSON.parse(catalogo.text).treinamentos;
  assert.equal(cursos.filter(c => !c.bloqueado).length, 5);
  assert.equal(cursos.filter(c => c.bloqueado).length, 2);
  assert.equal((await request('/api/microtreinamentos/6')).status, 403);
  const certificacoes = await request('/api/certificacoes?usuarioId=999');
  assert.equal(certificacoes.status, 200);
  assert.deepEqual(JSON.parse(certificacoes.text), {
    certificacoes: [{ id: 9, nome: 'Fundamentos', nivel: 'Inicial', codigo: 'FUND', versao: 1, concedidaEm: '2026-10-22' }],
    progresso: [{ definicaoId: 7, nome: 'Fundamentos', nivel: 'Inicial', codigo: 'FUND', versao: 1, concedida: true, requisitosAtendidos: 4, totalRequisitos: 4, requisitosPendentes: 0 }]
  });
  assert.equal((await request('/api/certificacoes', null, { 'x-usuario': '3', 'x-perfil': 'administrador' })).status, 403);
  assert.equal(abertura.status, 200); assert.equal(JSON.parse(abertura.text).treinamento.questoes.length, 5);
  assert.ok(!abertura.text.includes('correta')); assert.ok(!abertura.text.includes('chaves'));
  const incompleto = await request('/api/microtreinamentos/1/avaliacao', { respostas: [0] }); assert.equal(incompleto.status, 400);
  const resposta = await request('/api/microtreinamentos/1/avaliacao', { respostas: [0, 0, 0, 0, 1] });
  assert.equal(resposta.status, 200); assert.equal(JSON.parse(resposta.text).nota, 80);
  const enviada = chamadas.find(c => c[1] === '/aprendizagem/tentativas/123/respostas')[2];
  assert.deepEqual(enviada.respostas[4], { questaoId: 'q5', alternativa: 'B' });
  assert.equal((await request('/api/microtreinamentos/1/avaliacao', { respostas: [0, 0, 0, 0, 0] }, { 'x-usuario': '2' })).status, 409);
  const registro = await request('/api/registros', { usuarioId: 999, empresaId: 999, origem: 'PLATAFORMA', modeloIa: 'modelo' });
  assert.deepEqual(JSON.parse(registro.text), { usuarioId: 1, modeloIa: 'modelo' });
  assert.equal((await request('/api/admin/respostas')).status, 403);
  assert.equal((await request('/api/admin/respostas/1abc', null, { 'x-usuario': '3', 'x-perfil': 'administrador' })).status, 400);
  const erro = await request('/api/rag/perguntas', { pergunta: 'Dados', modo: 'CONSULTAR', chave: '1' });
  assert.equal(erro.status, 503); assert.ok(erro.text.includes('Fonte indisponível')); assert.ok(!erro.text.includes('simulada'));
});

test('scripts das telas integradas são válidos e a prova informa a regra do servidor', async () => {
  for (const nome of ['assistente-rag', 'microtreinamento', 'microtreinamento-avaliacao', 'microtreinamento-curso']) {
    const html = await ejs.renderFile(path.join(__dirname, '../views', nome + '.ejs'), { perfil: 'colaborador', email: 'teste@example.test', active: 'microtreinamentos', cursoId: '1' });
    for (const script of html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g)) if (script[1].trim()) new vm.Script(script[1]);
  }
  const prova = fs.readFileSync(path.join(__dirname, '../views/microtreinamento-avaliacao.ejs'), 'utf8');
  assert.ok(prova.includes('Nota minima: 80%')); assert.ok(prova.includes('?avaliacao=1'));
  const curso = fs.readFileSync(path.join(__dirname, '../views/microtreinamento-curso.ejs'), 'utf8');
  assert.ok(curso.includes('new BroadcastChannel("hestia-pass")'));
  assert.ok(curso.includes('curso.etapa >= curso.totalEtapas'));
  const lista = fs.readFileSync(path.join(__dirname, '../views/microtreinamento.ejs'), 'utf8');
  assert.ok(lista.includes('canalPasse.addEventListener("message", carregarCursos)'));
});


test('preserva a política recuperada no fluxo RAG entre interface e backend', async t => {
  const politica = {
    id: 77,
    titulo: 'Política corporativa de proteção de dados no uso de IA',
    trecho: 'Dados pessoais não podem ser enviados a ferramentas não aprovadas.',
    versao: 1,
    conteudoSha256: 'a'.repeat(64)
  };
  const api = {
    async get(url) {
      if (url === '/api/rag/historico') return { data: [] };
      throw { response: { status: 404, data: { message: 'Não encontrado' } } };
    },
    async post(url, body) {
      assert.equal(url, '/api/rag/perguntas');
      assert.deepEqual(body, {
        chave: 'validacao-465',
        pergunta: 'Posso enviar dados pessoais para uma ferramenta de IA?',
        modo: 'CONSULTAR',
        respostaId: null
      });
      return {
        data: {
          id: 101,
          estado: 'RECUPERADA',
          resposta: politica.trecho,
          fontes: [politica],
          confianca: 'NAO_CLASSIFICADA',
          natureza: 'PLATAFORMA'
        }
      };
    }
  };

  const app = express();
  app.use(express.json());
  app.use((req, res, next) => {
    req.session = {
      usuarioId: 10,
      perfil: 'colaborador',
      email: 'funcionario@qsoft.test',
      destroy: callback => callback()
    };
    next();
  });
  montar(app, api);

  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  t.after(() => new Promise(resolve => server.close(resolve)));

  const response = await fetch(
    `http://127.0.0.1:${server.address().port}/api/rag/perguntas`,
    {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({
        chave: 'validacao-465',
        pergunta: 'Posso enviar dados pessoais para uma ferramenta de IA?',
        modo: 'CONSULTAR'
      })
    }
  );

  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.estado, 'RECUPERADA');
  assert.equal(body.resposta, politica.trecho);
  assert.deepEqual(body.fontes, [politica]);
});

test('mantém os três modos do RAG no BFF e confirma a reutilização no backend', async t => {
  const chamadas = [];
  const fonte = { id: 77, titulo: 'Política de IA', trecho: 'Use somente ferramentas aprovadas.', versao: 2 };
  const api = {
    async post(url, body) {
      chamadas.push({ url, body });
      const estados = {
        CONSULTAR: { estado: 'RECUPERADA', resposta: fonte.trecho, fontes: [fonte] },
        GERAR_NOVA: { estado: 'GERADA_COM_FONTES', resposta: 'Resposta nova fundamentada.', fontes: [fonte] },
        REUTILIZAR: { estado: 'REUTILIZADA', resposta: fonte.trecho, fontes: [fonte] }
      };
      return { data: estados[body.modo] };
    }
  };
  const app = express();
  app.use(express.json());
  app.use((req, res, next) => {
    req.session = { usuarioId: 10, perfil: 'colaborador', email: 'funcionario@qsoft.test' };
    next();
  });
  montar(app, api);
  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  t.after(() => new Promise(resolve => server.close(resolve)));
  const base = `http://127.0.0.1:${server.address().port}`;
  const enviar = async payload => {
    const resposta = await fetch(base + '/api/rag/perguntas', {
      method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(payload)
    });
    assert.equal(resposta.status, 200);
    return resposta.json();
  };

  assert.equal((await enviar({ chave: 'c1', pergunta: 'Qual regra?', modo: 'CONSULTAR' })).estado, 'RECUPERADA');
  assert.equal((await enviar({ chave: 'c2', pergunta: 'Qual regra?', modo: 'GERAR_NOVA' })).estado, 'GERADA_COM_FONTES');
  assert.equal((await enviar({ chave: 'c3', pergunta: 'Qual regra?', modo: 'REUTILIZAR', respostaId: 77 })).estado, 'REUTILIZADA');
  assert.deepEqual(chamadas[2].body, {
    chave: 'c3', pergunta: 'Qual regra?', modo: 'REUTILIZAR', respostaId: 77
  });
});

test('as telas explicam fonte, reutilização, evolução, conquistas e próximo passo', () => {
  const rag = fs.readFileSync(path.join(__dirname, '../views/assistente-rag.ejs'), 'utf8');
  assert.ok(rag.includes("modo: 'REUTILIZAR'"));
  assert.ok(rag.includes("modo = proximoModo"));
  assert.ok(rag.includes('Fonte utilizada:'));
  assert.ok(rag.includes('Reutilização confirmada no servidor'));
  assert.ok(rag.includes("new BroadcastChannel('hestia-pass')"));
  assert.ok(rag.includes('+15 XP registrados no HestIA Pass'));
  assert.ok(rag.includes("reutilizada: dados.estado === 'REUTILIZADA'"));
  assert.ok(rag.includes("fetch('/api/rag/historico')"));

  const passe = fs.readFileSync(path.join(__dirname, '../views/passe-hestia.ejs'), 'utf8');
  assert.ok(passe.includes('Nível atual'));
  assert.ok(passe.includes('Sua jornada'));
  assert.ok(passe.includes('Próximo objetivo'));
  assert.ok(passe.includes('Desafios de Progressão'));
  assert.ok(passe.includes('Reconhecimento'));
  assert.ok(passe.includes("fetch('/api/passe-hestia/status?atualizacao='"));
  assert.ok(passe.includes("setInterval(verificarAtualizacao, 2000)"));
});
