import { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import { X, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useEmpireStore } from "@/stores/useEmpireStore";
import type { ExportRequest } from "@/types/empire";

interface ExportModalProps {
  open: boolean;
  onClose: () => void;
}

export function ExportModal({ open, onClose }: ExportModalProps) {
  const isSaving = useEmpireStore((s) => s.isSaving);
  const saveToGame = useEmpireStore((s) => s.saveToGame);
  const saveSuccess = useEmpireStore((s) => s.saveSuccess);
  const clearSaveState = useEmpireStore((s) => s.clearSaveState);

  const [empireName, setEmpireName] = useState("");
  const [speciesName, setSpeciesName] = useState("");
  const [speciesPlural, setSpeciesPlural] = useState("");
  const [speciesAdjective, setSpeciesAdjective] = useState("");
  const [rulerName, setRulerName] = useState("");

  // Close automatically on success
  useEffect(() => {
    if (saveSuccess && open) {
      onClose();
    }
  }, [saveSuccess, open, onClose]);

  // Reset form when modal opens and clear any stale save success state
  useEffect(() => {
    if (open) {
      clearSaveState();
      setEmpireName("");
      setSpeciesName("");
      setSpeciesPlural("");
      setSpeciesAdjective("");
      setRulerName("");
    }
  }, [open, clearSaveState]);

  if (!open) return null;

  const isValid = empireName.trim() && speciesName.trim() && rulerName.trim();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValid || isSaving) return;

    const req: ExportRequest = {
      empireName: empireName.trim(),
      speciesName: speciesName.trim(),
      rulerName: rulerName.trim(),
      ...(speciesPlural.trim() ? { speciesPlural: speciesPlural.trim() } : {}),
      ...(speciesAdjective.trim() ? { speciesAdjective: speciesAdjective.trim() } : {}),
    };

    await saveToGame(req);
  };

  const pluralPlaceholder = speciesName.trim() ? `auto: ${speciesName.trim()}s` : "auto: [name]s";
  const adjectivePlaceholder = speciesName.trim() ? `auto: ${speciesName.trim()}` : "auto: [name]";

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      role="dialog"
      aria-modal="true"
      aria-label="Save empire to game"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={!isSaving ? onClose : undefined}
      />

      {/* Modal panel */}
      <div className="relative z-10 w-full max-w-md mx-4 bg-card border border-border rounded-xl shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-border">
          <h2 className="text-lg font-semibold text-foreground">Save Empire to Game</h2>
          {!isSaving && (
            <Button
              variant="ghost"
              size="icon"
              onClick={onClose}
              className="h-8 w-8 text-muted-foreground hover:text-foreground"
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          <FormField
            label="Empire Name"
            required
            value={empireName}
            onChange={setEmpireName}
            placeholder="e.g. Galactic Commonwealth"
            disabled={isSaving}
          />
          <FormField
            label="Species Name"
            required
            value={speciesName}
            onChange={setSpeciesName}
            placeholder="e.g. Human"
            disabled={isSaving}
          />
          <FormField
            label="Species Plural"
            value={speciesPlural}
            onChange={setSpeciesPlural}
            placeholder={pluralPlaceholder}
            disabled={isSaving}
          />
          <FormField
            label="Species Adjective"
            value={speciesAdjective}
            onChange={setSpeciesAdjective}
            placeholder={adjectivePlaceholder}
            disabled={isSaving}
          />
          <FormField
            label="Ruler Name"
            required
            value={rulerName}
            onChange={setRulerName}
            placeholder="e.g. Grand Admiral Zhao"
            disabled={isSaving}
          />

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              className="flex-1"
              onClick={onClose}
              disabled={isSaving}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              className="flex-1 gap-2"
              disabled={!isValid || isSaving}
            >
              {isSaving ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Saving…
                </>
              ) : (
                "Save to Game"
              )}
            </Button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}

interface FormFieldProps {
  label: string;
  required?: boolean;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  disabled?: boolean;
}

function FormField({ label, required, value, onChange, placeholder, disabled }: FormFieldProps) {
  return (
    <div className="space-y-1.5">
      <label className="text-sm font-medium text-foreground">
        {label}
        {required && <span className="text-destructive ml-1">*</span>}
      </label>
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50 disabled:cursor-not-allowed"
      />
    </div>
  );
}
