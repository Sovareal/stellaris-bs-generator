import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { api, ApiError } from "@/lib/api";
import type { DlcInfo } from "@/types/empire";
import { FolderOpen, Loader2, CheckCircle, XCircle } from "lucide-react";

interface SettingsPageProps {
  onSaved: () => void;
  onClose?: () => void;
  errorMessage?: string | null;
}

/** Group DlcInfo[] by category, preserving insertion order. */
function groupByCategory(dlcs: DlcInfo[]): [string, DlcInfo[]][] {
  const map = new Map<string, DlcInfo[]>();
  for (const dlc of dlcs) {
    if (!map.has(dlc.category)) map.set(dlc.category, []);
    map.get(dlc.category)!.push(dlc);
  }
  return Array.from(map.entries());
}

function dlcsKey(set: Set<string>): string {
  return [...set].sort().join(",");
}

export function SettingsPage({ onSaved, onClose, errorMessage }: SettingsPageProps) {
  const [gamePath, setGamePath]           = useState("");
  const [saving, setSaving]               = useState(false);
  const [validation, setValidation]       = useState<{ valid: boolean; message: string } | null>(null);
  const [availableDlcs, setAvailableDlcs] = useState<DlcInfo[]>([]);
  const [disabledDlcs, setDisabledDlcs]   = useState<Set<string>>(new Set());

  const [originalGamePath, setOriginalGamePath] = useState("");
  const [originalDlcsKey, setOriginalDlcsKey]   = useState("");
  const [corrupted, setCorrupted]               = useState(false);
  const [resetting, setResetting]               = useState(false);

  useEffect(() => {
    api
      .getSettings()
      .then((settings) => {
        setGamePath(settings.gamePath);
        setAvailableDlcs(settings.availableDlcs ?? []);
        const loadedDlcs = new Set<string>(settings.disabledDlcs ?? []);
        setDisabledDlcs(loadedDlcs);
        setOriginalGamePath(settings.gamePath);
        setOriginalDlcsKey(dlcsKey(loadedDlcs));
        if (settings.validationMessage) {
          setValidation({ valid: settings.valid, message: settings.validationMessage });
        }
      })
      .catch((e) => {
        if (e instanceof ApiError && e.body.error === "settings_corrupted") {
          setCorrupted(true);
        }
      });
  }, []);

  function toggleDlc(name: string, enabled: boolean) {
    setDisabledDlcs((prev) => {
      const next = new Set(prev);
      if (enabled) {
        next.delete(name);
      } else {
        next.add(name);
      }
      return next;
    });
  }

  async function handleSave() {
    setSaving(true);
    setValidation(null);
    try {
      const result = await api.saveSettings(gamePath, Array.from(disabledDlcs));
      setValidation({ valid: result.valid, message: result.validationMessage });
      if (result.valid) {
        setTimeout(onSaved, 500);
      }
    } catch {
      setValidation({
        valid: false,
        message: "Failed to save settings. Is the backend running?",
      });
    } finally {
      setSaving(false);
    }
  }

  const hasChanges = gamePath !== originalGamePath || dlcsKey(disabledDlcs) !== originalDlcsKey;
  const categories = groupByCategory(availableDlcs);

  return (
    <div className="flex-1 flex items-center justify-center py-8">
      <div className="bg-card border border-border rounded-lg p-8 max-w-lg w-full space-y-6">

        {/* Game Path */}
        <div>
          <div className="flex items-center gap-2 mb-4">
            <FolderOpen className="size-5 text-primary" />
            <h2 className="text-lg font-semibold">Game Path Setup</h2>
          </div>

          {errorMessage && (
            <div className="mb-4 p-3 rounded-md bg-destructive/10 border border-destructive/20">
              <p className="text-sm text-destructive">{errorMessage}</p>
            </div>
          )}

          {corrupted && (
            <div className="mb-4 p-3 rounded-md bg-destructive/10 border border-destructive/20 flex items-center justify-between gap-3">
              <p className="text-sm text-destructive">
                Settings file is corrupted and cannot be read.
              </p>
              <Button
                variant="destructive"
                size="sm"
                disabled={resetting}
                onClick={async () => {
                  setResetting(true);
                  try {
                    await api.resetSettings();
                    window.location.reload();
                  } catch {
                    setResetting(false);
                  }
                }}
              >
                {resetting && <Loader2 className="size-3 animate-spin" />}
                Reset to Defaults
              </Button>
            </div>
          )}

          <p className="text-sm text-muted-foreground mb-4">
            Enter the path to your Stellaris installation directory.
          </p>

          <input
            id="game-path"
            type="text"
            value={gamePath}
            onChange={(e) => {
              setGamePath(e.target.value);
              setValidation(null);
            }}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !saving) handleSave();
            }}
            placeholder="C:\Program Files (x86)\Steam\steamapps\common\Stellaris"
            className="w-full h-9 px-3 rounded-md border border-input bg-background text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          />

          <p className="text-xs text-muted-foreground mt-2">
            Windows: C:\Program Files (x86)\Steam\steamapps\common\Stellaris
            <br />
            Linux: ~/.steam/steam/steamapps/common/Stellaris
          </p>
        </div>

        {/* DLC Filter */}
        {availableDlcs.length > 0 && (
          <div>
            <h2 className="text-base font-semibold mb-1">DLC Content Filter</h2>
            <p className="text-xs text-muted-foreground mb-3">
              By default all DLC content is included. Uncheck DLCs you don't own
              to exclude their species, origins, and civics from generation.
            </p>

            <div className="space-y-4">
              {categories.map(([category, dlcs]) => (
                <div key={category}>
                  <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider mb-2">
                    {category}
                  </p>
                  <div className="grid grid-cols-2 gap-1.5">
                    {dlcs.map((dlc) => {
                      const enabled = !disabledDlcs.has(dlc.name);
                      return (
                        <label
                          key={dlc.name}
                          className="flex items-center gap-2 text-sm cursor-pointer select-none"
                        >
                          <input
                            type="checkbox"
                            checked={enabled}
                            onChange={(e) => toggleDlc(dlc.name, e.target.checked)}
                            className="rounded border-input accent-primary"
                          />
                          <span className={enabled ? "" : "text-muted-foreground line-through"}>
                            {dlc.name}
                          </span>
                        </label>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="space-y-3">
          {validation && (
            <div
              className={`flex items-center gap-2 text-sm ${
                validation.valid ? "text-green-400" : "text-destructive"
              }`}
            >
              {validation.valid ? (
                <CheckCircle className="size-4 shrink-0" />
              ) : (
                <XCircle className="size-4 shrink-0" />
              )}
              <span>{validation.message}</span>
            </div>
          )}

          <div className={onClose ? "flex gap-3" : ""}>
            {onClose && (
              <Button
                variant="outline"
                onClick={onClose}
                disabled={saving}
                className="flex-1"
              >
                {hasChanges ? "Cancel" : "Close"}
              </Button>
            )}
            <Button
              onClick={handleSave}
              disabled={saving || !gamePath.trim()}
              className={onClose ? "flex-1" : "w-full"}
            >
              {saving && <Loader2 className="size-4 animate-spin" />}
              {saving ? "Saving & Reloading..." : "Save & Reload"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
