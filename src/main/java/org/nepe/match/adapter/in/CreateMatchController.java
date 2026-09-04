package org.nepe.match.adapter.in;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.nepe.competition.domain.Competition;
import org.nepe.competition.domain.Season;
import org.nepe.competition.domain.Team;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.competition.port.in.ManageSeasonUseCase;
import org.nepe.competition.port.in.ManageTeamUseCase;
import org.nepe.match.port.in.CreateMatchCommand;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.shared.exception.NepeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * Driving Inbound Adapter (JavaFX Modal Controller) for manually scheduling a new match.
 * <p>
 * Orchestrates user inputs for manual fixture creation:
 * <ul>
 *     <li>Selection of target Competition and active Season.</li>
 *     <li>Selection of Home and Away official teams with distinct identity validation.</li>
 *     <li>Local CET/CEST Kickoff date and time converted to UTC persistence timestamp.</li>
 *     <li>Optional pre-match reference betting odds (1X2).</li>
 * </ul>
 */
@Controller
public class CreateMatchController {

    private static final Logger log = LoggerFactory.getLogger(CreateMatchController.class);
    private static final ZoneId LOCAL_ZONE = ZoneId.of("Europe/Rome");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ManageMatchUseCase manageMatchUseCase;
    private final ManageCompetitionUseCase manageCompetitionUseCase;
    private final ManageSeasonUseCase manageSeasonUseCase;
    private final ManageTeamUseCase manageTeamUseCase;

    // --- FXML Controls ---
    @FXML private ComboBox<Competition> comboCompetition;
    @FXML private ComboBox<Season> comboSeason;
    @FXML private ComboBox<Team> comboHomeTeam;
    @FXML private ComboBox<Team> comboAwayTeam;
    @FXML private DatePicker dpMatchDate;
    @FXML private TextField txtMatchTime;
    @FXML private TextField txtOddsHome;
    @FXML private TextField txtOddsDraw;
    @FXML private TextField txtOddsAway;
    @FXML private Label lblError;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;

    private boolean matchCreated = false;

    public CreateMatchController(ManageMatchUseCase manageMatchUseCase,
                                 ManageCompetitionUseCase manageCompetitionUseCase,
                                 ManageSeasonUseCase manageSeasonUseCase,
                                 ManageTeamUseCase manageTeamUseCase) {
        this.manageMatchUseCase = Objects.requireNonNull(manageMatchUseCase, "ManageMatchUseCase must not be null");
        this.manageCompetitionUseCase = Objects.requireNonNull(manageCompetitionUseCase, "ManageCompetitionUseCase must not be null");
        this.manageSeasonUseCase = Objects.requireNonNull(manageSeasonUseCase, "ManageSeasonUseCase must not be null");
        this.manageTeamUseCase = Objects.requireNonNull(manageTeamUseCase, "ManageTeamUseCase must not be null");
    }

    @FXML
    public void initialize() {
        configureDropdownConverters();
        setupCompetitionSelectionListener();
        loadInitialDropdownData();
        dpMatchDate.setValue(LocalDate.now());
        txtMatchTime.setText("15:00");
    }

    private void setupCompetitionSelectionListener() {
        comboCompetition.getSelectionModel().selectedItemProperty().addListener((obs, oldComp, newComp) -> {
            updateTeamsForCompetition(newComp);
        });
    }

    private void updateTeamsForCompetition(Competition competition) {
        if (competition == null) {
            comboHomeTeam.setItems(FXCollections.emptyObservableList());
            comboAwayTeam.setItems(FXCollections.emptyObservableList());
            return;
        }
        try {
            List<Team> teams = manageTeamUseCase.getTeamsByCompetition(competition.getId());
            comboHomeTeam.setItems(FXCollections.observableArrayList(teams));
            comboAwayTeam.setItems(FXCollections.observableArrayList(teams));
        } catch (Exception e) {
            log.error("Failed to load teams for competition {}", competition.getId(), e);
            lblError.setText("Errore caricamento squadre: " + e.getMessage());
        }
    }

    private void configureDropdownConverters() {
        comboCompetition.setConverter(new StringConverter<>() {
            @Override
            public String toString(Competition c) {
                return (c != null) ? c.getName() + " (" + c.getCode() + ")" : "";
            }

            @Override
            public Competition fromString(String string) {
                return null;
            }
        });

        comboSeason.setConverter(new StringConverter<>() {
            @Override
            public String toString(Season s) {
                return (s != null) ? s.getName() : "";
            }

            @Override
            public Season fromString(String string) {
                return null;
            }
        });

        StringConverter<Team> teamConverter = new StringConverter<>() {
            @Override
            public String toString(Team t) {
                return (t != null) ? t.getName() : "";
            }

            @Override
            public Team fromString(String string) {
                return null;
            }
        };

        comboHomeTeam.setConverter(teamConverter);
        comboAwayTeam.setConverter(teamConverter);
    }

