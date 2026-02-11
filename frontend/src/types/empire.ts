export interface EthicDto {
  id: string;
  cost: number;
  isFanatic: boolean;
}

export interface AuthorityDto {
  id: string;
  isGestalt: boolean;
}

export interface CivicDto {
  id: string;
}

export interface OriginDto {
  id: string;
  dlcRequirement: string | null;
}

export interface ArchetypeDto {
  id: string;
  traitPoints: number;
  maxTraits: number;
  robotic: boolean;
}

export interface TraitDto {
  id: string;
  cost: number;
  allowedArchetypes: string[];
}

export interface PlanetClassDto {
  id: string;
  climate: string;
}

export interface LeaderDto {
  leaderClass: string;
  traitId: string | null;
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
  speciesTraits: TraitDto[];
  traitPointsUsed: number;
  traitPointsBudget: number;
  homeworld: PlanetClassDto;
  shipset: string;
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
