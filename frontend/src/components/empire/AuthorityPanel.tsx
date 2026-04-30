import { useEmpireStore } from "@/stores/useEmpireStore";
import { displayName } from "@/lib/format";
import { EntityIcon } from "@/components/EntityIcon";
import { Panel } from "./Panel";
import { MonoRow } from "./MonoRow";
import { InlineReroll } from "./InlineReroll";
import type { EmpireResponse } from "@/types/empire";

interface AuthorityPanelProps {
  empire: EmpireResponse;
}

export function AuthorityPanel({ empire }: AuthorityPanelProps) {
  const reroll = useEmpireStore((s) => s.reroll);
  const isRerolling = useEmpireStore((s) => s.isRerolling);
  const isLoading = useEmpireStore((s) => s.isLoading);
  const isAddingTrait = useEmpireStore((s) => s.isAddingTrait);
  const isAddingLeaderTrait = useEmpireStore((s) => s.isAddingLeaderTrait);

  const anyBusy = isRerolling !== null || isLoading || isAddingTrait || isAddingLeaderTrait;
  const r = empire.rerollsAvailable;

  return (
    <Panel
      code="IDE.02"
      title="Authority"
      headerReroll={
        <InlineReroll
          available={!!r["authority"] && !anyBusy}
          loading={isRerolling === "authority"}
          onClick={() => reroll("authority")}
          title="Reroll authority"
        />
      }
    >
      <MonoRow
        k="TYPE"
        v={
          <>
            <EntityIcon category="authorities" id={empire.authority.id} size={32} />
            <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
              {displayName(empire.authority)}
            </span>
          </>
        }
        id={empire.authority.isGestalt ? "GESTALT" : "STANDARD"}
        last
      />
    </Panel>
  );
}
