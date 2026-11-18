"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { createOrder } from "@store/order/orderActions";
import { listClients, listTests } from "@store/reference/referenceActions";
import AppShell from "@component/layout/AppShell";
import { EmptyState, ErrorNote, Skeleton } from "@component/common/ui";

export default function NewOrderPage() {
  const router = useRouter();
  const [clients, setClients] = useState([]);
  const [tests, setTests] = useState([]);
  const [form, setForm] = useState({
    clientId: "",
    mrn: "",
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    sex: "",
  });
  const [selected, setSelected] = useState([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([listClients(), listTests()])
      .then(([c, t]) => {
        setClients(c);
        setTests(t);
        if (c.length) setForm((f) => ({ ...f, clientId: String(c[0].id) }));
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  function toggle(id) {
    setSelected((s) => (s.includes(id) ? s.filter((x) => x !== id) : [...s, id]));
  }

  async function submit(e) {
    e.preventDefault();
    setError("");
    if (selected.length === 0) {
      setError("Select at least one test");
      return;
    }
    setSaving(true);
    try {
      const order = await createOrder({
        clientId: Number(form.clientId),
        testIds: selected,
        patient: {
          mrn: form.mrn,
          firstName: form.firstName,
          lastName: form.lastName,
          dateOfBirth: form.dateOfBirth,
          sex: form.sex || null,
        },
      });
      router.push(`/orders/${order.id}`);
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  }

  return (
    <AppShell title="New order">
      <ErrorNote error={error} />
      {loading ? (
        <section className="card"><Skeleton rows={8} /></section>
      ) : clients.length === 0 || tests.length === 0 ? (
        <section className="card">
          <EmptyState
            title="Catalog not ready"
            detail="Need at least one client and one test before creating an order. Ask a lab admin to seed Admin."
            href="/admin"
            actionLabel="Open admin"
          />
        </section>
      ) : (
      <form className="form" onSubmit={submit}>
        <section className="card">
          <h2>Client</h2>
          <label className="field">
            Ordering site
            <select value={form.clientId} onChange={set("clientId")} required>
              {clients.map((c) => <option key={c.id} value={c.id}>{c.name} ({c.code})</option>)}
            </select>
          </label>
        </section>

        <section className="card">
          <h2>Patient <span className="muted">(fictional data only)</span></h2>
          <div className="row">
            <label className="field">MRN<input value={form.mrn} onChange={set("mrn")} required /></label>
            <label className="field">First name<input value={form.firstName} onChange={set("firstName")} required /></label>
            <label className="field">Last name<input value={form.lastName} onChange={set("lastName")} required /></label>
            <label className="field">
              Date of birth
              <input type="date" value={form.dateOfBirth} onChange={set("dateOfBirth")} required />
            </label>
            <label className="field">
              Sex
              <select value={form.sex} onChange={set("sex")}>
                <option value="">—</option>
                <option value="F">F</option>
                <option value="M">M</option>
                <option value="X">X</option>
              </select>
            </label>
          </div>
          <p className="muted">An existing MRN reuses the stored patient record.</p>
        </section>

        <section className="card">
          <h2>Tests</h2>
          <div className="checks">
            {tests.map((t) => (
              <label key={t.id} className="check">
                <input type="checkbox" checked={selected.includes(t.id)} onChange={() => toggle(t.id)} />
                <span><strong>{t.code}</strong> — {t.name} <span className="muted">[{t.tubeType}]</span></span>
              </label>
            ))}
          </div>
        </section>

        <div className="actions">
          <button className="btn" type="submit" disabled={saving}>{saving ? "Saving…" : "Create order"}</button>
          <button className="btn secondary" type="button" onClick={() => router.push("/orders")}>Cancel</button>
        </div>
      </form>
      )}
    </AppShell>
  );
}
