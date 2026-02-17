import { EntityIcon } from "@/components/EntityIcon";
import { RerollButton } from "@/components/RerollButton";
import type { RerollCategory } from "@/types/empire";

interface EmpireSlotProps {
  label: string;
  value: string;
  sublabel?: string;
  category: RerollCategory;
  rerollAvailable: boolean;
  iconCategory?: string;
  iconId?: string;
}

export function EmpireSlot({ label, value, sublabel, category, rerollAvailable, iconCategory, iconId }: EmpireSlotProps) {
  return (
    <div className="flex items-center justify-between gap-4 py-2 border-b border-border last:border-b-0">
      <div className="flex flex-col gap-0.5 min-w-0">
        <span className="text-xs uppercase tracking-wider text-muted-foreground">
          {label}
        </span>
        <span className="text-foreground font-medium truncate flex items-center gap-1.5">
          {iconCategory && iconId && (
            <EntityIcon category={iconCategory} id={iconId} size={18} />
          )}
          {value}
        </span>
        {sublabel && (
          <span className="text-xs text-muted-foreground">{sublabel}</span>
        )}
      </div>
      <RerollButton category={category} available={rerollAvailable} />
    </div>
  );
}
