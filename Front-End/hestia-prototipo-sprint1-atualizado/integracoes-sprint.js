const express = require('express');
const { randomUUID } = require('node:crypto');

// Adaptadores HTTP das tasks de Ricardo; preservam os templates de Kevin.
module.exports = function montarIntegracoes(app, api) {
  const router = express.Router();
  router.use(express.json({ limit: '128kb' }));
  const executar = fn => async (req, res) => {
    try { await fn(req, res); }
    catch (erro) {
      const status = erro.response?.status || 503;
      const mensagem = erro.response?.data?.message || erro.response?.data?.detail || 'Serviço indisponível. Nenhum resultado demonstrativo foi usado.';
      if (status === 401) return req.session.destroy(() => res.status(401).json({ erro: 'Sessão expirada. Entre novamente.' }));
      return res.status(status).json({ erro: mensagem });
    }
  };
  const colaborador = (req, res, next) => req.session.perfil === 'colaborador' ? next() : res.status(403).json({ erro: 'Acesso exclusivo do colaborador.' });
  const admin = (req, res, next) => req.session.perfil === 'administrador' ? next() : res.status(403).json({ erro: 'Acesso exclusivo de administrador.' });
  const id = valor => {
    if (!/^\d+$/.test(String(valor)) || !Number.isSafeInteger(Number(valor)) || Number(valor) <= 0) {
      const erro = new Error('Identificador inválido'); erro.response = { status: 400, data: { message: erro.message } }; throw erro;
    }
    return Number(valor);
  };
  const adaptar = t => ({ ...t, id: String(t.id), bloqueado: false, nivel: 'Conteúdo disponível', motivo: 'Requisitos e avaliação definidos pela empresa', recomendado: false, xpAtual: null, medalhaAtual: 'Não utilizada como requisito' });
  router.get('/api/certificacoes', colaborador, executar(async (req, res) => {
    const usuarioId = id(req.session.usuarioId);
    const { data: situacoes } = await api.post(`/certificacoes/usuarios/${usuarioId}/verificar`);
    const { data: concessoes } = await api.get(`/certificacoes/usuarios/${usuarioId}`);
    res.json({
      certificacoes: concessoes.map(c => ({ id: c.id, nome: c.nome, nivel: c.nivel, codigo: c.codigo, versao: c.versao, concedidaEm: c.concedida_em })),
      progresso: situacoes.map(s => ({ definicaoId: s.definicaoId, nome: s.nome, nivel: s.nivel, codigo: s.codigo, versao: s.versao, concedida: s.concedida, requisitosAtendidos: s.requisitosAtendidos, totalRequisitos: s.totalRequisitos, requisitosPendentes: s.avaliacoesPendentes.length }))
    });
  }));
  router.get('/api/microtreinamentos', colaborador, executar(async (req, res) => {
    const { data } = await api.get('/aprendizagem/conteudos');
    res.json({ treinamentos: data.map(adaptar) });
  }));
  router.get('/api/microtreinamentos/:id', colaborador, executar(async (req, res) => {
    const { data } = await api.get(`/aprendizagem/conteudos/${id(req.params.id)}`);
    const treinamento = adaptar(data); treinamento.questoes = [];
    if (req.query.avaliacao === '1') {
      if (!data.catalogoId) return res.status(409).json({ erro: 'A avaliação deste conteúdo ainda não foi publicada pela empresa.' });
      const { data: tentativa } = await api.post(`/aprendizagem/catalogo/${id(data.catalogoId)}/tentativas`);
      const questoes = tentativa.questoes.map(q => ({ id: q.id, pergunta: q.enunciado, opcoes: Object.values(q.alternativas), chaves: Object.keys(q.alternativas) }));
      req.session.tentativasAprendizagem ||= {};
      req.session.tentativasAprendizagem[req.params.id] = { id: tentativa.id, questoes: questoes.map(q => ({ id: q.id, chaves: q.chaves })) };
      treinamento.questoes = questoes.map(({ chaves, ...q }) => q);
    }
    res.json({ treinamento });
  }));
  router.post('/api/microtreinamentos/:id/progresso', colaborador, executar(async (req, res) => {
    res.json((await api.post(`/aprendizagem/conteudos/${id(req.params.id)}/progresso`, { etapa: req.body.etapa, chave: randomUUID() })).data);
  }));
  router.post('/api/microtreinamentos/:id/avaliacao', colaborador, executar(async (req, res) => {
    id(req.params.id);
    const tentativa = req.session.tentativasAprendizagem?.[req.params.id];
    if (!tentativa) return res.status(409).json({ erro: 'Abra a avaliação antes de enviar.' });
    const valores = req.body.respostas;
    if (!Array.isArray(valores) || valores.length !== tentativa.questoes.length || valores.some((v, i) => !Number.isInteger(v) || !tentativa.questoes[i].chaves[v])) return res.status(400).json({ erro: 'Responda todas as perguntas com alternativas válidas.' });
    const respostas = valores.map((v, i) => ({ questaoId: tentativa.questoes[i].id, alternativa: tentativa.questoes[i].chaves[v] }));
    const { data } = await api.post(`/aprendizagem/tentativas/${id(tentativa.id)}/respostas`, { respostas });
    res.json({ ...data, total: data.questoes.length, aprovado: data.status === 'APROVADA', mensagem: data.status === 'APROVADA' ? 'Aprendizagem validada e registrada.' : 'Reveja o feedback. A aprovação exige pelo menos 80%; as tentativas são controladas pelo servidor.' });
  }));
  router.get('/microtreinamentos/:id/avaliacao', colaborador, executar(async (req, res) => {
    id(req.params.id); res.render('microtreinamento-avaliacao', { perfil: req.session.perfil, active: 'microtreinamentos', cursoId: req.params.id });
  }));
  router.get('/microtreinamentos/:id', colaborador, executar(async (req, res) => {
    id(req.params.id); res.render('microtreinamento-curso', { perfil: req.session.perfil, active: 'microtreinamentos', cursoId: req.params.id });
  }));
  router.post('/api/rag/perguntas', executar(async (req, res) => {
    const { chave, pergunta, modo, respostaId } = req.body;
    res.json((await api.post('/api/rag/perguntas', { chave, pergunta, modo: modo || 'CONSULTAR', respostaId: respostaId == null ? null : id(respostaId) }, { timeout: 45000 })).data);
  }));
  router.get('/api/rag/historico', executar(async (req, res) => res.json((await api.get('/api/rag/historico')).data)));
  router.get('/api/registros/contexto', executar(async (req, res) => res.json((await api.get('/registros-uso-ia/contexto')).data)));
  router.get('/utilizacoes/registros', executar(async (req, res) => {
    const contexto = (await api.get('/registros-uso-ia/contexto')).data;
    const registros = (await api.get('/registros-uso-ia', { params: { usuarioId: req.session.usuarioId } })).data;
    res.render('integracao-registros', { perfil: req.session.perfil, email: req.session.email, active: 'utilizacoes', contexto, registros });
  }));
  router.get('/api/registros', executar(async (req, res) => res.json((await api.get('/registros-uso-ia', { params: { usuarioId: req.session.usuarioId } })).data)));
  router.post('/api/registros', executar(async (req, res) => {
    const campos = ['ferramentaId','modeloIa','dataHora','finalidade','tokensEntrada','tokensSaida','custoEstimado','statusExecucao','tempoRespostaMs'];
    const dto = { usuarioId: req.session.usuarioId };
    for (const campo of campos) if (Object.hasOwn(req.body, campo)) dto[campo] = req.body[campo];
    res.status(201).json((await api.post('/registros-uso-ia', dto)).data);
  }));
  router.get('/api/admin/respostas', admin, executar(async (req, res) => res.json((await api.get('/api/admin/respostas', { params: { pagina: Number(req.query.pagina || 0), tamanho: 50 } })).data)));
  router.get('/api/admin/respostas/:id', admin, executar(async (req, res) => res.json((await api.get(`/api/admin/respostas/${id(req.params.id)}`)).data)));
  router.post('/api/admin/respostas/:id/revisoes', admin, executar(async (req, res) => res.json((await api.post(`/api/admin/respostas/${id(req.params.id)}/revisoes`, { status: req.body.status, justificativa: req.body.justificativa, expiraEm: req.body.expiraEm || null })).data)));
  router.get('/administracao/respostas', admin, executar(async (req, res) => {
    const lista = (await api.get('/api/admin/respostas')).data;
    const detalhe = req.query.id ? (await api.get(`/api/admin/respostas/${id(req.query.id)}`)).data : null;
    res.render('integracao-respostas', { perfil: req.session.perfil, email: req.session.email, active: 'administracao', lista, detalhe });
  }));
  router.post('/administracao/respostas/:id/revisoes', admin, executar(async (req, res) => {
    await api.post(`/api/admin/respostas/${id(req.params.id)}/revisoes`, { status: req.body.status, justificativa: req.body.justificativa, expiraEm: req.body.expiraEm || null });
    res.redirect(`/administracao/respostas?id=${id(req.params.id)}`);
  }));
  router.get('/maturidade/metodologia/detalhes', executar(async (req, res) => res.render('integracao-metodologia', { perfil: req.session.perfil, email: req.session.email, active: 'maturidade', metodologia: (await api.get('/indicadores/maturidade/metodologia')).data })));
  router.get('/api/maturidade/apuracoes', executar(async (req, res) => res.json((await api.get('/indicadores/maturidade/apuracoes')).data)));
  router.get('/api/maturidade/consolidada', executar(async (req, res) => {
    const dto = {};
    for (const campo of ['inicio','fim','fonte','snapshot','departamentoId','departamentoOrigem','tipoIssue']) if (typeof req.query[campo] === 'string' && req.query[campo]) dto[campo] = req.query[campo];
    res.json((await api.post('/indicadores/maturidade/consulta-consolidada', dto)).data);
  }));
  router.get('/api/maturidade/evolucao', executar(async (req, res) => res.json((await api.get('/indicadores/maturidade/evolucao', { params: { anteriorId: id(req.query.anteriorId), atualId: id(req.query.atualId) } })).data)));
  router.post('/api/maturidade/apuracoes', executar(async (req, res) => res.status(201).json((await api.post('/indicadores/maturidade/apuracoes', { inicio: req.body.inicio, fim: req.body.fim, fonte: req.body.fonte, snapshot: req.body.snapshot, departamentoId: req.body.departamentoId, departamentoOrigem: req.body.departamentoOrigem, tipoIssue: req.body.tipoIssue })).data)));
  app.use(router);
};
