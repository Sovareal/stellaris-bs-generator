import { useEmpireStore } from "@/stores/useEmpireStore";
import { displayName } from "@/lib/format";
import { EntityIcon } from "@/components/EntityIcon";
import { Panel } from "./Panel";
import { MonoRow } from "./MonoRow";
import { InlineReroll } from "./InlineReroll";
import type { EmpireResponse } from "@/types/empire";

interface HomeworldPanelProps {
  empire: EmpireResponse;
}

export function HomeworldPanel({ empire }: HomeworldPanelProps) {
  const reroll              = useEmpireStore((s) => s.reroll);
  const isRerolling         = useEmpireStore((s) => s.isRerolling);
  const isLoading           = useEmpireStore((s) => s.isLoading);
  const isAddingTrait       = useEmpireStore((s) => s.isAddingTrait);
  const isAddingLeaderTrait = useEmpireStore((s) => s.isAddingLeaderTrait);

  const anyBusy = isRerolling !== null || isLoading || isAddingTrait || isAddingLeaderTrait;

  const r       = empire.rerollsAvailable;
  const hw      = empire.homeworld;
  const isFixed = hw.climate === "fixed";
  const classId = isFixed ? "FIXED" : hw.climate.toUpperCase();

  return (
    <Panel
      code="LOG.01"
      title="Homeworld"
      headerReroll={
        <InlineReroll
          available={!isFixed && !!r["homeworld"] && !anyBusy}
          loading={isRerolling === "homeworld"}
          onClick={() => reroll("homeworld")}
          title="Reroll homeworld"
        />
      }
    >
      <MonoRow
        k="CLASS"
        v={
          <>
            <EntityIcon category="planets" id={hw.id} size={32} />
            <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
              {displayName(hw)}
            </span>
          </>
        }
        id={classId}
        last
      />
    </Panel>
  );
}
