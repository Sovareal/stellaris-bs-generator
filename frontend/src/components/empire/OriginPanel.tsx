import { useEmpireStore } from "@/stores/useEmpireStore";
import { displayName } from "@/lib/format";
import { EntityIcon } from "@/components/EntityIcon";
import { Panel } from "./Panel";
import { MonoRow } from "./MonoRow";
import { InlineReroll } from "./InlineReroll";
import { RowReroll } from "./RowReroll";
import { Tag } from "./Tag";
import type { EmpireResponse } from "@/types/empire";

interface OriginPanelProps {
  empire: EmpireResponse;
}

export function OriginPanel({ empire }: OriginPanelProps) {
  const reroll = useEmpireStore((s) => s.reroll);
  const isRerolling = useEmpireStore((s) => s.isRerolling);
  const isLoading = useEmpireStore((s) => s.isLoading);
  const isAddingTrait = useEmpireStore((s) => s.isAddingTrait);
  const isAddingLeaderTrait = useEmpireStore((s) => s.isAddingLeaderTrait);

  const anyBusy = isRerolling !== null || isLoading || isAddingTrait || isAddingLeaderTrait;
  const r = empire.rerollsAvailable;
  const originId = empire.origin.dlcRequirement
    ? `${empire.origin.dlcRequirement} DLC`
    : "BASE";
  const canRerollNomadic = "nomadic" in r;

  return (
    <Panel
      code="IDE.04"
      title="Origin"
      headerTag={empire.nomadic ? <Tag color="#facc15">NOMADIC</Tag> : undefined}
      headerReroll={
        <InlineReroll
          available={!!r["origin"] && !anyBusy}
          loading={isRerolling === "origin"}
          onClick={() => reroll("origin")}
          title="Reroll origin"
        />
      }
    >
      <MonoRow
        k="ORIGIN"
        v={
          <>
            <EntityIcon category="origins" id={empire.origin.id} size={36} />
            <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
              {displayName(empire.origin)}
            </span>
          </>
        }
        id={originId}
        last={!canRerollNomadic}
      />
      {canRerollNomadic && (
        <MonoRow
          k="NOMADIC"
          v={
            <>
              <EntityIcon category="indicators" id="nomadic" size={20} />
              <span>{empire.nomadic ? "Yes" : "No"}</span>
            </>
          }
          id=""
          last
          reroll={
            <RowReroll
              available={!!r["nomadic"] && !anyBusy}
              loading={isRerolling === "nomadic"}
              onClick={() => reroll("nomadic")}
              title="Reroll nomadic status"
            />
          }
        />
      )}
    </Panel>
  );
}
