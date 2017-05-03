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
package com.mccollinsmith.donovan.skewtvsp.controllers;

import com.mccollinsmith.donovan.skewtvsp.utils.AtmosThermoMath;
import com.mccollinsmith.donovan.skewtvsp.utils.ModelDataFile;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Donovan Smith
 */
public class SkewTPlot {

    private static final Logger LOG
            = LoggerFactory.getLogger(ModelDataFile.class.getName());

    /*
     * Various useful constants.
     */
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

    private static final int PLOT_VIEW_WIDTH = 1800;
    private static final int PLOT_VIEW_HEIGHT = 2400;
    private static final int PLOT_VIEW_SCALE = 2;
    private static final int PLOT_PRINT_SCALE = 3;
    private static final int PLOT_PRINT_WIDTH = 2400;
    private static final int PLOT_PRINT_HEIGHT = 3600;

    /**
     * ModelDataFile currently in use.
     */
    private static ModelDataFile mdfSkewTData = null;
    /**
     * GraphicsContext currently in use.
     */
    private static GraphicsContext gcSkewTPlot = null;

    /*
     * XY-coordinates in data grid to use.
     */
    private static int coordX = 0;
    private static int coordY = 0;

    /*
     * Plotting area setup variables.
     */
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

    /**
     * Factor to scale plotted elements by so that they have the same relative
     * size at higher resolutions.
     */
    private static int scaleLineFactor = 1;

    /**
     * Pressure levels to plot ticks and labels for.
     */
    private static List<Integer> presLevels;
    /**
     * Temperature steps to plot ticks and labels for.
     */
    private static List<Double> tempSteps;
    /**
     * Mixing ratio lines to plot.
     */
    private static List<Double> wLevels;

    /**
     * Sets up class fields for plot so that drawing and plotting methods will
     * render at the proper size to the correct GraphicsContext. Must be called
     * before calling any drawing or plotting method to avoid unpredictable
     * behavior.
     *
     * @param gcSkewT     GraphicsContext to use for plotting
     * @param doClearPlot true if plotting area should be cleared, false if not
     */
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

    /**
     * Plot a Skew-T diagram at given XY-coordinates.
     *
     * @param gcSkewT  GraphicsContext to use for plotting
     * @param mdfInUse ModelDataFile to obtain data from
     * @param curX     X-coordinate in data grid
     * @param curY     Y-coordinate in data grid
     */
    public static void plotSkewT(GraphicsContext gcSkewT,
            ModelDataFile mdfInUse, int curX, int curY) {
        initSkewT(gcSkewT, true);

        mdfSkewTData = mdfInUse;

        coordX = curX;
        coordY = curY;

        drawGridLines();

        plotTemps();

        drawAxes();
        drawTicksAndLabels();
        
        drawLocationAndTime();
        drawWeatherIndices();
    }

    /**
     * Draw a blank Skew-T diagram.
     *
     * @param gcSkewT GraphicsContext to use for plotting
     */
    public static void drawBlankSkewT(GraphicsContext gcSkewT) {
        initSkewT(gcSkewT, true);

        drawGridLines();

        drawAxes();
        drawTicksAndLabels();
    }

    /**
     * Render currently drawn Skew-T plot in high-resolution. Useful for saving
     * plot to a file or for printing.
     *
     * @return high-resolution plot
     */
    public static RenderedImage getHiResPlot() {
        // Save viewed GraphicsContext so it can be restored later
        GraphicsContext gcViewPlot = gcSkewTPlot;

        /*
         * Create a new Canvas at high-resolution to render plot to and then
         * plot to it using the same ModelDataFile and XY-coordinates as the
         * on-screen plot.
         */
        Canvas canvasHiResPlot = new Canvas();

        scaleLineFactor = PLOT_PRINT_SCALE;
        canvasHiResPlot.setHeight(PLOT_PRINT_HEIGHT);
        canvasHiResPlot.setWidth(PLOT_PRINT_WIDTH);

        plotSkewT(canvasHiResPlot.getGraphicsContext2D(), mdfSkewTData,
                coordX, coordY);

        //Create raster image to hold a snapshot of the Canvas.
        WritableImage writableImage = new WritableImage((int) PLOT_PRINT_WIDTH,
                (int) PLOT_PRINT_HEIGHT);

        // Take snapshot of plot and save to writableImage
        canvasHiResPlot.snapshot(null, writableImage);

        // Restore original scale factor and GraphicsContext.
        scaleLineFactor = PLOT_VIEW_SCALE;
        initSkewT(gcViewPlot, false);

        /*
         * Convert WriteableImage to RenderedImage and return it.
         */
        return SwingFXUtils.fromFXImage(writableImage, null);
    }

