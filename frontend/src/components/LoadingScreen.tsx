export function LoadingScreen() {
  return (
    <div className="flex-1 flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <div className="w-10 h-10 border-2 border-primary border-t-transparent rounded-full animate-spin" />
        <p className="text-muted-foreground text-lg">
          Waiting for backend&hellip;
        </p>
      </div>
    </div>
  );
}
