import { CheckCircle, Lock, Plus } from "lucide-react";
import { EntityIcon } from "@/components/EntityIcon";
import { displayName, formatCost, humanizeId, traitCostColor } from "@/lib/format";
import { useEmpireStore } from "@/stores/useEmpireStore";
import { Panel } from "./Panel";
import { Tag } from "./Tag";
import { HBar } from "./HBar";
import { InlineReroll } from "./InlineReroll";
import { ConsoleButton } from "./ConsoleButton";
import type { EmpireResponse } from "@/types/empire";

interface SecondarySpeciesPanelProps {
  empire: EmpireResponse;
}

export function SecondarySpeciesPanel({ empire }: SecondarySpeciesPanelProps) {
  const reroll                   = useEmpireStore((s) => s.reroll);
  const addSecondaryTrait        = useEmpireStore((s) => s.addSecondaryTrait);
  const finalizeSecondaryTraits  = useEmpireStore((s) => s.finalizeSecondaryTraits);
  const isAddingSecondaryTrait   = useEmpireStore((s) => s.isAddingSecondaryTrait);
  const isRerolling              = useEmpireStore((s) => s.isRerolling);
  const isLoading                = useEmpireStore((s) => s.isLoading);
  const isAddingTrait            = useEmpireStore((s) => s.isAddingTrait);
  const secondaryTraitsFinalized = useEmpireStore((s) => s.secondaryTraitsFinalized);

  const ss    = empire.secondarySpecies!;
  const title = ss.titleDisplayName ?? humanizeId(ss.title);

  const maxAdditionalPicks  = ss.maxTraitPicks - ss.enforcedTraits.length;
  const additionalPicksUsed = ss.additionalTraits.length;
  const picksRemaining      = maxAdditionalPicks - additionalPicksUsed;
  const ptsRemaining        = ss.traitPointsBudget - ss.traitPointsUsed;

  const anyBusy     = isRerolling !== null || isLoading || isAddingTrait || isAddingSecondaryTrait;
  const canAddTrait = picksRemaining > 0 && !anyBusy && !secondaryTraitsFinalized;
  const canFinalize = !secondaryTraitsFinalized && ptsRemaining >= 0 && picksRemaining >= 0 && !anyBusy;
  const rollPulse   = canAddTrait && additionalPicksUsed === 0;

  const r         = empire.rerollsAvailable;
  const tagColor  = additionalPicksUsed <= maxAdditionalPicks ? "#22c55e" : "#ef4444";
  const allTraits = [...ss.enforcedTraits, ...ss.additionalTraits];

  return (
    <Panel
      code="GEN.02"
      title={title}
      headerTag={<Tag color={tagColor}>{additionalPicksUsed}/{maxAdditionalPicks}</Tag>}
      headerReroll={
        <InlineReroll
          available={!!r["secondaryspecies"] && !anyBusy}
          loading={isRerolling === "secondaryspecies"}
          onClick={() => reroll("secondaryspecies")}
          title={`Reroll ${title}`}
        />
      }
    >
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
          TRAITS [{allTraits.length}]
        </span>
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: 2, marginTop: 4, flex: 1, overflowY: "auto", minHeight: 0 }}>
        {allTraits.map((trait) => {
          const isEnforced = ss.enforcedTraits.some((e) => e.id === trait.id);
          return (
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
              <EntityIcon category="traits" id={trait.id} size={32} />
              <span
                style={{
                  fontFamily: "Inter, system-ui, sans-serif",
                  fontSize: 14,
                  fontWeight: 500,
                  color: "#e0e6ed",
                  display: "flex",
                  alignItems: "center",
                  gap: 4,
                  overflow: "hidden",
                }}
              >
                <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  {displayName(trait)}
                </span>
                {isEnforced && (
                  <Lock size={11} style={{ color: "#7a8ba8", flexShrink: 0 }} />
                )}
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
          );
        })}
      </div>

      <HBar used={ss.traitPointsUsed} max={ss.traitPointsBudget} label="Trait Budget" />

      <div style={{ display: "flex", gap: 6, marginTop: 8 }}>
        {!secondaryTraitsFinalized ? (
          <>
            <ConsoleButton
              variant="primary"
              pulse={rollPulse}
              loading={isAddingSecondaryTrait}
              disabled={!canAddTrait}
              onClick={addSecondaryTrait}
              icon={<Plus size={12} />}
            >
              ROLL TRAIT
            </ConsoleButton>
            <ConsoleButton
              variant="finalize"
              disabled={!canFinalize}
              onClick={finalizeSecondaryTraits}
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
    </Panel>
  );
}
