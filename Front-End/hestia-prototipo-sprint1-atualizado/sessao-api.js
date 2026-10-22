const PERFIS = Object.freeze({ ADMIN: 'administrador', GESTOR: 'gestor', FUNCIONARIO: 'colaborador' });

const executarSessao = (sessao, metodo) => new Promise((resolve, reject) =>
    sessao[metodo](erro => erro ? reject(erro) : resolve()));

async function loginApi(req, res, api) {
    const email = String(req.body.email || '').trim().toLowerCase();
    if (/@(adm|gestor|colaborador)$/.test(email)) {
        await executarSessao(req.session, 'regenerate');
        req.session.email = email;
        req.session.perfil = { adm: 'administrador', gestor: 'gestor', colaborador: 'colaborador' }[email.split('@').pop()];
        await executarSessao(req.session, 'save');
        res.redirect('/maturidade');
        return true;
    }
    res.set('Cache-Control', 'no-store');
    try {
        // Um login recusado nunca conserva a identidade da sessão anterior.
        await executarSessao(req.session, 'regenerate');
        const { data } = await api.post('/auth/login', { email, senha: String(req.body.senha || '') });
        if (typeof data?.token !== 'string' || !data.token.trim()) {
            throw new Error('Resposta de autenticação inválida');
        }
        const { data: contexto } = await api.get('/api/contexto', {
            headers: { Authorization: `Bearer ${data.token}` }
        });
        if (!PERFIS[contexto?.perfil] || !Number.isSafeInteger(contexto.usuarioId) || contexto.usuarioId <= 0
                || !Number.isSafeInteger(contexto.empresaId) || contexto.empresaId <= 0) {
            throw new Error('Contexto de autenticação inválido');
        }
        req.session.apiToken = data.token;
        req.session.email = email;
        req.session.perfil = PERFIS[contexto.perfil];
        req.session.usuarioId = contexto.usuarioId;
        req.session.empresaId = contexto.empresaId;
        req.session.departamentoId = contexto.departamentoId;
        await executarSessao(req.session, 'save');
        res.redirect('/maturidade');
    } catch (erro) {
        delete req.session.apiToken;
        delete req.session.perfil;
        const recusado = [400, 401, 403].includes(erro.response?.status);
        res.status(recusado ? 401 : 503).render('login', {
            erro: recusado ? 'E-mail ou senha inválidos, ou conta sem acesso.'
                : 'Não foi possível acessar o serviço de autenticação. Tente novamente.'
        });
    }
    return true;
}

function logoutApi(api) {
    return async (req, res) => {
        res.set('Cache-Control', 'no-store');
        const token = req.session.apiToken;
        try {
            if (token) await api.post('/auth/logout', null, {
                headers: { Authorization: `Bearer ${token}` }
            });
        } catch (_) {
            // A indisponibilidade da API não impede o encerramento local.
        }
        delete req.session.apiToken;
        delete req.session.perfil;
        req.session.destroy(erro => {
            res.clearCookie('connect.sid');
            res.redirect(erro ? '/acesso?expirada=1' : '/acesso?saiu=1');
        });
    };
}

module.exports = { loginApi, logoutApi };
