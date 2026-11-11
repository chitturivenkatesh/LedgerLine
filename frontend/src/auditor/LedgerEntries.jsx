import * as ledgerService from "../services/ledgerService";
import useFetch from "../utils/useFetch";
import formatCurrency from "../utils/formatCurrency";
import formatDate from "../utils/formatDate";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";
import StatusBadge from "../components/StatusBadge";

export default function LedgerEntries() {
  const { data, loading, error, reload } = useFetch(ledgerService.all);
  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={reload} />;
  return (
    <>
      <h4 className="mb-3">Ledger Entries</h4>
      <div className="table-responsive">
        <table className="table table-hover table-sm align-middle bg-white">
          <thead className="table-light"><tr><th>#</th><th>Date</th><th>Transaction</th><th>Account</th><th>Entry</th><th className="text-end">Amount</th><th className="text-end">Balance after</th><th>By</th></tr></thead>
          <tbody>{data.map((e) => (
            <tr key={e.id}><td>{e.id}</td><td>{formatDate(e.createdAt)}</td><td className="font-monospace small">{e.transactionReference}</td>
              <td>{e.accountNumber}</td><td><StatusBadge value={e.entryType} /></td>
              <td className="text-end">{formatCurrency(e.amount)}</td><td className="text-end">{formatCurrency(e.balanceAfter)}</td><td>{e.initiatedBy}</td></tr>))}</tbody>
        </table>
      </div>
    </>
  );
}
