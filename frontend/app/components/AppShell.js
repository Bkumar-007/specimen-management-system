"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { clearSession, getToken, getUser } from "../../lib/api";
import BarcodeScan from "./BarcodeScan";
import { ToastHost } from "./ui";

const NAV = [
  { href: "/dashboard", label: "Work queue" },
  { href: "/orders", label: "Orders" },
  { href: "/specimens", label: "Specimens" },
  { href: "/instruments", label: "Instrument" },
  { href: "/admin", label: "Admin", adminOnly: true },
  { href: "/audit", label: "Audit", adminOnly: true },
];

/** Auth gate + navigation chrome for all signed-in pages. */
export default function AppShell({ title, actions, children }) {
  const router = useRouter();
  const pathname = usePathname();
  const [user, setUser] = useState(null);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const u = getUser();
    if (!getToken() || !u) {
      router.replace("/login");
      return;
    }
    setUser(u);
  }, [router]);

  useEffect(() => {
    setMenuOpen(false);
  }, [pathname]);

  function logout() {
    clearSession();
    router.replace("/login");
  }

  if (!user) {
    return (
      <main className="shell">
        <div className="content">
          <div className="skeleton" aria-busy="true" aria-label="Loading">
            <div className="skeletonRow" style={{ width: "40%" }} />
            <div className="skeletonRow" style={{ width: "70%" }} />
            <div className="skeletonRow" style={{ width: "55%" }} />
          </div>
        </div>
      </main>
    );
  }

  const links = NAV.filter((n) => !n.adminOnly || user.role === "LAB_ADMIN");

  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbarMain">
          <span className="brand">Specimen Management</span>
          <button
            className="menuToggle"
            type="button"
            aria-expanded={menuOpen}
            aria-controls="app-nav"
            onClick={() => setMenuOpen((o) => !o)}
          >
            {menuOpen ? "Close" : "Menu"}
          </button>
        </div>
        <nav id="app-nav" className={`nav ${menuOpen ? "open" : ""}`}>
          {links.map((n) => (
            <Link
              key={n.href}
              href={n.href}
              className={pathname === n.href || pathname.startsWith(`${n.href}/`) ? "navLink active" : "navLink"}
            >
              {n.label}
            </Link>
          ))}
          <div className="userBox mobileOnly">
            <span className="muted">{user.username} ({user.role})</span>
            <button className="btn secondary" type="button" onClick={logout}>Sign out</button>
          </div>
        </nav>
        <BarcodeScan />
        <div className="userBox desktopOnly">
          <span className="muted">{user.username} ({user.role})</span>
          <button className="btn secondary" type="button" onClick={logout}>Sign out</button>
        </div>
      </header>
      <main className="content">
        <div className="pageHead">
          <div>
            <p className="pageEyebrow">Specimen Management</p>
            <h1>{title}</h1>
          </div>
          <div className="actions">{actions}</div>
        </div>
        {children}
      </main>
      <ToastHost />
    </div>
  );
}
