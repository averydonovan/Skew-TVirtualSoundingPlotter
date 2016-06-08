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

import com.mccollinsmith.donovan.skewtmultitool.controllers.STMTController;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static javafx.application.Application.launch;

/**
 * Main class for application. Loads main window from FXML file.
 *
 * @author Donovan Smith <donovan@mccollinsmith.com>
 */
public class SkewTMultiTool extends Application {

    private static final Logger LOG
            = LoggerFactory.getLogger(SkewTMultiTool.class.getName());

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/STMT.fxml"));

        Scene scene = new Scene(root, 800, 600);

        primaryStage.titleProperty().bind(STMTController.windowTitle);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String args[]) {
        if (System.getProperty("os.name").contains("OS X")) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Skew-T MultiTool");
        }
        
        launch(args);
    }
}
