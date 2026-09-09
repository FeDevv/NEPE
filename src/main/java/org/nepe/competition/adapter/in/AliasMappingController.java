package org.nepe.competition.adapter.in;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.nepe.competition.domain.Team;
import org.nepe.competition.port.in.CreateTeamCommand;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.competition.port.in.ManageTeamUseCase;
import org.nepe.competition.port.in.MapTeamAliasCommand;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.NepeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Objects;

/**
 * Inbound Driving Adapter (JavaFX Modal Controller) for resolving unknown team names encountered during CSV ingestion.
 * <p>
 * Implements the Human-in-the-Loop pattern:
 * <ul>
 *     <li>Prompts the user when an unmapped team string is parsed from Football-Data datasets.</li>
 *     <li>Enables binding the raw name as an alias to an existing official team.</li>
 *     <li>Enables instant registration of a new official team in the database.</li>
 *     <li>Filters teams by competition with graceful fallback and real-time in-memory name search.</li>
 * </ul>
 */
@Controller
public class AliasMappingController {

    private static final Logger log = LoggerFactory.getLogger(AliasMappingController.class);

    private final ManageTeamUseCase manageTeamUseCase;
    private final ManageCompetitionUseCase manageCompetitionUseCase;

    // --- FXML Controls ---
    @FXML private Label lblRawTeamName;
    @FXML private RadioButton rbMapExisting;
    @FXML private RadioButton rbCreateNew;
    @FXML private ToggleGroup grpResolutionMode;
    @FXML private TextField txtSearchTeam;
    @FXML private CheckBox chkShowAllTeams;
    @FXML private ComboBox<Team> comboExistingTeams;
    @FXML private TextField txtNewTeamName;
    @FXML private Label lblError;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;

    // --- Filter & Observable Lists ---
    private final ObservableList<Team> masterTeams = FXCollections.observableArrayList();
    private final FilteredList<Team> filteredTeams = new FilteredList<>(masterTeams, p -> true);

    // --- Modal State ---
    private String rawTeamName;
    private String competitionCode;
    private boolean resolved = false;

    public AliasMappingController(ManageTeamUseCase manageTeamUseCase, ManageCompetitionUseCase manageCompetitionUseCase) {
        this.manageTeamUseCase = Objects.requireNonNull(manageTeamUseCase, "ManageTeamUseCase must not be null");
        this.manageCompetitionUseCase = Objects.requireNonNull(manageCompetitionUseCase, "ManageCompetitionUseCase must not be null");
    }

    @FXML
    public void initialize() {
        configureTeamDropdown();
        configureToggleBehavior();
        configureSearchAndFilters();
        loadExistingTeams();
    }

    private void configureTeamDropdown() {
        comboExistingTeams.setItems(filteredTeams);
        comboExistingTeams.setConverter(new StringConverter<>() {
            @Override
            public String toString(Team team) {
                return (team != null) ? team.getName() : "";
            }

            @Override
            public Team fromString(String string) {
                return null;
            }
        });
    }

