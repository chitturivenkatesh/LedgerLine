import { useState } from "react";
import { Link } from "react-router-dom";
import * as accountService from "../services/accountService";
import { getErrorMessage } from "../services/api";
import { useNotify } from "../context/NotifyContext";
import useFetch from "../utils/useFetch";
import formatCurrency from "../utils/formatCurrency";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";
import StatusBadge from "../components/StatusBadge";

export default function Accounts() {
  const { data, loading, error, reload } = useFetch(accountService.myAccounts);
  const notify = useNotify();
  const [type, setType] = useState("SAVINGS");
  const [busy, setBusy] = useState(false);

  const open = async () => {
    setBusy(true);
    try { const a = await accountService.createAccount(type); notify.success(`Account ${a.accountNumber} created`); reload(); }
    catch (e) { notify.error(getErrorMessage(e)); }
    finally { setBusy(false); }
  };

  if (loading && !data) return <Loading />;
  if (error && !data) return <ErrorMessage message={error} onRetry={reload} />;
  return (
    <>
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <h4 className="mb-0">My Accounts</h4>
        <div className="d-flex gap-2">
          <select className="form-select" value={type} onChange={(e) => setType(e.target.value)}>
            <option value="SAVINGS">Savings</option><option value="CURRENT">Current</option>
          </select>
          <button className="btn btn-primary text-nowrap" onClick={open} disabled={busy}>Open account</button>
        </div>
      </div>
      <div className="table-responsive">
        <table className="table table-hover align-middle bg-white">
          <thead className="table-light"><tr><th>Account number</th><th>Type</th><th className="text-end">Balance</th><th>Status</th><th /></tr></thead>
          <tbody>
            {data.map((a) => (
              <tr key={a.id}>
                <td className="font-monospace">{a.accountNumber}</td><td>{a.accountType}</td>
                <td className="text-end">{formatCurrency(a.balance)}</td><td><StatusBadge value={a.status} /></td>
                <td className="text-end"><Link to={`/accounts/${a.id}`}>Details</Link></td>
              </tr>
            ))}
            {data.length === 0 && <tr><td colSpan="5" className="text-muted text-center">No accounts yet</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}
