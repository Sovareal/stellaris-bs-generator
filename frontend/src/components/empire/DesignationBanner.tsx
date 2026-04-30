import { displayName, humanizeId } from "@/lib/format";
import type { EmpireResponse } from "@/types/empire";

interface DesignationBannerProps {
  empire: EmpireResponse;
}

export function DesignationBanner({ empire }: DesignationBannerProps) {
  const authorityLabel = displayName(empire.authority);
  const originLabel    = displayName(empire.origin);
  const classLabel     = empire.speciesClassName ?? humanizeId(empire.speciesClass);
  const subtitle       = `${classLabel} · ${authorityLabel} · ${originLabel}`;

  return (
    <div
      style={{
        padding: "10px 14px",
        background: "linear-gradient(90deg, rgba(79,195,247,0.05), transparent 70%)",
        borderLeft: "3px solid #4fc3f7",
        fontFamily: "JetBrains Mono, monospace",
        flexShrink: 0,
      }}
    >
      <div style={{ fontSize: 9, color: "#4fc3f7", letterSpacing: 1.4 }}>DESIGNATION</div>
      <div
        style={{
          fontSize: 22,
          fontWeight: 600,
          color: "#e0e6ed",
          letterSpacing: 0.5,
          fontFamily: "Inter, system-ui, sans-serif",
          overflow: "hidden",
          textOverflow: "ellipsis",
          whiteSpace: "nowrap",
        }}
      >
        {empire.suggestedName ?? "Unknown Empire"}
      </div>
      <div style={{ fontSize: 10, color: "#7a8ba8", marginTop: 2 }}>{subtitle}</div>
    </div>
  );
}
