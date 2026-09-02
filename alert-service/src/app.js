import express from "express";
import { randomUUID } from "node:crypto";

export function createApp(options = {}) {
  const notifications = options.notifications ?? new Map();
  const idempotency = options.idempotency ?? new Map();
  const warningThreshold = Number(options.warningThreshold ?? process.env.WARNING_THRESHOLD ?? 70);
  const criticalThreshold = Number(options.criticalThreshold ?? process.env.CRITICAL_THRESHOLD ?? 90);
  const app = express();

  app.use(express.json({ limit: "64kb" }));

  app.get("/health", (_request, response) => {
    response.json({ status: "UP", service: "hestia-alert-service" });
  });

  app.post("/api/v1/notifications", (request, response) => {
    const errors = validate(request.body);
    if (errors.length > 0) {
      return response.status(400).json({ code: "VALIDATION_ERROR", errors });
    }

    const input = request.body;
    const existingId = idempotency.get(input.idempotencyKey);
    if (existingId) {
      return response.status(200).json(notifications.get(existingId));
    }

    const notification = {
      id: randomUUID(),
      empresaId: input.empresaId,
      origem: input.origem.trim(),
      tipo: input.tipo.trim(),
      valor: input.valor,
      mensagem: input.mensagem.trim(),
      idempotencyKey: input.idempotencyKey,
      nivel: classify(input.valor, warningThreshold, criticalThreshold),
      status: "CREATED",
      createdAt: new Date().toISOString(),
      correlationId: request.header("x-correlation-id") || randomUUID()
    };

    notifications.set(notification.id, notification);
    idempotency.set(notification.idempotencyKey, notification.id);
    return response.status(201).json(notification);
  });

  app.get("/api/v1/notifications/:id", (request, response) => {
    const notification = notifications.get(request.params.id);
    if (!notification) {
      return response.status(404).json({ code: "NOT_FOUND", message: "Notificacao nao encontrada" });
    }
    return response.json(notification);
  });

  app.get("/api/v1/notifications", (request, response) => {
    const empresaId = Number(request.query.empresaId);
    if (!Number.isSafeInteger(empresaId) || empresaId < 1) {
      return response.status(400).json({ code: "VALIDATION_ERROR", errors: ["empresaId invalido"] });
    }
    const result = [...notifications.values()].filter(item => item.empresaId === empresaId);
    return response.json(result);
  });

  app.use((error, _request, response, _next) => {
    if (error instanceof SyntaxError) {
      return response.status(400).json({ code: "INVALID_JSON", message: "JSON invalido" });
    }
    console.error(error);
    return response.status(500).json({ code: "INTERNAL_ERROR", message: "Erro interno" });
  });

  return app;
}

function classify(value, warningThreshold, criticalThreshold) {
  if (value >= criticalThreshold) return "CRITICAL";
  if (value >= warningThreshold) return "WARNING";
  return "INFO";
}

function validate(input) {
  if (!input || typeof input !== "object") return ["corpo obrigatorio"];
  const errors = [];
  if (!Number.isSafeInteger(input.empresaId) || input.empresaId < 1) errors.push("empresaId invalido");
  for (const field of ["origem", "tipo", "mensagem", "idempotencyKey"]) {
    if (typeof input[field] !== "string" || input[field].trim().length < 2) errors.push(`${field} invalido`);
  }
  if (typeof input.idempotencyKey === "string" && input.idempotencyKey.length < 8) errors.push("idempotencyKey invalido");
  if (typeof input.valor !== "number" || input.valor < 0 || input.valor > 100) errors.push("valor invalido");
  return [...new Set(errors)];
}