    private void loadInitialDropdownData() {
        try {
            List<Competition> competitions = manageCompetitionUseCase.getAllCompetitions();
            comboCompetition.setItems(FXCollections.observableArrayList(competitions));
            if (!competitions.isEmpty()) {
                comboCompetition.getSelectionModel().selectFirst();
            } else {
                updateTeamsForCompetition(null);
            }

            List<Season> seasons = manageSeasonUseCase.getAllSeasons();
            comboSeason.setItems(FXCollections.observableArrayList(seasons));
            if (!seasons.isEmpty()) {
                comboSeason.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            log.error("Failed to populate create match dialog dropdowns", e);
            lblError.setText("Errore caricamento dati: " + e.getMessage());
        }
    }

    /**
     * Pre-selects the active competition and season in the modal.
     *
     * @param competition active competition
     * @param season      active season
     */
    public void setScope(Competition competition, Season season) {
        this.matchCreated = false;
        lblError.setText("");
        if (competition != null) {
            for (Competition c : comboCompetition.getItems()) {
                if (c.getId() == competition.getId()) {
                    comboCompetition.getSelectionModel().select(c);
                    updateTeamsForCompetition(c);
                    break;
                }
            }
        }
        if (season != null) {
            for (Season s : comboSeason.getItems()) {
                if (s.getId() == season.getId()) {
                    comboSeason.getSelectionModel().select(s);
                    break;
                }
            }
        }
    }

    @FXML
    public void handleConfirm(ActionEvent event) {
        lblError.setText("");

        Competition selectedComp = comboCompetition.getSelectionModel().getSelectedItem();
        Season selectedSeason = comboSeason.getSelectionModel().getSelectedItem();
        Team homeTeam = comboHomeTeam.getSelectionModel().getSelectedItem();
        Team awayTeam = comboAwayTeam.getSelectionModel().getSelectedItem();
        LocalDate matchDate = dpMatchDate.getValue();
        String timeStr = txtMatchTime.getText();

        if (selectedComp == null) {
            lblError.setText("Seleziona una competizione.");
            return;
        }
        if (selectedSeason == null) {
            lblError.setText("Seleziona una stagione.");
            return;
        }
        if (homeTeam == null || awayTeam == null) {
            lblError.setText("Seleziona sia la squadra di casa che la squadra ospite.");
            return;
        }
        if (homeTeam.getId() == awayTeam.getId()) {
            lblError.setText("La squadra di casa e la squadra ospite devono essere distinte.");
            return;
        }
        if (matchDate == null) {
            lblError.setText("Inserisci una data valida per la partita.");
            return;
        }

        LocalTime matchTime;
        try {
            matchTime = (timeStr != null && !timeStr.isBlank())
                    ? LocalTime.parse(timeStr.trim(), TIME_FORMATTER)
                    : LocalTime.of(12, 0);
        } catch (DateTimeParseException e) {
            lblError.setText("Formato orario non valido. Usa HH:mm (es. 15:00 o 20:45).");
            return;
        }

        Instant kickoffUtc = LocalDateTime.of(matchDate, matchTime)
                .atZone(LOCAL_ZONE)
                .toInstant();

        Double oddsH = parseDoubleOrNull(txtOddsHome.getText());
        Double oddsD = parseDoubleOrNull(txtOddsDraw.getText());
        Double oddsA = parseDoubleOrNull(txtOddsAway.getText());

        try {
            CreateMatchCommand command = new CreateMatchCommand(
                    selectedComp.getId(),
                    selectedSeason.getId(),
                    homeTeam.getId(),
                    awayTeam.getId(),
                    kickoffUtc,
                    oddsH,
                    oddsD,
                    oddsA
            );

            manageMatchUseCase.createMatch(command);
            log.info("Manually created match: {} vs {} ({})", homeTeam.getName(), awayTeam.getName(), kickoffUtc);
            this.matchCreated = true;
            closeModal();
        } catch (NepeException e) {
            lblError.setText(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating match", e);
            lblError.setText("Errore imprevisto durante la creazione: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        this.matchCreated = false;
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private static Double parseDoubleOrNull(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return Double.parseDouble(str.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean isMatchCreated() {
        return matchCreated;
    }
}
