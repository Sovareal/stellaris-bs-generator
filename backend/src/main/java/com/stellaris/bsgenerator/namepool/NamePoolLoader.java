package com.stellaris.bsgenerator.namepool;

import com.stellaris.bsgenerator.namepool.model.NamePool;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;

@Slf4j
@Component
public class NamePoolLoader {

    @Getter
    private NamePool pool;

    @PostConstruct
    public void load() {
        try (InputStream in = getClass().getResourceAsStream("/data/name_pool.json")) {
            if (in == null) {
                throw new IllegalStateException("name_pool.json not found in classpath at /data/name_pool.json");
            }
            var mapper = JsonMapper.builder().build();
            pool = mapper.readValue(in, NamePool.class);

            if (pool.schemaVersion() != NamePool.CURRENT_SCHEMA_VERSION) {
                throw new IllegalStateException(
                        "name_pool.json schema version mismatch: expected "
                        + NamePool.CURRENT_SCHEMA_VERSION + ", got " + pool.schemaVersion());
            }

            log.info("Loaded name pool: {} empire, {} ruler, {} regnal, {} homeworld, {} system names",
                    pool.empireNames().all().size(),
                    pool.rulerNames().all().size(),
                    pool.regnalNames().all().size(),
                    pool.homeworldNames().all().size(),
                    pool.systemNames().all().size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load name_pool.json", e);
        }
    }
}
