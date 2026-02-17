const PREFIXES = [
  "ethic_fanatic_",
  "ethic_",
  "auth_",
  "civic_",
  "origin_",
  "trait_ruler_",
  "leader_trait_",
  "trait_robot_",
  "trait_machine_",
  "trait_",
  "species_archetype_",
  "pc_",
];

const SHIPSET_NAMES: Record<string, string> = {
  mammalian_01: "Mammalian",
  reptilian_01: "Reptilian",
  avian_01: "Avian",
  molluscoid_01: "Molluscoid",
  fungoid_01: "Fungoid",
  arthropoid_01: "Arthropoid",
  humanoid_01: "Humanoid",
  plantoid_01: "Plantoid",
  lithoid_01: "Lithoid",
  necroid_01: "Necroid",
  aquatic_01: "Aquatic",
  toxoid_01: "Toxoid",
  subterranean_01: "Subterranean",
  cybernetics_01: "Cybernetics",
  synthetics_01: "Synthetics",
  biogenesis_01: "Spinovore",
  biogenesis_02: "Shellcraft",
  mindwarden_01: "Mindwarden",
  psionic_01: "Psionic",
  infernal_01: "Infernal",
};

const SPECIES_CLASS_NAMES: Record<string, string> = {
  MAM: "Mammalian",
  REP: "Reptilian",
  AVI: "Avian",
  ART: "Arthropoid",
  MOL: "Molluscoid",
  FUN: "Fungoid",
  HUM: "Humanoid",
  PLANT: "Plantoid",
  LITHOID: "Lithoid",
  NECROID: "Necroid",
  AQUATIC: "Aquatic",
  TOX: "Toxoid",
  INF: "Infernal",
  BIOGENESIS_01: "BioGenesis",
  MINDWARDEN: "Mindwarden",
  MACHINE: "Machine",
  ROBOT: "Robot",
};

/**
 * Returns the display name from localization data, falling back to humanizeId.
 */
export function displayName(item: { displayName?: string | null; id: string }): string {
  return item.displayName ?? humanizeId(item.id);
}

export function humanizeId(id: string): string {
  // Check display name maps first
  if (SHIPSET_NAMES[id]) return SHIPSET_NAMES[id];
  if (SPECIES_CLASS_NAMES[id]) return SPECIES_CLASS_NAMES[id];

  let stripped = id;
  for (const prefix of PREFIXES) {
    if (stripped.startsWith(prefix)) {
      stripped = stripped.slice(prefix.length);
      break;
    }
  }
  return stripped
    .split("_")
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}