    /**
     * Plot temperatures and dew points at various isobaric levels.
     */
    private static void plotTemps() {
        List<Double> dataTempVals = new ArrayList<>();
        List<Double> dataDewpVals = new ArrayList<>();
        List<Double> dataPresLevels = new ArrayList<>();

        /*
         * Get pressure, temperature, and dew point for each available isobaric
         * level.
         */
        for (int coordLvl = 0; coordLvl < 50; coordLvl++) {
            Double curLevel = mdfSkewTData.getLevelFromIndex(coordLvl);
            if (curLevel.intValue() >= 0) {
                dataPresLevels.add(curLevel);
                dataTempVals.add(mdfSkewTData
                        .getTempIso(coordX, coordY, coordLvl));
                dataDewpVals.add(mdfSkewTData
                        .getDewpIso(coordX, coordY, coordLvl));
            }
        }

        /*
         * Get surface pressure, add it to the list of isobaric levels, sort
         * that list, and then get the index of the surface pressure from that
         * list.
         */
        double presSurf = mdfSkewTData.getPresSfc(coordX, coordY);
        dataPresLevels.add(presSurf);
        Collections.sort(dataPresLevels);
        int presSurfIndex = dataPresLevels.indexOf(presSurf);

        /*
         * Add surface (really, 2m) temperature and dew point to temperature and
         * dew point lists at the appropriate place so that lists are ordered
         * from lowest to highest isobaric level.
         */
        dataTempVals.add(presSurfIndex, mdfSkewTData.getTemp2m(coordX, coordY));
        dataDewpVals.add(presSurfIndex, mdfSkewTData.getDewp2m(coordX, coordY));

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

        // Convert lists to arrays for use with JavaFX drawing methods.
        double[] xTempVals = xTempValsList
                .stream()
                .parallel()
                .mapToDouble(d -> d)
                .toArray();
        double[] xDewpVals = xDewpValsList
                .stream()
                .parallel()
                .mapToDouble(d -> d)
                .toArray();
        double[] yVals = yValsList
                .stream()
                .parallel()
                .mapToDouble(d -> d)
                .toArray();

        /*
         * Temperatures plotted as thick black line.
         */
        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 2);
        gcSkewTPlot.strokePolyline(xTempVals, yVals, yVals.length);

