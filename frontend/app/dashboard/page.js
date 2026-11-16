"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api } from "../../lib/api";
import AppShell from "../components/AppShell";
import { EmptyState, ErrorNote, Skeleton, StatusBadge, fmt } from "../components/ui";

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [recent, setRecent] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([api("/api/stats"), api("/api/specimens")])
      .then(([s, specimens]) => {
        setStats(s);
        setRecent(specimens.slice(0, 8));
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <AppShell
      title="Dashboard"
      actions={<Link className="btn" href="/orders/new">New order</Link>}
    >
      <ErrorNote error={error} />
      {loading ? (
        <section className="card"><Skeleton rows={6} /></section>
      ) : (
        <>
          {stats ? (
            <section className="card">
              <h2>Specimens by status</h2>
              <div className="stats">
                {Object.entries(stats.bySpecimenStatus).map(([status, count]) => (
                  <Link key={status} className="stat" href={`/specimens?status=${status}`}>
                    <div className="n">{count}</div>
                    <StatusBadge status={status} />
                  </Link>
                ))}
                <div className="stat">
                  <div className="n">{stats.totalOrders}</div>
                  <span className="muted">Orders</span>
                </div>
              </div>
            </section>
          ) : null}

          <section className="card">
            <h2>Recent specimens</h2>
            {recent.length === 0 ? (
              <EmptyState
                title="No specimens yet"
                detail="Create an order, then accession a specimen to get a barcode."
                href="/orders/new"
                actionLabel="Create first order"
              />
            ) : (
              <div className="tableWrap">
                <table className="table">
                  <thead>
                    <tr><th>Barcode</th><th>Patient</th><th>Order</th><th>Status</th><th>Received</th></tr>
                  </thead>
                  <tbody>
                    {recent.map((s) => (
                      <tr key={s.id}>
                        <td className="mono"><Link href={`/specimens/${s.id}`}>{s.barcode}</Link></td>
                        <td>{s.patientName} <span className="muted">({s.mrn})</span></td>
                        <td><Link href={`/orders/${s.orderId}`}>{s.orderNumber}</Link></td>
                        <td><StatusBadge status={s.status} /></td>
                        <td>{fmt(s.receivedAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </AppShell>
  );
}
