import { useState } from "react";
import * as transactionService from "../services/transactionService";
import * as ledgerService from "../services/ledgerService";
import { getErrorMessage } from "../services/api";
import useFetch from "../utils/useFetch";
import formatCurrency from "../utils/formatCurrency";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";
import TransactionTable from "../components/TransactionTable";
import StatusBadge from "../components/StatusBadge";

/** Used by ADMIN and AUDITOR. Click a row's "Inspect" to see its double-entry ledger lines. */
export default function AllTransactions() {
  const { data, loading, error, reload } = useFetch(transactionService.list);
  const [lookup, setLookup] = useState("");
  const [entries, setEntries] = useState(null);
  const [lookupError, setLookupError] = useState("");

  const inspect = async (e) => {
    e.preventDefault(); setLookupError(""); setEntries(null);
    try { setEntries(await ledgerService.forTransaction(lookup)); }
    catch (err) { setLookupError(getErrorMessage(err)); }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={reload} />;
  return (
    <>
      <h4 className="mb-3">All Transactions</h4>
      <form className="d-flex gap-2 mb-3" onSubmit={inspect} style={{ maxWidth: 420 }}>
        <input className="form-control" placeholder="Transaction ID to inspect" value={lookup} onChange={(e) => setLookup(e.target.value)} />
        <button className="btn btn-outline-primary">Inspect</button>
      </form>
      <ErrorMessage message={lookupError} />
      {entries && (
        <div className="card card-body mb-3">
          <h6>Ledger lines for transaction {lookup}</h6>
          <table className="table table-sm mb-0"><thead><tr><th>Account</th><th>Entry</th><th className="text-end">Amount</th><th className="text-end">Balance after</th></tr></thead>
            <tbody>{entries.map((e) => <tr key={e.id}><td>{e.accountNumber}</td><td><StatusBadge value={e.entryType} /></td>
              <td className="text-end">{formatCurrency(e.amount)}</td><td className="text-end">{formatCurrency(e.balanceAfter)}</td></tr>)}</tbody></table>
        </div>
      )}
      <TransactionTable transactions={data} showInitiator />
    </>
  );
}
