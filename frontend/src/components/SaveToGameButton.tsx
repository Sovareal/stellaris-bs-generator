import { useState } from "react";
import { Save } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useEmpireStore } from "@/stores/useEmpireStore";
import { ExportModal } from "@/components/ExportModal";

export function SaveToGameButton() {
  const empire = useEmpireStore((s) => s.empire);
  const traitsFinalized = useEmpireStore((s) => s.traitsFinalized);
  const secondaryTraitsFinalized = useEmpireStore((s) => s.secondaryTraitsFinalized);
  const leaderTraitsFinalized = useEmpireStore((s) => s.leaderTraitsFinalized);
  const [open, setOpen] = useState(false);

  const secondaryReady = !empire?.secondarySpecies || secondaryTraitsFinalized;
  const isLuminary = empire?.origin?.id === "origin_legendary_leader";
  const leaderReady = !isLuminary || leaderTraitsFinalized;
  if (!empire || !traitsFinalized || !secondaryReady || !leaderReady) return null;

  return (
    <>
      <Button
        variant="outline"
        size="lg"
        onClick={() => setOpen(true)}
        className="gap-2"
      >
        <Save className="h-4 w-4" />
        Save to Game
      </Button>
      <ExportModal open={open} onClose={() => setOpen(false)} />
    </>
  );
}
