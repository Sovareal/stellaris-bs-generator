import { useState } from "react";
import { iconUrl } from "@/lib/icons";

interface EntityIconProps {
  category: string;
  id: string;
  size?: number;
  className?: string;
}

export function EntityIcon({ category, id, size = 16, className = "" }: EntityIconProps) {
  const [hidden, setHidden] = useState(false);

  if (hidden) return null;

  return (
    <img
      src={iconUrl(category, id)}
      alt=""
      width={size}
      height={size}
      className={`inline-block flex-shrink-0 ${className}`}
      onError={() => setHidden(true)}
    />
  );
}
