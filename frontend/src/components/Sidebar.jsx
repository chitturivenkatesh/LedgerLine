import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const holder = [
  ["/", "Dashboard"], ["/accounts", "My Accounts"], ["/deposit", "Deposit"], ["/withdraw", "Withdraw"],
  ["/transfer", "Transfer"], ["/transactions", "Transactions"], ["/profile", "Profile"],
];
const admin = [
  ["/", "Admin Dashboard"], ["/admin/users", "Manage Users"], ["/admin/accounts", "Manage Accounts"],
  ["/all-transactions", "All Transactions"], ["/auditor/ledger", "Ledger"], ["/auditor/logs", "Audit Logs"],
  ["/auditor/failed", "Failed Attempts"], ["/profile", "Profile"],
];
const auditor = [
  ["/", "Auditor Dashboard"], ["/auditor/ledger", "Ledger Entries"], ["/all-transactions", "Transactions"],
  ["/auditor/logs", "Audit Logs"], ["/auditor/failed", "Failed Attempts"], ["/profile", "Profile"],
];

export default function Sidebar({ open, onNavigate }) {
  const { user } = useAuth();
  const links = user.role === "ADMIN" ? admin : user.role === "AUDITOR" ? auditor : holder;
  return (
    <aside className={`sidebar bg-white border-end ${open ? "open" : ""}`}>
      <ul className="nav flex-column p-2">
        {links.map(([to, label]) => (
          <li className="nav-item" key={to}>
            <NavLink to={to} end={to === "/"} onClick={onNavigate}
                     className={({ isActive }) => "nav-link rounded " + (isActive ? "active bg-primary text-white" : "text-dark")}>
              {label}
            </NavLink>
          </li>
        ))}
      </ul>
    </aside>
  );
}
