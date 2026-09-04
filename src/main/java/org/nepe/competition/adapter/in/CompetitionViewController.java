package org.nepe.competition.adapter.in;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.competition.domain.Competition;
import org.nepe.competition.domain.Team;
import org.nepe.competition.domain.TeamAlias;
import org.nepe.competition.port.in.*;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.NepeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Driving Inbound Adapter (JavaFX Controller) for managing Master Anagraphics.
 * <p>
 * Orchestrates CRUD operations on Competitions, Teams, and Alias mappings:
 * <ul>
 *     <li>Competition registration and Dixon-Coles rho (ρ) calibration.</li>
 *     <li>Team creation, filtering, search, and renaming.</li>
 *     <li>Team alias associations and alias deletion.</li>
 * </ul>
 */
@Controller
public class CompetitionViewController {

    private static final Logger log = LoggerFactory.getLogger(CompetitionViewController.class);

    private final ManageCompetitionUseCase manageCompetitionUseCase;
    private final ManageTeamUseCase manageTeamUseCase;
    private final SpringFXMLLoader springFXMLLoader;

    // --- FXML Header ---
    @FXML private Button btnBackToDashboard;
    @FXML private Button btnRefreshAll;

    // --- FXML Tab 1: Competitions ---
    @FXML private TableView<Competition> tblCompetitions;
    @FXML private TableColumn<Competition, String> colCompCode;
    @FXML private TableColumn<Competition, String> colCompName;
    @FXML private TableColumn<Competition, String> colCompCountry;
    @FXML private TableColumn<Competition, String> colCompRho;
    @FXML private TableColumn<Competition, String> colCompHomeAdv;

    @FXML private TextField txtCompCode;
    @FXML private TextField txtCompName;
    @FXML private TextField txtCompCountry;
    @FXML private TextField txtCompRho;
    @FXML private TextField txtCompHomeAdv;
    @FXML private CheckBox chkAutoHomeAdv;
    @FXML private Button btnClearCompForm;
    @FXML private Button btnSaveComp;
    @FXML private Button btnDeleteComp;

    // --- FXML Tab 2: Teams ---
    @FXML private TextField txtTeamSearch;
    @FXML private ComboBox<Competition> comboFilterCompetition;
    @FXML private Button btnClearTeamSearch;
    @FXML private TableView<Team> tblTeams;
    @FXML private TableColumn<Team, Number> colTeamId;
    @FXML private TableColumn<Team, String> colTeamName;

    @FXML private TextField txtTeamName;
    @FXML private ComboBox<Competition> comboTeamCompetition;
    @FXML private Button btnAddTeam;
    @FXML private Button btnAssociateTeam;
    @FXML private Button btnDisassociateTeam;
    @FXML private Button btnRenameTeam;
    @FXML private Button btnDeleteTeam;

    // --- FXML Tab 3: Aliases ---
    @FXML private TableView<TeamAlias> tblAliases;
    @FXML private TableColumn<TeamAlias, Number> colAliasId;
    @FXML private TableColumn<TeamAlias, String> colAliasName;
    @FXML private TableColumn<TeamAlias, Number> colAliasTeamId;

    @FXML private TextField txtAliasName;
    @FXML private ComboBox<Team> comboAliasTargetTeam;
    @FXML private Button btnMapAlias;
    @FXML private Button btnDeleteAlias;

    // --- FXML Bottom Status ---
    @FXML private Label lblStatus;

    // --- State ---
    private Competition selectedCompetition;
    private Team selectedTeam;
    private TeamAlias selectedAlias;

    public CompetitionViewController(ManageCompetitionUseCase manageCompetitionUseCase,
                                     ManageTeamUseCase manageTeamUseCase,
                                     SpringFXMLLoader springFXMLLoader) {
        this.manageCompetitionUseCase = Objects.requireNonNull(manageCompetitionUseCase, "ManageCompetitionUseCase must not be null");
        this.manageTeamUseCase = Objects.requireNonNull(manageTeamUseCase, "ManageTeamUseCase must not be null");
        this.springFXMLLoader = Objects.requireNonNull(springFXMLLoader, "SpringFXMLLoader must not be null");
    }

