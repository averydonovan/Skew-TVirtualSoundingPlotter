/*
 * Copyright (C) 2016 Donovan Smith <donovan@mccollinsmith.com>
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
package com.mccollinsmith.donovan.skewtmultitool.controllers;

import com.mccollinsmith.donovan.skewtmultitool.utils.ModelDataFile;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.converter.NumberStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class for main window.
 *
 * @author Donovan Smith <donovan@mccollinsmith.com>
 */
public class STMTController implements Initializable {

    private static final Logger LOG
            = LoggerFactory.getLogger(ModelDataFile.class.getName());

    public String modelFileName = "rap_252_20160524_0000_000.grb2";
    public ModelDataFile modelDataFile = null;

    // Properties that are bound to GUI
    public static StringProperty windowTitle
            = new SimpleStringProperty("Skew-T MultiTool");
    public BooleanProperty isNoFileOpen = new SimpleBooleanProperty(true);

    /*
     * GUI widgets that are accessed from this controller
     */
    @FXML
    public AnchorPane anchorPane;
    // Menu
    @FXML
    public MenuItem menuFileClose;
    // Data selection pane
    @FXML
    public VBox vbDataSelect;
    @FXML
    public TextField tfLonSearch;
    @FXML
    public TextField tfLatSearch;
    @FXML
    public Button btnLonLatSearch;
    @FXML
    public TextField tfLonFound;
    @FXML
    public TextField tfLatFound;
    @FXML
    public ComboBox cbVariable;
    @FXML
    public ComboBox cbLevel;
    // Data view tab
    @FXML
    public TableColumn tcVariable;
    @FXML
    public TableColumn tcLevel;
    @FXML
    public TableColumn tcValue;
    @FXML
    public TableColumn tcUnits;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        menuFileClose.disableProperty().bind(isNoFileOpen);
        vbDataSelect.disableProperty().bind(isNoFileOpen);
    }

    public void doAppendWindowTitle(String newTitle) {
        windowTitle.setValue("Skew-T MultiTool - " + newTitle);
    }

    public void doResetWindowTitle() {
        windowTitle.setValue("Skew-T MultiTool");
    }

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
        }

        try {
            modelDataFile = new ModelDataFile(modelFileName);
            if (modelDataFile != null) {
                doAppendWindowTitle(file.getName());
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

        if (modelDataFile != null) {
            // Texas State Capitol coords to test
            double lon = -97.740379;
            double lat = 30.274632;
            int[] coords = modelDataFile.getXYCoordsFromLonLat(lon, lat);
            int coordX = coords[0];
            int coordY = coords[1];
            // Look at 1000hPa level
            int coordLvl = modelDataFile.getIndexFromLevel(1000 * 100);
            LOG.debug("LonLat  {}", modelDataFile.getLonLatFromXYCoords(coordX, coordY));
            LOG.debug("Level   {}", modelDataFile.getLevelFromIndex(coordLvl));
            LOG.debug("TempIso {}", modelDataFile.getTempIso(coordX, coordY, coordLvl));
            LOG.debug("Temp2m  {}", modelDataFile.getTemp2m(coordX, coordY));
            LOG.debug("DewpIso {}", modelDataFile.getDewpIso(coordX, coordY, coordLvl));
            LOG.debug("Dewp2m  {}", modelDataFile.getDewp2m(coordX, coordY));
            LOG.debug("LCL     {}", modelDataFile.getLCL(coordX, coordY));
            LOG.debug("CAPE    {}", modelDataFile.getCAPE(coordX, coordY));
            LOG.debug("CIN     {}", modelDataFile.getCIN(coordX, coordY));
            LOG.debug("LFTX    {}", modelDataFile.getLFTX(coordX, coordY));
            LOG.debug("MSL     {}", modelDataFile.getMSL(coordX, coordY));
        }
    }

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

    @FXML
    protected void doLonLatSearch(ActionEvent event) {
        double searchLon = Double.parseDouble(tfLonSearch.getText());
        double searchLat = Double.parseDouble(tfLatSearch.getText());
        int[] coords = modelDataFile.getXYCoordsFromLonLat(searchLon, searchLat);
        int coordX = coords[0];
        int coordY = coords[1];
        double[] foundLonLat = modelDataFile.getLonLatFromXYCoords(coordX, coordY);
        tfLonFound.setText(String.format("%.6f", foundLonLat[0]));
        tfLatFound.setText(String.format("%.6f", foundLonLat[1]));
        // Look at 1000hPa level
        int coordLvl = modelDataFile.getIndexFromLevel(1000 * 100);
        LOG.debug("LonLat  {}", modelDataFile.getLonLatFromXYCoords(coordX, coordY));
        LOG.debug("Level   {}", modelDataFile.getLevelFromIndex(coordLvl));
        LOG.debug("TempIso {}", modelDataFile.getTempIso(coordX, coordY, coordLvl));
        LOG.debug("Temp2m  {}", modelDataFile.getTemp2m(coordX, coordY));
        LOG.debug("DewpIso {}", modelDataFile.getDewpIso(coordX, coordY, coordLvl));
        LOG.debug("Dewp2m  {}", modelDataFile.getDewp2m(coordX, coordY));
        LOG.debug("LCL     {}", modelDataFile.getLCL(coordX, coordY));
        LOG.debug("CAPE    {}", modelDataFile.getCAPE(coordX, coordY));
        LOG.debug("CIN     {}", modelDataFile.getCIN(coordX, coordY));
        LOG.debug("LFTX    {}", modelDataFile.getLFTX(coordX, coordY));
        LOG.debug("MSL     {}", modelDataFile.getMSL(coordX, coordY));
    }

    @FXML
    protected void doExit(ActionEvent event) {
        LOG.debug("Exiting application.");
        if (isNoFileOpen.get()) {
            try {
                modelDataFile.close();
            } catch (IOException ex) {
                LOG.error("Unable to close GRIB file!");
            }
        }
        System.exit(0);
    }
}
