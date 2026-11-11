import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getErrorMessage } from "../services/api";
import ErrorMessage from "../components/ErrorMessage";

export default function Register() {
  const { signUp } = useAuth();
  const nav = useNavigate();
  const [f, setF] = useState({ fullName: "", email: "", password: "", confirm: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    if (!f.fullName.trim()) return setError("Full name is required");
    if (f.password.length < 8) return setError("Password must be at least 8 characters");
    if (f.password !== f.confirm) return setError("Passwords do not match");
    setBusy(true); setError("");
    try { await signUp(f.fullName, f.email, f.password); nav("/"); }
    catch (err) { setError(getErrorMessage(err)); }
    finally { setBusy(false); }
  };

  return (
    <div className="container" style={{ maxWidth: 420, marginTop: 60 }}>
      <h3 className="mb-4">Create account</h3>
      <ErrorMessage message={error} />
      <form onSubmit={submit} noValidate className="card card-body shadow-sm">
        <input className="form-control mb-3" placeholder="Full name" value={f.fullName} onChange={set("fullName")} />
        <input className="form-control mb-3" type="email" placeholder="Email" value={f.email} onChange={set("email")} />
        <input className="form-control mb-3" type="password" placeholder="Password (min 8)" value={f.password} onChange={set("password")} />
        <input className="form-control mb-3" type="password" placeholder="Confirm password" value={f.confirm} onChange={set("confirm")} />
        <button className="btn btn-primary w-100" disabled={busy}>{busy ? "Creating..." : "Register"}</button>
      </form>
      <p className="mt-3">Have an account? <Link to="/login">Login</Link></p>
    </div>
  );
}
