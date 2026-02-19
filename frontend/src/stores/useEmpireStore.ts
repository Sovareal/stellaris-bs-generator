import { create } from "zustand";
import { api, ApiError } from "@/lib/api";
import type { EmpireResponse, RerollCategory } from "@/types/empire";

interface EmpireStore {
  empire: EmpireResponse | null;
  isLoading: boolean;
  isRerolling: RerollCategory | null;
  isRerollingTrait: string | null;
  error: string | null;
  generationId: number;
  generate: () => Promise<void>;
  reroll: (category: RerollCategory) => Promise<void>;
  rerollTrait: (traitId: string) => Promise<void>;
  clearError: () => void;
}

export const useEmpireStore = create<EmpireStore>((set, get) => ({
  empire: null,
  isLoading: false,
  isRerolling: null,
  isRerollingTrait: null,
  error: null,
  generationId: 0,

  generate: async () => {
    set({ isLoading: true, error: null });
    try {
      const empire = await api.generateEmpire();
      set((s) => ({
        empire,
        isLoading: false,
        generationId: s.generationId + 1,
      }));
    } catch (e) {
      const message = e instanceof ApiError ? e.body.message : "Failed to generate empire";
      set({ error: message, isLoading: false });
    }
  },

  reroll: async (category: RerollCategory) => {
    if (get().isRerolling) return;
    set({ isRerolling: category, error: null });
    try {
      const empire = await api.rerollCategory(category);
      set((s) => ({
        empire,
        isRerolling: null,
        generationId: s.generationId + 1,
      }));
    } catch (e) {
      const message = e instanceof ApiError ? e.body.message : "Failed to reroll";
      set({ error: message, isRerolling: null });
    }
  },

  rerollTrait: async (traitId: string) => {
    if (get().isRerolling || get().isRerollingTrait) return;
    set({ isRerollingTrait: traitId, error: null });
    try {
      const empire = await api.rerollTrait(traitId);
      set((s) => ({
        empire,
        isRerollingTrait: null,
        generationId: s.generationId + 1,
      }));
    } catch (e) {
      const message = e instanceof ApiError ? e.body.message : "Failed to reroll trait";
      set({ error: message, isRerollingTrait: null });
    }
  },

  clearError: () => set({ error: null }),
}));
