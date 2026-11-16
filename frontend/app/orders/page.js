"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api } from "../../lib/api";
import AppShell from "../components/AppShell";
import { EmptyState, ErrorNote, Skeleton, StatusBadge, fmt } from "../components/ui";

export default function OrdersPage() {
  const [q, setQ] = useState("");
  const [orders, setOrders] = useState(null);
  const [error, setError] = useState("");
  const [searched, setSearched] = useState(false);

  function load(query) {
    setError("");
    setOrders(null);
    api("/api/orders", { params: { q: query } })
      .then((rows) => {
        setOrders(rows);
        setSearched(Boolean(query && query.trim()));
      })
      .catch((e) => setError(e.message));
  }

  useEffect(() => {
    load("");
  }, []);

  return (
    <AppShell title="Orders" actions={<Link className="btn" href="/orders/new">New order</Link>}>
      <form
        className="card row"
        onSubmit={(e) => {
          e.preventDefault();
          load(q);
        }}
      >
        <label className="field">
          Search by order number, MRN or patient name
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="e.g. MRN-1001 or Doe" />
        </label>
        <div className="actions">
          <button className="btn" type="submit">Search</button>
          <button className="btn secondary" type="button" onClick={() => { setQ(""); load(""); }}>Clear</button>
        </div>
      </form>

      <ErrorNote error={error} />
      <section className="card">
        {orders === null ? (
          <Skeleton rows={6} />
        ) : orders.length === 0 ? (
          searched ? (
            <EmptyState
              title="No matching orders"
              detail="Try another order number, MRN, or patient name."
            />
          ) : (
            <EmptyState
              title="No orders yet"
              detail="Start with a fictional patient and one or more catalog tests."
              href="/orders/new"
              actionLabel="Create first order"
            />
          )
        ) : (
          <div className="tableWrap">
            <table className="table">
              <thead>
                <tr><th>Order</th><th>Patient</th><th>Client</th><th>Tests</th><th>Specimens</th><th>Status</th><th>Created</th></tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.id}>
                    <td className="mono"><Link href={`/orders/${o.id}`}>{o.orderNumber}</Link></td>
                    <td>{o.patientName} <span className="muted">({o.mrn})</span></td>
                    <td>{o.clientName}</td>
                    <td>{o.testCodes.join(", ")}</td>
                    <td>{o.specimenCount}</td>
                    <td><StatusBadge status={o.status} /></td>
                    <td>{fmt(o.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </AppShell>
  );
}
