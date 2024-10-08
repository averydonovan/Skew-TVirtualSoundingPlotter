/*
 * Copyright (c) 2024, Avery Donovan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.averydonovan.skewtvsp.controllers;

import com.averydonovan.skewtvsp.utils.ModelDataFile;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class for main window.
 *
 * @author Avery Donovan
 */
public class STVSPController implements Initializable {

    private static final Logger LOG =
            LoggerFactory.getLogger(ModelDataFile.class.getName());

    public String modelFileName = "rap_252_20160524_0000_000.grb2";
    public ModelDataFile modelDataFile = null;

    public String currentWorkingDirectory = "";

    public static String applicationName = "";

    // Properties that are bound to GUI
    public static StringProperty windowTitle = new SimpleStringProperty(applicationName);
    public BooleanProperty isNoFileOpen = new SimpleBooleanProperty(true);
    public BooleanProperty isNoSkewTDrawn = new SimpleBooleanProperty(true);

    /*
     * GUI widgets that are accessed from this controller
     */
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private VBox mainContainer;
    // Menu
    @FXML
    private MenuBar menuBar;
    @FXML
    private MenuItem menuFileOpen;
    @FXML
    private MenuItem menuURLOpen;
    @FXML
    private MenuItem menuFileClose;
    @FXML
    private MenuItem menuFileSaveSkewT;
    @FXML
    private MenuItem menuFileExit;
    @FXML
    private MenuItem menuThreddsUcarRAP;
    @FXML
    private MenuItem menuThreddsUcarNamCONUS;
    @FXML
    private MenuItem menuThreddsUcarNamAlaska;
    @FXML
    private MenuItem menuThreddsUcarGfsAnalysis;
    @FXML
    private MenuItem menuThreddsUcarGfsForecast;
    // Data selection pane
    @FXML
    private AnchorPane apDataTab;
    @FXML
    private VBox vbDataSelect;
    @FXML
    private MenuButton menuButton;
    @FXML
    private GridPane gpDateTimeSelect;
    @FXML
    private GridPane gpDateTimeUsed;
    @FXML
    private GridPane gpRequestedPoint;
    @FXML
    private GridPane gpFoundPoint;
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
    // @FXML
    // private ComboBox cbVariable;
    // @FXML
    // private ComboBox cbLevel;
    /*
     * // Data view tab
     * 
     * @FXML private TableView<DataEntry> tblData;
     * 
     * @FXML private TableColumn<DataEntry, String> tcVariable;
     * 
     * @FXML private TableColumn<DataEntry, String> tcLevel;
     * 
     * @FXML private TableColumn<DataEntry, String> tcLevelUnits;
     * 
     * @FXML private TableColumn<DataEntry, String> tcValue;
     * 
     * @FXML private TableColumn<DataEntry, String> tcValueUnits; // Data pane
     * 
     * @FXML private TabPane tpDataPane;
     */
    // Skew-T tab
    @FXML
    private ScrollPane spSkewTTab;
    @FXML
    private StackPane apSkewTTab;
    @FXML
    private Canvas canvasSkewT;
    @FXML
    private Canvas canvasBlankSkewT;
    // Status bar
    @FXML
    private Label lblStatus;
    @FXML
    private ProgressBar pbProgress;

