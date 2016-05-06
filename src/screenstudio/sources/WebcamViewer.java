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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.encoder.FFMpeg;

/**
 *
 * @author patrick
 */
public class WebcamViewer implements Runnable {

    private final File mDevice;
    private final Screen mScreen;
    private final int mWidth;
    private final int mHeight;
    private BufferedImage buffer;
    private final int mFPS;
    private boolean stopMe = false;

    /**
     * Creates new form WebcamViewer
     *
     * @param screen
     * @param device
     * @param width
     * @param height
     * @param fps
     */
    public WebcamViewer(Screen screen, File device, int width, int height, int fps) {
        mDevice = device;
        mScreen = screen;
        mWidth = width;
        mHeight = height;
        mFPS = fps;
        buffer = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }

    public BufferedImage getImage() {
        return buffer;
    }

    public void stop() {
        stopMe = true;
    }

    @Override
    public void run() {

        String webcamFormat = "video4linux2";
        String displayFormat = "x11grab";
        String bin = "ffmpeg";
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
                    webcamFormat = p.getProperty("WEBCAMFORMAT", webcamFormat);
                    displayFormat = p.getProperty("DESKTOPFORMAT", displayFormat);
                    bin = p.getProperty("BIN", bin);
                } catch (MalformedURLException ex) {
                    Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

        try {
            stopMe = false;
            String command;

            if (mDevice.getName().equals("MOUSE")) {
                if (Screen.isOSX()) {
                    command = bin + " -nostats -loglevel 0 -f " + displayFormat + " -follow_mouse centered -video_size " + mWidth / 2 + "x" + mHeight / 2 + " -i " + mScreen.getId() + ": -s " + mWidth + "x" + mHeight + " -r " + mFPS + "  -f rawvideo -pix_fmt bgr24 -";
                } else {
                    command = bin + " -nostats -loglevel 0 -f " + displayFormat + " -follow_mouse centered -video_size " + mWidth / 2 + "x" + mHeight / 2 + " -i " + ":0.0 -s " + mWidth + "x" + mHeight + " -r " + mFPS + " -f rawvideo -pix_fmt bgr24 -";
                }
            } else {
                command = bin + " -nostats -loglevel 0 -f " + webcamFormat + " -i " + mDevice.toString() + " -s " + mWidth + "x" + mHeight + " -r " + mFPS + " -f rawvideo -pix_fmt bgr24 -";
            }
            Process p = Runtime.getRuntime().exec(command);
            java.io.DataInputStream in = new java.io.DataInputStream(p.getInputStream());
            BufferedImage b1 = new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            BufferedImage b2 = new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            boolean flip = false;
            while (!stopMe) {
                if (flip) {
                    in.readFully(((DataBufferByte) b1.getRaster().getDataBuffer()).getData());
                    buffer = b1;
                } else {
                    in.readFully(((DataBufferByte) b2.getRaster().getDataBuffer()).getData());
                    buffer = b2;
                }
                flip = !flip;
            }
            in.close();
            p.destroy();
        } catch (IOException ex) {
            Logger.getLogger(WebcamViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
