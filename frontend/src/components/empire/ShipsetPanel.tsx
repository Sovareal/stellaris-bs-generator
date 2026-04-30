import { useEmpireStore } from "@/stores/useEmpireStore";
import { humanizeId } from "@/lib/format";
import { Panel } from "./Panel";
import { MonoRow } from "./MonoRow";
import { InlineReroll } from "./InlineReroll";
import type { EmpireResponse } from "@/types/empire";

interface ShipsetPanelProps {
  empire: EmpireResponse;
}

export function ShipsetPanel({ empire }: ShipsetPanelProps) {
  const reroll           = useEmpireStore((s) => s.reroll);
  const isRerolling      = useEmpireStore((s) => s.isRerolling);
  const isLoading        = useEmpireStore((s) => s.isLoading);
  const isAddingTrait    = useEmpireStore((s) => s.isAddingTrait);
  const isAddingLeaderTrait = useEmpireStore((s) => s.isAddingLeaderTrait);

  const anyBusy = isRerolling !== null || isLoading || isAddingTrait || isAddingLeaderTrait;
  const r = empire.rerollsAvailable;

  const shipsetLabel = empire.shipsetName ?? humanizeId(empire.shipset);

  return (
    <Panel
      code="LOG.02"
      title="Shipset"
      headerReroll={
        <InlineReroll
          available={!!r["shipset"] && !anyBusy}
          loading={isRerolling === "shipset"}
          onClick={() => reroll("shipset")}
          title="Reroll shipset"
        />
      }
    >
      <MonoRow k="STYLE" v={shipsetLabel} id="" last />
    </Panel>
  );
}
