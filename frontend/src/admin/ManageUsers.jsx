import { useState } from "react";
import * as userService from "../services/userService";
import { getErrorMessage } from "../services/api";
import { useNotify } from "../context/NotifyContext";
import { useAuth } from "../context/AuthContext";
import useFetch from "../utils/useFetch";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";
import StatusBadge from "../components/StatusBadge";
import ConfirmModal from "../components/ConfirmModal";

const empty = { fullName: "", email: "", password: "", role: "ACCOUNT_HOLDER" };

export default function ManageUsers() {
  const { data, loading, error, reload } = useFetch(userService.list);
  const { user: me } = useAuth();
  const notify = useNotify();
  const [form, setForm] = useState(empty);
  const [busy, setBusy] = useState(false);
  const [toDisable, setToDisable] = useState(null);

  const create = async (e) => {
    e.preventDefault(); setBusy(true);
    try { await userService.create(form); notify.success("User created"); setForm(empty); reload(); }
    catch (err) { notify.error(getErrorMessage(err)); }
    finally { setBusy(false); }
  };
  const patch = async (id, body) => {
    try { await userService.update(id, body); notify.success("User updated"); reload(); }
    catch (err) { notify.error(getErrorMessage(err)); }
  };
  const disable = async () => {
    try { await userService.disable(toDisable.id); notify.success("User disabled"); reload(); }
    catch (err) { notify.error(getErrorMessage(err)); }
    setToDisable(null);
  };

  if (loading && !data) return <Loading />;
  if (error && !data) return <ErrorMessage message={error} onRetry={reload} />;
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  return (
    <>
      <h4 className="mb-3">Manage Users</h4>
      <form onSubmit={create} className="card card-body shadow-sm mb-4">
        <div className="row g-2">
          <div className="col-md-3"><input className="form-control" placeholder="Full name" value={form.fullName} onChange={set("fullName")} required /></div>
          <div className="col-md-3"><input className="form-control" type="email" placeholder="Email" value={form.email} onChange={set("email")} required /></div>
          <div className="col-md-2"><input className="form-control" type="password" placeholder="Password (min 8)" minLength="8" value={form.password} onChange={set("password")} required /></div>
          <div className="col-md-2"><select className="form-select" value={form.role} onChange={set("role")}>
            <option>ACCOUNT_HOLDER</option><option>AUDITOR</option><option>ADMIN</option></select></div>
          <div className="col-md-2"><button className="btn btn-primary w-100" disabled={busy}>Create user</button></div>
        </div>
      </form>
      <div className="table-responsive">
        <table className="table align-middle bg-white">
          <thead className="table-light"><tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Enabled</th><th /></tr></thead>
          <tbody>
            {data.map((u) => (
              <tr key={u.id}>
                <td>{u.id}</td><td>{u.fullName}</td><td>{u.email}</td>
                <td>{u.id === me.id ? <StatusBadge value={u.role} /> :
                  <select className="form-select form-select-sm" value={u.role} onChange={(e) => patch(u.id, { role: e.target.value })}>
                    <option>ACCOUNT_HOLDER</option><option>AUDITOR</option><option>ADMIN</option></select>}</td>
                <td>{u.enabled ? <span className="badge text-bg-success">Yes</span> : <span className="badge text-bg-secondary">No</span>}</td>
                <td className="text-end">
                  {u.id !== me.id && (u.enabled
                    ? <button className="btn btn-sm btn-outline-danger" onClick={() => setToDisable(u)}>Disable</button>
                    : <button className="btn btn-sm btn-outline-success" onClick={() => patch(u.id, { enabled: true })}>Enable</button>)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ConfirmModal show={!!toDisable} title="Disable user" confirmText="Disable" onConfirm={disable} onCancel={() => setToDisable(null)}>
        Disable <strong>{toDisable?.email}</strong>? They will lose access immediately. Their history is kept.
      </ConfirmModal>
    </>
  );
}
