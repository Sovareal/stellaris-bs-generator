import { create } from "zustand";
import { api, ApiError } from "@/lib/api";
import type { EmpireResponse, ExportRequest, RerollCategory } from "@/types/empire";

interface EmpireStore {
  empire: EmpireResponse | null;
  isLoading: boolean;
  isRerolling: RerollCategory | null;
  isRerollingTrait: string | null;
  isAddingTrait: boolean;
  isAddingLeaderTrait: boolean;
  traitsFinalized: boolean;
  isSaving: boolean;
  saveSuccess: string | null; // file path on success, null otherwise
  error: string | null;
  generationId: number;
  generate: () => Promise<void>;
  reroll: (category: RerollCategory) => Promise<void>;
  rerollTrait: (traitId: string) => Promise<void>;
  addTrait: () => Promise<void>;
  addLeaderTrait: () => Promise<void>;
  finalizeTraits: () => void;
  saveToGame: (req: ExportRequest) => Promise<void>;
  clearError: () => void;
  clearSaveState: () => void;
}

export const useEmpireStore = create<EmpireStore>((set, get) => ({
  empire: null,
  isLoading: false,
  isRerolling: null,
  isRerollingTrait: null,
  isAddingTrait: false,
  isAddingLeaderTrait: false,
  traitsFinalized: false,
  isSaving: false,
  saveSuccess: null,
  error: null,
  generationId: 0,

  generate: async () => {
    set({ isLoading: true, error: null, traitsFinalized: false });
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
    set({ isRerolling: category, error: null, traitsFinalized: false });
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
    if (get().isRerolling || get().isRerollingTrait || get().isAddingTrait) return;
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

  addTrait: async () => {
    if (get().isRerolling || get().isRerollingTrait || get().isAddingTrait || get().isAddingLeaderTrait) return;
    set({ isAddingTrait: true, error: null, traitsFinalized: false });
    try {
      const empire = await api.addTrait();
      set((s) => ({
        empire,
        isAddingTrait: false,
        generationId: s.generationId + 1,
      }));
    } catch (e) {
      const message = e instanceof ApiError ? e.body.message : "Failed to add trait";
      set({ isAddingTrait: false, error: message });
    }
  },

  addLeaderTrait: async () => {
    if (get().isRerolling || get().isAddingTrait || get().isAddingLeaderTrait) return;
    set({ isAddingLeaderTrait: true, error: null, traitsFinalized: false });
    try {
      const empire = await api.addLeaderTrait();
      set((s) => ({
        empire,
        isAddingLeaderTrait: false,
        generationId: s.generationId + 1,
      }));
    } catch (e) {
      const message = e instanceof ApiError ? e.body.message : "Failed to add leader trait";
      set({ isAddingLeaderTrait: false, error: message });
    }
  },

  finalizeTraits: () => set({ traitsFinalized: true }),

  saveToGame: async (req: ExportRequest) => {
    set({ isSaving: true, error: null, saveSuccess: null });
    try {
      const result = await api.exportEmpire(req);
      set({ isSaving: false, saveSuccess: result.filePath });
    } catch (e) {
      const message = e instanceof ApiError ? e.body.message : "Failed to save empire";
      set({ isSaving: false, error: message });
    }
  },

  clearError: () => set({ error: null }),
  clearSaveState: () => set({ saveSuccess: null }),
}));
