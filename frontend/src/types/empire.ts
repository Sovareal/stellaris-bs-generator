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
}

export interface PlanetClassDto {
  id: string;
  displayName: string | null;
  climate: string;
}

export interface LeaderDto {
  leaderClass: string;
  traitId: string | null;
  traitDisplayName: string | null;
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
  | "leader";

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
  shipset: string;
  shipsetName: string | null;
  leader: LeaderDto;
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
