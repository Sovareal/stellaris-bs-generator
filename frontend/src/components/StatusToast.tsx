import { X, CheckCircle2, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useEmpireStore } from "@/stores/useEmpireStore";

export function StatusToast() {
  const error = useEmpireStore((s) => s.error);
  const saveSuccess = useEmpireStore((s) => s.saveSuccess);
  const clearError = useEmpireStore((s) => s.clearError);
  const clearSaveState = useEmpireStore((s) => s.clearSaveState);

  if (error) {
    return (
      <div className="w-full max-w-2xl bg-destructive/10 border border-destructive/30 rounded-lg px-4 py-3 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <AlertCircle className="h-4 w-4 text-destructive shrink-0" />
          <p className="text-destructive text-sm">{error}</p>
        </div>
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

  if (saveSuccess) {
    const fileName = saveSuccess.split(/[\\/]/).pop() ?? saveSuccess;
    return (
      <div className="w-full max-w-2xl bg-green-900/20 border border-green-700/30 rounded-lg px-4 py-3 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 text-green-400 shrink-0" />
          <p className="text-green-400 text-sm">
            Empire saved to <span className="font-medium">{fileName}</span> — restart Stellaris to see it
          </p>
        </div>
        <Button
          variant="ghost"
          size="icon"
          onClick={clearSaveState}
          className="shrink-0 text-green-400 hover:text-green-300 h-6 w-6"
        >
          <X className="h-4 w-4" />
        </Button>
      </div>
    );
  }

  return null;
}
