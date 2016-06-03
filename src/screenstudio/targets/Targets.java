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
package screenstudio.targets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.encoder.FFMpeg;

/**
 *
 * @author patrick
 */
public class Targets {
// <editor-fold defaultstate="collapsed" desc="Contants">

    public enum FORMATS {
        TS,
        FLV,
        MOV,
        MP4,
        HITBOX,
        TWITCH,
        USTREAM,
        VAUGHNLIVE,
        YOUTUBE,
        RTMP,
        BROADCAST,
    }

    public static boolean isRTMP(FORMATS f) {
        switch (f) {
            case HITBOX:
            case RTMP:
            case TWITCH:
            case USTREAM:
            case VAUGHNLIVE:
            case YOUTUBE:
                return true;
            default:
                return false;
        }
    }

    public static String[] getServerList(FORMATS format) {
        String[] list = new String[0];
        File folder = new File("RTMP");
        if (folder.exists()) {
            File file = new File(folder, format.name() + ".properties");
            if (file.exists()) {
                Properties p = new Properties();
                InputStream in;
                try {
                    java.util.ArrayList<String> l = new java.util.ArrayList<>();
                    in = file.toURI().toURL().openStream();
                    p.load(in);
                    in.close();
                    p.values().stream().forEach((server) -> {
                        l.add(server.toString());
                    });
                    list = l.toArray(list);
                    java.util.Arrays.sort(list);
                } catch (MalformedURLException ex) {
                    Logger.getLogger(Targets.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(Targets.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

        }
        return list;
    }
    // </editor-fold >
// <editor-fold defaultstate="collapsed" desc="Members">
    public String format = "";
    public String size = "";
    public String server = "";
    public String mainSource = "";
    public String mainAudio = "";
    public String secondAudio = "";
    public String mainOverlay = "";
    public String mainOverlayLocation = "Right";
    public String mainOverlaySize = "320";
    public String framerate = "";
    public String captureX = "";
    public String captureY = "";
    public String captureWidth = "";
    public String captureHeight = "";
    public String webcamDevice = "";
    public String webcamWidth = "320";
    public String webcamHeight = "240";
    public String webcamOffset = "0.0";
    public String outputPreset = "ultrafast";
    public String outputVideoBitrate = "9000";
    public String outputAudioRate = "Audio22K";
    public String showDuration = "60";
    public String panelTextContent = "";
    public String shortcutCaptureKey = "control shift R";
    public String shortcutPrivacyKey = "control shift P";
    public String doNotHide = "false";
    public String webcamLocation = "Top";
    public String webcamGreenScreenMode = "false";
    public String command = "";
    public String waterMarkFile = "";
    public String twitchalertsfolder = "/home/user/twitchalerts";
    
    private final TreeMap<String, String> keys = new TreeMap();
// </editor-fold>

    public void updateKey(Targets.FORMATS format, String key) {
        keys.put(format.name(), key);
    }

    public String getKey(Targets.FORMATS format) {
        if (keys.containsKey(format.name())) {
            return keys.get(format.name());
        } else {
            return "";
        }
    }

    public void saveDefault(File config) throws IOException {
        if (config == null) {
            config = new File(new FFMpeg().getHome(), "screenstudio.properties");
        }
        java.util.Properties props = new java.util.Properties();
        FileWriter out = new FileWriter(config);
        for (Field f : this.getClass().getFields()) {
            try {
                if (f.get(this) != null) {
                    props.setProperty(f.getName(), f.get(this).toString());
                } else {
                    props.setProperty(f.getName(), "");
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(Targets.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        props.store(out, "ScreenStudio default settings");
        out.close();
        // save keys...
        for (String key : keys.keySet()) {
            FileWriter fout = new FileWriter(new File(config.getParentFile(), key + ".key"));
            fout.write(keys.get(key));
            fout.close();
        }
    }

    public void loadDefault(File config) throws FileNotFoundException, IOException {
        if (config == null) {
            config = new File(new FFMpeg().getHome(), "screenstudio.properties");
        }
        if (config.exists()) {
            FileReader in = new FileReader(config);
            java.util.Properties props = new java.util.Properties();
            props.load(in);
            for (Field f : this.getClass().getFields()) {
                try {
                    if (props.getProperty(f.getName()) != null) {
                        f.set(this, props.getProperty(f.getName()));
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(Targets.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            in.close();
            keys.clear();
            for (File key : config.getParentFile().listFiles()) {
                byte[] data = new byte[1024];
                int count;
                if (key.getName().endsWith(".key")) {
                    InputStream fin = key.toURI().toURL().openStream();
                    count = fin.read(data);
                    if (count > 1) {
                        keys.put(key.getName().replaceAll(".key", ""), new String(data, 0, count));
                    }
                    in.close();
                }
            }
        }
    }
}
