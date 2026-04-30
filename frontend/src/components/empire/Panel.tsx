import type { ReactNode } from "react";

interface PanelProps {
  title: string;
  code: string;
  children: ReactNode;
  headerTag?: ReactNode;
  headerReroll?: ReactNode;
}

export function Panel({ title, code, children, headerTag, headerReroll }: PanelProps) {
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        background: "#0f1524",
        border: "1px solid #1a2238",
        borderRadius: 4,
        overflow: "hidden",
        minHeight: 0,
      }}
    >
      {/* Header strip */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "7px 12px",
          background: "#131b2e",
          borderBottom: "1px solid #1a2238",
          flexShrink: 0,
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 7 }}>
          <span
            style={{
              fontFamily: "JetBrains Mono, monospace",
              fontSize: 12,
              fontWeight: 700,
              color: "#4fc3f7",
              letterSpacing: 1.2,
            }}
          >
            [{code}]
          </span>
          <span
            style={{
              fontFamily: "JetBrains Mono, monospace",
              fontSize: 14,
              fontWeight: 600,
              color: "#9aabc7",
              letterSpacing: 1.2,
              textTransform: "uppercase",
            }}
          >
            {title}
          </span>
          {headerTag && <span style={{ marginLeft: 4 }}>{headerTag}</span>}
        </div>
        {headerReroll && <div>{headerReroll}</div>}
      </div>

      {/* Body */}
      <div style={{ padding: "10px 14px", display: "flex", flexDirection: "column", flex: 1, minHeight: 0 }}>
        {children}
      </div>
    </div>
  );
}
