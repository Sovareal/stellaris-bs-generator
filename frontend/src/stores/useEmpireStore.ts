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

export const useEmpireStore = create<EmpireStore>((set, get) => {
  async function withApi<T>(
    before: Partial<EmpireStore>,
    apiFn: () => Promise<T>,
    onSuccess: (result: T, s: EmpireStore) => Partial<EmpireStore>,
    onError: Partial<EmpireStore>,
    fallbackMsg: string
  ): Promise<void> {
    set({ error: null, ...before });
    try {
      const result = await apiFn();
      set((s) => onSuccess(result, s));
    } catch (e) {
      const message = e instanceof ApiError ? e.body.message : fallbackMsg;
      set({ error: message, ...onError });
    }
  }

  return {
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

    generate: () =>
      withApi(
        { isLoading: true, traitsFinalized: false, secondaryTraitsFinalized: false, leaderTraitsFinalized: false },
        api.generateEmpire,
        (empire, s) => ({ empire, isLoading: false, generationId: s.generationId + 1 }),
        { isLoading: false },
        "Failed to generate empire"
      ),

    reroll: async (category: RerollCategory) => {
      if (get().isRerolling) return;
      await withApi(
        { isRerolling: category, traitsFinalized: false, secondaryTraitsFinalized: false, leaderTraitsFinalized: false },
        () => api.rerollCategory(category),
        (empire, s) => ({ empire, isRerolling: null, generationId: s.generationId + 1 }),
        { isRerolling: null },
        "Failed to reroll"
      );
    },

    rerollTrait: async (traitId: string) => {
      if (get().isRerolling || get().isRerollingTrait || get().isAddingTrait) return;
      await withApi(
        { isRerollingTrait: traitId },
        () => api.rerollTrait(traitId),
        (empire) => ({ empire, isRerollingTrait: null }),
        { isRerollingTrait: null },
        "Failed to reroll trait"
      );
    },

    addTrait: async () => {
      if (get().isRerolling || get().isRerollingTrait || get().isAddingTrait || get().isAddingLeaderTrait) return;
      await withApi(
        { isAddingTrait: true, traitsFinalized: false },
        api.addTrait,
        (empire) => ({ empire, isAddingTrait: false }),
        { isAddingTrait: false },
        "Failed to add trait"
      );
    },

    addLeaderTrait: async () => {
      if (get().isRerolling || get().isAddingTrait || get().isAddingLeaderTrait) return;
      await withApi(
        { isAddingLeaderTrait: true, traitsFinalized: false },
        api.addLeaderTrait,
        (empire) => ({ empire, isAddingLeaderTrait: false }),
        { isAddingLeaderTrait: false },
        "Failed to add leader trait"
      );
    },

    removeTrait: async () => {
      if (get().isRerolling || get().isRerollingTrait || get().isAddingTrait || get().isRemovingTrait) return;
      await withApi(
        { isRemovingTrait: true },
        api.removeTrait,
        (empire) => ({ empire, isRemovingTrait: false }),
        { isRemovingTrait: false },
        "Failed to remove trait"
      );
    },

    addSecondaryTrait: async () => {
      if (get().isRerolling || get().isAddingTrait || get().isAddingSecondaryTrait) return;
      await withApi(
        { isAddingSecondaryTrait: true, secondaryTraitsFinalized: false },
        api.addSecondaryTrait,
        (empire) => ({ empire, isAddingSecondaryTrait: false }),
        { isAddingSecondaryTrait: false },
        "Failed to add secondary species trait"
      );
    },

    finalizeTraits: () => set({ traitsFinalized: true }),
    finalizeSecondaryTraits: () => set({ secondaryTraitsFinalized: true }),
    finalizeLeaderTraits: () => set({ leaderTraitsFinalized: true }),

    saveToGame: (req: ExportRequest) =>
      withApi(
        { isSaving: true, saveSuccess: null },
        () => api.exportEmpire(req),
        (result) => ({ isSaving: false, saveSuccess: result.filePath }),
        { isSaving: false },
        "Failed to save empire"
      ),

    suggestNames: () =>
      withApi(
        { isSuggestingNames: true },
        api.suggestNames,
        (suggestedNames) => ({ suggestedNames, isSuggestingNames: false }),
        { isSuggestingNames: false },
        "Failed to suggest names"
      ),

    clearError: () => set({ error: null }),
    clearSaveState: () => set({ saveSuccess: null }),
  };
});
