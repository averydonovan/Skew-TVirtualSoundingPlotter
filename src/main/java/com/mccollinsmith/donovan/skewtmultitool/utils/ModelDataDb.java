/*
 * Copyright (C) 2016 Donovan Smith <donovan@mcollinsmith.com>
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author Donovan Smith <donovan@mcollinsmith.com>
 */
public class ModelDataDb {
    private static Connection dataDb = null;
    
    public ModelDataDb() {
        connect();
    }
    
    @Override
    public String toString() {
        if (isConnected() == true) {
            return "Connected to database containing data model";
        } else {
            return "Not connected to database containing data model";
        }
    }
    
    public static boolean connect() {
        // Check if already connected
        if (dataDb != null) return true;
        
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            System.out.println("Database driver error, cannot continue.");
            return false;
        }

        try {
            dataDb = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/model_data?"
                    + "user=model_data&password=noaa&useSSL=false");
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return false;
        }

        return true;
    }
    
    public static boolean close() {
        try {
            dataDb.close();
            return true;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return false;
        }
    }
    
    public boolean isConnected() {
        if (dataDb == null) {
            return false;
        } else {
            return true;
        }
    }
}
