"use client";

import { useCallback, useEffect, useState } from "react";
import { api, getUser } from "../../lib/api";
import AppShell from "../components/AppShell";
import { EmptyState, ErrorNote, Skeleton, fmt } from "../components/ui";

export default function AuditPage() {
  const [filters, setFilters] = useState({ entityType: "", entityId: "" });
  const [rows, setRows] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback((f) => {
    setError("");
    setRows(null);
    api("/api/audit", { params: f })
      .then(setRows)
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    if (getUser()?.role !== "LAB_ADMIN") {
      setError("Audit trail is available to lab admins only.");
      return;
    }
    load({ entityType: "", entityId: "" });
  }, [load]);

  const set = (key) => (e) => setFilters({ ...filters, [key]: e.target.value });

  return (
    <AppShell title="Audit trail">
      <form
        className="card row"
        onSubmit={(e) => {
          e.preventDefault();
          load(filters);
        }}
      >
        <label className="field">
          Entity type
          <select value={filters.entityType} onChange={set("entityType")}>
            <option value="">Any</option>
            {["ORDER", "SPECIMEN", "CLIENT", "TEST"].map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>
        <label className="field">Entity id<input value={filters.entityId} onChange={set("entityId")} inputMode="numeric" /></label>
        <div className="actions"><button className="btn" type="submit">Filter</button></div>
      </form>
      <ErrorNote error={error} />
      <section className="card">
        {rows === null ? (
          <Skeleton rows={6} />
        ) : rows.length === 0 ? (
          <EmptyState title="No audit entries" detail="Actions will appear here as orders and specimens change." />
        ) : (
          <div className="tableWrap">
            <table className="table">
              <thead><tr><th>When</th><th>Entity</th><th>Action</th><th>Details</th><th>By</th></tr></thead>
              <tbody>
                {rows.map((a) => (
                  <tr key={a.id}>
                    <td>{fmt(a.performedAt)}</td>
                    <td>{a.entityType} #{a.entityId}</td>
                    <td>{a.action}</td>
                    <td>{a.details}</td>
                    <td>{a.performedBy}</td>
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
