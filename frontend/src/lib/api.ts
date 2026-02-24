import type { EmpireResponse, ExportRequest, ExportResponse, RerollCategory, SettingsResponse, VersionResponse } from "@/types/empire";

const BASE_URL = "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  body: { message: string };

  constructor(status: number, body: { message: string }) {
    super(body.message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!res.ok) {
    let body: { message: string };
    try {
      body = await res.json();
    } catch {
      body = { message: `HTTP ${res.status}` };
    }
    throw new ApiError(res.status, body);
  }

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

  getVersion: () =>
    request<VersionResponse>("/api/data/version"),

  getSettings: () =>
    request<SettingsResponse>("/api/settings"),

  saveSettings: (gamePath: string) =>
    request<SettingsResponse>("/api/settings", {
      method: "PUT",
      body: JSON.stringify({ gamePath }),
    }),

  exportEmpire: (req: ExportRequest) =>
    request<ExportResponse>("/api/empire/export", {
      method: "POST",
      body: JSON.stringify(req),
    }),
};
