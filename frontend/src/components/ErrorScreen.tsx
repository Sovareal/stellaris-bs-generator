interface ErrorScreenProps {
  message: string;
  title?: string;
}

export function ErrorScreen({ message, title = "Connection Error" }: ErrorScreenProps) {
  return (
    <div className="flex-1 flex items-center justify-center">
      <div className="bg-card border border-border rounded-lg p-8 max-w-md text-center">
        <p className="text-destructive text-lg font-semibold mb-2">
          {title}
        </p>
        <p className="text-muted-foreground text-sm">{message}</p>
      </div>
    </div>
  );
}
