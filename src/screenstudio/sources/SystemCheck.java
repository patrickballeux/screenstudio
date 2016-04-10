/*
 * Copyright (C) 2014 Patrick Balleux
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
package screenstudio.sources;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author patrick
 */
public class SystemCheck {

    public static boolean isSystemReady(boolean interactive) {
        ArrayList<String> msgs = new ArrayList<>();
        if (Screen.isOSX()) {
// do nothing...
        } else {
            //Looks for avconv or ffmpeg
            if (!new File("/usr/bin/avconv").exists() && !new File("/usr/bin/ffmpeg").exists()) {
                msgs.add("----");
                msgs.add("WARNING!");
                msgs.add("libav-tools not detected...");
                msgs.add("You won't be able to record or stream using AVCONV...");
                msgs.add("Note: Install libav-tools (sudo apt-get install libav-tools)");
                msgs.add("----");

            } else if (!new File("/usr/bin/avconv").exists() && new File("/usr/bin/ffmpeg").exists()) {
                msgs.add("----");
                msgs.add("INFO!");
                msgs.add("libav-tools not detected but ffmpeg was...");
                msgs.add("You'll have to use a FFMPEG based confoguration XML to record or stream");
                msgs.add("Note: Install libav-tools (sudo apt-get install libav-tools)");
                msgs.add("----");
            }

            //Looks for libjna-tools
            if (new File("/usr/share/doc/libjna-java").exists()) {
                msgs.add("----");
                msgs.add("WARNING!");
                msgs.add("libjna-java was detected...");
                msgs.add("You may not be able use the global shortcuts...");
                msgs.add("ScreenStudio does provide its own library and may conflict with libjna-java.");
                msgs.add("Note: Uninstall libjna-java (sudo apt-get remove libjna-java)");
                msgs.add("Note: if shortcut keys do not work.");
                msgs.add("Note: You can also replace jna.jar by your own by creating a symlink.");
                msgs.add("----");
            }
            //Looks for xwininfo
            if (!new File("/usr/bin/xwininfo").exists()) {
                msgs.add("----");
                msgs.add("WARNING!");
                msgs.add("/usr/bin/xwininfo was not detected...");
                msgs.add("You won't be able to capture a window area...");
                msgs.add("----");
            }
            if (interactive) {
                if (msgs.size() > 0) {
                } else {
                    msgs.add("Everyting looks good!");
                    msgs.add("Have fun!");
                    msgs.clear();
                }
            }
        }
        return msgs.isEmpty();
    }

    public static void main(String[] args) {
        isSystemReady(true);
    }
}
