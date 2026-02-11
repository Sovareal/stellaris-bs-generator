import { TooltipProvider } from "@/components/ui/tooltip";
import { useBackendReady } from "@/hooks/useBackendReady";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { LoadingScreen } from "@/components/LoadingScreen";
import { ErrorScreen } from "@/components/ErrorScreen";
import { EmpireView } from "@/components/EmpireView";

function App() {
  const backend = useBackendReady();

  return (
    <TooltipProvider>
      <div className="min-h-screen bg-background text-foreground flex flex-col">
        <Header gameVersion={backend.gameVersion} />

        {!backend.ready && !backend.error && <LoadingScreen />}
        {backend.error && <ErrorScreen message={backend.error} />}
        {backend.ready && <EmpireView />}

        <Footer connected={backend.ready} appVersion={backend.version ?? "0.1.0"} />
      </div>
    </TooltipProvider>
  );
}

export default App;
