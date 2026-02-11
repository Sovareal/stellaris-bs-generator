import { Dices, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useEmpireStore } from "@/stores/useEmpireStore";
import type { RerollCategory } from "@/types/empire";

interface RerollButtonProps {
  category: RerollCategory;
  available: boolean;
}

export function RerollButton({ category, available }: RerollButtonProps) {
  const reroll = useEmpireStore((s) => s.reroll);
  const isRerolling = useEmpireStore((s) => s.isRerolling);
  const isLoading = useEmpireStore((s) => s.isLoading);
  const isThisRerolling = isRerolling === category;
  const disabled = !available || isRerolling !== null || isLoading;

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          onClick={() => reroll(category)}
          disabled={disabled}
          className="shrink-0 text-muted-foreground hover:text-primary disabled:opacity-30"
        >
          {isThisRerolling ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Dices className="h-4 w-4" />
          )}
        </Button>
      </TooltipTrigger>
      <TooltipContent>
        {available ? `Reroll ${category}` : "Reroll used"}
      </TooltipContent>
    </Tooltip>
  );
}
