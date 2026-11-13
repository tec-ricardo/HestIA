const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const express = require('express');
const axios = require('axios');
const montar = require('../integracoes-sprint');

async function listen(server) {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  return `http://127.0.0.1:${server.address().port}`;
}

test('registro aguarda confirmação após cinco segundos, sem reenviar a gravação', async t => {
  let gravacoes = 0;
  const backend = http.createServer((req, res) => {
    gravacoes++;
    req.resume();
    setTimeout(() => {
      res.writeHead(201, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ id: 123 }));
    }, 5200);
  });
  const baseURL = await listen(backend);
  t.after(() => new Promise(resolve => backend.close(resolve)));
  const app = express();
  app.use((req, res, next) => { req.session = { usuarioId: 6 }; next(); });
  montar(app, axios.create({ baseURL, timeout: 5000 }));
  const frontend = http.createServer(app);
  const base = await listen(frontend);
  t.after(() => new Promise(resolve => frontend.close(resolve)));
  const resposta = await fetch(`${base}/api/registros`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ ferramentaId: 3, tokensEntrada: 500, tokensSaida: 300 })
  });
  assert.equal(resposta.status, 201);
  assert.equal((await resposta.json()).id, 123);
  assert.equal(gravacoes, 1);
});

test('falha de confirmação orienta consultar histórico e não repete o POST', async t => {
  let chamadas = 0;
  const app = express();
  app.use((req, res, next) => { req.session = { usuarioId: 6 }; next(); });
  montar(app, { post: async () => {
    chamadas++;
    throw Object.assign(new Error('timeout'), { code: 'ECONNABORTED' });
  } });
  const server = http.createServer(app);
  const base = await listen(server);
  t.after(() => new Promise(resolve => server.close(resolve)));
  const resposta = await fetch(`${base}/api/registros`, {
    method: 'POST', headers: { 'content-type': 'application/json' }, body: '{}'
  });
  assert.equal(resposta.status, 504);
  assert.match((await resposta.json()).erro, /Atualize o histórico/);
  assert.equal(chamadas, 1);
});
