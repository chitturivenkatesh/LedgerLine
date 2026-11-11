import { useAuth } from "../context/AuthContext";
import StatusBadge from "../components/StatusBadge";
import formatDate from "../utils/formatDate";

export default function Profile() {
  const { user } = useAuth();
  return (
    <div style={{ maxWidth: 520 }}>
      <h4 className="mb-3">Profile</h4>
      <div className="card card-body shadow-sm">
        <dl className="row mb-0">
          <dt className="col-4">Name</dt><dd className="col-8">{user.fullName}</dd>
          <dt className="col-4">Email</dt><dd className="col-8">{user.email}</dd>
          <dt className="col-4">Role</dt><dd className="col-8"><StatusBadge value={user.role} /></dd>
          <dt className="col-4">Member since</dt><dd className="col-8">{formatDate(user.createdAt)}</dd>
        </dl>
      </div>
    </div>
  );
}
