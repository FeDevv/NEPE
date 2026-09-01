package org.nepe.match.adapter.in;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.competition.domain.Competition;
import org.nepe.competition.domain.Season;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.competition.port.in.ManageSeasonUseCase;
import org.nepe.inference.port.in.CalculatePreMatchInferenceUseCase;
import org.nepe.inference.port.in.PreMatchAnalysisResult;
import org.nepe.match.domain.MatchState;
import org.nepe.match.port.in.ImportCsvMatchesUseCase;
import org.nepe.match.port.in.ImportCsvResultDTO;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.match.port.out.MatchDetailsDTO;
import org.nepe.shared.exception.AliasMappingRequiredException;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.EntityNotFoundException;
import org.nepe.shared.exception.GuiException;
import org.nepe.shared.exception.NepeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Driving Inbound Adapter (JavaFX Controller) for the central Application Dashboard.
 * <p>
 * Orchestrates the main user interface workflow:
 * <ul>
 *     <li>Competition selection and active tournament context binding.</li>
 *     <li>Matches fixture browsing with state-based filtering (SCHEDULED, LIVE, FINISHED).</li>
 *     <li>Local CET/CEST timezone kickoff formatting.</li>
 *     <li>Synthetic pre-match Expected Value (EV+) signal detection and badge rendering.</li>
 *     <li>CSV ingestion with FileChooser and automated redirection to {@code AliasMappingController}.</li>
 *     <li>Scene switching and navigation across analytical panels.</li>
 * </ul>
 */
