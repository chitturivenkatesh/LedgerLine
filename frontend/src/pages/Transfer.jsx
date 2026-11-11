import { useState } from "react";
import { Link } from "react-router-dom";
import * as accountService from "../services/accountService";
import * as transactionService from "../services/transactionService";
import { getErrorMessage } from "../services/api";
import { useNotify } from "../context/NotifyContext";
import useFetch from "../utils/useFetch";
import useIdempotencyKey from "../utils/useIdempotencyKey";
import formatCurrency from "../utils/formatCurrency";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";
import ConfirmModal from "../components/ConfirmModal";
import { isValidAmount } from "../components/MoneyForm";

export default function Transfer() {
  const { data: accounts, loading, error, reload } = useFetch(accountService.myAccounts);
  const notify = useNotify();
  const idem = useIdempotencyKey();
  const [sourceId, setSourceId] = useState("");
  const [dest, setDest] = useState("");
  const [amount, setAmount] = useState("");
  const [formError, setFormError] = useState("");
  const [confirm, setConfirm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  const [retryable, setRetryable] = useState(false);

  if (loading && !accounts) return <Loading />;
  if (error && !accounts) return <ErrorMessage message={error} onRetry={reload} />;

  const review = (e) => {
    e.preventDefault(); setFormError("");
    if (!sourceId) return setFormError("Select a source account");
    if (!dest.trim()) return setFormError("Enter the destination account number");
    if (!isValidAmount(amount)) return setFormError("Enter a positive amount with at most 2 decimals");
    setConfirm(true);
  };

  const submit = async () => {
    setBusy(true); setRetryable(false);
    try {
      // The key is tied to this exact payload: a retry of the same transfer re-sends the SAME key.
      const key = idem.keyFor(`${sourceId}|${dest.trim()}|${amount}`);
      const r = await transactionService.transfer(Number(sourceId), dest.trim(), amount, key);
      idem.reset(); setResult(r); setConfirm(false); setAmount(""); setDest("");
      notify.success(r.replayed ? "Already processed - showing the original result." : "Transfer successful");
      reload();
    } catch (err) {
      setConfirm(false);
      const msg = getErrorMessage(err);
      setFormError(msg); notify.error(msg);
      // No HTTP response = outcome unknown. Retrying is safe because the key is preserved.
      if (!err.response) setRetryable(true);
    } finally { setBusy(false); }
  };

  return (
    <div style={{ maxWidth: 560 }}>
      <h4 className="mb-3">Transfer</h4>
      <ErrorMessage message={formError} />
      {retryable && <div className="alert alert-warning">Connection lost - we do not know if the transfer went through. Press <strong>Review transfer</strong> and confirm again: the same request key is reused, so it cannot be applied twice.</div>}
      <form onSubmit={review} className="card card-body shadow-sm" noValidate>
        <label className="form-label">From account</label>
        <select className="form-select mb-3" value={sourceId} onChange={(e) => { setSourceId(e.target.value); setResult(null); }}>
          <option value="">Select account</option>
          {accounts.map((a) => <option key={a.id} value={a.id}>{a.accountNumber} - {formatCurrency(a.balance)}</option>)}
        </select>
        <label className="form-label">Destination account number</label>
        <input className="form-control mb-3 font-monospace" placeholder="LL0000000000" value={dest} onChange={(e) => setDest(e.target.value)} />
        <label className="form-label">Amount</label>
        <input className="form-control mb-3" inputMode="decimal" placeholder="0.00" value={amount} onChange={(e) => setAmount(e.target.value)} />
        <button className="btn btn-primary" disabled={busy}>Review transfer</button>
      </form>
      {result && (
        <div className="alert alert-success mt-3">
          <div><strong>Reference:</strong> <span className="font-monospace">{result.transactionReference}</span></div>
          <div>Transferred {formatCurrency(result.amount)} to {result.destinationAccountNumber}</div>
          <div>Your new balance: {formatCurrency(result.sourceBalanceAfter)}</div>
          <Link to={`/accounts/${result.sourceAccountId}/statement`}>View statement</Link>
        </div>
      )}
      <ConfirmModal show={confirm} title="Confirm transfer" busy={busy} onConfirm={submit} onCancel={() => setConfirm(false)}>
        Send <strong>{formatCurrency(amount)}</strong> from <strong>{accounts.find((a) => String(a.id) === String(sourceId))?.accountNumber}</strong> to <strong>{dest}</strong>?
        <div className="small text-muted mt-2">Transfers cannot be undone.</div>
      </ConfirmModal>
    </div>
  );
}
