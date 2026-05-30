// AUTO-GENERATED -- do not edit manually
// Source: backend/src/main/java/com/stellaris/bsgenerator/dto/
// Regenerate: gradle :backend:generateTypeScript

export interface ArchetypeDto {
  id: string;
  displayName: string | null;
  traitPoints: number;
  maxTraits: number;
  robotic: boolean;
}

export interface AuthorityDto {
  id: string;
  displayName: string | null;
  isGestalt: boolean;
}

export interface CivicDto {
  id: string;
  displayName: string | null;
}

export interface DlcInfo {
  name: string;
  category: string;
}

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

export interface EthicDto {
  id: string;
  displayName: string | null;
  cost: number;
  isFanatic: boolean;
}

export interface ExportRequest {
  empireName: string;
  speciesName: string;
  speciesPlural: string | null;
  speciesAdjective: string | null;
  rulerName: string;
  homeworldName: string | null;
  homeSystemName: string | null;
}

export interface ExportResponse {
  success: boolean;
  filePath: string;
  empireName: string;
}

export interface LeaderDto {
  leaderClass: string;
  traits: LeaderTraitDto[];
  leaderPicksMax: number;
  leaderBudget: number;
}

export interface LeaderTraitDto {
  id: string;
  displayName: string | null;
  cost: number;
  gfxKey: string | null;
}

export interface OriginDto {
  id: string;
  displayName: string | null;
  dlcRequirement: string | null;
}

export interface PlanetClassDto {
  id: string;
  displayName: string | null;
  climate: string;
}

export interface SecondarySpeciesDto {
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

export interface SettingsResponse {
  gamePath: string;
  valid: boolean;
  validationMessage: string;
  disabledDlcs: string[] | null;
  availableDlcs: DlcInfo[];
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

export interface TraitDto {
  id: string;
  displayName: string | null;
  cost: number;
  allowedArchetypes: string[];
  enforced: boolean;
  free: boolean;
}

export interface VersionResponse {
  version: string;
  rawVersion: string;
  buildHash: string;
}

