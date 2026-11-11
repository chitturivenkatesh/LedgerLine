import { useAuth } from "../context/AuthContext";
import StatusBadge from "./StatusBadge";

export default function Navbar({ onToggle }) {
  const { user, signOut } = useAuth();
  return (
    <nav className="navbar navbar-dark bg-dark px-3">
      <div className="d-flex align-items-center">
        <button className="btn btn-outline-light btn-sm d-md-none me-2" onClick={onToggle} aria-label="Toggle menu">&#9776;</button>
        <span className="navbar-brand mb-0 h5">LedgerLine</span>
      </div>
      <div className="d-flex align-items-center gap-3 text-light">
        <span className="d-none d-sm-inline">{user.fullName}</span>
        <StatusBadge value={user.role} />
        <button className="btn btn-outline-light btn-sm" onClick={signOut}>Logout</button>
      </div>
    </nav>
  );
}
