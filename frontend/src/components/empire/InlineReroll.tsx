import { Dices, Loader2 } from "lucide-react";

interface InlineRerollProps {
  available: boolean;
  onClick: () => void;
  loading?: boolean;
  label?: string;
  title?: string;
}

export function InlineReroll({
  available,
  onClick,
  loading = false,
  label = "REROLL",
  title,
}: InlineRerollProps) {
  const active = available && !loading;
  return (
    <button
      onClick={active ? onClick : undefined}
      disabled={!active}
      title={title ?? (available ? label : "Reroll used")}
      aria-label={title ?? label}
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 5,
        padding: "3px 9px",
        fontFamily: "JetBrains Mono, monospace",
        fontSize: 12,
        fontWeight: 700,
        letterSpacing: 1,
        color: active ? "#4fc3f7" : "#3a4866",
        background: active ? "rgba(79,195,247,0.10)" : "transparent",
        border: `1px solid ${active ? "rgba(79,195,247,0.45)" : "#243151"}`,
        borderRadius: 3,
        cursor: active ? "pointer" : "not-allowed",
        opacity: available ? 1 : 0.45,
        transition: "all 0.15s",
      }}
    >
      {loading ? (
        <Loader2 size={13} style={{ animation: "spin 1s linear infinite" }} />
      ) : (
        <Dices size={13} />
      )}
      {label}
    </button>
  );
}