        /*
         * Dew points plotted as thick red line.
         */
        gcSkewTPlot.setFill(Color.RED);
        gcSkewTPlot.setStroke(Color.RED);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 2);
        gcSkewTPlot.strokePolyline(xDewpVals, yVals, yVals.length);
    }
    
    /**
     * Draws labels for location, analysis time, and valid time.
     */
    private static void drawLocationAndTime() {
        // All labels drawn in black
        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);

        /*
         * Draw location and time labels.
         */
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0);

        gcSkewTPlot.setFont(
                Font.font("sans-serif", FontWeight.NORMAL, 12 * plotAvgStep));

        gcSkewTPlot.setTextAlign(TextAlignment.CENTER);
        gcSkewTPlot.setTextBaseline(VPos.CENTER);
        
        double yAxisLocation = plotYMax/10 * 4.5;
        double yAxisTime = plotYMax/10 * 7;
        double yAxisModelName = plotYMax/10 * 9;
        double xAxisLocation = canvasWidth/2;
        double xAxisTime = canvasWidth/2;
        double xAxisModelName = canvasWidth/2;
        
        double[] plotLonLat = 
                mdfSkewTData.getLonLatFromXYCoords(coordX, coordY);
        String plotLocation = 
                String.format("Longitude, Latitude: %.6f, %.6f",
                        plotLonLat[0], plotLonLat[1]);

        gcSkewTPlot.fillText(plotLocation, xAxisLocation, yAxisLocation);
        
        gcSkewTPlot.setFont(
                Font.font("sans-serif", FontWeight.NORMAL, 9 * plotAvgStep));

        String plotTime = "Analysis: "
                + mdfSkewTData.getAnalysisTime().toString()
                + "   "
                + "Valid: "
                + mdfSkewTData.getValidTime().toString();
 
        gcSkewTPlot.fillText(plotTime, xAxisTime, yAxisTime);
        
        gcSkewTPlot.setFont(
                Font.font("sans-serif", FontWeight.NORMAL, FontPosture.ITALIC,
                        7 * plotAvgStep));

        String plotModelName = "Source: " + mdfSkewTData.getModelName();

        gcSkewTPlot.fillText(plotModelName, xAxisModelName, yAxisModelName);
    }

    /**
     * Draws labels for location, analysis time, and valid time.
     */
    private static void drawWeatherIndices() {
        // All labels drawn in black
        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);

        /*
         * Draw weather indices labels.
         */
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0);

        gcSkewTPlot.setTextAlign(TextAlignment.CENTER);
        gcSkewTPlot.setTextBaseline(VPos.CENTER);
        
        double yAxisIndices1 = (canvasHeight - plotYOffset)/20 * 9 + plotYOffset;
        double yAxisIndices2 = (canvasHeight - plotYOffset)/20 * 12 + plotYOffset;
        double yAxisIndices3 = (canvasHeight - plotYOffset)/20 * 14 + plotYOffset;
        double xAxisIndices = canvasWidth/2;
        
        String plotIndices1 =
                String.format("Temperature 2m: %.1f C", 
                        mdfSkewTData.getTemp2m(coordX, coordY) - C_TO_K)
                + "     "
                + String.format("Dew Point 2m: %.1f C", 
                        mdfSkewTData.getDewp2m(coordX, coordY) - C_TO_K)
                + "     "
                + String.format("Pressure Sfc: %.0f hPa", 
                        mdfSkewTData.getPresSfc(coordX, coordY)/HPA_TO_PA);
        String plotIndices2 =
                String.format("LCL: %.0f hPa", 
                        mdfSkewTData.getLCL(coordX, coordY)[0]/HPA_TO_PA)
                + "     "
                + String.format("MSL: %.0f hPa", 
                        mdfSkewTData.getMSL(coordX, coordY)/HPA_TO_PA)
                + "     "
                + String.format("CAPE: %.0f J/kg", 
                        mdfSkewTData.getCAPE(coordX, coordY))
                + "     "
                + String.format("CIN: %.0f J/kg", 
                        mdfSkewTData.getCIN(coordX, coordY));
        String plotIndices3 =
                String.format("Lifted Index: %.1f", 
                        mdfSkewTData.getLFTX(coordX, coordY))
                + "     "
                + String.format("K-Index: %.0f", 
                        mdfSkewTData.getKIndex(coordX, coordY))
                + "     "
                + String.format("Total Totals: %.0f", 
                        mdfSkewTData.getTotalTotals(coordX, coordY))
                + "     "
                + String.format("SWEAT: %.0f", 
                        mdfSkewTData.getSWEAT(coordX, coordY));
 
        gcSkewTPlot.setFont(
                Font.font("sans-serif", FontWeight.NORMAL, 8 * plotAvgStep));

        gcSkewTPlot.fillText(plotIndices1, xAxisIndices, yAxisIndices1);

        gcSkewTPlot.setFont(
                Font.font("sans-serif", FontWeight.NORMAL, 7 * plotAvgStep));

        gcSkewTPlot.fillText(plotIndices2, xAxisIndices, yAxisIndices2);
        gcSkewTPlot.fillText(plotIndices3, xAxisIndices, yAxisIndices3);
    }

    /**
     * Draws ticks and labels on plot axes.
     */
    private static void drawTicksAndLabels() {
        // All ticks and labels drawn in black
        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);

        /*
         * Draw isobaric level and temperature tick marks and labels.
         */
        gcSkewTPlot.setLineWidth(scaleLineFactor * 1.5);

        // Draw isobaric level ticks
        presLevels.stream()
                .mapToDouble(i -> getYFromPres(i))
                .forEach(d -> gcSkewTPlot.strokeLine(plotXOffset, d,
                        plotXOffset - (3 * plotAvgStep), d));

        // Draw temperature ticks
        tempSteps.stream()
                .mapToDouble(i -> getXFromTempY(i, getYFromPres(PRES_BASE)))
                .forEach(d -> gcSkewTPlot.strokeLine(d, plotYOffset,
                        d, plotYOffset + (3 * plotAvgStep)));

        /*
         * Draw isobaric level and temperature labels.
         */
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0);

        gcSkewTPlot.setFont(
                Font.font("sans-serif", FontWeight.NORMAL, 7 * plotAvgStep));

        gcSkewTPlot.setTextAlign(TextAlignment.RIGHT);
        gcSkewTPlot.setTextBaseline(VPos.CENTER);

        // Draw isobaric level labels
        presLevels.stream()
                .mapToDouble(i -> i)
                .forEach(d -> gcSkewTPlot
                        .fillText(String.format("%.0f", d / HPA_TO_PA),
                                plotXOffset - 4 * plotAvgStep,
                                getYFromPres(d)));

        gcSkewTPlot.setTextAlign(TextAlignment.CENTER);
        gcSkewTPlot.setTextBaseline(VPos.TOP);

        // Draw temperature labels
        tempSteps.stream()
                .mapToDouble(i -> i)
                .forEach(d -> gcSkewTPlot
                        .fillText(String.format("%.0f", d - C_TO_K),
                                getXFromTempY(d, getYFromPres(PRES_BASE)),
                                plotYOffset + 4 * plotAvgStep));

        /*
         * Draw axes labels.
         */
        double axisLabelSize = 10 * plotAvgStep;
        double yAxisLabelX = (canvasWidth * 0.075) + axisLabelSize;
        double yAxisLabelY = (plotYRange / 2) + plotYMax;
        double xAxisLabelX = (plotXRange / 2) + plotXOffset;
        double xAxisLabelY = (canvasHeight * 0.90) - axisLabelSize;

        gcSkewTPlot.setTextBaseline(VPos.CENTER);
        gcSkewTPlot.setFont(
                Font.font("sans-serif", FontWeight.NORMAL, axisLabelSize));

        // Draw Y-axis label
        gcSkewTPlot.save();
        Rotate r = new Rotate(-90, yAxisLabelX, yAxisLabelY);
        gcSkewTPlot.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(),
                r.getTx(), r.getTy());
        gcSkewTPlot.fillText("Pressure (hPa)", yAxisLabelX, yAxisLabelY);
        gcSkewTPlot.restore();

        // Draw X-axis label
        gcSkewTPlot.fillText("Temperature (C)", xAxisLabelX, xAxisLabelY);
    }

    /**
     * Draws the various grid lines on the plot, including isobaric levels,
     * temperatures, dry adiabats, saturated adiabats, and mixing ratio lines.
     */
    private static void drawGridLines() {
        /*
         * Erase canvas before drawing.
         */
        gcSkewTPlot.setFill(Color.WHITE);
        gcSkewTPlot.setStroke(Color.WHITE);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0);
        gcSkewTPlot.fillRect(0, 0, canvasWidth, canvasHeight);

        /*
         * Create lists of temperatures at a set interval for drawing grid
         * lines.
         */
        // 10 K between steps, for skewed temperatures and dry adiabats
        List<Double> tempsBy10 = IntStream
                .rangeClosed((TEMP_MIN_C - (TEMP_MAX_C - TEMP_MIN_C)) / 10,
                        TEMP_MAX_C / 10)
                .parallel()
                .mapToDouble(i -> (i * 10) + C_TO_K)
                .boxed()
                .collect(Collectors.toList());
        // 5 K between steps, for saturated adiabats
        List<Double> tempsBy5 = IntStream
                .rangeClosed((TEMP_MIN_C - (TEMP_MAX_C - TEMP_MIN_C)) / 5,
                        TEMP_MAX_C / 5)
                .parallel()
                .mapToDouble(i -> (i * 5) + C_TO_K)
                .boxed()
                .collect(Collectors.toList());

        /*
         * Draw grid lines.
         */
        // Draw dry and saturated adiabats
        tempsBy10.forEach(d -> drawDryAdiabat(d));
        tempsBy5.forEach(d -> drawSatAdiabat(d));

        // Draw mixing ratio lines
        wLevels.forEach(d -> drawMixRatios(d));

        // Draw skewed temperature and isobaric level lines
        tempsBy10.forEach(d -> drawSkewTemp(d));
        presLevels.forEach(i -> drawIsobar(i));
    }

    /**
     * Draws axes for the plot and erases any lines drawn outside of the plot
     * area.
     */
    private static void drawAxes() {
        /*
         * Clear areas outside of plot area to neaten up plot.
         */
        gcSkewTPlot.setFill(Color.WHITE);
        gcSkewTPlot.setStroke(Color.WHITE);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0);
        // Upper
        gcSkewTPlot.fillRect(0, 0,
                canvasWidth, plotYMax);
        // Lower
        gcSkewTPlot.fillRect(0, plotYOffset,
                canvasWidth, plotYMax - plotYOffset);
        // Left
        gcSkewTPlot.fillRect(0, plotYMax,
                plotXOffset, canvasHeight - plotYMax);
        // Lower
        gcSkewTPlot.fillRect(plotXMax, plotYMax,
                canvasWidth - plotXMax, canvasHeight - plotYMax);

        /*
         * Draw axes lines.
         */
        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 1.5);
        gcSkewTPlot.strokeLine(plotXOffset, plotYOffset, plotXOffset, plotYMax);
        gcSkewTPlot.strokeLine(plotXOffset, plotYOffset, plotXMax, plotYOffset);
    }

    /**
     * Draws a single isobaric line (Y-axis).
     *
     * @param isoLevel pressure in Pa
     */
    private static void drawIsobar(int isoLevel) {
        double y = getYFromPres(isoLevel);
        gcSkewTPlot.setFill(Color.BLUE);
        gcSkewTPlot.setStroke(Color.BLUE);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0.75);
        gcSkewTPlot.strokeLine(plotXOffset, y, plotXMax, y);
    }

    /**
     * Draws a single skewed temperature line (X-axis).
     *
     * @param tempStep temperature in K
     */
    private static void drawSkewTemp(double tempStep) {
        double y1 = getYFromPres(PRES_MAX);
        double y2 = getYFromPres(PRES_MIN);
        double x1 = getXFromTempY(tempStep, y1);
        double x2 = getXFromTempY(tempStep, y2);
        gcSkewTPlot.setFill(Color.BLACK);
        gcSkewTPlot.setStroke(Color.BLACK);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 1.25);
        gcSkewTPlot.strokeLine(x1, y1, x2, y2);
    }

    /**
     * Draws and labels a single dry adiabat.
     *
     * @param tempStep potential temperature in K
     */
    private static void drawDryAdiabat(double tempStep) {
        double y1 = getYFromPres(22000);
        double y2 = getYFromPres(20000);
        double x1 = getXFromTempY(
                AtmosThermoMath.calcTempFromPot(tempStep, 22000), y1);
        double x2 = getXFromTempY(
                AtmosThermoMath.calcTempFromPot(tempStep, 20000), y2);

        double labelY = getYFromPres(21000);
        double labelX = getXFromTempY(
                AtmosThermoMath.calcTempFromPot(tempStep, 21000), labelY)
                + (1.5 * plotAvgStep);

        /*
         * Compute XY-coordinates for segments of dry adiabat line.
         */
        List<Double> xValsList = new ArrayList<>();
        List<Double> yValsList = new ArrayList<>();
        for (int curLevel = PRES_MAX; curLevel >= PRES_MIN; curLevel -= 10) {
            double[] results = getXYFromTempPres(
                    AtmosThermoMath.calcTempFromPot(tempStep, curLevel),
                    curLevel);
            xValsList.add(results[0]);
            yValsList.add(results[1]);
        }

        // Convert lists to arrays for use with JavaFX drawing methods.
        double[] xVals = xValsList
                .stream()
                .parallel()
                .mapToDouble(d -> d)
                .toArray();
        double[] yVals = yValsList
                .stream()
                .parallel()
                .mapToDouble(d -> d)
                .toArray();

        /*
         * Draw dry adiabat line.
         */
        gcSkewTPlot.setFill(Color.rgb(127, 95, 63));
        gcSkewTPlot.setStroke(Color.rgb(127, 95, 63));
        gcSkewTPlot.setLineWidth(scaleLineFactor * 1.0);
        gcSkewTPlot.strokePolyline(xVals, yVals, yVals.length);

        /*
         * Draw label parallel to line.
         */
        gcSkewTPlot.setTextAlign(TextAlignment.CENTER);
        gcSkewTPlot.setTextBaseline(VPos.BASELINE);
        gcSkewTPlot.setFont(
                Font.font("sans-serif", FontWeight.BOLD, 5.0 * plotAvgStep));

        gcSkewTPlot.save();
        Rotate r = new Rotate(-Math.toDegrees(Math.atan((y1 - y2) / (x2 - x1))),
                labelX, labelY);
        gcSkewTPlot.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(),
                r.getTx(), r.getTy());
        gcSkewTPlot.fillText(
                String.format("%.0f C", tempStep - C_TO_K), labelX, labelY);
        gcSkewTPlot.restore();
    }

    /**
     * Draws and labels a single saturated adiabat.
     *
     * @param osTemp saturated potential temperature in K
     */
    private static void drawSatAdiabat(double osTemp) {
        double osaTemp = AtmosThermoMath.calcSatPotTemp(osTemp, PRES_BASE);

        double y1 = getYFromPres(28000);
        double y2 = getYFromPres(26000);
        double x1 = getXFromTempY(
                AtmosThermoMath.calcTempSatAdiabat(osaTemp, 28000), y1);
        double x2 = getXFromTempY(
                AtmosThermoMath.calcTempSatAdiabat(osaTemp, 26000), y2);

        double labelY = getYFromPres(27000);
        double labelX = getXFromTempY(
                AtmosThermoMath.calcTempSatAdiabat(osaTemp, 27000), labelY)
                + (1.5 * plotAvgStep);

        /*
         * Compute XY-coordinates for segments of saturated adiabat line.
         */
        ArrayList<Double> xValsList = new ArrayList<>();
        ArrayList<Double> yValsList = new ArrayList<>();
        for (int curLevel = PRES_MAX; curLevel >= PRES_MIN; curLevel -= 100) {
            double[] results = getXYFromTempPres(
                    AtmosThermoMath.calcTempSatAdiabat(osaTemp, curLevel),
                    curLevel);
            xValsList.add(results[0]);
            yValsList.add(results[1]);
        }

        // Convert lists to arrays for use with JavaFX drawing methods.
        double[] xVals = xValsList
                .stream()
                .parallel()
                .mapToDouble(d -> d)
                .toArray();
        double[] yVals = yValsList
                .stream()
                .parallel()
                .mapToDouble(d -> d)
                .toArray();

        /*
         * Draw saturated adiabat line.
         */
        gcSkewTPlot.setFill(Color.GREEN);
        gcSkewTPlot.setStroke(Color.GREEN);
        gcSkewTPlot.setLineDashes(scaleLineFactor * 3.0);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0.75);
        gcSkewTPlot.strokePolyline(xVals, yVals, yVals.length);
        gcSkewTPlot.setLineDashes(null);

        /*
         * Draw label parallel to line.
         */
        gcSkewTPlot.setTextAlign(TextAlignment.CENTER);
        gcSkewTPlot.setTextBaseline(VPos.BASELINE);
        gcSkewTPlot.setFont(
                Font.font("sans-serif", FontWeight.BOLD, 5.0 * plotAvgStep));

        gcSkewTPlot.save();
        Rotate r = new Rotate(-Math.toDegrees(Math.atan((y1 - y2) / (x2 - x1))),
                labelX, labelY);
        gcSkewTPlot.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(),
                r.getTx(), r.getTy());
        gcSkewTPlot.fillText(String.format("%.0f C", osTemp - C_TO_K),
                labelX, labelY);
        gcSkewTPlot.restore();
    }

    /**
     * Draws and labels a single mixing ratio line.
     *
     * @param wLine mixing ratio in g/kg
     */
    private static void drawMixRatios(double wLine) {
        // Draw mixing ratio lines at predetermined intervals
        double y1 = getYFromPres(75000);
        double y2 = getYFromPres(73000);
        double x1 = getXFromTempY(
                AtmosThermoMath.calcTempAtMixingRatio(wLine, 75000), y1);
        double x2 = getXFromTempY(
                AtmosThermoMath.calcTempAtMixingRatio(wLine, 73000), y2);

        double labelY = getYFromPres(74000);
        double labelX = getXFromTempY(
                AtmosThermoMath.calcTempAtMixingRatio(wLine, 74000), labelY)
                - (1.5 * plotAvgStep);

        /*
         * Compute XY-coordinates for segments of mixing ratio line.
         */
        ArrayList<Double> xValsList = new ArrayList<>();
        ArrayList<Double> yValsList = new ArrayList<>();
        for (int curLevel = PRES_MAX; curLevel >= PRES_MIN; curLevel -= 100) {
            double[] results = getXYFromTempPres(
                    AtmosThermoMath.calcTempAtMixingRatio(wLine, curLevel),
                    curLevel);
            xValsList.add(results[0]);
            yValsList.add(results[1]);
        }

        // Convert lists to arrays for use with JavaFX drawing methods.
        double[] xVals = xValsList
                .stream()
                .parallel()
                .mapToDouble(d -> d)
                .toArray();
        double[] yVals = yValsList
                .stream()
                .parallel()
                .mapToDouble(d -> d)
                .toArray();

        /*
         * Draw mixing ratio line.
         */
        gcSkewTPlot.setFill(Color.TEAL);
        gcSkewTPlot.setStroke(Color.TEAL);
        gcSkewTPlot.setLineDashes(scaleLineFactor * 6.0);
        gcSkewTPlot.setLineWidth(scaleLineFactor * 0.75);
        gcSkewTPlot.strokePolyline(xVals, yVals, yVals.length);
        gcSkewTPlot.setLineDashes(null);

        /*
         * Draw label parallel to line.
         */
        gcSkewTPlot.setTextAlign(TextAlignment.CENTER);
        gcSkewTPlot.setTextBaseline(VPos.BASELINE);
        gcSkewTPlot.setFont(
                Font.font("sans-serif", FontWeight.BOLD, 4.0 * plotAvgStep));

        gcSkewTPlot.save();
        Rotate r = new Rotate(-Math.toDegrees(Math.atan((y1 - y2) / (x2 - x1))),
                labelX, labelY);
        gcSkewTPlot.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(),
                r.getTx(), r.getTy());
        gcSkewTPlot.fillText(String.format("%.1f g/kg", wLine), labelX, labelY);
        gcSkewTPlot.restore();
    }

    /**
     * Get XY-coordinate on plot for a given temperature and isobaric level.
     *
     * @param temp temperature in K
     * @param pres pressure in Pa
     *
     * @return XY-coordinate as double[2]; [0] = X, [1] = Y
     */
    private static double[] getXYFromTempPres(double temp, double pres) {
        double y = getYFromPres(pres);
        double x = getXFromTempY(temp, y);
        double[] results = {x, y};
        return results;
    }

    /**
     * Get Y-coordinate on plot for a given isobaric level
     *
     * @param pres pressure in Pa
     *
     * @return Y-coordinate
     */
    private static double getYFromPres(double pres) {
        double presLog = Math.log(pres);
        double presMinLog = Math.log(PRES_MIN);
        double presMaxLog = Math.log(PRES_MAX);
        double presLogRange = presMaxLog - presMinLog;
        double presLogPercent = Math.abs((presLog - presMinLog) / presLogRange);
        double y = plotYMax + (presLogPercent * plotYRange);
        return y;
    }

    /**
     * Get X-coordinate for a given temperature and already-computed
     * Y-coordinate.
     *
     * @param temp temperature in K
     * @param y    Y-coordinate
     *
     * @return X-coordinate
     */
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
