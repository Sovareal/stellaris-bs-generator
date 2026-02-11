import { Badge } from "@/components/ui/badge";
import { RerollButton } from "@/components/RerollButton";
import { humanizeId } from "@/lib/format";
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
              className={ethic.isFanatic ? "bg-primary/20 text-primary border-primary/30" : ""}
            >
              {ethic.isFanatic && (
                <span className="text-primary font-semibold mr-1">Fanatic</span>
              )}
              {humanizeId(ethic.id)}
            </Badge>
          ))}
        </div>
      </div>
      <RerollButton category="ethics" available={rerollAvailable} />
    </div>
  );
}
