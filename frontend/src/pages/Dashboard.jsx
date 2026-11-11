import { Link } from "react-router-dom";
import * as accountService from "../services/accountService";
import * as transactionService from "../services/transactionService";
import useFetch from "../utils/useFetch";
import formatCurrency from "../utils/formatCurrency";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";
import StatCard from "../components/StatCard";
import TransactionTable from "../components/TransactionTable";

export default function Dashboard() {
  const acc = useFetch(accountService.myAccounts);
  const tx = useFetch(transactionService.list);
  if (acc.loading || tx.loading) return <Loading />;
  if (acc.error || tx.error) return <ErrorMessage message={acc.error || tx.error} onRetry={() => { acc.reload(); tx.reload(); }} />;

  const total = acc.data.reduce((s, a) => s + Number(a.balance), 0);
  return (
    <>
      <h4 className="mb-3">Dashboard</h4>
      <div className="row g-3 mb-4">
        <div className="col-12 col-md-4"><StatCard label="Total balance" value={formatCurrency(total)} /></div>
        <div className="col-6 col-md-4"><StatCard label="Accounts" value={acc.data.length} /></div>
        <div className="col-6 col-md-4"><StatCard label="Transactions" value={tx.data.length} sub="most recent 100" /></div>
      </div>
      <div className="mb-4 d-flex gap-2 flex-wrap">
        <Link className="btn btn-primary" to="/deposit">Deposit</Link>
        <Link className="btn btn-outline-primary" to="/withdraw">Withdraw</Link>
        <Link className="btn btn-outline-primary" to="/transfer">Transfer</Link>
      </div>
      <h6>Your accounts</h6>
      <div className="row g-3 mb-4">
        {acc.data.map((a) => (
          <div className="col-12 col-md-6" key={a.id}>
            <Link to={`/accounts/${a.id}`} className="text-decoration-none text-dark">
              <StatCard label={`${a.accountType} - ${a.accountNumber}`} value={formatCurrency(a.balance)} sub={a.status} />
            </Link>
          </div>
        ))}
        {acc.data.length === 0 && <p className="text-muted">No accounts yet. <Link to="/accounts">Open one</Link>.</p>}
      </div>
      <h6>Recent transactions</h6>
      <TransactionTable transactions={tx.data.slice(0, 5)} />
    </>
  );
}
