import type { ReactNode } from "react";
import { Loader2 } from "lucide-react";

type Variant = "primary" | "finalize" | "destructive";

interface ConsoleButtonProps {
  variant: Variant;
  onClick: () => void;
  icon: ReactNode;
  children: ReactNode;
  pulse?: boolean;
  loading?: boolean;
  disabled?: boolean;
}

const STYLES: Record<Variant, React.CSSProperties> = {
  primary: {
    color: "#0a0e17",
    background: "linear-gradient(180deg, #6dd0fb, #4fc3f7)",
    border: "1px solid #7fd6fc",
  },
  finalize: {
    color: "#86efac",
    background: "linear-gradient(180deg, rgba(34,197,94,0.18), rgba(34,197,94,0.08))",
    border: "1px solid rgba(34,197,94,0.55)",
    boxShadow: "0 0 14px rgba(34,197,94,0.30)",
  },
  destructive: {
    color: "#fca5a5",
    background: "linear-gradient(180deg, rgba(239,68,68,0.18), rgba(239,68,68,0.08))",
    border: "1px solid rgba(239,68,68,0.55)",
    boxShadow: "0 0 14px rgba(239,68,68,0.25)",
  },
};

export function ConsoleButton({
  variant,
  onClick,
  icon,
  children,
  pulse = false,
  loading = false,
  disabled = false,
}: ConsoleButtonProps) {
  const isDisabled = disabled || loading;
  const shouldPulse = pulse && !isDisabled && variant === "primary";

  return (
    <button
      onClick={isDisabled ? undefined : onClick}
      disabled={isDisabled}
      aria-label={typeof children === "string" ? children : undefined}
      style={{
        flex: 1,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        gap: 6,
        padding: "7px 12px",
        fontFamily: "JetBrains Mono, monospace",
        fontSize: 11,
        fontWeight: 700,
        letterSpacing: 1.2,
        borderRadius: 3,
        cursor: isDisabled ? "not-allowed" : "pointer",
        opacity: isDisabled ? 0.45 : 1,
        transition: "opacity 0.15s",
        animation: shouldPulse ? "telemetryAttn 1.8s ease-in-out infinite" : "none",
        ...STYLES[variant],
      }}
    >
      {loading ? <Loader2 size={12} style={{ animation: "spin 1s linear infinite" }} /> : icon}
      {children}
    </button>
  );
}
