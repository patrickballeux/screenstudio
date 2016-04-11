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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.gui.overlays.PanelWebcam;

/**
 *
 * @author patrick
 */
public class OverlayTCPIP implements Runnable {

    private ServerSocket server = null;
    private PanelWebcam mPanel = null;
    private long mFPS = 10;
    private boolean stopMe = false;
    private boolean mIsRunning = false;

    public OverlayTCPIP(PanelWebcam panel, long fps) throws IOException, InterruptedException {
        mPanel = panel;
        mFPS = fps;
        server = new ServerSocket(8000 + new Random().nextInt(1000));
        server.setSoTimeout(2000);
        new Thread(this).start();
    }

    public int getPort() {
        if (server != null) {
            return server.getLocalPort();
        } else {
            return -1;
        }

    }

    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void run() {
        mIsRunning = true;
        stopMe = false;
        java.io.DataOutputStream out = null;
        try {
            // Pipe created. so we need to paint
            // the panel in the fifo each x ms seconds..
            // Use a BGR 24 bits images as ffmpeg will read  -pix_format BGR24
            BufferedImage img = new BufferedImage(mPanel.getWidth(), mPanel.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            //waiting for connection...
            while (!stopMe && out == null) {
                try {
                    System.out.println("Waiting for connection...");
                    Socket s = server.accept();
                    System.out.println("Got connection...");
                    s.setSendBufferSize(img.getWidth() * img.getHeight() * 3);
                    out = new DataOutputStream(s.getOutputStream());
                } catch (java.net.SocketTimeoutException ex) {
                    //do nothing...
                }
            }
            mPanel.doLayout();
            Graphics g = img.getGraphics();
            byte[] imageBytes = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            long frameTime = (1000000000 / mFPS);
            long nextPTS = System.nanoTime() + frameTime;
            while (!stopMe && out != null) {
                try {
                    if (!mPanel.IsUpdating()) {
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
            if (out != null) {
                out.close();
            }
        } catch (IOException ex) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex1) {
                    Logger.getLogger(OverlayTCPIP.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
        if (server != null) {
            try {
                server.close();
            } catch (IOException ex) {
                Logger.getLogger(OverlayTCPIP.class.getName()).log(Level.SEVERE, null, ex);
            }
            server = null;
        }
        mIsRunning = false;
    }

    public void stop() {
        stopMe = true;
    }
}
