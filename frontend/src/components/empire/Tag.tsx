import type { ReactNode } from "react";

interface TagProps {
  children: ReactNode;
  color?: string;
}

export function Tag({ children, color = "#4fc3f7" }: TagProps) {
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        padding: "1px 5px",
        fontFamily: "JetBrains Mono, monospace",
        fontSize: 9.5,
        fontWeight: 600,
        letterSpacing: 0.4,
        color,
        background: `${color}15`,
        border: `1px solid ${color}40`,
        borderRadius: 2,
      }}
    >
      {children}
    </span>
  );
}
