"use client";

import { useState } from "react";
import { toast } from "@utils/toast";
import { ingestInstrument } from "@store/reference/referenceActions";
import AppShell from "@component/layout/AppShell";
import { ErrorNote } from "@component/common/ui";

const SAMPLE = {
  instrumentId: "SIM-ANALYZER-1",
  barcode: "PASTE-SPECIMEN-BARCODE",
  results: [
    { analyte: "WBC", value: "11.9", unit: "10^3/uL", flag: "H" },
    { analyte: "HGB", value: "13.8", unit: "g/dL", flag: "N" },
    { analyte: "PLT", value: "250", unit: "10^3/uL", flag: "N" },
  ],
};

export default function InstrumentPage() {
  const [text, setText] = useState(JSON.stringify(SAMPLE, null, 2));
  const [out, setOut] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function send(e) {
    e.preventDefault();
    setError("");
    setOut("");
    let body;
    try {
      body = JSON.parse(text);
    } catch {
      setError("Message is not valid JSON");
      return;
    }
    setBusy(true);
    try {
      const res = await ingestInstrument(body);
      setOut(JSON.stringify(res, null, 2));
      toast(`Posted ${res.resultsPosted} result(s) to ${res.barcode}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AppShell title="Instrument interface (simulated analyzer)">
      <section className="card">
        <p className="muted">
          Posts an analyzer message to <code>POST /api/instruments/ingest</code>. The specimen is matched by barcode,
          results are attached, and the status moves to RESULTED. The specimen must be RECEIVED or IN_TESTING.
          Flags: <code>H</code>, <code>L</code>, <code>HH</code>, <code>LL</code>, <code>A</code> mark a result abnormal; <code>N</code> does not.
        </p>
        <form className="form" onSubmit={send}>
          <textarea className="json" value={text} onChange={(e) => setText(e.target.value)} spellCheck={false} />
          <ErrorNote error={error} />
          <div className="actions">
            <button className="btn" type="submit" disabled={busy}>{busy ? "Sending…" : "Send analyzer message"}</button>
          </div>
        </form>
        {out ? <pre className="out">{out}</pre> : null}
      </section>
    </AppShell>
  );
}
