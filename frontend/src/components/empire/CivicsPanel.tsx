import { useEmpireStore } from "@/stores/useEmpireStore";
import { displayName } from "@/lib/format";
import { EntityIcon } from "@/components/EntityIcon";
import { Panel } from "./Panel";
import { MonoRow } from "./MonoRow";
import { Tag } from "./Tag";
import { RowReroll } from "./RowReroll";
import type { EmpireResponse } from "@/types/empire";

const SLOT_LABELS = ["SLOT.1", "SLOT.2"] as const;
const CATEGORIES = ["civic1", "civic2"] as const;

interface CivicsPanelProps {
  empire: EmpireResponse;
}

export function CivicsPanel({ empire }: CivicsPanelProps) {
  const reroll = useEmpireStore((s) => s.reroll);
  const isRerolling = useEmpireStore((s) => s.isRerolling);
  const isLoading = useEmpireStore((s) => s.isLoading);
  const isAddingTrait = useEmpireStore((s) => s.isAddingTrait);
  const isAddingLeaderTrait = useEmpireStore((s) => s.isAddingLeaderTrait);

  const anyBusy = isRerolling !== null || isLoading || isAddingTrait || isAddingLeaderTrait;
  const r = empire.rerollsAvailable;

  return (
    <Panel
      code="IDE.03"
      title="Civics"
      headerTag={<Tag>{empire.civics.length}/2</Tag>}
    >
      {empire.civics.map((civic, i) => {
        const cat = CATEGORIES[i];
        return (
          <MonoRow
            key={civic.id}
            k={SLOT_LABELS[i] ?? `SLOT.${i + 1}`}
            v={
              <>
                <EntityIcon category="civics" id={civic.id} size={32} />
                <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  {displayName(civic)}
                </span>
              </>
            }
            id=""
            last={i === empire.civics.length - 1}
            reroll={
              cat ? (
                <RowReroll
                  available={!!r[cat] && !anyBusy}
                  loading={isRerolling === cat}
                  onClick={() => reroll(cat)}
                  title={`Reroll ${SLOT_LABELS[i]}`}
                />
              ) : undefined
            }
          />
        );
      })}
    </Panel>
  );
}
