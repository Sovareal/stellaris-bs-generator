import { useState } from "react";
import { iconUrl } from "@/lib/icons";

interface EntityIconProps {
  category: string;
  id: string;
  size?: number;
  className?: string;
}

export function EntityIcon({ category, id, size = 16, className = "" }: EntityIconProps) {
  const [errored, setErrored] = useState(false);

  if (errored) {
    return (
      <img
        src="/billy.png"
        alt=""
        width={size}
        height={size}
        className={`inline-block flex-shrink-0 ${className}`}
        title={id}
      />
    );
  }

  return (
    <img
      src={iconUrl(category, id)}
      alt=""
      width={size}
      height={size}
      className={`inline-block flex-shrink-0 ${className}`}
      onError={() => setErrored(true)}
    />
  );
}
