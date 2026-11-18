"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { deleteOrder, listOrders } from "@store/order/orderActions";
import { toast } from "@utils/toast";
import AppShell from "@component/layout/AppShell";
import { EmptyState, ErrorNote, Skeleton, StatusBadge, fmt } from "@component/common/ui";

export default function OrdersPage() {
  const router = useRouter();
  const [q, setQ] = useState("");
  const [orders, setOrders] = useState(null);
  const [error, setError] = useState("");
  const [searched, setSearched] = useState(false);
  const [busyId, setBusyId] = useState(null);

  function load(query) {
    setError("");
    setOrders(null);
    listOrders(query)
      .then((rows) => {
        setOrders(rows);
        setSearched(Boolean(query && query.trim()));
      })
      .catch((e) => setError(e.message));
  }

  useEffect(() => {
    load("");
  }, []);

  async function onDelete(o, e) {
    e.stopPropagation();
    if (o.specimenCount > 0) {
      setError("Cannot delete an order that has specimens. Open it and cancel/reject specimens instead.");
      return;
    }
    if (!window.confirm(`Delete order ${o.orderNumber}? This cannot be undone.`)) return;
    setBusyId(o.id);
    setError("");
    try {
      await deleteOrder(o.id);
      toast(`Deleted ${o.orderNumber}`);
      load(q);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  }

  return (
    <AppShell title="Orders" actions={<Link className="btn" href={"/orders/new"}>New order</Link>}>
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
              href={"/orders/new"}
              actionLabel="Create first order"
            />
          )
        ) : (
          <div className="tableWrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Order</th>
                  <th>Patient</th>
                  <th>Client</th>
                  <th>Tests</th>
                  <th>Specimens</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr
                    key={o.id}
                    className="clickable"
                    onClick={() => router.push(`/orders/${o.id}`)}
                  >
                    <td className="mono">
                      <Link href={`/orders/${o.id}`} onClick={(e) => e.stopPropagation()}>{o.orderNumber}</Link>
                    </td>
                    <td>{o.patientName} <span className="muted">({o.mrn})</span></td>
                    <td>{o.clientName}</td>
                    <td>{o.testCodes.join(", ")}</td>
                    <td>{o.specimenCount}</td>
                    <td><StatusBadge status={o.status} /></td>
                    <td>{fmt(o.createdAt)}</td>
                    <td>
                      <div className="rowActions" onClick={(e) => e.stopPropagation()}>
                        <Link className="btn secondary small" href={`/orders/${o.id}`}>View</Link>
                        <Link className="btn secondary small" href={`/orders/${o.id}/history`}>History</Link>
                        <button
                          className="btn danger small"
                          type="button"
                          disabled={busyId === o.id || o.specimenCount > 0}
                          title={o.specimenCount > 0 ? "Orders with specimens cannot be deleted" : "Delete order"}
                          onClick={(e) => onDelete(o, e)}
                        >
                          {busyId === o.id ? "…" : "Delete"}
                        </button>
                      </div>
                    </td>
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
