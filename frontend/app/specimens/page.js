"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../lib/api";
import AppShell from "../components/AppShell";
import { EmptyState, ErrorNote, Pagination, Skeleton, StatusBadge, fmt } from "../components/ui";

const STATUSES = ["COLLECTED", "RECEIVED", "IN_TESTING", "RESULTED", "REPORTED", "CANCELLED", "REJECTED"];
const PAGE_SIZE = 25;

const emptyFilters = () => ({ barcode: "", patient: "", status: "", from: "", to: "" });

export default function SpecimensPage() {
  const [filters, setFilters] = useState(emptyFilters());
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [hasFilters, setHasFilters] = useState(false);
  const barcodeRef = useRef(null);

  const search = useCallback((f, pageIndex = 0) => {
    setError("");
    setResult(null);
    const active = Boolean(f.barcode?.trim() || f.patient?.trim() || f.status || f.from || f.to);
    setHasFilters(active);
    api("/api/specimens", {
      params: {
        barcode: f.barcode,
        patient: f.patient,
        status: f.status || undefined,
        from: f.from || undefined,
        to: f.to || undefined,
        page: pageIndex,
        size: PAGE_SIZE,
      },
    })
      .then(setResult)
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const initial = {
      ...emptyFilters(),
      barcode: params.get("barcode") || "",
      patient: params.get("patient") || "",
      status: params.get("status") || "",
      from: params.get("from") || "",
      to: params.get("to") || "",
    };
    setFilters(initial);
    search(initial, 0);
    barcodeRef.current?.focus();
  }, [search]);

  const set = (key) => (e) => setFilters({ ...filters, [key]: e.target.value });

  return (
    <AppShell title="Specimens">
      <form
        className="card row"
        onSubmit={(e) => {
          e.preventDefault();
          search(filters, 0);
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
        <label className="field">
          Patient MRN / name
          <input value={filters.patient} onChange={set("patient")} />
        </label>
        <label className="field">
          Status
          <select value={filters.status} onChange={set("status")}>
            <option value="">Any</option>
            {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
        <label className="field">
          From
          <input type="date" value={filters.from} onChange={set("from")} />
        </label>
        <label className="field">
          To
          <input type="date" value={filters.to} onChange={set("to")} />
        </label>
        <div className="actions">
          <button className="btn" type="submit">Search</button>
          <button
            className="btn secondary"
            type="button"
            onClick={() => {
              const empty = emptyFilters();
              setFilters(empty);
              search(empty, 0);
              barcodeRef.current?.focus();
            }}
          >
            Clear
          </button>
        </div>
      </form>

      <ErrorNote error={error} />
      <section className="card">
        {result === null ? (
          <Skeleton rows={6} />
        ) : result.content.length === 0 ? (
          hasFilters ? (
            <EmptyState
              title="No specimens match"
              detail="Try another barcode, MRN, date range, or clear the filters."
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
          <>
            <div className="tableWrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Barcode</th>
                    <th>Patient</th>
                    <th>Order</th>
                    <th>Tube</th>
                    <th>Status</th>
                    <th>Received</th>
                  </tr>
                </thead>
                <tbody>
                  {result.content.map((s) => (
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
            <Pagination
              page={result.page}
              totalPages={result.totalPages}
              totalElements={result.totalElements}
              onPage={(p) => search(filters, p)}
            />
          </>
        )}
      </section>
    </AppShell>
  );
}
