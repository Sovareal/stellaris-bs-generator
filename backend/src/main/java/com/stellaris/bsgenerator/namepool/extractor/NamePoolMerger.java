package com.stellaris.bsgenerator.namepool.extractor;

import com.stellaris.bsgenerator.namepool.model.NamePool;
import com.stellaris.bsgenerator.namepool.model.PoolSection;

import java.util.List;

/**
 * Merges freshly extracted data into an existing NamePool, replacing only the
 * {@code extracted} arrays. All {@code custom} arrays are preserved verbatim.
 */
public class NamePoolMerger {

    public NamePool merge(NamePool existing, RulerNameExtractor.Result rulers,
                          List<String> homeworldNames, String generatedAt, String stellarisVersion) {
        return new NamePool(
                NamePool.CURRENT_SCHEMA_VERSION,
                generatedAt,
                stellarisVersion,
                existing.empireNames(),
                existing.rulerNames().withNewExtracted(rulers.rulerNames()),
                existing.regnalNames().withNewExtracted(rulers.regnalNames()),
                existing.homeworldNames().withNewExtracted(homeworldNames),
                existing.systemNames()
        );
    }
}
