package com.stellaris.bsgenerator.namepool;

import com.stellaris.bsgenerator.namepool.model.CustomNameFile;
import com.stellaris.bsgenerator.namepool.model.ExtractedNameFile;
import com.stellaris.bsgenerator.namepool.model.NamePool;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class NamePoolLoader {

    @Getter
    private NamePool pool;

    @PostConstruct
    public void load() {
        var mapper = JsonMapper.builder().build();

        CustomNameFile custom = loadCustom(mapper);
        ExtractedNameFile extracted = loadExtracted(mapper);

        pool = merge(custom, extracted);

        log.info("Loaded name pool: {} empire, {} ruler, {} regnal, {} homeworld, {} system names",
                pool.empireNames().size(), pool.rulerNames().size(), pool.regnalNames().size(),
                pool.homeworldNames().size(), pool.systemNames().size());
    }

    private static CustomNameFile loadCustom(JsonMapper mapper) {
        try (InputStream in = NamePoolLoader.class.getResourceAsStream("/data/custom_names.json")) {
            if (in == null) throw new IllegalStateException("custom_names.json not found in classpath at /data/custom_names.json");
            var file = mapper.readValue(in, CustomNameFile.class);
            if (file.schemaVersion() != CustomNameFile.CURRENT_SCHEMA_VERSION) {
                throw new IllegalStateException("custom_names.json schema version mismatch: expected "
                        + CustomNameFile.CURRENT_SCHEMA_VERSION + ", got " + file.schemaVersion());
            }
            return file;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load custom_names.json", e);
        }
    }

    private static ExtractedNameFile loadExtracted(JsonMapper mapper) {
        try (InputStream in = NamePoolLoader.class.getResourceAsStream("/data/name_pool_extracted.json")) {
            if (in == null) {
                log.warn("name_pool_extracted.json not found -- run ./gradlew :backend:extractNamePool to populate extracted names");
                return null;
            }
            var file = mapper.readValue(in, ExtractedNameFile.class);
            if (file.schemaVersion() != ExtractedNameFile.CURRENT_SCHEMA_VERSION) {
                log.warn("name_pool_extracted.json schema version mismatch (expected {}, got {}) -- skipping extracted names",
                        ExtractedNameFile.CURRENT_SCHEMA_VERSION, file.schemaVersion());
                return null;
            }
            return file;
        } catch (Exception e) {
            log.warn("Failed to load name_pool_extracted.json -- skipping: {}", e.getMessage());
            return null;
        }
    }

    private static NamePool merge(CustomNameFile custom, ExtractedNameFile extracted) {
        return new NamePool(
                orEmpty(custom.empireNames()),
                concat(custom.rulerNames(),    extracted != null ? extracted.rulerNames()    : null),
                concat(custom.regnalNames(),   extracted != null ? extracted.regnalNames()   : null),
                concat(custom.homeworldNames(), extracted != null ? extracted.homeworldNames() : null),
                orEmpty(custom.systemNames())
        );
    }

    private static List<String> concat(List<String> a, List<String> b) {
        if (b == null || b.isEmpty()) return orEmpty(a);
        if (a == null || a.isEmpty()) return List.copyOf(b);
        var merged = new ArrayList<>(a);
        merged.addAll(b);
        return List.copyOf(merged);
    }

    private static List<String> orEmpty(List<String> list) {
        return list != null ? list : List.of();
    }
}
