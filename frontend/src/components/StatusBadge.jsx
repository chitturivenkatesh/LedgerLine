const colors = { SUCCESS: "success", ACTIVE: "success", FAILED: "danger", FROZEN: "warning", CLOSED: "secondary",
  DEBIT: "danger", CREDIT: "success", ADMIN: "dark", AUDITOR: "info", ACCOUNT_HOLDER: "primary" };

export default function StatusBadge({ value }) {
  return <span className={`badge text-bg-${colors[value] || "secondary"}`}>{String(value).replace("_", " ")}</span>;
}