    private ObservableList menuList;

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
            versionProps.load(getClass().getClassLoader()
                    .getResourceAsStream("version.properties"));
            applicationName = versionProps.getProperty("name");
        } catch (IOException ex) {
            LOG.error("Failed to load version properties file.");
        }

        if (System.getProperty("os.name").contains("OS X")) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                    applicationName);
            menuBar.setUseSystemMenuBar(true);
            menuFileOpen.setAccelerator(KeyCombination.keyCombination("Meta+O"));
            menuFileClose.setAccelerator(KeyCombination.keyCombination("Meta+W"));
            menuFileSaveSkewT.setAccelerator(KeyCombination.keyCombination("Meta+S"));
            menuFileExit.setAccelerator(KeyCombination.keyCombination("Meta+Q"));
        }

        menuFileClose.disableProperty().bind(isNoFileOpen);
        // vbDataSelect.disableProperty().bind(isNoFileOpen);

        // Todo: need to bind these to separate variables
        gpDateTimeSelect.disableProperty().bind(isNoFileOpen);
        gpDateTimeUsed.disableProperty().bind(isNoFileOpen);
        gpRequestedPoint.disableProperty().bind(isNoFileOpen);
        gpFoundPoint.disableProperty().bind(isNoFileOpen);

        // These are useless when no Skew-T plot has been drawn
        menuFileSaveSkewT.disableProperty().bind(isNoSkewTDrawn);
        // tblData.disableProperty().bind(isNoSkewTDrawn);

        // cbChooseOption.setItems(optionList);
        menuList = FXCollections.observableArrayList();
        // menuButton.setItems(menuList);
        // menuList.addAll("Open Data File...", new Separator());

        /*
         * Show blank Skew-T whenever either no Skew-T at all has been plotted yet or if
         * on-screen Skew-T is out-of-date due to situations such as a new file being
         * opened.
         */
        canvasSkewT.visibleProperty().bind(isNoSkewTDrawn.not());
        canvasBlankSkewT.visibleProperty().bind(isNoSkewTDrawn);

        SkewTPlot.drawBlankSkewT(canvasBlankSkewT.getGraphicsContext2D());

        spSkewTTab.widthProperty().addListener((b, o, n) -> doScaleSkewTView());

        // Get current working directory
        currentWorkingDirectory = Paths.get("").toAbsolutePath().toString();
        LOG.debug("CWD is " + currentWorkingDirectory);

        doResetWindowTitle();
        doScaleSkewTView();
        doUpdateStatus("Ready");
    }

    /**
     * Gets primary stage of application.
     *
     * @return Main stage
     */
    public Window getMainStage() {
        return mainContainer.getScene().getWindow();
    }

    /**
     * Appends text to the application name in the window title. Typically shows the name
     * of the data file currently opened.
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
        File curPathAsFile = new File(currentWorkingDirectory);
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(curPathAsFile);
        chooser.setInitialFileName(modelFileName);
        ExtensionFilter fileExtsGRIB = new ExtensionFilter("GRIB files", "*.grb",
                "*.grib", "*.grb2", "*.grib2", "*.pgrb2.*");
        chooser.getExtensionFilters().addAll(fileExtsGRIB);
        File file = chooser.showOpenDialog(getMainStage());

        Task<Void> taskOpenFile = new Task<Void>() {
            @Override
            public Void call() throws IOException {
                try {
                    updateProgress(20, 100);
                    updateMessage("Opening data file " + file.getName() + "...");
                    modelDataFile = new ModelDataFile(modelFileName);
                    updateProgress(80, 100);
                } catch (IOException ex) {
                    LOG.error("Error when attempting to open {}\n{}", file.getName(),
                            ex.getLocalizedMessage());
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
                } catch (NullPointerException ex) {
                    LOG.error("Unable to read data file type\n{}",
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
            currentWorkingDirectory = file.getParent();
            isNoSkewTDrawn.set(true);

            lblStatus.textProperty().bind(taskOpenFile.messageProperty());
            pbProgress.progressProperty().bind(taskOpenFile.progressProperty());
            pbProgress.setVisible(true);

            new Thread(taskOpenFile).start();
        }
    }

    /**
     * Shows an URL entry box and opens selected URL.
     *
     * @param event
     */
    @FXML
    protected void doOpenURL(ActionEvent event) {
        File file = new File("blah.txt");

        TextInputDialog urlInputDialog = new TextInputDialog();
        urlInputDialog.setHeaderText("Enter GRIB URL");
        urlInputDialog.setTitle("Open GRIB URL");
        urlInputDialog.showAndWait();

        if (urlInputDialog.getResult() == null) {
            return;
        }

        try {
            URL dataURL = new URL(urlInputDialog.getEditor().getText());
        } catch (IOException ex) {
            LOG.error("Invalid data URL: {}", ex.getLocalizedMessage());
            return;
        }

        Task<Void> taskOpenFile = new Task<Void>() {
            @Override
            public Void call() throws IOException {
                try {
                    updateProgress(20, 100);
                    updateMessage("Opening data file " + file.getName() + "...");
                    modelFileName =
                            "cdmremote:https://thredds.ucar.edu/thredds/cdmremote/grib/NCEP/RAP/CONUS_13km/RR_CONUS_13km_20201224_1700.grib2";
                    // modelFileName =
                    // "netcdfsubset:https://thredds.ucar.edu/thredds/ncss/grib/NCEP/RAP/CONUS_13km/RR_CONUS_13km_20201125_0000.grib2/dataset.html";
                    // modelFileName =
                    // "https://thredds.ucar.edu/thredds/dodsC/grib/NCEP/RAP/CONUS_13km/RR_CONUS_13km_20201125_0000.grib2";
                    modelDataFile = new ModelDataFile(modelFileName);
                    updateProgress(80, 100);
                } catch (IOException ex) {
                    LOG.error("Error when attempting to open {}\n{}", file.getName(),
                            ex.getLocalizedMessage());
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
                } catch (NullPointerException ex) {
                    LOG.error("Unable to read data file type\n{}",
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
     * Connect to UCAR THREDDS server for RAP forecast model data.
     *
     * @param event
     */
    @FXML
    protected void doThreddsUcarRAP(ActionEvent event) {
        doThreddsUCAR(
                "https://thredds.ucar.edu/thredds/catalog/grib/NCEP/RAP/CONUS_13km/catalog.xml",
                event);
    }

    /**
     * Connect to UCAR THREDDS server for NAM (CONUS) forecast model data.
     *
     * @param event
     */
    @FXML
    protected void doThreddsUcarNamCONUS(ActionEvent event) {
        doThreddsUCAR(
                "https://thredds.ucar.edu/thredds/catalog/grib/NCEP/NAM/CONUS_12km/catalog.xml",
                event);
    }

    /**
     * Connect to UCAR THREDDS server for NAM (Alaska) forecast model data.
     *
     * @param event
     */
    @FXML
    protected void doThreddsUcarNamAlaska(ActionEvent event) {
        doThreddsUCAR(
                "https://thredds.ucar.edu/thredds/catalog/grib/NCEP/NAM/Alaska_11km/catalog.xml",
                event);
    }

    /**
     * Connect to UCAR THREDDS server for GFS analysis model data.
     *
     * @param event
     */
    @FXML
    protected void doThreddsUcarGfsAnalysis(ActionEvent event) {
        doThreddsUCAR(
                "https://thredds.ucar.edu/thredds/catalog/grib/NCEP/GFS/Global_0p25deg_ana/catalog.xml",
                event);
    }

    /**
     * Connect to UCAR THREDDS server for GFS forecast model data.
     *
     * @param event
     */
    @FXML
    protected void doThreddsUcarGfsForecast(ActionEvent event) {
        doThreddsUCAR(
                "https://thredds.ucar.edu/thredds/catalog/grib/NCEP/GFS/Global_0p25deg/catalog.xml",
                event);
    }

    /**
     * Connect to UCAR THREDDS server.
     *
     * @param event
     */
    @FXML
    protected void doThreddsUCAR(String threddsURL, ActionEvent event) {}

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
     * Update displayed data based on user-entered longitude and latitude coordinates.
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
         * If both longitude and latitude are valid, plot data and update data. If either
         * or both are invalid, display error message.
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
            versionProps.load(getClass().getClassLoader()
                    .getResourceAsStream("version.properties"));

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
         * double[] plotLonLat = mdfSkewTData.getLonLatFromXYCoords(coordX, coordY);
         * String plotLocation = String.format("Longitude, Latitude: %.6f, %.6f",
         * plotLonLat[0], plotLonLat[1]);
         */
        String initFileName =
                "skewt" + "_" + lblAnalTime.getText().replaceAll("[^a-zA-Z0-9]", "") + "_"
                        + lblValidTime.getText().replaceAll("[^a-zA-Z0-9]", "") + "_"
                        + tfLonFound.getText() + "_" + tfLatFound.getText() + ".png";

        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(curPathAsFile);
        chooser.setInitialFileName(initFileName);
        ExtensionFilter fileExtsPNG = new ExtensionFilter("PNG images", "*.png", "*.PNG");
        chooser.getExtensionFilters().addAll(fileExtsPNG);
        File file = chooser.showSaveDialog(getMainStage());

        // Only try to save plot if a location and filename was chosen
        if (file != null) {
            pbProgress.setVisible(true);

            pbProgress.setProgress(0.1);
            RenderedImage renderedImage = SkewTPlot.getHiResPlot();
            pbProgress.setProgress(0.8);

            try {
                // Save the plot to the chosen PNG file
                ImageIO.write(renderedImage, "png", file);
                doUpdateStatus("Plot saved to file " + file.getName());
            } catch (IOException ex) {
                // Unable to save PNG so log the error...
                LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
                LOG.error("Unable to save PNG file!");

                // ...and show an alert
                doUpdateStatus("Unable to save plot to file");
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("File Save Error");
                alert.setHeaderText("Unable to save PNG file");
                alert.setContentText("File name not valid or path not writeable.");
                alert.showAndWait();
            }

            pbProgress.setVisible(false);
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
        doScaleSkewTView();
    }

    /**
     * Scale Skew-T AnchorPane when window width changes.
     */
    public void doScaleSkewTView() {
        double scrollBarWidth = 14.0;
        double viewWidth = 900.0;

        if (spSkewTTab.getViewportBounds().getWidth() != 0) {
            Set<Node> scrollPaneNodes = spSkewTTab.lookupAll(".scroll-bar");
            for (final Node scrollPaneNode : scrollPaneNodes) {
                if (scrollPaneNode instanceof ScrollBar) {
                    ScrollBar scrollBar = (ScrollBar) scrollPaneNode;
                    if (scrollBar.getOrientation() == Orientation.VERTICAL) {
                        scrollBarWidth = scrollBar.getWidth();
                    }
                }
            }
        }

        viewWidth = spSkewTTab.getWidth() - scrollBarWidth - 1.0;

        double scale = (viewWidth / apSkewTTab.getWidth());

        apSkewTTab.setScaleX(scale);
        apSkewTTab.setScaleY(scale);
        spSkewTTab.setContent(new Group(apSkewTTab));
    }

    /**
     * Find nearest coordinate point in data file to that entered by user, display data
     * for found point in tabular format, and plot data for found point in Skew-T Log-P
     * plot.
     */
    public void doUpdateData() {
        isNoSkewTDrawn.set(true);

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

            isNoSkewTDrawn.set(false);
            doUpdateStatus("Data table updated and Skew-T plotted");
        });

        lblStatus.textProperty().bind(taskUpdateTable.messageProperty());
        pbProgress.progressProperty().bind(taskUpdateTable.progressProperty());
        pbProgress.setVisible(true);

        new Thread(taskUpdateTable).start();
    }
}
