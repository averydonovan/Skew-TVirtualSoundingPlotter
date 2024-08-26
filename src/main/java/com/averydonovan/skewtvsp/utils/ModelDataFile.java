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
package com.averydonovan.skewtvsp.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.Variable;
import ucar.nc2.dt.*;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.ma2.*;

/**
 * Loads GRIB files, reads in longitudes and latitudes of data points, and provides access
 * to various parameters useful in generating Skew-T plots.
 *
 * @author Avery Donovan
 */
public class ModelDataFile {

    private static final Logger LOG
                                = LoggerFactory.getLogger(ModelDataFile.class.getName());

    private NetcdfFile gribFile = null;
    private GridCoordSystem gribGCS = null;

    private final String varNameTempIso = "Temperature_isobaric";
    private final String varNameTemp2m = "Temperature_height_above_ground";
    private final String varNameRHIso = "Relative_humidity_isobaric";
    private final String varNameDewp2m = "Dewpoint_temperature_height_above_ground";
    private final String varNameDewp2mNAM = "Dew_point_temperature_height_above_ground";
    private final String varNameDewpIso = "Dewpoint_temperature_isobaric";
    private final String varNameCape = "Convective_available_potential_energy_surface";
    private final String varNameCapeGRB = "Convective_Available_Potential_Energy_surface";
    private final String varNameCin = "Convective_inhibition_surface";
    private final String varNamePresSfc = "Pressure_surface";
    private final String varNameMsl = "MSLP_MAPS_System_Reduction_msl";
    private final String varNameMslGFS4 = "MSLP_Eta_model_reduction_msl";
    private final String varNameMslGRB
                         = "Mean_Sea_Level_Pressure_NAM_Model_Reduction_msl";
    private final String varNameLftx = "Surface_Lifted_Index_isobaric_layer";
    private final String varNameLftxGFS4 = "Surface_Lifted_Index_surface";
    private final String varNameLftxGFS3 = "Surface_lifted_index_surface";
    private final String varNameLftxGRB
                         = "Parcel_lifted_index_to_500_hPa_layer_between_two_pressure_difference_from_ground_layer";
    private final String varNameLftxHRRR = "Surface_lifted_index_isobaric_layer";
    private final String varNameUGrd = "u-component_of_wind_isobaric";
    private final String varNameVGrd = "v-component_of_wind_isobaric";
    
    private String varNameIso = "isobaric";

    // THREDDS-specific variables
    private final String varNameTHREDDSRH2m = "Relative_humidity_height_above_ground";

    private final String modelNameGFS = "NOAA Global Forecast System";
    private final String modelNameNAM = "NOAA North American Model";
    private final String modelNameRAP = "NOAA Rapid Refresh";
    private final String modelNameHRRR = "NOAA High-Resolution Rapid Refresh";

    private String modelName = "";

    private Map<Double, Integer> isoLevels = null;

    private int maxX = 0;
    private int maxY = 0;
    private int maxLevel = 0;

    private boolean modelIsGRB = false;
    private boolean modelIsGFS3 = false;
    private boolean modelIsGFS4 = false;
    private boolean modelIsHRRR = false;
    private boolean modelIsNAMGRB2 = false;

    private boolean modelIsGFS = false;
    private boolean modelIsNAM = false;
    private boolean modelIsRAP = false;

    private boolean usingTHREDDS = false;

    /**
     * Create new instance. Need to call {@link #open(java.lang.String) open} before
     * attempting to access any methods.
     */
    public ModelDataFile() {
        // Do nothing
    }

    /**
     * Create new instance and open data file.
     *
     * @param gribFileName path and filename of GRIB file to open
     *
     * @throws IOException GRIB file not found or unusable
     */
    public ModelDataFile(String gribFileName) throws IOException {
        boolean didOpen = open(gribFileName);

        if (didOpen == false) {
            throw new IOException("Unable to open file");
        }
    }

