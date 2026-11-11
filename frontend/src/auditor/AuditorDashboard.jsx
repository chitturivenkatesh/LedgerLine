import { Link } from "react-router-dom";
import * as ledgerService from "../services/ledgerService";
import useFetch from "../utils/useFetch";
import formatCurrency from "../utils/formatCurrency";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";
import StatCard from "../components/StatCard";

export default function AuditorDashboard() {
  const { data: s, loading, error, reload } = useFetch(ledgerService.stats);
  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={reload} />;
  return (
    <>
      <h4 className="mb-1">Auditor Dashboard</h4>
      <p className="text-muted">Read-only access. Nothing here can change a balance or a ledger entry.</p>
      <div className="row g-3 mb-4">
        <div className="col-6 col-md-3"><StatCard label="Ledger entries" value={s.ledgerEntries} /></div>
        <div className="col-6 col-md-3"><StatCard label="Transactions" value={s.totalTransactions} /></div>
        <div className="col-6 col-md-3"><StatCard label="Refused attempts" value={s.failedAttempts} /></div>
        <div className="col-6 col-md-3"><StatCard label="Total balance" value={formatCurrency(s.totalBalance)} /></div>
      </div>
      <div className="d-flex gap-2 flex-wrap">
        <Link className="btn btn-primary" to="/auditor/ledger">Ledger entries</Link>
        <Link className="btn btn-outline-primary" to="/auditor/logs">Audit logs</Link>
        <Link className="btn btn-outline-primary" to="/auditor/failed">Failed attempts</Link>
        <Link className="btn btn-outline-primary" to="/all-transactions">Inspect transactions</Link>
      </div>
    </>
  );
}
