import { create } from "zustand";
import { api, ApiError } from "@/lib/api";
import type { EmpireResponse, ExportRequest, RerollCategory, SuggestedNames } from "@/types/empire";

interface EmpireStore {
  empire: EmpireResponse | null;
  isLoading: boolean;
  isRerolling: RerollCategory | null;
  isRerollingTrait: string | null;
  isAddingTrait: boolean;
  isAddingLeaderTrait: boolean;
  isRemovingTrait: boolean;
  isAddingSecondaryTrait: boolean;
  traitsFinalized: boolean;
  secondaryTraitsFinalized: boolean;
  leaderTraitsFinalized: boolean;
  isSaving: boolean;
  saveSuccess: string | null; // file path on success, null otherwise
  suggestedNames: SuggestedNames | null;
  isSuggestingNames: boolean;
  error: string | null;
  generationId: number;
  generate: () => Promise<void>;
  reroll: (category: RerollCategory) => Promise<void>;
  rerollTrait: (traitId: string) => Promise<void>;
  addTrait: () => Promise<void>;
  addLeaderTrait: () => Promise<void>;
  removeTrait: () => Promise<void>;
  addSecondaryTrait: () => Promise<void>;
  finalizeTraits: () => void;
  finalizeSecondaryTraits: () => void;
  finalizeLeaderTraits: () => void;
  saveToGame: (req: ExportRequest) => Promise<void>;
  suggestNames: () => Promise<void>;
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
  isRemovingTrait: false,
  isAddingSecondaryTrait: false,
  traitsFinalized: false,
  secondaryTraitsFinalized: false,
  leaderTraitsFinalized: false,
  isSaving: false,
  saveSuccess: null,
  suggestedNames: null,
  isSuggestingNames: false,
  error: null,
  generationId: 0,

  generate: async () => {
    set({ isLoading: true, error: null, traitsFinalized: false, secondaryTraitsFinalized: false, leaderTraitsFinalized: false });
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
    set({ isRerolling: category, error: null, traitsFinalized: false, secondaryTraitsFinalized: false, leaderTraitsFinalized: false });
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
      set({ empire, isRerollingTrait: null });
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
      set({ empire, isAddingTrait: false });
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
      set({ empire, isAddingLeaderTrait: false });
    } catch (e) {
      const message = e instanceof ApiError ? e.body.message : "Failed to add leader trait";
      set({ isAddingLeaderTrait: false, error: message });
    }
  },

  removeTrait: async () => {
    if (get().isRerolling || get().isRerollingTrait || get().isAddingTrait || get().isRemovingTrait) return;
    set({ isRemovingTrait: true, error: null });
    try {
      const empire = await api.removeTrait();
      set({ empire, isRemovingTrait: false });
    } catch (e) {
      const message = e instanceof ApiError ? e.body.message : "Failed to remove trait";
      set({ isRemovingTrait: false, error: message });
    }
  },

  addSecondaryTrait: async () => {
    if (get().isRerolling || get().isAddingTrait || get().isAddingSecondaryTrait) return;
    set({ isAddingSecondaryTrait: true, error: null, secondaryTraitsFinalized: false });
    try {
      const empire = await api.addSecondaryTrait();
      set({ empire, isAddingSecondaryTrait: false });
    } catch (e) {
      const message = e instanceof ApiError ? e.body.message : "Failed to add secondary species trait";
      set({ isAddingSecondaryTrait: false, error: message });
    }
  },

  finalizeTraits: () => set({ traitsFinalized: true }),

  finalizeSecondaryTraits: () => set({ secondaryTraitsFinalized: true }),

  finalizeLeaderTraits: () => set({ leaderTraitsFinalized: true }),

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

  suggestNames: async () => {
    set({ isSuggestingNames: true, error: null });
    try {
      const suggestedNames = await api.suggestNames();
      set({ suggestedNames, isSuggestingNames: false });
    } catch (e) {
      const message = e instanceof ApiError ? e.body.message : "Failed to suggest names";
      set({ isSuggestingNames: false, error: message });
    }
  },

  clearError: () => set({ error: null }),
  clearSaveState: () => set({ saveSuccess: null }),
}));
