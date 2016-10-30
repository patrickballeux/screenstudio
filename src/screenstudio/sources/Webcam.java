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

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 *
 * @author patrick
 */
public class Webcam {

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the offset
     */
    public double getOffset() {
        return offset;
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset(double offset) {
        this.offset = offset;
    }

    private int width = 320;
    private int height = 240;
    private String device = null;
    private String description = "";
    private String id = "";
    private double offset = 0;
    private boolean mIsGreenScreen = false;
    private int mGreenSensitivity = 1;

    private Webcam(String dev, String id, String desc) {
        device = dev;
        description = desc.trim();
        this.id = id;
    }

    public int getGcreenSensitivity() {
        return mGreenSensitivity;
    }

    public void setGreenSensitivity(int s) {
        mGreenSensitivity = s;
    }

    public boolean isGreenScreen() {
        return mIsGreenScreen;
    }

    public void setGreenScreen(boolean value) {
        mIsGreenScreen = value;
    }

    public static Webcam[] getSources() throws IOException, InterruptedException {
        java.util.ArrayList<Webcam> list = new java.util.ArrayList<>();
        System.out.println("Webcam List:");
        if (Screen.isOSX()) {
            list.addAll(getOSXDevices());
        } else if (Screen.isWindows()) {
            list.addAll(getWINDevices());
        } else {
            File dev = new File("/dev");
            if (dev.isDirectory()) {
                File[] files = dev.listFiles();
                for (File f : files) {
                    if (f.getName().startsWith("video")) {
                        System.out.println(f.getName());
                        Webcam source = new Webcam(f.getAbsolutePath(), f.getName(), "");
                        list.add(source);
                    }
                }
            }

            for (Webcam s : list) {
                File desc = new File("/sys/class/video4linux", s.id + "/name");
                if (desc.exists()) {
                    InputStream in = desc.toURI().toURL().openStream();
                    byte[] buffer = new byte[in.available()];
                    in.read(buffer);
                    in.close();
                    s.description = new String(buffer).trim();
                }
            }
        }
        return list.toArray(new Webcam[list.size()]);
    }

    private static ArrayList<Webcam> getOSXDevices() throws IOException, InterruptedException {
        ArrayList<Webcam> list = new ArrayList<Webcam>();
        String command = "./FFMPEG/ffmpeg-osx -list_devices true -f avfoundation -i dummy";
        String line = "";
        System.out.println(command);
        Process p = Runtime.getRuntime().exec(command);
        InputStream in = p.getErrorStream();
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(isr);
        line = reader.readLine();
        while (line != null) {
            if (line.endsWith("AVFoundation video devices:")) {
                // we have some audio sources
                line = reader.readLine();
                while (line != null && line.indexOf("input device") > 0 && !line.contains("audio devices")) {
                    if (!line.contains("Capture screen")) {
                        Webcam w = new Webcam("", "", "");
                        w.description = "";
                        w.id = "";
                        String[] parts = line.split(" ");
                        System.out.println(line);
                        for (int i = parts.length - 1; i >= 0; i--) {
                            if (parts[i].startsWith("[")) {
                                // reached device id
                                w.device = parts[i].substring(1, parts[i].length() - 1) + ":";
                                break;
                            } else {
                                w.id = parts[i] + " " + w.id;
                            }
                        }
                        w.id = w.id.trim();
                        w.description = w.id;
                        System.out.println(w.description);
                        list.add(w);
                    }
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

    private static ArrayList<Webcam> getWINDevices() throws IOException, InterruptedException {
        ArrayList<Webcam> list = new ArrayList<Webcam>();
        String command = "./FFMPEG/ffmpeg.exe -list_devices true -f dshow -i dummy";
        String line = "";
        System.out.println(command);
        Process p = Runtime.getRuntime().exec(command);
        InputStream in = p.getErrorStream();
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(isr);
        line = reader.readLine();
        while (line != null) {
            //System.out.println(line);
            if (line.contains("DirectShow video devices")) {
                // we have some video sources
                line = reader.readLine();
                while (line != null && !line.contains("audio devices")) {
                    if (!line.contains("Alternative")) {
                        Webcam w = new Webcam("", "", "");
                        w.description = "";
                        w.id = "";
                        String[] parts = line.trim().split(" ");
                        System.out.println(line);
                        line = line.substring(line.indexOf("]") + 1).trim();
                        w.device = "video=" + line;
                        w.id = line;
                        w.description = w.id.replaceAll("\"", "");

                        w.id = w.id.trim();
                        System.out.println(w.description);
                        list.add(w);
                    }
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
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the heigh
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the heigh to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    public Rectangle getSize() {
        return new Rectangle(0, 0, this.width, this.height);
    }

    /**
     * @return the device
     */
    public String getDevice() {
        return device;
    }

    @Override
    public String toString() {
        return description.trim();
    }
}
