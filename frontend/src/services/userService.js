import api from "./api";

export const list = () => api.get("/api/users").then((r) => r.data);
export const create = (body) => api.post("/api/users", body).then((r) => r.data);
export const update = (id, body) => api.put(`/api/users/${id}`, body).then((r) => r.data);
export const disable = (id) => api.delete(`/api/users/${id}`);
