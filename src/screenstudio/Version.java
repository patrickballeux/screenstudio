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
package screenstudio;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.sources.Screen;

public class Version {
//Using this to set the main version of ScreenStudio

    public final static String MAIN = "3.0.0";

    public static boolean hasNewVersion() {
        boolean retValue = false;
        System.out.println("Checking for newest version...");
        try {
            String path = "http://screenstudio.crombz.com/archives/ubuntu/ubuntu.last.version";
            if (Screen.isOSX()) {
                path = "http://screenstudio.crombz.com/archives/osx/osx.last.version";
            }
            URL url = new URL(path);
            InputStream in = url.openStream();
            String data = "";
            byte[] buffer = new byte[50];
            int count = in.read(buffer);
            while (count > 0) {
                data += new String(buffer, 0, count);
                count = in.read(buffer);
            }
            retValue = !MAIN.equals(data.trim());
            System.out.println("Version " + data.trim() + " is available ...");
            in.close();
        } catch (MalformedURLException ex) {
            Logger.getLogger(Version.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            System.out.println("Version is the latest...");
        }
        return retValue;
    }
}
