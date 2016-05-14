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

import java.awt.Image;
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
public class DesktopViewer implements Runnable {

    private final Screen mDevice;
    private BufferedImage buffer;
    private boolean stopMe = false;

    /**
     * Creates new form WebcamViewer
     *
     * @param device
     */
    public DesktopViewer(Screen device) {
        mDevice = device;
        int w = (int) mDevice.getSize().getWidth();
        int h = (int) mDevice.getSize().getHeight();
        buffer = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
    }

    public void stop() {
        stopMe = true;
    }

    public Image getImage() {
        return buffer;
    }

    @Override
    public void run() {
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
            if (Screen.isOSX()) {
                command = bin + " -nostats -loglevel 0 -f " + displayFormat + " -video_size " + buffer.getWidth() + "x" + buffer.getHeight() + " -i " + mDevice.getId() + ": -r " + mDevice.getFps() + "  -f rawvideo -pix_fmt bgr24 -";
            } else {
                command = bin + " -nostats -loglevel 0 -f " + displayFormat + " -video_size " + buffer.getWidth() + "x" + buffer.getHeight() + " -i " + ":0.0  -r " + mDevice.getFps() + "  -f rawvideo -pix_fmt bgr24 -";
            }
            System.out.println(command);
            Process p = Runtime.getRuntime().exec(command);
            java.io.DataInputStream in = new java.io.DataInputStream(p.getInputStream());

            BufferedImage b1 = new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            BufferedImage b2 = new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            byte[] byteBuffer1 = ((DataBufferByte) b1.getRaster().getDataBuffer()).getData();
            byte[] byteBuffer2 = ((DataBufferByte) b2.getRaster().getDataBuffer()).getData();
            boolean flip = false;
            while (!stopMe) {
                if (flip) {
                    in.readFully(byteBuffer1);
                    buffer = b1;
                } else {
                    in.readFully(byteBuffer2);
                    buffer = b2;
                }
                flip = !flip;
            }
            in.close();
            p.destroy();
        } catch (IOException ex) {
            Logger.getLogger(DesktopViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
