import { Badge } from "@/components/ui/badge";
import { EntityIcon } from "@/components/EntityIcon";
import { RerollButton } from "@/components/RerollButton";
import { displayName, humanizeId } from "@/lib/format";
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
  const allTraits = [...secondarySpecies.enforcedTraits, ...secondarySpecies.additionalTraits];
  const totalPicks = allTraits.length;
  const ptsRemaining = secondarySpecies.traitPointsBudget - secondarySpecies.traitPointsUsed;

  return (
    <div className="flex items-start justify-between gap-4 py-2">
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
          {secondarySpecies.enforcedTraits.map((trait) => (
            <Badge key={trait.id} variant="outline" className="border-primary/40 flex items-center gap-1">
              <EntityIcon category="traits" id={trait.id} size={14} />
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
              <EntityIcon category="traits" id={trait.id} size={14} />
              <span className={traitColor(trait.cost)}>
                {displayName(trait)}
              </span>
              <span className={`ml-1 text-xs ${traitColor(trait.cost)}`}>
                {trait.cost > 0 ? `+${trait.cost}` : trait.cost}
              </span>
            </Badge>
          ))}
        </div>
        <span className="text-xs text-muted-foreground">
          {totalPicks}/{secondarySpecies.maxTraitPicks} picks · {ptsRemaining} pts remaining
        </span>
      </div>
      <RerollButton category="secondaryspecies" available={rerollAvailable} />
    </div>
  );
}
