import assert from "node:assert/strict";
import { after, before, test } from "node:test";
import { createApp } from "../src/app.js";

let server;
let baseUrl;

before(async () => {
  server = createApp().listen(0);
  await new Promise(resolve => server.once("listening", resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(() => server.close());

test("health check", async () => {
  const response = await fetch(`${baseUrl}/health`);
  assert.equal(response.status, 200);
  assert.equal((await response.json()).status, "UP");
});

test("creates, classifies and reads a notification", async () => {
  const input = {
    empresaId: 1,
    origem: "consumo",
    tipo: "limite",
    valor: 95,
    mensagem: "Limite de consumo atingido",
    idempotencyKey: "empresa-1-evento-1"
  };
  const createdResponse = await fetch(`${baseUrl}/api/v1/notifications`, {
    method: "POST",
    headers: { "content-type": "application/json", "x-correlation-id": "test-1" },
    body: JSON.stringify(input)
  });
  assert.equal(createdResponse.status, 201);
  const created = await createdResponse.json();
  assert.equal(created.nivel, "CRITICAL");
  assert.equal(created.correlationId, "test-1");

  const readResponse = await fetch(`${baseUrl}/api/v1/notifications/${created.id}`);
  assert.equal(readResponse.status, 200);
  assert.equal((await readResponse.json()).id, created.id);
});

test("is idempotent", async () => {
  const input = {
    empresaId: 2,
    origem: "risco",
    tipo: "avaliacao",
    valor: 75,
    mensagem: "Risco elevado",
    idempotencyKey: "empresa-2-evento-1"
  };
  const first = await fetch(`${baseUrl}/api/v1/notifications`, {
    method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(input)
  });
  const second = await fetch(`${baseUrl}/api/v1/notifications`, {
    method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(input)
  });
  assert.equal(first.status, 201);
  assert.equal(second.status, 200);
  assert.equal((await first.json()).id, (await second.json()).id);
});

test("rejects invalid input and missing resource", async () => {
  const invalid = await fetch(`${baseUrl}/api/v1/notifications`, {
    method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ empresaId: 0 })
  });
  assert.equal(invalid.status, 400);

  const missing = await fetch(`${baseUrl}/api/v1/notifications/does-not-exist`);
  assert.equal(missing.status, 404);
});
