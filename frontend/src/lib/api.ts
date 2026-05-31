import type { EmpireResponse, ExportRequest, ExportResponse, RerollCategory, SettingsResponse, SuggestedNames, VersionResponse } from "@/types/empire";

// Port passed via URL query string ?p=PORT by the Tauri shell.
// Synchronously available at module load time. Falls back to 8080 in browser dev mode.
function readPortFromUrl(): number {
  if (typeof window === "undefined") return 8080;
  const p = new URLSearchParams(window.location.search).get("p");
  const n = p ? parseInt(p, 10) : NaN;
  return isNaN(n) ? 8080 : n;
}

export const backendPortPromise: Promise<number> = Promise.resolve(readPortFromUrl());

export class ApiError extends Error {
  status: number;
  body: { message: string; error?: string };

  constructor(status: number, body: { message: string; error?: string }) {
    super(body.message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const port = await backendPortPromise;
  const hasBody = options?.body !== undefined;
  const res = await fetch(`http://127.0.0.1:${port}${path}`, {
    ...options,
    headers: {
      ...(hasBody ? { "Content-Type": "application/json" } : {}),
      ...(options?.headers as Record<string, string> ?? {}),
    },
  });

  if (!res.ok) {
    let body: { message: string; error?: string };
    try {
      body = await res.json();
    } catch {
      body = { message: `HTTP ${res.status}` };
    }
    throw new ApiError(res.status, body);
  }

  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  generateEmpire: () =>
    request<EmpireResponse>("/api/empire/generate", { method: "POST" }),

  rerollCategory: (category: RerollCategory) =>
    request<EmpireResponse>("/api/empire/reroll", {
      method: "POST",
      body: JSON.stringify({ category }),
    }),

  rerollTrait: (traitId: string) =>
    request<EmpireResponse>("/api/empire/reroll", {
      method: "POST",
      body: JSON.stringify({ category: "trait_single", traitId }),
    }),

  addTrait: () =>
    request<EmpireResponse>("/api/empire/reroll", {
      method: "POST",
      body: JSON.stringify({ category: "trait_add" }),
    }),

  addLeaderTrait: () =>
    request<EmpireResponse>("/api/empire/reroll", {
      method: "POST",
      body: JSON.stringify({ category: "leader_trait_add" }),
    }),

  removeTrait: () =>
    request<EmpireResponse>("/api/empire/reroll", {
      method: "POST",
      body: JSON.stringify({ category: "trait_remove" }),
    }),

  addSecondaryTrait: () =>
    request<EmpireResponse>("/api/empire/reroll", {
      method: "POST",
      body: JSON.stringify({ category: "trait_secondary_add" }),
    }),

  getVersion: () =>
    request<VersionResponse>("/api/data/version"),

  getSettings: () =>
    request<SettingsResponse>("/api/settings"),

  saveSettings: (gamePath: string, disabledDlcs: string[]) =>
    request<SettingsResponse>("/api/settings", {
      method: "PUT",
      body: JSON.stringify({ gamePath, disabledDlcs }),
    }),

  exportEmpire: (req: ExportRequest) =>
    request<ExportResponse>("/api/empire/export", {
      method: "POST",
      body: JSON.stringify(req),
    }),

  suggestNames: () =>
    request<SuggestedNames>("/api/empire/suggest-names", { method: "POST" }),

  resetSettings: () =>
    request<void>("/api/settings/reset", { method: "POST" }),
};
