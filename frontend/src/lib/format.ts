const PREFIXES = [
  "ethic_fanatic_",
  "ethic_",
  "auth_",
  "civic_",
  "origin_",
  "trait_ruler_",
  "leader_trait_",
  "trait_",
  "species_archetype_",
  "pc_",
];

export function humanizeId(id: string): string {
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
