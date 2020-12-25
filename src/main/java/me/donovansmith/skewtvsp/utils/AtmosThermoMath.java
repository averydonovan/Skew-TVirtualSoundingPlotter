/*
 * Copyright (c) 2018, Donovan Smith
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
package me.donovansmith.skewtvsp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various methods that are useful in atmospheric thermodynamics calculations.
 * All methods use base SI units (e.g. K, Pa, m).
 *
 * @author Donovan Smith
 */
public class AtmosThermoMath {

    private static final Logger LOG = LoggerFactory.getLogger(ModelDataFile.class.getName());

    private static final double C_TO_K = 273.15;

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
    public static double calcTotalTotals(double temp500, double temp850, double dewp500,
            double dewp850) {
        double totVt = temp850 - temp500;
        double totCt = dewp850 - dewp500;
        double result = totVt + totCt;
        return result;
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
    public static double calcKIndex(double temp500, double temp700, double temp850, double dewp700,
            double dewp850) {
        // Need to convert K to C
        temp500 -= C_TO_K;
        temp700 -= C_TO_K;
        temp850 -= C_TO_K;
        dewp700 -= C_TO_K;
        dewp850 -= C_TO_K;

        double result = (temp850 - temp500) + (dewp850 - (temp700 - dewp700));
        return result;
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
    public static double calcDewp(double temp, double pres, double rh) {
        rh = rh / 100.0f;
        double result = calcTempAtMixingRatio(w(temp, pres) * rh, pres);
        return result;
    }

    /**
     * Calculates lifting condensation level (LCL).
     *
     * @param temp 2m surface temperature in K
     * @param dewp 2m surface dew point in K
     * @param pres surface pressure in Pa
     *
     * @return LCL as double[2]; [0] = pressure in Pa, [1] = temperature in K
     */
    public static double[] calcLCL(double temp, double dewp, double pres) {
        final double stepSize = 100.0;
        double pt = pot_temp(temp, pres);
        double w_0 = w(dewp, pres);
        double w_s = w(temp, pres);
        double delta = stepSize * 10.0;
        double lcl = Math.ceil(pres / 100.0) * 100.0;
        double pt_l = 0.0;
        while (Math.abs(delta) > 0.1 && lcl > 10000.0) {
            lcl -= stepSize;
            pt_l = calcTempFromPot(pt, lcl);
            delta = w(pt_l, lcl) - w_0;
        }
        double[] result = { lcl, pt_l };
        return result;
    }

    /**
     * Calculates Severe WEAther Threat (SWEAT) index. Total totals (TT) index must
     * be provided or calculated using
     * {@link #calcTotalTotals(double, double, double, double) calcTotalTotals}.
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
    public static double calcSWEAT(double totalTotals, double dewp850, double uGrd500,
            double vGrd500, double uGrd850, double vGrd850) {
        double[] wind500 = calcWindFromVec(uGrd500, vGrd500);
        double[] wind850 = calcWindFromVec(uGrd850, vGrd850);
        double windSpd500 = wind500[0];
        double windDir500 = wind500[1];
        double windSpd850 = wind850[0];
        double windDir850 = wind850[1];

        // Need to convert K to C
        dewp850 -= C_TO_K;

        double sweat1 = 12.0 * dewp850;
        if (sweat1 < 0) {
            sweat1 = 0;
        }

        double sweat2 = 20.0 * (totalTotals - 49.0);
        if (sweat2 < 0) {
            sweat2 = 0;
        }

        double sweat3 = 2.0 * windSpd850;
        if (sweat3 < 0) {
            sweat3 = 0;
        }

        double sweat4 = windSpd500;
        if (sweat4 < 0) {
            sweat4 = 0;
        }

        double sweat5 = 125.0 * (Math.sin(windDir500 - windDir850) + 0.2);
        if (sweat5 < 0) {
            sweat5 = 0;
        }

        double result = sweat1 + sweat2 + sweat3 + sweat4 + sweat5;
        return result;
    }

    /*
     * All methods in the remainder of this class both take doubles as arguments and
     * return doubles.
     */
    /**
     * Calculate temperature of air from its potential temperature.
     *
     * @param pot_temp potential temperature in K
     * @param pres     pressure in Pa
     *
     * @return temperature in K
     */
    public static double calcTempFromPot(double pot_temp, double pres) {
        double result = pot_temp * Math.pow(pres / 100000.0, 2.0 / 7.0);
        return result;
    }

    /**
     * Calculate potential temperature of dry air at a given temperature and
     * isobaric level.
     *
     * @param temp temperature in K
     * @param pres pressure in Pa
     *
     * @return potential temperature in K
     */
    private static double pot_temp(double temp, double pres) {
        double result = temp * Math.pow(pres / 100000.0, -2.0 / 7.0);
        return result;
    }

    /**
     * Calculates wind velocity and speed components from zonal and meridional wind
     * components. Note that if using the u-component and v-component of wind from
     * gridded data whose grid is skewed in relation to its central longitude and
     * latitude lines, there will be an error in the resulting wind direction. Also
     * note that the wind direction is output in radians.
     *
     * @param uGrd zonal component of wind
     * @param vGrd meridional component of wind
     *
     * @return wind as double[2]; [0] = speed in m/s, [1] = direction in radians
     */
    private static double[] calcWindFromVec(double uGrd, double vGrd) {
        double windSpeed = Math.sqrt(Math.pow(uGrd, 2.0) + Math.pow(vGrd, 2.0));
        double windDir = Math.atan2(vGrd, uGrd);
        double[] results = { windSpeed, windDir };
        return results;
    }

    /*
     * The following functions are based on IDL code found at:
     * http://cimss.ssec.wisc.edu/camex3/archive/quicklooks/skewt.pro
     */
    /**
     * Calculate temperature of moist air at a given mixing ratio and isobaric
     * level.
     *
     * @param w mixing ratio in g/kg
     * @param p pressure in Pa
     *
     * @return temperature in K
     */
    public static double calcTempAtMixingRatio(double w, double p) {
        p = p / 100.0; // Convert Pa to hPa
        double x = Math.log10(w * p / (622.0 + w));
        double result = Math.pow(10.0, 0.0498646455 * x + 2.4082965) - 7.07475
                        + 38.9114 * Math.pow(Math.pow(10.0, 0.0915 * x) - 1.2035, 2.0);
        return result;
    }

    /**
     * Calculate temperature of air when following a saturated adiabat.
     *
     * @param os   saturated potential temperature in K
     * @param pres pressure in Pa
     *
     * @return temperature in K
     */
    public static double calcTempSatAdiabat(double os, double pres) {
        double tq = 253.15;
        double d = 120.0;
        double x = 0.0;
        for (int i = 0; i < 13; i++) {
            d = d / 2.0;
            x = os * Math.exp(-2.6518986 * w(tq, pres) / tq)
                - tq * Math.pow((100000.0 / pres), (2.0 / 7.0));
            if (Math.abs(x) < 0.01) {
                break;
            } else {
                d = Math.copySign(d, x);
                tq += d;
            }
        }
        return tq;
    }

    /**
     * Calculates saturated potential temperature of moist air.
     *
     * @param temp temperature in K
     * @param pres pressure in Pa
     *
     * @return saturated potential temperature in K
     */
    public static double calcSatPotTemp(double temp, double pres) {
        double os = temp * Math.pow((100000.0 / pres), (2.0 / 7.0))
                    / Math.exp(-2.6518986 * (w(temp, pres) / temp));
        return os;
    }

    // in g/kg from temp in K, pres in Pa
    /**
     * Calculate saturated mixing ratio of moist air.
     *
     * @param temp temperature in K
     * @param pres pressure in Pa
     *
     * @return saturated mixing ratio in g/kg
     */
    private static double w(double temp, double pres) {
        double result = 0;

        pres = pres / 100.0; // Convert Pa to hPa

        if (temp < 999) {
            double x = esat(temp) / 100; // Convert sat. pres. from Pa to hPa
            result = 621.97 * x / (pres - x);
        }
        return result;
    }

    /**
     * Calculate saturation pressure of moist air.
     *
     * @param temp temperature in K
     *
     * @return pressure in Pa
     */
    private static double esat(double temp) {
        temp -= 273.15;
        double result = 6.1078 * Math.exp((17.2693882 * temp) / (temp + 237.3));
        result = result * 100.0; // Convert hPa to Pa
        return result;
    }
}
