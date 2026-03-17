import { Settings } from "lucide-react";
import { Button } from "@/components/ui/button";

interface HeaderProps {
  gameVersion: string | null;
  onSettingsClick?: () => void;
}

export function Header({ gameVersion, onSettingsClick }: HeaderProps) {
  return (
    <header className="border-b border-border px-6 py-4 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <img src="/favicon.png" alt="" width={28} height={28} className="flex-shrink-0" />
        <h1 className="text-xl font-bold tracking-wide text-primary">
          Stellaris BS Empire Generator
        </h1>
      </div>
      <div className="flex items-center gap-3">
        {gameVersion && (
          <span className="text-sm text-muted-foreground">
            Stellaris {gameVersion}
          </span>
        )}
        {onSettingsClick && (
          <Button variant="ghost" size="icon" onClick={onSettingsClick} title="Settings">
            <Settings className="size-4" />
          </Button>
        )}
      </div>
    </header>
  );
}