    @FXML
    public void initialize() {
        configureCompetitionsTable();
        configureTeamsTable();
        configureAliasesTable();
        configureDropdowns();
        configureFormListeners();
        loadAllData();
        if (btnDeleteComp != null) btnDeleteComp.setDisable(true);
        if (btnDeleteTeam != null) btnDeleteTeam.setDisable(true);
        if (btnRenameTeam != null) btnRenameTeam.setDisable(true);
        if (btnAssociateTeam != null) btnAssociateTeam.setDisable(true);
        if (btnDisassociateTeam != null) btnDisassociateTeam.setDisable(true);
        if (btnDeleteAlias != null) btnDeleteAlias.setDisable(true);
    }

    private void configureFormListeners() {
        if (chkAutoHomeAdv != null) {
            chkAutoHomeAdv.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (txtCompHomeAdv != null) {
                    txtCompHomeAdv.setDisable(newVal);
                    if (newVal) {
                        txtCompHomeAdv.setText("");
                    }
                }
            });
        }
    }

    // --- Tab 1: Competitions Setup ---

    private void configureCompetitionsTable() {
        colCompCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        colCompName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colCompCountry.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCountry()));
        colCompRho.setCellValueFactory(data -> new SimpleStringProperty(String.format(java.util.Locale.US, "%.4f", data.getValue().getDixonColesRho())));
        colCompHomeAdv.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().hasManualHomeAdvantage()
                        ? String.format(java.util.Locale.US, "%.2f (Manuale)", data.getValue().getHomeAdvantage())
                        : "Automatico"
        ));

        tblCompetitions.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            this.selectedCompetition = newVal;
            if (btnDeleteComp != null) {
                btnDeleteComp.setDisable(newVal == null);
            }
            if (newVal != null) {
                txtCompCode.setText(newVal.getCode());
                txtCompCode.setDisable(true); // Code is unique key, cannot edit code
                txtCompName.setText(newVal.getName());
                txtCompCountry.setText(newVal.getCountry());
                txtCompRho.setText(String.format(java.util.Locale.US, "%.4f", newVal.getDixonColesRho()));
                if (newVal.hasManualHomeAdvantage()) {
                    if (chkAutoHomeAdv != null) chkAutoHomeAdv.setSelected(false);
                    if (txtCompHomeAdv != null) {
                        txtCompHomeAdv.setDisable(false);
                        txtCompHomeAdv.setText(String.format(java.util.Locale.US, "%.2f", newVal.getHomeAdvantage()));
                    }
                } else {
                    if (chkAutoHomeAdv != null) chkAutoHomeAdv.setSelected(true);
                    if (txtCompHomeAdv != null) {
                        txtCompHomeAdv.setDisable(true);
                        txtCompHomeAdv.setText("");
                    }
                }
            }
        });
    }

    // --- Tab 2: Teams Setup ---

    private void configureTeamsTable() {
        colTeamId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));
        colTeamName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        tblTeams.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            this.selectedTeam = newVal;
            if (btnDeleteTeam != null) {
                btnDeleteTeam.setDisable(newVal == null);
            }
            if (btnRenameTeam != null) {
                btnRenameTeam.setDisable(newVal == null);
            }
            if (btnAssociateTeam != null) {
                btnAssociateTeam.setDisable(newVal == null);
            }
            if (btnDisassociateTeam != null) {
                btnDisassociateTeam.setDisable(newVal == null);
            }
            if (newVal != null) {
                txtTeamName.setText(newVal.getName());
            }
        });

        if (txtTeamSearch != null) {
            txtTeamSearch.textProperty().addListener((obs, oldVal, newVal) -> refreshTeamsTable());
        }
        if (comboFilterCompetition != null) {
            comboFilterCompetition.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> refreshTeamsTable());
        }
    }

    // --- Tab 3: Aliases Setup ---

    private void configureAliasesTable() {
        colAliasId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));
        colAliasName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAliasName()));
        colAliasTeamId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTeamId()));

        tblAliases.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            this.selectedAlias = newVal;
            if (btnDeleteAlias != null) {
                btnDeleteAlias.setDisable(newVal == null);
            }
            if (newVal != null) {
                txtAliasName.setText(newVal.getAliasName());
                selectTeamInCombo(newVal.getTeamId());
            }
        });
    }

    private void configureDropdowns() {
        comboAliasTargetTeam.setConverter(new StringConverter<>() {
            @Override
            public String toString(Team team) {
                return (team != null) ? String.format("%s (ID: %d)", team.getName(), team.getId()) : "";
            }

            @Override
            public Team fromString(String string) {
                return null;
            }
        });

        if (comboTeamCompetition != null) {
            comboTeamCompetition.setConverter(new StringConverter<>() {
                @Override
                public String toString(Competition c) {
                    return (c != null) ? c.getName() + " (" + c.getCode() + ")" : "";
                }

                @Override
                public Competition fromString(String string) {
                    return null;
                }
            });
        }

        if (comboFilterCompetition != null) {
            comboFilterCompetition.setConverter(new StringConverter<>() {
                @Override
                public String toString(Competition c) {
                    return (c != null) ? c.getName() + " (" + c.getCode() + ")" : "Tutti i campionati";
                }

                @Override
                public Competition fromString(String string) {
                    return null;
                }
            });
        }
    }

    private void selectTeamInCombo(int teamId) {
        for (Team t : comboAliasTargetTeam.getItems()) {
            if (t.getId() == teamId) {
                comboAliasTargetTeam.getSelectionModel().select(t);
                break;
            }
        }
    }

    // --- Data Ingestion & Refresh ---

    private void loadAllData() {
        reloadCompetitions();
        reloadTeams();
        reloadAliases();
    }

    private void reloadCompetitions() {
        try {
            List<Competition> list = manageCompetitionUseCase.getAllCompetitions();
            tblCompetitions.setItems(FXCollections.observableArrayList(list));

            if (comboTeamCompetition != null) {
                Competition prev = comboTeamCompetition.getValue();
                comboTeamCompetition.setItems(FXCollections.observableArrayList(list));
                if (prev != null && list.contains(prev)) {
                    comboTeamCompetition.setValue(prev);
                } else if (!list.isEmpty()) {
                    comboTeamCompetition.getSelectionModel().selectFirst();
                }
            }

            if (comboFilterCompetition != null) {
                Competition prevFilter = comboFilterCompetition.getValue();
                ObservableList<Competition> filterItems = FXCollections.observableArrayList();
                filterItems.add(null);
                filterItems.addAll(list);
                comboFilterCompetition.setItems(filterItems);
                if (prevFilter != null && list.contains(prevFilter)) {
                    comboFilterCompetition.setValue(prevFilter);
                } else {
                    comboFilterCompetition.getSelectionModel().selectFirst();
                }
            }
        } catch (Exception e) {
            log.error("Failed to load competitions", e);
        }
    }

    private void refreshTeamsTable() {
        try {
            Competition filterComp = (comboFilterCompetition != null) ? comboFilterCompetition.getValue() : null;
            String search = (txtTeamSearch != null) ? txtTeamSearch.getText() : null;

            List<Team> baseList;
            if (filterComp != null) {
                baseList = manageTeamUseCase.getTeamsByCompetition(filterComp.getId());
            } else {
                baseList = manageTeamUseCase.getAllTeams();
            }

            if (search != null && !search.isBlank()) {
                String query = search.trim().toLowerCase();
                baseList = baseList.stream()
                        .filter(t -> t.getName().toLowerCase().contains(query))
                        .toList();
            }

            tblTeams.setItems(FXCollections.observableArrayList(baseList));

            List<Team> allTeams = manageTeamUseCase.getAllTeams();
            if (comboAliasTargetTeam != null) {
                comboAliasTargetTeam.setItems(FXCollections.observableArrayList(allTeams));
            }
        } catch (Exception e) {
            log.error("Failed to load teams", e);
        }
    }

    private void reloadTeams() {
        refreshTeamsTable();
    }

    private void reloadAliases() {
        try {
            List<TeamAlias> list = manageTeamUseCase.getAllAliases();
            tblAliases.setItems(FXCollections.observableArrayList(list));
        } catch (Exception e) {
            log.error("Failed to load team aliases", e);
        }
    }

    // --- Tab 1 Action Handlers ---

    @FXML
    public void handleSaveCompetition(ActionEvent event) {
        lblStatus.setText("");
        String code = txtCompCode.getText();
        String name = txtCompName.getText();
        String country = txtCompCountry.getText();
        String rhoStr = txtCompRho.getText();

        if (name == null || name.isBlank() || country == null || country.isBlank()) {
            lblStatus.setText("Nome e Nazione della competizione sono obbligatori.");
            return;
        }

        double rho = Competition.DEFAULT_DIXON_COLES_RHO;
        if (rhoStr != null && !rhoStr.isBlank()) {
            try {
                rho = Double.parseDouble(rhoStr.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                lblStatus.setText("Valore del parametro rho non valido.");
                return;
            }
        }

        Double homeAdvantage = null;
        if (chkAutoHomeAdv != null && !chkAutoHomeAdv.isSelected() && txtCompHomeAdv != null) {
            String haStr = txtCompHomeAdv.getText();
            if (haStr != null && !haStr.isBlank()) {
                try {
                    homeAdvantage = Double.parseDouble(haStr.trim().replace(',', '.'));
                } catch (NumberFormatException e) {
                    lblStatus.setText("Valore di Home Advantage non valido.");
                    return;
                }
            }
        }

        try {
            if (selectedCompetition != null) {
                manageCompetitionUseCase.updateCompetition(new UpdateCompetitionCommand(
                        selectedCompetition.getId(),
                        name.trim(),
                        country.trim(),
                        rho,
                        homeAdvantage
                ));
                lblStatus.setText("Competizione aggiornata con successo!");
            } else {
                if (code == null || code.isBlank()) {
                    lblStatus.setText("Codice competizione obbligatorio per una nuova lega.");
                    return;
                }
                manageCompetitionUseCase.createCompetition(new CreateCompetitionCommand(
                        code.trim().toUpperCase(),
                        name.trim(),
                        country.trim(),
                        rho,
                        homeAdvantage
                ));
                lblStatus.setText("Nuova competizione creata con successo!");
            }

            handleClearCompForm(null);
            reloadCompetitions();
        } catch (NepeException e) {
            lblStatus.setText("Errore: " + e.getMessage());
        }
    }

    @FXML
    public void handleClearCompForm(ActionEvent event) {
        this.selectedCompetition = null;
        tblCompetitions.getSelectionModel().clearSelection();
        txtCompCode.setText("");
        txtCompCode.setDisable(false);
        txtCompName.setText("");
        txtCompCountry.setText("");
        txtCompRho.setText(String.format(java.util.Locale.US, "%.4f", Competition.DEFAULT_DIXON_COLES_RHO));
        if (chkAutoHomeAdv != null) {
            chkAutoHomeAdv.setSelected(true);
        }
        if (txtCompHomeAdv != null) {
            txtCompHomeAdv.setText("");
            txtCompHomeAdv.setDisable(true);
        }
        if (btnDeleteComp != null) {
            btnDeleteComp.setDisable(true);
        }
    }

    @FXML
    public void handleDeleteCompetition(ActionEvent event) {
        lblStatus.setText("");
        if (selectedCompetition == null) {
            lblStatus.setText("Seleziona prima una competizione dalla tabella per eliminarla.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Conferma Eliminazione Campionato");
        confirmAlert.setHeaderText("Eliminazione definitiva competizione");
        confirmAlert.setContentText(String.format("Sei sicuro di voler eliminare la competizione '%s' (%s)?",
                selectedCompetition.getName(), selectedCompetition.getCode()));

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                manageCompetitionUseCase.deleteCompetition(selectedCompetition.getId());
                lblStatus.setText("Competizione '" + selectedCompetition.getName() + "' eliminata con successo.");
                handleClearCompForm(null);
                reloadCompetitions();
            } catch (NepeException e) {
                log.warn("Cannot delete competition {}: {}", selectedCompetition.getId(), e.getMessage());
                lblStatus.setText("Errore eliminazione: " + e.getMessage());
                showErrorAlert("Impossibile eliminare il campionato", e.getMessage());
            }
        }
    }

    // --- Tab 2 Action Handlers ---

    @FXML
    public void handleAddTeam(ActionEvent event) {
        lblStatus.setText("");
        String name = txtTeamName.getText();
        if (name == null || name.isBlank()) {
            lblStatus.setText("Inserisci il nome della squadra da creare.");
            return;
        }

        try {
            Team createdTeam = manageTeamUseCase.createTeam(new CreateTeamCommand(name.trim()));
            Competition targetComp = (comboTeamCompetition != null) ? comboTeamCompetition.getValue() : null;
            if (targetComp != null) {
                manageTeamUseCase.associateTeamToCompetition(targetComp.getId(), createdTeam.getId());
                lblStatus.setText(String.format("Squadra '%s' creata e associata a '%s' con successo!",
                        createdTeam.getName(), targetComp.getName()));
            } else {
                lblStatus.setText("Squadra '" + createdTeam.getName() + "' creata con successo (senza associazione).");
            }
            txtTeamName.setText("");
            reloadTeams();
        } catch (NepeException e) {
            lblStatus.setText("Errore: " + e.getMessage());
        }
    }

    @FXML
    public void handleAssociateTeam(ActionEvent event) {
        lblStatus.setText("");
        if (selectedTeam == null) {
            lblStatus.setText("Seleziona prima una squadra dalla tabella.");
            return;
        }
        Competition targetComp = (comboTeamCompetition != null) ? comboTeamCompetition.getValue() : null;
        if (targetComp == null) {
            lblStatus.setText("Seleziona un campionato a cui associare la squadra.");
            return;
        }

        try {
            if (manageTeamUseCase.isTeamAssociatedWithCompetition(targetComp.getId(), selectedTeam.getId())) {
                lblStatus.setText(String.format("La squadra '%s' è già associata a '%s'.",
                        selectedTeam.getName(), targetComp.getName()));
                return;
            }
            manageTeamUseCase.associateTeamToCompetition(targetComp.getId(), selectedTeam.getId());
            lblStatus.setText(String.format("Squadra '%s' associata a '%s' con successo!",
                    selectedTeam.getName(), targetComp.getName()));
            reloadTeams();
        } catch (NepeException e) {
            lblStatus.setText("Errore: " + e.getMessage());
        }
    }

    @FXML
    public void handleDisassociateTeam(ActionEvent event) {
        lblStatus.setText("");
        if (selectedTeam == null) {
            lblStatus.setText("Seleziona prima una squadra dalla tabella.");
            return;
        }
        Competition targetComp = (comboTeamCompetition != null) ? comboTeamCompetition.getValue() : null;
        if (targetComp == null) {
            lblStatus.setText("Seleziona il campionato da cui rimuovere l'associazione.");
            return;
        }

        try {
            if (!manageTeamUseCase.isTeamAssociatedWithCompetition(targetComp.getId(), selectedTeam.getId())) {
                lblStatus.setText(String.format("La squadra '%s' non è associata a '%s'.",
                        selectedTeam.getName(), targetComp.getName()));
                return;
            }
            manageTeamUseCase.disassociateTeamFromCompetition(targetComp.getId(), selectedTeam.getId());
            lblStatus.setText(String.format("Squadra '%s' rimossa da '%s' con successo!",
                    selectedTeam.getName(), targetComp.getName()));
            reloadTeams();
        } catch (NepeException e) {
            lblStatus.setText("Errore: " + e.getMessage());
        }
    }

    @FXML
    public void handleRenameTeam(ActionEvent event) {
        lblStatus.setText("");
        if (selectedTeam == null) {
            lblStatus.setText("Seleziona prima una squadra dalla tabella per rinominarla.");
            return;
        }
        String newName = txtTeamName.getText();
        if (newName == null || newName.isBlank()) {
            lblStatus.setText("Inserisci il nuovo nome per la squadra.");
            return;
        }

        try {
            manageTeamUseCase.renameTeam(new RenameTeamCommand(selectedTeam.getId(), newName.trim()));
            lblStatus.setText("Squadra rinominata in '" + newName.trim() + "'!");
            reloadTeams();
        } catch (NepeException e) {
            lblStatus.setText("Errore: " + e.getMessage());
        }
    }

    @FXML
    public void handleDeleteTeam(ActionEvent event) {
        lblStatus.setText("");
        if (selectedTeam == null) {
            lblStatus.setText("Seleziona prima una squadra dalla tabella per eliminarla.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Conferma Eliminazione Squadra");
        confirmAlert.setHeaderText("Eliminazione definitiva squadra");
        confirmAlert.setContentText(String.format("Sei sicuro di voler eliminare la squadra '%s' e tutti i relativi alias?",
                selectedTeam.getName()));

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                manageTeamUseCase.deleteTeam(selectedTeam.getId());
                lblStatus.setText("Squadra '" + selectedTeam.getName() + "' eliminata con successo.");
                this.selectedTeam = null;
                tblTeams.getSelectionModel().clearSelection();
                txtTeamName.setText("");
                if (btnDeleteTeam != null) btnDeleteTeam.setDisable(true);
                if (btnRenameTeam != null) btnRenameTeam.setDisable(true);
                if (btnAssociateTeam != null) btnAssociateTeam.setDisable(true);
                if (btnDisassociateTeam != null) btnDisassociateTeam.setDisable(true);
                reloadTeams();
                reloadAliases();
            } catch (NepeException e) {
                log.warn("Cannot delete team {}: {}", selectedTeam.getId(), e.getMessage());
                lblStatus.setText("Errore eliminazione: " + e.getMessage());
                showErrorAlert("Impossibile eliminare la squadra", e.getMessage());
            }
        }
    }

    @FXML
    public void handleClearTeamSearch(ActionEvent event) {
        if (txtTeamSearch != null) {
            txtTeamSearch.setText("");
        }
        if (comboFilterCompetition != null) {
            comboFilterCompetition.getSelectionModel().selectFirst();
        }
        reloadTeams();
    }

    // --- Tab 3 Action Handlers ---

    @FXML
    public void handleMapAlias(ActionEvent event) {
        lblStatus.setText("");
        String alias = txtAliasName.getText();
        Team targetTeam = comboAliasTargetTeam.getSelectionModel().getSelectedItem();

        if (alias == null || alias.isBlank() || targetTeam == null) {
            lblStatus.setText("Specifica sia il nome alias che la squadra ufficiale di destinazione.");
            return;
        }

        try {
            manageTeamUseCase.mapAlias(new MapTeamAliasCommand(alias.trim(), targetTeam.getId()));
            txtAliasName.setText("");
            lblStatus.setText("Alias '" + alias.trim() + "' associato a " + targetTeam.getName() + "!");
            reloadAliases();
        } catch (NepeException e) {
            lblStatus.setText("Errore: " + e.getMessage());
        }
    }

    @FXML
    public void handleDeleteAlias(ActionEvent event) {
        lblStatus.setText("");
        if (selectedAlias == null) {
            lblStatus.setText("Seleziona prima un alias dalla tabella per eliminarlo.");
            return;
        }

        try {
            manageTeamUseCase.deleteAlias(selectedAlias.getId());
            lblStatus.setText("Alias eliminato con successo.");
            this.selectedAlias = null;
            tblAliases.getSelectionModel().clearSelection();
            txtAliasName.setText("");
            reloadAliases();
        } catch (NepeException e) {
            lblStatus.setText("Errore eliminazione: " + e.getMessage());
        }
    }

    // --- Header & Global Actions ---

    @FXML
    public void handleRefreshAll(ActionEvent event) {
        loadAllData();
        lblStatus.setText("Anagrafiche aggiornate.");
    }

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        try {
            Parent root = springFXMLLoader.load("/views/dashboard.fxml");
            Stage stage = (Stage) btnBackToDashboard.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("NEPE - Nexus Exchange Prediction Engine");
        } catch (Exception e) {
            log.error("Failed to return to dashboard", e);
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
