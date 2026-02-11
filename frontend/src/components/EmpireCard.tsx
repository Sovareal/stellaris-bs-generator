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
      </CardContent>
    </Card>
  );
}
