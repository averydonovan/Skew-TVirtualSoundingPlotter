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
package com.mccollinsmith.donovan.skewtmultitool.controllers;

import com.mccollinsmith.donovan.skewtmultitool.utils.AtmosThermoMath;
import com.mccollinsmith.donovan.skewtmultitool.utils.ModelDataFile;
import java.util.ArrayList;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Donovan Smith <donovan@mccollinsmith.com>
 */
public class SkewTPlot {

    private static final Logger LOG
            = LoggerFactory.getLogger(ModelDataFile.class.getName());

    private static ModelDataFile mdfSkewTData = null;
    private static GraphicsContext gcSkewTPlot = null;

    private static int coordX = 0;
    private static int coordY = 0;

    private static double plotXOffset = 0;
    private static double plotYOffset = 0;
    private static double plotXMax = 0;
    private static double plotYMax = 0;
    private static double plotXStep = 0;
    private static double plotYStep = 0;
    private static double plotAvgStep = 0;
    private static double plotXRange = 0;
    private static double plotYRange = 0;
    private static double canvasWidth = 0;
    private static double canvasHeight = 0;

    private static final int HPA_TO_PA = 100;
    private static final double C_TO_K = 273.15;

    private static final int PLOT_MAX_STEPS = 400;
    private static final int PRES_MIN = 100 * HPA_TO_PA;
    private static final int PRES_BASE = 1000 * HPA_TO_PA;
    private static final int PRES_MAX = 1050 * HPA_TO_PA;
    private static final double TEMP_MIN = -50 + C_TO_K;
    private static final double TEMP_MAX = 50 + C_TO_K;
    private static final double[] W_LINES
            = new double[]{0.1, 0.5, 1, 1.5, 2, 3, 4, 6, 8, 10, 12, 15, 20,
                25, 30, 35, 40, 45};

    private static int[] presLevels;
    private static double[] tempSteps;
    private static double[] presAll;

    public SkewTPlot() {
        // Do nothing
    }

    public static void plotSkewT(GraphicsContext gcSkewT, ModelDataFile mdfInUse, int curX, int curY) {
        ArrayList<Integer> presLevelsList = new ArrayList<>();
        for (int count = (PRES_MAX - (50 * HPA_TO_PA)); count >= PRES_MIN; count -= (100 * HPA_TO_PA)) {
            presLevelsList.add(count);
        }
        presLevels = presLevelsList.stream().mapToInt(i -> i).toArray();

        ArrayList<Double> tempStepsList = new ArrayList<>();
        for (double count = TEMP_MIN; count <= TEMP_MAX; count += 10) {
            tempStepsList.add(count);
        }
        tempSteps = tempStepsList.stream().mapToDouble(d -> d).toArray();

        gcSkewTPlot = gcSkewT;
        mdfSkewTData = mdfInUse;

        coordX = curX;
        coordY = curY;

        double[] foundLonLat = mdfSkewTData.getLonLatFromXYCoords(coordX, coordY);

        canvasWidth = gcSkewTPlot.getCanvas().getWidth();
        canvasHeight = gcSkewTPlot.getCanvas().getHeight();
        plotXOffset = canvasWidth * 0.15;
        plotYOffset = canvasHeight * 0.85;
        plotXMax = canvasWidth * 0.90;
        plotYMax = canvasHeight * 0.10;
        plotXRange = Math.abs(plotXMax - plotXOffset);
        plotYRange = Math.abs(plotYMax - plotYOffset);

        plotXStep = plotXRange / PLOT_MAX_STEPS;
        plotYStep = plotYRange / PLOT_MAX_STEPS;
        plotAvgStep = (plotXStep + plotYStep) / 2;

        drawGridLines();

        plotTemps();

        drawAxes();
        drawTicksAndLabels();
    }

