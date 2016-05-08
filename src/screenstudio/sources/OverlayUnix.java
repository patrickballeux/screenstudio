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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.encoder.FFMpeg;
import screenstudio.gui.overlays.Renderer;

/**
 *
 * @author patrick
 */
public class OverlayUnix implements Runnable {

    private Renderer mPanel = null;
    private long mFPS = 10;
    private boolean stopMe = false;
    private boolean mIsRunning = false;
    private final File mOutput;
    private boolean mIsPrivateMode = false;
    private Image privacyImage = null;

    public OverlayUnix(Renderer panel, long fps) throws IOException, InterruptedException {
        mPanel = panel;
        mFPS = fps;
        mOutput = File.createTempFile("screenstudio", ".raw");
        mOutput.deleteOnExit();
        //Make sure it does not exists
        mOutput.delete();
        Runtime.getRuntime().exec("mkfifo " + mOutput.getAbsolutePath());
    }

    public void setPrivateMode(boolean value) {
        mIsPrivateMode = value;
    }

    public void start() {
        new Thread(this).start();
    }

    public File getOutput() {
        return mOutput;
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void run() {
        mIsRunning = true;
        stopMe = false;
        java.io.FileOutputStream out = null;
        File privacyTemplate = new File(new FFMpeg().getHome() + "/Overlays", "privacy.png");
        if (privacyTemplate.exists()) {
            System.out.println("Found privacy template... Loading...");
            try {
                privacyImage = javax.imageio.ImageIO.read(privacyTemplate).getScaledInstance(mPanel.getWidth(), mPanel.getHeight(), BufferedImage.SCALE_SMOOTH);
            } catch (IOException ex) {
                Logger.getLogger(OverlayUnix.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            privacyImage = null;
        }
        try {
            // Pipe created. so we need to paint
            // the panel in the fifo each x ms seconds..
            // Use a BGR 24 bits images as ffmpeg will read  -pix_format BGR24
            BufferedImage img = new BufferedImage(mPanel.getWidth(), mPanel.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics g = img.getGraphics();
            out = new FileOutputStream(mOutput);
            byte[] imageBytes = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            long frameTime = (1000000000 / mFPS);
            long nextPTS = System.nanoTime() + frameTime;
            while (!stopMe) {
                try {
                    if (mIsPrivateMode) {
                        if (privacyImage == null) {
                            g.setColor(Color.BLACK);
                            g.fillRect(0, 0, img.getWidth(), img.getHeight());
                        } else {
                            g.drawImage(privacyImage, 0, 0, null);
                        }
                    } else if (!mPanel.IsUpdating()) {
                        mPanel.paint(g);
                    }
                } catch (Exception e) {
                    //Do nothing if painting failed...
                    System.err.println("Error painting overlay..." + e.getMessage());
                }
                out.write(imageBytes);
                while (nextPTS - System.nanoTime() > 0) {
                    long wait = nextPTS - System.nanoTime();
                    if (wait > 0) {
                        try {
                            Thread.sleep(wait / 1000000, (int) (wait % 1000000));
                        } catch (Exception ex) {
                            System.err.println("Error: Thread.sleep(" + (wait / 1000000) + "," + ((int) (wait % 1000000)) + ")");
                        }
                    }
                }
                nextPTS += frameTime;
            }
            g.dispose();
            out.close();
        } catch (IOException ex) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex1) {
                    Logger.getLogger(OverlayUnix.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
        mOutput.delete();
        mIsRunning = false;
    }

    public void stop() {
        stopMe = true;
    }
}
