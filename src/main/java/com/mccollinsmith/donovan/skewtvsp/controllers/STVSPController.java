/*
 * Copyright (C) 2016 Donovan Smith
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mccollinsmith.donovan.skewtvsp.controllers;

import com.mccollinsmith.donovan.skewtvsp.utils.ModelDataFile;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class for main window.
 *
 * @author Donovan Smith
 */
public class STVSPController implements Initializable {

    private static final Logger LOG
            = LoggerFactory.getLogger(ModelDataFile.class.getName());

    public String modelFileName = "rap_252_20160524_0000_000.grb2";
    public ModelDataFile modelDataFile = null;

    public static String applicationName = "";

    // Properties that are bound to GUI
    public static StringProperty windowTitle
            = new SimpleStringProperty(applicationName);
    public BooleanProperty isNoFileOpen = new SimpleBooleanProperty(true);
    public BooleanProperty isNoSkewTDrawn = new SimpleBooleanProperty(true);

    /*
     * GUI widgets that are accessed from this controller
     */
    @FXML
    private AnchorPane anchorPane;
    // Menu
    @FXML
    private MenuBar menuBar;
    @FXML
    private MenuItem menuFileOpen;
    @FXML
    private MenuItem menuFileClose;
    @FXML
    private MenuItem menuFileSaveSkewT;
    @FXML
    private MenuItem menuFileExit;
    // Toolbar
    @FXML
    private Button btnOpenFile;
    @FXML
    private Button btnSavePlot;
    // Data selection pane
    @FXML
    private AnchorPane apDataTab;
    @FXML
    private VBox vbDataSelect;
    @FXML
    private Label lblAnalTime;
    @FXML
    private Label lblValidTime;
    @FXML
    private TextField tfLonSearch;
    @FXML
    private TextField tfLatSearch;
    @FXML
    private Button btnLonLatSearch;
    @FXML
    private TextField tfLonFound;
    @FXML
    private TextField tfLatFound;
    @FXML
    private ComboBox cbVariable;
    @FXML
    private ComboBox cbLevel;
    // Data view tab
    @FXML
    private TableView<DataEntry> tblData;
    @FXML
    private TableColumn<DataEntry, String> tcVariable;
    @FXML
    private TableColumn<DataEntry, String> tcLevel;
    @FXML
    private TableColumn<DataEntry, String> tcLevelUnits;
    @FXML
    private TableColumn<DataEntry, String> tcValue;
    @FXML
    private TableColumn<DataEntry, String> tcValueUnits;
    // Skew-T tab
    @FXML
    private ScrollPane spSkewTTab;
    @FXML
    private AnchorPane apSkewTTab;
    @FXML
    private Canvas canvasSkewT;
    @FXML
    private Canvas canvasBlankSkewT;
    // Status bar
    @FXML
    private Label lblStatus;
    @FXML
    private ProgressBar pbProgress;

