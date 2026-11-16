"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../lib/api";
import AppShell from "../components/AppShell";
import { EmptyState, ErrorNote, Skeleton, StatusBadge, fmt } from "../components/ui";

const STATUSES = ["COLLECTED", "RECEIVED", "IN_TESTING", "RESULTED", "REPORTED", "CANCELLED", "REJECTED"];

export default function SpecimensPage() {
  const [filters, setFilters] = useState({ barcode: "", patient: "", status: "" });
  const [rows, setRows] = useState(null);
  const [error, setError] = useState("");
  const [hasFilters, setHasFilters] = useState(false);
  const barcodeRef = useRef(null);

  const search = useCallback((f) => {
    setError("");
    setRows(null);
    setHasFilters(Boolean(f.barcode?.trim() || f.patient?.trim() || f.status));
    api("/api/specimens", { params: f })
      .then(setRows)
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    const status = new URLSearchParams(window.location.search).get("status") || "";
    const initial = { barcode: "", patient: "", status };
    setFilters(initial);
    search(initial);
    barcodeRef.current?.focus();
  }, [search]);

  const set = (key) => (e) => setFilters({ ...filters, [key]: e.target.value });

  return (
    <AppShell title="Specimens">
      <form
        className="card row"
        onSubmit={(e) => {
          e.preventDefault();
          search(filters);
        }}
      >
        <label className="field">
          Barcode
          <input
            ref={barcodeRef}
            value={filters.barcode}
            onChange={set("barcode")}
            placeholder="SPC…"
            autoComplete="off"
          />
        </label>
        <label className="field">Patient MRN / name<input value={filters.patient} onChange={set("patient")} /></label>
        <label className="field">
          Status
          <select value={filters.status} onChange={set("status")}>
            <option value="">Any</option>
            {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
        <div className="actions">
          <button className="btn" type="submit">Search</button>
          <button
            className="btn secondary"
            type="button"
            onClick={() => {
              const empty = { barcode: "", patient: "", status: "" };
              setFilters(empty);
              search(empty);
              barcodeRef.current?.focus();
            }}
          >
            Clear
          </button>
        </div>
      </form>

      <ErrorNote error={error} />
      <section className="card">
        {rows === null ? (
          <Skeleton rows={6} />
        ) : rows.length === 0 ? (
          hasFilters ? (
            <EmptyState
              title="No specimens match"
              detail="Try another barcode, MRN, or clear the filters."
            />
          ) : (
            <EmptyState
              title="No specimens yet"
              detail="Accession a specimen from an order to generate a barcode."
              href="/orders"
              actionLabel="Go to orders"
            />
          )
        ) : (
          <div className="tableWrap">
            <table className="table">
              <thead><tr><th>Barcode</th><th>Patient</th><th>Order</th><th>Tube</th><th>Status</th><th>Received</th></tr></thead>
              <tbody>
                {rows.map((s) => (
                  <tr key={s.id}>
                    <td className="mono"><Link href={`/specimens/${s.id}`}>{s.barcode}</Link></td>
                    <td>{s.patientName} <span className="muted">({s.mrn})</span></td>
                    <td><Link href={`/orders/${s.orderId}`}>{s.orderNumber}</Link></td>
                    <td>{s.tubeType}</td>
                    <td><StatusBadge status={s.status} /></td>
                    <td>{fmt(s.receivedAt)}</td>
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
