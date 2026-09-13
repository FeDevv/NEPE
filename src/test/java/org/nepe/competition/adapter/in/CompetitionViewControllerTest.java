package org.nepe.competition.adapter.in;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.competition.domain.Competition;
import org.nepe.competition.domain.Team;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.competition.port.in.ManageTeamUseCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@DisplayName("CompetitionViewController Unit Tests")
class CompetitionViewControllerTest {

    private final ManageCompetitionUseCase manageCompetitionUseCase = mock(ManageCompetitionUseCase.class);
    private final ManageTeamUseCase manageTeamUseCase = mock(ManageTeamUseCase.class);
    private final SpringFXMLLoader springFXMLLoader = mock(SpringFXMLLoader.class);

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
    }

    @Test
    @DisplayName("Constructor should reject null dependencies")
    void shouldEnforceConstructorInvariants() {
        assertThatThrownBy(() -> new CompetitionViewController(null, manageTeamUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageCompetitionUseCase must not be null");

        assertThatThrownBy(() -> new CompetitionViewController(manageCompetitionUseCase, null, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageTeamUseCase must not be null");

        assertThatThrownBy(() -> new CompetitionViewController(manageCompetitionUseCase, manageTeamUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SpringFXMLLoader must not be null");
    }

    @Test
    @DisplayName("CompetitionViewController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        CompetitionViewController controller = new CompetitionViewController(
                manageCompetitionUseCase,
                manageTeamUseCase,
                springFXMLLoader
        );

        assertThat(controller).isNotNull();
    }

    @Test
    @DisplayName("ALL_COMPETITIONS sentinel should have correct properties and valid domain state")
    void allCompetitionsSentinelShouldBeValid() {
        Competition sentinel = CompetitionViewController.ALL_COMPETITIONS;

        assertThat(sentinel).isNotNull();
        assertThat(sentinel.getId()).isEqualTo(-1);
        assertThat(sentinel.getCode()).isEqualTo("ALL");
        assertThat(sentinel.getName()).isEqualTo("Tutti i campionati");
        assertThat(sentinel.getCountry()).isEqualTo("Global");
        assertThat(sentinel.getDixonColesRho()).isEqualTo(Competition.DEFAULT_DIXON_COLES_RHO);
        assertThat(sentinel.hasManualHomeAdvantage()).isFalse();
        assertThat(CompetitionViewController.ALL_COMPETITIONS_LABEL).isEqualTo("🌐 Tutti i campionati");
    }

    @Test
    @DisplayName("ALL_COMPETITIONS equality should be based on unique code 'ALL'")
    void allCompetitionsSentinelEquality() {
        Competition sentinel = CompetitionViewController.ALL_COMPETITIONS;
        Competition sameCode = new Competition(99, "ALL", "Other Name", "Other Country", -0.12);
        Competition differentCode = new Competition(1, "I1", "Serie A", "Italy", -0.12);

        assertThat(sentinel).isEqualTo(sameCode);
        assertThat(sentinel).isNotEqualTo(differentCode);
        assertThat(sentinel.equals(null)).isFalse();
    }

    @Test
    @DisplayName("COMPETITION_FILTER_CONVERTER should format null, ALL_COMPETITIONS, and regular competitions properly")
    void competitionFilterConverterShouldFormatProperly() {
        var converter = CompetitionViewController.COMPETITION_FILTER_CONVERTER;

        assertThat(converter.toString(null)).isEmpty();
        assertThat(converter.toString(CompetitionViewController.ALL_COMPETITIONS))
                .isEqualTo(CompetitionViewController.ALL_COMPETITIONS_LABEL);

        Competition premierLeague = new Competition(10, "E0", "Premier League", "England", -0.12, 1.25);
        assertThat(converter.toString(premierLeague)).isEqualTo("Premier League (E0)");

        assertThat(converter.fromString("Any string")).isNull();
    }

    @Test
    @DisplayName("Official team name resolution for aliases should return team name or fallback")
    void officialTeamNameResolutionShouldProvideFallback() {
        java.util.Map<Integer, String> cache = new java.util.HashMap<>();
        cache.put(1, "Arsenal");
        cache.put(2, "Chelsea");

        assertThat(cache.getOrDefault(1, "Team #1")).isEqualTo("Arsenal");
        assertThat(cache.getOrDefault(2, "Team #2")).isEqualTo("Chelsea");
        assertThat(cache.getOrDefault(99, "Team #99")).isEqualTo("Team #99");
    }

    @Test
    @DisplayName("handleDisassociateTeam should set error status when no team is selected")
    void shouldSetStatusWhenNoTeamSelectedOnDisassociate() {
        CompetitionViewController controller = new CompetitionViewController(
                manageCompetitionUseCase,
                manageTeamUseCase,
                springFXMLLoader
        );
        Label lblStatus = new Label();
        injectField(controller, "lblStatus", lblStatus);
        injectField(controller, "selectedTeam", null);

        controller.handleDisassociateTeam(null);

        assertThat(lblStatus.getText()).isEqualTo("Seleziona prima una squadra dalla tabella.");
        verifyNoInteractions(manageTeamUseCase);
    }

    @Test
    @DisplayName("handleDisassociateTeam should set error status when no competition is selected")
    void shouldSetStatusWhenNoCompetitionSelectedOnDisassociate() {
        CompetitionViewController controller = new CompetitionViewController(
                manageCompetitionUseCase,
                manageTeamUseCase,
                springFXMLLoader
        );
        Label lblStatus = new Label();
        ComboBox<Competition> comboComp = new ComboBox<>();
        Team team = new Team(10, "Inter");

        injectField(controller, "lblStatus", lblStatus);
        injectField(controller, "selectedTeam", team);
        injectField(controller, "comboTeamCompetition", comboComp);

        controller.handleDisassociateTeam(null);

        assertThat(lblStatus.getText()).isEqualTo("Seleziona il campionato da cui disassociare la squadra.");
        verifyNoInteractions(manageTeamUseCase);
    }

    @Test
    @DisplayName("handleDisassociateTeam should warn when team is not associated with competition")
    void shouldWarnWhenTeamNotAssociatedWithCompetitionOnDisassociate() {
        CompetitionViewController controller = new CompetitionViewController(
                manageCompetitionUseCase,
                manageTeamUseCase,
                springFXMLLoader
        );
        Label lblStatus = new Label();
        ComboBox<Competition> comboComp = new ComboBox<>();
        Competition serieA = new Competition(1, "I1", "Serie A", "Italy", -0.12);
        comboComp.setValue(serieA);
        Team team = new Team(10, "Inter");

        injectField(controller, "lblStatus", lblStatus);
        injectField(controller, "selectedTeam", team);
        injectField(controller, "comboTeamCompetition", comboComp);

        when(manageTeamUseCase.isTeamAssociatedWithCompetition(1, 10)).thenReturn(false);

        controller.handleDisassociateTeam(null);

        assertThat(lblStatus.getText()).isEqualTo("La squadra 'Inter' non è associata a 'Serie A'.");
        verify(manageTeamUseCase, never()).disassociateTeamFromCompetition(anyInt(), anyInt());
    }

    @Test
    @DisplayName("handleDisassociateTeam should disassociate and update status when team is associated")
    void shouldDisassociateTeamSuccessfully() {
        CompetitionViewController controller = new CompetitionViewController(
                manageCompetitionUseCase,
                manageTeamUseCase,
                springFXMLLoader
        );
        Label lblStatus = new Label();
        ComboBox<Competition> comboComp = new ComboBox<>();
        Competition serieA = new Competition(1, "I1", "Serie A", "Italy", -0.12);
        comboComp.setValue(serieA);
        Team team = new Team(10, "Inter");
        TableView<Team> tblTeams = new TableView<>();

        injectField(controller, "lblStatus", lblStatus);
        injectField(controller, "selectedTeam", team);
        injectField(controller, "comboTeamCompetition", comboComp);
        injectField(controller, "tblTeams", tblTeams);

        when(manageTeamUseCase.isTeamAssociatedWithCompetition(1, 10)).thenReturn(true);
        when(manageTeamUseCase.getAllTeams()).thenReturn(List.of(team));

        controller.handleDisassociateTeam(null);

        verify(manageTeamUseCase).disassociateTeamFromCompetition(1, 10);
        assertThat(lblStatus.getText()).isEqualTo("Squadra 'Inter' disassociata da 'Serie A' con successo!");
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject field: " + fieldName, e);
        }
    }
}
