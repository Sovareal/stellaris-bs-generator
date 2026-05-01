interface EthicDto {
  id: string;
  displayName: string | null;
  cost: number;
  isFanatic: boolean;
}

interface AuthorityDto {
  id: string;
  displayName: string | null;
  isGestalt: boolean;
}

interface CivicDto {
  id: string;
  displayName: string | null;
}

interface OriginDto {
  id: string;
  displayName: string | null;
  dlcRequirement: string | null;
}

interface ArchetypeDto {
  id: string;
  displayName: string | null;
  traitPoints: number;
  maxTraits: number;
  robotic: boolean;
}

interface TraitDto {
  id: string;
  displayName: string | null;
  cost: number;
  allowedArchetypes: string[];
  enforced: boolean;
  /** True for origin-enforced traits: shown with lock icon but don't count toward picks or budget. */
  free: boolean;
}

interface PlanetClassDto {
  id: string;
  displayName: string | null;
  climate: string;
}

interface LeaderTraitDto {
  id: string;
  displayName: string | null;
  cost: number;
  gfxKey: string | null;
}

interface LeaderDto {
  leaderClass: string;
  traits: LeaderTraitDto[];
  leaderPicksMax: number;
  leaderBudget: number;
}

interface SecondarySpeciesDto {
  title: string;
  titleDisplayName: string | null;
  speciesClass: string;
  speciesClassName: string | null;
  enforcedTraits: TraitDto[];
  additionalTraits: TraitDto[];
  traitPointsUsed: number;
  traitPointsBudget: number;
  maxTraitPicks: number;
}

export type RerollCategory =
  | "ethics"
  | "authority"
  | "civic1"
  | "civic2"
  | "origin"
  | "trait_single"
  | "trait_add"
  | "leader_trait_add"
  | "homeworld"
  | "shipset"
  | "leader"
  | "secondaryspecies";

export interface EmpireResponse {
  ethics: EthicDto[];
  authority: AuthorityDto;
  civics: CivicDto[];
  origin: OriginDto;
  speciesArchetype: ArchetypeDto;
  speciesClass: string;
  speciesClassName: string | null;
  speciesTraits: TraitDto[];
  traitPointsUsed: number;
  traitPointsBudget: number;
  homeworld: PlanetClassDto;
  habitabilityPreference: PlanetClassDto;
  shipset: string;
  shipsetName: string | null;
  leader: LeaderDto;
  secondarySpecies: SecondarySpeciesDto | null;
  rerollsAvailable: Record<string, boolean>;
}

export interface SuggestedNames {
  empireName: string;
  rulerName: string;
  homeworldName: string;
  systemName: string;
  speciesName: string;
  speciesPlural: string;
  speciesAdjective: string;
}

export interface VersionResponse {
  version: string;
  rawVersion: string;
  buildHash: string;
}

export interface DlcInfo {
  name: string;
  category: string;
}

export interface SettingsResponse {
  gamePath: string;
  valid: boolean;
  validationMessage: string;
  disabledDlcs: string[] | null;
  availableDlcs: DlcInfo[];
}

export interface ExportRequest {
  empireName: string;
  speciesName: string;
  speciesPlural?: string;
  speciesAdjective?: string;
  rulerName: string;
  homeworldName?: string;
  homeSystemName?: string;
}

export interface ExportResponse {
  success: boolean;
  filePath: string;
  empireName: string;
}
