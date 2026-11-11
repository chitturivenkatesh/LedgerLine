import formatCurrency from "../utils/formatCurrency";
import formatDate from "../utils/formatDate";
import StatusBadge from "./StatusBadge";

export default function TransactionTable({ transactions, showInitiator = false }) {
  if (!transactions?.length) return <p className="text-muted">No transactions yet.</p>;
  return (
    <div className="table-responsive">
      <table className="table table-hover align-middle bg-white">
        <thead className="table-light">
          <tr><th>Reference</th><th>Type</th><th className="text-end">Amount</th><th>Status</th><th>Source</th><th>Destination</th>
            {showInitiator && <th>Initiated by</th>}<th>Date</th></tr>
        </thead>
        <tbody>
          {transactions.map((t) => (
            <tr key={t.id}>
              <td className="font-monospace small">{t.transactionReference}</td>
              <td>{t.transactionType}</td>
              <td className="text-end">{formatCurrency(t.amount)}</td>
              <td><StatusBadge value={t.status} /></td>
              <td>{t.sourceAccountNumber || "-"}</td>
              <td>{t.destinationAccountNumber || "-"}</td>
              {showInitiator && <td>{t.initiatedBy}</td>}
              <td>{formatDate(t.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
