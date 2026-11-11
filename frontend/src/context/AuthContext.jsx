import { createContext, useContext, useEffect, useState } from "react";
import * as authService from "../services/authService";

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(!!localStorage.getItem("ll_token"));

  useEffect(() => {
    if (!localStorage.getItem("ll_token")) return;
    authService.me().then(setUser).catch(() => localStorage.removeItem("ll_token")).finally(() => setLoading(false));
  }, []);

  const startSession = (res) => { localStorage.setItem("ll_token", res.token); setUser(res.user); };
  const signIn = async (email, password) => startSession(await authService.login(email, password));
  const signUp = async (name, email, password) => startSession(await authService.register(name, email, password));
  const signOut = () => { localStorage.removeItem("ll_token"); setUser(null); };

  return <AuthContext.Provider value={{ user, loading, signIn, signUp, signOut }}>{children}</AuthContext.Provider>;
}
