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

/**
 * Various methods that are useful in atmospheric thermodynamics calculations.
 * All methods use base SI units (e.g. K, Pa, m). Parameters and returned values
 * are floats.
 *
 * @author Donovan Smith <donovan@mccollinsmith.com>
 */
public class AtmosThermoMath {

    /**
     * Calculates total totals (TT) index.
     *
     * @param temp500 temperature at 500hPa, in K
     * @param temp850 temperature at 850hPa, in K
     * @param dewp500 dew point at 500hPa, in K
     * @param dewp850 dew point at 850hPa, in K
     *
     * @return TT index
     */
    public static float calcTotalTotals(float temp500, float temp850,
            float dewp500, float dewp850) {
        double totVt = temp850 - temp500;
        double totCt = dewp850 - dewp500;
        double result = totVt + totCt;
        return (float) result;
    }

    /**
     * Calculates K-index.
     *
     * @param temp500 temperature at 500hPa, in K
     * @param temp700 temperature at 700hPa, in K
     * @param temp850 temperature at 850hPa, in K
     * @param dewp700 dew point at 700hPa, in K
     * @param dewp850 dew point at 850hPa, in K
     *
     * @return K-index
     */
    public static float calcKIndex(float temp500, float temp700, float temp850,
            float dewp700, float dewp850) {
        double result = Math.abs(temp850 - temp500) + (dewp850 - (temp700 - dewp700));
        return (float) result;
    }

    /**
     * Calculate dew point of air.
     *
     * @param temp temperature in K
     * @param pres pressure in Pa
     * @param rh   relative humidity in %
     *
     * @return dew point in K
     */
    public static float calcDewp(float temp, float pres, float rh) {
        rh = rh / 100;
        double result = tmr(w(temp, pres) * rh, pres);
        return (float) result;
    }

    /**
     * Calculates lifting condensation level (LCL).
     *
     * @param temp 2m surface temperature in K
     * @param dewp 2m surface dew point in K
     * @param pres surface pressure in Pa
     *
     * @return LCL as float[2]; [0] = pressure in Pa, [1] = temperature in K
     */
    public static float[] calcLCL(float temp, float dewp, float pres) {
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

    /**
     * Calculates Severe WEAther Threat (SWEAT) index. Total totals (TT) index
     * must be provided or calculated using
     * {@link #calcTotalTotals(float, float, float, float) calcTotalTotals}.
     *
     * @param totalTotals TT index
     * @param dewp850     dew point at 850hPa, in K
     * @param uGrd500     u-component of wind at 500hPa, in m/s
     * @param vGrd500     v-component of wind at 500hPa, in m/s
     * @param uGrd850     u-component of wind at 850hPa, in m/s
     * @param vGrd850     v-component of wind at 850hPa, in m/s
     *
     * @return SWEAT index
     */
    public static float calcSweat(float totalTotals, float dewp850,
            float uGrd500, float vGrd500, float uGrd850, float vGrd850) {
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

        double result = sweat1 + sweat2 + sweat3 + sweat4 + sweat5;
        return (float) result;
    }

    /*
     * Internal-use functions. Not that all return doubles to reduce rounding
     * errors, although externally accessible functions return floats.
     */
    private static double[] calcWindFromVec(double uGrd, double vGrd) {
        double windSpeed = Math.sqrt(Math.pow(uGrd, 2.0) + Math.pow(vGrd, 2.0));
        double windDir = Math.atan2(vGrd, uGrd);
        double[] results = {windSpeed, windDir};
        return results;
    }

    // Potential temperature in K from temp in K, pressure in Pa
    private static double pot_temp(double temp, double pres) {
        double result = temp * Math.pow(pres / 100000.0, -2.0 / 7);
        return result;
    }

    // Temperature of air in K from potential temperature in K, pressure in Pa
    private static double from_pot_temp(double pot_temp, double pres) {
        double result = pot_temp * Math.pow(pres / 100000.0, 2.0 / 7);
        return result;
    }

    /*
     * The following functions are based on IDL code found at:
     * http://cimss.ssec.wisc.edu/camex3/archive/quicklooks/skewt.pro
     */
    // Temperature of air (K) at a given mixing ratio (g/kg) and pressure (Pa)
    private static double tmr(double w, double p) {
        p = p / 100; //Convert Pa to hPa
        double x = Math.log10(w * p / (622.0 + w));
        double result = Math.pow(10, 0.0498646455 * x + 2.4082965) - 7.07475
                + 38.9114 * Math.pow(Math.pow(10, 0.0915 * x) - 1.2035, 2.0);
        return result;
    }

    // Saturated mixing ratio in g/kg from temp in K, pres in Pa
    private static double w(double temp, double pres) {
        double result = 0;

        pres = pres / 100; // Convert Pa to hPa

        if (temp < 999) {
            double x = esat(temp) / 100; // Convert sat. pres. from Pa to hPa
            result = 621.97 * x / (pres - x);
        }
        return result;
    }

    // Saturated pressure in Pa from temp in K
    private static double esat(double temp) {
        temp -= 273.15;
        double result = 6.1078 * Math.exp((17.2693882 * temp) / (temp + 237.3));
        result = result * 100; // Convert hPa to Pa
        return result;
    }
}
