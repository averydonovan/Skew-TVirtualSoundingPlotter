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
package com.mccollinsmith.donovan.skewtmultitool.ui;

import javafx.scene.control.TextArea;

/**
 *
 * @author Donovan Smith <donovan@mccollinsmith.com>
 */
public class StatusConsole extends TextArea {
    
    public void println() {
        println("");
    }
    
    public void println(Object text) {
        appendText(text.toString() + "\n");
        pruneText();
    }
    
    private void pruneText() {
        if (getLength() > 5000) {
            do {
                int curPos = 0;
                int curLength = getLength();
                do {
                    curPos++;
                } while (!getText(curPos, 1).equals("\n") && curPos < curLength);
                deleteText(0, curPos);
            } while (getLength() > 5000);
        }
    }
}
