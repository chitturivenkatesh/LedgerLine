import { useState } from "react";
import * as accountService from "../services/accountService";
import * as userService from "../services/userService";
import { getErrorMessage } from "../services/api";
import { useNotify } from "../context/NotifyContext";
import useFetch from "../utils/useFetch";
import formatCurrency from "../utils/formatCurrency";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";

export default function ManageAccounts() {
  const acc = useFetch(accountService.allAccounts);
  const usr = useFetch(userService.list);
  const notify = useNotify();
  const [holderId, setHolderId] = useState("");
  const [type, setType] = useState("SAVINGS");

  const create = async (e) => {
    e.preventDefault();
    try { const a = await accountService.createAccount(type, Number(holderId)); notify.success(`Created ${a.accountNumber}`); acc.reload(); }
    catch (err) { notify.error(getErrorMessage(err)); }
  };
  const setStatus = async (id, status) => {
    try { await accountService.setAccountStatus(id, status); notify.success("Status updated"); acc.reload(); }
    catch (err) { notify.error(getErrorMessage(err)); }
  };

  if ((acc.loading && !acc.data) || (usr.loading && !usr.data)) return <Loading />;
  if (acc.error || usr.error) return <ErrorMessage message={acc.error || usr.error} onRetry={() => { acc.reload(); usr.reload(); }} />;
  const holders = usr.data.filter((u) => u.role === "ACCOUNT_HOLDER" && u.enabled);
  return (
    <>
      <h4 className="mb-3">Manage Accounts</h4>
      <form onSubmit={create} className="card card-body shadow-sm mb-4">
        <div className="row g-2">
          <div className="col-md-5"><select className="form-select" value={holderId} onChange={(e) => setHolderId(e.target.value)} required>
            <option value="">Select account holder</option>{holders.map((u) => <option key={u.id} value={u.id}>{u.fullName} ({u.email})</option>)}</select></div>
          <div className="col-md-3"><select className="form-select" value={type} onChange={(e) => setType(e.target.value)}>
            <option value="SAVINGS">Savings</option><option value="CURRENT">Current</option></select></div>
          <div className="col-md-4"><button className="btn btn-primary w-100">Create account</button></div>
        </div>
      </form>
      <div className="table-responsive">
        <table className="table align-middle bg-white">
          <thead className="table-light"><tr><th>Number</th><th>Holder</th><th>Type</th><th className="text-end">Balance</th><th>Status</th></tr></thead>
          <tbody>
            {acc.data.map((a) => (
              <tr key={a.id}>
                <td className="font-monospace">{a.accountNumber}</td><td>{a.holderName}</td><td>{a.accountType}</td>
                <td className="text-end">{formatCurrency(a.balance)}</td>
                <td><select className="form-select form-select-sm" value={a.status} onChange={(e) => setStatus(a.id, e.target.value)}>
                  <option>ACTIVE</option><option>FROZEN</option><option>CLOSED</option></select></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
