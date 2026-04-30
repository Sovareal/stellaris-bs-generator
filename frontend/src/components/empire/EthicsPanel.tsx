import { useEmpireStore } from "@/stores/useEmpireStore";
import { displayName } from "@/lib/format";
import { EntityIcon } from "@/components/EntityIcon";
import { Panel } from "./Panel";
import { MonoRow } from "./MonoRow";
import { Tag } from "./Tag";
import { InlineReroll } from "./InlineReroll";
import type { EmpireResponse } from "@/types/empire";

interface EthicsPanelProps {
  empire: EmpireResponse;
}

export function EthicsPanel({ empire }: EthicsPanelProps) {
  const reroll = useEmpireStore((s) => s.reroll);
  const isRerolling = useEmpireStore((s) => s.isRerolling);
  const isLoading = useEmpireStore((s) => s.isLoading);
  const isAddingTrait = useEmpireStore((s) => s.isAddingTrait);
  const isAddingLeaderTrait = useEmpireStore((s) => s.isAddingLeaderTrait);

  const anyBusy = isRerolling !== null || isLoading || isAddingTrait || isAddingLeaderTrait;
  const r = empire.rerollsAvailable;
  const totalCost = empire.ethics.reduce((sum, e) => sum + e.cost, 0);

  return (
    <Panel
      code="IDE.01"
      title="Ethics"
      headerTag={<Tag>{totalCost}/3 PT</Tag>}
      headerReroll={
        <InlineReroll
          available={!!r["ethics"] && !anyBusy}
          loading={isRerolling === "ethics"}
          onClick={() => reroll("ethics")}
          title="Reroll ethics"
        />
      }
    >
      {empire.ethics.map((ethic, i) => (
        <MonoRow
          key={ethic.id}
          k={ethic.isFanatic ? "FANATIC" : "STANDARD"}
          v={
            <>
              <EntityIcon category="ethics" id={ethic.id} size={32} />
              <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                {displayName(ethic)}
              </span>
            </>
          }
          id={`+${ethic.cost}`}
          costColor={ethic.isFanatic ? "#4fc3f7" : "#5d6e8a"}
          accent={ethic.isFanatic ? "#4fc3f7" : undefined}
          last={i === empire.ethics.length - 1}
        />
      ))}
    </Panel>
  );
}