    private static void plotTemps() {
        ArrayList<Double> dataTempVals = new ArrayList<>();
        ArrayList<Double> dataDewpVals = new ArrayList<>();
        ArrayList<Double> dataPresLevels = new ArrayList<>();

        for (int coordLvl = 0; coordLvl < 50; coordLvl++) {
            Double curLevel = (double) mdfSkewTData.getLevelFromIndex(coordLvl);
            if (curLevel.intValue() >= 0) {
                dataPresLevels.add(curLevel);
                dataTempVals.add(new Double(mdfSkewTData.getTempIso(coordX, coordY, coordLvl)));
                dataDewpVals.add(new Double(mdfSkewTData.getDewpIso(coordX, coordY, coordLvl)));
            }
        }

        ArrayList<Double> xTempValsList = new ArrayList<>();
        ArrayList<Double> xDewpValsList = new ArrayList<>();
        ArrayList<Double> yValsList = new ArrayList<>();

        for (int count = 0; count < dataPresLevels.size(); count++) {
            double[] resultsTemp = getXYFromTempPres(dataTempVals.get(count),
                    dataPresLevels.get(count));
            double[] resultsDewp = getXYFromTempPres(dataDewpVals.get(count),
                    dataPresLevels.get(count));
            xTempValsList.add(resultsTemp[0]);
            xDewpValsList.add(resultsDewp[0]);
            yValsList.add(resultsTemp[1]);
        }

        double[] xTempVals = xTempValsList.stream().mapToDouble(d -> d).toArray();
        double[] xDewpVals = xDewpValsList.stream().mapToDouble(d -> d).toArray();
        double[] yVals = yValsList.stream().mapToDouble(d -> d).toArray();

        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);
        gcSkewTPlot.setLineWidth(1.5);
        gcSkewTPlot.strokePolyline(xTempVals, yVals, yVals.length);

