import { CheckCircle, Lock, Minus, Plus } from "lucide-react";
import { EntityIcon } from "@/components/EntityIcon";
import { displayName, formatCost, humanizeId, traitCostColor } from "@/lib/format";
import { useEmpireStore } from "@/stores/useEmpireStore";
import { Panel } from "./Panel";
import { MonoRow } from "./MonoRow";
import { Tag } from "./Tag";
import { HBar } from "./HBar";
import { RowReroll } from "./RowReroll";
import { ConsoleButton } from "./ConsoleButton";
import type { EmpireResponse } from "@/types/empire";

interface SpeciesPanelProps {
  empire: EmpireResponse;
}

export function SpeciesPanel({ empire }: SpeciesPanelProps) {
  const rerollTrait      = useEmpireStore((s) => s.rerollTrait);
  const addTrait         = useEmpireStore((s) => s.addTrait);
  const removeTrait      = useEmpireStore((s) => s.removeTrait);
  const finalizeTraits   = useEmpireStore((s) => s.finalizeTraits);
  const isRerollingTrait = useEmpireStore((s) => s.isRerollingTrait);
  const isRerolling      = useEmpireStore((s) => s.isRerolling);
  const isLoading        = useEmpireStore((s) => s.isLoading);
  const isAddingTrait    = useEmpireStore((s) => s.isAddingTrait);
  const isRemovingTrait  = useEmpireStore((s) => s.isRemovingTrait);
  const isAddingLeaderTrait = useEmpireStore((s) => s.isAddingLeaderTrait);
  const traitsFinalized  = useEmpireStore((s) => s.traitsFinalized);

  const { speciesArchetype: archetype, speciesClass, speciesClassName, speciesTraits: traits,
          traitPointsUsed, traitPointsBudget } = empire;

  const picksUsed      = traits.filter((t) => !t.free).length;
  const picksRemaining = archetype.maxTraits - picksUsed;
  const ptsRemaining   = traitPointsBudget - traitPointsUsed;

  const anyBusy      = isRerolling !== null || isLoading || isRerollingTrait !== null
                       || isAddingTrait || isAddingLeaderTrait || isRemovingTrait;
  const canAddTrait  = picksRemaining > 0 && !anyBusy && !traitsFinalized;
  const canRemove    = (picksRemaining < 0 || ptsRemaining < 0) && !anyBusy;
  const canFinalize  = !traitsFinalized && ptsRemaining >= 0 && picksRemaining >= 0 && !anyBusy;
  const rollPulse    = canAddTrait && !traitsFinalized;

  const tagColor   = picksUsed <= archetype.maxTraits ? "#22c55e" : "#ef4444";
  const classLabel = speciesClassName ?? humanizeId(speciesClass);

  return (
    <Panel
      code="GEN.01"
      title="Species"
      headerTag={<Tag color={tagColor}>{picksUsed}/{archetype.maxTraits}</Tag>}
    >
      <MonoRow
        k="CLASS"
        v={classLabel}
        id=""
        last
      />

      <div
        style={{
          borderTop: "1px dashed #1c2740",
          marginTop: 6,
          paddingTop: 5,
          display: "flex",
          alignItems: "center",
          gap: 6,
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
          TRAITS [{traits.length}]
        </span>
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: 2, marginTop: 4, flex: 1, overflowY: "auto", minHeight: 0 }}>
        {traits.map((trait) => {
          const isThisRerolling = isRerollingTrait === trait.id;
          const canRerollThis   = !trait.enforced && !anyBusy && !traitsFinalized;
          return (
            <div
              key={trait.id}
              style={{
                display: "grid",
                gridTemplateColumns: "24px 1fr auto 22px",
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
                {trait.enforced && (
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
              <RowReroll
                available={canRerollThis}
                loading={isThisRerolling}
                onClick={() => rerollTrait(trait.id)}
                title={`Reroll ${displayName(trait)}`}
              />
            </div>
          );
        })}
      </div>

      <HBar used={traitPointsUsed} max={traitPointsBudget} label="Trait Budget" />

      <div style={{ display: "flex", gap: 6, marginTop: 8 }}>
        {!traitsFinalized ? (
          <>
            <ConsoleButton
              variant="primary"
              pulse={rollPulse}
              loading={isAddingTrait}
              disabled={!canAddTrait}
              onClick={addTrait}
              icon={<Plus size={12} />}
            >
              ROLL TRAIT
            </ConsoleButton>
            {canRemove && (
              <ConsoleButton
                variant="destructive"
                loading={isRemovingTrait}
                disabled={!canRemove}
                onClick={removeTrait}
                icon={<Minus size={12} />}
              >
                REMOVE
              </ConsoleButton>
            )}
            <ConsoleButton
              variant="finalize"
              disabled={!canFinalize}
              onClick={finalizeTraits}
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
