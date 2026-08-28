package org.nepe.competition.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.competition.domain.Competition;
import org.nepe.competition.port.in.CreateCompetitionCommand;
import org.nepe.competition.port.in.UpdateCompetitionCommand;
import org.nepe.competition.port.out.CompetitionRepositoryPort;
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

@DisplayName("CompetitionService Unit Tests")
class CompetitionServiceTest {

    private InMemoryCompetitionRepository repository;
    private CompetitionService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCompetitionRepository();
        service = new CompetitionService(repository);
    }

    @Nested
    @DisplayName("Constructor and Invariants")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw when repository port is null")
        void shouldThrowWhenRepositoryIsNull() {
            assertThatThrownBy(() -> new CompetitionService(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("CompetitionRepositoryPort must not be null");
        }
    }

    @Nested
    @DisplayName("createCompetition() Tests")
    class CreateCompetitionTests {

        @Test
        @DisplayName("Should successfully create and persist a new competition with default rho")
        void shouldCreateCompetitionWithDefaultRho() {
            CreateCompetitionCommand command = new CreateCompetitionCommand("I1", "Serie A", "Italy");

            Competition created = service.createCompetition(command);

            assertThat(created.getId()).isPositive();
            assertThat(created.getCode()).isEqualTo("I1");
            assertThat(created.getName()).isEqualTo("Serie A");
            assertThat(created.getCountry()).isEqualTo("Italy");
            assertThat(created.getDixonColesRho()).isEqualTo(Competition.DEFAULT_DIXON_COLES_RHO);
        }

        @Test
        @DisplayName("Should successfully create competition with custom rho")
        void shouldCreateCompetitionWithCustomRho() {
            CreateCompetitionCommand command = new CreateCompetitionCommand("E0", "Premier League", "England", -0.145);

            Competition created = service.createCompetition(command);

            assertThat(created.getCode()).isEqualTo("E0");
            assertThat(created.getDixonColesRho()).isEqualTo(-0.145);
        }

        @Test
        @DisplayName("Should throw DomainValidationException when command is null")
        void shouldThrowWhenCommandIsNull() {
            assertThatThrownBy(() -> service.createCompetition(null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("CreateCompetitionCommand cannot be null");
        }

        @Test
        @DisplayName("Should throw DomainValidationException when competition code already exists")
        void shouldThrowWhenCodeAlreadyExists() {
            service.createCompetition(new CreateCompetitionCommand("I1", "Serie A", "Italy"));

            assertThatThrownBy(() -> service.createCompetition(new CreateCompetitionCommand("I1", "Serie A Duplicate", "Italy")))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("updateCompetition() Tests")
    class UpdateCompetitionTests {

        @Test
        @DisplayName("Should update existing competition details and rho")
        void shouldUpdateExistingCompetition() {
            Competition initial = service.createCompetition(new CreateCompetitionCommand("I1", "Serie A", "Italy"));
            UpdateCompetitionCommand command = new UpdateCompetitionCommand(initial.getId(), "Serie A TIM", "Italia", -0.110);

            Competition updated = service.updateCompetition(command);

            assertThat(updated.getName()).isEqualTo("Serie A TIM");
            assertThat(updated.getCountry()).isEqualTo("Italia");
            assertThat(updated.getDixonColesRho()).isEqualTo(-0.110);
            assertThat(repository.findById(initial.getId()).orElseThrow().getName()).isEqualTo("Serie A TIM");
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when updating non-existent competition")
        void shouldThrowWhenUpdatingNonExistent() {
            UpdateCompetitionCommand command = new UpdateCompetitionCommand(999, "Unknown", "Unknown", -0.12);

            assertThatThrownBy(() -> service.updateCompetition(command))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Competition with ID 999 not found");
        }

        @Test
        @DisplayName("Should throw DomainValidationException when command is null")
        void shouldThrowWhenCommandIsNull() {
            assertThatThrownBy(() -> service.updateCompetition(null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("UpdateCompetitionCommand cannot be null");
        }
    }

    @Nested
    @DisplayName("Query and Retrieval Tests")
    class RetrievalTests {

        @Test
        @DisplayName("getAllCompetitions() should return all persisted competitions")
        void shouldReturnAllCompetitions() {
            service.createCompetition(new CreateCompetitionCommand("I1", "Serie A", "Italy"));
            service.createCompetition(new CreateCompetitionCommand("E0", "Premier League", "England"));

            List<Competition> all = service.getAllCompetitions();

            assertThat(all).hasSize(2);
        }

        @Test
        @DisplayName("getCompetitionById() should return competition when present")
        void shouldReturnCompetitionById() {
            Competition saved = service.createCompetition(new CreateCompetitionCommand("I1", "Serie A", "Italy"));

            Competition result = service.getCompetitionById(saved.getId());

            assertThat(result.getCode()).isEqualTo("I1");
        }

        @Test
        @DisplayName("getCompetitionById() should throw EntityNotFoundException when absent")
        void shouldThrowWhenIdNotFound() {
            assertThatThrownBy(() -> service.getCompetitionById(404))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Competition with ID 404 not found");
        }

        @Test
        @DisplayName("getCompetitionByCode() should find competition case-insensitively")
        void shouldFindCompetitionByCode() {
            service.createCompetition(new CreateCompetitionCommand("I1", "Serie A", "Italy"));

            Competition found = service.getCompetitionByCode("i1");

            assertThat(found.getCode()).isEqualTo("I1");
            assertThat(found.getName()).isEqualTo("Serie A");
        }

        @Test
        @DisplayName("getCompetitionByCode() should throw DomainValidationException on blank code")
        void shouldThrowOnBlankCode() {
            assertThatThrownBy(() -> service.getCompetitionByCode("  "))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Competition code cannot be null or blank");
        }

        @Test
        @DisplayName("getCompetitionByCode() should throw EntityNotFoundException when absent")
        void shouldThrowWhenCodeNotFound() {
            assertThatThrownBy(() -> service.getCompetitionByCode("D1"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Competition with code 'D1' not found");
        }
    }

    @Nested
    @DisplayName("deleteCompetition() Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete existing competition")
        void shouldDeleteExistingCompetition() {
            Competition saved = service.createCompetition(new CreateCompetitionCommand("I1", "Serie A", "Italy"));

            service.deleteCompetition(saved.getId());

            assertThat(repository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting non-existent competition")
        void shouldThrowWhenDeletingNonExistent() {
            assertThatThrownBy(() -> service.deleteCompetition(999))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Competition with ID 999 not found");
        }
    }

    /**
     * In-memory test double (Fake) for {@link CompetitionRepositoryPort}.
     */
    private static class InMemoryCompetitionRepository implements CompetitionRepositoryPort {
        private final Map<Integer, Competition> storage = new HashMap<>();
        private int idSequence = 1;

        @Override
        public Competition save(Competition competition) {
            if (competition.getId() == null) {
                competition.assignId(idSequence++);
            }
            storage.put(competition.getId(), competition);
            return competition;
        }

        @Override
        public Optional<Competition> findById(int id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<Competition> findByCode(String code) {
            return storage.values().stream()
                    .filter(c -> c.getCode().equalsIgnoreCase(code.trim()))
                    .findFirst();
        }

        @Override
        public List<Competition> findAll() {
            List<Competition> list = new ArrayList<>(storage.values());
            list.sort(Comparator.comparing(Competition::getName));
            return List.copyOf(list);
        }

        @Override
        public boolean existsByCode(String code) {
            return storage.values().stream()
                    .anyMatch(c -> c.getCode().equalsIgnoreCase(code.trim()));
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
