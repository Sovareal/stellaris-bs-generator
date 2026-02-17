import { Card, CardContent } from "@/components/ui/card";
import { EmpireSlot } from "@/components/EmpireSlot";
import { EthicsSlot } from "@/components/EthicsSlot";
import { SecondarySpeciesSlot } from "@/components/SecondarySpeciesSlot";
import { TraitsSlot } from "@/components/TraitsSlot";
import { displayName, humanizeId } from "@/lib/format";
import type { EmpireResponse } from "@/types/empire";

interface EmpireCardProps {
  empire: EmpireResponse;
}

export function EmpireCard({ empire }: EmpireCardProps) {
  const leaderClassName = humanizeId(empire.leader.leaderClass);
  const leaderTraitNames = empire.leader.traits
    .map((t) => t.displayName ?? humanizeId(t.id))
    .join(", ");
  const leaderValue = leaderTraitNames
    ? `${leaderClassName} — ${leaderTraitNames}`
    : leaderClassName;

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
          />
        ))}

        <EmpireSlot
          label="Origin"
          value={displayName(empire.origin)}
          sublabel={empire.origin.dlcRequirement ? `Requires ${empire.origin.dlcRequirement} DLC` : undefined}
          category="origin"
          rerollAvailable={empire.rerollsAvailable["origin"] ?? false}
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
        />

        <EmpireSlot
          label="Shipset"
          value={empire.shipsetName ?? humanizeId(empire.shipset)}
          category="shipset"
          rerollAvailable={empire.rerollsAvailable["shipset"] ?? false}
        />

        <EmpireSlot
          label="Starting Leader"
          value={leaderValue}
          category="leader"
          rerollAvailable={empire.rerollsAvailable["leader"] ?? false}
        />
      </CardContent>
    </Card>
  );
}
