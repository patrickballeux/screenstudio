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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import screenstudio.gui.LabelText;
import screenstudio.targets.Layout;
import screenstudio.targets.Layout.SourceType;

/**
 *
 * @author patrick
 */
public class Compositor implements Runnable {

    private final java.util.List<Source> mSources;
    private boolean mStopMe = false;
    private final int mFPS;
    private final Rectangle mOutputSize;
    private byte[] mData;
    private boolean mIsReady = false;

    public Compositor(java.util.List<Source> sources, Rectangle outputSize, int fps) {
        sources.sort((a, b) -> Integer.compare(b.getZOrder(), a.getZOrder()));
        mSources = sources;
        mOutputSize = outputSize;
        mFPS = fps;
        mData = new byte[mOutputSize.width * mOutputSize.height * 3];
    }

    public boolean isReady(){
        return mIsReady;
    }
    public byte[] getData() {
        return mData;
    }

    @Override
    public void run() {
        mStopMe = false;
        for (Source s : mSources) {
            new Thread(s).start();
            while (s.getImage() == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Compositor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        long frameTime = (1000000000 / mFPS);
        long nextPTS = System.nanoTime() + frameTime;
        BufferedImage img = new BufferedImage(mOutputSize.width, mOutputSize.height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = img.createGraphics();
        byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        mData = new byte[data.length];
        while (!mStopMe) {
            java.util.Arrays.fill(data, (byte) 0);
            for (Source s : mSources) {
                g.setComposite(s.getAlpha());
                g.drawImage(s.getImage(), s.getBounds().x, s.getBounds().y, null);
            }
            byte[] mBuffer = new byte[data.length];
            System.arraycopy(data, 0, mBuffer, 0, mBuffer.length);
            mData = mBuffer;
            mIsReady=true;
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
        for (Source s : mSources) {
            s.stop();
        }
    }

    public int getFPS() {
        return mFPS;
    }

    public int getWidth() {
        return mOutputSize.width;
    }

    public int getHeight() {
        return mOutputSize.height;
    }

    public void stop() {
        mStopMe = true;
    }

    public static List<Source> getSources(JTable sources, int fps) {
        java.util.ArrayList<Source> list = new java.util.ArrayList();
        for (int i = sources.getRowCount() - 1; i >= 0; i--) {
            if ((Boolean) sources.getValueAt(i, 0)) {
                int sx = (int) sources.getValueAt(i, 3);
                int sy = (int) sources.getValueAt(i, 4);
                int sw = (int) sources.getValueAt(i, 5);
                int sh = (int) sources.getValueAt(i, 6);
                float alpha = new Float(sources.getValueAt(i, 7).toString());
                Object source = sources.getValueAt(i, 2);
                // Detect type of source...
                if (source instanceof Screen) {
                    Screen screen = (Screen) source;
                    SourceFFMpeg s = SourceFFMpeg.getDesktopInstance(screen, fps);
                    s.getBounds().setBounds(new Rectangle(sx, sy, sw, sh));
                    s.setAlpha(alpha);
                    s.setZOrder(i);
                    s.setFPS(fps);
                    list.add(s);
                } else if (source instanceof Webcam) {
                    Webcam webcam = (Webcam) source;
                    SourceFFMpeg s = SourceFFMpeg.getWebcamInstance(webcam, fps);
                    s.getBounds().setBounds(new Rectangle(sx, sy, sw, sh));
                    s.setAlpha(alpha);
                    s.setZOrder(i);
                    s.setFPS(fps);
                    list.add(s);
                } else if (source instanceof File) {
                    switch ((Layout.SourceType) sources.getValueAt(i, 1)) {
                        case Image:
                            list.add(new SourceImage(new Rectangle(sx, sy, sw, sh), i, alpha, (File) source));
                            break;
                        case Video:
                            SourceFFMpeg s = SourceFFMpeg.getFileInstance(new Rectangle(sx, sy, sw, sh), ((File) source), fps);
                            s.setAlpha(alpha);
                            s.setZOrder(i);
                            list.add(s);
                            break;
                        case LabelFile:
                            break;
                    }
                } else if (source instanceof LabelText) {
                    if (sources.getValueAt(i, 1) == SourceType.LabelText) {
                        list.add(new SourceLabel(new Rectangle(sx, sy, sw, sh), i, alpha, ((LabelText) source)));
                    } else if (sources.getValueAt(i, 1) == SourceType.LabelFile) {
                        list.add(new SourceFileLabel(new Rectangle(sx, sy, sw, sh), i, alpha, 1000, (LabelText) source));
                    }
                }
            }
        }
        return list;
    }

}
