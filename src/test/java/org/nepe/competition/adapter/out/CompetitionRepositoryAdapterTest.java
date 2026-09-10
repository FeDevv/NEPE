package org.nepe.competition.adapter.out;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.nepe.competition.domain.Competition;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CompetitionRepositoryAdapter Unit Tests")
class CompetitionRepositoryAdapterTest {

    private SpringDataCompetitionRepository springDataRepository;
    private SpringDataCompetitionTeamRepository springDataCompetitionTeamRepository;
    private CompetitionMapper mapper;
    private CompetitionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataCompetitionRepository.class);
        springDataCompetitionTeamRepository = mock(SpringDataCompetitionTeamRepository.class);
        mapper = new CompetitionMapper(); // Real mapper to avoid unnecessary mocking
        adapter = new CompetitionRepositoryAdapter(springDataRepository, springDataCompetitionTeamRepository, mapper);
    }

    @Nested
    @DisplayName("Constructor and Invariant Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should reject null SpringDataCompetitionRepository")
        void shouldRejectNullCompetitionRepository() {
            assertThatThrownBy(() -> new CompetitionRepositoryAdapter(null, springDataCompetitionTeamRepository, mapper))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("SpringDataCompetitionRepository must not be null");
        }

        @Test
        @DisplayName("Constructor should reject null SpringDataCompetitionTeamRepository")
        void shouldRejectNullCompetitionTeamRepository() {
            assertThatThrownBy(() -> new CompetitionRepositoryAdapter(springDataRepository, null, mapper))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("SpringDataCompetitionTeamRepository must not be null");
        }

        @Test
        @DisplayName("Constructor should reject null CompetitionMapper")
        void shouldRejectNullMapper() {
            assertThatThrownBy(() -> new CompetitionRepositoryAdapter(springDataRepository, springDataCompetitionTeamRepository, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("CompetitionMapper must not be null");
        }
    }

    @Nested
    @DisplayName("Save and Mutation Tests")
    class SaveTests {

        @Test
        @DisplayName("save() should reject null competition")
        void shouldRejectNullCompetition() {
            assertThatThrownBy(() -> adapter.save(null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Competition to save cannot be null");
        }

        @Test
        @DisplayName("save() should persist and map domain competition correctly")
        void shouldSaveCompetitionSuccessfully() {
            Competition domain = Competition.create("I1", "Serie A", "Italy", -0.1200, 1.22);
            CompetitionJpaEntity savedJpa = new CompetitionJpaEntity(1, "I1", "Serie A", "Italy", -0.1200, 1.22);

            when(springDataRepository.save(any(CompetitionJpaEntity.class))).thenReturn(savedJpa);

            Competition result = adapter.save(domain);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getCode()).isEqualTo("I1");
            assertThat(result.getName()).isEqualTo("Serie A");
            verify(springDataRepository).save(any(CompetitionJpaEntity.class));
        }

        @Test
        @DisplayName("save() should translate DataIntegrityViolationException into DomainValidationException")
        void shouldTranslateDataIntegrityViolationExceptionOnSave() {
            Competition domain = Competition.create("I1", "Serie A", "Italy", -0.1200);

            when(springDataRepository.save(any(CompetitionJpaEntity.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate code"));

            assertThatThrownBy(() -> adapter.save(domain))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Unable to persist competition with code 'I1'")
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Query and Retrieval Tests")
    class QueryTests {

        @Test
        @DisplayName("findById() should return mapped domain entity when found")
        void shouldFindById() {
            CompetitionJpaEntity jpa = new CompetitionJpaEntity(10, "E0", "Premier League", "England", -0.1300, 1.25);
            when(springDataRepository.findById(10)).thenReturn(Optional.of(jpa));

            Optional<Competition> result = adapter.findById(10);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(10);
            assertThat(result.get().getCode()).isEqualTo("E0");
            assertThat(result.get().getName()).isEqualTo("Premier League");
        }

        @Test
        @DisplayName("findByCode() should return empty when code is null or blank")
        void shouldReturnEmptyForBlankCode() {
            assertThat(adapter.findByCode(null)).isEmpty();
            assertThat(adapter.findByCode("")).isEmpty();
            assertThat(adapter.findByCode("   ")).isEmpty();
            verifyNoInteractions(springDataRepository);
        }

        @Test
        @DisplayName("findByCode() should trim and uppercase code before lookup")
        void shouldFindByCodeWithTrimming() {
            CompetitionJpaEntity jpa = new CompetitionJpaEntity(5, "D1", "Bundesliga", "Germany", -0.1100);
            when(springDataRepository.findByCode("D1")).thenReturn(Optional.of(jpa));

            Optional<Competition> result = adapter.findByCode("  d1  ");

            assertThat(result).isPresent();
            assertThat(result.get().getCode()).isEqualTo("D1");
            verify(springDataRepository).findByCode("D1");
        }

        @Test
        @DisplayName("findAll() should return all competitions mapped to domain")
        void shouldFindAll() {
            List<CompetitionJpaEntity> list = List.of(
                    new CompetitionJpaEntity(1, "I1", "Serie A", "Italy", -0.1200),
                    new CompetitionJpaEntity(2, "E0", "Premier League", "England", -0.1300)
            );
            when(springDataRepository.findAllByOrderByNameAsc()).thenReturn(list);

            List<Competition> result = adapter.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Serie A");
            assertThat(result.get(1).getName()).isEqualTo("Premier League");
        }

        @Test
        @DisplayName("existsByCode() should return false for blank code or query repository")
        void shouldCheckExistsByCode() {
            assertThat(adapter.existsByCode(" ")).isFalse();

            when(springDataRepository.existsByCode("I1")).thenReturn(true);
            assertThat(adapter.existsByCode("i1")).isTrue();
            verify(springDataRepository).existsByCode("I1");
        }

        @Test
        @DisplayName("count() should delegate to repository count")
        void shouldDelegateCount() {
            when(springDataRepository.count()).thenReturn(5L);
            assertThat(adapter.count()).isEqualTo(5L);
            verify(springDataRepository).count();
        }
    }

    @Nested
    @DisplayName("Delete Operations (Ticket 12 Resolution)")
    class DeleteTests {

        @Test
        @DisplayName("deleteById() should disassociate teams, delete competition and flush in strict order")
        void shouldDisassociateTeamsAndFlushOnDeleteById() {
            int competitionId = 42;

            adapter.deleteById(competitionId);

            // Verify the sequence: 1. delete from competition_teams, 2. delete from competitions, 3. flush()
            InOrder inOrder = inOrder(springDataCompetitionTeamRepository, springDataRepository);
            inOrder.verify(springDataCompetitionTeamRepository).deleteByCompetitionId(competitionId);
            inOrder.verify(springDataRepository).deleteById(competitionId);
            inOrder.verify(springDataRepository).flush();
        }

        @Test
        @DisplayName("deleteById() should catch DataIntegrityViolationException on flush and translate to DomainValidationException")
        void shouldTranslateDataIntegrityViolationExceptionWhenFlushFails() {
            int competitionId = 42;

            doThrow(new DataIntegrityViolationException("FK constraint failed on matches table"))
                    .when(springDataRepository).flush();

            assertThatThrownBy(() -> adapter.deleteById(competitionId))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Cannot delete competition with ID 42 because related records (matches/seasons) depend on it.")
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);

            // Ensure disassociation and deleteById were attempted before flush failed
            verify(springDataCompetitionTeamRepository).deleteByCompetitionId(competitionId);
            verify(springDataRepository).deleteById(competitionId);
            verify(springDataRepository).flush();
        }

        @Test
        @DisplayName("deleteById() should catch DataIntegrityViolationException on deleteById and translate to DomainValidationException")
        void shouldTranslateDataIntegrityViolationExceptionWhenDeleteByIdFails() {
            int competitionId = 99;

            doThrow(new DataIntegrityViolationException("Cannot delete parent row"))
                    .when(springDataRepository).deleteById(competitionId);

            assertThatThrownBy(() -> adapter.deleteById(competitionId))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Cannot delete competition with ID 99")
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);

            verify(springDataCompetitionTeamRepository).deleteByCompetitionId(competitionId);
            verify(springDataRepository, never()).flush();
        }
    }
}
