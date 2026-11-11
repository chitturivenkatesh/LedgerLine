import { Link } from "react-router-dom";
import * as ledgerService from "../services/ledgerService";
import useFetch from "../utils/useFetch";
import formatCurrency from "../utils/formatCurrency";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";
import StatCard from "../components/StatCard";

export default function AdminDashboard() {
  const { data: s, loading, error, reload } = useFetch(ledgerService.stats);
  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={reload} />;
  return (
    <>
      <h4 className="mb-3">Admin Dashboard</h4>
      <div className="row g-3 mb-4">
        <div className="col-6 col-md-3"><StatCard label="Users" value={s.totalUsers} /></div>
        <div className="col-6 col-md-3"><StatCard label="Accounts" value={s.totalAccounts} /></div>
        <div className="col-6 col-md-3"><StatCard label="Transactions" value={s.totalTransactions} /></div>
        <div className="col-6 col-md-3"><StatCard label="Refused attempts" value={s.failedAttempts} /></div>
        <div className="col-12 col-md-6"><StatCard label="Total balance held" value={formatCurrency(s.totalBalance)} /></div>
        <div className="col-12 col-md-6"><StatCard label="Ledger entries" value={s.ledgerEntries} /></div>
      </div>
      <div className="d-flex gap-2 flex-wrap">
        <Link className="btn btn-primary" to="/admin/users">Manage users</Link>
        <Link className="btn btn-outline-primary" to="/admin/accounts">Manage accounts</Link>
        <Link className="btn btn-outline-primary" to="/all-transactions">All transactions</Link>
      </div>
    </>
  );
}
