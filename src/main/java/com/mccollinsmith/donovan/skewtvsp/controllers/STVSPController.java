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
    private Button btnSaveSkewT;
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
    // Status bar
    @FXML
    private Label lblStatus;

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

        menuFileSaveSkewT.disableProperty().bind(isNoSkewTDrawn);
        btnSaveSkewT.disableProperty().bind(isNoSkewTDrawn);

        dataList = FXCollections.observableArrayList();

        tcVariable.setCellValueFactory(cellData -> cellData.getValue().varNameProperty());
        tcLevel.setCellValueFactory(cellData -> cellData.getValue().levelValueProperty());
        tcLevelUnits.setCellValueFactory(cellData -> cellData.getValue().levelUnitsProperty());
        tcValue.setCellValueFactory(cellData -> cellData.getValue().entryValueProperty());
        tcValueUnits.setCellValueFactory(cellData -> cellData.getValue().entryUnitsProperty());

        tblData.setItems(dataList);

        SkewTPlot.drawBlankSkewT(canvasSkewT.getGraphicsContext2D());

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

        if (file == null) {
            return;
        } else {
            modelFileName = file.getAbsolutePath();
            isNoSkewTDrawn.set(true);
        }

        try {
            modelDataFile = new ModelDataFile(modelFileName);
            if (modelDataFile != null) {
                doAppendWindowTitle(file.getName());
                doUpdateStatus("File opened");
                isNoFileOpen.set(false);
            } else {
                modelDataFile.close();
                throw new IOException("Invalid file format.");
            }
        } catch (IOException ex) {
            Alert alert = new Alert(AlertType.ERROR);
            LOG.error("Unable to open GRIB file!");
            alert.setTitle("File Open Error");
            alert.setHeaderText("Unable to open GRIB file");
            alert.setContentText("File not found or invalid file format.");
            alert.showAndWait();
            return;
        }

        // Texas State Capitol coords to test
        double lon = -97.740379;
        double lat = 30.274632;

        List<DataEntry> newData = new ArrayList<>();
        dataList = FXCollections.observableArrayList(newData);
        tblData.setItems(dataList);
        tblData.setPrefSize(apDataTab.getWidth(), apDataTab.getHeight());
        tfLonFound.setText("0.0");
        tfLatFound.setText("0.0");

        lblAnalTime.setText(modelDataFile.getAnalysisTime().toString());
        lblValidTime.setText(modelDataFile.getValidTime().toString());

        SkewTPlot.drawBlankSkewT(canvasSkewT.getGraphicsContext2D());
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
        doUpdateData();
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
        String pngFileName = "";

        Path curPath = Paths.get("");
        String cwd = curPath.toAbsolutePath().toString();
        File curPathAsFile = new File(cwd);

        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(curPathAsFile);
        chooser.setInitialFileName("skewt.png");
        ExtensionFilter fileExtsPNG
                = new ExtensionFilter(
                        "PNG images", "*.png", "*.PNG");
        chooser.getExtensionFilters().addAll(fileExtsPNG);
        File file = chooser.showSaveDialog(anchorPane.getScene().getWindow());

        if (file == null) {
            return;
        } else {
            doUpdateStatus("Saving plot to file...");
            pngFileName = file.getAbsolutePath();
            RenderedImage renderedImage = SkewTPlot.getHiResPlot();
            try {
                ImageIO.write(renderedImage, "png", file);
            } catch (IOException ex) {
                LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
                Alert alert = new Alert(AlertType.ERROR);
                LOG.error("Unable to save PNG file!");
                doUpdateStatus("Unable to save plot to file");
                alert.setTitle("File Save Error");
                alert.setHeaderText("Unable to save PNG file");
                alert.setContentText("File name not valid or path not writeable.");
                alert.showAndWait();
                return;
            }
            doUpdateStatus("Plot successfully saved to file " + file.getName());
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
        List<DataEntry> newData = new ArrayList<DataEntry>();

        double searchLon = Double.parseDouble(tfLonSearch.getText());
        double searchLat = Double.parseDouble(tfLatSearch.getText());
        int[] coords = modelDataFile.getXYCoordsFromLonLat(searchLon, searchLat);
        int coordX = coords[0];
        int coordY = coords[1];
        double[] foundLonLat = modelDataFile.getLonLatFromXYCoords(coordX, coordY);
        tfLonFound.setText(String.format("%.6f", foundLonLat[0]));
        tfLatFound.setText(String.format("%.6f", foundLonLat[1]));

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

        dataList = FXCollections.observableArrayList(newData);
        tblData.setItems(dataList);
        tblData.setPrefSize(apDataTab.getWidth(), apDataTab.getHeight());

        SkewTPlot.plotSkewT(canvasSkewT.getGraphicsContext2D(), modelDataFile,
                coordX, coordY);

        isNoSkewTDrawn.set(false);
        doUpdateStatus("Skew-T plot and data table updated");
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
