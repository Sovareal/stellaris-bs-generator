export interface EthicDto {
  id: string;
  displayName: string | null;
  cost: number;
  isFanatic: boolean;
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

export interface OriginDto {
  id: string;
  displayName: string | null;
  dlcRequirement: string | null;
}

export interface ArchetypeDto {
  id: string;
  displayName: string | null;
  traitPoints: number;
  maxTraits: number;
  robotic: boolean;
}

export interface TraitDto {
  id: string;
  displayName: string | null;
  cost: number;
  allowedArchetypes: string[];
  enforced: boolean;
}

export interface PlanetClassDto {
  id: string;
  displayName: string | null;
  climate: string;
}

export interface LeaderTraitDto {
  id: string;
  displayName: string | null;
  cost: number;
  gfxKey: string | null;
}

export interface LeaderDto {
  leaderClass: string;
  traits: LeaderTraitDto[];
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

export type RerollCategory =
  | "ethics"
  | "authority"
  | "civic1"
  | "civic2"
  | "origin"
  | "traits"
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

export interface VersionResponse {
  version: string;
  rawVersion: string;
  buildHash: string;
}

export interface ErrorResponse {
  message: string;
  status: number;
}

export interface SettingsResponse {
  gamePath: string;
  valid: boolean;
  validationMessage: string;
}
