import { Link } from "react-router-dom";

export default function NotFound() {
  return <div className="text-center mt-5"><h3>404</h3><p>Page not found.</p><Link to="/">Go to dashboard</Link></div>;
}
