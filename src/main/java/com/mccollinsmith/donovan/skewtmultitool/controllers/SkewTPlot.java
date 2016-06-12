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
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javafx.application.Application.Parameters;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
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
    private static int scaleLineFactor = 1;

    private static final int HPA_TO_PA = 100;
    private static final double C_TO_K = 273.15;

    private static final int PLOT_MAX_STEPS = 400;
    private static final int PRES_MIN_HPA = 100;
    private static final int PRES_BASE_HPA = 1000;
    private static final int PRES_MAX_HPA = 1050;
    private static final int TEMP_MIN_C = -50;
    private static final int TEMP_MAX_C = 50;

    private static final int PRES_MIN = PRES_MIN_HPA * HPA_TO_PA;
    private static final int PRES_BASE = PRES_BASE_HPA * HPA_TO_PA;
    private static final int PRES_MAX = PRES_MAX_HPA * HPA_TO_PA;
    private static final double TEMP_MIN = TEMP_MIN_C + C_TO_K;
    private static final double TEMP_MAX = TEMP_MAX_C + C_TO_K;

    private static final int PLOT_PRINT_WIDTH = 2700;
    private static final int PLOT_PRINT_HEIGHT = 3600;
    private static final int PLOT_VIEW_WIDTH = 900;
    private static final int PLOT_VIEW_HEIGHT = 1200;

    private static List<Integer> presLevels;
    private static List<Double> tempSteps;
    private static List<Double> wLevels;

    private static boolean skewTBackgroundDrawn = false;

    private static Image skewTBackground;

    public SkewTPlot() {
        // Do nothing
    }

    public static void initSkewT(GraphicsContext gcSkewT, boolean doClearPlot) {
        gcSkewTPlot = gcSkewT;

        presLevels = IntStream
                .rangeClosed(PRES_MIN_HPA / 100, PRES_MAX_HPA / 100)
                .map(i -> i * 100 * HPA_TO_PA)
                .boxed()
                .collect(Collectors.toList());

        tempSteps = IntStream
                .rangeClosed(TEMP_MIN_C / 10, TEMP_MAX_C / 10)
                .mapToDouble(i -> (i * 10) + C_TO_K)
                .boxed()
                .collect(Collectors.toList());

        wLevels = Stream
                .of(0.1, 0.5, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0, 10.0, 12.0,
                        15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0)
                .collect(Collectors.toList());

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

        if (doClearPlot == true) {
            gcSkewTPlot.clearRect(0, 0, canvasWidth, canvasHeight);
        }
    }

    public static void plotSkewT(GraphicsContext gcSkewT,
            ModelDataFile mdfInUse, int curX, int curY) {
        initSkewT(gcSkewT, true);

        mdfSkewTData = mdfInUse;

        coordX = curX;
        coordY = curY;

        double[] foundLonLat = mdfSkewTData.getLonLatFromXYCoords(coordX, coordY);

        drawGridLines();

        plotTemps();

        drawAxes();
        drawTicksAndLabels();
    }
    
    public static void drawBlankSkewT(GraphicsContext gcSkewT) {
        initSkewT(gcSkewT, true);

        drawGridLines();

        drawAxes();
        drawTicksAndLabels();
    }

    public static RenderedImage getHiResPlot() {
        scaleLineFactor = 3;
        
        Canvas canvasHiResPlot = new Canvas();
        GraphicsContext gcViewPlot = gcSkewTPlot;
        
        canvasHiResPlot.setHeight(PLOT_PRINT_HEIGHT);
        canvasHiResPlot.setWidth(PLOT_PRINT_WIDTH);

        WritableImage writableImage = new WritableImage((int) PLOT_PRINT_WIDTH,
                (int) PLOT_PRINT_HEIGHT);
        plotSkewT(canvasHiResPlot.getGraphicsContext2D(), mdfSkewTData,
                coordX, coordY);
        
        canvasHiResPlot.snapshot(null, writableImage);

        scaleLineFactor = 1;

        initSkewT(gcViewPlot, false);
        
        return SwingFXUtils.fromFXImage(writableImage, null);
    }

    private static void plotTemps() {
        List<Double> dataTempVals = new ArrayList<>();
        List<Double> dataDewpVals = new ArrayList<>();
        List<Double> dataPresLevels = new ArrayList<>();

        for (int coordLvl = 0; coordLvl < 50; coordLvl++) {
            Double curLevel = (double) mdfSkewTData.getLevelFromIndex(coordLvl);
            if (curLevel.intValue() >= 0) {
                dataPresLevels.add(curLevel);
                dataTempVals.add(new Double(mdfSkewTData.getTempIso(coordX, coordY, coordLvl)));
                dataDewpVals.add(new Double(mdfSkewTData.getDewpIso(coordX, coordY, coordLvl)));
            }
        }

        double presSurf = mdfSkewTData.getPresSfc(coordX, coordY);
        dataPresLevels.add(presSurf);
        Collections.sort(dataPresLevels);
        
        int presSurfIndex = dataPresLevels.indexOf(presSurf);
        
        dataTempVals.add(presSurfIndex, new Double(mdfSkewTData.getTemp2m(coordX, coordY)));
        dataDewpVals.add(presSurfIndex, new Double(mdfSkewTData.getDewp2m(coordX, coordY)));
        
        List<Double> xTempValsList = new ArrayList<>();
        List<Double> xDewpValsList = new ArrayList<>();
        List<Double> yValsList = new ArrayList<>();

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
        gcSkewTPlot.setLineWidth(scaleLineFactor * 2);
        gcSkewTPlot.strokePolyline(xTempVals, yVals, yVals.length);

        gcSkewTPlot.setFill(Color.RED);
        gcSkewTPlot.setStroke(Color.RED);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 2);
        gcSkewTPlot.strokePolyline(xDewpVals, yVals, yVals.length);
    }

    private static void drawTicksAndLabels() {
        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);

        gcSkewTPlot.setFont(Font.font("monospaced", FontWeight.NORMAL, 7 * plotAvgStep));

        gcSkewTPlot.setLineWidth(scaleLineFactor * 1);
        presLevels.stream()
                .mapToDouble(i -> getYFromPres(i))
                .forEach(d -> gcSkewTPlot.strokeLine(plotXOffset, d,
                        plotXOffset - (3 * plotAvgStep), d));
        tempSteps.stream()
                .mapToDouble(i -> getXFromTempY(i, getYFromPres(PRES_BASE)))
                .forEach(d -> gcSkewTPlot.strokeLine(d, plotYOffset,
                        d, plotYOffset + (3 * plotAvgStep)));

        gcSkewTPlot.setLineWidth(scaleLineFactor * 0);

        gcSkewTPlot.setTextAlign(TextAlignment.RIGHT);
        gcSkewTPlot.setTextBaseline(VPos.CENTER);

        presLevels.stream()
                .mapToDouble(i -> i)
                .forEach(d -> gcSkewTPlot
                        .fillText(String.format("%.0f", d / 100),
                                plotXOffset - 4 * plotAvgStep,
                                getYFromPres(d)));

        gcSkewTPlot.setTextAlign(TextAlignment.CENTER);
        gcSkewTPlot.setTextBaseline(VPos.TOP);

        tempSteps.stream()
                .mapToDouble(i -> i)
                .forEach(d -> gcSkewTPlot
                        .fillText(String.format("%.0f", d - C_TO_K),
                                getXFromTempY(d, getYFromPres(PRES_BASE)),
                                plotYOffset + 4 * plotAvgStep));

        double axisLabelSize = 10 * plotAvgStep;
        double yAxisLabelX = (canvasWidth * 0.075) + axisLabelSize;
        double yAxisLabelY = (plotYRange / 2) + plotYMax;
        double xAxisLabelX = (plotXRange / 2) + plotXOffset;
        double xAxisLabelY = (canvasHeight * 0.90) - axisLabelSize;

        gcSkewTPlot.setTextBaseline(VPos.CENTER);
        gcSkewTPlot.setFont(Font.font("monospaced", FontWeight.NORMAL, axisLabelSize));

        gcSkewTPlot.save();
        Rotate r = new Rotate(-90, yAxisLabelX, yAxisLabelY);
        gcSkewTPlot.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(), r.getTx(), r.getTy());
        gcSkewTPlot.fillText("Pressure (hPa)", yAxisLabelX, yAxisLabelY);
        gcSkewTPlot.restore();

        gcSkewTPlot.fillText("Temperature (C)", xAxisLabelX, xAxisLabelY);
    }

    private static void drawGridLines() {
        gcSkewTPlot.setFill(Color.WHITE);
        gcSkewTPlot.setStroke(Color.WHITE);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0);
        gcSkewTPlot.fillRect(0, 0, canvasWidth, canvasHeight);

        List<Double> tempsBy10 = IntStream
                .rangeClosed((TEMP_MIN_C - (TEMP_MAX_C - TEMP_MIN_C)) / 10, TEMP_MAX_C / 10)
                .mapToDouble(i -> (i * 10) + C_TO_K)
                .boxed()
                .collect(Collectors.toList());
        List<Double> tempsBy5 = IntStream
                .rangeClosed((TEMP_MIN_C - (TEMP_MAX_C - TEMP_MIN_C)) / 5, TEMP_MAX_C / 5)
                .mapToDouble(i -> (i * 5) + C_TO_K)
                .boxed()
                .collect(Collectors.toList());

        tempsBy10.forEach(d -> drawDryAdiabat(d));
        tempsBy5.forEach(d -> drawSatAdiabat(d));

        wLevels.forEach(d -> drawMixRatios(d));

        tempsBy10.forEach(d -> drawSkewTemp(d));
        presLevels.forEach(i -> drawIsobar(i));
    }

    private static void drawAxes() {
        gcSkewTPlot.setFill(Color.WHITE);
        gcSkewTPlot.setStroke(Color.WHITE);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0);
        gcSkewTPlot.fillRect(0, 0, canvasWidth, plotYMax);
        gcSkewTPlot.fillRect(0, plotYOffset, canvasWidth, plotYMax - plotYOffset);
        gcSkewTPlot.fillRect(0, plotYMax, plotXOffset, plotYOffset - plotYMax);
        gcSkewTPlot.fillRect(plotXMax, plotYMax, canvasWidth - plotXMax, canvasHeight - plotYMax);

        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 1);
        gcSkewTPlot.strokeLine(plotXOffset, plotYOffset, plotXOffset, plotYMax);
        gcSkewTPlot.strokeLine(plotXOffset, plotYOffset, plotXMax, plotYOffset);
    }

    private static void drawIsobar(int isoLevel) {
        double y = getYFromPres((double) isoLevel);
        gcSkewTPlot.setFill(Color.BLUE);
        gcSkewTPlot.setStroke(Color.BLUE);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0.75);
        gcSkewTPlot.strokeLine(plotXOffset, y, plotXMax, y);
    }

    private static void drawSkewTemp(double tempStep) {
        double y1 = getYFromPres(PRES_MAX);
        double y2 = getYFromPres(PRES_MIN);
        double x1 = getXFromTempY(tempStep, y1);
        double x2 = getXFromTempY(tempStep, y2);
        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0.75);
        gcSkewTPlot.strokeLine(x1, y1, x2, y2);
    }

    private static void drawDryAdiabat(double tempStep) {
        List<Double> xValsList = new ArrayList<>();
        List<Double> yValsList = new ArrayList<>();
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
        gcSkewTPlot.setLineWidth(scaleLineFactor * 1);
        gcSkewTPlot.strokePolyline(xVals, yVals, yVals.length);
    }

    private static void drawSatAdiabat(double osTemp) {
        ArrayList<Double> xValsList = new ArrayList<>();
        ArrayList<Double> yValsList = new ArrayList<>();
        double osaTemp = AtmosThermoMath.calcSatPotTemp(osTemp, PRES_BASE);
        for (int curLevel = PRES_MAX; curLevel >= PRES_MIN; curLevel -= 100) {
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
        gcSkewTPlot.setLineDashes(3);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0.75);
        gcSkewTPlot.strokePolyline(xVals, yVals, yVals.length);
        gcSkewTPlot.setLineDashes(null);
    }

    private static void drawMixRatios(double wLine) {
        // Draw mixing ratio lines at predetermined intervals
        double y1 = getYFromPres(PRES_MAX);
        double y2 = getYFromPres(PRES_MIN);
        double x1 = getXFromTempY(
                AtmosThermoMath.calcTempAtMixingRatio(wLine, PRES_MAX), y1);
        double x2 = getXFromTempY(
                AtmosThermoMath.calcTempAtMixingRatio(wLine, PRES_MIN), y2);
        gcSkewTPlot.setFill(Color.TEAL);
        gcSkewTPlot.setStroke(Color.TEAL);
        gcSkewTPlot.setLineDashes(6);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0.5);
        gcSkewTPlot.strokeLine(x1, y1, x2, y2);
        gcSkewTPlot.setLineDashes(null);
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
