import { useEmpireStore } from "@/stores/useEmpireStore";
import { GenerateButton } from "@/components/GenerateButton";
import { ErrorToast } from "@/components/ErrorToast";
import { EmpireCard } from "@/components/EmpireCard";

export function EmpireView() {
  const empire = useEmpireStore((s) => s.empire);
  const generationId = useEmpireStore((s) => s.generationId);

  return (
    <main className="flex-1 flex flex-col items-center justify-center gap-6 p-8">
      <ErrorToast />
      {empire ? (
        <EmpireCard key={generationId} empire={empire} />
      ) : (
        <p className="text-muted-foreground text-lg">
          Generate a random valid Stellaris empire
        </p>
      )}
      <GenerateButton />
    </main>
  );
}
