"use client";

import { useCallback, useEffect, useState } from "react";
import { api, getUser } from "../../lib/api";
import { toast } from "../../lib/toast";
import AppShell from "../components/AppShell";
import { EmptyState, ErrorNote, Skeleton } from "../components/ui";

export default function AdminPage() {
  const [clients, setClients] = useState([]);
  const [tests, setTests] = useState([]);
  const [client, setClient] = useState({ code: "", name: "" });
  const [test, setTest] = useState({ code: "", name: "", tubeType: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    Promise.all([api("/api/clients"), api("/api/tests")])
      .then(([c, t]) => {
        setClients(c);
        setTests(t);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (getUser()?.role !== "LAB_ADMIN") {
      setError("Admin tools are available to lab admins only.");
      setLoading(false);
      return;
    }
    load();
  }, [load]);

  async function add(path, body, reset, label) {
    setError("");
    try {
      await api(path, { method: "POST", body });
      reset();
      toast(`${label} saved`);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <AppShell title="Admin: clients and test catalog">
      <ErrorNote error={error} />
      {loading ? <section className="card"><Skeleton rows={6} /></section> : null}
      {!loading ? (
      <div className="grid2">
        <section className="card">
          <h2>Clients</h2>
          {clients.length === 0 ? (
            <EmptyState title="No clients" detail="Add a clinic or ordering site below." />
          ) : (
            <ul>{clients.map((c) => <li key={c.id}><strong>{c.code}</strong> — {c.name}</li>)}</ul>
          )}
          <form
            className="form"
            onSubmit={(e) => {
              e.preventDefault();
              add("/api/clients", client, () => setClient({ code: "", name: "" }), "Client");
            }}
          >
            <div className="row">
              <label className="field">Code<input value={client.code} onChange={(e) => setClient({ ...client, code: e.target.value })} required /></label>
              <label className="field">Name<input value={client.name} onChange={(e) => setClient({ ...client, name: e.target.value })} required /></label>
            </div>
            <div><button className="btn" type="submit">Add client</button></div>
          </form>
        </section>

        <section className="card">
          <h2>Test catalog</h2>
          {tests.length === 0 ? (
            <EmptyState title="No tests" detail="Add catalog entries so techs can order panels." />
          ) : (
            <ul>{tests.map((t) => <li key={t.id}><strong>{t.code}</strong> — {t.name} <span className="muted">[{t.tubeType}]</span></li>)}</ul>
          )}
          <form
            className="form"
            onSubmit={(e) => {
              e.preventDefault();
              add("/api/tests", test, () => setTest({ code: "", name: "", tubeType: "" }), "Test");
            }}
          >
            <div className="row">
              <label className="field">Code<input value={test.code} onChange={(e) => setTest({ ...test, code: e.target.value })} required /></label>
              <label className="field">Name<input value={test.name} onChange={(e) => setTest({ ...test, name: e.target.value })} required /></label>
              <label className="field">Tube type<input value={test.tubeType} onChange={(e) => setTest({ ...test, tubeType: e.target.value })} required /></label>
            </div>
            <div><button className="btn" type="submit">Add test</button></div>
          </form>
        </section>
      </div>
      ) : null}
    </AppShell>
  );
}
