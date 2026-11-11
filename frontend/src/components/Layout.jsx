import { useState } from "react";
import { Outlet } from "react-router-dom";
import Navbar from "./Navbar";
import Sidebar from "./Sidebar";
import Footer from "./Footer";

export default function Layout() {
  const [open, setOpen] = useState(false);
  return (
    <div className="d-flex flex-column min-vh-100">
      <Navbar onToggle={() => setOpen(!open)} />
      <div className="d-flex flex-grow-1">
        <Sidebar open={open} onNavigate={() => setOpen(false)} />
        <main className="flex-grow-1 p-3 p-md-4" style={{ minWidth: 0 }}><Outlet /></main>
      </div>
      <Footer />
    </div>
  );
}
