import axios from "axios";

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080" });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("ll_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (error) => {
    const url = error.config?.url || "";
    const isAuthCall = url.startsWith("/api/auth/login") || url.startsWith("/api/auth/register");
    if (error.response?.status === 401 && !isAuthCall) {
      // Session expired or token invalid: clear it and send the user to login.
      localStorage.removeItem("ll_token");
      window.location.href = "/login?expired=1";
    }
    return Promise.reject(error);
  }
);

export function getErrorMessage(err) {
  if (!err.response) return "Cannot reach the server. Check your connection and try again.";
  const d = err.response.data;
  if (d?.fieldErrors) return Object.values(d.fieldErrors).join(", ");
  if (d?.message) return d.message;
  if (err.response.status === 403) return "You are not allowed to do this.";
  return "Something went wrong.";
}

export default api;
