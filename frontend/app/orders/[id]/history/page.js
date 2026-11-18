"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { loadOrderHistoryPage } from "@store/order/orderActions";
import AppShell from "@component/layout/AppShell";
import { EmptyState, ErrorNote, Skeleton, StatusBadge, StatusSteps, fmt } from "@component/common/ui";

export default function OrderHistoryPage() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [timeline, setTimeline] = useState(null);
  const [error, setError] = useState("");
  const [loadFailed, setLoadFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoadFailed(false);
    setError("");
    setOrder(null);
    setTimeline(null);

    loadOrderHistoryPage(id)
      .then(({ order: o, timeline: t }) => {
        if (cancelled) return;
        setOrder(o);
        setTimeline(t);
      })
      .catch((e) => {
        if (cancelled) return;
        setError(e.message);
        setLoadFailed(true);
      });

    return () => {
      cancelled = true;
    };
  }, [id]);

  return (
    <AppShell
      title={order ? `History · ${order.orderNumber}` : "Order history"}
      actions={
        <>
          <Link className="btn secondary" href={`/orders/${id}`}>View order</Link>
          <Link className="btn secondary" href={"/orders"}>All orders</Link>
        </>
      }
    >
      <ErrorNote error={error} />
      {!order && !loadFailed ? (
        <section className="card"><Skeleton rows={8} /></section>
      ) : loadFailed && !order ? (
        <section className="card">
          <EmptyState
            title="Order not found"
            detail={error || "Check the order id or return to the list."}
            href={"/orders"}
            actionLabel="All orders"
          />
        </section>
      ) : (
        <>
          <section className="card">
            <h2>Current status</h2>
            <StatusSteps status={order.status} />
            <div className="row" style={{ marginTop: "1rem" }}>
              <dl className="dl">
                <dt>Status</dt><dd><StatusBadge status={order.status} /></dd>
                <dt>Patient</dt><dd>{order.patient.lastName}, {order.patient.firstName} ({order.patient.mrn})</dd>
                <dt>Client</dt><dd>{order.client.name}</dd>
                <dt>Tests</dt><dd>{order.tests.map((t) => t.code).join(", ")}</dd>
                <dt>Created</dt><dd>{fmt(order.createdAt)} by {order.createdBy}</dd>
              </dl>
            </div>
            <p className="muted" style={{ marginBottom: 0 }}>
              Full lab lifecycle: ORDERED → COLLECTED → RECEIVED → IN_TESTING → RESULTED → REPORTED
              (plus CANCELLED / REJECTED). Events below include order actions and specimen status changes.
            </p>
          </section>

          <section className="card">
            <h2>Timeline</h2>
            {!timeline || timeline.length === 0 ? (
              <EmptyState title="No history yet" detail="Events appear as the order and its specimens change." />
            ) : (
              <ul className="timeline">
                {timeline.map((t) => (
                  <li key={`${t.entityType}-${t.id}`}>
                    <strong>{t.action.replaceAll("_", " ")}</strong>
                    {t.entityType === "SPECIMEN" ? (
                      <span className="muted"> · specimen</span>
                    ) : (
                      <span className="muted"> · order</span>
                    )}
                    {t.details ? <> — {t.details}</> : null}
                    <div className="when">{fmt(t.performedAt)} · {t.performedBy}</div>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {order.specimens.length > 0 ? (
            <section className="card">
              <h2>Specimens on this order</h2>
              <div className="tableWrap">
                <table className="table">
                  <thead>
                    <tr><th>Barcode</th><th>Status</th><th>Received</th></tr>
                  </thead>
                  <tbody>
                    {order.specimens.map((s) => (
                      <tr key={s.id}>
                        <td className="mono"><Link href={`/specimens/${s.id}`}>{s.barcode}</Link></td>
                        <td><StatusBadge status={s.status} /></td>
                        <td>{fmt(s.receivedAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}
        </>
      )}
    </AppShell>
  );
}
