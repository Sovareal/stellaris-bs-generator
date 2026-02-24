import { Dices, Loader2, Plus, CheckCircle, Minus } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EntityIcon } from "@/components/EntityIcon";
import { displayName, humanizeId } from "@/lib/format";
import { useEmpireStore } from "@/stores/useEmpireStore";
import type { ArchetypeDto, TraitDto } from "@/types/empire";

interface TraitsSlotProps {
  archetype: ArchetypeDto;
  speciesClass: string;
  speciesClassName: string | null;
  traits: TraitDto[];
  pointsUsed: number;
  pointsBudget: number;
  rerollAvailable: boolean;
}

function traitColor(cost: number): string {
  if (cost > 0) return "text-primary";       // positive cost = cyan
  if (cost < 0) return "text-destructive";    // negative cost = red (beneficial)
  return "text-muted-foreground";             // zero cost = neutral
}

export function TraitsSlot({ archetype, speciesClass, speciesClassName, traits, pointsUsed, pointsBudget, rerollAvailable }: TraitsSlotProps) {
  const archetypeName = displayName(archetype);
  const classDisplayName = speciesClassName ?? humanizeId(speciesClass);
  const speciesLabel = speciesClass !== archetype.id
    ? `${archetypeName} — ${classDisplayName}`
    : archetypeName;

  const rerollTrait = useEmpireStore((s) => s.rerollTrait);
  const addTrait = useEmpireStore((s) => s.addTrait);
  const removeTrait = useEmpireStore((s) => s.removeTrait);
  const finalizeTraits = useEmpireStore((s) => s.finalizeTraits);
  const isRerollingTrait = useEmpireStore((s) => s.isRerollingTrait);
  const isAddingTrait = useEmpireStore((s) => s.isAddingTrait);
  const isRemovingTrait = useEmpireStore((s) => s.isRemovingTrait);
  const isAddingLeaderTrait = useEmpireStore((s) => s.isAddingLeaderTrait);
  const isRerolling = useEmpireStore((s) => s.isRerolling);
  const isLoading = useEmpireStore((s) => s.isLoading);
  const traitsFinalized = useEmpireStore((s) => s.traitsFinalized);

  // Picks remaining: origin-free traits don't count against picks
  const picksUsed = traits.filter(t => !t.free).length;
  const picksRemaining = archetype.maxTraits - picksUsed;
  const ptsRemaining = pointsBudget - pointsUsed;

  const anyBusy = isRerolling !== null || isLoading || isRerollingTrait !== null || isAddingTrait || isAddingLeaderTrait || isRemovingTrait;
  const canAddTrait = picksRemaining > 0 && !anyBusy;
  const canRemoveTrait = picksRemaining < 0 && !anyBusy;
  const canFinalize = !traitsFinalized && ptsRemaining >= 0 && picksRemaining >= 0 && !anyBusy;

  return (
    <div className="flex flex-col gap-2 py-2 border-b border-border">
      <div className="flex items-start justify-between gap-4">
        <div className="flex flex-col gap-1.5 min-w-0">
          <div className="flex items-center gap-2">
            <span className="text-xs uppercase tracking-wider text-muted-foreground">
              Species Traits
            </span>
            <span className="text-xs text-muted-foreground">
              {speciesLabel}
              {archetype.robotic && " (Robotic)"}
            </span>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {traits.length === 0 && (
              <span className="text-xs text-muted-foreground italic">No traits rolled yet</span>
            )}
            {traits.map((trait) => {
              const isThisRerolling = isRerollingTrait === trait.id;
              return (
                <Badge key={trait.id} variant="secondary" className="flex items-center gap-1">
                  <EntityIcon category="traits" id={trait.id} size={24} />
                  {trait.enforced && (
                    <span className="text-xs text-yellow-500" title="Locked by origin">🔒</span>
                  )}
                  <span className={traitColor(trait.cost)}>
                    {displayName(trait)}
                  </span>
                  <span className={`ml-1 text-xs ${traitColor(trait.cost)}`}>
                    {trait.cost > 0 ? `+${trait.cost}` : trait.cost}
                  </span>
                  {!trait.enforced && (
                    <button
                      onClick={() => rerollTrait(trait.id)}
                      disabled={!rerollAvailable || anyBusy}
                      className="ml-0.5 text-muted-foreground hover:text-primary disabled:opacity-30 disabled:cursor-not-allowed"
                      title={rerollAvailable ? `Reroll ${displayName(trait)}` : "Reroll used"}
                    >
                      {isThisRerolling ? (
                        <Loader2 className="h-3 w-3 animate-spin" />
                      ) : (
                        <Dices className="h-3 w-3" />
                      )}
                    </button>
                  )}
                </Badge>
              );
            })}
          </div>
          <span className={`text-xs ${picksRemaining >= 0 ? "text-green-500" : "text-destructive"}`}>
            {picksUsed}/{archetype.maxTraits} picks
          </span>
          <span className={`text-xs font-medium ${ptsRemaining >= 0 ? "text-green-500" : "text-destructive"}`}>
            {ptsRemaining >= 0 ? `+${ptsRemaining}` : ptsRemaining} pts remaining
          </span>
        </div>
      </div>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={addTrait}
          disabled={!canAddTrait}
          className="gap-1.5 text-xs"
        >
          {isAddingTrait ? (
            <Loader2 className="h-3 w-3 animate-spin" />
          ) : (
            <Plus className="h-3 w-3" />
          )}
          Roll Trait
        </Button>
        {picksRemaining < 0 && (
          <Button
            variant="outline"
            size="sm"
            onClick={removeTrait}
            disabled={!canRemoveTrait}
            className="gap-1.5 text-xs text-destructive border-destructive/50 hover:bg-destructive/10"
          >
            {isRemovingTrait ? (
              <Loader2 className="h-3 w-3 animate-spin" />
            ) : (
              <Minus className="h-3 w-3" />
            )}
            Remove Trait
          </Button>
        )}
        {!traitsFinalized && (
          <Button
            variant="ghost"
            size="sm"
            onClick={finalizeTraits}
            disabled={!canFinalize}
            className="gap-1.5 text-xs text-muted-foreground hover:text-foreground"
          >
            <CheckCircle className="h-3 w-3" />
            Done Rolling
          </Button>
        )}
        {traitsFinalized && (
          <span className="text-xs text-muted-foreground flex items-center gap-1">
            <CheckCircle className="h-3 w-3 text-green-500" />
            Traits finalized
          </span>
        )}
      </div>
    </div>
  );
}
