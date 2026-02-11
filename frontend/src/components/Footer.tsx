interface FooterProps {
  connected: boolean;
  appVersion: string;
}

export function Footer({ connected, appVersion }: FooterProps) {
  return (
    <footer className="border-t border-border px-6 py-3 text-center text-sm text-muted-foreground">
      {connected
        ? `Backend connected \u00b7 v${appVersion}`
        : "Backend: connecting\u2026"}
    </footer>
  );
}
