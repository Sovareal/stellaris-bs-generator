import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";
import { FolderOpen, Loader2, CheckCircle, XCircle } from "lucide-react";

interface SettingsPageProps {
  onSaved: () => void;
  errorMessage?: string | null;
}

export function SettingsPage({ onSaved, errorMessage }: SettingsPageProps) {
  const [gamePath, setGamePath] = useState("");
  const [saving, setSaving] = useState(false);
  const [validation, setValidation] = useState<{
    valid: boolean;
    message: string;
  } | null>(null);

  useEffect(() => {
    api
      .getSettings()
      .then((settings) => {
        setGamePath(settings.gamePath);
        if (settings.validationMessage) {
          setValidation({
            valid: settings.valid,
            message: settings.validationMessage,
          });
        }
      })
      .catch(() => {
        // Settings endpoint may fail if backend just started
      });
  }, []);

  async function handleSave() {
    setSaving(true);
    setValidation(null);
    try {
      const result = await api.saveSettings(gamePath);
      setValidation({ valid: result.valid, message: result.validationMessage });
      if (result.valid) {
        // Brief delay so user sees the success message
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

  return (
    <div className="flex-1 flex items-center justify-center">
      <div className="bg-card border border-border rounded-lg p-8 max-w-lg w-full">
        <div className="flex items-center gap-2 mb-4">
          <FolderOpen className="size-5 text-primary" />
          <h2 className="text-lg font-semibold">Game Path Setup</h2>
        </div>

        {errorMessage && (
          <div className="mb-4 p-3 rounded-md bg-destructive/10 border border-destructive/20">
            <p className="text-sm text-destructive">{errorMessage}</p>
          </div>
        )}

        <p className="text-sm text-muted-foreground mb-4">
          Enter the path to your Stellaris installation directory. This is
          typically found in your Steam library.
        </p>

        <div className="space-y-4">
          <div>
            <label
              htmlFor="game-path"
              className="text-sm font-medium mb-1.5 block"
            >
              Stellaris Installation Path
            </label>
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
          </div>

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

          <Button
            onClick={handleSave}
            disabled={saving || !gamePath.trim()}
            className="w-full"
          >
            {saving && <Loader2 className="size-4 animate-spin" />}
            {saving ? "Saving & Reloading..." : "Save & Reload"}
          </Button>
        </div>

        <p className="text-xs text-muted-foreground mt-4">
          Example paths: <br />
          Windows: C:\Program Files (x86)\Steam\steamapps\common\Stellaris
          <br />
          Linux: ~/.steam/steam/steamapps/common/Stellaris
        </p>
      </div>
    </div>
  );
}
