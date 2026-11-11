import { Link, useParams } from "react-router-dom";
import * as accountService from "../services/accountService";
import useFetch from "../utils/useFetch";
import formatCurrency from "../utils/formatCurrency";
import formatDate from "../utils/formatDate";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";
import StatusBadge from "../components/StatusBadge";
import StatCard from "../components/StatCard";

export default function AccountDetails() {
  const { id } = useParams();
  const acc = useFetch(() => accountService.getAccount(id), [id]);
  const st = useFetch(() => accountService.getStatement(id), [id]);
  if (acc.loading || st.loading) return <Loading />;
  if (acc.error || st.error) return <ErrorMessage message={acc.error || st.error} />;
  const a = acc.data;
  return (
    <>
      <h4 className="mb-3">Account {a.accountNumber}</h4>
      <div className="row g-3 mb-4">
        <div className="col-md-4"><StatCard label="Balance" value={formatCurrency(a.balance)} /></div>
        <div className="col-md-4"><StatCard label="Type" value={a.accountType} /></div>
        <div className="col-md-4"><StatCard label="Status" value={<StatusBadge value={a.status} />} sub={`Holder: ${a.holderName}`} /></div>
      </div>
      <div className="d-flex justify-content-between align-items-center mb-2">
        <h6 className="mb-0">Latest ledger entries</h6>
        <Link to={`/accounts/${id}/statement`}>Full statement</Link>
      </div>
      <div className="table-responsive">
        <table className="table bg-white"><thead className="table-light"><tr><th>Date</th><th>Type</th><th className="text-end">Amount</th><th className="text-end">Balance after</th></tr></thead>
          <tbody>
            {[...st.data].reverse().slice(0, 5).map((e) => (
              <tr key={e.id}><td>{formatDate(e.createdAt)}</td><td><StatusBadge value={e.entryType} /></td>
                <td className="text-end">{formatCurrency(e.amount)}</td><td className="text-end">{formatCurrency(e.balanceAfter)}</td></tr>
            ))}
            {st.data.length === 0 && <tr><td colSpan="4" className="text-muted text-center">No entries yet</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}
