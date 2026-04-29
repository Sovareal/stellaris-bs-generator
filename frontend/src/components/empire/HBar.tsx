import { formatCost } from "@/lib/format";

const SEGMENTS = 20;

interface HBarProps {
  used: number;
  max: number;
  label: string;
  accent?: string;
}

export function HBar({ used, max, label, accent = "#4fc3f7" }: HBarProps) {
  const remaining = max - used;
  const overBudget = used > max;

  const filled = max > 0 ? Math.min(Math.round((used / max) * SEGMENTS), SEGMENTS) : 0;
  const segColor = overBudget ? "#ef4444" : accent;
  const remColor = remaining >= 0 ? "#22c55e" : "#ef4444";

  return (
    <div style={{ marginTop: 8 }}>
      {/* Label row */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "baseline",
          marginBottom: 4,
        }}
      >
        <span
          style={{
            fontFamily: "JetBrains Mono, monospace",
            fontSize: 9,
            fontWeight: 600,
            color: "#7a8ba8",
            letterSpacing: 0.8,
            textTransform: "uppercase",
          }}
        >
          {label}
        </span>
        <span
          style={{
            fontFamily: "JetBrains Mono, monospace",
            fontSize: 9.5,
            fontWeight: 600,
            color: remColor,
            letterSpacing: 0.4,
          }}
        >
          [{used}/{max}]{" "}
          <span style={{ color: remColor }}>
            {formatCost(remaining)}
          </span>
        </span>
      </div>

      {/* Segments */}
      <div style={{ display: "flex", gap: 1.5 }}>
        {Array.from({ length: SEGMENTS }, (_, i) => (
          <div
            key={i}
            style={{
              flex: 1,
              height: 6,
              borderRadius: 1,
              background: i < filled ? segColor : "#1a2238",
              transition: "background 0.15s",
            }}
          />
        ))}
      </div>
    </div>
  );
}
