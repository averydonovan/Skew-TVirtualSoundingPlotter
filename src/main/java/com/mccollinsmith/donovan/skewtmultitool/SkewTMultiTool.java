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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.NCdumpW;
import ucar.nc2.dt.*;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.ma2.*;

/**
 *
 * @author Donovan Smith <donovan@mccollinsmith.com>
 */
public class SkewTMultiTool {

    public static void main(String args[]) {
        String gribFileName = "rap_252_20160524_0000_000.grb2";
        String varName = "Temperature_isobaric";
        //int coordX = 0;
        //int coordY = 0;

        System.out.println("Skew-T MultiTool");
        System.out.println("Copyright 2016 Donovan Smith");
        System.out.println();
        System.out.println("This software is licensed under the GNU General Public License version 3.");
        System.out.println("This software comes with ABSOLUTELY NO WARRANTY!");
        System.out.println();
        
        ModelDataDb db = new ModelDataDb();

        if (db.isConnected() == true) {
            System.out.println("Connected to database.");
        }
        
        ModelDataFile gribFile = new ModelDataFile(gribFileName);
        
        int coordX = 150;
        int coordY = 113;
        System.out.println(gribFile.getLon(coordX, coordY));
        System.out.println(gribFile.getLat(coordX, coordY));
        System.out.println(gribFile.getLevel(36));
        System.out.println(gribFile.getTempIso(36, coordX, coordY));
        System.out.println(gribFile.getTemp2m(coordX, coordY));
        System.out.println(gribFile.getDewpIso(36, coordX, coordY));
        System.out.println(gribFile.getDewp2m(coordX, coordY));
        System.out.println((int)gribFile.getLCLPres(coordX, coordY));
        System.out.println(gribFile.getLCLTemp(coordX, coordY));

        db.close();
    }
}
