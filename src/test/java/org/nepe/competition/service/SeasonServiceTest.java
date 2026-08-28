package org.nepe.competition.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.competition.domain.Season;
import org.nepe.competition.port.out.SeasonRepositoryPort;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.EntityNotFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SeasonService Unit Tests")
class SeasonServiceTest {

    private InMemorySeasonRepository repository;
    private SeasonService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySeasonRepository();
        service = new SeasonService(repository);
    }

    @Nested
    @DisplayName("Constructor and Invariant Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NullPointerException when repository is null")
        void shouldThrowWhenRepositoryIsNull() {
            assertThatThrownBy(() -> new SeasonService(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("SeasonRepositoryPort must not be null");
        }
    }

    @Nested
    @DisplayName("getOrCreateSeason() Tests")
    class GetOrCreateSeasonTests {

        @Test
        @DisplayName("Should create new season if not yet present in repository")
        void shouldCreateSeasonWhenNotPresent() {
            Season season = service.getOrCreateSeason("2025/2026");

            assertThat(season.getId()).isPositive();
            assertThat(season.getName()).isEqualTo("2025/2026");
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return existing season without creating duplicate")
        void shouldReturnExistingSeason() {
            Season existing = service.createSeason("2025/2026");

            Season retrieved = service.getOrCreateSeason("2025/2026");

            assertThat(retrieved.getId()).isEqualTo(existing.getId());
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw DomainValidationException when seasonName is blank or null")
        void shouldThrowOnBlankSeasonName() {
            assertThatThrownBy(() -> service.getOrCreateSeason("   "))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Season name cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("createSeason() and createSeasonFromYear() Tests")
    class CreationTests {

        @Test
        @DisplayName("createSeason() should create and persist season")
        void shouldCreateSeasonByName() {
            Season season = service.createSeason("2024/2025");

            assertThat(season.getName()).isEqualTo("2024/2025");
            assertThat(repository.existsByName("2024/2025")).isTrue();
        }

        @Test
        @DisplayName("createSeason() should throw DomainValidationException if season already exists")
        void shouldThrowWhenSeasonAlreadyExists() {
            service.createSeason("2024/2025");

            assertThatThrownBy(() -> service.createSeason("2024/2025"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("createSeasonFromYear() should format YYYY/YYYY and persist")
        void shouldCreateSeasonFromYear() {
            Season season = service.createSeasonFromYear(2023);

            assertThat(season.getName()).isEqualTo("2023/2024");
            assertThat(repository.existsByName("2023/2024")).isTrue();
        }

        @Test
        @DisplayName("createSeasonFromYear() should throw DomainValidationException if already exists")
        void shouldThrowWhenSeasonFromYearAlreadyExists() {
            service.createSeasonFromYear(2023);

            assertThatThrownBy(() -> service.createSeasonFromYear(2023))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("Query and Retrieval Tests")
    class RetrievalTests {

        @Test
        @DisplayName("getAllSeasons() should return all persisted seasons ordered chronologically")
        void shouldReturnAllSeasons() {
            service.createSeason("2023/2024");
            service.createSeason("2025/2026");
            service.createSeason("2024/2025");

            List<Season> seasons = service.getAllSeasons();

            assertThat(seasons).hasSize(3);
            assertThat(seasons.get(0).getName()).isEqualTo("2025/2026");
        }

        @Test
        @DisplayName("getLatestSeason() should return the newest season")
        void shouldReturnLatestSeason() {
            service.createSeason("2023/2024");
            service.createSeason("2025/2026");

            Season latest = service.getLatestSeason();

            assertThat(latest.getName()).isEqualTo("2025/2026");
        }

        @Test
        @DisplayName("getLatestSeason() should throw EntityNotFoundException when repository is empty")
        void shouldThrowWhenNoSeasonsExist() {
            assertThatThrownBy(() -> service.getLatestSeason())
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No seasons currently registered");
        }

        @Test
        @DisplayName("getSeasonById() should return season when found")
        void shouldFindSeasonById() {
            Season saved = service.createSeason("2025/2026");

            Season result = service.getSeasonById(saved.getId());

            assertThat(result.getName()).isEqualTo("2025/2026");
        }

        @Test
        @DisplayName("getSeasonById() should throw EntityNotFoundException when not found")
        void shouldThrowWhenIdNotFound() {
            assertThatThrownBy(() -> service.getSeasonById(404))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Season with ID 404 not found");
        }

        @Test
        @DisplayName("getSeasonByName() should find season by formatted name")
        void shouldFindSeasonByName() {
            service.createSeason("2025/2026");

            Season result = service.getSeasonByName("2025/2026");

            assertThat(result.getName()).isEqualTo("2025/2026");
        }

        @Test
        @DisplayName("getSeasonByName() should throw EntityNotFoundException when not found")
        void shouldThrowWhenNameNotFound() {
            assertThatThrownBy(() -> service.getSeasonByName("1990/1991"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Season with name '1990/1991' not found");
        }
    }

    @Nested
    @DisplayName("deleteSeason() Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete existing season")
        void shouldDeleteExistingSeason() {
            Season saved = service.createSeason("2025/2026");

            service.deleteSeason(saved.getId());

            assertThat(repository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting non-existent season")
        void shouldThrowWhenDeletingNonExistent() {
            assertThatThrownBy(() -> service.deleteSeason(999))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Season with ID 999 not found");
        }
    }

    /**
     * In-memory test double (Fake) for {@link SeasonRepositoryPort}.
     */
    private static class InMemorySeasonRepository implements SeasonRepositoryPort {
        private final Map<Integer, Season> storage = new HashMap<>();
        private int idSequence = 1;

        @Override
        public Season save(Season season) {
            if (season.getId() == null) {
                season.assignId(idSequence++);
            }
            storage.put(season.getId(), season);
            return season;
        }

        @Override
        public Optional<Season> findById(int id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<Season> findByName(String name) {
            return storage.values().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(name.trim()))
                    .findFirst();
        }

        @Override
        public Optional<Season> findLatest() {
            return storage.values().stream()
                    .max(Season::compareTo);
        }

        @Override
        public List<Season> findAll() {
            List<Season> list = new ArrayList<>(storage.values());
            list.sort(Comparator.reverseOrder()); // newest first
            return List.copyOf(list);
        }

        @Override
        public boolean existsByName(String name) {
            return storage.values().stream()
                    .anyMatch(s -> s.getName().equalsIgnoreCase(name.trim()));
        }

        @Override
        public void deleteById(int id) {
            storage.remove(id);
        }

        @Override
        public long count() {
            return storage.size();
        }
    }
}
