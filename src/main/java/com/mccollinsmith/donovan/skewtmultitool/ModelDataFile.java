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
package com.mccollinsmith.donovan.skewtmultitool;

import static com.mccollinsmith.donovan.skewtmultitool.SkewTMultiTool.statusConsole;
import java.io.IOException;
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
 *
 * @author Donovan Smith <donovan@mccollinsmith.com>
 */
public class ModelDataFile {

    private static NetcdfFile gribFile = null;

    private static float[] isoLevels = null;
    private static double[][] lons = null;
    private static double[][] lats = null;
    private static float[][][] tempIso = null;
    private static float[][][] dewpIso = null;
    private static float[][] temp2m = null;
    private static float[][] dewp2m = null;
    private static float[][] presSfc = null;
    private static float[][] mslSfc = null;
    private static float[][] lclPres = null;
    private static float[][] lclTemp = null;
    private static float[][] capeSfc = null;
    private static float[][] cinSfc = null;
    private static float[][] lftx = null;
    private static float[][] totalTotals = null;
    private static float[][] kIndex = null;
    private static float[][] sweat = null;

    private static int maxX = 0;
    private static int maxY = 0;
    private static int maxLevel = 0;

    public ModelDataFile() {
        // Do nothing
    }

    public ModelDataFile(String gribFileName) throws IOException {
        open(gribFileName);
    }

    public boolean open(String gribFileName) throws IOException {
        GridDataset gribGDS = null;
        String varName = "Temperature_isobaric";

        try {
            gribGDS = ucar.nc2.dt.grid.GridDataset.open(gribFileName);
        } catch (IOException ex) {
            statusConsole.println(ex);
            throw ex;
        }

        GridDatatype gribVarGDT = gribGDS.findGridByShortName(varName);
        GridCoordSystem varGCS = gribVarGDT.getCoordinateSystem();
        try {
            gribGDS.close();
        } catch (IOException ex) {
            statusConsole.println(ex);
            throw ex;
        }

        try {
            gribFile = NetcdfDataset.openDataset(gribFileName);
        } catch (IOException ex) {
            statusConsole.println(ex);
            throw ex;
        }

        Variable gribVar = gribFile.findVariable(varName);

        Array gribVarData = null;
        try {
            gribVarData = gribVar.read();
        } catch (IOException ex) {
            statusConsole.println(ex);
            throw ex;
        }

        if (gribVarData == null) {
            IOException ex = new IOException("Unusable file");
            statusConsole.println(ex);
            throw ex;
        }

        int[] varShape = gribVarData.getShape();
        maxX = varShape[varShape.length - 1];
        maxY = varShape[varShape.length - 2];
        maxLevel = varShape[varShape.length - 3];

        //statusConsole.println("Getting longitudes and latitudes...");
        doGetLonLats(varGCS);
        //statusConsole.println("Getting isobaric levels...");
        doGetLevels();
        //statusConsole.println("Getting temperatures and calculating dewpoints...");
        doGetTemps();
        //statusConsole.println("Getting surface variables...");
        doGetSfcVars();
        //statusConsole.println("Calculating LCLs...");
        doGetLcl();
        //statusConsole.println("Calculating Total-Totals, K-Index, and SWEAT...");
        doGetTKS();
        //statusConsole.println("Done!");
        
        return true;
    }

    public boolean close() throws IOException {
        try {
            gribFile.close();
            return true;
        } catch (IOException ex) {
            statusConsole.println(ex);
            throw ex;
        }
    }

    public int getIndexFromLevel(float isoLevel) {
        return (int) Math.round(((isoLevel - 10000) / 100) / 25);
    }

    public double getLat(int coordX, int coordY) {
        if (coordX < maxX && coordY < maxY) {
            return lats[coordX][coordY];
        } else {
            return 0;
        }
    }

    public double getLon(int coordX, int coordY) {
        if (coordX < maxX && coordY < maxY) {
            return lons[coordX][coordY];
        } else {
            return 0;
        }
    }

    public float getLevel(int coordLvl) {
        if (coordLvl < maxLevel) {
            return isoLevels[coordLvl];
        } else {
            return 0;
        }
    }

    public float getTempIso(int coordLvl, int coordX, int coordY) {
        if (coordLvl < maxLevel && coordX < maxX && coordY < maxY) {
            return tempIso[coordLvl][coordX][coordY];
        } else {
            return 0;
        }
    }

