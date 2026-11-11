import api from "./api";

export const all = () => api.get("/api/ledger").then((r) => r.data);
export const forTransaction = (id) => api.get(`/api/ledger/transaction/${id}`).then((r) => r.data);
export const auditLogs = () => api.get("/api/audit/logs").then((r) => r.data);
export const attempts = () => api.get("/api/audit/attempts").then((r) => r.data);
export const stats = () => api.get("/api/audit/stats").then((r) => r.data);
