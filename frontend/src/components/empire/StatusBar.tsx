import { useState } from "react";
import { Zap } from "lucide-react";
import { formatSeed } from "@/lib/format";
import { useEmpireStore } from "@/stores/useEmpireStore";
import { ExportModal } from "@/components/ExportModal";
import type { EmpireResponse } from "@/types/empire";

interface StatusBarProps {
  empire: EmpireResponse | null;
  gameVersion: string | null;
  generationId: number;
}

export function StatusBar({ empire, gameVersion, generationId }: StatusBarProps) {
  const generate  = useEmpireStore((s) => s.generate);
  const isLoading = useEmpireStore((s) => s.isLoading);
  const [exportOpen, setExportOpen] = useState(false);

  const sep = (
    <span style={{ color: "#3a4866", margin: "0 8px", userSelect: "none" }}>│</span>
  );

  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "8px 14px",
        background: "#0d121e",
        borderBottom: "1px solid #1a2238",
        fontFamily: "JetBrains Mono, monospace",
        fontSize: 10.5,
        flexShrink: 0,
      }}
    >
      {/* Left cluster */}
      <div style={{ display: "flex", alignItems: "center" }}>
        <span style={{ color: "#4fc3f7", fontWeight: 700, letterSpacing: 1.2 }}>EMPIRE.GEN</span>

        {sep}

        <span style={{ color: "#7a8ba8" }}>SEED:&nbsp;</span>
        <span style={{ color: "#e0e6ed" }}>{formatSeed(generationId)}</span>

        {sep}

        {/* Valid pill */}
        <span
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: 6,
            padding: "3px 10px",
            background: "linear-gradient(180deg, rgba(34,197,94,0.22), rgba(34,197,94,0.10))",
            border: "1px solid rgba(34,197,94,0.55)",
            borderRadius: 3,
            color: "#86efac",
            fontWeight: 700,
            letterSpacing: 1.4,
            fontSize: 10.5,
            boxShadow: "0 0 14px rgba(34,197,94,0.35), inset 0 0 0 1px rgba(34,197,94,0.15)",
          }}
        >
          <span
            style={{
              width: 7,
              height: 7,
              borderRadius: "50%",
              background: "#22c55e",
              boxShadow: "0 0 0 3px rgba(34,197,94,0.25), 0 0 10px #22c55e",
              flexShrink: 0,
              animation: "telemetryPulse 1.6s ease-in-out infinite",
            }}
          />
          VALID · BUILD OK
        </span>

        {gameVersion && (
          <>
            {sep}
            <span style={{ color: "#5d6e8a" }}>{gameVersion}</span>
          </>
        )}
      </div>

      {/* Right cluster */}
      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
        <button
          disabled={!empire}
          onClick={() => setExportOpen(true)}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: 5,
            padding: "5px 12px",
            background: "rgba(79,195,247,0.12)",
            border: "1px solid rgba(79,195,247,0.40)",
            borderRadius: 3,
            color: empire ? "#4fc3f7" : "#3a4866",
            fontFamily: "JetBrains Mono, monospace",
            fontSize: 10,
            fontWeight: 600,
            letterSpacing: 0.8,
            cursor: empire ? "pointer" : "default",
            opacity: empire ? 1 : 0.45,
          }}
        >
          EXPORT
        </button>

        <button
          disabled={isLoading}
          onClick={generate}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: 5,
            padding: "5px 14px",
            background: "linear-gradient(180deg, #6dd0fb, #4fc3f7)",
            border: "1px solid #7fd6fc",
            borderRadius: 3,
            color: "#0a0e17",
            fontFamily: "JetBrains Mono, monospace",
            fontSize: 10,
            fontWeight: 700,
            letterSpacing: 0.8,
            cursor: isLoading ? "default" : "pointer",
            opacity: isLoading ? 0.7 : 1,
            boxShadow: "0 0 16px rgba(79,195,247,0.45)",
          }}
        >
          <Zap size={11} />
          GENERATE
        </button>
      </div>

      <ExportModal open={exportOpen} onClose={() => setExportOpen(false)} />
    </div>
  );
}
