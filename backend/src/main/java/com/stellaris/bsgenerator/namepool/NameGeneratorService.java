package com.stellaris.bsgenerator.namepool;

import com.stellaris.bsgenerator.dto.SuggestedNames;
import com.stellaris.bsgenerator.engine.GeneratedEmpire;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class NameGeneratorService {

    private final NamePoolLoader loader;
    private final Random random = new Random();

    public SuggestedNames suggest(GeneratedEmpire empire) {
        var pool = loader.getPool();

        String empireName    = pick(pool.empireNames());
        String rulerName     = pickRulerName(empire, pool);
        String homeworldName = pick(pool.homeworldNames());
        String systemName    = pick(pool.systemNames());

        return new SuggestedNames(
                empireName,
                rulerName,
                homeworldName,
                systemName,
                empireName,
                empireName,
                empireName
        );
    }

    private String pickRulerName(GeneratedEmpire empire,
                                 com.stellaris.bsgenerator.namepool.model.NamePool pool) {
        boolean isImperial = "auth_imperial".equals(empire.authority().id());
        if (isImperial) {
            var regnal = pool.regnalNames();
            if (!regnal.isEmpty()) return pick(regnal);
        }
        return pick(pool.rulerNames());
    }

    private String pick(List<String> names) {
        if (names.isEmpty()) {
            throw new IllegalStateException("Name pool section is empty -- add entries to custom_names.json");
        }
        return names.get(random.nextInt(names.size()));
    }
}