    private void configureToggleBehavior() {
        grpResolutionMode.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            boolean mapExisting = (newToggle == rbMapExisting);
            comboExistingTeams.setDisable(!mapExisting);
            if (txtSearchTeam != null) {
                txtSearchTeam.setDisable(!mapExisting);
            }
            if (chkShowAllTeams != null) {
                chkShowAllTeams.setDisable(!mapExisting);
            }
            txtNewTeamName.setDisable(mapExisting);
        });

        // Initial default state: map to existing
        rbMapExisting.setSelected(true);
        comboExistingTeams.setDisable(false);
        if (txtSearchTeam != null) {
            txtSearchTeam.setDisable(false);
        }
        if (chkShowAllTeams != null) {
            chkShowAllTeams.setDisable(false);
        }
        txtNewTeamName.setDisable(true);
    }

    private void configureSearchAndFilters() {
        if (txtSearchTeam != null) {
            txtSearchTeam.textProperty().addListener((obs, oldVal, newVal) -> applySearchFilter(newVal));
        }
        if (chkShowAllTeams != null) {
            chkShowAllTeams.selectedProperty().addListener((obs, oldVal, newVal) -> loadExistingTeams());
        }
    }

    private void applySearchFilter(String query) {
        if (query == null || query.isBlank()) {
            filteredTeams.setPredicate(team -> true);
        } else {
            String lowerQuery = query.trim().toLowerCase();
            filteredTeams.setPredicate(team -> team != null && team.getName().toLowerCase().contains(lowerQuery));
        }

        if (comboExistingTeams != null && !filteredTeams.isEmpty()) {
            Team currentSelected = comboExistingTeams.getSelectionModel().getSelectedItem();
            if (currentSelected == null || !filteredTeams.contains(currentSelected)) {
                comboExistingTeams.getSelectionModel().selectFirst();
            }
        }
    }

    public void loadExistingTeams() {
        try {
            List<Team> teams;
            boolean showAll = chkShowAllTeams != null && chkShowAllTeams.isSelected();

            if (!showAll && competitionCode != null && !competitionCode.isBlank()) {
                try {
                    var competition = manageCompetitionUseCase.getCompetitionByCode(competitionCode);
                    if (competition != null) {
                        teams = manageTeamUseCase.getTeamsByCompetition(competition.getId());
                        if (teams.isEmpty()) {
                            log.info("No teams associated with competition '{}' (ID: {}). Gracefully falling back to all teams.",
                                    competitionCode, competition.getId());
                            teams = manageTeamUseCase.getAllTeams();
                        }
                    } else {
                        teams = manageTeamUseCase.getAllTeams();
                    }
                } catch (Exception ex) {
                    log.warn("Could not resolve competition '{}' or its teams, falling back to all teams: {}",
                            competitionCode, ex.getMessage());
                    teams = manageTeamUseCase.getAllTeams();
                }
            } else {
                teams = manageTeamUseCase.getAllTeams();
            }

            masterTeams.setAll(teams);
            applySearchFilter(txtSearchTeam != null ? txtSearchTeam.getText() : null);

            if (comboExistingTeams != null && !filteredTeams.isEmpty() && comboExistingTeams.getSelectionModel().getSelectedItem() == null) {
                comboExistingTeams.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            log.error("Failed to load existing teams for alias mapping", e);
            if (lblError != null) {
                lblError.setText("Errore caricamento squadre: " + e.getMessage());
            }
        }
    }

    /**
     * Initializes the modal context with the unmapped team name and competition metadata.
     *
     * @param rawTeamName     unrecognized name string from the CSV dataset
     * @param competitionCode competition code identifier (e.g., "I1")
     */
    public void setContext(String rawTeamName, String competitionCode) {
        this.rawTeamName = (rawTeamName != null) ? rawTeamName.trim() : "";
        this.competitionCode = (competitionCode != null) ? competitionCode.trim() : "";
        this.resolved = false;

        if (lblRawTeamName != null) {
            lblRawTeamName.setText(this.rawTeamName);
        }
        if (txtNewTeamName != null) {
            txtNewTeamName.setText(this.rawTeamName);
        }
        if (lblError != null) {
            lblError.setText("");
        }

        if (txtSearchTeam != null) {
            txtSearchTeam.clear();
        }
        if (chkShowAllTeams != null) {
            chkShowAllTeams.setSelected(false);
        }

        loadExistingTeams();
    }

    @FXML
    public void handleConfirm(ActionEvent event) {
        lblError.setText("");

        if (rawTeamName == null || rawTeamName.isBlank()) {
            lblError.setText("Nome squadra grezzo non specificato.");
            return;
        }

        try {
            if (rbMapExisting.isSelected()) {
                Team selectedTeam = comboExistingTeams.getSelectionModel().getSelectedItem();
                if (selectedTeam == null) {
                    throw new DomainValidationException("Seleziona una squadra esistente dal menu a tendina.");
                }

                log.info("Mapping raw alias '{}' to existing team '{}' (ID: {})",
                        rawTeamName, selectedTeam.getName(), selectedTeam.getId());
                manageTeamUseCase.mapAlias(new MapTeamAliasCommand(rawTeamName, selectedTeam.getId()));
            } else if (rbCreateNew.isSelected()) {
                String newTeamName = txtNewTeamName.getText();
                if (newTeamName == null || newTeamName.isBlank()) {
                    throw new DomainValidationException("Inserisci un nome ufficiale valido per la nuova squadra.");
                }

                log.info("Creating new official team '{}' from unmapped raw name '{}'",
                        newTeamName.trim(), rawTeamName);
                Team newTeam = manageTeamUseCase.createTeam(new CreateTeamCommand(newTeamName.trim()));

                // If the official name differs from the CSV spelling, map the alias automatically
                if (!rawTeamName.equalsIgnoreCase(newTeamName.trim())) {
                    manageTeamUseCase.mapAlias(new MapTeamAliasCommand(rawTeamName, newTeam.getId()));
                }
            }

            this.resolved = true;
            closeModal();
        } catch (NepeException e) {
            log.warn("Validation error during alias mapping: {}", e.getMessage());
            lblError.setText(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error resolving team alias", e);
            lblError.setText("Errore imprevisto: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        this.resolved = false;
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) btnConfirm.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    public boolean isResolved() {
        return resolved;
    }

    public String getRawTeamName() {
        return rawTeamName;
    }

    public String getCompetitionCode() {
        return competitionCode;
    }
}
