import { CheckCircle, Loader2, Plus } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { EmpireSlot } from "@/components/EmpireSlot";
import { EntityIcon } from "@/components/EntityIcon";
import { EthicsSlot } from "@/components/EthicsSlot";
import { RerollButton } from "@/components/RerollButton";
import { SecondarySpeciesSlot } from "@/components/SecondarySpeciesSlot";
import { TraitsSlot } from "@/components/TraitsSlot";
import { displayName, humanizeId } from "@/lib/format";
import { useEmpireStore } from "@/stores/useEmpireStore";
import type { EmpireResponse } from "@/types/empire";

interface EmpireCardProps {
  empire: EmpireResponse;
}

export function EmpireCard({ empire }: EmpireCardProps) {
  const leaderClassName = humanizeId(empire.leader.leaderClass);

  const addLeaderTrait = useEmpireStore((s) => s.addLeaderTrait);
  const finalizeLeaderTraits = useEmpireStore((s) => s.finalizeLeaderTraits);
  const isAddingLeaderTrait = useEmpireStore((s) => s.isAddingLeaderTrait);
  const isRerolling = useEmpireStore((s) => s.isRerolling);
  const isLoading = useEmpireStore((s) => s.isLoading);
  const isAddingTrait = useEmpireStore((s) => s.isAddingTrait);
  const leaderTraitsFinalized = useEmpireStore((s) => s.leaderTraitsFinalized);

  const isLuminary = empire.origin.id === "origin_legendary_leader";
  const leaderBudgetUsed = empire.leader.traits.reduce((sum, t) => sum + t.cost, 0);
  const leaderBudgetRemaining = empire.leader.leaderBudget - leaderBudgetUsed;
  const leaderPicksRemaining = empire.leader.leaderPicksMax - empire.leader.traits.length;
  const anyBusy = isRerolling !== null || isLoading || isAddingTrait || isAddingLeaderTrait;
  const canAddLeaderTrait = isLuminary && leaderPicksRemaining > 0 && !anyBusy;
  const canFinalizeLeader = isLuminary && !leaderTraitsFinalized && leaderBudgetRemaining >= 0 && leaderPicksRemaining >= 0 && !anyBusy;

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
          rerollAvailable={empire.rerollsAvailable["ethics"] ?? false}
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

        <div className="flex items-center justify-between gap-4 py-2 border-b border-border">
          <div className="flex flex-col gap-0.5 min-w-0">
            <span className="text-xs uppercase tracking-wider text-muted-foreground">
              Habitability Preference
            </span>
            <span className="text-foreground font-medium truncate flex items-center gap-1.5">
              <EntityIcon category="planets" id={empire.habitabilityPreference.id} size={36} />
              {displayName(empire.habitabilityPreference)}
            </span>
            {empire.habitabilityPreference.id !== empire.homeworld.id && (
              <span className="text-xs text-muted-foreground">
                {empire.habitabilityPreference.climate === "fixed" ? "Fixed by origin" : `${empire.habitabilityPreference.climate} climate`}
              </span>
            )}
          </div>
        </div>

        <EmpireSlot
          label="Shipset"
          value={empire.shipsetName ?? humanizeId(empire.shipset)}
          category="shipset"
          rerollAvailable={empire.rerollsAvailable["shipset"] ?? false}
        />

        <div className="flex flex-col gap-2 py-2 border-b border-border last:border-b-0">
          <div className="flex items-start justify-between gap-4">
            <div className="flex flex-col gap-1 min-w-0">
              <span className="text-xs uppercase tracking-wider text-muted-foreground">
                Starting Leader
              </span>
              <span className="text-foreground font-medium">{leaderClassName}</span>
              {empire.leader.traits.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                  {empire.leader.traits.map((trait) => (
                    <Badge key={trait.id} className="flex items-center gap-1 bg-emerald-900/50 text-emerald-100 border border-emerald-700/40 hover:bg-emerald-900/60">
                      <EntityIcon category="leadertraits" id={trait.id} size={24} />
                      <span>{trait.displayName ?? humanizeId(trait.id)}</span>
                    </Badge>
                  ))}
                </div>
              )}
              {isLuminary && (
                <span className="text-xs text-muted-foreground">
                  {empire.leader.traits.length}/{empire.leader.leaderPicksMax} traits · {leaderBudgetRemaining} budget remaining
                </span>
              )}
            </div>
            <RerollButton category="leader" available={empire.rerollsAvailable["leader"] ?? false} />
          </div>
          {isLuminary && (
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={addLeaderTrait}
                disabled={!canAddLeaderTrait}
                className="gap-1.5 text-xs"
              >
                {isAddingLeaderTrait ? (
                  <Loader2 className="h-3 w-3 animate-spin" />
                ) : (
                  <Plus className="h-3 w-3" />
                )}
                Roll Leader Trait
              </Button>
              {!leaderTraitsFinalized && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={finalizeLeaderTraits}
                  disabled={!canFinalizeLeader}
                  className="gap-1.5 text-xs text-muted-foreground hover:text-foreground"
                >
                  <CheckCircle className="h-3 w-3" />
                  Done Rolling
                </Button>
              )}
              {leaderTraitsFinalized && (
                <span className="text-xs text-muted-foreground flex items-center gap-1">
                  <CheckCircle className="h-3 w-3 text-green-500" />
                  Traits finalized
                </span>
              )}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
