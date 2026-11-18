"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { toast } from "@utils/toast";
import {
  addSpecimenResults,
  changeSpecimenStatus,
  loadSpecimenPage,
} from "@store/specimen/specimenActions";
import AppShell from "@component/layout/AppShell";
import LabelPreview from "@component/common/LabelPreview";
import { BarcodeBlock, EmptyState, ErrorNote, Skeleton, StatusBadge, StatusSteps, fmt } from "@component/common/ui";

const emptyRow = () => ({ analyte: "", value: "", unit: "", flagged: false });

export default function SpecimenDetailPage() {
  const { id } = useParams();
  const [specimen, setSpecimen] = useState(null);
  const [timeline, setTimeline] = useState([]);
  const [error, setError] = useState("");
  const [reason, setReason] = useState("");
  const [rows, setRows] = useState([emptyRow()]);
  const [busy, setBusy] = useState(false);
  const [printOpen, setPrintOpen] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);

  const load = useCallback(() => {
    setLoadFailed(false);
    return loadSpecimenPage(id)
      .then(({ specimen: s, timeline: t }) => {
        setSpecimen(s);
        setTimeline(t);
      })
      .catch((e) => {
        setError(e.message);
        setLoadFailed(true);
      });
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  async function run(fn, successMessage) {
    setError("");
    setBusy(true);
    try {
      await fn();
      await load();
      if (successMessage) toast(successMessage);
    } catch (err) {
      if (err.status === 409) {
        setError(err.message || "Someone else updated this specimen. Refreshing…");
        await load();
      } else {
        setError(err.message);
      }
    } finally {
      setBusy(false);
    }
  }

  function changeStatus(status) {
    const negative = status === "CANCELLED" || status === "REJECTED";
    if (negative && !reason.trim()) {
      setError(`Enter a reason before setting ${status}`);
      return;
    }
    run(async () => {
      await changeSpecimenStatus(id, {
        status,
        version: specimen.version,
        reason: negative ? reason : undefined,
      });
      setReason("");
    }, `Status updated to ${status.replaceAll("_", " ")}`);
  }

  function submitResults(e) {
    e.preventDefault();
    const results = rows
      .filter((r) => r.analyte.trim() || r.value.trim())
      .map((r) => ({ analyte: r.analyte, value: r.value, unit: r.unit || null, flagged: r.flagged }));
    if (results.length === 0) {
      setError("Enter at least one result");
      return;
    }
    run(async () => {
      await addSpecimenResults(id, { version: specimen.version, results });
      setRows([emptyRow()]);
    }, "Results saved — specimen is RESULTED");
  }

  function updateRow(i, key, value) {
    setRows(rows.map((r, idx) => (idx === i ? { ...r, [key]: value } : r)));
  }

  const canEnterResults = specimen && (specimen.status === "RECEIVED" || specimen.status === "IN_TESTING");
  const forward = specimen ? specimen.allowedNext.filter((s) => s !== "CANCELLED" && s !== "REJECTED" && s !== "RESULTED") : [];
  const negative = specimen ? specimen.allowedNext.filter((s) => s === "CANCELLED" || s === "REJECTED") : [];

  return (
    <AppShell
      title={specimen ? "Specimen" : "Specimen"}
      actions={specimen ? <Link className="btn secondary" href={`/orders/${specimen.orderId}`}>Order {specimen.orderNumber}</Link> : null}
    >
      <ErrorNote error={error} />
      {!specimen && !loadFailed ? (
        <section className="card"><Skeleton rows={10} /></section>
      ) : loadFailed && !specimen ? (
        <section className="card">
          <EmptyState
            title="Specimen not found"
            detail={error || "It may have been removed, or the link is wrong."}
            href="/dashboard"
            actionLabel="Back to work queue"
          />
        </section>
      ) : (
        <>
          <LabelPreview open={printOpen} onClose={() => setPrintOpen(false)} specimen={specimen} />
          <section className="card">
            <BarcodeBlock barcode={specimen.barcode} onPrint={() => setPrintOpen(true)} />
            <h2>Status</h2>
            <StatusSteps status={specimen.status} />
            <div className="row" style={{ marginTop: "1rem" }}>
              <dl className="dl">
                <dt>Patient</dt><dd>{specimen.patientName} ({specimen.mrn})</dd>
                <dt>Tube</dt><dd>{specimen.tubeType}</dd>
                <dt>Collected</dt><dd>{fmt(specimen.collectedAt)}</dd>
                <dt>Received</dt><dd>{fmt(specimen.receivedAt)}</dd>
                <dt>Current</dt><dd><StatusBadge status={specimen.status} /></dd>
                {specimen.statusReason ? (<><dt>Reason</dt><dd>{specimen.statusReason}</dd></>) : null}
              </dl>
            </div>
            {specimen.allowedNext.length > 0 ? (
              <div style={{ marginTop: "1rem" }}>
                <div className="actions">
                  {forward.map((s) => (
                    <button key={s} className="btn" type="button" disabled={busy} onClick={() => changeStatus(s)}>
                      Move to {s.replaceAll("_", " ")}
                    </button>
                  ))}
                </div>
                {negative.length > 0 ? (
                  <div className="row" style={{ marginTop: "0.75rem" }}>
                    <label className="field">
                      Reason (required to cancel or reject)
                      <input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="e.g. hemolyzed sample" />
                    </label>
                    <div className="actions">
                      {negative.map((s) => (
                        <button key={s} className="btn danger" type="button" disabled={busy} onClick={() => changeStatus(s)}>
                          {s === "CANCELLED" ? "Cancel" : "Reject"}
                        </button>
                      ))}
                    </div>
                  </div>
                ) : null}
              </div>
            ) : (
              <p className="muted">This specimen is in a final state.</p>
            )}
          </section>

          <section className="card">
            <h2>Results</h2>
            {specimen.results.length === 0 ? (
              <p className="muted">No results yet.</p>
            ) : (
              <div className="tableWrap">
                <table className="table">
                  <thead><tr><th>Analyte</th><th>Value</th><th>Unit</th><th>Flag</th><th>Resulted</th><th>By</th></tr></thead>
                  <tbody>
                    {specimen.results.map((r) => (
                      <tr key={r.id}>
                        <td>{r.analyte}</td>
                        <td className={r.flagged ? "flagged" : ""}>{r.value}</td>
                        <td>{r.unit || "—"}</td>
                        <td className={r.flagged ? "flagged" : ""}>{r.flagged ? "Abnormal" : "—"}</td>
                        <td>{fmt(r.resultedAt)}</td>
                        <td>{r.enteredBy}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {canEnterResults ? (
              <form className="form" onSubmit={submitResults} style={{ marginTop: "1rem" }}>
                <h2>Enter results</h2>
                {rows.map((r, i) => (
                  <div className="row" key={i}>
                    <label className="field">Analyte<input value={r.analyte} onChange={(e) => updateRow(i, "analyte", e.target.value)} placeholder="WBC" /></label>
                    <label className="field">Value<input value={r.value} onChange={(e) => updateRow(i, "value", e.target.value)} placeholder="7.2" /></label>
                    <label className="field">Unit<input value={r.unit} onChange={(e) => updateRow(i, "unit", e.target.value)} placeholder="10^3/uL" /></label>
                    <label className="check">
                      <input type="checkbox" checked={r.flagged} onChange={(e) => updateRow(i, "flagged", e.target.checked)} />
                      Abnormal
                    </label>
                  </div>
                ))}
                <div className="actions">
                  <button className="btn secondary" type="button" onClick={() => setRows([...rows, emptyRow()])}>Add row</button>
                  <button className="btn" type="submit" disabled={busy}>Save results (marks RESULTED)</button>
                </div>
              </form>
            ) : null}
          </section>

          <section className="card">
            <h2>Timeline</h2>
            <ul className="timeline">
              {timeline.map((t) => (
                <li key={t.id}>
                  <strong>{t.action.replaceAll("_", " ")}</strong> — {t.details}
                  <div className="when">{fmt(t.performedAt)} · {t.performedBy}</div>
                </li>
              ))}
            </ul>
          </section>
        </>
      )}
    </AppShell>
  );
}
