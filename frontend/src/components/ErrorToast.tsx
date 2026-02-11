import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useEmpireStore } from "@/stores/useEmpireStore";

export function ErrorToast() {
  const error = useEmpireStore((s) => s.error);
  const clearError = useEmpireStore((s) => s.clearError);

  if (!error) return null;

  return (
    <div className="w-full max-w-2xl bg-destructive/10 border border-destructive/30 rounded-lg px-4 py-3 flex items-center justify-between gap-3">
      <p className="text-destructive text-sm">{error}</p>
      <Button
        variant="ghost"
        size="icon"
        onClick={clearError}
        className="shrink-0 text-destructive hover:text-destructive/80 h-6 w-6"
      >
        <X className="h-4 w-4" />
      </Button>
    </div>
  );
}
