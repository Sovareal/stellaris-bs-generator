import { Info } from "lucide-react";
import { EntityIcon } from "@/components/EntityIcon";
import { RerollButton } from "@/components/RerollButton";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import type { RerollCategory } from "@/types/empire";

interface ModifierEntry {
  name: string;
  value: string;
  positive: boolean;
}

interface CivicEffects {
  description: string | null;
  modifiers: ModifierEntry[];
}

interface EmpireSlotProps {
  label: string;
  value: string;
  sublabel?: string;
  category: RerollCategory;
  rerollAvailable: boolean;
  iconCategory?: string;
  iconId?: string;
  effects?: CivicEffects;
}

export function EmpireSlot({ label, value, sublabel, category, rerollAvailable, iconCategory, iconId, effects }: EmpireSlotProps) {
  const hasEffects = effects && (effects.description || effects.modifiers.length > 0);

  return (
    <div className="flex items-center justify-between gap-4 py-2 border-b border-border last:border-b-0">
      <div className="flex flex-col gap-0.5 min-w-0">
        <span className="text-xs uppercase tracking-wider text-muted-foreground">
          {label}
        </span>
        <span className="text-foreground font-medium truncate flex items-center gap-1.5">
          {iconCategory && iconId && (
            <EntityIcon category={iconCategory} id={iconId} size={36} />
          )}
          {value}
        </span>
        {sublabel && (
          <span className="text-xs text-muted-foreground">{sublabel}</span>
        )}
      </div>
      <div className="flex items-center gap-1.5 shrink-0">
        {hasEffects && (
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Info className="h-3.5 w-3.5 text-muted-foreground hover:text-foreground cursor-pointer" />
              </TooltipTrigger>
              <TooltipContent className="max-w-64 text-xs" side="left">
                {effects!.description && (
                  <p className="mb-1.5">{effects!.description}</p>
                )}
                {effects!.modifiers.length > 0 && (
                  <ul className="space-y-0.5">
                    {effects!.modifiers.map((mod, i) => (
                      <li key={i} className={mod.positive ? "text-green-400" : "text-destructive"}>
                        {mod.value} {mod.name}
                      </li>
                    ))}
                  </ul>
                )}
              </TooltipContent>
            </Tooltip>
          </TooltipProvider>
        )}
        <RerollButton category={category} available={rerollAvailable} />
      </div>
    </div>
  );
}