    private ObservableList<DataEntry> dataList;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            Properties versionProps = new Properties();
            versionProps.load(getClass().getClassLoader().getResourceAsStream("version.properties"));
            applicationName = versionProps.getProperty("name");
        } catch (IOException ex) {
            LOG.error("Failed to load version properties file.");
        }

        if (System.getProperty("os.name").contains("OS X")) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", applicationName);
            menuBar.setUseSystemMenuBar(true);
            menuFileOpen.setAccelerator(KeyCombination.keyCombination("Meta+O"));
            menuFileClose.setAccelerator(KeyCombination.keyCombination("Meta+W"));
            menuFileSaveSkewT.setAccelerator(KeyCombination.keyCombination("Meta+S"));
            menuFileExit.setAccelerator(KeyCombination.keyCombination("Meta+Q"));
        }

        menuFileClose.disableProperty().bind(isNoFileOpen);
        vbDataSelect.disableProperty().bind(isNoFileOpen);

        // These are useless when no Skew-T plot has been drawn
        menuFileSaveSkewT.disableProperty().bind(isNoSkewTDrawn);
        btnSavePlot.disableProperty().bind(isNoSkewTDrawn);
        tblData.disableProperty().bind(isNoSkewTDrawn);

        // Tooltips for buttons and input boxes
        btnOpenFile.setTooltip(new Tooltip("Open GRIB data file"));
        btnSavePlot.setTooltip(new Tooltip("Save Skew-T plot to a PNG file"));
        btnLonLatSearch.setTooltip(
                new Tooltip("Plot Skew-T for closest point to\n"
                        + "requested longitude and latitude"));

        /*
         * Show blank Skew-T whenever either no Skew-T at all has been plotted
         * yet or if on-screen Skew-T is out-of-date due to situations such as a
         * new file being opened.
         */
        canvasSkewT.visibleProperty().bind(isNoSkewTDrawn.not());
        canvasBlankSkewT.visibleProperty().bind(isNoSkewTDrawn);

        dataList = FXCollections.observableArrayList();

        tcVariable.setCellValueFactory(cellData -> cellData.getValue().varNameProperty());
        tcLevel.setCellValueFactory(cellData -> cellData.getValue().levelValueProperty());
        tcLevelUnits.setCellValueFactory(cellData -> cellData.getValue().levelUnitsProperty());
        tcValue.setCellValueFactory(cellData -> cellData.getValue().entryValueProperty());
        tcValueUnits.setCellValueFactory(cellData -> cellData.getValue().entryUnitsProperty());

        tblData.setItems(dataList);

        SkewTPlot.drawBlankSkewT(canvasBlankSkewT.getGraphicsContext2D());

        doResetWindowTitle();
        doUpdateStatus("Ready");
    }

    /**
     * Appends text to the application name in the window title. Typically shows
     * the name of the data file currently opened.
     *
     * @param newTitle text to be appended
     */
    public void doAppendWindowTitle(String newTitle) {
        windowTitle.setValue(applicationName + " - " + newTitle);
    }

    /**
     * Reset window title back to just showing application name.
     */
    public void doResetWindowTitle() {
        windowTitle.setValue(applicationName);
    }

    /**
     * Changes text shown in status bar.
     *
     * @param newStatus new status bar text
     */
    public void doUpdateStatus(String newStatus) {
        lblStatus.setText(newStatus);
    }

    /**
     * Shows a file selection dialog and opens selected data file.
     *
     * @param event
     */
    @FXML
    protected void doOpenFile(ActionEvent event) {
        Path curPath = Paths.get("");
        String cwd = curPath.toAbsolutePath().toString();
        File curPathAsFile = new File(cwd);
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(curPathAsFile);
        chooser.setInitialFileName(modelFileName);
        ExtensionFilter fileExtsGRIB
                = new ExtensionFilter(
                        "GRIB files", "*.grb", "*.grib", "*.grb2", "*.grib2");
        chooser.getExtensionFilters().addAll(fileExtsGRIB);
        File file = chooser.showOpenDialog(anchorPane.getScene().getWindow());

        Task<Void> taskOpenFile = new Task<Void>() {
            @Override
            public Void call() throws IOException {
                try {
                    updateProgress(20, 100);
                    updateMessage("Opening data file " + file.getName() + "...");
                    modelDataFile = new ModelDataFile(modelFileName);
                    updateProgress(80, 100);
                } catch (IOException ex) {
                    LOG.error("Error when attempting to open {}\n{}",
                            file.getName(), ex.getLocalizedMessage());
                    modelDataFile = null;
                }
                return null;
            }
        };

        taskOpenFile.setOnSucceeded(taskEvent -> {
            lblStatus.textProperty().unbind();

            if (modelDataFile != null) {
                doAppendWindowTitle(file.getName());
                doUpdateStatus("Data file " + file.getName() + " opened");
                isNoFileOpen.set(false);

                List<DataEntry> newData = new ArrayList<>();
                dataList = FXCollections.observableArrayList(newData);
                tblData.setItems(dataList);
                tblData.setPrefSize(apDataTab.getWidth(), apDataTab.getHeight());
                tfLonFound.setText("0.0");
                tfLatFound.setText("0.0");

                lblAnalTime.setText(modelDataFile.getAnalysisTime().toString());
                lblValidTime.setText(modelDataFile.getValidTime().toString());
            } else {
                doUpdateStatus("Unable to open data file " + file.getName());
                LOG.error("Unable to open data file {}", file.getName());
                try {
                    modelDataFile.close();
                } catch (IOException ex) {
                    LOG.error("Error when attempting to close data file\n{}",
                            ex.getLocalizedMessage());
                }
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("File Open Error");
                alert.setHeaderText("Unable to open file " + file.getName());
                alert.setContentText("File not found or invalid file format.");
                alert.showAndWait();
            }

            pbProgress.progressProperty().unbind();
            pbProgress.setVisible(false);
        });

        if (file != null) {
            modelFileName = file.getAbsolutePath();
            isNoSkewTDrawn.set(true);

            lblStatus.textProperty().bind(taskOpenFile.messageProperty());
            pbProgress.progressProperty().bind(taskOpenFile.progressProperty());
            pbProgress.setVisible(true);

            new Thread(taskOpenFile).start();
        }
    }

    /**
     * Closes currently open data file.
     *
     * @param event
     */
    @FXML
    protected void doCloseFile(ActionEvent event) {
        try {
            modelDataFile.close();
            doResetWindowTitle();
            isNoFileOpen.set(true);
        } catch (IOException ex) {
            Alert alert = new Alert(AlertType.ERROR);
            LOG.error("Unable to close GRIB file!");
            alert.setTitle("File Close Error");
            alert.setHeaderText("Unable to close GRIB file");
            alert.showAndWait();
        }
    }

    /**
     * Update displayed data based on user-entered longitude and latitude
     * coordinates.
     *
     * @param event
     */
    @FXML
    protected void doLonLatSearch(ActionEvent event) {
        // If longitude or latitude are empty, display error dialog
        if (tfLonSearch.getText().isEmpty() && tfLatSearch.getText().isEmpty()) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Unable to Plot");
            alert.setHeaderText("No coordinates input");
            alert.setContentText("Enter a longitude and latitude before plotting.");
            alert.showAndWait();
            tfLonSearch.requestFocus();
            return;
        } else if (tfLonSearch.getText().isEmpty()) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Unable to Plot");
            alert.setHeaderText("No longitude input");
            alert.setContentText("Enter a longitude before plotting.");
            alert.showAndWait();
            tfLonSearch.requestFocus();
            return;
        } else if (tfLatSearch.getText().isEmpty()) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Unable to Plot");
            alert.setHeaderText("No latitude input");
            alert.setContentText("Enter a latitude before plotting.");
            alert.showAndWait();
            tfLatSearch.requestFocus();
            return;
        }

        boolean lonIsValid = false;
        boolean latIsValid = false;
        String msgLonLat = "";

        // Make sure longitude is a decimal number between -180 and 180
        try {
            Double searchLon = Double.parseDouble(tfLonSearch.getText());
            if (searchLon < -180) {
                msgLonLat += "Longitude must be \u2265 -180.\n";
            } else if (searchLon > 180) {
                msgLonLat += "Longitude must be \u2264 180.\n";
            } else {
                lonIsValid = true;
            }
        } catch (Exception ex) {
            msgLonLat += "Longitude must be in decimal form.\n";
        }

        // Make sure latitude is a decimal number between -90 and 90
        try {
            Double searchLat = Double.parseDouble(tfLatSearch.getText());
            if (searchLat < -90) {
                msgLonLat += "Latitude must be \u2265 -90.\n";
            } else if (searchLat > 90) {
                msgLonLat += "Latitude must be \u2264 90.\n";
            } else {
                latIsValid = true;
            }
        } catch (Exception ex) {
            msgLonLat += "Latitude must be in decimal form.\n";
        }

        /*
         * If both longitude and latitude are valid, plot data and update data.
         * If either or both are invalid, display error message.
         */
        if (lonIsValid == true && latIsValid == true) {
            doUpdateData();
        } else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Unable to Plot");
            alert.setHeaderText("Invalid coordinate(s)");
            alert.setContentText(msgLonLat);
            alert.showAndWait();
            if (lonIsValid == false) {
                tfLonSearch.requestFocus();
            } else {
                tfLatSearch.requestFocus();
            }
        }
    }

    /**
     * Show "About" dialog with application information.
     *
     * @param event
     */
    @FXML
    protected void doHelpAbout(ActionEvent event) {
        try {
            Properties versionProps = new Properties();
            versionProps.load(getClass().getClassLoader().getResourceAsStream("version.properties"));

            String propVers = versionProps.getProperty("version");
            String propAuthor = versionProps.getProperty("author");
            String propYearStart = versionProps.getProperty("year.start");
            String propYearEnd = versionProps.getProperty("year.lastmodified");
            String propLicense = versionProps.getProperty("license");
            String propURL = versionProps.getProperty("url");
            String propYears = "";
            if (propYearStart.equals(propYearEnd)) {
                propYears = propYearStart;
            } else {
                propYears = propYearStart + "-" + propYearEnd;
            }

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("About");
            alert.setHeaderText(applicationName + " v" + propVers);
            alert.setContentText("Copright " + propYears + " " + propAuthor + "\n\n"
                    + "Licensed under the " + propLicense + "." + "\n\n"
                    + "Makes use of the UCAR/Unidata NetCDF-Java library.");
            alert.showAndWait();
        } catch (IOException ex) {
            LOG.error("Failed to load version properties file.");
        }
    }

    /**
     * Save a high-resolution version of displayed plot to a PNG file.
     *
     * @param event
     */
    @FXML
    protected void doSaveSkewT(ActionEvent event) {
        Path curPath = Paths.get("");
        String cwd = curPath.toAbsolutePath().toString();
        File curPathAsFile = new File(cwd);

        /*
         * double[] plotLonLat = mdfSkewTData.getLonLatFromXYCoords(coordX,
         * coordY); String plotLocation = String.format("Longitude, Latitude:
         * %.6f, %.6f", plotLonLat[0], plotLonLat[1]);
         */
        String initFileName = "skewt"
                + "_" + lblAnalTime.getText().replaceAll("[^a-zA-Z0-9]", "")
                + "_" + lblValidTime.getText().replaceAll("[^a-zA-Z0-9]", "")
                + "_" + tfLonFound.getText()
                + "_" + tfLatFound.getText()
                + ".png";

        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(curPathAsFile);
        chooser.setInitialFileName(initFileName);
        ExtensionFilter fileExtsPNG
                = new ExtensionFilter(
                        "PNG images", "*.png", "*.PNG");
        chooser.getExtensionFilters().addAll(fileExtsPNG);
        File file = chooser.showSaveDialog(anchorPane.getScene().getWindow());

        Task<Boolean> taskSavePlot = new Task<Boolean>() {
            @Override
            public Boolean call() {
                updateMessage("Saving plot to " + file.getName() + "...");
                updateProgress(10, 100);
                RenderedImage renderedImage = SkewTPlot.getHiResPlot();
                updateProgress(80, 100);
                try {
                    ImageIO.write(renderedImage, "png", file);
                } catch (IOException ex) {
                    LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
                    LOG.error("Unable to save PNG file!");
                    return false;
                }
                return true;
            }
        };

        taskSavePlot.setOnSucceeded(taskEvent -> {
            lblStatus.textProperty().unbind();
            pbProgress.progressProperty().unbind();
            pbProgress.setVisible(false);

            if (taskSavePlot.getValue() == true) {
                doUpdateStatus("Plot saved to file " + file.getName());
            } else {
                doUpdateStatus("Unable to save plot to file");
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("File Save Error");
                alert.setHeaderText("Unable to save PNG file");
                alert.setContentText("File name not valid or path not writeable.");
                alert.showAndWait();
            }
        });

        if (file != null) {
            lblStatus.textProperty().bind(taskSavePlot.messageProperty());
            pbProgress.progressProperty().bind(taskSavePlot.progressProperty());
            pbProgress.setVisible(true);

            new Thread(taskSavePlot).start();
        }
    }

    /**
     * Exit application, attempting to close open file if needed.
     *
     * @param event
     */
    @FXML
    protected void doExit(ActionEvent event) {
        LOG.debug("Exiting application.");
        if (isNoFileOpen.get() == false) {
            try {
                modelDataFile.close();
            } catch (IOException ex) {
                LOG.error("Unable to close GRIB file!");
            }
        }
        System.exit(0);
    }

    /**
     * Do something on window resize events.
     *
     * @param event
     */
    @FXML
    protected void eventWindowResized(ActionEvent event) {
        // Do nothing
    }

    /**
     * Find nearest coordinate point in data file to that entered by user,
     * display data for found point in tabular format, and plot data for found
     * point in Skew-T Log-P plot.
     */
    public void doUpdateData() {
        isNoSkewTDrawn.set(true);

        List<DataEntry> newData = new ArrayList<DataEntry>();

        double searchLon = Double.parseDouble(tfLonSearch.getText());
        double searchLat = Double.parseDouble(tfLatSearch.getText());
        int[] coords = modelDataFile.getXYCoordsFromLonLat(searchLon, searchLat);
        int coordX = coords[0];
        int coordY = coords[1];
        double[] foundLonLat = modelDataFile.getLonLatFromXYCoords(coordX, coordY);
        tfLonFound.setText(String.format("%.6f", foundLonLat[0]));
        tfLatFound.setText(String.format("%.6f", foundLonLat[1]));

        Task<Void> taskUpdateTable = new Task<Void>() {
            @Override
            public Void call() {
                updateProgress(0, 100);
                updateMessage("Updating data table...");

                for (int coordLvl = 0; coordLvl < 50; coordLvl++) {
                    float curLevel = modelDataFile.getLevelFromIndex(coordLvl);
                    if ((int) curLevel != -1) {
                        newData.add(new DataEntry("Temperature",
                                String.format("%d", (int) curLevel / 100),
                                "hPa",
                                String.format("%f", modelDataFile.getTempIso(coordX, coordY, coordLvl)),
                                "K"));
                    }
                }
                newData.add(new DataEntry("Temperature",
                        "2",
                        "m above ground",
                        String.format("%f", modelDataFile.getTemp2m(coordX, coordY)),
                        "K"));
                updateProgress(30, 100);

                for (int coordLvl = 0; coordLvl < 50; coordLvl++) {
                    float curLevel = modelDataFile.getLevelFromIndex(coordLvl);
                    if ((int) curLevel != -1) {
                        newData.add(new DataEntry("Dew Point",
                                String.format("%d", (int) curLevel / 100),
                                "hPa",
                                String.format("%f", modelDataFile.getDewpIso(coordX, coordY, coordLvl)),
                                "K"));
                    }
                }
                newData.add(new DataEntry("Dew Point",
                        "2",
                        "m above ground",
                        String.format("%f", modelDataFile.getDewp2m(coordX, coordY)),
                        "K"));
                updateProgress(60, 100);

                newData.add(new DataEntry("Surface Pressure",
                        "surface",
                        "surface",
                        String.format("%d", (int) modelDataFile.getPresSfc(coordX, coordY) / 100),
                        "hPa"));

                newData.add(new DataEntry("Mean Sea Level Pressure",
                        "surface",
                        "surface",
                        String.format("%d", (int) modelDataFile.getMSL(coordX, coordY) / 100),
                        "hPa"));

                newData.add(new DataEntry("Lifted Condensation Level",
                        "surface",
                        "surface",
                        String.format("%d", (int) modelDataFile.getLCL(coordX, coordY)[0] / 100),
                        "hPa"));

                newData.add(new DataEntry("Convective Available Potential Energy",
                        "surface",
                        "surface",
                        String.format("%d", (int) modelDataFile.getCAPE(coordX, coordY)),
                        "J/kg"));
                updateProgress(75, 100);

                newData.add(new DataEntry("Convective Inhibition",
                        "surface",
                        "surface",
                        String.format("%d", (int) modelDataFile.getCIN(coordX, coordY)),
                        "J/kg"));

                newData.add(new DataEntry("Lifted Index",
                        "1000-500",
                        "hPa",
                        String.format("%.1f", modelDataFile.getLFTX(coordX, coordY)),
                        "K"));

                newData.add(new DataEntry("K-Index",
                        "850-500",
                        "hPa",
                        String.format("%.0f", modelDataFile.getKIndex(coordX, coordY)),
                        "K"));

                newData.add(new DataEntry("Total Totals",
                        "850-500",
                        "hPa",
                        String.format("%.0f", modelDataFile.getTotalTotals(coordX, coordY)),
                        "K"));

                newData.add(new DataEntry("SWEAT",
                        "850-500",
                        "hPa",
                        String.format("%.0f", modelDataFile.getSWEAT(coordX, coordY)),
                        "(N/A)"));

                updateProgress(80, 100);
                updateMessage("Plotting Skew-T...");

                SkewTPlot.plotSkewT(canvasSkewT.getGraphicsContext2D(), modelDataFile,
                        coordX, coordY);
                updateProgress(100, 100);

                return null;
            }
        };

        taskUpdateTable.setOnSucceeded(event -> {
            lblStatus.textProperty().unbind();
            pbProgress.progressProperty().unbind();
            pbProgress.setVisible(false);

            dataList = FXCollections.observableArrayList(newData);
            tblData.setItems(dataList);
            tblData.setPrefSize(apDataTab.getWidth(), apDataTab.getHeight());

            isNoSkewTDrawn.set(false);
            doUpdateStatus("Data table updated and Skew-T plotted");
        });

        lblStatus.textProperty().bind(taskUpdateTable.messageProperty());
        pbProgress.progressProperty().bind(taskUpdateTable.progressProperty());
        pbProgress.setVisible(true);

        new Thread(taskUpdateTable).start();
    }

    /**
     * Holds tabular data that is displayed in the data tab of the application.
     */
    public static class DataEntry {

        private final SimpleStringProperty varName;
        private final SimpleStringProperty levelValue;
        private final SimpleStringProperty levelUnits;
        private final SimpleStringProperty entryValue;
        private final SimpleStringProperty entryUnits;

        public DataEntry(String vName, String lName, String lUnits,
                String eValue, String eUnits) {
            this.varName = new SimpleStringProperty(vName);
            this.levelValue = new SimpleStringProperty(lName);
            this.levelUnits = new SimpleStringProperty(lUnits);
            this.entryValue = new SimpleStringProperty(eValue);
            this.entryUnits = new SimpleStringProperty(eUnits);
        }

        public String getVarName() {
            return this.varName.get();
        }

        public String getLevelValue() {
            return this.levelValue.get();
        }

        public String getLevelUnits() {
            return this.levelUnits.get();
        }

        public String getEntryValue() {
            return this.entryValue.get();
        }

        public String getEntryUnits() {
            return this.entryUnits.get();
        }

        public StringProperty varNameProperty() {
            return this.varName;
        }

        public StringProperty levelValueProperty() {
            return this.levelValue;
        }

        public StringProperty levelUnitsProperty() {
            return this.levelUnits;
        }

        public StringProperty entryValueProperty() {
            return this.entryValue;
        }

        public StringProperty entryUnitsProperty() {
            return this.entryUnits;
        }
    }
}
