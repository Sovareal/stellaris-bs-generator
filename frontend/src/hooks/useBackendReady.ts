import { useState, useEffect, useRef } from "react";
import { api } from "@/lib/api";

const BACKEND_URL = "http://localhost:8080";
const POLL_INTERVAL_MS = 1000;
const MAX_RETRIES = 30;

interface BackendState {
  ready: boolean;
  error: string | null;
  needsSetup: boolean;
  version: string | null;
  gameVersion: string | null;
}

interface HealthResponse {
  status: string;
  version: string;
  dataStatus: string;
  dataError: string | null;
}

export function useBackendReady(): BackendState {
  const [state, setState] = useState<BackendState>({
    ready: false,
    error: null,
    needsSetup: false,
    version: null,
    gameVersion: null,
  });
  const retriesRef = useRef(0);

  useEffect(() => {
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;

    async function poll() {
      if (cancelled) return;

      try {
        const res = await fetch(`${BACKEND_URL}/api/health`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data: HealthResponse = await res.json();

        if (data.dataStatus === "loading") {
          // Still loading — keep polling
          timer = setTimeout(poll, POLL_INTERVAL_MS);
          return;
        }

        if (data.dataStatus === "error") {
          // Data failed to load — needs setup
          if (!cancelled) {
            setState({
              ready: false,
              error: data.dataError,
              needsSetup: true,
              version: data.version,
              gameVersion: null,
            });
          }
          return;
        }

        // dataStatus === "ready"
        let gameVersion: string | null = null;
        try {
          const versionData = await api.getVersion();
          gameVersion = versionData.rawVersion;
        } catch {
          // Non-critical — game version display is optional
        }

        if (!cancelled) {
          setState({
            ready: true,
            error: null,
            needsSetup: false,
            version: data.version,
            gameVersion,
          });
        }
      } catch {
        retriesRef.current++;
        if (retriesRef.current >= MAX_RETRIES) {
          if (!cancelled) {
            setState({
              ready: false,
              error: "Backend not reachable after 30s. Is it running?",
              needsSetup: false,
              version: null,
              gameVersion: null,
            });
          }
        } else {
          timer = setTimeout(poll, POLL_INTERVAL_MS);
        }
      }
    }

    poll();

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, []);

  return state;
}
