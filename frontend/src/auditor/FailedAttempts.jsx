import * as ledgerService from "../services/ledgerService";
import useFetch from "../utils/useFetch";
import formatCurrency from "../utils/formatCurrency";
import formatDate from "../utils/formatDate";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";

export default function FailedAttempts() {
  const { data, loading, error, reload } = useFetch(ledgerService.attempts);
  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={reload} />;
  return (
    <>
      <h4 className="mb-3">Refused / Failed Attempts</h4>
      <div className="table-responsive">
        <table className="table table-hover table-sm align-middle bg-white">
          <thead className="table-light"><tr><th>Date</th><th>Request</th><th>Type</th><th>User</th><th>Account</th><th className="text-end">Amount</th><th>Reason</th></tr></thead>
          <tbody>{data.map((a) => (
            <tr key={a.id}><td>{formatDate(a.createdAt)}</td><td className="font-monospace small">{a.requestReference}</td><td>{a.transactionType}</td>
              <td>{a.initiatedBy ?? "-"}</td><td>{a.accountId ?? "-"}</td><td className="text-end">{formatCurrency(a.amount)}</td>
              <td className="text-danger">{a.failureReason}</td></tr>))}
            {data.length === 0 && <tr><td colSpan="7" className="text-center text-muted">No refused attempts</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}
