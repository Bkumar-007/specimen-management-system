"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { api } from "../../../lib/api";
import { toast } from "../../../lib/toast";
import AppShell from "../../components/AppShell";
import { EmptyState, ErrorNote, Skeleton, StatusBadge, fmt } from "../../components/ui";

export default function OrderDetailPage() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");
  const [tubeType, setTubeType] = useState("");
  const [markReceived, setMarkReceived] = useState(true);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    return api(`/api/orders/${id}`)
      .then((o) => {
        setOrder(o);
        setTubeType((t) => t || o.tests[0]?.tubeType || "");
      })
      .catch((e) => setError(e.message));
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  async function accession(e) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const specimen = await api(`/api/orders/${id}/specimens`, {
        method: "POST",
        body: { tubeType, markReceived },
      });
      toast(`Accessioned ${specimen.barcode}`);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const tubeOptions = order ? [...new Set(order.tests.map((t) => t.tubeType))] : [];

  return (
    <AppShell title={order ? `Order ${order.orderNumber}` : "Order"} actions={<Link className="btn secondary" href="/orders">All orders</Link>}>
      <ErrorNote error={error} />
      {!order ? (
        <section className="card"><Skeleton rows={8} /></section>
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
            <form className="row" onSubmit={accession}>
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
