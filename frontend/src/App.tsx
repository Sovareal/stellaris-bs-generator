import { useBackendReady } from "./hooks/useBackendReady";

function App() {
  const backend = useBackendReady();

  return (
    <div className="min-h-screen bg-stellaris-bg text-stellaris-text flex flex-col">
      <header className="border-b border-stellaris-border px-6 py-4 flex items-center justify-between">
        <h1 className="text-xl font-bold tracking-wide text-stellaris-accent">
          Stellaris BS Empire Generator
        </h1>
        <span className="text-sm text-stellaris-muted">v0.1.0</span>
      </header>

      <main className="flex-1 flex items-center justify-center p-8">
        <div className="bg-stellaris-surface border border-stellaris-border rounded-lg p-8 max-w-2xl w-full text-center">
          {!backend.ready && !backend.error && (
            <div className="flex flex-col items-center gap-4">
              <div className="w-8 h-8 border-2 border-stellaris-accent border-t-transparent rounded-full animate-spin" />
              <p className="text-stellaris-muted text-lg">
                Waiting for backend...
              </p>
            </div>
          )}

          {backend.error && (
            <div className="text-red-400">
              <p className="text-lg font-semibold mb-2">Connection Error</p>
              <p className="text-sm">{backend.error}</p>
            </div>
          )}

          {backend.ready && (
            <>
              <p className="text-stellaris-muted text-lg mb-6">
                Generate a random valid Stellaris empire
              </p>
              <button className="bg-stellaris-accent text-stellaris-bg font-semibold px-6 py-3 rounded-md hover:opacity-90 transition-opacity cursor-pointer">
                Generate Empire
              </button>
            </>
          )}
        </div>
      </main>

      <footer className="border-t border-stellaris-border px-6 py-3 text-center text-sm text-stellaris-muted">
        {backend.ready
          ? `Backend connected (v${backend.version})`
          : "Backend: connecting..."}
      </footer>
    </div>
  );
}

export default App;
