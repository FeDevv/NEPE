package org.nepe.settings.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link SettingsJpaEntity}.
 * <p>
 * Internal data access interface within the outbound persistence adapter for managing
 * key-value application settings stored in MariaDB.
 */
@Repository
public interface SpringDataSettingsRepository extends JpaRepository<SettingsJpaEntity, String> {
}
