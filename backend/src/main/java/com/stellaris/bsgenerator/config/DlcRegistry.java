package com.stellaris.bsgenerator.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of known Stellaris DLC identifiers.
 * <p>
 * Two types of DLC flags appear in game files:
 * <ul>
 *   <li>{@code host_has_dlc = "DLC Name"} - used in civics and origins playable/potential blocks</li>
 *   <li>{@code has_xxx_dlc = yes} - used in species classes and newer content</li>
 * </ul>
 * Both are normalised to a canonical DLC name for consistent settings storage.
 */
public final class DlcRegistry {

    public record DlcInfo(String name, String category) {}

    /** Ordered list of all known DLCs shown in the settings UI. */
    public static final List<DlcInfo> ALL_DLCS = List.of(
            // Expansions
            new DlcInfo("Utopia",            "Expansion"),
            new DlcInfo("Apocalypse",        "Expansion"),
            new DlcInfo("Megacorp",          "Expansion"),
            new DlcInfo("Federations",       "Expansion"),
            new DlcInfo("Overlord",          "Expansion"),
            new DlcInfo("Galactic Paragons", "Expansion"),
            new DlcInfo("Nomads",            "Expansion"),
            // Story Packs
            new DlcInfo("Synthetic Dawn Story Pack", "Story Pack"),
            new DlcInfo("First Contact",             "Story Pack"),
            new DlcInfo("Astral Planes",             "Story Pack"),
            new DlcInfo("Grand Archive",             "Story Pack"),
            new DlcInfo("Machine Age",               "Story Pack"),
            new DlcInfo("The Shroud",                "Story Pack"),
            new DlcInfo("Cosmic Storms",             "Story Pack"),
            // Species Packs
            new DlcInfo("Humanoids Species Pack",  "Species Pack"),
            new DlcInfo("Plantoids Species Pack",  "Species Pack"),
            new DlcInfo("Lithoids Species Pack",   "Species Pack"),
            new DlcInfo("Necroids Species Pack",   "Species Pack"),
            new DlcInfo("Aquatics Species Pack",   "Species Pack"),
            new DlcInfo("Toxoids Species Pack",    "Species Pack"),
            new DlcInfo("Infernals Species Pack",  "Species Pack"),
            new DlcInfo("BioGenesis",              "Species Pack")
    );

    /** All canonical DLC names as a set for fast lookups. */
    public static final Set<String> ALL_DLC_NAMES = ALL_DLCS.stream()
            .map(DlcInfo::name)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    /**
     * Maps {@code host_has_dlc = "..."} strings to canonical DLC names.
     * Some strings match exactly; others differ in casing or wording.
     */
    private static final Map<String, String> HOST_DLC_MAP = Map.ofEntries(
            Map.entry("Utopia",                    "Utopia"),
            Map.entry("Apocalypse",                "Apocalypse"),
            Map.entry("Megacorp",                  "Megacorp"),
            Map.entry("Federations",               "Federations"),
            Map.entry("Overlord",                  "Overlord"),
            Map.entry("Galactic Paragons",         "Galactic Paragons"),
            Map.entry("Synthetic Dawn Story Pack", "Synthetic Dawn Story Pack"),
            Map.entry("Humanoids Species Pack",    "Humanoids Species Pack"),
            Map.entry("Plantoids Species Pack",    "Plantoids Species Pack"),
            Map.entry("Lithoids Species Pack",     "Lithoids Species Pack"),
            Map.entry("Necroids Species Pack",     "Necroids Species Pack"),
            Map.entry("Infernals Species Pack",    "Infernals Species Pack")
    );

    /**
     * Maps {@code has_xxx_dlc = yes} / {@code has_xxx = yes} trigger names
     * to canonical DLC names.
     */
    private static final Map<String, String> TRIGGER_MAP = Map.ofEntries(
            Map.entry("has_aquatics",          "Aquatics Species Pack"),
            Map.entry("has_lithoids",          "Lithoids Species Pack"),
            Map.entry("has_necroids",          "Necroids Species Pack"),
            Map.entry("has_toxoids",           "Toxoids Species Pack"),
            Map.entry("has_infernals",         "Infernals Species Pack"),
            Map.entry("has_biogenesis_dlc",    "BioGenesis"),
            Map.entry("has_shroud_dlc",        "The Shroud"),
            Map.entry("has_machine_age_dlc",   "Machine Age"),
            Map.entry("has_first_contact_dlc", "First Contact"),
            Map.entry("has_astral_planes_dlc", "Astral Planes"),
            Map.entry("has_grand_archive_dlc", "Grand Archive"),
            Map.entry("has_overlord_dlc",      "Overlord"),
            Map.entry("has_paragon_dlc",       "Galactic Paragons"),
            Map.entry("has_synthetic_dawn",    "Synthetic Dawn Story Pack"),
            Map.entry("has_cosmic_storms_dlc", "Cosmic Storms"),
            Map.entry("has_nomads_dlc",        "Nomads")
    );

    /** Resolve a {@code host_has_dlc} string to a canonical DLC name. Returns null if unknown. */
    public static String fromHostDlc(String hostDlc) {
        if (hostDlc == null) return null;
        return HOST_DLC_MAP.get(hostDlc);
    }

    /** Resolve a {@code has_xxx_dlc} trigger key to a canonical DLC name. Returns null if not a DLC trigger. */
    public static String fromTrigger(String trigger) {
        if (trigger == null) return null;
        return TRIGGER_MAP.get(trigger);
    }

    private DlcRegistry() {}
}
