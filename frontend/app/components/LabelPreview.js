"use client";

import { useEffect } from "react";

/** Simple bar pattern for a print-style label (not a standards barcode). */
function barsFor(text) {
  const bits = [];
  for (let i = 0; i < text.length; i++) {
    const code = text.charCodeAt(i);
    bits.push((code & 1) === 0 ? 2 : 1);
    bits.push((code & 2) === 0 ? 1 : 2);
    bits.push((code & 4) === 0 ? 2 : 1);
    bits.push(1);
  }
  return bits;
}

/**
 * Accession label preview + browser print.
 * Optional 4.5 — educational print layout, not a certified barcode printer driver.
 */
export default function LabelPreview({ open, onClose, specimen }) {
  useEffect(() => {
    if (!open) return undefined;
    function onKey(e) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open || !specimen) return null;

  const widths = barsFor(specimen.barcode || "");
  let x = 0;

  function printLabel() {
    window.print();
  }

  return (
    <div className="labelOverlay" role="dialog" aria-modal="true" aria-labelledby="label-title">
      <div className="labelDialog no-print">
        <div className="pageHead" style={{ marginBottom: "1rem", border: "none", paddingBottom: 0 }}>
          <div>
            <p className="pageEyebrow">Accession label</p>
            <h2 id="label-title" style={{ margin: 0, fontSize: "1.25rem" }}>Print preview</h2>
          </div>
          <div className="actions">
            <button className="btn" type="button" onClick={printLabel}>Print</button>
            <button className="btn secondary" type="button" onClick={onClose}>Close</button>
          </div>
        </div>
        <p className="muted" style={{ marginTop: 0 }}>
          Uses the browser print dialog. Bar pattern is a visual stand-in for demos — not Code 128 / Code 39.
        </p>
      </div>

      <div className="labelSheet">
        <div className="labelCard">
          <div className="labelBrand">Specimen Management</div>
          <svg className="labelBars" viewBox={`0 0 ${Math.max(widths.reduce((a, b) => a + b, 0), 40)} 40`} aria-hidden="true">
            {widths.map((w, i) => {
              const rect = (
                <rect key={i} x={x} y="0" width={w} height="40" fill={i % 2 === 0 ? "#13202c" : "transparent"} />
              );
              x += w;
              return rect;
            })}
          </svg>
          <div className="labelBarcode mono">{specimen.barcode}</div>
          <div className="labelMeta">
            <div><strong>{specimen.patientName}</strong></div>
            <div>MRN {specimen.mrn}</div>
            <div>Order {specimen.orderNumber}</div>
            <div>{specimen.tubeType}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