    /**
     * Check if a data file is currently open for access.
     *
     * @return true if data file is open or false if not
     */
    public boolean isOpen() {
        if (gribFile == null) {
            return false;
        } else if (gribFile.getLocation().equals("")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Open data file, read in longitudes and latitudes, and read in model data
     * parameters.
     *
     * @param gribFileName path and filename of GRIB file to open
     *
     * @return success of opening and reading GRIB file
     *
     * @throws IOException GRIB file not found or unusable
     */
    public boolean open(String gribFileName) throws IOException {
        LOG.debug("Attempting to open GRIB file: {}", gribFileName);

        // Set all model type flags to false and clear model name string
        modelIsGRB = false;
        modelIsGFS3 = false;
        modelIsGFS4 = false;
        modelIsNAMGRB2 = false;
        modelIsGFS = false;
        modelIsNAM = false;
        modelIsRAP = false;
        modelName = "";

        /*
         * Make sure the model data file being opened can be read by program and detect
         * type of model data file.
         * 
         * Different forecasting model's data files may need slightly different variable
         * names and or grid dimensions for a variable. Methods that retrieve data utilize
         * these values to adjust how they retrieve data.
         */
        if (gribFileName.contains("gfs_3_") && gribFileName.endsWith(".grb")) {
            LOG.debug("Detected GFS GRIB1 file");
            modelIsGRB = true;
            modelIsGFS3 = true;

            modelIsGFS = true;
            modelName = modelNameGFS;
        } else if (gribFileName.contains("gfs_4_") && gribFileName.endsWith(".grb2")) {
            LOG.debug("Detected GFS GRIB2 file");
            modelIsGFS4 = true;

            modelIsGFS = true;
            modelName = modelNameGFS;
        } else if (gribFileName.contains("nam_218_") && gribFileName.endsWith(".grb")) {
            LOG.debug("Detected NAM GRIB1 file");
            modelIsGRB = true;

            modelIsNAM = true;
            modelName = modelNameNAM;
        } else if (gribFileName.contains("nam_218_") && gribFileName.endsWith(".grb2")) {
            LOG.debug("Detected NAM GRIB2 file");
            modelIsNAMGRB2 = true;

            modelName = modelNameNAM;
        } else if (gribFileName.contains("rap_130_") && gribFileName.endsWith(".grb2")) {
            LOG.debug("Detected RAP 130 GRIB2 file");

            modelIsRAP = true;
            modelName = modelNameRAP;
        } else if (gribFileName.contains("rap_252_") && gribFileName.endsWith(".grb2")) {
            LOG.debug("Detected RAP 252 GRIB2 file");

            modelIsRAP = true;
            modelName = modelNameRAP;
        } else if (gribFileName.contains("rap.") && gribFileName.contains("awp130pgrbf")
                   && gribFileName.endsWith(".grib2")) {
            LOG.debug("Detected RAP 130 GRIB2 file");

            modelIsRAP = true;
            modelName = modelNameRAP;
        } else if (gribFileName.contains("rap.") && gribFileName.contains("awp252pgrbf")
                   && gribFileName.endsWith(".grib2")) {
            LOG.debug("Detected RAP 252 GRIB2 file");

            modelIsRAP = true;
            modelName = modelNameRAP;
        } else if (gribFileName.contains("hrrr.") && gribFileName.contains("wrfprsf")
                   && gribFileName.endsWith(".grib2")) {
            LOG.debug("Detected HRRR GRIB2 file");

            modelIsRAP = true;
            modelIsHRRR = true;
            modelName = modelNameHRRR;
        } else if (gribFileName.contains("nam.") && gribFileName.contains("z.awphys")
                   && gribFileName.endsWith(".grib2")) {
            LOG.debug("Detected NAM GRIB2 file");
            modelIsNAMGRB2 = true;

            modelIsNAM = true;
            modelName = modelNameNAM;
        } else if (gribFileName.contains("gfs.")
                   && gribFileName.contains(".pgrb2.0p25")) {
            LOG.debug("Detected GFS 0.25 GRIB2 file");
            modelIsGFS4 = true;

            modelIsGFS = true;
            modelName = modelNameGFS;
        } else if (gribFileName.contains("gfs.")
                   && gribFileName.contains(".pgrb2.0p50")) {
            LOG.debug("Detected GFS 0.50 GRIB2 file");
            modelIsGFS4 = true;

            modelIsGFS = true;
            modelName = modelNameGFS;
        } else if (gribFileName.contains("gfs.")
                   && gribFileName.contains(".pgrb2.1p00")) {
            LOG.debug("Detected GFS 1.00 GRIB2 file");
            modelIsGFS4 = true;

            modelIsGFS = true;
            modelName = modelNameGFS;
        } else if (gribFileName.contains(
                "cdmremote:https://thredds.ucar.edu/thredds/cdmremote/grib/NCEP/RAP/CONUS_13km")) {
            LOG.debug("Detected RAP 130 GRIB2 via THREDDS");

            // modelIsRAP = true;
            modelName = modelNameRAP;

            usingTHREDDS = true;
        } else if (gribFileName.contains("https://thredds.ucar.edu/thredds")
                   || gribFileName.contains("grib/NCEP/RAP/CONUS_13km")) {
            LOG.debug("Detected RAP 130 GRIB2 via THREDDS");

            // modelIsRAP = true;
            modelName = modelNameRAP;

            usingTHREDDS = true;
        } else {
            LOG.debug("Unable to read file");
            return false;
        }

        /*
         * Need to retrieve basic data such as grid size from data file and need to use a
         * variable name that is both shared among the forecasting models' output files
         * and provides complete information about XY-size and the number of isobaric
         * levels.
         */
        String varName = "Temperature_isobaric";
        // varName = "isobaric";

        GridDataset gribGDS = null;

        try {
            gribGDS = ucar.nc2.dt.grid.GridDataset.open(gribFileName);
        } catch (IOException ex) {
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
            throw ex;
        }

        LOG.debug("Opened GridDataset");

        GridDatatype gribVarGDT = gribGDS.findGridByShortName(varName);
        LOG.debug("Found variable in GDS");
        gribGCS = gribVarGDT.getCoordinateSystem();
        LOG.debug("Got coordinate system");
        // try {
        // gribGDS.close();
        // } catch (IOException ex) {
        // LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
        // throw ex;
        // }

        try {
            gribFile = NetcdfDatasets.openDataset(gribFileName);
        } catch (IOException ex) {
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
            throw ex;
        }
        LOG.debug("Opened dataset");

        // varName = "isobaric";
        Variable gribVar = gribFile.findVariable(varName);
        LOG.debug("Found variable in file");

        // Array gribVarData = null;
        int[] gribVarShape = null;
        try {
            // gribVarData = gribVar.read();
            gribVarShape = gribVar.getShape();
            LOG.debug("Read variable from file");
            // } catch (FileNotFoundException ex) {
            /*
             * Error may be caused by an automatically created sidecar file and deleting
             * it and trying again may allow the GRIB file to be successfully opened.
             */
 /*
             * if ((new File(gribFileName).isFile()) && (new File(gribFileName +
             * ".gbx9").isFile())) {
             * LOG.error("Unable to read {} possibly due to sidecar file", gribFileName);
             * LOG.
             * warn("Attempting to delete {}.gbx9 and then trying to read GRIB file again"
             * , gribFileName);
             * 
             * try { File gbx9File = new File(gribFileName + ".gbx9"); gbx9File.delete();
             * 
             * gribGDS = ucar.nc2.dt.grid.GridDataset.open(gribFileName); gribVarGDT =
             * gribGDS.findGridByShortName(varName); gribGCS =
             * gribVarGDT.getCoordinateSystem(); gribFile =
             * NetcdfDatasets.openDataset(gribFileName); gribVar =
             * gribFile.findVariable(varName); //gribVarData = gribVar.read();
             * gribVarShape = gribVar.getShape(); } catch (IOException ex2) {
             * LOG.error("{}\n{}", ex2.getLocalizedMessage(), ex2.toString()); throw ex2;
             * } } else { LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
             * throw ex; } } catch (IOException ex) { LOG.error("{}\n{}",
             * ex.getLocalizedMessage(), ex.toString()); throw ex;
             */
        } catch (Exception ex) {
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
            throw ex;
        }

        if (gribVarShape == null) {
            IOException ex = new IOException("Unusable file");
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
            throw ex;
        }

        // int[] varShape = gribVarData.getShape();
        int[] varShape = gribFile.findVariable("Temperature_isobaric").getShape();
        LOG.debug("varShape = {}", varShape);
        maxX = varShape[varShape.length - 1];
        maxY = varShape[varShape.length - 2];
        maxLevel = varShape[varShape.length - 3];
        LOG.debug("Got shape with maxX = {}, maxY = {}, and maxLevel = {}",
                  maxX, maxY, maxLevel);

        boolean didGetLevels = doGetLevels();

        if (didGetLevels == true) {
            LOG.debug("Successfully opened GRIB file: {}", gribFileName);
            return true;
        } else {
            LOG.debug("Unable to open GRIB file: {}", gribFileName);
            return false;
        }
    }

    /**
     * Closes open GRIB file.
     *
     * @return true on success
     *
     * @throws IOException GRIB file unable to be closed
     */
    public boolean close() throws IOException {
        LOG.debug("Closing GRIB file...");

        try {
            gribFile.close();
            gribFile = null;
            LOG.debug("Successfully closed GRIB file.");
            return true;
        } catch (IOException ex) {
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
            throw ex;
        }
    }

    /**
     * Returns name of model used to generate data file.
     *
     * @return model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Returns longitude and latitude corresponding to XY-coordinates on data grid.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return double[2]; [0] = longitude in degrees, [1] = latitude in degrees
     */
    public double[] getLonLatFromXYCoords(int coordX, int coordY) {
        LatLonPoint ptLatLon = gribGCS.getLatLon(coordX, coordY);
        double[] result = { ptLatLon.getLongitude(), ptLatLon.getLatitude() };
        return result;
    }

    /**
     * Get nearest XY-coordinates in data grid for a longitude-latitude point. Returns -1
     * if outside bounds of grid.
     *
     * @param lon longitude in degrees (-180 to 180)
     * @param lat latitude in degrees (-90 to 90)
     *
     * @return int[2]; [0] = x-coordinate, [1] = y-coordinate
     */
    public int[] getXYCoordsFromLonLat(double lon, double lat) {
        int[] result = gribGCS.findXYindexFromLatLonBounded(lat, lon, null);
        return result;
    }

    /**
     * Get isobaric level corresponding to the data grid's index for that level.
     *
     * @param coordLvl index of level
     *
     * @return isobaric level in Pa
     */
    public double getLevelFromIndex(int coordLvl) {
        if (coordLvl < maxLevel) {
            double result = -1;
            for (Map.Entry<Double, Integer> entry : isoLevels.entrySet()) {
                if (entry.getValue() == coordLvl) {
                    result = entry.getKey();
                    break;
                }
            }
            return result;
        } else {
            return -1;
        }
    }

    /**
     * Get index for nearest available isobaric level.
     *
     * @param level desired isobaric level in Pa
     *
     * @return index for nearest isobaric level in data grid
     */
    public int getIndexFromLevel(double level) {
        int result = -1;
        int closest = 5000;
        double howClose = 10000;
        for (Map.Entry<Double, Integer> entry : isoLevels.entrySet()) {
            howClose = Math.abs(entry.getKey() - level);
            if (howClose < closest) {
                result = entry.getValue();
            }
        }
        return result;
    }

    /**
     * Get all temperatures at a given XY-coordinate.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return temperature in K
     */
    public double[] getTempsAll(int coordX, int coordY) {
        double[] result = { -1 };

        int[] arrayOrigin = { 0, 0, coordY, coordX };
        int[] arraySize = { 1, maxLevel, 1, 1 };

        try {
            // Successful only if an exception doesn't occur here
            Array results = gribFile.findVariable(varNameTempIso)
                    .read(arrayOrigin, arraySize).reduce();
            result = (double[]) results.get1DJavaArray(DataType.DOUBLE);
            LOG.debug("Temps: {}", result);
        } catch (IOException | InvalidRangeException | NullPointerException ex) {
            LOG.error("{}", ex.getLocalizedMessage());
        }

        return result;
    }

    /**
     * Get temperature at a given XY-coordinate and isobaric level index.
     *
     * @param coordX   x-coordinate in data grid
     * @param coordY   y-coordinate in data grid
     * @param coordLvl index of isobaric level in data grid
     *
     * @return temperature in K
     */
    public double getTempIso(int coordX, int coordY, int coordLvl) {
        double result = getValFromVar(varNameTempIso, coordX, coordY, coordLvl, 4);
        return result;
    }

    /**
     * Get temperature at 2m above ground level at a given XY-coordinate.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return temperature in K
     */
    public double getTemp2m(int coordX, int coordY) {
        double result = getValFromVar(varNameTemp2m, coordX, coordY, 4);
        return result;
    }

    /**
     * Get all pressure levels, temperatures, and dewpoints at a given XY-coordinate.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return pressure levels (Pa), temperatures (K), and dewpoints (K)
     */
    public double[][] getTempDewpAll(int coordX, int coordY) {
        double[][] result = { { -1 }, { -1 }, { -1 } };

        int[] arrayOrigin = { 0, 0, coordY, coordX };
        int[] arraySize = { 1, maxLevel, 1, 1 };
        
        int minPresArrayIndex = 0;

        double[] allRHs = null;
        double[] allTemps = null;
        double[] allDewps = null;
        double[] allPres = null;

        try {
            // Successful only if an exception doesn't occur here
            if (modelIsHRRR) {
                allDewps = (double[]) gribFile.findVariable(varNameDewpIso)
                        .read(arrayOrigin, arraySize).reduce()
                        .get1DJavaArray(DataType.DOUBLE);
            } else {
                allRHs = (double[]) gribFile.findVariable(varNameRHIso)
                        .read(arrayOrigin, arraySize).reduce()
                        .get1DJavaArray(DataType.DOUBLE);
            }
            allTemps = (double[]) gribFile.findVariable(varNameTempIso)
                    .read(arrayOrigin, arraySize).reduce()
                    .get1DJavaArray(DataType.DOUBLE);
            allPres = (double[]) gribFile.findVariable(varNameIso)
                    .read().reduce()
                    .get1DJavaArray(DataType.DOUBLE);
        } catch (IOException | InvalidRangeException | NullPointerException ex) {
            LOG.error("{}", ex.getLocalizedMessage());
            return result;
        }

        for (int index = 0; index < allPres.length; index++) {
            if (modelIsGRB) {
                // GRIB1 files use hPa, must convert to Pa
                allPres[index] = allPres[index] * 100;
            }
            
            if (allPres[index] < 10000) {
                // Plotting doesn't work for levels below 100 hPa
                minPresArrayIndex++;
            }
        }
        
        if (!modelIsHRRR) {
            allDewps = new double[allRHs.length];

            for (int index = 0; index < allRHs.length; index++) {
                allDewps[index] = AtmosThermoMath.calcDewp(allTemps[index],
                                                           allPres[index],
                                                           allRHs[index]);
            }
        }
        
        if ((allPres.length == allTemps.length) && (allPres.length == allDewps.length)) {
            result[0] = Arrays.copyOfRange(allPres, minPresArrayIndex, allPres.length);
            result[1] = Arrays.copyOfRange(allTemps, minPresArrayIndex, allTemps.length);
            result[2] = Arrays.copyOfRange(allDewps, minPresArrayIndex, allDewps.length);
        }

        // LOG.debug("Pres: {} {}", result[0].length, result[0]);
        // LOG.debug("Temps: {} {}", result[1].length, result[1]);
        // LOG.debug("Dewps: {} {}", result[2].length, result[2]);
        
        return result;
    }

    /**
     * Get dew point at a given XY-coordinate and isobaric level index. Note that the GRIB
     * files from NOAA forecasting models output relative humidity, not dew point, for the
     * various isobaric levels so this method converts that to dew point.
     *
     * @param coordX   x-coordinate in data grid
     * @param coordY   y-coordinate in data grid
     * @param coordLvl index of isobaric level in data grid
     *
     * @return dew point in K
     */
    public double getDewpIso(int coordX, int coordY, int coordLvl) {
        double pres = getLevelFromIndex(coordLvl);
        double result = 0.0;

        if (modelIsHRRR) {
            result = getValFromVar(varNameDewpIso, coordX, coordY, coordLvl, 4);
            // LOG.debug("HRRR getDewpIso {} {} {}, pres {}, result {}",
            //          coordX, coordY, coordLvl, pres, result);
        } else {
            double temp = getTempIso(coordX, coordY, coordLvl);
            double rh = getValFromVar(varNameRHIso, coordX, coordY, coordLvl, 4);
            result = AtmosThermoMath.calcDewp(temp, pres, rh);
        }

        return result;
    }

    /**
     * Get dew point at 2m above ground level at a given XY-coordinate.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return dew point in K
     */
    public double getDewp2m(int coordX, int coordY) {
        double result = -1;

        if (usingTHREDDS) {
            double temp = getTemp2m(coordX, coordY);
            double rh = getValFromVar(varNameTHREDDSRH2m, coordX, coordY, 4);
            double pres = getPresSfc(coordX, coordY);
            result = AtmosThermoMath.calcDewp(temp, pres, rh);
        } else {
            if (modelIsGRB) {
                result = getValFromVar(varNameDewp2mNAM, coordX, coordY, 4);
            } else {
                result = getValFromVar(varNameDewp2m, coordX, coordY, 4);
            }
        }

        return result;
    }

    /**
     * Get surface pressure at a given XY-coordinate.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return pressure in Pa
     */
    public double getPresSfc(int coordX, int coordY) {
        double result = getValFromVar(varNamePresSfc, coordX, coordY, 3);
        return result;
    }

    /**
     * Get convective available potential energy (CAPE) at a given XY-coordinate.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return CAPE in J/kg
     */
    public double getCAPE(int coordX, int coordY) {
        double result = -1;

        if (modelIsGRB) {
            result = getValFromVar(varNameCapeGRB, coordX, coordY, 3);
        } else {
            result = getValFromVar(varNameCape, coordX, coordY, 3);
        }

        return result;
    }

    /**
     * Get convective inhibition (CIN) at a given XY-coordinate.
     *
     * @param coordX x coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return CIN in J/kg
     */
    public double getCIN(int coordX, int coordY) {
        double result = getValFromVar(varNameCin, coordX, coordY, 3);
        return result;
    }

    /**
     * Get surface (1000 to 500hPa) lifted index at a given XY-coordinate.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return lifted index in K
     */
    public double getLFTX(int coordX, int coordY) {
        double result = -1;

        if (modelIsGFS3) {
            result = getValFromVar(varNameLftxGFS3, coordX, coordY, 3);
        } else if (modelIsGRB) {
            result = getValFromVar(varNameLftxGRB, coordX, coordY, 4);
        } else if (modelIsGFS4) {
            result = getValFromVar(varNameLftxGFS4, coordX, coordY, 3);
        } else if (modelIsHRRR) {
            result = getValFromVar(varNameLftxHRRR, coordX, coordY, 4);
        } else {
            result = getValFromVar(varNameLftx, coordX, coordY, 4);
        }

        return result;
    }

    /**
     * Get mean sea level pressure at a given XY-coordinate. This is the surface pressure
     * at a location adjusted to what it might be if that location was at mean sea level,
     * making it useful in comparing surface pressures of various locations. Note that
     * different forecasting models may use different algorithms to calculate this.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return mean sea level pressure in Pa
     */
    public double getMSL(int coordX, int coordY) {
        double result = -1;

        if (modelIsGRB) {
            result = getValFromVar(varNameMslGRB, coordX, coordY, 3);
        } else if (modelIsGFS4 || modelIsNAMGRB2) {
            result = getValFromVar(varNameMslGFS4, coordX, coordY, 3);
        } else {
            result = getValFromVar(varNameMsl, coordX, coordY, 3);
        }

        return result;
    }

    /**
     * Get lifted condensation level (LCL) at a given XY-coordinate. This is an
     * approximation that is computed essentially the way it would be done by hand on a
     * Skew-T plot. In other words, the dry adiabat intersected by the surface (really,
     * 2m) temperature and the saturation mixing ratio line intersected by the surface
     * (again, really 2m) dew point are followed vertically until they intersect. Both the
     * isobaric level and the temperature at which this occurs are provided, though only
     * the pressure is what is referred to as the LCL.
     *
     * @param coordX x-coordinate in the data grid
     * @param coordY y-coordinate in the data grid
     *
     * @return LCL as double[2]; [0] = pressure in Pa, [1] = temperature in K
     */
    public double[] getLCL(int coordX, int coordY) {
        double curTemp2m = getTemp2m(coordX, coordY);
        double curDewp2m = getDewp2m(coordX, coordY);
        double curPresSfc = getPresSfc(coordX, coordY);

        double[] result = AtmosThermoMath.calcLCL(curTemp2m, curDewp2m, curPresSfc);

        return result;
    }

    /**
     * Get total totals (TT) index at a given XY-coordinate.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return total totals in K
     */
    public double getTotalTotals(int coordX, int coordY) {
        double curTemp500 = getTempIso(coordX, coordY, getIndexFromLevel(50000));
        double curTemp850 = getTempIso(coordX, coordY, getIndexFromLevel(85000));
        double curDewp500 = getDewpIso(coordX, coordY, getIndexFromLevel(50000));
        double curDewp850 = getDewpIso(coordX, coordY, getIndexFromLevel(85000));

        double result = AtmosThermoMath.calcTotalTotals(curTemp500, curTemp850,
                                                        curDewp500, curDewp850);

        return result;
    }

    /**
     * Get K-index (KI) at a given XY-coordinate.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return K-index in K
     */
    public double getKIndex(int coordX, int coordY) {
        double curTemp500 = getTempIso(coordX, coordY, getIndexFromLevel(50000));
        double curTemp700 = getTempIso(coordX, coordY, getIndexFromLevel(70000));
        double curTemp850 = getTempIso(coordX, coordY, getIndexFromLevel(85000));
        double curDewp700 = getDewpIso(coordX, coordY, getIndexFromLevel(70000));
        double curDewp850 = getDewpIso(coordX, coordY, getIndexFromLevel(85000));

        double result = AtmosThermoMath.calcKIndex(curTemp500, curTemp700, curTemp850,
                                                   curDewp700, curDewp850);

        return result;
    }

    /**
     * Get SWEAT index at a given XY-coordinate.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return SWEAT index
     */
    public double getSWEAT(int coordX, int coordY) {
        double curTotalTotals = getTotalTotals(coordX, coordY);

        double uGrd500
               = getValFromVar(varNameUGrd, coordX, coordY, getIndexFromLevel(50000), 4);
        double uGrd850
               = getValFromVar(varNameUGrd, coordX, coordY, getIndexFromLevel(85000), 4);
        double vGrd500
               = getValFromVar(varNameVGrd, coordX, coordY, getIndexFromLevel(50000), 4);
        double vGrd850
               = getValFromVar(varNameVGrd, coordX, coordY, getIndexFromLevel(85000), 4);
        double curDewp850 = getDewpIso(coordX, coordY, getIndexFromLevel(85000));

        double result = AtmosThermoMath.calcSWEAT(curTotalTotals, curDewp850, uGrd500,
                                                  vGrd500, uGrd850, vGrd850);

        return result;
    }

    /**
     * Get analysis time of data file.
     *
     * @return analysis time
     */
    public ZonedDateTime getAnalysisTime() {
        String gribTimeUnits = gribFile.findVariable("reftime").getUnitsString();
        DateTimeFormatter dtFormat = DateTimeFormatter
                .ofPattern("'Hour since 'uuuu-MM-dd'T'HH:mm:ssX", Locale.US);
        dtFormat.withResolverStyle(ResolverStyle.STRICT);
        return ZonedDateTime.parse(gribTimeUnits, dtFormat);
    }

    /**
     * Get valid time of data file. This typically corresponds to the forecast time in the
     * data file.
     *
     * @return forecast time
     */
    public ZonedDateTime getValidTime() {
        String validTimeVarName = "time";
        if (modelIsHRRR) {
            validTimeVarName = "time1";
        }

        String gribTimeUnits = gribFile.findVariable(validTimeVarName).getUnitsString();
        DateTimeFormatter dtFormat = DateTimeFormatter
                .ofPattern("'Hour since 'uuuu-MM-dd'T'HH:mm:ssX", Locale.US);
        dtFormat.withResolverStyle(ResolverStyle.STRICT);
        ZonedDateTime gribAnalTime = ZonedDateTime.parse(gribTimeUnits, dtFormat);

        int gribTimeOffset = 0;
        try {
            gribTimeOffset = gribFile.findVariable(validTimeVarName).read().reduce().
                    getInt(0);
        } catch (IOException ex) {
            LOG.error("Can't read valid time from file, returning analysis time");
        }

        return gribAnalTime.plusHours(gribTimeOffset);
    }

    /**
     * Retrieve isobaric levels available in the data file. Only those levels which are
     * between and include 100hPa to 1000hPa and are in 25hPa increments are used.
     * Isobaric levels are converted from hPa to Pa as necessary.
     *
     * @return true if successful
     */
    private boolean doGetLevels() {
        int initCoordLvl = 0;

        if (modelIsGFS3) {
            varNameIso = "isobaric1";
        } else if (modelIsRAP && !modelIsHRRR) {
            varNameIso = "isobaric1";
        } else if (modelIsNAMGRB2) {
            varNameIso = "isobaric2";
            // Need to skip first 2 isobaric levels
            // initCoordLvl = 2;
        }

        isoLevels = new TreeMap<>();

        Array gribVarDataIso = null;
        try {
            gribVarDataIso = gribFile.findVariable(varNameIso).read().reduce();
        } catch (java.io.IOException ex) {
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
        }

        if (gribVarDataIso != null) {
            Index idxIso = gribVarDataIso.getIndex();

            for (int coordLvl = initCoordLvl; coordLvl < maxLevel; coordLvl++) {
                idxIso.set(coordLvl);
                Integer curLevel = gribVarDataIso.getInt(idxIso);
                if (modelIsGRB && curLevel >= 100 && curLevel <= 1000
                    && (curLevel % 25) == 0) {
                    // GRIB1 files use hPa, must convert to Pa
                    isoLevels.put((double) (curLevel * 100), coordLvl);
                } else if (curLevel >= 10000 && curLevel <= 100000
                           && (curLevel % 2500) == 0) {
                    // GRIB2 files use Pa
                    isoLevels.put((double) (curLevel), coordLvl);
                }
            }
            LOG.debug("Number of isobaric levels used: {}", isoLevels.size());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieve a given variable's value at a particular XY-coordinate.
     *
     * @param varName name of variable to retrieve
     * @param coordX  x-coordinate in data grid
     * @param coordY  y-coordinate in data grid
     * @param varDim  expected dimensions of grid for variable
     *
     * @return value of variable at XY-coordinate, -99999 if not found
     */
    private double getValFromVar(String varName, int coordX, int coordY, int varDim) {
        return getValFromVar(varName, coordX, coordY, 0, varDim);
    }

    /**
     * Retrieve a given variable's value at a particular XY-coordinate and isobaric level
     * index.
     *
     * @param varName  name of variable to retrieve
     * @param coordX   x-coordinate in data grid
     * @param coordY   y-coordinate in data grid
     * @param coordLvl index of isobaric level in data grid
     * @param varDim   expected dimensions of grid for variable
     *
     * @return value of variable at XY-coordinate, -99999 if not found
     */
    private double getValFromVar(String varName, int coordX, int coordY, int coordLvl,
                                 int varDim) {
        final double errorVal = -99999;
        double result = errorVal;

        int[] arrayOrigin = null;
        int[] arraySize = null;

        switch (varDim) {
            case 2:
                arrayOrigin = new int[] { coordY, coordX };
                arraySize = new int[] { 1, 1 };
                break;
            case 3:
                arrayOrigin = new int[] { coordLvl, coordY, coordX };
                arraySize = new int[] { 1, 1, 1 };
                break;
            case 4:
                arrayOrigin = new int[] { 0, coordLvl, coordY, coordX };
                arraySize = new int[] { 1, 1, 1, 1 };
                break;
            default:
                LOG.error("Invalid array dimension specified.");
                return errorVal;
        }

        try {
            // Successful only if an exception doesn't occur here
            result = gribFile.findVariable(varName).read(arrayOrigin, arraySize).reduce()
                    .getDouble(0);
        } catch (IOException | InvalidRangeException | NullPointerException ex) {
            /*
             * These exceptions almost invariably point to programmer error. Make sure the
             * variable name and the dimensions are correct for the particular type of
             * data file. Use the UCAR Unidata Tools UI to inspect the data file you are
             * trying to read to verify.
             */
            LOG.error("Can't read variable: {}\n{}", varName, ex.getLocalizedMessage());
            return errorVal;
        }
        return result;
    }
}
