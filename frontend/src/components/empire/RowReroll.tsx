import { Dices, Loader2 } from "lucide-react";

interface RowRerollProps {
  available: boolean;
  onClick: () => void;
  loading?: boolean;
  title?: string;
}

export function RowReroll({ available, onClick, loading = false, title }: RowRerollProps) {
  const active = available && !loading;
  return (
    <button
      onClick={active ? onClick : undefined}
      disabled={!active}
      title={title ?? (available ? "Reroll" : "Reroll used")}
      aria-label={title ?? "Reroll"}
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        width: 22,
        height: 22,
        padding: 0,
        color: active ? "#4fc3f7" : "#3a4866",
        background: active ? "rgba(79,195,247,0.10)" : "transparent",
        border: `1px solid ${active ? "rgba(79,195,247,0.45)" : "#243151"}`,
        borderRadius: 3,
        cursor: active ? "pointer" : "not-allowed",
        opacity: available ? 1 : 0.4,
        transition: "all 0.15s",
        flexShrink: 0,
      }}
    >
      {loading ? (
        <Loader2 size={11} style={{ animation: "spin 1s linear infinite" }} />
      ) : (
        <Dices size={11} />
      )}
    </button>
  );
}
