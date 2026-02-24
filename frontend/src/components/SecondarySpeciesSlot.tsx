import { CheckCircle, Loader2, Plus } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EntityIcon } from "@/components/EntityIcon";
import { RerollButton } from "@/components/RerollButton";
import { displayName, humanizeId } from "@/lib/format";
import { useEmpireStore } from "@/stores/useEmpireStore";
import type { SecondarySpeciesDto } from "@/types/empire";

interface SecondarySpeciesSlotProps {
  secondarySpecies: SecondarySpeciesDto;
  rerollAvailable: boolean;
}

function traitColor(cost: number): string {
  if (cost > 0) return "text-primary";
  if (cost < 0) return "text-destructive";
  return "text-muted-foreground";
}

export function SecondarySpeciesSlot({ secondarySpecies, rerollAvailable }: SecondarySpeciesSlotProps) {
  const title = secondarySpecies.titleDisplayName ?? humanizeId(secondarySpecies.title);
  const className = secondarySpecies.speciesClassName ?? humanizeId(secondarySpecies.speciesClass);

  const addSecondaryTrait = useEmpireStore((s) => s.addSecondaryTrait);
  const finalizeSecondaryTraits = useEmpireStore((s) => s.finalizeSecondaryTraits);
  const isAddingSecondaryTrait = useEmpireStore((s) => s.isAddingSecondaryTrait);
  const isAddingTrait = useEmpireStore((s) => s.isAddingTrait);
  const isRerolling = useEmpireStore((s) => s.isRerolling);
  const isLoading = useEmpireStore((s) => s.isLoading);
  const secondaryTraitsFinalized = useEmpireStore((s) => s.secondaryTraitsFinalized);

  const maxAdditionalPicks = secondarySpecies.maxTraitPicks - secondarySpecies.enforcedTraits.length;
  const additionalPicksUsed = secondarySpecies.additionalTraits.length;
  const picksRemaining = maxAdditionalPicks - additionalPicksUsed;
  const ptsRemaining = secondarySpecies.traitPointsBudget - secondarySpecies.traitPointsUsed;

  const anyBusy = isRerolling !== null || isLoading || isAddingTrait || isAddingSecondaryTrait;
  const canAddTrait = picksRemaining > 0 && !anyBusy;
  const canFinalize = !secondaryTraitsFinalized && ptsRemaining >= 0 && picksRemaining >= 0 && !anyBusy;
  const rollPulse = canAddTrait && additionalPicksUsed === 0;
  const donePulse = canFinalize && additionalPicksUsed > 0;

  return (
    <div className="flex flex-col gap-2 py-2 border-b border-border">
      <div className="flex items-start justify-between gap-4">
        <div className="flex flex-col gap-1.5 min-w-0">
          <div className="flex items-center gap-2">
            <span className="text-xs uppercase tracking-wider text-muted-foreground">
              {title}
            </span>
            <span className="text-xs text-muted-foreground">
              {className}
            </span>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {secondarySpecies.enforcedTraits.length === 0 && secondarySpecies.additionalTraits.length === 0 && (
              <span className="text-xs text-muted-foreground italic">No traits rolled yet</span>
            )}
            {secondarySpecies.enforcedTraits.map((trait) => (
              <Badge key={trait.id} variant="outline" className="border-primary/40 flex items-center gap-1">
                <EntityIcon category="traits" id={trait.id} size={24} />
                <span className={traitColor(trait.cost)}>
                  {displayName(trait)}
                </span>
                <span className={`ml-1 text-xs ${traitColor(trait.cost)}`}>
                  {trait.cost > 0 ? `+${trait.cost}` : trait.cost}
                </span>
                <span className="ml-1 text-xs text-muted-foreground">locked</span>
              </Badge>
            ))}
            {secondarySpecies.additionalTraits.map((trait) => (
              <Badge key={trait.id} variant="secondary" className="flex items-center gap-1">
                <EntityIcon category="traits" id={trait.id} size={24} />
                <span className={traitColor(trait.cost)}>
                  {displayName(trait)}
                </span>
                <span className={`ml-1 text-xs ${traitColor(trait.cost)}`}>
                  {trait.cost > 0 ? `+${trait.cost}` : trait.cost}
                </span>
              </Badge>
            ))}
          </div>
          <span className={`text-xs ${picksRemaining >= 0 ? "text-green-500" : "text-destructive"}`}>
            {additionalPicksUsed}/{maxAdditionalPicks} picks
          </span>
          <span className={`text-xs font-medium ${ptsRemaining >= 0 ? "text-green-500" : "text-destructive"}`}>
            {ptsRemaining >= 0 ? `+${ptsRemaining}` : ptsRemaining} pts remaining
          </span>
        </div>
        <RerollButton category="secondaryspecies" available={rerollAvailable} />
      </div>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={addSecondaryTrait}
          disabled={!canAddTrait}
          className={`gap-1.5 text-xs${rollPulse ? " animate-pulse ring-2 ring-primary/60" : ""}`}
        >
          {isAddingSecondaryTrait ? (
            <Loader2 className="h-3 w-3 animate-spin" />
          ) : (
            <Plus className="h-3 w-3" />
          )}
          Roll Trait
        </Button>
        {!secondaryTraitsFinalized && (
          <Button
            variant="ghost"
            size="sm"
            onClick={finalizeSecondaryTraits}
            disabled={!canFinalize}
            className={`gap-1.5 text-xs${donePulse ? " text-green-400 animate-pulse" : " text-muted-foreground hover:text-foreground"}`}
          >
            <CheckCircle className="h-3 w-3" />
            Done Rolling
          </Button>
        )}
        {secondaryTraitsFinalized && (
          <span className="text-xs text-muted-foreground flex items-center gap-1">
            <CheckCircle className="h-3 w-3 text-green-500" />
            Traits finalized
          </span>
        )}
      </div>
    </div>
  );
}
