"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@utils/api";
import { routesMapping } from "@utils/apiRoutes";

/**
 * Global keyboard-first barcode lookup.
 * Focus with `/` (when not typing in another field). Enter jumps to the specimen.
 */
export default function BarcodeScan() {
  const router = useRouter();
  const inputRef = useRef(null);
  const [q, setQ] = useState("");
  const [busy, setBusy] = useState(false);
  const [hint, setHint] = useState("");

  useEffect(() => {
    function onKey(e) {
      if (e.key !== "/" || e.ctrlKey || e.metaKey || e.altKey) return;
      const tag = e.target?.tagName;
      if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || e.target?.isContentEditable) return;
      e.preventDefault();
      inputRef.current?.focus();
      inputRef.current?.select();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  async function submit(e) {
    e.preventDefault();
    const barcode = q.trim();
    if (!barcode) return;
    setBusy(true);
    setHint("");
    try {
      const page = await api(routesMapping.specimen.List, { params: { barcode, size: 10 } });
      const exact = (page.content || []).find((s) => s.barcode.toLowerCase() === barcode.toLowerCase());
      if (exact) {
        setQ("");
        setHint("");
        router.push(`/specimens/${exact.id}`);
        return;
      }
      if ((page.content || []).length === 1) {
        setQ("");
        router.push(`/specimens/${page.content[0].id}`);
        return;
      }
      if ((page.content || []).length > 1) {
        setHint("");
        router.push(`/specimens?barcode=${encodeURIComponent(barcode)}`);
        setQ("");
        return;
      }
      setHint("No specimen matches that barcode");
    } catch (err) {
      setHint(err.message || "Lookup failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="scanBox" onSubmit={submit} role="search">
      <label className="scanLabel" htmlFor="global-barcode-scan">
        Scan
      </label>
      <input
        id="global-barcode-scan"
        ref={inputRef}
        className="scanInput mono"
        value={q}
        onChange={(e) => {
          setQ(e.target.value);
          if (hint) setHint("");
        }}
        placeholder="Barcode (/ to focus)"
        autoComplete="off"
        spellCheck={false}
        disabled={busy}
        aria-describedby={hint ? "scan-hint" : undefined}
      />
      <button className="btn secondary small scanGo" type="submit" disabled={busy || !q.trim()}>
        {busy ? "…" : "Go"}
      </button>
      {hint ? (
        <span id="scan-hint" className="scanHint" role="status">
          {hint}
        </span>
      ) : null}
    </form>
  );
}
