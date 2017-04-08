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
package com.mccollinsmith.donovan.skewtvsp.utils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Variable;
import ucar.nc2.dt.*;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.ma2.*;

/**
 * Loads GRIB files, reads in longitudes and latitudes of data points, and
 * provides access to various parameters useful in generating Skew-T plots.
 *
 * @author Donovan Smith
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
    private final String varNameCape = "Convective_available_potential_energy_surface";
    private final String varNameCapeGRB
            = "Convective_Available_Potential_Energy_surface";
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
    private final String varNameUGrd = "u-component_of_wind_isobaric";
    private final String varNameVGrd = "v-component_of_wind_isobaric";

    private Map<Float, Integer> isoLevels = null;

    private int maxX = 0;
    private int maxY = 0;
    private int maxLevel = 0;

    private boolean modelIsGRB = false;
    private boolean modelIsGFS3 = false;
    private boolean modelIsGFS4 = false;
    private boolean modelIsNAMGRB2 = false;

    /**
     * Create new instance. Need to call {@link #open(java.lang.String) open}
     * before attempting to access any methods.
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
        
        /* 
         * Make sure the model data file being opened can be read by program and
         * detect type of model data file.
         * 
         * Different forecasting model's data files may need slightly different
         * variable names and or grid dimensions for a variable. Methods that
         * retrieve data utilize these values to adjust how they retrieve data.
         */
        if (gribFileName.contains("gfs_3_")
                && gribFileName.endsWith(".grb")) {
            LOG.debug("Detected GFS GRIB1 file");
            modelIsGRB = true;
            modelIsGFS3 = true;
        } else if (gribFileName.contains("gfs_4_")
                && gribFileName.endsWith(".grb2")) {
            LOG.debug("Detected GFS GRIB2 file");
            modelIsGFS4 = true;
        } else if (gribFileName.contains("nam_218_")
                && gribFileName.endsWith(".grb")) {
            LOG.debug("Detected NAM GRIB1 file");
            modelIsGRB = true;
        } else if (gribFileName.contains("rap_130_")
                && gribFileName.endsWith(".grb2")) {
            LOG.debug("Detected RAP 130 GRIB2 file");
        } else if (gribFileName.contains("rap_252_")
                && gribFileName.endsWith(".grb2")) {
            LOG.debug("Detected RAP 252 GRIB2 file");
        } else if (gribFileName.contains("rap.")
                && gribFileName.contains("awp130pgrbf")
                && gribFileName.endsWith(".grib2")) {
            LOG.debug("Detected RAP 130 GRIB2 file");
        } else if (gribFileName.contains("rap.")
                && gribFileName.contains("awp252pgrbf")
                && gribFileName.endsWith(".grib2")) {
            LOG.debug("Detected RAP 252 GRIB2 file");
        } else if (gribFileName.contains("nam.")
                && gribFileName.contains("z.awphys")
                && gribFileName.endsWith(".grib2")) {
            LOG.debug("Detected NAM GRIB2 file");
            modelIsNAMGRB2 = true;
        } else if (gribFileName.contains("gfs.")
                && gribFileName.contains(".pgrb2.0p25")) {
            LOG.debug("Detected GFS 0.25 GRIB2 file");
            modelIsGFS4 = true;
        } else if (gribFileName.contains("gfs.")
                && gribFileName.contains(".pgrb2.0p50")) {
            LOG.debug("Detected GFS 0.50 GRIB2 file");
            modelIsGFS4 = true;
        } else if (gribFileName.contains("gfs.")
                && gribFileName.contains(".pgrb2.1p00")) {
            LOG.debug("Detected GFS 1.00 GRIB2 file");
            modelIsGFS4 = true;
        } else {
            LOG.debug("Unable to read file");
            return false;
        }

        /*
         * Need to retrieve basic data such as grid size from data file and need
         * to use a variable name that is both shared among the forecasting
         * models' output files and provides complete information about XY-size
         * and the number of isobaric levels.
         */
        String varName = "Temperature_isobaric";

        GridDataset gribGDS = null;

        try {
            gribGDS = ucar.nc2.dt.grid.GridDataset.open(gribFileName);
        } catch (IOException ex) {
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
            throw ex;
        }

        GridDatatype gribVarGDT = gribGDS.findGridByShortName(varName);
        gribGCS = gribVarGDT.getCoordinateSystem();
        try {
            gribGDS.close();
        } catch (IOException ex) {
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
            throw ex;
        }

        try {
            gribFile = NetcdfDataset.openDataset(gribFileName);
        } catch (IOException ex) {
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
            throw ex;
        }

        Variable gribVar = gribFile.findVariable(varName);

        Array gribVarData = null;
        try {
            gribVarData = gribVar.read();
        } catch (IOException ex) {
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
            throw ex;
        }

        if (gribVarData == null) {
            IOException ex = new IOException("Unusable file");
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
            throw ex;
        }

        int[] varShape = gribVarData.getShape();
        LOG.debug("varShape = {}", varShape);
        maxX = varShape[varShape.length - 1];
        maxY = varShape[varShape.length - 2];
        maxLevel = varShape[varShape.length - 3];
        
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
     * Returns longitude and latitude corresponding to XY-coordinates on data
     * grid.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return float[2]; [0] = longitude in degrees, [1] = latitude in degrees
     */
    public double[] getLonLatFromXYCoords(int coordX, int coordY) {
        LatLonPoint ptLatLon = gribGCS.getLatLon(coordX, coordY);
        double[] result = {ptLatLon.getLongitude(), ptLatLon.getLatitude()};
        return result;
    }

    /**
     * Get nearest XY-coordinates in data grid for a longitude-latitude point.
     * Returns -1 if outside bounds of grid.
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
    public float getLevelFromIndex(int coordLvl) {
        if (coordLvl < maxLevel) {
            float result = -1;
            for (Map.Entry<Float, Integer> entry : isoLevels.entrySet()) {
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
    public int getIndexFromLevel(float level) {
        int result = -1;
        int closest = 5000;
        float howClose = 10000;
        for (Map.Entry<Float, Integer> entry : isoLevels.entrySet()) {
            howClose = Math.abs(entry.getKey() - level);
            if (howClose < closest) {
                result = entry.getValue();
            }
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
    public float getTempIso(int coordX, int coordY, int coordLvl) {
        float result = getValFromVar(varNameTempIso, coordX, coordY, coordLvl, 4);
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
    public float getTemp2m(int coordX, int coordY) {
        float result = getValFromVar(varNameTemp2m, coordX, coordY, 4);
        return result;
    }

    /**
     * Get dew point at a given XY-coordinate and isobaric level index. Note
     * that the GRIB files from NOAA forecasting models output relative
     * humidity, not dew point, for the various isobaric levels so this method
     * converts that to dew point.
     *
     * @param coordX   x-coordinate in data grid
     * @param coordY   y-coordinate in data grid
     * @param coordLvl index of isobaric level in data grid
     *
     * @return dew point in K
     */
    public float getDewpIso(int coordX, int coordY, int coordLvl) {
        float temp = getTempIso(coordX, coordY, coordLvl);
        float rh = getValFromVar(varNameRHIso, coordX, coordY, coordLvl, 4);
        float pres = getLevelFromIndex(coordLvl);
        float result = AtmosThermoMath.calcDewp(temp, pres, rh);
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
    public float getDewp2m(int coordX, int coordY) {
        float result = -1;

        if (modelIsGRB) {
            result = getValFromVar(varNameDewp2mNAM, coordX, coordY, 4);
        } else {
            result = getValFromVar(varNameDewp2m, coordX, coordY, 4);
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
    public float getPresSfc(int coordX, int coordY) {
        float result = getValFromVar(varNamePresSfc, coordX, coordY, 3);
        return result;
    }

    /**
     * Get convective available potential energy (CAPE) at a given
     * XY-coordinate.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return CAPE in J/kg
     */
    public float getCAPE(int coordX, int coordY) {
        float result = -1;

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
    public float getCIN(int coordX, int coordY) {
        float result = getValFromVar(varNameCin, coordX, coordY, 3);
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
    public float getLFTX(int coordX, int coordY) {
        float result = -1;

        if (modelIsGFS3) {
            result = getValFromVar(varNameLftxGFS3, coordX, coordY, 3);
        } else if (modelIsGRB) {
            result = getValFromVar(varNameLftxGRB, coordX, coordY, 4);
        } else if (modelIsGFS4) {
            result = getValFromVar(varNameLftxGFS4, coordX, coordY, 3);
        } else {
            result = getValFromVar(varNameLftx, coordX, coordY, 4);
        }

        return result;
    }

    /**
     * Get mean sea level pressure at a given XY-coordinate. This is the surface
     * pressure at a location adjusted to what it might be if that location was
     * at mean sea level, making it useful in comparing surface pressures of
     * various locations. Note that different forecasting models may use
     * different algorithms to calculate this.
     *
     * @param coordX x-coordinate in data grid
     * @param coordY y-coordinate in data grid
     *
     * @return mean sea level pressure in Pa
     */
    public float getMSL(int coordX, int coordY) {
        float result = -1;

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
     * approximation that is computed essentially the way it would be done by
     * hand on a Skew-T plot. In other words, the dry adiabat intersected by the
     * surface (really, 2m) temperature and the saturation mixing ratio line
     * intersected by the surface (again, really 2m) dew point are followed
     * vertically until they intersect. Both the isobaric level and the
     * temperature at which this occurs are provided, though only the pressure
     * is what is referred to as the LCL.
     *
     * @param coordX x-coordinate in the data grid
     * @param coordY y-coordinate in the data grid
     *
     * @return LCL as float[2]; [0] = pressure in Pa, [1] = temperature in K
     */
    public float[] getLCL(int coordX, int coordY) {
        float curTemp2m = getTemp2m(coordX, coordY);
        float curDewp2m = getDewp2m(coordX, coordY);
        float curPresSfc = getPresSfc(coordX, coordY);

        float[] result = AtmosThermoMath.calcLCL(curTemp2m, curDewp2m, curPresSfc);

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
    public float getTotalTotals(int coordX, int coordY) {
        float curTemp500 = getTempIso(coordX, coordY, getIndexFromLevel(50000));
        float curTemp850 = getTempIso(coordX, coordY, getIndexFromLevel(85000));
        float curDewp500 = getDewpIso(coordX, coordY, getIndexFromLevel(50000));
        float curDewp850 = getDewpIso(coordX, coordY, getIndexFromLevel(85000));

        float result = AtmosThermoMath.calcTotalTotals(
                curTemp500, curTemp850, curDewp500, curDewp850);

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
    public float getKIndex(int coordX, int coordY) {
        float curTemp500 = getTempIso(coordX, coordY, getIndexFromLevel(50000));
        float curTemp700 = getTempIso(coordX, coordY, getIndexFromLevel(70000));
        float curTemp850 = getTempIso(coordX, coordY, getIndexFromLevel(85000));
        float curDewp700 = getDewpIso(coordX, coordY, getIndexFromLevel(70000));
        float curDewp850 = getDewpIso(coordX, coordY, getIndexFromLevel(85000));

        float result = AtmosThermoMath.calcKIndex(
                curTemp500, curTemp700, curTemp850, curDewp700, curDewp850);

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
    public float getSWEAT(int coordX, int coordY) {
        float curTotalTotals = getTotalTotals(coordX, coordY);

        float uGrd500 = getValFromVar(varNameUGrd, coordX, coordY, getIndexFromLevel(50000), 4);
        float uGrd850 = getValFromVar(varNameUGrd, coordX, coordY, getIndexFromLevel(85000), 4);
        float vGrd500 = getValFromVar(varNameVGrd, coordX, coordY, getIndexFromLevel(50000), 4);
        float vGrd850 = getValFromVar(varNameVGrd, coordX, coordY, getIndexFromLevel(85000), 4);
        float curDewp850 = getDewpIso(coordX, coordY, getIndexFromLevel(85000));

        float result = AtmosThermoMath.calcSWEAT(
                curTotalTotals, curDewp850, uGrd500, vGrd500, uGrd850, vGrd850);

        return result;
    }

    /**
     * Get analysis time of data file.
     *
     * @return analysis time
     */
    public LocalDateTime getAnalysisTime() {
        String gribTimeUnits = gribFile.findVariable("reftime").getUnitsString();
        DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("'Hour since 'uuuu-MM-dd'T'HH:mm:ssX", Locale.US);
        dtFormat.withResolverStyle(ResolverStyle.STRICT);
        LocalDateTime gribAnalTime = LocalDateTime.parse(gribTimeUnits, dtFormat);
        return gribAnalTime;
    }

    /**
     * Get valid time of data file. This typically corresponds to the forecast
     * time in the data file.
     *
     * @return forecast time
     */
    public LocalDateTime getValidTime() {
        String gribTimeUnits = gribFile.findVariable("time").getUnitsString();
        DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("'Hour since 'uuuu-MM-dd'T'HH:mm:ssX", Locale.US);
        dtFormat.withResolverStyle(ResolverStyle.STRICT);
        LocalDateTime gribAnalTime = LocalDateTime.parse(gribTimeUnits, dtFormat);

        int gribTimeOffset = 0;
        try {
            gribTimeOffset = gribFile.findVariable("time").read().reduce().getInt(0);
        } catch (IOException ex) {
            LOG.error("Can't read valid time from file, returning analysis time");
            return gribAnalTime;
        }

        LocalDateTime gribValidTime = gribAnalTime.plusHours(gribTimeOffset);

        return gribValidTime;
    }

    /**
     * Retrieve isobaric levels available in the data file. Only those levels
     * which are between and include 100hPa to 1000hPa and are in 25hPa
     * increments are used. Isobaric levels are converted from hPa to Pa as
     * necessary.
     *
     * @return true if successful
     */
    private boolean doGetLevels() {
        String varNameIso;
        int initCoordLvl = 0;

        if (modelIsGFS3) {
            varNameIso = "isobaric1";
        } else if (modelIsNAMGRB2) {
            varNameIso = "isobaric1";
            // Need to skip first 2 isobaric levels
            initCoordLvl = 2;
        } else {
            varNameIso = "isobaric";
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
                if (curLevel >= 10000 && curLevel <= 100000
                        && (curLevel % 2500) == 0) {
                    // GRIB2 files use Pa
                    isoLevels.put(new Float(curLevel), coordLvl);
                } else if (curLevel >= 100 && curLevel <= 1000
                        && (curLevel % 25) == 0 && modelIsGRB) {
                    // GRIB1 files use hPa, must conver to Pa
                    isoLevels.put(new Float(curLevel * 100), coordLvl);
                }
            }
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
    private float getValFromVar(String varName,
            int coordX, int coordY, int varDim) {
        return getValFromVar(varName, coordX, coordY, 0, varDim);
    }

    /**
     * Retrieve a given variable's value at a particular XY-coordinate and
     * isobaric level index.
     *
     * @param varName  name of variable to retrieve
     * @param coordX   x-coordinate in data grid
     * @param coordY   y-coordinate in data grid
     * @param coordLvl index of isobaric level in data grid
     * @param varDim   expected dimensions of grid for variable
     *
     * @return value of variable at XY-coordinate, -99999 if not found
     */
    private float getValFromVar(String varName,
            int coordX, int coordY, int coordLvl, int varDim) {
        final float errorVal = -99999;
        float result = errorVal;

        int[] arrayOrigin = null;
        int[] arraySize = null;

        switch (varDim) {
            case 2:
                arrayOrigin = new int[]{coordY, coordX};
                arraySize = new int[]{1, 1};
                break;
            case 3:
                arrayOrigin = new int[]{coordLvl, coordY, coordX};
                arraySize = new int[]{1, 1, 1};
                break;
            case 4:
                arrayOrigin = new int[]{0, coordLvl, coordY, coordX};
                arraySize = new int[]{1, 1, 1, 1};
                break;
            default:
                LOG.error("Invalid array dimension specified.");
                return errorVal;
        }

        try {
            // Successful only if an exception doesn't occur here
            result = gribFile.findVariable(varName).read(arrayOrigin, arraySize)
                    .reduce().getFloat(0);
        } catch (IOException | InvalidRangeException | NullPointerException ex) {
            /*
             * These exceptions almost invariably point to programmer error.
             * Make sure the variable name and the dimensions are correct for
             * the particular type of data file. Use the UCAR Unidata Tools UI
             * to inspect the data file you are trying to read to verify.
             */
            LOG.error("Can't read variable: {}\n{}", varName, ex.getLocalizedMessage());
            return errorVal;
        }
        return result;
    }
}
