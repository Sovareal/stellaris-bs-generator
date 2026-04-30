import { EthicsPanel } from "./EthicsPanel";
import { AuthorityPanel } from "./AuthorityPanel";
import { CivicsPanel } from "./CivicsPanel";
import { OriginPanel } from "./OriginPanel";
import { SpeciesPanel } from "./SpeciesPanel";
import { SecondarySpeciesPanel } from "./SecondarySpeciesPanel";
import { HomeworldPanel } from "./HomeworldPanel";
import { ShipsetPanel } from "./ShipsetPanel";
import { LeaderPanel } from "./LeaderPanel";
import type { EmpireResponse } from "@/types/empire";

interface EmpireConsoleProps {
  empire: EmpireResponse;
}

export function EmpireConsole({ empire }: EmpireConsoleProps) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "1fr 1fr 1fr",
        gap: 10,
        flex: 1,
        minHeight: 0,
        overflowY: "auto",
      }}
    >
      {/* IDEOLOGY column */}
      <div style={{ display: "flex", flexDirection: "column", gap: 8, minHeight: 0 }}>
        <EthicsPanel empire={empire} />
        <AuthorityPanel empire={empire} />
        <CivicsPanel empire={empire} />
        <OriginPanel empire={empire} />
      </div>

      {/* GENOME column */}
      <div style={{ display: "flex", flexDirection: "column", gap: 8, minHeight: 0 }}>
        <SpeciesPanel empire={empire} />
        {empire.secondarySpecies && (
          <SecondarySpeciesPanel empire={empire} />
        )}
      </div>

      {/* LOGISTICS column */}
      <div style={{ display: "flex", flexDirection: "column", gap: 8, minHeight: 0 }}>
        <HomeworldPanel empire={empire} />
        <ShipsetPanel empire={empire} />
        <LeaderPanel empire={empire} />
      </div>
    </div>
  );
}
