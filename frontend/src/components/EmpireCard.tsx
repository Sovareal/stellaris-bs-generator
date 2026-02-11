import { Card, CardContent } from "@/components/ui/card";
import { EmpireSlot } from "@/components/EmpireSlot";
import { EthicsSlot } from "@/components/EthicsSlot";
import { TraitsSlot } from "@/components/TraitsSlot";
import { humanizeId } from "@/lib/format";
import type { EmpireResponse } from "@/types/empire";

interface EmpireCardProps {
  empire: EmpireResponse;
}

export function EmpireCard({ empire }: EmpireCardProps) {
  const leaderValue = empire.leader.traitId
    ? `${humanizeId(empire.leader.leaderClass)} — ${humanizeId(empire.leader.traitId)}`
    : humanizeId(empire.leader.leaderClass);

  return (
    <Card className="w-full max-w-2xl animate-empire-enter">
      <CardContent className="flex flex-col gap-4 p-6">
        <EthicsSlot
          ethics={empire.ethics}
          rerollAvailable={empire.rerollsAvailable["ethics"] ?? false}
        />

        <EmpireSlot
          label="Authority"
          value={humanizeId(empire.authority.id)}
          sublabel={empire.authority.isGestalt ? "Gestalt Consciousness" : undefined}
          category="authority"
          rerollAvailable={empire.rerollsAvailable["authority"] ?? false}
        />

        {empire.civics.map((civic, i) => (
          <EmpireSlot
            key={civic.id}
            label={`Civic ${i + 1}`}
            value={humanizeId(civic.id)}
            category={i === 0 ? "civic1" : "civic2"}
            rerollAvailable={
              empire.rerollsAvailable[i === 0 ? "civic1" : "civic2"] ?? false
            }
          />
        ))}

        <EmpireSlot
          label="Origin"
          value={humanizeId(empire.origin.id)}
          sublabel={empire.origin.dlcRequirement ?? undefined}
          category="origin"
          rerollAvailable={empire.rerollsAvailable["origin"] ?? false}
        />

        <TraitsSlot
          archetype={empire.speciesArchetype}
          traits={empire.speciesTraits}
          pointsUsed={empire.traitPointsUsed}
          pointsBudget={empire.traitPointsBudget}
          rerollAvailable={empire.rerollsAvailable["traits"] ?? false}
        />

        <EmpireSlot
          label="Homeworld"
          value={humanizeId(empire.homeworld.id)}
          sublabel={empire.homeworld.climate !== "fixed" ? `${empire.homeworld.climate} climate` : "Fixed by origin"}
          category="homeworld"
          rerollAvailable={empire.homeworld.climate !== "fixed" && (empire.rerollsAvailable["homeworld"] ?? false)}
        />

        <EmpireSlot
          label="Shipset"
          value={humanizeId(empire.shipset)}
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
