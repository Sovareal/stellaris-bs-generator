import { Sparkles, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useEmpireStore } from "@/stores/useEmpireStore";

export function GenerateButton() {
  const generate = useEmpireStore((s) => s.generate);
  const isLoading = useEmpireStore((s) => s.isLoading);
  const isRerolling = useEmpireStore((s) => s.isRerolling);
  const hasEmpire = useEmpireStore((s) => s.empire !== null);
  const disabled = isLoading || isRerolling !== null;

  return (
    <Button
      size="lg"
      onClick={generate}
      disabled={disabled}
      className="gap-2"
    >
      {isLoading ? (
        <Loader2 className="h-4 w-4 animate-spin" />
      ) : (
        <Sparkles className="h-4 w-4" />
      )}
      {hasEmpire ? "Generate New Empire" : "Generate Empire"}
    </Button>
  );
}