    public float getTemp2m(int coordX, int coordY) {
        if (coordX < maxX && coordY < maxY) {
            return temp2m[coordX][coordY];
        } else {
            return 0;
        }
    }

    public float getDewpIso(int coordLvl, int coordX, int coordY) {
        if (coordLvl < maxLevel && coordX < maxX && coordY < maxY) {
            return dewpIso[coordLvl][coordX][coordY];
        } else {
            return 0;
        }
    }

    public float getDewp2m(int coordX, int coordY) {
        if (coordX < maxX && coordY < maxY) {
            return dewp2m[coordX][coordY];
        } else {
            return 0;
        }
    }

    public float getLCLPres(int coordX, int coordY) {
        if (coordX < maxX && coordY < maxY) {
            return lclPres[coordX][coordY];
        } else {
            return 0;
        }
    }

    public float getLCLTemp(int coordX, int coordY) {
        if (coordX < maxX && coordY < maxY) {
            return lclTemp[coordX][coordY];
        } else {
            return 0;
        }
    }

    private void doGetLonLats(GridCoordSystem varGCS) {
        LatLonPoint ptLatLon = null;

        lons = new double[maxX][maxY];
        lats = new double[maxX][maxY];

        for (int coordX = 0; coordX < maxX; coordX++) {
            for (int coordY = 0; coordY < maxY; coordY++) {
                ptLatLon = varGCS.getLatLon(coordX, coordY);
                lons[coordX][coordY] = ptLatLon.getLongitude();
                lats[coordX][coordY] = ptLatLon.getLatitude();
            }
        }
    }

