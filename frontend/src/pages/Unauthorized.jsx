import { Link } from "react-router-dom";

export default function Unauthorized() {
  return <div className="text-center mt-5"><h3>403</h3><p>You do not have access to this page.</p><Link to="/">Go to dashboard</Link></div>;
}
