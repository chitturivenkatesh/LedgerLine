import api from "./api";

export const myAccounts = () => api.get("/api/accounts/my-accounts").then((r) => r.data);
export const allAccounts = () => api.get("/api/accounts").then((r) => r.data);
export const getAccount = (id) => api.get(`/api/accounts/${id}`).then((r) => r.data);
export const getStatement = (id) => api.get(`/api/accounts/${id}/statement`).then((r) => r.data);
export const createAccount = (accountType, holderId) =>
  api.post("/api/accounts", { accountType, holderId: holderId || null }).then((r) => r.data);
export const setAccountStatus = (id, status) => api.put(`/api/accounts/${id}/status`, { status }).then((r) => r.data);
