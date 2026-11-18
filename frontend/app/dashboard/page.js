"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { api, getUser } from "../../lib/api";
import AppShell from "../components/AppShell";
import { EmptyState, ErrorNote, Skeleton, StatusBadge, fmt, nextActionLabel } from "../components/ui";

const QUEUE_HINT = {
  LAB_TECH: "Specimens waiting for receive, testing, results, or report.",
  LAB_ADMIN: "Open work for the bench. Cancel/reject stays on the specimen page.",
};

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [queue, setQueue] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [role, setRole] = useState("LAB_TECH");

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    Promise.all([
      api("/api/stats"),
      api("/api/specimens", { params: { needsAction: true, size: 50 } }),
    ])
      .then(([s, page]) => {
        setStats(s);
        setQueue(page.content || []);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    setRole(getUser()?.role || "LAB_TECH");
    load();
  }, [load]);

  const byBucket = (queue || []).reduce((acc, s) => {
    acc[s.status] = (acc[s.status] || 0) + 1;
    return acc;
  }, {});

  return (
    <AppShell
      title="Work queue"
      actions={
        <>
          <button className="btn secondary" type="button" onClick={load} disabled={loading}>Refresh</button>
          <Link className="btn" href="/orders/new">New order</Link>
        </>
      }
    >
      <ErrorNote error={error} />
      <p className="muted queueIntro">{QUEUE_HINT[role] || QUEUE_HINT.LAB_TECH} Press <kbd>/</kbd> to focus barcode scan.</p>

      {loading ? (
        <section className="card"><Skeleton rows={8} /></section>
      ) : (
        <>
          {stats ? (
            <section className="card">
              <h2>Bench snapshot</h2>
              <div className="stats">
                {["COLLECTED", "RECEIVED", "IN_TESTING", "RESULTED"].map((status) => (
                  <Link key={status} className="stat" href={`/specimens?status=${status}`}>
                    <div className="n">{stats.bySpecimenStatus?.[status] ?? 0}</div>
                    <StatusBadge status={status} />
                  </Link>
                ))}
                <div className="stat">
                  <div className="n">{queue?.length ?? 0}</div>
                  <span className="muted">In queue</span>
                </div>
              </div>
              {Object.keys(byBucket).length > 0 ? (
                <p className="muted" style={{ marginBottom: 0, marginTop: "0.85rem" }}>
                  Showing {queue.length} actionable specimen{queue.length === 1 ? "" : "s"}
                  {Object.entries(byBucket).map(([st, n]) => ` · ${n} ${st.replaceAll("_", " ")}`).join("")}
                </p>
              ) : null}
            </section>
          ) : null}

          <section className="card">
            <h2>Needs action</h2>
            {!queue || queue.length === 0 ? (
              <EmptyState
                title="Queue is clear"
                detail="Create an order and accession a specimen, or open Specimens to browse everything."
                href="/orders/new"
                actionLabel="Create order"
              />
            ) : (
              <div className="tableWrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Barcode</th>
                      <th>Patient</th>
                      <th>Order</th>
                      <th>Status</th>
                      <th>Received</th>
                      <th>Next</th>
                    </tr>
                  </thead>
                  <tbody>
                    {queue.map((s) => (
                      <tr key={s.id} className="clickable" onClick={() => { window.location.href = `/specimens/${s.id}`; }}>
                        <td className="mono">
                          <Link href={`/specimens/${s.id}`} onClick={(e) => e.stopPropagation()}>{s.barcode}</Link>
                        </td>
                        <td>{s.patientName} <span className="muted">({s.mrn})</span></td>
                        <td>
                          <Link href={`/orders/${s.orderId}`} onClick={(e) => e.stopPropagation()}>{s.orderNumber}</Link>
                        </td>
                        <td><StatusBadge status={s.status} /></td>
                        <td>{fmt(s.receivedAt)}</td>
                        <td>
                          <Link className="btn small" href={`/specimens/${s.id}`} onClick={(e) => e.stopPropagation()}>
                            {nextActionLabel(s.status)}
                          </Link>
                        </td>
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
