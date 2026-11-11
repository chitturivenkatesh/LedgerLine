import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getErrorMessage } from "../services/api";
import ErrorMessage from "../components/ErrorMessage";

export default function Login() {
  const { signIn } = useAuth();
  const nav = useNavigate();
  const [params] = useSearchParams();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState(params.get("expired") ? "Session expired. Please log in again." : "");
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (!form.email || !form.password) return setError("Email and password are required");
    setBusy(true); setError("");
    try { await signIn(form.email, form.password); nav("/"); }
    catch (err) { setError(getErrorMessage(err)); }
    finally { setBusy(false); }
  };

  return (
    <div className="container" style={{ maxWidth: 420, marginTop: 80 }}>
      <h3 className="mb-1">LedgerLine</h3>
      <p className="text-muted mb-4">Money movement with a ledger you can prove</p>
      <ErrorMessage message={error} />
      <form onSubmit={submit} noValidate className="card card-body shadow-sm">
        <input className="form-control mb-3" type="email" placeholder="Email" autoComplete="username" value={form.email}
               onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <input className="form-control mb-3" type="password" placeholder="Password" autoComplete="current-password" value={form.password}
               onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <button className="btn btn-primary w-100" disabled={busy}>{busy ? "Signing in..." : "Login"}</button>
      </form>
      <p className="mt-3">No account? <Link to="/register">Register</Link></p>
    </div>
  );
}
