interface HeaderProps {
  gameVersion: string | null;
}

export function Header({ gameVersion }: HeaderProps) {
  return (
    <header className="border-b border-border px-6 py-4 flex items-center justify-between">
      <h1 className="text-xl font-bold tracking-wide text-primary">
        Stellaris BS Empire Generator
      </h1>
      {gameVersion && (
        <span className="text-sm text-muted-foreground">
          Stellaris {gameVersion}
        </span>
      )}
    </header>
  );
}
