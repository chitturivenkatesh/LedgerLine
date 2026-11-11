export default function StatCard({ label, value, sub }) {
  return (
    <div className="card shadow-sm h-100"><div className="card-body">
      <div className="text-muted small text-uppercase">{label}</div>
      <div className="fs-4 fw-semibold">{value}</div>
      {sub && <div className="small text-muted">{sub}</div>}
    </div></div>
  );
}
