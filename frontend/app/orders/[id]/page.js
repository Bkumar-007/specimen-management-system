"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { accessionSpecimen, deleteOrder, getOrder } from "@store/order/orderActions";
import { toast } from "@utils/toast";
import AppShell from "@component/layout/AppShell";
import LabelPreview from "@component/common/LabelPreview";
import { EmptyState, ErrorNote, Skeleton, StatusBadge, fmt } from "@component/common/ui";

export default function OrderDetailPage() {
  const { id } = useParams();
  const router = useRouter();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");
  const [tubeType, setTubeType] = useState("");
  const [markReceived, setMarkReceived] = useState(true);
  const [busy, setBusy] = useState(false);
  const [labelSpecimen, setLabelSpecimen] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false);

  function load() {
    setLoadFailed(false);
    return getOrder(id)
      .then((o) => {
        setOrder(o);
        setTubeType((t) => t || o.tests[0]?.tubeType || "");
      })
      .catch((e) => {
        setError(e.message);
        setLoadFailed(true);
      });
  }

  useEffect(() => {
    let cancelled = false;
    setLoadFailed(false);
    setError("");
    setOrder(null);

    getOrder(id)
      .then((o) => {
        if (cancelled) return;
        setOrder(o);
        setTubeType((t) => t || o.tests[0]?.tubeType || "");
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

  async function onAccession(e) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const specimen = await accessionSpecimen(id, { tubeType, markReceived });
      toast(`Accessioned ${specimen.barcode}`);
      setLabelSpecimen(specimen);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function onDelete() {
    if (!order) return;
    if (order.specimens.length > 0) {
      setError("Cannot delete an order that has specimens. Cancel or reject the specimens instead.");
      return;
    }
    if (!window.confirm(`Delete order ${order.orderNumber}? This cannot be undone.`)) return;
    setBusy(true);
    setError("");
    try {
      await deleteOrder(id);
      toast(`Deleted ${order.orderNumber}`);
      router.push("/orders");
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  }

  const tubeOptions = order ? [...new Set(order.tests.map((t) => t.tubeType))] : [];
  const canDelete = order && order.specimens.length === 0;

  return (
    <AppShell
      title={order ? `Order ${order.orderNumber}` : "Order"}
      actions={
        <>
          <Link className="btn secondary" href={`/orders/${id}/history`}>History</Link>
          {canDelete ? (
            <button className="btn danger" type="button" disabled={busy} onClick={onDelete}>Delete</button>
          ) : null}
          <Link className="btn secondary" href={"/orders"}>All orders</Link>
        </>
      }
    >
      <ErrorNote error={error} />
      <LabelPreview open={Boolean(labelSpecimen)} onClose={() => setLabelSpecimen(null)} specimen={labelSpecimen} />
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
          <div className="grid2">
            <section className="card">
              <h2>Order</h2>
              <dl className="dl">
                <dt>Status</dt><dd><StatusBadge status={order.status} /></dd>
                <dt>Client</dt><dd>{order.client.name} ({order.client.code})</dd>
                <dt>Created</dt><dd>{fmt(order.createdAt)} by {order.createdBy}</dd>
                <dt>Tests</dt><dd>{order.tests.map((t) => t.code).join(", ")}</dd>
              </dl>
            </section>
            <section className="card">
              <h2>Patient</h2>
              <dl className="dl">
                <dt>Name</dt><dd>{order.patient.lastName}, {order.patient.firstName}</dd>
                <dt>MRN</dt><dd>{order.patient.mrn}</dd>
                <dt>DOB</dt><dd>{order.patient.dateOfBirth}</dd>
                <dt>Sex</dt><dd>{order.patient.sex || "—"}</dd>
              </dl>
            </section>
          </div>

          <section className="card">
            <h2>Accession specimen</h2>
            <form className="row" onSubmit={onAccession}>
              <label className="field">
                Tube type
                <input list="tubes" value={tubeType} onChange={(e) => setTubeType(e.target.value)} required />
                <datalist id="tubes">{tubeOptions.map((t) => <option key={t} value={t} />)}</datalist>
              </label>
              <label className="check">
                <input type="checkbox" checked={markReceived} onChange={(e) => setMarkReceived(e.target.checked)} />
                Mark received now
              </label>
              <div><button className="btn" type="submit" disabled={busy}>{busy ? "Accessioning…" : "Accession"}</button></div>
            </form>
            <p className="muted">A barcode is generated automatically.</p>
          </section>

          <section className="card">
            <h2>Specimens</h2>
            {order.specimens.length === 0 ? (
              <EmptyState
                title="No specimens accessioned yet"
                detail="Use the form above to receive the sample and generate a barcode."
              />
            ) : (
              <div className="tableWrap">
                <table className="table">
                  <thead><tr><th>Barcode</th><th>Tube</th><th>Collected</th><th>Received</th><th>Status</th><th>Results</th></tr></thead>
                  <tbody>
                    {order.specimens.map((s) => (
                      <tr key={s.id}>
                        <td className="mono"><Link href={`/specimens/${s.id}`}>{s.barcode}</Link></td>
                        <td>{s.tubeType}</td>
                        <td>{fmt(s.collectedAt)}</td>
                        <td>{fmt(s.receivedAt)}</td>
                        <td><StatusBadge status={s.status} /></td>
                        <td>{s.results.length}</td>
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