@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Rome"));

    private final ManageMatchUseCase manageMatchUseCase;
    private final ManageCompetitionUseCase manageCompetitionUseCase;
    private final ManageSeasonUseCase manageSeasonUseCase;
    private final ImportCsvMatchesUseCase importCsvMatchesUseCase;
    private final CalculatePreMatchInferenceUseCase calculatePreMatchInferenceUseCase;
    private final org.nepe.settings.port.in.ManageSettingsUseCase manageSettingsUseCase;
    private final SpringFXMLLoader springFXMLLoader;

    // --- FXML UI Injections ---

    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavPreMatch;
    @FXML private Button btnNavLive;
    @FXML private Button btnNavCompetition;
    @FXML private Button btnNavSettings;
    @FXML private Label lblDbStatus;

    @FXML private ComboBox<Competition> comboCompetition;
    @FXML private ComboBox<Season> comboSeason;
    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterScheduled;
    @FXML private Button btnFilterLive;
    @FXML private Button btnFilterFinished;

    @FXML private Button btnImportCsv;
    @FXML private Button btnAddMatch;
    @FXML private Button btnRefresh;

    @FXML private TableView<MatchDetailsDTO> tblMatches;
    @FXML private TableColumn<MatchDetailsDTO, String> colDateTime;
    @FXML private TableColumn<MatchDetailsDTO, String> colHomeTeam;
    @FXML private TableColumn<MatchDetailsDTO, String> colAwayTeam;
    @FXML private TableColumn<MatchDetailsDTO, String> colState;
    @FXML private TableColumn<MatchDetailsDTO, String> colScore;
    @FXML private TableColumn<MatchDetailsDTO, String> colOdds;
    @FXML private TableColumn<MatchDetailsDTO, String> colEvBadge;
    @FXML private TableColumn<MatchDetailsDTO, Void> colActions;

    @FXML private Label lblSummary;
    @FXML private Label lblMessage;

    // --- State Management ---
    private MatchState currentFilterState = null;
    private Season currentSeason;
    private Integer selectedMatchIdForNavigation = null;

    public DashboardController(ManageMatchUseCase manageMatchUseCase,
                               ManageCompetitionUseCase manageCompetitionUseCase,
                               ManageSeasonUseCase manageSeasonUseCase,
                               ImportCsvMatchesUseCase importCsvMatchesUseCase,
                               CalculatePreMatchInferenceUseCase calculatePreMatchInferenceUseCase,
                               org.nepe.settings.port.in.ManageSettingsUseCase manageSettingsUseCase,
                               SpringFXMLLoader springFXMLLoader) {
        this.manageMatchUseCase = Objects.requireNonNull(manageMatchUseCase, "ManageMatchUseCase must not be null");
        this.manageCompetitionUseCase = Objects.requireNonNull(manageCompetitionUseCase, "ManageCompetitionUseCase must not be null");
        this.manageSeasonUseCase = Objects.requireNonNull(manageSeasonUseCase, "ManageSeasonUseCase must not be null");
        this.importCsvMatchesUseCase = Objects.requireNonNull(importCsvMatchesUseCase, "ImportCsvMatchesUseCase must not be null");
        this.calculatePreMatchInferenceUseCase = Objects.requireNonNull(calculatePreMatchInferenceUseCase, "CalculatePreMatchInferenceUseCase must not be null");
        this.manageSettingsUseCase = Objects.requireNonNull(manageSettingsUseCase, "ManageSettingsUseCase must not be null");
        this.springFXMLLoader = Objects.requireNonNull(springFXMLLoader, "SpringFXMLLoader must not be null");
    }

    @FXML
    public void initialize() {
        configureTableColumns();
        configureCompetitionDropdown();
        configureSeasonDropdown();
        loadInitialData();
    }

    // --- Table Column Configuration ---

    private void configureTableColumns() {
        colDateTime.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatDateTime(cellData.getValue().matchDateTime()))
        );

        colHomeTeam.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().homeTeamName())
        );

        colAwayTeam.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().awayTeamName())
        );

        colState.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().matchState().name())
        );
        colState.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    switch (item) {
                        case "SCHEDULED" -> badge.getStyleClass().add("badge-status-scheduled");
                        case "LIVE" -> badge.getStyleClass().add("badge-status-live");
                        case "FINISHED" -> badge.getStyleClass().add("badge-status-finished");
                        default -> badge.getStyleClass().add("badge-ev-neutral");
                    }
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colScore.setCellValueFactory(cellData -> {
            MatchDetailsDTO dto = cellData.getValue();
            if (dto.homeScore() != null && dto.awayScore() != null) {
                return new SimpleStringProperty(dto.homeScore() + " - " + dto.awayScore());
            }
            return new SimpleStringProperty("-");
        });
        colScore.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        colOdds.setCellValueFactory(cellData -> {
            MatchDetailsDTO dto = cellData.getValue();
            String oddsStr = formatOdds(dto.oddsHome(), dto.oddsDraw(), dto.oddsAway());
            return new SimpleStringProperty(oddsStr);
        });
        colOdds.setStyle("-fx-alignment: CENTER;");

        colEvBadge.setCellValueFactory(cellData ->
                new SimpleStringProperty(getSyntheticEvSignal(cellData.getValue()))
        );
        colEvBadge.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String signal, boolean empty) {
                super.updateItem(signal, empty);
                if (empty || signal == null || signal.isBlank() || signal.equals("-")) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(signal);
                    badge.getStyleClass().addAll("badge", "badge-ev-positive");
                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnAnalyze = new Button("🎯 Pre-Match");
            private final Button btnLive = new Button("⚡ Live");
            private final HBox container = new HBox(6, btnAnalyze, btnLive);

            {
                btnAnalyze.getStyleClass().addAll("button", "btn-sm", "btn-primary");
                btnLive.getStyleClass().addAll("button", "btn-sm", "btn-danger");
                container.setAlignment(Pos.CENTER);

                btnAnalyze.setOnAction(event -> {
                    MatchDetailsDTO match = getTableView().getItems().get(getIndex());
                    if (match != null && match.matchState().allowsPreMatchAnalysis()) {
                        openPreMatchAnalysis(match.matchId());
                    }
                });

                btnLive.setOnAction(event -> {
                    MatchDetailsDTO match = getTableView().getItems().get(getIndex());
                    if (match != null && match.matchState().allowsLiveTrading()) {
                        openLiveConsole(match.matchId());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    MatchDetailsDTO match = getTableView().getItems().get(getIndex());
                    if (match != null) {
                        btnAnalyze.setDisable(!match.matchState().allowsPreMatchAnalysis());
                        btnLive.setDisable(!match.matchState().allowsLiveTrading());
                        setGraphic(container);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void configureCompetitionDropdown() {
        comboCompetition.setConverter(new StringConverter<>() {
            @Override
            public String toString(Competition competition) {
                return (competition != null) ? competition.getName() + " (" + competition.getCode() + ")" : "";
            }

            @Override
            public Competition fromString(String string) {
                return null;
            }
        });

        comboCompetition.getSelectionModel().selectedItemProperty().addListener((obs, oldComp, newComp) -> {
            if (newComp != null) {
                syntheticEvCache.clear();
                reloadMatches();
            }
        });
    }

    private void configureSeasonDropdown() {
        comboSeason.setConverter(new StringConverter<>() {
            @Override
            public String toString(Season season) {
                return (season != null) ? season.getName() : "";
            }

            @Override
            public Season fromString(String string) {
                return null;
            }
        });

        comboSeason.getSelectionModel().selectedItemProperty().addListener((obs, oldSeason, newSeason) -> {
            if (newSeason != null) {
                this.currentSeason = newSeason;
                syntheticEvCache.clear();
                reloadMatches();
            }
        });
    }

    private void loadInitialData() {
        try {
            List<Season> seasons = manageSeasonUseCase.getAllSeasons();
            if (seasons.isEmpty()) {
                Season defaultSeason = manageSeasonUseCase.getOrCreateSeason(Season.current().getName());
                seasons = List.of(defaultSeason);
            }
            comboSeason.setItems(FXCollections.observableArrayList(seasons));

            if (currentSeason != null && seasons.contains(currentSeason)) {
                comboSeason.getSelectionModel().select(currentSeason);
            } else {
                comboSeason.getSelectionModel().selectFirst();
                this.currentSeason = comboSeason.getSelectionModel().getSelectedItem();
            }

            List<Competition> competitions = manageCompetitionUseCase.getAllCompetitions();
            comboCompetition.setItems(FXCollections.observableArrayList(competitions));

            if (!competitions.isEmpty()) {
                comboCompetition.getSelectionModel().selectFirst();
            } else {
                lblMessage.setText("Nessuna competizione trovata nel database. Crea una competizione o importa un file CSV.");
            }
        } catch (Exception e) {
            log.error("Error loading initial dashboard data", e);
            lblMessage.setText("Errore di connessione: " + e.getMessage());
        }
    }

    // --- Match Query & Filtering ---

    public void reloadMatches() {
        Competition selectedComp = comboCompetition.getSelectionModel().getSelectedItem();
        Season selectedSeason = comboSeason.getSelectionModel().getSelectedItem();
        if (selectedComp == null || selectedSeason == null) {
            tblMatches.setItems(FXCollections.emptyObservableList());
            lblSummary.setText("Partite caricate: 0");
            return;
        }

        this.currentSeason = selectedSeason;

        try {
            List<MatchDetailsDTO> matches;
            if (currentFilterState == null) {
                matches = manageMatchUseCase.getMatchDetailsByCompetitionAndSeason(selectedComp.getId(), selectedSeason.getId());
            } else {
                matches = manageMatchUseCase.getMatchDetailsByState(selectedComp.getId(), selectedSeason.getId(), currentFilterState);
            }

            ObservableList<MatchDetailsDTO> observableList = FXCollections.observableArrayList(matches);
            tblMatches.setItems(observableList);
            lblSummary.setText(String.format("Partite caricate: %d (Competizione: %s | Stagione: %s)", matches.size(), selectedComp.getName(), selectedSeason.getName()));
            lblMessage.setText("");

            // Compute EV signals asynchronously on a Java 25 Virtual Thread to prevent UI thread stuttering
            computeSyntheticEvSignalsAsync(matches);
        } catch (Exception e) {
            log.error("Error reloading matches", e);
            lblMessage.setText("Errore nel caricamento delle partite: " + e.getMessage());
        }
    }

    // --- Synthetic EV+ Evaluator for Dashboard Badge ---

    private final Map<Integer, String> syntheticEvCache = new java.util.concurrent.ConcurrentHashMap<>();

    private String getSyntheticEvSignal(MatchDetailsDTO match) {
        if (match == null) {
            return "-";
        }
        return syntheticEvCache.getOrDefault(match.matchId(), "-");
    }

    private void computeSyntheticEvSignalsAsync(List<MatchDetailsDTO> matches) {
        if (matches == null || matches.isEmpty()) {
            return;
        }

        Thread.startVirtualThread(() -> {
            boolean updated = false;
            for (MatchDetailsDTO match : matches) {
                if (match != null && !syntheticEvCache.containsKey(match.matchId())) {
                    String signal = calculateSyntheticEv(match);
                    syntheticEvCache.put(match.matchId(), signal);
                    updated = true;
                }
            }
            if (updated) {
                Platform.runLater(tblMatches::refresh);
            }
        });
    }

    /**
     * Computes a high-performance synthetic EV+ signal for batch fixture rendering on the Dashboard table.
     * <p>
     * <b>Architectural Trade-Off & Design Rationale:</b>
     * To ensure a responsive 60 FPS UI experience during table scrolling and rapid filter switching,
     * this method applies a lightweight heuristic based on market-implied goal expectations derived
     * from 1X2 market odds, rather than querying and processing historical N-match time-series
     * for every fixture in the viewport.
     * <p>
     * Complete and rigorous pre-match inference using the full Dixon-Coles bivariate Poisson model
     * and historical team strengths is executed when navigating to the Pre-Match Analysis view.
     *
     * @param match the match projection DTO
     * @return a synthetic EV+ badge label (e.g., "EV+ 1 (+5.2%)") or "-" if no positive value is identified
     */
    private String calculateSyntheticEv(MatchDetailsDTO match) {
        if (match.oddsHome() == null && match.oddsDraw() == null && match.oddsAway() == null) {
            return "-";
        }
        if (match.matchState() == MatchState.FINISHED || match.matchState() == MatchState.CANCELLED) {
            return "-";
        }

        try {
            // Approximate market-implied goal expectation rates
            double pHomeImplied = (match.oddsHome() != null && match.oddsHome() > 1.0) ? (1.0 / match.oddsHome()) : 0.40;
            double pAwayImplied = (match.oddsAway() != null && match.oddsAway() > 1.0) ? (1.0 / match.oddsAway()) : 0.30;
            double totalImplied = pHomeImplied + pAwayImplied;

            double lambdaH = Math.max(0.6, totalImplied * 1.5 * (pHomeImplied / Math.max(0.1, totalImplied)));
            double muA = Math.max(0.5, totalImplied * 1.5 * (pAwayImplied / Math.max(0.1, totalImplied)));

            double commRate = (manageSettingsUseCase != null) ? manageSettingsUseCase.getSettings().getCommissionRate() : 0.05;

            PreMatchAnalysisResult result = calculatePreMatchInferenceUseCase.calculate(
                    lambdaH,
                    muA,
                    match.dixonColesRho(),
                    commRate,
                    Collections.emptyList()
            );

            if (match.oddsHome() != null) {
                double evHome = (result.homeWin().probability() * (match.oddsHome() - 1.0) * (1.0 - commRate)) - (1.0 - result.homeWin().probability());
                if (evHome > 0.03) {
                    return String.format("EV+ 1 (+%.1f%%)", evHome * 100);
                }
            }
            if (match.oddsDraw() != null) {
                double evDraw = (result.draw().probability() * (match.oddsDraw() - 1.0) * (1.0 - commRate)) - (1.0 - result.draw().probability());
                if (evDraw > 0.03) {
                    return String.format("EV+ X (+%.1f%%)", evDraw * 100);
                }
            }
            if (match.oddsAway() != null) {
                double evAway = (result.awayWin().probability() * (match.oddsAway() - 1.0) * (1.0 - commRate)) - (1.0 - result.awayWin().probability());
                if (evAway > 0.03) {
                    return String.format("EV+ 2 (+%.1f%%)", evAway * 100);
                }
            }
        } catch (Exception ignored) {
        }
        return "-";
    }

    // --- Action & Event Handlers ---

    @FXML
    public void handleFilterAll(ActionEvent event) {
        currentFilterState = null;
        updateFilterButtonStyles(btnFilterAll);
        reloadMatches();
    }

    @FXML
    public void handleFilterScheduled(ActionEvent event) {
        currentFilterState = MatchState.SCHEDULED;
        updateFilterButtonStyles(btnFilterScheduled);
        reloadMatches();
    }

    @FXML
    public void handleFilterLive(ActionEvent event) {
        currentFilterState = MatchState.LIVE;
        updateFilterButtonStyles(btnFilterLive);
        reloadMatches();
    }

    @FXML
    public void handleFilterFinished(ActionEvent event) {
        currentFilterState = MatchState.FINISHED;
        updateFilterButtonStyles(btnFilterFinished);
        reloadMatches();
    }

    private void updateFilterButtonStyles(Button activeBtn) {
        btnFilterAll.getStyleClass().removeAll("btn-primary");
        btnFilterScheduled.getStyleClass().removeAll("btn-primary");
        btnFilterLive.getStyleClass().removeAll("btn-primary");
        btnFilterFinished.getStyleClass().removeAll("btn-primary");

        if (activeBtn != null && !activeBtn.getStyleClass().contains("btn-primary")) {
            activeBtn.getStyleClass().add("btn-primary");
        }
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        syntheticEvCache.clear();
        loadInitialData();
        reloadMatches();
    }

    @FXML
    public void handleImportCsv(ActionEvent event) {
        Stage stage = (Stage) tblMatches.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona file CSV Football-Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("File CSV (*.csv)", "*.csv"));

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile == null) {
            return;
        }

        String initialSeason = (currentSeason != null) ? currentSeason.getName() : Season.current().getName();
        java.util.Optional<String> chosenSeasonOpt = promptForSeasonName(stage, initialSeason);
        if (chosenSeasonOpt.isEmpty() || chosenSeasonOpt.get().isBlank()) {
            return;
        }

        String seasonNameInput = chosenSeasonOpt.get().trim();
        Season targetSeason;
        try {
            targetSeason = manageSeasonUseCase.getOrCreateSeason(seasonNameInput);
        } catch (DomainValidationException e) {
            showErrorAlert("Formato Stagione Non Valido", e.getMessage());
            return;
        }

        String targetSeasonName = targetSeason.getName();

        // Execute CSV ingestion asynchronously on a Java 25 Virtual Thread to prevent UI thread freezes
        // and support seamless interactive alias resolution continuation
        Thread.startVirtualThread(() -> {
            boolean importCompleted = false;
            while (!importCompleted) {
                try {
                    ImportCsvResultDTO result = importCsvMatchesUseCase.importCsvFile(selectedFile.toPath(), targetSeasonName);
                    importCompleted = true;
                    Platform.runLater(() -> {
                        syntheticEvCache.clear();

                        // Reload seasons in comboSeason and select imported season
                        List<Season> updatedSeasons = manageSeasonUseCase.getAllSeasons();
                        comboSeason.setItems(FXCollections.observableArrayList(updatedSeasons));
                        for (Season s : updatedSeasons) {
                            if (s.getName().equalsIgnoreCase(targetSeasonName)) {
                                comboSeason.getSelectionModel().select(s);
                                this.currentSeason = s;
                                break;
                            }
                        }

                        showInformationAlert("Importazione CSV Completata",
                                String.format("Stagione: %s\nRighe elaborate: %d\nNuove partite: %d\nPartite aggiornate: %d\nModifiche manuali preservate: %d\nRighe saltate: %d",
                                        targetSeasonName,
                                        result.totalRowsParsed(),
                                        result.newMatchesInserted(),
                                        result.existingMatchesUpdated(),
                                        result.manualMatchesPreserved(),
                                        result.skippedRows()));
                        reloadMatches();
                    });
                } catch (AliasMappingRequiredException ex) {
                    log.warn("Unknown team detected in CSV: {}", ex.getRawTeamName());
                    java.util.concurrent.CompletableFuture<Boolean> mappingFuture = new java.util.concurrent.CompletableFuture<>();
                    Platform.runLater(() -> {
                        boolean resolved = openAliasMappingDialog(ex.getRawTeamName(), ex.getCompetitionCode());
                        mappingFuture.complete(resolved);
                    });

                    try {
                        Boolean resolved = mappingFuture.get();
                        if (!Boolean.TRUE.equals(resolved)) {
                            log.info("User canceled alias mapping. Aborting CSV import.");
                            Platform.runLater(() -> {
                                showInformationAlert("Importazione Annullata", "L'importazione del CSV è stata annullata dall'utente.");
                                reloadMatches();
                            });
                            break;
                        }
                        // Alias was resolved and persisted; the loop retries importCsvFile seamlessly!
                    } catch (Exception waitEx) {
                        log.error("Error awaiting alias mapping dialog response", waitEx);
                        break;
                    }
                } catch (NepeException ex) {
                    Platform.runLater(() -> showErrorAlert("Errore durante l'importazione CSV", ex.getMessage()));
                    break;
                } catch (Exception ex) {
                    Platform.runLater(() -> showErrorAlert("Errore imprevisto", "Impossibile completare l'importazione: " + ex.getMessage()));
                    break;
                }
            }
        });
    }

    private java.util.Optional<String> promptForSeasonName(Stage owner, String defaultSeasonName) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Stagione di Destinazione CSV");
        dialog.setHeaderText("Specifica la stagione a cui associare le partite del file CSV:");
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType btnConfirm = new ButtonType("Conferma e Importa", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirm, ButtonType.CANCEL);

        List<String> existingSeasonNames = manageSeasonUseCase.getAllSeasons()
                .stream()
                .map(Season::getName)
                .toList();

        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(existingSeasonNames));
        combo.setEditable(true);
        combo.setPrefWidth(240.0);
        combo.setPromptText("es. 2026/2027");

        if (defaultSeasonName != null && !defaultSeasonName.isBlank()) {
            combo.getSelectionModel().select(defaultSeasonName);
            combo.getEditor().setText(defaultSeasonName);
        } else if (!existingSeasonNames.isEmpty()) {
            combo.getSelectionModel().selectFirst();
            combo.getEditor().setText(existingSeasonNames.getFirst());
        }

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(8);
        Label promptLabel = new Label("Seleziona una stagione esistente o digita un nuovo anno (Formato: YYYY/YYYY):");
        promptLabel.setStyle("-fx-font-size: 12px;");
        content.getChildren().addAll(promptLabel, combo);
        content.setPadding(new javafx.geometry.Insets(10, 10, 10, 10));

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnConfirm) {
                String val = combo.getEditor().getText();
                return (val != null) ? val.trim() : null;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    @FXML
    public void handleAddMatch(ActionEvent event) {
        boolean created = openCreateMatchDialog();
        if (created) {
            syntheticEvCache.clear();
            reloadMatches();
        }
    }

    // --- Navigation Handlers ---

    @FXML
    public void handleNavDashboard(ActionEvent event) {
        reloadMatches();
    }

    @FXML
    public void handleNavPreMatch(ActionEvent event) {
        MatchDetailsDTO selected = tblMatches.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (!selected.matchState().allowsPreMatchAnalysis()) {
                showInformationAlert("Analisi Pre-Match Non Disponibile",
                        "L'analisi pre-match è disponibile solo per partite programmate o posticipate (SCHEDULED / POSTPONED).");
                return;
            }
            openPreMatchAnalysis(selected.matchId());
        } else {
            int matchId = (selectedMatchIdForNavigation != null) ? selectedMatchIdForNavigation : 0;
            openPreMatchAnalysis(matchId);
        }
    }

    @FXML
    public void handleNavLive(ActionEvent event) {
        MatchDetailsDTO selected = tblMatches.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (!selected.matchState().allowsLiveTrading()) {
                showInformationAlert("Trading Live Non Disponibile",
                        "La console live è attiva solo per partite in corso (LIVE).");
                return;
            }
            openLiveConsole(selected.matchId());
        } else {
            int matchId = (selectedMatchIdForNavigation != null) ? selectedMatchIdForNavigation : 0;
            openLiveConsole(matchId);
        }
    }

    @FXML
    public void handleNavCompetition(ActionEvent event) {
        switchScene("/views/competition_manager.fxml", "NEPE - Gestione Anagrafiche e Competizioni");
    }

    @FXML
    public void handleNavSettings(ActionEvent event) {
        switchScene("/views/settings.fxml", "NEPE - Impostazioni");
    }

    private void openPreMatchAnalysis(int matchId) {
        this.selectedMatchIdForNavigation = matchId;
        try {
            SpringFXMLLoader.ViewResult<Parent, org.nepe.inference.adapter.in.PreMatchAnalysisController> view =
                    springFXMLLoader.loadWithController("/views/pre_match_analysis.fxml");
            Competition comp = comboCompetition.getSelectionModel().getSelectedItem();
            if (matchId > 0) {
                MatchDetailsDTO match = manageMatchUseCase.getMatchDetailsById(matchId);
                view.controller().loadMatchDetails(match);
            } else if (comp != null && currentSeason != null) {
                view.controller().setScope(comp.getId(), currentSeason.getId());
            }
            Stage stage = (Stage) tblMatches.getScene().getWindow();
            stage.getScene().setRoot(view.rootNode());
            stage.setTitle("NEPE - Analisi Pre-Match & Calcolo EV");
        } catch (Exception e) {
            log.error("Failed to navigate to pre-match analysis", e);
            showErrorAlert("Errore Navigazione", "Impossibile caricare la schermata di analisi: " + e.getMessage());
        }
    }

    private void openLiveConsole(int matchId) {
        this.selectedMatchIdForNavigation = matchId;
        try {
            SpringFXMLLoader.ViewResult<Parent, LiveConsoleController> view =
                    springFXMLLoader.loadWithController("/views/live_console.fxml");
            Competition comp = comboCompetition.getSelectionModel().getSelectedItem();
            if (matchId > 0) {
                MatchDetailsDTO match = manageMatchUseCase.getMatchDetailsById(matchId);
                view.controller().loadMatchDetails(match);
            } else if (comp != null && currentSeason != null) {
                view.controller().setScope(comp.getId(), currentSeason.getId());
            }
            Stage stage = (Stage) tblMatches.getScene().getWindow();
            stage.getScene().setRoot(view.rootNode());
            stage.setTitle("NEPE - Console Trading Live");
        } catch (Exception e) {
            log.error("Failed to navigate to live console", e);
            showErrorAlert("Errore Navigazione", "Impossibile caricare la console live: " + e.getMessage());
        }
    }

    private boolean openAliasMappingDialog(String rawTeamName, String competitionCode) {
        try {
            SpringFXMLLoader.ViewResult<Parent, org.nepe.competition.adapter.in.AliasMappingController> view =
                    springFXMLLoader.loadWithController("/views/alias_mapping_popup.fxml");
            view.controller().setContext(rawTeamName, competitionCode);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Risoluzione Squadra Sconosciuta");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(tblMatches.getScene().getWindow());

            Scene scene = new Scene(view.rootNode());
            java.net.URL cssResource = getClass().getResource("/styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            return view.controller().isResolved();
        } catch (Exception e) {
            log.error("Failed to open alias mapping dialog", e);
            showErrorAlert("Errore Apertura Modale", "Impossibile aprire il dialogo di mapping alias: " + e.getMessage());
            return false;
        }
    }

    private boolean openCreateMatchDialog() {
        try {
            SpringFXMLLoader.ViewResult<Parent, CreateMatchController> view =
                    springFXMLLoader.loadWithController("/views/create_match_popup.fxml");
            Competition comp = comboCompetition.getSelectionModel().getSelectedItem();
            view.controller().setScope(comp, currentSeason);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Nuova Partita - Inserimento Manuale");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(tblMatches.getScene().getWindow());

            Scene scene = new Scene(view.rootNode());
            java.net.URL cssResource = getClass().getResource("/styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            return view.controller().isMatchCreated();
        } catch (Exception e) {
            log.error("Failed to open create match dialog", e);
            showErrorAlert("Errore Apertura Modale", "Impossibile aprire il dialogo di creazione partita: " + e.getMessage());
            return false;
        }
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            Parent root = springFXMLLoader.load(fxmlPath);
            Stage stage = (Stage) tblMatches.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (Exception e) {
            log.error("Failed to navigate to view: {}", fxmlPath, e);
            showErrorAlert("Errore Navigazione", "Impossibile caricare la schermata: " + e.getMessage());
        }
    }

    // --- Helpers ---

    private static String formatDateTime(Instant instant) {
        return (instant != null) ? DATE_TIME_FORMATTER.format(instant) : "-";
    }

    private static String formatOdds(Double h, Double d, Double a) {
        if (h == null && d == null && a == null) {
            return "-";
        }
        return String.format("%.2f | %.2f | %.2f",
                (h != null ? h : 0.0),
                (d != null ? d : 0.0),
                (a != null ? a : 0.0));
    }

    private void showInformationAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public Integer getSelectedMatchIdForNavigation() {
        return selectedMatchIdForNavigation;
    }
}
