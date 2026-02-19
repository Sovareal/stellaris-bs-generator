import { Dices, Loader2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { EntityIcon } from "@/components/EntityIcon";
import { RerollButton } from "@/components/RerollButton";
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
  const isRerollingTrait = useEmpireStore((s) => s.isRerollingTrait);
  const isRerolling = useEmpireStore((s) => s.isRerolling);
  const isLoading = useEmpireStore((s) => s.isLoading);
  const traitButtonsDisabled = !rerollAvailable || isRerolling !== null || isLoading || isRerollingTrait !== null;

  return (
    <div className="flex items-start justify-between gap-4 py-2">
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
                    disabled={traitButtonsDisabled}
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
        <span className="text-xs text-muted-foreground">
          {traits.filter(t => !t.enforced).length}/{archetype.maxTraits} picks · {pointsBudget - pointsUsed} pts remaining
        </span>
      </div>
      <RerollButton category="traits" available={rerollAvailable} />
    </div>
  );
}
