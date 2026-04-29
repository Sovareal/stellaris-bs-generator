import type { ReactNode } from "react";

interface MonoRowProps {
  k: string;
  v: ReactNode;
  id?: string;
  costColor?: string;
  accent?: string;
  last?: boolean;
  reroll?: ReactNode;
}

export function MonoRow({ k, v, id, costColor, accent, last, reroll }: MonoRowProps) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "12px 86px 1fr auto 18px",
        alignItems: "center",
        gap: 6,
        padding: "5px 0",
        borderBottom: last ? "none" : "1px dashed #1c2740",
      }}
    >
      {/* Chevron */}
      <span
        style={{
          fontSize: 9,
          color: accent ?? "#3a4866",
          lineHeight: 1,
          userSelect: "none",
        }}
      >
        ›
      </span>

      {/* Label (k) */}
      <span
        style={{
          fontFamily: "JetBrains Mono, monospace",
          fontSize: 10,
          fontWeight: 500,
          color: "#7a8ba8",
          letterSpacing: 0.8,
          textTransform: "uppercase",
          overflow: "hidden",
          textOverflow: "ellipsis",
          whiteSpace: "nowrap",
        }}
      >
        {k}
      </span>

      {/* Value (v) */}
      <span
        style={{
          fontFamily: "Inter, system-ui, sans-serif",
          fontSize: 11,
          fontWeight: 500,
          color: "#e0e6ed",
          display: "flex",
          alignItems: "center",
          gap: 5,
          overflow: "hidden",
        }}
      >
        {v}
      </span>

      {/* ID / cost */}
      <span
        style={{
          fontFamily: "JetBrains Mono, monospace",
          fontSize: 10,
          fontWeight: 500,
          color: costColor ?? "#5d6e8a",
          textAlign: "right",
          whiteSpace: "nowrap",
        }}
      >
        {id ?? ""}
      </span>

      {/* Reroll cell */}
      <span style={{ display: "flex", justifyContent: "center", alignItems: "center" }}>
        {reroll ?? null}
      </span>
    </div>
  );
}
