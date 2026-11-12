const { empresaAtual } = require('./api-contexto');
const { randomBytes, timingSafeEqual } = require('node:crypto');
module.exports = function montarConfiguracaoInicial(app, api) {
  function admin(req, res, next) {
    if (req.session?.perfil !== 'administrador') return res.sendStatus(403);
    next();
  }
  app.get('/configuracao-inicial', admin, async (req, res) => {
    req.session.setupCsrf ||= randomBytes(32).toString('hex');
    let situacao = null;
    let erro = '';
    try { situacao = (await api.get(`/empresas/${empresaAtual()}/configuracao-inicial`)).data; }
    catch { erro = 'Não foi possível consultar as pendências. Tente novamente; seus dados foram preservados.'; }
    res.status(erro ? 503 : 200).render('configuracao-inicial', {
      perfil: req.session.perfil, email: req.session.email, active: 'configuracao-inicial',
      situacao, erro, csrf: req.session.setupCsrf, salvo: req.query.salvo === '1'
    });
  });
  app.post('/configuracao-inicial', admin, async (req, res) => {
    const recebido = Buffer.from(String(req.body.csrf || ''));
    const esperado = Buffer.from(req.session.setupCsrf || '');
    if (!esperado.length || recebido.length !== esperado.length || !timingSafeEqual(recebido, esperado)) return res.sendStatus(403);
    const dias = Number(req.body.diasRetencaoAuditoria);
    const limite = req.body.limiteCustoMensal === '' ? null : Number(req.body.limiteCustoMensal);
    if (!Number.isInteger(dias) || dias < 1 || (limite !== null && (!Number.isFinite(limite) || limite < 0))) {
      return res.status(400).send('Informe retenção de pelo menos um dia e limite não negativo. Volte ao formulário para corrigir.');
    }
    try {
      await api.put(`/empresas/${empresaAtual()}/configuracao-inicial`, {
        registrarUsosAutomaticamente: req.body.registrarUsosAutomaticamente === 'on',
        bloquearFerramentasNaoAprovadas: req.body.bloquearFerramentasNaoAprovadas === 'on',
        limiteCustoMensal: limite, diasRetencaoAuditoria: dias
      });
      res.redirect('/configuracao-inicial?salvo=1');
    } catch { res.status(502).send('Não foi possível confirmar o salvamento. Volte e consulte as configurações antes de tentar novamente.'); }
  });
};
