import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { EmpireSlot } from "@/components/EmpireSlot";
import { EntityIcon } from "@/components/EntityIcon";
import { EthicsSlot } from "@/components/EthicsSlot";
import { RerollButton } from "@/components/RerollButton";
import { SecondarySpeciesSlot } from "@/components/SecondarySpeciesSlot";
import { TraitsSlot } from "@/components/TraitsSlot";
import { displayName, humanizeId } from "@/lib/format";
import type { EmpireResponse } from "@/types/empire";

interface EmpireCardProps {
  empire: EmpireResponse;
}

export function EmpireCard({ empire }: EmpireCardProps) {
  const leaderClassName = humanizeId(empire.leader.leaderClass);

  return (
    <Card className="w-full max-w-2xl animate-empire-enter">
      <CardContent className="flex flex-col gap-4 p-6">
        <EthicsSlot
          ethics={empire.ethics}
          rerollAvailable={empire.rerollsAvailable["ethics"] ?? false}
        />

        <EmpireSlot
          label="Authority"
          value={displayName(empire.authority)}
          sublabel={empire.authority.isGestalt ? "Gestalt Consciousness" : undefined}
          category="authority"
          rerollAvailable={empire.rerollsAvailable["authority"] ?? false}
          iconCategory="authorities"
          iconId={empire.authority.id}
        />

        {empire.civics.map((civic, i) => (
          <EmpireSlot
            key={civic.id}
            label={`Civic ${i + 1}`}
            value={displayName(civic)}
            category={i === 0 ? "civic1" : "civic2"}
            rerollAvailable={
              empire.rerollsAvailable[i === 0 ? "civic1" : "civic2"] ?? false
            }
            iconCategory="civics"
            iconId={civic.id}
          />
        ))}

        <EmpireSlot
          label="Origin"
          value={displayName(empire.origin)}
          sublabel={empire.origin.dlcRequirement ? `Requires ${empire.origin.dlcRequirement} DLC` : undefined}
          category="origin"
          rerollAvailable={empire.rerollsAvailable["origin"] ?? false}
          iconCategory="origins"
          iconId={empire.origin.id}
        />

        <TraitsSlot
          archetype={empire.speciesArchetype}
          speciesClass={empire.speciesClass}
          speciesClassName={empire.speciesClassName}
          traits={empire.speciesTraits}
          pointsUsed={empire.traitPointsUsed}
          pointsBudget={empire.traitPointsBudget}
          rerollAvailable={empire.rerollsAvailable["traits"] ?? false}
        />

        {empire.secondarySpecies && (
          <SecondarySpeciesSlot
            secondarySpecies={empire.secondarySpecies}
            rerollAvailable={empire.rerollsAvailable["secondaryspecies"] ?? false}
          />
        )}

        <EmpireSlot
          label="Homeworld"
          value={displayName(empire.homeworld)}
          sublabel={empire.homeworld.climate !== "fixed" ? `${empire.homeworld.climate} climate` : "Fixed by origin"}
          category="homeworld"
          rerollAvailable={empire.homeworld.climate !== "fixed" && (empire.rerollsAvailable["homeworld"] ?? false)}
          iconCategory="planets"
          iconId={empire.homeworld.id}
        />

        <EmpireSlot
          label="Shipset"
          value={empire.shipsetName ?? humanizeId(empire.shipset)}
          category="shipset"
          rerollAvailable={empire.rerollsAvailable["shipset"] ?? false}
        />

        <div className="flex items-start justify-between gap-4 py-2 border-b border-border last:border-b-0">
          <div className="flex flex-col gap-1 min-w-0">
            <span className="text-xs uppercase tracking-wider text-muted-foreground">
              Starting Leader
            </span>
            <span className="text-foreground font-medium">{leaderClassName}</span>
            {empire.leader.traits.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {empire.leader.traits.map((trait) => (
                  <Badge key={trait.id} variant="secondary" className="flex items-center gap-1">
                    <EntityIcon category="leadertraits" id={trait.id} size={14} />
                    <span>{trait.displayName ?? humanizeId(trait.id)}</span>
                  </Badge>
                ))}
              </div>
            )}
          </div>
          <RerollButton category="leader" available={empire.rerollsAvailable["leader"] ?? false} />
        </div>
      </CardContent>
    </Card>
  );
}
