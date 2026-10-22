const STATUS = Object.freeze({
    ABERTA: 'Aberta',
    EM_TRATAMENTO: 'Em tratamento',
    RESOLVIDA: 'Resolvida'
});

function paginaInteira(valor) {
    const numero = Number(valor ?? 0);
    return Number.isSafeInteger(numero) && numero >= 0 ? numero : 0;
}

function mensagemFalha(codigo, detalhe = false) {
    if (codigo === 400) return 'Confira os dados informados e tente novamente.';
    if (codigo === 401) return 'Sua sessão da API expirou. Entre novamente.';
    if (codigo === 403) return 'Você não tem permissão para consultar ou alterar estas ocorrências.';
    if (codigo === 404 && detalhe) return 'Ocorrência não encontrada no seu escopo.';
    if (codigo === 409) return 'A ocorrência foi alterada ou os dados não atendem às regras. Recarregue e confira responsável, ação e status.';
    return 'Não foi possível acessar as ocorrências. Tente novamente.';
}

function statusFalha(codigo) {
    return [400, 401, 403, 404, 409].includes(codigo) ? codigo : 503;
}

function base(req) {
    return {
        perfil: req.session.perfil,
        email: req.session.email,
        active: 'ocorrencias',
        nomesStatus: STATUS
    };
}

function autorizado(req, res) {
    if (['administrador', 'gestor'].includes(req.session.perfil)) return true;
    res.redirect('/');
    return false;
}

function credenciais(req) {
    return { headers: { Authorization: `Bearer ${req.session.apiToken}` } };
}

function idOpcional(valor) {
    if (valor === undefined || valor === null || String(valor).trim() === '') return null;
    const id = Number(valor);
    return Number.isSafeInteger(id) && id > 0 ? id : undefined;
}

async function carregarResponsaveis(api, req) {
    try {
        const resposta = await api.get('/usuarios', credenciais(req));
        if (!Array.isArray(resposta.data)) return [];
        return resposta.data
            .filter(usuario => usuario && usuario.ativo === true && Number.isSafeInteger(usuario.id))
            .map(usuario => ({ id: usuario.id, nome: usuario.nome || usuario.email || `Usuário #${usuario.id}` }));
    } catch {
        return [];
    }
}

function registrarRotasOcorrencias(app, api) {
    app.get('/ocorrencias', async (req, res) => {
        if (!autorizado(req, res)) return;
        const status = Object.hasOwn(STATUS, req.query.status) ? req.query.status : '';
        const pagina = paginaInteira(req.query.pagina);
        const dados = { ...base(req), status, pagina, itens: [],
            totalItens: 0, totalPaginas: 0, erro: null };
        res.set('Cache-Control', 'no-store');
        try {
            const resposta = await api.get('/tratamentos-nao-conformidade', {
                ...credenciais(req),
                params: { pagina, tamanho: 20, ...(status ? { status } : {}) }
            });
            const lista = resposta.data;
            if (!lista || !Array.isArray(lista.itens) ||
                    !Number.isSafeInteger(lista.pagina) ||
                    !Number.isSafeInteger(lista.totalItens) ||
                    !Number.isSafeInteger(lista.totalPaginas)) {
                throw new Error('Contrato de paginação inválido');
            }
            return res.render('ocorrencias', {
                ...dados, itens: lista.itens, pagina: lista.pagina,
                totalItens: lista.totalItens, totalPaginas: lista.totalPaginas
            });
        } catch (erro) {
            const codigo = erro.response?.status;
            return res.status(statusFalha(codigo)).render('ocorrencias', {
                ...dados, erro: mensagemFalha(codigo)
            });
        }
    });

    app.get('/ocorrencias/:id', async (req, res) => {
        if (!autorizado(req, res)) return;
        const id = Number(req.params.id);
        const dados = { ...base(req), detalhe: null, responsaveis: [], erro: null,
            sucesso: req.query.atualizada === '1' };
        res.set('Cache-Control', 'no-store');
        if (!/^\d+$/.test(req.params.id) || !Number.isSafeInteger(id) || id <= 0) {
            return res.status(404).render('ocorrencia-detalhe', {
                ...dados, erro: mensagemFalha(404, true)
            });
        }
        try {
            const [resposta, responsaveis] = await Promise.all([
                api.get(`/tratamentos-nao-conformidade/${id}`, credenciais(req)),
                carregarResponsaveis(api, req)
            ]);
            const detalhe = resposta.data;
            if (!detalhe?.tratamento || !detalhe?.ocorrencia || !detalhe?.avaliacao) {
                throw new Error('Contrato de detalhe inválido');
            }
            return res.render('ocorrencia-detalhe', { ...dados, detalhe, responsaveis });
        } catch (erro) {
            const codigo = erro.response?.status;
            return res.status(statusFalha(codigo)).render('ocorrencia-detalhe', {
                ...dados, erro: mensagemFalha(codigo, true)
            });
        }
    });

    app.post('/ocorrencias/:id/tratamento', async (req, res) => {
        if (!autorizado(req, res)) return;
        const id = Number(req.params.id);
        const responsavelId = idOpcional(req.body.responsavelId);
        const versaoRegistro = Number(req.body.versaoRegistro);
        const status = req.body.status;
        const acaoCorretiva = typeof req.body.acaoCorretiva === 'string'
            ? req.body.acaoCorretiva.trim() : '';
        if (!/^\d+$/.test(req.params.id) || !Number.isSafeInteger(id) || id <= 0 ||
                responsavelId === undefined || !Object.hasOwn(STATUS, status) ||
                !Number.isSafeInteger(versaoRegistro) || versaoRegistro < 0 ||
                acaoCorretiva.length > 1000 ||
                (status !== 'ABERTA' && (responsavelId === null || !acaoCorretiva))) {
            return res.status(400).render('ocorrencia-detalhe', {
                ...base(req), detalhe: null, responsaveis: [], sucesso: false,
                erro: mensagemFalha(400, true)
            });
        }
        try {
            await api.put(`/tratamentos-nao-conformidade/${id}`, {
                responsavelId,
                acaoCorretiva: acaoCorretiva || null,
                status,
                versaoRegistro
            }, credenciais(req));
            return res.redirect(303, `/ocorrencias/${id}?atualizada=1`);
        } catch (erro) {
            const codigo = erro.response?.status;
            let detalhe = null;
            let responsaveis = [];
            try {
                [detalhe, responsaveis] = await Promise.all([
                    api.get(`/tratamentos-nao-conformidade/${id}`, credenciais(req))
                        .then(resposta => resposta.data),
                    carregarResponsaveis(api, req)
                ]);
            } catch {}
            return res.status(statusFalha(codigo)).render('ocorrencia-detalhe', {
                ...base(req), detalhe, responsaveis, sucesso: false,
                erro: mensagemFalha(codigo, true)
            });
        }
    });
}

module.exports = { registrarRotasOcorrencias };