package org.nepe.competition.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.competition.domain.Team;
import org.nepe.competition.domain.TeamAlias;
import org.nepe.competition.port.in.CreateTeamCommand;
import org.nepe.competition.port.in.MapTeamAliasCommand;
import org.nepe.competition.port.in.RenameTeamCommand;
import org.nepe.competition.port.out.TeamAliasRepositoryPort;
import org.nepe.competition.port.out.TeamRepositoryPort;
import org.nepe.shared.exception.AliasMappingRequiredException;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.EntityNotFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TeamService Unit Tests")
class TeamServiceTest {

    private InMemoryTeamRepository teamRepository;
    private InMemoryTeamAliasRepository aliasRepository;
    private TeamService service;

    @BeforeEach
    void setUp() {
        teamRepository = new InMemoryTeamRepository();
        aliasRepository = new InMemoryTeamAliasRepository();
        service = new TeamService(teamRepository, aliasRepository);
    }

    @Nested
    @DisplayName("Constructor and Invariants")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NullPointerException when dependencies are null")
        void shouldThrowWhenDependenciesNull() {
            assertThatThrownBy(() -> new TeamService(null, aliasRepository))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("TeamRepositoryPort must not be null");

            assertThatThrownBy(() -> new TeamService(teamRepository, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("TeamAliasRepositoryPort must not be null");
        }
    }

    @Nested
    @DisplayName("Team CRUD and Query Tests")
    class TeamCrudTests {

        @Test
        @DisplayName("createTeam() should create and persist team")
        void shouldCreateTeam() {
            Team team = service.createTeam(new CreateTeamCommand("Inter"));

            assertThat(team.getId()).isPositive();
            assertThat(team.getName()).isEqualTo("Inter");
            assertThat(teamRepository.existsByName("Inter")).isTrue();
        }

        @Test
        @DisplayName("createTeam() should throw DomainValidationException if team already exists")
        void shouldThrowWhenTeamAlreadyExists() {
            service.createTeam(new CreateTeamCommand("Milan"));

            assertThatThrownBy(() -> service.createTeam(new CreateTeamCommand("milan")))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("renameTeam() should rename team and persist")
        void shouldRenameTeam() {
            Team team = service.createTeam(new CreateTeamCommand("Juventus FC"));

            Team renamed = service.renameTeam(new RenameTeamCommand(team.getId(), "Juventus"));

            assertThat(renamed.getName()).isEqualTo("Juventus");
            assertThat(teamRepository.findById(team.getId()).orElseThrow().getName()).isEqualTo("Juventus");
        }

        @Test
        @DisplayName("renameTeam() should throw DomainValidationException if new name collides with another team")
        void shouldThrowWhenNewNameCollides() {
            Team inter = service.createTeam(new CreateTeamCommand("Inter"));
            Team milan = service.createTeam(new CreateTeamCommand("Milan"));

            assertThatThrownBy(() -> service.renameTeam(new RenameTeamCommand(inter.getId(), "Milan")))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("searchTeams() should find teams by substring")
        void shouldSearchTeams() {
            service.createTeam(new CreateTeamCommand("Manchester City"));
            service.createTeam(new CreateTeamCommand("Manchester United"));
            service.createTeam(new CreateTeamCommand("Liverpool"));

            List<Team> results = service.searchTeams("manchester");

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("deleteTeam() should delete team and cascade delete its aliases")
        void shouldDeleteTeamAndAliases() {
            Team team = service.createTeam(new CreateTeamCommand("Arsenal"));
            service.mapAlias(new MapTeamAliasCommand("Gunners", team.getId()));

            service.deleteTeam(team.getId());

            assertThat(teamRepository.findById(team.getId())).isEmpty();
            assertThat(aliasRepository.findByTeamId(team.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Alias Mapping and Resolution Tests")
    class AliasTests {

        @Test
        @DisplayName("mapAlias() should map raw name to existing team")
        void shouldMapAlias() {
            Team team = service.createTeam(new CreateTeamCommand("Tottenham Hotspur"));

            TeamAlias alias = service.mapAlias(new MapTeamAliasCommand("Spurs", team.getId()));

            assertThat(alias.getAliasName()).isEqualTo("Spurs");
            assertThat(alias.getTeamId()).isEqualTo(team.getId());
        }

        @Test
        @DisplayName("mapAlias() should throw EntityNotFoundException if target team does not exist")
        void shouldThrowWhenMappingToNonExistentTeam() {
            assertThatThrownBy(() -> service.mapAlias(new MapTeamAliasCommand("Spurs", 999)))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Target team with ID 999 not found");
        }

        @Test
        @DisplayName("resolveTeamByRawName() should resolve via mapped alias")
        void shouldResolveViaAlias() {
            Team team = service.createTeam(new CreateTeamCommand("Manchester City"));
            service.mapAlias(new MapTeamAliasCommand("Man City", team.getId()));

            Team resolved = service.resolveTeamByRawName("Man City");

            assertThat(resolved.getId()).isEqualTo(team.getId());
            assertThat(resolved.getName()).isEqualTo("Manchester City");
        }

        @Test
        @DisplayName("resolveTeamByRawName() should fallback to direct official team name match")
        void shouldResolveViaDirectMatch() {
            Team team = service.createTeam(new CreateTeamCommand("Chelsea"));

            Team resolved = service.resolveTeamByRawName("Chelsea");

            assertThat(resolved.getId()).isEqualTo(team.getId());
        }

        @Test
        @DisplayName("resolveTeamByRawName() should throw AliasMappingRequiredException when unmapped")
        void shouldThrowAliasMappingRequiredException() {
            assertThatThrownBy(() -> service.resolveTeamByRawName("Unknown FC"))
                    .isInstanceOf(AliasMappingRequiredException.class)
                    .hasMessageContaining("Unknown FC");
        }

        @Test
        @DisplayName("deleteAlias() should remove alias")
        void shouldDeleteAlias() {
            Team team = service.createTeam(new CreateTeamCommand("Roma"));
            TeamAlias alias = service.mapAlias(new MapTeamAliasCommand("AS Roma", team.getId()));

            service.deleteAlias(alias.getId());

            assertThat(aliasRepository.findById(alias.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Competition Association Tests")
    class CompetitionAssociationTests {

        @Test
        @DisplayName("associateTeamToCompetition() should associate team with competition")
        void shouldAssociateTeamToCompetition() {
            Team team = service.createTeam(new CreateTeamCommand("Manchester City"));
            int compId = 1;

            service.associateTeamToCompetition(compId, team.getId());

            assertThat(service.isTeamAssociatedWithCompetition(compId, team.getId())).isTrue();
            List<Team> teams = service.getTeamsByCompetition(compId);
            assertThat(teams).hasSize(1);
            assertThat(teams.getFirst().getName()).isEqualTo("Manchester City");
        }

        @Test
        @DisplayName("associateTeamToCompetition() should throw EntityNotFoundException if team does not exist")
        void shouldThrowWhenAssociatingNonExistentTeam() {
            assertThatThrownBy(() -> service.associateTeamToCompetition(1, 999))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Team with ID 999 not found");
        }

        @Test
        @DisplayName("disassociateTeamFromCompetition() should remove association")
        void shouldDisassociateTeamFromCompetition() {
            Team team = service.createTeam(new CreateTeamCommand("Liverpool"));
            int compId = 1;

            service.associateTeamToCompetition(compId, team.getId());
            assertThat(service.isTeamAssociatedWithCompetition(compId, team.getId())).isTrue();

            service.disassociateTeamFromCompetition(compId, team.getId());
            assertThat(service.isTeamAssociatedWithCompetition(compId, team.getId())).isFalse();
            assertThat(service.getTeamsByCompetition(compId)).isEmpty();
        }

        @Test
        @DisplayName("getTeamsByCompetition() should return sorted teams belonging to competition")
        void shouldReturnSortedTeamsByCompetition() {
            Team chelsea = service.createTeam(new CreateTeamCommand("Chelsea"));
            Team arsenal = service.createTeam(new CreateTeamCommand("Arsenal"));
            Team inter = service.createTeam(new CreateTeamCommand("Inter"));

            int premierLeague = 1;
            int serieA = 2;

            service.associateTeamToCompetition(premierLeague, chelsea.getId());
            service.associateTeamToCompetition(premierLeague, arsenal.getId());
            service.associateTeamToCompetition(serieA, inter.getId());

            List<Team> plTeams = service.getTeamsByCompetition(premierLeague);
            assertThat(plTeams).extracting(Team::getName).containsExactly("Arsenal", "Chelsea");

            List<Team> saTeams = service.getTeamsByCompetition(serieA);
            assertThat(saTeams).extracting(Team::getName).containsExactly("Inter");
        }
    }

    /**
     * In-memory test double for {@link TeamRepositoryPort}.
     */
    private static class InMemoryTeamRepository implements TeamRepositoryPort {
        private final Map<Integer, Team> storage = new HashMap<>();
        private final Map<Integer, Set<Integer>> competitionTeams = new HashMap<>();
        private int idSequence = 1;

        @Override
        public Team save(Team team) {
            if (team.getId() == null) {
                team.assignId(idSequence++);
            }
            storage.put(team.getId(), team);
            return team;
        }

        @Override
        public Optional<Team> findById(int id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<Team> findByName(String name) {
            return storage.values().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(name.trim()))
                    .findFirst();
        }

        @Override
        public List<Team> findAll() {
            List<Team> list = new ArrayList<>(storage.values());
            list.sort(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER));
            return List.copyOf(list);
        }

        @Override
        public List<Team> searchByName(String query) {
            return storage.values().stream()
                    .filter(t -> t.getName().toLowerCase().contains(query.toLowerCase()))
                    .sorted(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        @Override
        public boolean existsByName(String name) {
            return storage.values().stream()
                    .anyMatch(t -> t.getName().equalsIgnoreCase(name.trim()));
        }

        @Override
        public void deleteById(int id) {
            storage.remove(id);
            competitionTeams.values().forEach(set -> set.remove(id));
        }

        @Override
        public long count() {
            return storage.size();
        }

        @Override
        public List<Team> findByCompetitionId(int competitionId) {
            Set<Integer> teamIds = competitionTeams.getOrDefault(competitionId, Set.of());
            return storage.values().stream()
                    .filter(t -> teamIds.contains(t.getId()))
                    .sorted(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        @Override
        public void associateTeamToCompetition(int competitionId, int teamId) {
            competitionTeams.computeIfAbsent(competitionId, k -> new HashSet<>()).add(teamId);
        }

        @Override
        public void disassociateTeamFromCompetition(int competitionId, int teamId) {
            Set<Integer> teams = competitionTeams.get(competitionId);
            if (teams != null) {
                teams.remove(teamId);
            }
        }

        @Override
        public boolean isTeamAssociatedWithCompetition(int competitionId, int teamId) {
            return competitionTeams.getOrDefault(competitionId, Set.of()).contains(teamId);
        }
    }

    /**
     * In-memory test double for {@link TeamAliasRepositoryPort}.
     */
    private static class InMemoryTeamAliasRepository implements TeamAliasRepositoryPort {
        private final Map<Integer, TeamAlias> storage = new HashMap<>();
        private int idSequence = 1;

        @Override
        public TeamAlias save(TeamAlias teamAlias) {
            if (teamAlias.getId() == null) {
                teamAlias.assignId(idSequence++);
            }
            storage.put(teamAlias.getId(), teamAlias);
            return teamAlias;
        }

        @Override
        public Optional<TeamAlias> findById(int id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<TeamAlias> findByAliasName(String aliasName) {
            return storage.values().stream()
                    .filter(a -> a.getAliasName().equalsIgnoreCase(aliasName.trim()))
                    .findFirst();
        }

        @Override
        public List<TeamAlias> findByTeamId(int teamId) {
            return storage.values().stream()
                    .filter(a -> a.getTeamId().equals(teamId))
                    .toList();
        }

        @Override
        public List<TeamAlias> findAll() {
            return List.copyOf(storage.values());
        }

        @Override
        public boolean existsByAliasName(String aliasName) {
            return storage.values().stream()
                    .anyMatch(a -> a.getAliasName().equalsIgnoreCase(aliasName.trim()));
        }

        @Override
        public void deleteById(int id) {
            storage.remove(id);
        }

        @Override
        public void deleteByTeamId(int teamId) {
            storage.values().removeIf(a -> a.getTeamId().equals(teamId));
        }

        @Override
        public long count() {
            return storage.size();
        }
    }
}
