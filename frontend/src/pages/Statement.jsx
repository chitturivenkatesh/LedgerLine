import { Link, useParams } from "react-router-dom";
import * as accountService from "../services/accountService";
import useFetch from "../utils/useFetch";
import formatCurrency from "../utils/formatCurrency";
import formatDate from "../utils/formatDate";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";

export default function Statement() {
  const { id } = useParams();
  const { data, loading, error, reload } = useFetch(() => accountService.getStatement(id), [id]);
  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={reload} />;
  return (
    <>
      <div className="d-flex justify-content-between mb-3">
        <h4 className="mb-0">Statement {data[0]?.accountNumber || ""}</h4>
        <Link to={`/accounts/${id}`}>Back to account</Link>
      </div>
      <div className="table-responsive">
        <table className="table table-hover align-middle bg-white">
          <thead className="table-light">
            <tr><th>Date</th><th>Reference</th><th>Type</th><th className="text-end">Debit</th><th className="text-end">Credit</th>
              <th className="text-end">Running balance</th><th>Initiated by</th></tr>
          </thead>
          <tbody>
            {data.map((e) => (
              <tr key={e.id}>
                <td>{formatDate(e.createdAt)}</td>
                <td className="font-monospace small">{e.transactionReference}</td>
                <td>{e.transactionType}</td>
                <td className="text-end text-danger">{e.entryType === "DEBIT" ? formatCurrency(e.amount) : ""}</td>
                <td className="text-end text-success">{e.entryType === "CREDIT" ? formatCurrency(e.amount) : ""}</td>
                <td className="text-end fw-semibold">{formatCurrency(e.balanceAfter)}</td>
                <td>{e.initiatedBy}</td>
              </tr>
            ))}
            {data.length === 0 && <tr><td colSpan="7" className="text-center text-muted">No entries</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}
