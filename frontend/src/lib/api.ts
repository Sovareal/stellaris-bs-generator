import { invoke } from "@tauri-apps/api/core";
import type { EmpireResponse, ExportRequest, ExportResponse, RerollCategory, SettingsResponse, SuggestedNames, VersionResponse } from "@/types/empire";

const isTauri = typeof window !== "undefined" && "__TAURI__" in window;

export const backendPortPromise: Promise<number> = isTauri
  ? invoke<number>("get_backend_port").catch(() => 8080)
  : Promise.resolve(8080);

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
  const res = await fetch(`http://localhost:${port}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
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
