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
        gridTemplateColumns: "14px 110px 1fr auto 22px",
        alignItems: "center",
        gap: 8,
        padding: "7px 0",
        borderBottom: last ? "none" : "1px dashed #1c2740",
      }}
    >
      {/* Chevron */}
      <span
        style={{
          fontSize: 12,
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
          fontSize: 13,
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
          fontSize: 14,
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
          fontSize: 12,
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
