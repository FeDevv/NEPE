package org.nepe.competition.adapter.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nepe.competition.domain.Competition;
import org.nepe.competition.domain.Team;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.competition.port.in.ManageTeamUseCase;
import org.nepe.shared.exception.EntityNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("AliasMappingController Unit Tests")
class AliasMappingControllerTest {

    private final ManageTeamUseCase manageTeamUseCase = mock(ManageTeamUseCase.class);
    private final ManageCompetitionUseCase manageCompetitionUseCase = mock(ManageCompetitionUseCase.class);

    @Test
    @DisplayName("Constructor should reject null ManageTeamUseCase")
    void shouldRejectNullManageTeamUseCase() {
        assertThatThrownBy(() -> new AliasMappingController(null, manageCompetitionUseCase))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageTeamUseCase must not be null");
    }

    @Test
    @DisplayName("Constructor should reject null ManageCompetitionUseCase")
    void shouldRejectNullManageCompetitionUseCase() {
        assertThatThrownBy(() -> new AliasMappingController(manageTeamUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageCompetitionUseCase must not be null");
    }

    @Test
    @DisplayName("AliasMappingController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        AliasMappingController controller = new AliasMappingController(manageTeamUseCase, manageCompetitionUseCase);

        assertThat(controller).isNotNull();
        assertThat(controller.isResolved()).isFalse();
    }

    @Test
    @DisplayName("setContext should trim and store raw team name and competition code")
    void shouldSetContextProperly() {
        AliasMappingController controller = new AliasMappingController(manageTeamUseCase, manageCompetitionUseCase);

        controller.setContext("  Arsenal FC  ", "  E0  ");

        assertThat(controller.getRawTeamName()).isEqualTo("Arsenal FC");
        assertThat(controller.getCompetitionCode()).isEqualTo("E0");
        assertThat(controller.isResolved()).isFalse();
    }

    @Test
    @DisplayName("setContext should handle null inputs gracefully")
    void shouldHandleNullInputsInSetContext() {
        AliasMappingController controller = new AliasMappingController(manageTeamUseCase, manageCompetitionUseCase);

        controller.setContext(null, null);

        assertThat(controller.getRawTeamName()).isEmpty();
        assertThat(controller.getCompetitionCode()).isEmpty();
        assertThat(controller.isResolved()).isFalse();
    }

    @Test
    @DisplayName("loadExistingTeams should load competition teams when competition is found")
    void shouldLoadCompetitionTeamsWhenFound() {
        org.nepe.competition.domain.Competition competition =
                new org.nepe.competition.domain.Competition(10, "E0", "Premier League", "England", -0.12, 1.25);
        org.nepe.competition.domain.Team team1 = new org.nepe.competition.domain.Team(1, "Arsenal");
        org.nepe.competition.domain.Team team2 = new org.nepe.competition.domain.Team(2, "Chelsea");

        org.mockito.Mockito.when(manageCompetitionUseCase.getCompetitionByCode("E0")).thenReturn(competition);
        org.mockito.Mockito.when(manageTeamUseCase.getTeamsByCompetition(10)).thenReturn(java.util.List.of(team1, team2));

        AliasMappingController controller = new AliasMappingController(manageTeamUseCase, manageCompetitionUseCase);
        controller.setContext("Gunners", "E0");

        org.mockito.Mockito.verify(manageCompetitionUseCase).getCompetitionByCode("E0");
        org.mockito.Mockito.verify(manageTeamUseCase).getTeamsByCompetition(10);
    }

    @Test
    @DisplayName("loadExistingTeams should fallback to all teams when competition has no registered teams")
    void shouldFallbackToAllTeamsWhenEmpty() {
        Competition competition =
                new Competition(20, "D1", "Bundesliga", "Germany", -0.12, 1.20);
        Team team = new Team(5, "Bayern");

        Mockito.when(manageCompetitionUseCase.getCompetitionByCode("D1")).thenReturn(competition);
        Mockito.when(manageTeamUseCase.getTeamsByCompetition(20)).thenReturn(List.of());
        Mockito.when(manageTeamUseCase.getAllTeams()).thenReturn(List.of(team));

        AliasMappingController controller = new AliasMappingController(manageTeamUseCase, manageCompetitionUseCase);
        controller.setContext("FCB", "D1");

        Mockito.verify(manageCompetitionUseCase).getCompetitionByCode("D1");
        Mockito.verify(manageTeamUseCase).getTeamsByCompetition(20);
        Mockito.verify(manageTeamUseCase).getAllTeams();
    }

    @Test
    @DisplayName("loadExistingTeams should fallback to all teams when competition resolution throws an exception")
    void shouldFallbackToAllTeamsOnCompetitionException() {
        Mockito.when(manageCompetitionUseCase.getCompetitionByCode("UNKNOWN"))
                .thenThrow(new EntityNotFoundException("Competition not found"));
        Team team = new Team(99, "Generic Team");
        Mockito.when(manageTeamUseCase.getAllTeams()).thenReturn(List.of(team));

        AliasMappingController controller = new AliasMappingController(manageTeamUseCase, manageCompetitionUseCase);
        controller.setContext("SomeTeam", "UNKNOWN");

        Mockito.verify(manageCompetitionUseCase).getCompetitionByCode("UNKNOWN");
        Mockito.verify(manageTeamUseCase).getAllTeams();
    }
}
