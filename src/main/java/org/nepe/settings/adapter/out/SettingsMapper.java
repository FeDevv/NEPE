package org.nepe.settings.adapter.out;

import org.nepe.settings.domain.AppSettings;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bidirectional mapper between the strongly-typed domain model {@link AppSettings}
 * and the relational key-value entities {@link SettingsJpaEntity}.
 */
@Component
public class SettingsMapper {

    /**
     * Reconstructs an {@link AppSettings} domain entity from a list of key-value JPA records.
     * If the list is null or empty, returns default domain settings.
     *
     * @param entities list of key-value JPA entities loaded from DB
     * @return populated {@link AppSettings} instance
     */
    public AppSettings toDomain(List<SettingsJpaEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return AppSettings.defaults();
        }

        Map<String, String> map = entities.stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .collect(Collectors.toMap(
                        SettingsJpaEntity::getKey,
                        SettingsJpaEntity::getValue,
                        (existing, replacement) -> replacement
                ));

        return AppSettings.fromMap(map);
    }

    /**
     * Converts a strongly-typed {@link AppSettings} domain model into a list of key-value JPA entities.
     *
     * @param domain the domain settings model
     * @return list of JPA key-value entities ready for persistence
     */
    public List<SettingsJpaEntity> toJpaEntities(AppSettings domain) {
        if (domain == null) {
            return Collections.emptyList();
        }

        Map<String, String> map = domain.toMap();
        return map.entrySet().stream()
                .map(entry -> new SettingsJpaEntity(entry.getKey(), entry.getValue()))
                .toList();
    }
}
