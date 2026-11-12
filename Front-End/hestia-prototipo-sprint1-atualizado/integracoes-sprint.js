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
  const niveisPasse = Object.freeze({
    APRENDIZ: { numero: 1, nome: 'Aprendiz' },
    CONSCIENTE: { numero: 2, nome: 'Consciente' },
    GUARDIAO: { numero: 3, nome: 'Guardião' },
    EMBAIXADOR: { numero: 4, nome: 'Embaixador' }
  });
  const nivelNecessarioPorId = cursoId => {
    const numero = Number(cursoId);
    if (!Number.isInteger(numero) || numero < 1 || numero > 16) throw new Error(`Curso fora da trilha: ${cursoId}`);
    return Math.ceil(numero / 4);
  };
  const nomeNivel = numero => ({ 1: 'Aprendiz', 2: 'Consciente', 3: 'Guardião', 4: 'Embaixador' })[numero] || 'Embaixador';
  const consultarNivelPasse = async req => {
    const usuarioId = id(req.session.usuarioId);
    const { data } = await api.get(`/gamificacao/usuarios/${usuarioId}/passe`);
    const xp = Number(data?.xpTotal);
    if (!Number.isFinite(xp) || xp < 0) {
      const erro = new Error('Não foi possível confirmar o XP do HestIA Pass. Acesso bloqueado por segurança.');
      erro.response = { status: 503, data: { message: erro.message } };
      throw erro;
    }
    const chaveNivel = String(data?.nivel || '').trim().toUpperCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    const nivel = niveisPasse[chaveNivel];
    if (!nivel) {
      const erro = new Error('Nível do HestIA Pass inválido ou indisponível.');
      erro.response = { status: 503, data: { message: erro.message } };
      throw erro;
    }
    return { ...nivel, xpAtual: xp };
  };
  const adaptar = (treinamento, indice, nivelAtual) => {
    if (treinamento.catalogoPersistido === true) return {...treinamento, id:String(treinamento.id), bloqueado:false, nivel:'Catálogo da organização', motivo:'Conteúdo publicado pela organização.', recomendado:false, xpAtual:null, medalhaAtual:nivelAtual.nome};
    const exigido = nivelNecessarioPorId(treinamento.id);
    // O primeiro curso do nível Consciente (5) é liberado ao atingir o nível 2.
    // Os demais cursos dessa etapa (6 a 8) exigem também 300 XP.
    // Assim, um usuário Consciente com 150 XP acessa os cursos 1 a 5.
    const exigeXpComplementar = nivelAtual.numero === 2 &&
      Number(treinamento.id) >= 6 && Number(treinamento.id) <= 8 &&
      nivelAtual.xpAtual < 300;
    const bloqueado = nivelAtual.numero < exigido || exigeXpComplementar;
    return {
      ...treinamento,
      id: String(treinamento.id),
      bloqueado,
      nivel: `Nível ${exigido} — ${nomeNivel(exigido)}`,
      motivo: bloqueado
        ? `Alcance o nível ${nomeNivel(exigido)} no HestIA Pass para liberar.`
        : 'Liberado pela evolução no HestIA Pass.',
      recomendado: exigido === nivelAtual.numero,
      xpAtual: null,
      medalhaAtual: nivelAtual.nome
    };
  };
  const consultarTreinamentosComNivel = async req => {
    const [{ data }, nivelAtual] = await Promise.all([
      api.get('/aprendizagem/conteudos'),
      consultarNivelPasse(req)
    ]);
    return { treinamentos: data.map((treinamento, indice) => adaptar(treinamento, indice, nivelAtual)).sort((a,b) => Number(a.id) - Number(b.id)), nivelAtual };
  };
  const exigirCursoLiberado = async (req, cursoId) => {
    const { treinamentos } = await consultarTreinamentosComNivel(req);
    const treinamento = treinamentos.find(item => String(item.id) === String(cursoId));
    if (!treinamento) {
      const erro = new Error('Microtreinamento não encontrado.');
      erro.response = { status: 404, data: { message: erro.message } };
      throw erro;
    }
    if (treinamento.bloqueado) {
      const erro = new Error(treinamento.motivo);
      erro.response = { status: 403, data: { message: erro.message } };
      throw erro;
    }
    return treinamento;
  };
  // Cadastro administrativo das certificações existentes da Academy (sem conceder sem aprovação).
  router.post('/api/admin/academy/certificacoes/inicializar', admin, executar(async (req, res) => {
    const { data } = await api.post('/aprendizagem/academy/certificacoes/inicializar');
    res.json(data);
  }));
  // Tela administrativa para executar a inicializacao com a sessao autenticada do HestIA.
  router.get('/admin/academy/certificacoes', admin, (req, res) => {
    res.type('html').send(`<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Certificações | HestIA Academy</title><link rel="stylesheet" href="/styles.css"></head><body style="font-family:Arial,sans-serif;padding:32px;max-width:720px;margin:auto"><h1>Certificações HestIA Academy</h1><p>Cadastre ou atualize as quatro certificações usando sua sessão de administrador. Isso não concede certificados sem aprovação.</p><button id="inicializar" type="button" style="padding:12px 18px;cursor:pointer">Inicializar certificações</button><pre id="resultado" style="white-space:pre-wrap;overflow-wrap:anywhere;margin-top:18px"></pre><p><a href="/">Voltar à visão geral</a></p><script>document.getElementById('inicializar').addEventListener('click', async function(){const botao=this;botao.disabled=true;const saida=document.getElementById('resultado');saida.textContent='Inicializando...';try{const resposta=await fetch('/api/admin/academy/certificacoes/inicializar',{method:'POST',credentials:'same-origin'});const dados=await resposta.json();saida.textContent=resposta.ok?'Concluído: '+JSON.stringify(dados,null,2):'Erro '+resposta.status+': '+(dados.erro||JSON.stringify(dados));}catch(e){saida.textContent='Erro de conexão: '+e.message;}finally{botao.disabled=false;}});</script></body></html>`);
  });
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
    const { treinamentos, nivelAtual } = await consultarTreinamentosComNivel(req);
    res.json({ treinamentos, nivelAtual });
  }));
  router.get('/api/microtreinamentos/:id', colaborador, executar(async (req, res) => {
    const cursoId = id(req.params.id);
    const liberado = await exigirCursoLiberado(req, cursoId);
    const { data } = await api.get(`/aprendizagem/conteudos/${cursoId}`);
    const treinamento = { ...data, bloqueado: false, nivel: liberado.nivel, motivo: liberado.motivo, recomendado: liberado.recomendado, medalhaAtual: liberado.medalhaAtual };
    treinamento.questoes = [];
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
    const cursoId = id(req.params.id);
    await exigirCursoLiberado(req, cursoId);
    res.json((await api.post(`/aprendizagem/conteudos/${cursoId}/progresso`, { etapa: req.body.etapa, chave: randomUUID() })).data);
  }));
  router.post('/api/microtreinamentos/:id/avaliacao', colaborador, executar(async (req, res) => {
    const cursoId = id(req.params.id);
    await exigirCursoLiberado(req, cursoId);
    const tentativa = req.session.tentativasAprendizagem?.[req.params.id];
    if (!tentativa) return res.status(409).json({ erro: 'Abra a avaliação antes de enviar.' });
    const valores = req.body.respostas;
    if (!Array.isArray(valores) || valores.length !== tentativa.questoes.length || valores.some((v, i) => !Number.isInteger(v) || !tentativa.questoes[i].chaves[v])) return res.status(400).json({ erro: 'Responda todas as perguntas com alternativas válidas.' });
    const respostas = valores.map((v, i) => ({ questaoId: tentativa.questoes[i].id, alternativa: tentativa.questoes[i].chaves[v] }));
    const { data } = await api.post(`/aprendizagem/tentativas/${id(tentativa.id)}/respostas`, { respostas });
    res.json({ ...data, total: data.questoes.length, aprovado: data.status === 'APROVADA', mensagem: data.status === 'APROVADA' ? 'Aprendizagem validada e registrada.' : 'Reveja o feedback. A aprovação exige pelo menos 80%; as tentativas são controladas pelo servidor.' });
  }));
  router.get('/microtreinamentos/:id/avaliacao', colaborador, executar(async (req, res) => {
    await exigirCursoLiberado(req, id(req.params.id)); res.render('microtreinamento-avaliacao', { perfil: req.session.perfil, active: 'microtreinamentos', cursoId: req.params.id });
  }));
  router.get('/microtreinamentos/:id', colaborador, executar(async (req, res) => {
    await exigirCursoLiberado(req, id(req.params.id)); res.render('microtreinamento-curso', { perfil: req.session.perfil, active: 'microtreinamentos', cursoId: req.params.id });
  }));
  router.post('/api/rag/perguntas', executar(async (req, res) => {
    const { chave, pergunta, modo, respostaId, conversaId } = req.body;
    const payload={ chave, pergunta, modo: modo || 'CONSULTAR', respostaId: respostaId == null ? null : id(respostaId) };
    if(conversaId!=null)payload.conversaId=id(conversaId);
    res.json((await api.post('/api/rag/perguntas',payload,{timeout:45000})).data);
  }));
  router.post('/api/rag/conversas', executar(async(req,res)=>res.status(201).json((await api.post('/api/rag/conversas',{titulo:req.body.titulo})).data)));
  router.get('/api/rag/conversas', executar(async(req,res)=>res.json((await api.get('/api/rag/conversas')).data)));
  router.get('/api/rag/conversas/:id/historico', executar(async(req,res)=>res.json((await api.get('/api/rag/conversas/'+id(req.params.id)+'/historico')).data)));
  router.get('/api/rag/historico', executar(async (req, res) => res.json((await api.get('/api/rag/historico')).data)));
  router.get('/api/registros/contexto', executar(async (req, res) => res.json((await api.get('/registros-uso-ia/contexto')).data)));
  router.get('/utilizacoes/registros', executar(async (req, res) => {
    const contexto = (await api.get('/registros-uso-ia/contexto')).data;
    const registros = (await api.get('/registros-uso-ia', { params: { usuarioId: req.session.usuarioId } })).data;
    res.render('integracao-registros', { perfil: req.session.perfil, email: req.session.email, active: 'utilizacoes', contexto, registros });
  }));
  router.get('/api/registros', executar(async (req, res) => res.json((await api.get('/registros-uso-ia', { params: { usuarioId: req.session.usuarioId } })).data)));
  router.post('/api/registros', executar(async (req, res) => {
    const campos = ['ferramentaId','modeloIa','dataHora','finalidade','tokensEntrada','tokensSaida','custoEstimado','statusExecucao','tempoRespostaMs','evidencias'];
    const dto = { usuarioId: req.session.usuarioId };
    for (const campo of campos) if (Object.hasOwn(req.body, campo)) dto[campo] = req.body[campo];
    try {
      const { data } = await api.post('/registros-uso-ia', dto, { timeout: 30000 });
      res.status(201).json(data);
    } catch (erro) {
      if (['ECONNABORTED', 'ETIMEDOUT', 'ECONNRESET'].includes(erro.code)) {
        return res.status(504).json({ erro: 'Não foi possível confirmar o salvamento a tempo. O registro pode ter sido salvo. Atualize o histórico antes de tentar novamente para evitar duplicação.' });
      }
      throw erro;
    }
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