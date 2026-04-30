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
      <StatusBar empire={empire} gameVersion={gameVersion} generationId={generationId} />

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
            color: "#5d6e8a",
            fontFamily: "JetBrains Mono, monospace",
            fontSize: 12,
            letterSpacing: 0.8,
          }}
        >
          PRESS GENERATE TO BEGIN
        </div>
      )}
    </main>
  );
}
