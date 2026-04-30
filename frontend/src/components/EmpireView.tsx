import { Zap } from "lucide-react";
import { useEmpireStore } from "@/stores/useEmpireStore";
import { StatusToast } from "@/components/StatusToast";
import { StatusBar } from "@/components/empire/StatusBar";
import { DesignationBanner } from "@/components/empire/DesignationBanner";
import { EmpireConsole } from "@/components/empire/EmpireConsole";

interface EmpireViewProps {
  gameVersion: string | null;
}

export function EmpireView({ gameVersion }: EmpireViewProps) {
  const empire       = useEmpireStore((s) => s.empire);
  const generationId = useEmpireStore((s) => s.generationId);
  const generate     = useEmpireStore((s) => s.generate);
  const isLoading    = useEmpireStore((s) => s.isLoading);

  return (
    <main
      style={{
        flex: 1,
        display: "flex",
        flexDirection: "column",
        minHeight: 0,
        overflow: "hidden",
      }}
    >
      <StatusToast />
      <StatusBar empire={empire} gameVersion={gameVersion} />

      {empire ? (
        <div
          key={generationId}
          style={{
            flex: 1,
            display: "flex",
            flexDirection: "column",
            padding: "14px 16px",
            gap: 12,
            minHeight: 0,
            overflow: "hidden",
          }}
        >
          <DesignationBanner empire={empire} />
          <EmpireConsole empire={empire} />
        </div>
      ) : (
        <div
          style={{
            flex: 1,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 20,
            }}
          >
            <span
              style={{
                fontFamily: "JetBrains Mono, monospace",
                fontSize: 12,
                color: "#3a4866",
                letterSpacing: 2,
                textTransform: "uppercase",
              }}
            >
              NO EMPIRE DATA
            </span>
            <button
              onClick={generate}
              disabled={isLoading}
              aria-label="Generate empire"
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: 12,
                padding: "16px 44px",
                background: "linear-gradient(180deg, #6dd0fb, #4fc3f7)",
                border: "1px solid #7fd6fc",
                borderRadius: 4,
                color: "#0a0e17",
                fontFamily: "JetBrains Mono, monospace",
                fontSize: 18,
                fontWeight: 700,
                letterSpacing: 1.5,
                cursor: isLoading ? "default" : "pointer",
                opacity: isLoading ? 0.7 : 1,
                boxShadow: "0 0 40px rgba(79,195,247,0.5), 0 0 80px rgba(79,195,247,0.2)",
                animation: "telemetryAttn 1.8s ease-in-out infinite",
              }}
            >
              <Zap size={22} />
              GENERATE TO BEGIN
            </button>
          </div>
        </div>
      )}
    </main>
  );
}
