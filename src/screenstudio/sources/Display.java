/*
 * Copyright (C) 2016 patrick
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.encoder.FFMpeg;

/**
 * This class is a test to capture, mix multiple sources and output the result
 * to FFMPEG
 *
 * @author patrick
 */
public class Display implements Runnable {

    private final int mWidth;
    private final int mHeight;
    private final BufferedImage buffer;
    private final int mFPS;
    private boolean stopMe = false;
    private final String mDevice;

    public Display(int fps, int w, int h, String device) {
        mFPS = fps;
        mWidth = w;
        mHeight = h;
        mDevice = device;
        buffer = new BufferedImage(mWidth, mHeight, BufferedImage.TYPE_3BYTE_BGR);
    }

    public void stop() {
        stopMe = true;
    }

    public Image getImage() {
        return buffer;
    }

    @Override
    public void run() {
        String format = "x11grab";
        final String bin;
        File folder = new File("FFMPEG");
        if (folder.exists()) {
            File file;
            if (Screen.isOSX()) {
                file = new File(folder, "osx.properties");
            } else {
                file = new File(folder, "default.properties");
            }

            if (file.exists()) {
                try {
                    Properties p = new Properties();
                    try (InputStream in = file.toURI().toURL().openStream()) {
                        p.load(in);
                    }
                    format = p.getProperty("DESKTOPFORMAT", format);
                    bin = p.getProperty("BIN", "ffmpeg");
                } catch (MalformedURLException ex) {
                    Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else {
                bin = "ffmpeg";
            }
        } else {
            bin = "ffmpeg";
        }
        try {
            stopMe = false;
            Process p = Runtime.getRuntime().exec("ffmpeg -nostats -loglevel 0 -f " + format + " -video_size " + mWidth + "x" + mHeight + " -i " + mDevice + " -r " + mFPS + " -f rawvideo -pix_fmt bgr24 -");
            java.io.DataInputStream in = new java.io.DataInputStream(p.getInputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Process pout = Runtime.getRuntime().exec("ffmpeg -nostats -loglevel 0 -framerate " + mFPS + " -video_size " + mWidth + "x" + mHeight + " -f rawvideo -pix_fmt bgr24 -i -  -f pulse -i default -vb 4500k -f mpegts -y test.ts");
                        java.io.OutputStream out = pout.getOutputStream();
                        int count = 0;
                        BufferedImage mixer = new BufferedImage(mWidth, mHeight, BufferedImage.TYPE_3BYTE_BGR);
                        Graphics2D g = mixer.createGraphics();
                        byte[] data = ((DataBufferByte) mixer.getRaster().getDataBuffer()).getData();
                        g.setColor(Color.red);
                        long nextPTS = System.currentTimeMillis() + (1000 / mFPS);
                        while (!stopMe) {
                            g.drawImage(buffer, 0, 0, null);
                            g.drawImage(buffer, 0, 0, null);
                            g.drawString(new Date().toString(), 100, 100);
                            out.write(data);
                            count++;
                            if (System.currentTimeMillis() < nextPTS) {
                                Thread.sleep(nextPTS - System.currentTimeMillis());
                            }
                            nextPTS += (1000 / mFPS);
                        }
                        g.dispose();
                        System.out.println("Frames captured: " + count);
                        out.close();
                        pout.destroy();
                    } catch (Exception ex) {
                    }

                }

            }).start();

            while (!stopMe) {
                in.readFully(((DataBufferByte) buffer.getRaster().getDataBuffer()).getData());
            }
            in.close();
            p.destroy();
        } catch (IOException ex) {
            Logger.getLogger(WebcamViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        Display d = new Display(20, 1340, 768, ":0.0");

        new Thread(() -> {
            d.run();
        }).start();

        try {
            Thread.sleep(60000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Display.class.getName()).log(Level.SEVERE, null, ex);
        }
        d.stop();
    }
}
