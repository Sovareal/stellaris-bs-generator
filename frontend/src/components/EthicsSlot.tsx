import { Badge } from "@/components/ui/badge";
import { EntityIcon } from "@/components/EntityIcon";
import { RerollButton } from "@/components/RerollButton";
import { displayName } from "@/lib/format";
import type { EthicDto } from "@/types/empire";

interface EthicsSlotProps {
  ethics: EthicDto[];
  rerollAvailable: boolean;
}

export function EthicsSlot({ ethics, rerollAvailable }: EthicsSlotProps) {
  return (
    <div className="flex items-center justify-between gap-4 py-2 border-b border-border">
      <div className="flex flex-col gap-1.5 min-w-0">
        <span className="text-xs uppercase tracking-wider text-muted-foreground">
          Ethics
        </span>
        <div className="flex flex-wrap gap-1.5">
          {ethics.map((ethic) => (
            <Badge
              key={ethic.id}
              variant={ethic.isFanatic ? "default" : "secondary"}
              className={`${ethic.isFanatic ? "bg-primary/20 text-primary border-primary/30" : ""} flex items-center gap-1`}
            >
              <EntityIcon category="ethics" id={ethic.id} size={16} />
              {ethic.isFanatic && (
                <span className="text-primary font-semibold">Fanatic</span>
              )}
              {displayName(ethic)}
            </Badge>
          ))}
        </div>
      </div>
      <RerollButton category="ethics" available={rerollAvailable} />
    </div>
  );
}
