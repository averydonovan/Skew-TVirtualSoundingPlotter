/*
 * Copyright (c) 2024, Avery Donovan
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
package com.averydonovan.skewtvsp;

import com.averydonovan.skewtvsp.controllers.STVSPController;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static javafx.application.Application.launch;
import javafx.stage.Screen;

/**
 * Main class for application. Loads main window from FXML file.
 *
 * @author Avery Donovan
 */
public class SkewTVSP extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(SkewTVSP.class.getName());

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/STVSP.fxml"));
        
        int screenHeight = (int) (Screen.getPrimary().getBounds().getHeight()*10)/11;
        int screenWidth = (screenHeight*9)/12 + 240;
    
        //Scene scene = new Scene(root, 1100, 1000);
        Scene scene = new Scene(root, screenWidth, screenHeight);

        primaryStage.titleProperty().bind(STVSPController.windowTitle);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String args[]) {
        if (System.getProperty("os.name").contains("OS X")) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                    "Skew-T Virtual Sounding Plotter");
        }

        launch(args);
    }
}
