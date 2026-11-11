import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { NotifyProvider } from "./context/NotifyContext";
import ProtectedRoute from "./components/ProtectedRoute";
import RoleBasedRoute from "./components/RoleBasedRoute";
import Layout from "./components/Layout";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Accounts from "./pages/Accounts";
import AccountDetails from "./pages/AccountDetails";
import Deposit from "./pages/Deposit";
import Withdraw from "./pages/Withdraw";
import Transfer from "./pages/Transfer";
import Transactions from "./pages/Transactions";
import Statement from "./pages/Statement";
import Profile from "./pages/Profile";
import Unauthorized from "./pages/Unauthorized";
import NotFound from "./pages/NotFound";
import AdminDashboard from "./admin/AdminDashboard";
import ManageUsers from "./admin/ManageUsers";
import ManageAccounts from "./admin/ManageAccounts";
import AllTransactions from "./admin/AllTransactions";
import AuditorDashboard from "./auditor/AuditorDashboard";
import LedgerEntries from "./auditor/LedgerEntries";
import AuditLogs from "./auditor/AuditLogs";
import FailedAttempts from "./auditor/FailedAttempts";

const HOLDER = ["ACCOUNT_HOLDER"], ADMIN = ["ADMIN"], STAFF = ["ADMIN", "AUDITOR"];
const only = (roles, el) => <RoleBasedRoute roles={roles}>{el}</RoleBasedRoute>;

function Home() {
  const { user } = useAuth();
  if (user.role === "ADMIN") return <AdminDashboard />;
  if (user.role === "AUDITOR") return <AuditorDashboard />;
  return <Dashboard />;
}

export default function App() {
  return (
    <AuthProvider>
      <NotifyProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
              <Route path="/" element={<Home />} />
              <Route path="/profile" element={<Profile />} />
              <Route path="/unauthorized" element={<Unauthorized />} />
              <Route path="/accounts" element={only(HOLDER, <Accounts />)} />
              <Route path="/accounts/:id" element={<AccountDetails />} />
              <Route path="/accounts/:id/statement" element={<Statement />} />
              <Route path="/deposit" element={only(HOLDER, <Deposit />)} />
              <Route path="/withdraw" element={only(HOLDER, <Withdraw />)} />
              <Route path="/transfer" element={only(HOLDER, <Transfer />)} />
              <Route path="/transactions" element={only(HOLDER, <Transactions />)} />
              <Route path="/admin/users" element={only(ADMIN, <ManageUsers />)} />
              <Route path="/admin/accounts" element={only(ADMIN, <ManageAccounts />)} />
              <Route path="/all-transactions" element={only(STAFF, <AllTransactions />)} />
              <Route path="/auditor/ledger" element={only(STAFF, <LedgerEntries />)} />
              <Route path="/auditor/logs" element={only(STAFF, <AuditLogs />)} />
              <Route path="/auditor/failed" element={only(STAFF, <FailedAttempts />)} />
              <Route path="*" element={<NotFound />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </NotifyProvider>
    </AuthProvider>
  );
}
