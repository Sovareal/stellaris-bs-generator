import { CheckCircle, Plus } from "lucide-react";
import { EntityIcon } from "@/components/EntityIcon";
import { displayName, formatCost, humanizeId, traitCostColor } from "@/lib/format";
import { useEmpireStore } from "@/stores/useEmpireStore";
import { Panel } from "./Panel";
import { MonoRow } from "./MonoRow";
import { Tag } from "./Tag";
import { HBar } from "./HBar";
import { InlineReroll } from "./InlineReroll";
import { ConsoleButton } from "./ConsoleButton";
import type { EmpireResponse } from "@/types/empire";

interface LeaderPanelProps {
  empire: EmpireResponse;
}

export function LeaderPanel({ empire }: LeaderPanelProps) {
  const reroll               = useEmpireStore((s) => s.reroll);
  const addLeaderTrait       = useEmpireStore((s) => s.addLeaderTrait);
  const finalizeLeaderTraits = useEmpireStore((s) => s.finalizeLeaderTraits);
  const isRerolling          = useEmpireStore((s) => s.isRerolling);
  const isLoading            = useEmpireStore((s) => s.isLoading);
  const isAddingTrait        = useEmpireStore((s) => s.isAddingTrait);
  const isAddingLeaderTrait  = useEmpireStore((s) => s.isAddingLeaderTrait);
  const leaderTraitsFinalized = useEmpireStore((s) => s.leaderTraitsFinalized);

  const { leader, origin } = empire;
  const isLuminary = origin.id === "origin_legendary_leader";

  const budgetUsed     = leader.traits.reduce((sum, t) => sum + t.cost, 0);
  const picksRemaining = leader.leaderPicksMax - leader.traits.length;
  const ptsRemaining   = leader.leaderBudget - budgetUsed;

  const anyBusy     = isRerolling !== null || isLoading || isAddingTrait || isAddingLeaderTrait;
  const canAddTrait = isLuminary && picksRemaining > 0 && !anyBusy && !leaderTraitsFinalized;
  const canFinalize = isLuminary && !leaderTraitsFinalized && ptsRemaining >= 0 && picksRemaining >= 0 && !anyBusy;
  const rollPulse   = canAddTrait && leader.traits.length === 0;

  const r        = empire.rerollsAvailable;
  const tagColor = leader.traits.length <= leader.leaderPicksMax ? "#22c55e" : "#ef4444";

  return (
    <Panel
      code="LOG.03"
      title="Leader"
      headerTag={isLuminary ? <Tag color={tagColor}>{leader.traits.length}/{leader.leaderPicksMax}</Tag> : undefined}
      headerReroll={
        <InlineReroll
          available={!!r["leader"] && !anyBusy}
          loading={isRerolling === "leader"}
          onClick={() => reroll("leader")}
          title="Reroll leader"
        />
      }
    >
      <MonoRow
        k="CLASS"
        v={humanizeId(leader.leaderClass)}
        id=""
        last={leader.traits.length === 0}
      />

      {/* Non-Luminary: show traits as colored badge chips */}
      {!isLuminary && leader.traits.length > 0 && (
        <div style={{ display: "flex", flexWrap: "wrap", gap: 5, marginTop: 8 }}>
          {leader.traits.map((trait) => (
            <span
              key={trait.id}
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: 5,
                padding: "3px 8px",
                background: `${traitCostColor(trait.cost)}18`,
                border: `1px solid ${traitCostColor(trait.cost)}45`,
                borderRadius: 3,
                fontFamily: "Inter, system-ui, sans-serif",
                fontSize: 12,
                fontWeight: 500,
                color: traitCostColor(trait.cost),
                whiteSpace: "nowrap",
              }}
            >
              <EntityIcon category="leadertraits" id={trait.id} size={26} />
              {displayName(trait)}
            </span>
          ))}
        </div>
      )}

      {/* Luminary: interactive trait rolling section */}
      {isLuminary && (
        <>
          <div
            style={{
              borderTop: "1px dashed #1c2740",
              marginTop: 6,
              paddingTop: 5,
            }}
          >
            <span
              style={{
                fontFamily: "JetBrains Mono, monospace",
                fontSize: 12,
                fontWeight: 600,
                color: "#5d6e8a",
                letterSpacing: 0.8,
                textTransform: "uppercase",
              }}
            >
              TRAITS [{leader.traits.length}]
            </span>
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: 2, marginTop: 4, flex: 1, overflowY: "auto", minHeight: 0 }}>
            {leader.traits.map((trait) => (
              <div
                key={trait.id}
                style={{
                  display: "grid",
                  gridTemplateColumns: "24px 1fr auto",
                  alignItems: "center",
                  gap: 5,
                  padding: "2px 0",
                }}
              >
                <EntityIcon category="leadertraits" id={trait.id} size={32} />
                <span
                  style={{
                    fontFamily: "Inter, system-ui, sans-serif",
                    fontSize: 14,
                    fontWeight: 500,
                    color: "#e0e6ed",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                    whiteSpace: "nowrap",
                  }}
                >
                  {displayName(trait)}
                </span>
                <span
                  style={{
                    fontFamily: "JetBrains Mono, monospace",
                    fontSize: 12,
                    fontWeight: 600,
                    color: traitCostColor(trait.cost),
                    whiteSpace: "nowrap",
                  }}
                >
                  {formatCost(trait.cost)}
                </span>
              </div>
            ))}
          </div>

          <HBar used={budgetUsed} max={leader.leaderBudget} label="Leader Budget" />

          <div style={{ display: "flex", gap: 6, marginTop: 8 }}>
            {!leaderTraitsFinalized ? (
              <>
                <ConsoleButton
                  variant="primary"
                  pulse={rollPulse}
                  loading={isAddingLeaderTrait}
                  disabled={!canAddTrait}
                  onClick={addLeaderTrait}
                  icon={<Plus size={12} />}
                >
                  ROLL TRAIT
                </ConsoleButton>
                <ConsoleButton
                  variant="finalize"
                  disabled={!canFinalize}
                  onClick={finalizeLeaderTraits}
                  icon={<CheckCircle size={12} />}
                >
                  FINALIZE
                </ConsoleButton>
              </>
            ) : (
              <span
                style={{
                  fontFamily: "JetBrains Mono, monospace",
                  fontSize: 13,
                  color: "#22c55e",
                  display: "flex",
                  alignItems: "center",
                  gap: 5,
                }}
              >
                <CheckCircle size={14} />
                Traits finalized
              </span>
            )}
          </div>
        </>
      )}
    </Panel>
  );
}