    private boolean doGetLevels() {
        String varNameIso = "isobaric";

        isoLevels = new float[maxLevel];
        Array gribVarDataIso = null;
        try {
            gribVarDataIso = gribFile.findVariable(varNameIso).read().reduce();
        } catch (java.io.IOException ex) {
            System.out.println(ex);
        }
        if (gribVarDataIso != null) {
            Index idxIso = gribVarDataIso.getIndex();

            for (int coordLvl = 0; coordLvl < maxLevel; coordLvl++) {
                idxIso.set(coordLvl);
                isoLevels[coordLvl] = gribVarDataIso.getFloat(idxIso);
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean doGetTemps() {
        String varNameTempIso = "Temperature_isobaric";
        String varNameTemp2m = "Temperature_height_above_ground";
        String varNameRHIso = "Relative_humidity_isobaric";
        String varNameDewp2m = "Dewpoint_temperature_height_above_ground";

        tempIso = new float[maxLevel][maxX][maxY];
        temp2m = new float[maxX][maxY];
        dewpIso = new float[maxLevel][maxX][maxY];
        dewp2m = new float[maxX][maxY];
        Array gribVarDataTempIso = null;
        Array gribVarDataTemp2m = null;
        Array gribVarDataRHIso = null;
        Array gribVarDataDewp2m = null;
        try {
            gribVarDataTempIso = gribFile.findVariable(varNameTempIso).read().reduce();
            gribVarDataTemp2m = gribFile.findVariable(varNameTemp2m).read().reduce();
            gribVarDataRHIso = gribFile.findVariable(varNameRHIso).read().reduce();
            gribVarDataDewp2m = gribFile.findVariable(varNameDewp2m).read().reduce();
        } catch (java.io.IOException ex) {
            System.out.println(ex);
        }
        if (gribVarDataTempIso != null && gribVarDataTemp2m != null
                && gribVarDataRHIso != null && gribVarDataDewp2m != null) {
            Index idxIso = gribVarDataTempIso.getIndex();
            Index idx2m = gribVarDataTemp2m.getIndex();
            Index idxSfc = gribVarDataDewp2m.getIndex();

            for (int coordX = 0; coordX < maxX; coordX++) {
                for (int coordY = 0; coordY < maxY; coordY++) {
                    for (int coordLvl = 0; coordLvl < maxLevel; coordLvl++) {
                        idxIso.set(coordLvl, coordY, coordX);
                        tempIso[coordLvl][coordX][coordY] = gribVarDataTempIso.getFloat(idxIso);
                        dewpIso[coordLvl][coordX][coordY] = calcDewp(
                                tempIso[coordLvl][coordX][coordY],
                                isoLevels[coordLvl],
                                gribVarDataRHIso.getFloat(idxIso));
                    }
                    idx2m.set(0, coordY, coordX);
                    idxSfc.set(coordY, coordX);
                    temp2m[coordX][coordY] = gribVarDataTemp2m.getFloat(idx2m);
                    dewp2m[coordX][coordY] = gribVarDataDewp2m.getFloat(idxSfc);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean doGetSfcVars() {
        String varNameCape = "Convective_available_potential_energy_surface";
        String varNameCin = "Convective_inhibition_surface";
        String varNamePres = "Pressure_surface";
        String varNameMsl = "MSLP_MAPS_System_Reduction_msl";
        String varNameLftx = "Surface_Lifted_Index_isobaric_layer";

        capeSfc = new float[maxX][maxY];
        cinSfc = new float[maxX][maxY];
        presSfc = new float[maxX][maxY];
        mslSfc = new float[maxX][maxY];
        lftx = new float[maxX][maxY];

        Array gribVarDataCape = null;
        Array gribVarDataCin = null;
        Array gribVarDataPres = null;
        Array gribVarDataMsl = null;
        Array gribVarDataLftx = null;
        try {
            gribVarDataCape = gribFile.findVariable(varNameCape).read().reduce();
            gribVarDataCin = gribFile.findVariable(varNameCin).read().reduce();
            gribVarDataPres = gribFile.findVariable(varNamePres).read().reduce();
            gribVarDataMsl = gribFile.findVariable(varNameMsl).read().reduce();
            gribVarDataLftx = gribFile.findVariable(varNameLftx).read().reduce();
        } catch (java.io.IOException ex) {
            System.out.println(ex);
        }
        if (gribVarDataCape != null) {
            Index idx = gribVarDataCape.getIndex();

            for (int coordX = 0; coordX < maxX; coordX++) {
                for (int coordY = 0; coordY < maxY; coordY++) {
                    idx.set(coordY, coordX);
                    capeSfc[coordX][coordY] = gribVarDataCape.getFloat(idx);
                    cinSfc[coordX][coordY] = gribVarDataCin.getFloat(idx);
                    presSfc[coordX][coordY] = gribVarDataPres.getFloat(idx);
                    mslSfc[coordX][coordY] = gribVarDataMsl.getFloat(idx);
                    lftx[coordX][coordY] = gribVarDataLftx.getFloat(idx);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private void doGetLcl() {
        float[] results = null;

        lclPres = new float[maxX][maxY];
        lclTemp = new float[maxX][maxY];

        for (int coordX = 0; coordX < maxX; coordX++) {
            for (int coordY = 0; coordY < maxY; coordY++) {
                results = calcLcl(
                        temp2m[coordX][coordY],
                        dewp2m[coordX][coordY],
                        presSfc[coordX][coordY]);
                lclPres[coordX][coordY] = results[0];
                lclTemp[coordX][coordY] = results[1];
            }
        }
    }

    private boolean doGetTKS() {
        String varNameUGrd = "u-component_of_wind_isobaric";
        String varNameVGrd = "v-component_of_wind_isobaric";

        totalTotals = new float[maxX][maxY];
        kIndex = new float[maxX][maxY];
        sweat = new float[maxX][maxY];

        Array gribVarDataUGrd = null;
        Array gribVarDataVGrd = null;
        try {
            gribVarDataUGrd = gribFile.findVariable(varNameUGrd).read().reduce();
            gribVarDataVGrd = gribFile.findVariable(varNameVGrd).read().reduce();
        } catch (java.io.IOException ex) {
            System.out.println(ex);
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
                            = calcTotalTotals(temp500, temp850, dewp500, dewp850);
                    kIndex[coordX][coordY]
                            = calcKIndex(temp500, temp700, temp850,
                                    dewp500, dewp850);
                    sweat[coordX][coordY] = calcSweat(totalTotals[coordX][coordY],
                            dewp850, uGrd500, vGrd500, uGrd850, vGrd850);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    // Saturated pressure in Pa from temp in K
    private double esat(double temp) {
        double result = 0;
        temp -= 273.15;
        result = 6.1078 * Math.exp((17.2693882 * temp) / (temp + 237.3));
        result = result * 100;  // Convert hPa to Pa
        return result;
    }

    // Saturated mixing ratio in g/kg from temp in K, pres in Pa
    private double w(double temp, double pres) {
        double result = 0;
        pres = pres / 100; // Convert Pa to hPa
        if (temp >= 999) {
            result = 0;
        } else {
            double x = esat(temp) / 100;  // Convert sat. pres. from Pa to hPa
            result = 621.97 * x / (pres - x);
        }
        return result;
    }

    // Temperature of air (K) at a given mixing ratio (g/kg) and pressure (Pa)
    private double tmr(double w, double p) {
        double result = 0;
        p = p / 100;    //Convert Pa to hPa
        double x = Math.log10(w * p / (622.0 + w));
        result = Math.pow(10, 0.0498646455 * x + 2.4082965) - 7.07475
                + 38.9114 * Math.pow((Math.pow(10, 0.0915 * x) - 1.2035), 2.0);
        return result;
    }

    // Potential temperature in K from temp in K, pressure in Pa
    private double pot_temp(double temp, double pres) {
        double result = temp * Math.pow(pres / 100000.0, -2.0 / 7);
        return result;
    }

    private double from_pot_temp(double pot_temp, double pres) {
        double result = pot_temp * Math.pow(pres / 100000.0, 2.0 / 7);
        return result;
    }

    private double[] calcWindFromVec(double uGrd, double vGrd) {
        double windSpeed = Math.sqrt(Math.pow(uGrd, 2.0) + Math.pow(vGrd, 2.0));
        double windDir = Math.atan2(vGrd, uGrd);
        double[] results = {windSpeed, windDir};
        return results;
    }

    /**
     * Calculate dewpoint temperature of air.
     *
     * @param temp temperature in K
     * @param pres pressure in Pa
     * @param rh relative humidity in %
     * @return dewpoint temperature in K
     */
    private float calcDewp(float temp, float pres, float rh) {
        double result = 0;
        rh = rh / 100;
        result = tmr((w(temp, pres) * rh), pres);
        return (float) result;
    }

    /**
     * Calculates lifting condensation level (LCL).
     *
     * @param temp 2m surface temperature in K
     * @param dewp 2m surface dewpoint temperature in K
     * @param pres surface pressure in Pa
     * @return LCL pressure in Pa, LCL temperature in K
     */
    // Lifting condensation level in Pa
    // Surface temp and dewp in K, surface pres in Pa
    private float[] calcLcl(float temp, float dewp, float pres) {
        //pres = pres * 100;
        double stepSize = 100;
        double pt = pot_temp(temp, pres);
        double w_0 = w(dewp, pres);
        double w_s = w(temp, pres);
        double delta = stepSize * 10;
        double lcl = Math.ceil(pres / 100) * 100;
        double pt_l = 0;
        while (Math.abs(delta) > stepSize && lcl > 10000) {
            lcl -= stepSize;
            pt_l = from_pot_temp(pt, lcl);
            delta = w(pt_l, lcl) - w_0;
        }
        float[] result = {(float) lcl, (float) pt_l};
        return result;
    }

    private float calcKIndex(
            float temp500, float temp700, float temp850,
            float dewp700, float dewp850) {
        double result = 0;
        result = Math.abs(temp850 - temp500) + (dewp850 - (temp700 - dewp700));
        return (float) result;
    }

    private float calcTotalTotals(
            float temp500, float temp850, float dewp500, float dewp850) {
        double result = 0;
        double totVt = (temp850 - temp500);
        double totCt = (dewp850 - dewp500);
        result = totVt + totCt;
        return (float) result;
    }

    private float calcSweat(
            float totalTotals, float dewp850,
            float uGrd500, float vGrd500, float uGrd850, float vGrd850) {
        double result = 0;

        double[] wind500 = calcWindFromVec(uGrd500, vGrd500);
        double[] wind850 = calcWindFromVec(uGrd850, vGrd850);
        double windSpd500 = wind500[0];
        double windDir500 = wind500[1];
        double windSpd850 = wind850[0];
        double windDir850 = wind850[1];

        double sweat1 = 12 * dewp850;
        if (sweat1 < 0) {
            sweat1 = 0;
        }

        double sweat2 = 20 * (totalTotals - 49);
        if (sweat2 < 0) {
            sweat2 = 0;
        }

        double sweat3 = 2 * windSpd850;
        if (sweat3 < 0) {
            sweat3 = 0;
        }

        double sweat4 = windSpd500;
        if (sweat4 < 0) {
            sweat4 = 0;
        }

        double sweat5 = 125 * (Math.sin(windDir500 - windDir850) + 0.2);
        if (sweat5 < 0) {
            sweat5 = 0;
        }

        result = sweat1 + sweat2 + sweat3 + sweat4 + sweat5;
        return (float) result;
    }
}
