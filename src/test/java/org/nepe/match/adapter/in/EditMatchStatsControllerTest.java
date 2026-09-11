package org.nepe.match.adapter.in;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.match.domain.MatchState;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.match.port.out.MatchDetailsDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("EditMatchStatsController Unit Tests")
class EditMatchStatsControllerTest {

    private final ManageMatchUseCase manageMatchUseCase = mock(ManageMatchUseCase.class);

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
    }

    @Test
    @DisplayName("Constructor should reject null ManageMatchUseCase")
    void shouldEnforceConstructorInvariants() {
        assertThatThrownBy(() -> new EditMatchStatsController(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMatchUseCase must not be null");
    }

    @Test
    @DisplayName("EditMatchStatsController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        EditMatchStatsController controller = new EditMatchStatsController(manageMatchUseCase);

        assertThat(controller).isNotNull();
        assertThat(controller.isStatsUpdated()).isFalse();
        assertThat(controller.getChkMarkAsFinished()).isNull();
    }

    @Test
    @DisplayName("getChkMarkAsFinished and setChkMarkAsFinished should work properly")
    void shouldGetAndSetChkMarkAsFinished() {
        EditMatchStatsController controller = new EditMatchStatsController(manageMatchUseCase);
        CheckBox chk = new CheckBox();

        controller.setChkMarkAsFinished(chk);

        assertThat(controller.getChkMarkAsFinished()).isSameAs(chk);
    }

    @Test
    @DisplayName("setMatch should handle null gracefully without throwing")
    void shouldHandleNullMatchInSetMatch() {
        EditMatchStatsController controller = new EditMatchStatsController(manageMatchUseCase);

        controller.setMatch(null);

        assertThat(controller.isStatsUpdated()).isFalse();
    }

    @Test
    @DisplayName("setMatch should configure chkMarkAsFinished for SCHEDULED match")
    void shouldConfigureChkMarkAsFinishedForScheduledMatch() {
        EditMatchStatsController controller = new EditMatchStatsController(manageMatchUseCase);
        initAllControls(controller);

        MatchDetailsDTO match = createTestMatchDto(10, MatchState.SCHEDULED);
        controller.setMatch(match);

        assertThat(controller.getChkMarkAsFinished().isSelected()).isFalse();
        assertThat(controller.getChkMarkAsFinished().isDisabled()).isFalse();
    }

    @Test
    @DisplayName("setMatch should configure chkMarkAsFinished as selected and disabled for FINISHED match")
    void shouldConfigureChkMarkAsFinishedForFinishedMatch() {
        EditMatchStatsController controller = new EditMatchStatsController(manageMatchUseCase);
        initAllControls(controller);

        MatchDetailsDTO match = createTestMatchDto(11, MatchState.FINISHED);
        controller.setMatch(match);

        assertThat(controller.getChkMarkAsFinished().isSelected()).isTrue();
        assertThat(controller.getChkMarkAsFinished().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("setMatch should configure chkMarkAsFinished as disabled for CANCELLED match")
    void shouldConfigureChkMarkAsFinishedForCancelledMatch() {
        EditMatchStatsController controller = new EditMatchStatsController(manageMatchUseCase);
        initAllControls(controller);

        MatchDetailsDTO match = createTestMatchDto(12, MatchState.CANCELLED);
        controller.setMatch(match);

        assertThat(controller.getChkMarkAsFinished().isSelected()).isFalse();
        assertThat(controller.getChkMarkAsFinished().isDisabled()).isTrue();
    }

    @Test
    @DisplayName("handleSave should call markAsFinished when chkMarkAsFinished is selected")
    void shouldCallMarkAsFinishedWhenCheckboxIsSelected() {
        EditMatchStatsController controller = new EditMatchStatsController(manageMatchUseCase);
        initAllControls(controller);

        MatchDetailsDTO match = createTestMatchDto(20, MatchState.SCHEDULED);
        controller.setMatch(match);

        // User enters score 2-1 and checks the box
        getTextField(controller, "txtHomeScore").setText("2");
        getTextField(controller, "txtAwayScore").setText("1");
        controller.getChkMarkAsFinished().setSelected(true);

        controller.handleSave(null);

        verify(manageMatchUseCase).updateStatistics(any());
        verify(manageMatchUseCase).markAsFinished(20);
        assertThat(controller.isStatsUpdated()).isTrue();
    }

    @Test
    @DisplayName("handleSave should NOT call markAsFinished when chkMarkAsFinished is unselected")
    void shouldNotCallMarkAsFinishedWhenCheckboxIsUnselected() {
        EditMatchStatsController controller = new EditMatchStatsController(manageMatchUseCase);
        initAllControls(controller);

        MatchDetailsDTO match = createTestMatchDto(21, MatchState.SCHEDULED);
        controller.setMatch(match);

        getTextField(controller, "txtHomeScore").setText("1");
        getTextField(controller, "txtAwayScore").setText("0");
        controller.getChkMarkAsFinished().setSelected(false);

        controller.handleSave(null);

        verify(manageMatchUseCase).updateStatistics(any());
        verify(manageMatchUseCase, org.mockito.Mockito.never()).markAsFinished(21);
        assertThat(controller.isStatsUpdated()).isTrue();
    }

    private void initAllControls(EditMatchStatsController controller) {
        injectField(controller, "lblMatchTitle", new Label());
        injectField(controller, "lblMatchInfo", new Label());
        injectField(controller, "lblHomeHeader", new Label());
        injectField(controller, "lblAwayHeader", new Label());
        injectField(controller, "txtHomeScore", new TextField());
        injectField(controller, "txtAwayScore", new TextField());
        injectField(controller, "txtHomeShots", new TextField());
        injectField(controller, "txtAwayShots", new TextField());
        injectField(controller, "txtHomeShotsOnTarget", new TextField());
        injectField(controller, "txtAwayShotsOnTarget", new TextField());
        injectField(controller, "txtHomeRedCards", new TextField());
        injectField(controller, "txtAwayRedCards", new TextField());
        injectField(controller, "txtManualHomeXg", new TextField());
        injectField(controller, "txtManualAwayXg", new TextField());
        injectField(controller, "lblPreviewHomeXg", new Label());
        injectField(controller, "lblPreviewAwayXg", new Label());
        injectField(controller, "lblError", new Label());
        injectField(controller, "chkMarkAsFinished", new CheckBox());
        injectField(controller, "btnCancel", new Button());
        injectField(controller, "btnSave", new Button());
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TextField getTextField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (TextField) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MatchDetailsDTO createTestMatchDto(int matchId, MatchState state) {
        return new MatchDetailsDTO(
                matchId,
                java.time.Instant.parse("2026-09-10T18:00:00Z"),
                state,
                false,
                null, null,
                null, null,
                null, null,
                0, 0,
                null, null,
                2.00, 3.20, 3.80,
                false, false, false, false, false,
                1.0, 1.0, 1.0, 1.0,
                0,
                1, "I1", "Serie A", "Italy", -0.12,
                1, "2025/2026",
                10, "Inter",
                20, "Milan"
        );
    }
}
