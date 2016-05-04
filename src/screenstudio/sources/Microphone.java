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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author patrick
 */
public class Microphone {

    private String device = null;
    private String description = "None";

    @Override
    public String toString() {
        return getDescription().trim();
    }

    public static Microphone[] getSources() throws IOException, InterruptedException {
        java.util.ArrayList<Microphone> list = new java.util.ArrayList<>();
        System.out.println("Source Audio List:");
        if (Screen.isOSX()) {
            list = getOSXDevices();
        } else {
            Process p = Runtime.getRuntime().exec("pactl list sources");
            InputStream in = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            while (line != null) {
                if (line.trim().toUpperCase().matches("^.* \\#\\d{1,2}$")) {
                    reader.readLine();
                    line = reader.readLine();
                    String l = line.trim().split(":")[1];
                    //Ignoring already loaded virtual module
                    if (!l.contains("ScreenStudio")) {
                        Microphone s = new Microphone();
                        System.out.println(l);
                        s.device = l.trim();
                        line = reader.readLine();
                        l = line.trim().split(":")[1];
                        s.description = l.trim();
                        list.add(s);
                    }
                }
                line = reader.readLine();
            }
            in.close();
            isr.close();
            reader.close();
            p.destroy();
        }
        return list.toArray(new Microphone[list.size()]);
    }

    public static String getVirtualAudio(Microphone source1, Microphone source2) throws IOException, InterruptedException {
        ArrayList<String> loadedModules = new ArrayList<>();
        String device = "default";
        if (Screen.isOSX()) {
            if (source1 != null) {
                device = source1.getDevice();
            } else if (source1 != null) {
                device = source2.getDevice();
            }
        } else {
            Process p = Runtime.getRuntime().exec("pactl list modules");
            InputStream in = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader reader = new BufferedReader(isr);

            String line = reader.readLine();
            while (line != null) {
                if (line.trim().toUpperCase().matches("^.* \\#\\d{1,4}$")) {
                    String id = line.split("#")[1];
                    //Skipping module name
                    reader.readLine();
                    //Reading arguments...
                    line = reader.readLine();
                    if (line.contains("ScreenStudio")) {
                        loadedModules.add(id);
                    }
                }
                line = reader.readLine();
            }
            in.close();
            isr.close();
            reader.close();
            p.destroy();
            // Unloading previous modules...
            for (int i = loadedModules.size() - 1; i >= 0; i--) {
                execPACTL("pactl unload-module " + loadedModules.get(i));
                pause(100);
            }
            if (source1 != null && source2 != null && source1.getDevice() != null && source2.getDevice() != null) {
                execPACTL("pactl load-module module-null-sink sink_name=ScreenStudio sink_properties=device.description=\"ScreenStudio\"");
                pause(100);
                execPACTL("pactl load-module module-loopback sink=ScreenStudio source=" + source1.getDevice());
                pause(100);
                execPACTL("pactl load-module module-loopback sink=ScreenStudio source=" + source2.getDevice());
                pause(100);
                device = "ScreenStudio.monitor";
            } else if (source1 != null && source1.getDevice() != null) {
                device = source1.getDevice();
            } else if (source2 != null && source2.getDevice() != null) {
                device = source2.getDevice();
            }
        }
        return device;
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Logger.getLogger(Microphone.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String execPACTL(String command) throws IOException, InterruptedException {
        String output;
        String result = "";
        System.out.println(command);
        Process p = Runtime.getRuntime().exec(command);
        InputStream in = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(isr);
        output = reader.readLine();
        reader.close();
        isr.close();
        in.close();
        p.waitFor();
        p.destroy();
        System.out.println("Output: " + output);
        return output;
    }

    private static ArrayList<Microphone> getOSXDevices() throws IOException, InterruptedException {
        ArrayList<Microphone> list = new ArrayList<>();
        String command = "./FFMPEG/ffmpeg-osx -list_devices true -f avfoundation -i dummy";
        String line = "";
        System.out.println(command);
        Process p = Runtime.getRuntime().exec(command);
        InputStream in = p.getErrorStream();
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(isr);
        line = reader.readLine();
        while (line != null) {
            if (line.endsWith("AVFoundation audio devices:")) {
                // we have some audio sources
                line = reader.readLine();
                while (line != null && line.indexOf("input device") > 0) {
                    Microphone m = new Microphone();
                    System.out.println(line);
                    m.description = "";
                    String[] parts = line.split(" ");
                    for (int i = parts.length - 1; i >= 0; i--) {
                        if (parts[i].startsWith("[")) {
                            // reached device id
                            m.device = ":"+parts[i].substring(1, parts[i].length() - 1);
                            break;
                        } else {
                            m.description = parts[i] + " " + m.description;
                        }
                    }
                    m.description = m.description.trim();
                    list.add(m);
                    line = reader.readLine();
                }
            } else {
                line = reader.readLine();
            }
        }
        reader.close();
        isr.close();
        in.close();
        p.destroy();

        return list;
    }

    /**
     * @return the device
     */
    public String getDevice() {
        return device;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
