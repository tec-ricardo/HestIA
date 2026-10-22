const { test } = require("node:test");
const assert = require("node:assert/strict");
const { contextoApi, anexarCredenciais, empresaAtual } = require("../api-contexto");

test("sessões concorrentes não compartilham token nem empresa", async () => {
    const resultados = await Promise.all([
        contextoApi.run({ tokenApi: "token-a", empresaId: 1 }, async () => {
            await new Promise(resolve => setTimeout(resolve, 15));
            return [anexarCredenciais({ headers: {} }).headers.Authorization, empresaAtual()];
        }),
        contextoApi.run({ tokenApi: "token-b", empresaId: 2 }, async () => {
            await new Promise(resolve => setTimeout(resolve, 1));
            return [anexarCredenciais({ headers: {} }).headers.Authorization, empresaAtual()];
        })
    ]);
    assert.deepEqual(resultados, [["Bearer token-a", 1], ["Bearer token-b", 2]]);
});

test("sem contexto não inventa empresa nem credencial", () => {
    assert.deepEqual(anexarCredenciais({ headers: {} }).headers, {});
    assert.throws(() => empresaAtual(), /Empresa autenticada/);
});

test("login pode consultar seu contexto com o novo token explícito", () => {
    const config = contextoApi.run({ tokenApi: "antigo", empresaId: 1 }, () => anexarCredenciais({ headers: { Authorization: "Bearer novo" } }));
    assert.equal(config.headers.Authorization, "Bearer novo");
});
