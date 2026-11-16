"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { subscribeToast } from "../../lib/toast";

export function StatusBadge({ status }) {
  return <span className={`badge st-${status}`}>{status.replaceAll("_", " ")}</span>;
}

export function ErrorNote({ error }) {
  if (!error) return null;
  return <p className="error" role="alert">{error}</p>;
}

export function fmt(ts) {
  if (!ts) return "—";
  return new Date(ts).toLocaleString();
}

export const FLOW = ["COLLECTED", "RECEIVED", "IN_TESTING", "RESULTED", "REPORTED"];

const STEP_TONE = {
  COLLECTED: "tone-collected",
  RECEIVED: "tone-received",
  IN_TESTING: "tone-testing",
  RESULTED: "tone-resulted",
  REPORTED: "tone-reported",
};

/** Horizontal lifecycle progress for a specimen. */
export function StatusSteps({ status }) {
  const terminalBad = status === "CANCELLED" || status === "REJECTED";
  const idx = FLOW.indexOf(status);
  return (
    <div className="steps">
      {FLOW.map((s, i) => {
        const done = !terminalBad && i < idx;
        const current = !terminalBad && i === idx;
        return (
          <div
            key={s}
            className={`step ${STEP_TONE[s]} ${done ? "done" : ""} ${current ? "current" : ""}`}
          >
            <span className="dot" />
            <span>{s.replaceAll("_", " ")}</span>
          </div>
        );
      })}
      {terminalBad ? (
        <div className={`step bad ${status === "REJECTED" ? "tone-rejected" : "tone-cancelled"}`}>
          <span className="dot" />
          <span>{status}</span>
        </div>
      ) : null}
    </div>
  );
}

export function EmptyState({ title, detail, href, actionLabel }) {
  return (
    <div className="empty">
      <p className="emptyTitle">{title}</p>
      {detail ? <p className="muted">{detail}</p> : null}
      {href && actionLabel ? (
        <Link className="btn" href={href}>{actionLabel}</Link>
      ) : null}
    </div>
  );
}

export function Skeleton({ rows = 5 }) {
  return (
    <div className="skeleton" aria-busy="true" aria-label="Loading">
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="skeletonRow" style={{ width: `${92 - (i % 4) * 10}%` }} />
      ))}
    </div>
  );
}

export function ToastHost() {
  const [msg, setMsg] = useState(null);

  useEffect(() => {
    let timer;
    return subscribeToast((message) => {
      clearTimeout(timer);
      setMsg(message);
      timer = setTimeout(() => setMsg(null), 2800);
    });
  }, []);

  if (!msg) return null;
  return (
    <div className="toast" role="status">
      {msg}
    </div>
  );
}

export function BarcodeBlock({ barcode }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    try {
      await navigator.clipboard.writeText(barcode);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* ignore */
    }
  }

  return (
    <div className="barcodeBlock">
      <div>
        <div className="barcodeLabel">Barcode</div>
        <div className="barcodeValue mono">{barcode}</div>
      </div>
      <button className="btn secondary small" type="button" onClick={copy}>
        {copied ? "Copied" : "Copy"}
      </button>
    </div>
  );
}
