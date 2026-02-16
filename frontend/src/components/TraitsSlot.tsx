import { Badge } from "@/components/ui/badge";
import { RerollButton } from "@/components/RerollButton";
import { displayName, humanizeId } from "@/lib/format";
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
          {traits.map((trait) => (
            <Badge key={trait.id} variant="secondary">
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
          {traits.length}/{archetype.maxTraits} picks · {pointsBudget - pointsUsed} pts remaining
        </span>
      </div>
      <RerollButton category="traits" available={rerollAvailable} />
    </div>
  );
}
