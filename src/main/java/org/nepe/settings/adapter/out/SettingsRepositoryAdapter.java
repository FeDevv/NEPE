package org.nepe.settings.adapter.out;

import org.nepe.settings.domain.AppSettings;
import org.nepe.settings.port.out.SettingsRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Outbound Persistence Adapter implementing the {@link SettingsRepositoryPort}.
 * <p>
 * Loads and persists global application configurations in the MariaDB key-value settings table.
 */
@Repository
public class SettingsRepositoryAdapter implements SettingsRepositoryPort {

    private final SpringDataSettingsRepository springDataRepository;
    private final SettingsMapper mapper;

    public SettingsRepositoryAdapter(SpringDataSettingsRepository springDataRepository,
                                     SettingsMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataSettingsRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "SettingsMapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public AppSettings loadSettings() {
        List<SettingsJpaEntity> entities = springDataRepository.findAll();
        return mapper.toDomain(entities);
    }

    @Override
    @Transactional
    public AppSettings saveSettings(AppSettings settings) {
        if (settings == null) {
            throw new DomainValidationException("Settings to persist cannot be null.");
        }

        List<SettingsJpaEntity> entities = mapper.toJpaEntities(settings);
        springDataRepository.saveAll(entities);
        return settings;
    }
}
