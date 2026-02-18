interface HeaderProps {
  gameVersion: string | null;
}

export function Header({ gameVersion }: HeaderProps) {
  return (
    <header className="border-b border-border px-6 py-4 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <img src="/favicon.png" alt="" width={28} height={28} className="flex-shrink-0" />
        <h1 className="text-xl font-bold tracking-wide text-primary">
          Stellaris BS Empire Generator
        </h1>
      </div>
      {gameVersion && (
        <span className="text-sm text-muted-foreground">
          Stellaris {gameVersion}
        </span>
      )}
    </header>
  );
}
