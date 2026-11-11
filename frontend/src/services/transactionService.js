import api from "./api";

// Amount is sent as a string so no floating point value ever represents money on the wire.
const post = (path, body, key) =>
  api.post(path, body, { headers: { "Idempotency-Key": key } }).then((r) => ({ ...r.data, replayed: r.headers["idempotent-replayed"] === "true" }));

export const deposit = (accountId, amount, key) => post("/api/transactions/deposit", { accountId, amount }, key);
export const withdraw = (accountId, amount, key) => post("/api/transactions/withdraw", { accountId, amount }, key);
export const transfer = (sourceAccountId, destinationAccountNumber, amount, key) =>
  post("/api/transactions/transfer", { sourceAccountId, destinationAccountNumber, amount }, key);
export const list = () => api.get("/api/transactions").then((r) => r.data);
export const get = (id) => api.get(`/api/transactions/${id}`).then((r) => r.data);
