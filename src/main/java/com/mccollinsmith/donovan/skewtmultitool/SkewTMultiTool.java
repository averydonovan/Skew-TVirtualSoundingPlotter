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

import com.mccollinsmith.donovan.skewtmultitool.ui.StatusConsole;
import java.io.IOException;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import static javafx.application.Application.launch;
import javafx.stage.WindowEvent;

/**
 *
 * @author Donovan Smith <donovan@mccollinsmith.com>
 */
public class SkewTMultiTool extends Application {

    public static StatusConsole statusConsole = null;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setOnShown((WindowEvent event) -> {
                    execOnShown();
                });

        Button btnLoadFile = new Button();
        btnLoadFile.setText("Load GRIB");
        btnLoadFile.setOnAction((ActionEvent event) -> {
            statusConsole.println("Loading GRIB file...");
            doBtnLoadFile();
        });
        
        statusConsole = new StatusConsole();
        statusConsole.setPrefRowCount(8);
        statusConsole.setPrefColumnCount(80);
        statusConsole.setStyle("-fx-font-family: monospace;");

        HBox hbMainToolbar = new HBox();
        hbMainToolbar.setPadding(new Insets(5, 5, 5, 5));
        hbMainToolbar.setSpacing(5);
        hbMainToolbar.getChildren().addAll(btnLoadFile);

        HBox hbStatus = new HBox();
        hbStatus.setPadding(new Insets(5, 5, 5, 5));
        hbStatus.setSpacing(5);
        hbStatus.getChildren().addAll(statusConsole);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(hbMainToolbar);
        borderPane.setBottom(hbStatus);

        Scene scene = new Scene(borderPane, 800, 600);

        primaryStage.setTitle("Skew-T MultiTool");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void execOnShown() {
        statusConsole.println("Skew-T MultiTool");
        statusConsole.println("Copyright 2016 Donovan Smith");
        statusConsole.println();
        statusConsole.println("This software is licensed under the GNU General Public License version 3.");
        statusConsole.println("This software comes with ABSOLUTELY NO WARRANTY!");
        statusConsole.println();

        //ModelDataDb db = new ModelDataDb();
        //if (db.isConnected() == true) {
        //    System.out.println("Connected to database.");
        //}
        //db.close()
    }
    
    private void doBtnLoadFile() {
        String gribFileName = "rap_252_20160524_0000_000.grb2";

        ModelDataFile gribFile = null;
        try {
            gribFile = new ModelDataFile(gribFileName);
        } catch (IOException ex) {
            statusConsole.println("Unable to open GRIB file.");
        }

        if (gribFile != null) {
            int coordX = 150;
            int coordY = 113;
            statusConsole.println(gribFile.getLon(coordX, coordY));
            statusConsole.println(gribFile.getLat(coordX, coordY));
            statusConsole.println(gribFile.getLevel(36));
            statusConsole.println(gribFile.getTempIso(36, coordX, coordY));
            statusConsole.println(gribFile.getTemp2m(coordX, coordY));
            statusConsole.println(gribFile.getDewpIso(36, coordX, coordY));
            statusConsole.println(gribFile.getDewp2m(coordX, coordY));
            statusConsole.println((int) gribFile.getLCLPres(coordX, coordY));
            statusConsole.println(gribFile.getLCLTemp(coordX, coordY));
            statusConsole.println();

            try {
                gribFile.close();
            } catch (IOException ex) {
                statusConsole.println("Unable to close GRIB file.");
            }
        }
    }

    public static void main(String args[]) {
        launch(args);
    }
}