        gcSkewTPlot.setFill(Color.RED);
        gcSkewTPlot.setStroke(Color.RED);
        gcSkewTPlot.setLineWidth(1.5);
        gcSkewTPlot.strokePolyline(xDewpVals, yVals, yVals.length);
    }

    private static void drawTicksAndLabels() {
        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);

        for (double curLevel : presLevels) {
            double y = getYFromPres(curLevel);

            gcSkewTPlot.setLineWidth(1);
            gcSkewTPlot.setTextAlign(TextAlignment.RIGHT);
            gcSkewTPlot.setTextBaseline(VPos.CENTER);
            gcSkewTPlot.setFont(Font.font("monospaced", FontWeight.NORMAL, 12 * plotAvgStep));
            gcSkewTPlot.strokeLine(plotXOffset, y, plotXOffset - (5 * plotAvgStep), y);
            gcSkewTPlot.setLineWidth(0);
            gcSkewTPlot.fillText(String.format("%.0f", curLevel / 100), plotXOffset - 6 * plotAvgStep, y);
        }

        for (double curStep : tempSteps) {
            double x = getXFromTempY(curStep, getYFromPres(PRES_BASE));

            gcSkewTPlot.setLineWidth(1);
            gcSkewTPlot.setTextAlign(TextAlignment.CENTER);
            gcSkewTPlot.setTextBaseline(VPos.TOP);
            gcSkewTPlot.setFont(Font.font("monospaced", FontWeight.NORMAL, 12 * plotAvgStep));
            gcSkewTPlot.strokeLine(x, plotYOffset, x, plotYOffset + (5 * plotAvgStep));
            gcSkewTPlot.setLineWidth(0);
            gcSkewTPlot.fillText(String.format("%.0f", curStep - C_TO_K), x, plotYOffset + 6 * plotAvgStep);
        }

        double yAxisLabelSize = 15 * plotAvgStep;
        double yAxisLabelX = (canvasWidth * 0.05) + yAxisLabelSize;
        double yAxisLabelY = (plotYRange / 2) + plotYMax;

        gcSkewTPlot.save();
        Rotate r = new Rotate(-90, yAxisLabelX, yAxisLabelY);
        gcSkewTPlot.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(), r.getTx(), r.getTy());
        gcSkewTPlot.setLineWidth(0);
        gcSkewTPlot.setTextAlign(TextAlignment.CENTER);
        gcSkewTPlot.setTextBaseline(VPos.CENTER);
        gcSkewTPlot.setFont(Font.font("monospaced", FontWeight.NORMAL, yAxisLabelSize));
        gcSkewTPlot.fillText("Pressure (hPa)", yAxisLabelX, yAxisLabelY);
        gcSkewTPlot.restore();

        double xAxisLabelSize = 15 * plotAvgStep;
        double xAxisLabelX = (plotXRange / 2) + plotXOffset;
        double xAxisLabelY = (canvasHeight * 0.95) - xAxisLabelSize;
        gcSkewTPlot.setLineWidth(0);
        gcSkewTPlot.setTextAlign(TextAlignment.CENTER);
        gcSkewTPlot.setTextBaseline(VPos.CENTER);
        gcSkewTPlot.setFont(Font.font("monospaced", FontWeight.NORMAL, yAxisLabelSize));
        gcSkewTPlot.fillText("Temperature (C)", xAxisLabelX, xAxisLabelY);
    }

    private static void drawGridLines() {
        gcSkewTPlot.setFill(Color.WHITE);
        gcSkewTPlot.setStroke(Color.WHITE);
        gcSkewTPlot.setLineWidth(0);
        gcSkewTPlot.fillRect(0, 0, canvasWidth, canvasHeight);

        for (int curPresLevel : presLevels) {
            drawIsobar(curPresLevel);
        }

        for (double curTempStep = (TEMP_MIN - (TEMP_MAX - TEMP_MIN));
                curTempStep <= TEMP_MAX;
                curTempStep += 10) {
            drawSkewTemp(curTempStep);
            drawDryAdiabat(curTempStep);
        }

        for (double curTempStep = (TEMP_MIN - (TEMP_MAX - TEMP_MIN));
                curTempStep <= TEMP_MAX;
                curTempStep += 5) {
            drawSatAdiabat(curTempStep);
        }

        drawMixRatios();
    }

    private static void drawAxes() {
        gcSkewTPlot.setFill(Color.WHITE);
        gcSkewTPlot.setStroke(Color.WHITE);
        gcSkewTPlot.setLineWidth(0);
        gcSkewTPlot.fillRect(0, 0, canvasWidth, plotYMax);
        gcSkewTPlot.fillRect(0, plotYOffset, canvasWidth, plotYMax - plotYOffset);
        gcSkewTPlot.fillRect(0, plotYMax, plotXOffset, plotYOffset - plotYMax);
        gcSkewTPlot.fillRect(plotXMax, plotYMax, canvasWidth - plotXMax, canvasHeight - plotYMax);

        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);
        gcSkewTPlot.setLineWidth(1);
        gcSkewTPlot.strokeLine(plotXOffset, plotYOffset, plotXOffset, plotYMax);
        gcSkewTPlot.strokeLine(plotXOffset, plotYOffset, plotXMax, plotYOffset);
    }

    private static void drawIsobar(int isoLevel) {
        double y = getYFromPres((double) isoLevel);
        gcSkewTPlot.setFill(Color.BLUE);
        gcSkewTPlot.setStroke(Color.BLUE);
        gcSkewTPlot.setLineWidth(0.5);
        gcSkewTPlot.strokeLine(plotXOffset, y, plotXMax, y);
    }

    private static void drawSkewTemp(double tempStep) {
        double y1 = getYFromPres(PRES_MAX);
        double y2 = getYFromPres(PRES_MIN);
        double x1 = getXFromTempY(tempStep, y1);
        double x2 = getXFromTempY(tempStep, y2);
        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);
        gcSkewTPlot.setLineWidth(0.5);
        gcSkewTPlot.strokeLine(x1, y1, x2, y2);
    }

    private static void drawDryAdiabat(double tempStep) {
        ArrayList<Double> xValsList = new ArrayList<>();
        ArrayList<Double> yValsList = new ArrayList<>();
        for (int curLevel = PRES_MAX; curLevel >= PRES_MIN; curLevel -= 10) {
            double[] results = getXYFromTempPres(
                    AtmosThermoMath.calcTempFromPot(tempStep, curLevel),
                    curLevel);
            xValsList.add(results[0]);
            yValsList.add(results[1]);
        }
        double[] xVals = xValsList.stream().mapToDouble(d -> d).toArray();
        double[] yVals = yValsList.stream().mapToDouble(d -> d).toArray();

        gcSkewTPlot.setFill(Color.TAN);
        gcSkewTPlot.setStroke(Color.TAN);
        gcSkewTPlot.setLineDashes(2);
        gcSkewTPlot.setLineWidth(0.5);
        gcSkewTPlot.strokePolyline(xVals, yVals, yVals.length);
        gcSkewTPlot.setLineDashes(null);
    }

    private static void drawSatAdiabat(double osTemp) {
        ArrayList<Double> xValsList = new ArrayList<>();
        ArrayList<Double> yValsList = new ArrayList<>();
        double osaTemp = AtmosThermoMath.calcSatPotTemp(osTemp, PRES_BASE);
        for (int curLevel = PRES_MAX; curLevel >= PRES_MIN; curLevel -= 10) {
            double[] results = getXYFromTempPres(
                    AtmosThermoMath.calcTempSatAdiabat(osaTemp, curLevel),
                    curLevel);
            xValsList.add(results[0]);
            yValsList.add(results[1]);
        }
        double[] xVals = xValsList.stream().mapToDouble(d -> d).toArray();
        double[] yVals = yValsList.stream().mapToDouble(d -> d).toArray();

        gcSkewTPlot.setFill(Color.GREEN);
        gcSkewTPlot.setStroke(Color.GREEN);
        gcSkewTPlot.setLineDashes(2);
        gcSkewTPlot.setLineWidth(0.5);
        gcSkewTPlot.strokePolyline(xVals, yVals, yVals.length);
        gcSkewTPlot.setLineDashes(null);
    }

    private static void drawMixRatios() {
        // Draw mixing ratio lines at predetermined intervals
        for (double wLine : W_LINES) {
            double y1 = getYFromPres(PRES_MAX);
            double y2 = getYFromPres(PRES_MIN);
            double x1 = getXFromTempY(
                    AtmosThermoMath.calcTempAtMixingRatio(wLine, PRES_MAX), y1);
            double x2 = getXFromTempY(
                    AtmosThermoMath.calcTempAtMixingRatio(wLine, PRES_MIN), y2);
            gcSkewTPlot.setFill(Color.GREEN);
            gcSkewTPlot.setStroke(Color.GREEN);
            gcSkewTPlot.setLineDashes(5);
            gcSkewTPlot.setLineWidth(0.5);
            gcSkewTPlot.strokeLine(x1, y1, x2, y2);
            gcSkewTPlot.setLineDashes(null);
        }
    }

    private static double[] getXYFromTempPres(double temp, double pres) {
        double y = getYFromPres(pres);
        double x = getXFromTempY(temp, y);
        double[] results = {x, y};
        return results;
    }

    private static double getYFromPres(double pres) {
        double presLog = Math.log(pres);
        double presMinLog = Math.log(PRES_MIN);
        double presMaxLog = Math.log(PRES_MAX);
        double presLogRange = presMaxLog - presMinLog;
        double presLogPercent = Math.abs((presLog - presMinLog) / presLogRange);
        double y = plotYMax + (presLogPercent * plotYRange);
        return y;
    }

    private static double getXFromTempY(double temp, double y) {
        double tempRange = (TEMP_MAX - TEMP_MIN);
        double yBase = getYFromPres(PRES_BASE);
        double yRangeNew = Math.abs(yBase - plotYMax);
        double yOffset = Math.abs(plotYOffset - yBase);
        double yPercentInv = (y + yOffset - plotYOffset) / (yRangeNew);
        double tempMinNew = TEMP_MIN + (tempRange * yPercentInv);
        double tempPercent = (temp - tempMinNew) / tempRange;
        double x = (tempPercent * plotXRange) + plotXOffset;
        return x;
    }
}
