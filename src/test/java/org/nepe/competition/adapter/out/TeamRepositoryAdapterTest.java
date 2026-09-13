package org.nepe.competition.adapter.out;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.nepe.competition.domain.Team;
import org.nepe.shared.exception.DomainValidationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("TeamRepositoryAdapter Unit Tests")
class TeamRepositoryAdapterTest {

    private SpringDataTeamRepository springDataRepository;
    private SpringDataCompetitionTeamRepository competitionTeamRepository;
    private TeamMapper mapper;
    private TeamRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataTeamRepository.class);
        competitionTeamRepository = mock(SpringDataCompetitionTeamRepository.class);
        mapper = new TeamMapper();
        adapter = new TeamRepositoryAdapter(springDataRepository, competitionTeamRepository, mapper);
    }

    @Nested
    @DisplayName("Constructor and Invariant Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NullPointerException when dependencies are null")
        void shouldRejectNullDependencies() {
            assertThatThrownBy(() -> new TeamRepositoryAdapter(null, competitionTeamRepository, mapper))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("SpringDataTeamRepository must not be null");

            assertThatThrownBy(() -> new TeamRepositoryAdapter(springDataRepository, null, mapper))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("SpringDataCompetitionTeamRepository must not be null");

            assertThatThrownBy(() -> new TeamRepositoryAdapter(springDataRepository, competitionTeamRepository, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("TeamMapper must not be null");
        }
    }

    @Nested
    @DisplayName("Save and Mutation Tests")
    class SaveTests {

        @Test
        @DisplayName("save() should throw DomainValidationException when team is null")
        void shouldRejectNullTeamOnSave() {
            assertThatThrownBy(() -> adapter.save(null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Team to save cannot be null");
        }

        @Test
        @DisplayName("save() should map domain entity to JPA, persist, and return reconstructed domain model")
        void shouldSaveSuccessfully() {
            Team domain = Team.create("Juventus");
            TeamJpaEntity savedEntity = new TeamJpaEntity(1, "Juventus");

            when(springDataRepository.save(any(TeamJpaEntity.class))).thenReturn(savedEntity);

            Team result = adapter.save(domain);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getName()).isEqualTo("Juventus");
            verify(springDataRepository).save(any(TeamJpaEntity.class));
        }

        @Test
        @DisplayName("save() should translate DataIntegrityViolationException into DomainValidationException")
        void shouldTranslateDataIntegrityViolationExceptionOnSave() {
            Team domain = Team.create("Milan");

            when(springDataRepository.save(any(TeamJpaEntity.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate team name"));

            assertThatThrownBy(() -> adapter.save(domain))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Unable to persist team 'Milan'")
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Query and Retrieval Tests")
    class QueryTests {

        @Test
        @DisplayName("findById() should return mapped domain entity when found")
        void shouldFindById() {
            TeamJpaEntity jpa = new TeamJpaEntity(10, "Inter");
            when(springDataRepository.findById(10)).thenReturn(Optional.of(jpa));

            Optional<Team> result = adapter.findById(10);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(10);
            assertThat(result.get().getName()).isEqualTo("Inter");
        }

        @Test
        @DisplayName("findByName() should return empty when name is null or blank")
        void shouldReturnEmptyForBlankName() {
            assertThat(adapter.findByName(null)).isEmpty();
            assertThat(adapter.findByName("")).isEmpty();
            assertThat(adapter.findByName("   ")).isEmpty();
            verifyNoInteractions(springDataRepository);
        }

        @Test
        @DisplayName("findByName() should trim name before lookup")
        void shouldFindByNameWithTrimming() {
            TeamJpaEntity jpa = new TeamJpaEntity(5, "Arsenal");
            when(springDataRepository.findByNameIgnoreCase("Arsenal")).thenReturn(Optional.of(jpa));

            Optional<Team> result = adapter.findByName("  Arsenal  ");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Arsenal");
            verify(springDataRepository).findByNameIgnoreCase("Arsenal");
        }

        @Test
        @DisplayName("findAll() should return all teams mapped to domain")
        void shouldFindAll() {
            List<TeamJpaEntity> list = List.of(
                    new TeamJpaEntity(1, "Arsenal"),
                    new TeamJpaEntity(2, "Chelsea")
            );
            when(springDataRepository.findAllByOrderByNameAsc()).thenReturn(list);

            List<Team> result = adapter.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Arsenal");
            assertThat(result.get(1).getName()).isEqualTo("Chelsea");
        }

        @Test
        @DisplayName("searchByName() should return empty when query is null or blank")
        void shouldReturnEmptyForBlankSearchQuery() {
            assertThat(adapter.searchByName(null)).isEmpty();
            assertThat(adapter.searchByName("")).isEmpty();
            assertThat(adapter.searchByName("   ")).isEmpty();
            verifyNoInteractions(springDataRepository);
        }

        @Test
        @DisplayName("searchByName() should trim query and delegate to repository")
        void shouldSearchByNameWithTrimming() {
            List<TeamJpaEntity> list = List.of(new TeamJpaEntity(1, "Real Madrid"));
            when(springDataRepository.findByNameContainingIgnoreCaseOrderByNameAsc("Real")).thenReturn(list);

            List<Team> result = adapter.searchByName("  Real  ");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Real Madrid");
            verify(springDataRepository).findByNameContainingIgnoreCaseOrderByNameAsc("Real");
        }

        @Test
        @DisplayName("existsByName() should return false for null or blank input")
        void shouldReturnFalseForBlankExistsQuery() {
            assertThat(adapter.existsByName(null)).isFalse();
            assertThat(adapter.existsByName("")).isFalse();
            assertThat(adapter.existsByName("   ")).isFalse();
            verifyNoInteractions(springDataRepository);
        }

        @Test
        @DisplayName("existsByName() should trim name and delegate to repository")
        void shouldCheckExistsByNameWithTrimming() {
            when(springDataRepository.existsByNameIgnoreCase("Barcelona")).thenReturn(true);
            assertThat(adapter.existsByName("  Barcelona  ")).isTrue();
            verify(springDataRepository).existsByNameIgnoreCase("Barcelona");
        }

        @Test
        @DisplayName("count() should delegate to repository count")
        void shouldDelegateCount() {
            when(springDataRepository.count()).thenReturn(20L);
            assertThat(adapter.count()).isEqualTo(20L);
            verify(springDataRepository).count();
        }

        @Test
        @DisplayName("findByCompetitionId() should delegate to repository and map to domain")
        void shouldFindByCompetitionId() {
            List<TeamJpaEntity> list = List.of(
                    new TeamJpaEntity(10, "Liverpool"),
                    new TeamJpaEntity(11, "Manchester City")
            );
            when(springDataRepository.findByCompetitionId(1)).thenReturn(list);

            List<Team> result = adapter.findByCompetitionId(1);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Liverpool");
            assertThat(result.get(1).getName()).isEqualTo("Manchester City");
        }

        @Test
        @DisplayName("isTeamAssociatedWithCompetition() should delegate to competitionTeamRepository")
        void shouldCheckIsTeamAssociatedWithCompetition() {
            when(competitionTeamRepository.existsByCompetitionIdAndTeamId(1, 10)).thenReturn(true);
            assertThat(adapter.isTeamAssociatedWithCompetition(1, 10)).isTrue();
            verify(competitionTeamRepository).existsByCompetitionIdAndTeamId(1, 10);
        }
    }

    @Nested
    @DisplayName("Association and Disassociation Tests")
    class AssociationTests {

        @Test
        @DisplayName("associateTeamToCompetition() should save junction entity when not already associated")
        void shouldAssociateTeamWhenNotExists() {
            when(competitionTeamRepository.existsByCompetitionIdAndTeamId(1, 10)).thenReturn(false);

            adapter.associateTeamToCompetition(1, 10);

            verify(competitionTeamRepository).save(any(CompetitionTeamJpaEntity.class));
        }

        @Test
        @DisplayName("associateTeamToCompetition() should do nothing when association already exists")
        void shouldNotAssociateTeamWhenAlreadyExists() {
            when(competitionTeamRepository.existsByCompetitionIdAndTeamId(1, 10)).thenReturn(true);

            adapter.associateTeamToCompetition(1, 10);

            verify(competitionTeamRepository, never()).save(any(CompetitionTeamJpaEntity.class));
        }

        @Test
        @DisplayName("associateTeamToCompetition() should gracefully ignore DataIntegrityViolationException on concurrent save")
        void shouldIgnoreDataIntegrityViolationOnConcurrentSave() {
            when(competitionTeamRepository.existsByCompetitionIdAndTeamId(1, 10)).thenReturn(false);
            when(competitionTeamRepository.save(any(CompetitionTeamJpaEntity.class)))
                    .thenThrow(new DataIntegrityViolationException("Concurrent duplicate key"));

            adapter.associateTeamToCompetition(1, 10);

            verify(competitionTeamRepository).save(any(CompetitionTeamJpaEntity.class));
        }

        @Test
        @DisplayName("disassociateTeamFromCompetition() should delegate to competitionTeamRepository")
        void shouldDisassociateTeam() {
            adapter.disassociateTeamFromCompetition(1, 10);
            verify(competitionTeamRepository).deleteByCompetitionIdAndTeamId(1, 10);
        }
    }

    @Nested
    @DisplayName("Delete Operations (Ticket 18 Resolution)")
    class DeleteTests {

        @Test
        @DisplayName("deleteById() should disassociate from competition_teams, delete team and flush in strict order")
        void shouldDisassociateCompetitionsAndFlushOnDeleteById() {
            int teamId = 42;

            adapter.deleteById(teamId);

            // Verify strict order: 1. competitionTeamRepository.deleteByTeamId, 2. springDataRepository.deleteById, 3. springDataRepository.flush()
            InOrder inOrder = inOrder(competitionTeamRepository, springDataRepository);
            inOrder.verify(competitionTeamRepository).deleteByTeamId(teamId);
            inOrder.verify(springDataRepository).deleteById(teamId);
            inOrder.verify(springDataRepository).flush();
        }

        @Test
        @DisplayName("deleteById() should catch DataIntegrityViolationException on flush and translate to DomainValidationException")
        void shouldTranslateDataIntegrityViolationExceptionWhenFlushFails() {
            int teamId = 42;

            doThrow(new DataIntegrityViolationException("FK constraint failed on matches table"))
                    .when(springDataRepository).flush();

            assertThatThrownBy(() -> adapter.deleteById(teamId))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Cannot delete team with ID 42 because associated records (matches/aliases) depend on it.")
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);

            // Ensure disassociation and deleteById were attempted before flush failed
            verify(competitionTeamRepository).deleteByTeamId(teamId);
            verify(springDataRepository).deleteById(teamId);
            verify(springDataRepository).flush();
        }

        @Test
        @DisplayName("deleteById() should catch DataIntegrityViolationException on deleteById and translate to DomainValidationException")
        void shouldTranslateDataIntegrityViolationExceptionWhenDeleteByIdFails() {
            int teamId = 99;

            doThrow(new DataIntegrityViolationException("Cannot delete parent row"))
                    .when(springDataRepository).deleteById(teamId);

            assertThatThrownBy(() -> adapter.deleteById(teamId))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Cannot delete team with ID 99")
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);

            verify(competitionTeamRepository).deleteByTeamId(teamId);
            verify(springDataRepository, never()).flush();
        }
    }
}
