import { useState } from "react";
import { Link } from "react-router-dom";
import * as accountService from "../services/accountService";
import { getErrorMessage } from "../services/api";
import { useNotify } from "../context/NotifyContext";
import useFetch from "../utils/useFetch";
import useIdempotencyKey from "../utils/useIdempotencyKey";
import formatCurrency from "../utils/formatCurrency";
import Loading from "./Loading";
import ErrorMessage from "./ErrorMessage";
import ConfirmModal from "./ConfirmModal";

export const isValidAmount = (v) => /^\d{1,12}(\.\d{1,2})?$/.test(v) && Number(v) > 0;

/** Shared by Deposit and Withdraw. The server re-validates everything; this only saves a round trip. */
export default function MoneyForm({ title, verb, action }) {
  const { data: accounts, loading, error, reload } = useFetch(accountService.myAccounts);
  const notify = useNotify();
  const idem = useIdempotencyKey();
  const [accountId, setAccountId] = useState("");
  const [amount, setAmount] = useState("");
  const [formError, setFormError] = useState("");
  const [confirm, setConfirm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);

  if (loading && !accounts) return <Loading />;
  if (error && !accounts) return <ErrorMessage message={error} onRetry={reload} />;
  const selected = accounts.find((a) => String(a.id) === String(accountId));

  const review = (e) => {
    e.preventDefault(); setFormError("");
    if (!accountId) return setFormError("Select an account");
    if (!isValidAmount(amount)) return setFormError("Enter a positive amount with at most 2 decimals");
    setConfirm(true);
  };

  const submit = async () => {
    setBusy(true);
    try {
      const key = idem.keyFor(`${accountId}|${amount}`);   // same key if the user retries the same request
      const r = await action(Number(accountId), amount, key);
      idem.reset(); setResult(r); setConfirm(false); setAmount("");
      notify.success(r.replayed ? "This request was already processed. Showing the original result." : `${title} successful`);
      reload();
    } catch (err) {
      setConfirm(false); setFormError(getErrorMessage(err));
      notify.error(getErrorMessage(err));
    } finally { setBusy(false); }
  };

  return (
    <div style={{ maxWidth: 560 }}>
      <h4 className="mb-3">{title}</h4>
      <ErrorMessage message={formError} />
      <form onSubmit={review} className="card card-body shadow-sm" noValidate>
        <label className="form-label">Account</label>
        <select className="form-select mb-3" value={accountId} onChange={(e) => { setAccountId(e.target.value); setResult(null); }}>
          <option value="">Select account</option>
          {accounts.map((a) => <option key={a.id} value={a.id}>{a.accountNumber} ({a.accountType}) - {formatCurrency(a.balance)}</option>)}
        </select>
        <label className="form-label">Amount</label>
        <input className="form-control mb-3" inputMode="decimal" placeholder="0.00" value={amount} onChange={(e) => setAmount(e.target.value)} />
        <button className="btn btn-primary" disabled={busy}>Review {verb}</button>
      </form>
      {result && (
        <div className="alert alert-success mt-3">
          <div><strong>Reference:</strong> <span className="font-monospace">{result.transactionReference}</span></div>
          <div>Amount: {formatCurrency(result.amount)}</div>
          <div>New balance: {formatCurrency(result.destinationBalanceAfter ?? result.sourceBalanceAfter)}</div>
          <Link to={`/accounts/${result.destinationAccountId ?? result.sourceAccountId}/statement`}>View statement</Link>
        </div>
      )}
      <ConfirmModal show={confirm} title={`Confirm ${verb}`} busy={busy} onConfirm={submit} onCancel={() => setConfirm(false)}>
        {verb} <strong>{formatCurrency(amount)}</strong> {verb === "deposit" ? "into" : "from"} <strong>{selected?.accountNumber}</strong>?
      </ConfirmModal>
    </div>
  );
}
