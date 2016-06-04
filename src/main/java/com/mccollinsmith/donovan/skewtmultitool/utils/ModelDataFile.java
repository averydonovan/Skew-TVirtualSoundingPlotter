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
package com.mccollinsmith.donovan.skewtmultitool.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Variable;
//import ucar.nc2.Dimension;
//import ucar.nc2.NCdumpW;
import ucar.nc2.dt.*;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.ma2.*;
//import java.lang.Math;

/**
 * Loads GRIB files, reads in longitudes and latitudes of data points, and
 * provides access to various parameters useful in generating Skew-T plots.
 *
 * @author Donovan Smith <donovan@mccollinsmith.com>
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
            = "Surface_lifted_index_layer_between_two_isobaric_layer";
    private final String varNameUGrd = "u-component_of_wind_isobaric";
    private final String varNameVGrd = "v-component_of_wind_isobaric";

    //private float[] isoLevels = null;
    private Map<Float, Integer> isoLevels = null;
    private double[][] lons = null;
    private double[][] lats = null;
    private float[][][] tempIso = null;
    private float[][][] dewpIso = null;
    private float[][] temp2m = null;
    private float[][] dewp2m = null;
    private float[][] presSfc = null;
    private float[][] mslSfc = null;
    private float[][] lclPres = null;
    private float[][] lclTemp = null;
    private float[][] capeSfc = null;
    private float[][] cinSfc = null;
    private float[][] lftx = null;
    private float[][] totalTotals = null;
    private float[][] kIndex = null;
    private float[][] sweat = null;

    private int maxX = 0;
    private int maxY = 0;
    private int maxLevel = 0;

    private boolean modelIsGRB = false;
    private boolean modelIsGFS3 = false;
    private boolean modelIsGFS4 = false;

    private AtmosThermoMath atMath = new AtmosThermoMath();

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
        open(gribFileName);
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

        if (gribFileName.endsWith(".grb") || gribFileName.endsWith(".grib")) {
            modelIsGRB = true;
        } else {
            modelIsGRB = false;
        }

        if (gribFileName.contains("gfs_4_")
                || (gribFileName.contains("z.pgrb2.") && gribFileName.contains("gfs"))) {
            modelIsGFS4 = true;
        } else {
            modelIsGFS4 = false;
        }

        if (gribFileName.contains("gfs_3_") && modelIsGRB) {
            modelIsGFS3 = true;
        } else {
            modelIsGFS3 = false;
        }

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
        maxX = varShape[varShape.length - 1];
        maxY = varShape[varShape.length - 2];
        maxLevel = varShape[varShape.length - 3];

        doGetLevels();

        LOG.debug("Successfully opened GRIB file: {}", gribFileName);

        return true;
    }

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

    public double[] getLonLatFromXYCoords(int coordX, int coordY) {
        LatLonPoint ptLatLon = gribGCS.getLatLon(coordX, coordY);
        double[] result = {ptLatLon.getLongitude(), ptLatLon.getLatitude()};
        return result;
    }

    public int[] getXYCoordsFromLonLat(double lon, double lat) {
        int[] result = gribGCS.findXYindexFromLatLonBounded(lat, lon, null);
        return result;
    }

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

    public float getTempIso(int coordX, int coordY, int coordLvl) {
        float result = getValFromVar(varNameTempIso, coordX, coordY, coordLvl, 4);
        return result;
    }

    public float getTemp2m(int coordX, int coordY) {
        float result = getValFromVar(varNameTemp2m, coordX, coordY, 4);
        return result;
    }

    public float getDewpIso(int coordX, int coordY, int coordLvl) {
        float temp = getTempIso(coordX, coordY, coordLvl);
        float rh = getValFromVar(varNameRHIso, coordX, coordY, coordLvl, 4);
        float pres = getLevelFromIndex(coordLvl);
        float result = atMath.calcDewp(temp, pres, rh);
        return result;
    }

    public float getDewp2m(int coordX, int coordY) {
        float result = -1;

        if (modelIsGRB) {
            result = getValFromVar(varNameDewp2mNAM, coordX, coordY, 4);
        } else {
            result = getValFromVar(varNameDewp2m, coordX, coordY, 4);
        }

        return result;
    }

    public float getPresSfc(int coordX, int coordY) {
        float result = getValFromVar(varNamePresSfc, coordX, coordY, 3);
        return result;
    }

    public float getCAPE(int coordX, int coordY) {
        float result = -1;

        if (modelIsGRB) {
            result = getValFromVar(varNameCapeGRB, coordX, coordY, 3);
        } else {
            result = getValFromVar(varNameCape, coordX, coordY, 3);
        }

        return result;
    }

    public float getCIN(int coordX, int coordY) {
        float result = getValFromVar(varNameCin, coordX, coordY, 3);
        return result;
    }

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

    public float getMSL(int coordX, int coordY) {
        float result = -1;

        if (modelIsGRB) {
            result = getValFromVar(varNameMslGRB, coordX, coordY, 3);
        } else if (modelIsGFS4) {
            result = getValFromVar(varNameMslGFS4, coordX, coordY, 3);
        } else {
            result = getValFromVar(varNameMsl, coordX, coordY, 3);
        }

        return result;
    }

    public float[] getLCL(int coordX, int coordY) {
        float curTemp2m = getTemp2m(coordX, coordY);
        float curDewp2m = getDewp2m(coordX, coordY);
        float curPresSfc = getPresSfc(coordX, coordY);

        float[] result = atMath.calcLCL(curTemp2m, curDewp2m, curPresSfc);

        return result;
    }

    private boolean doGetLevels() {
        String varNameIso;

        if (modelIsGFS3) {
            varNameIso = "isobaric1";
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

            for (int coordLvl = 0; coordLvl < maxLevel; coordLvl++) {
                idxIso.set(coordLvl);
                Integer curLevel = gribVarDataIso.getInt(idxIso);
                if (curLevel >= 10000 && curLevel <= 100000
                        && (curLevel % 2500) == 0) {
                    isoLevels.put(new Float(curLevel), coordLvl);
                } else if (curLevel >= 100 && curLevel <= 1000
                        && (curLevel % 25) == 0 && modelIsGRB) {
                    isoLevels.put(new Float(curLevel * 100), coordLvl);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean doGetTKS() {
        totalTotals = new float[maxX][maxY];
        kIndex = new float[maxX][maxY];
        sweat = new float[maxX][maxY];

        Array gribVarDataUGrd = null;
        Array gribVarDataVGrd = null;
        try {
            gribVarDataUGrd = gribFile.findVariable(varNameUGrd).read().reduce();
            gribVarDataVGrd = gribFile.findVariable(varNameVGrd).read().reduce();
        } catch (java.io.IOException ex) {
            LOG.error("{}\n{}", ex.getLocalizedMessage(), ex.toString());
        }
        if (gribVarDataUGrd != null && gribVarDataVGrd != null) {
            Index idx500 = gribVarDataUGrd.getIndex();
            Index idx850 = gribVarDataUGrd.getIndex();

            int level500 = getIndexFromLevel(50000);
            int level700 = getIndexFromLevel(70000);
            int level850 = getIndexFromLevel(85000);

            float temp500 = 0;
            float temp700 = 0;
            float temp850 = 0;
            float dewp500 = 0;
            float dewp700 = 0;
            float dewp850 = 0;

            float uGrd500 = 0;
            float vGrd500 = 0;
            float uGrd850 = 0;
            float vGrd850 = 0;

            for (int coordX = 0; coordX < maxX; coordX++) {
                for (int coordY = 0; coordY < maxY; coordY++) {
                    idx500.set(level500, coordY, coordX);
                    idx850.set(level850, coordY, coordX);

                    temp500 = tempIso[level500][coordX][coordY];
                    temp700 = tempIso[level700][coordX][coordY];
                    temp850 = tempIso[level850][coordX][coordY];
                    dewp500 = dewpIso[level500][coordX][coordY];
                    dewp700 = dewpIso[level700][coordX][coordY];
                    dewp850 = dewpIso[level850][coordX][coordY];

                    uGrd500 = gribVarDataUGrd.getFloat(idx500);
                    vGrd500 = gribVarDataVGrd.getFloat(idx500);
                    uGrd850 = gribVarDataUGrd.getFloat(idx850);
                    vGrd850 = gribVarDataVGrd.getFloat(idx850);

                    totalTotals[coordX][coordY]
                            = atMath.calcTotalTotals(temp500, temp850, dewp500, dewp850);
                    kIndex[coordX][coordY]
                            = atMath.calcKIndex(temp500, temp700, temp850,
                                    dewp500, dewp850);
                    sweat[coordX][coordY] = atMath.calcSweat(totalTotals[coordX][coordY],
                            dewp850, uGrd500, vGrd500, uGrd850, vGrd850);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private float getValFromVar(String varName,
            int coordX, int coordY, int varDim) {
        return getValFromVar(varName, coordX, coordY, 0, varDim);
    }

    private float getValFromVar(String varName,
            int coordX, int coordY, int coordLvl, int varDim) {
        float result = -1;

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
                return -1;
        }

        try {
            result = gribFile.findVariable(varName).read(arrayOrigin,
                    arraySize).reduce().getFloat(0);
        } catch (IOException | InvalidRangeException | NullPointerException ex) {
            LOG.error("Can't read variable: {}", ex.getLocalizedMessage());
            return -1;
        }
        return result;
    }
}
