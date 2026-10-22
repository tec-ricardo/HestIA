const { AsyncLocalStorage } = require("node:async_hooks");
const contextoApi = new AsyncLocalStorage();

function anexarCredenciais(config) {
    const contexto = contextoApi.getStore();
    if (contexto?.tokenApi && !config.headers?.Authorization) {
        config.headers = { ...config.headers, Authorization: `Bearer ${contexto.tokenApi}` };
    }
    return config;
}

function empresaAtual() {
    const id = contextoApi.getStore()?.empresaId;
    if (!Number.isSafeInteger(id) || id <= 0) throw new Error("Empresa autenticada indisponível");
    return id;
}

module.exports = { contextoApi, anexarCredenciais, empresaAtual };
