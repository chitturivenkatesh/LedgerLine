import * as ledgerService from "../services/ledgerService";
import useFetch from "../utils/useFetch";
import formatDate from "../utils/formatDate";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";

export default function AuditLogs() {
  const { data, loading, error, reload } = useFetch(ledgerService.auditLogs);
  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={reload} />;
  return (
    <>
      <h4 className="mb-3">Audit Logs</h4>
      <div className="table-responsive">
        <table className="table table-hover table-sm align-middle bg-white">
          <thead className="table-light"><tr><th>#</th><th>Date</th><th>User</th><th>Action</th><th>Entity</th><th>Details</th></tr></thead>
          <tbody>{data.map((l) => (
            <tr key={l.id}><td>{l.id}</td><td>{formatDate(l.createdAt)}</td><td>{l.userId ?? "-"}</td><td>{l.action}</td>
              <td>{l.entityType} {l.entityId}</td><td className="small text-muted">{l.details}</td></tr>))}</tbody>
        </table>
      </div>
    </>
  );
}
