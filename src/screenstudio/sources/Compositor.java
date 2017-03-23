/*
 * Copyright (C) 2016 Patrick Balleux (Twitter: @patrickballeux)
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
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import screenstudio.gui.LabelText;
import screenstudio.gui.images.frames.Frames;
import screenstudio.sources.effects.Effect;
import screenstudio.sources.transitions.Transition;
import screenstudio.targets.Layout;

/**
 *
 * @author patrick
 */
public class Compositor {

    private final java.util.List<Source> mSources;
    private final int mFPS;
    private final Rectangle mOutputSize;
    private final byte[] mData;
    private boolean mIsReady = false;
    private final Graphics2D g;
    private final long mStartTime;
    private boolean mRequestStop = false;
    private final BufferedImage mImage;
    private byte[] mPreviewBuffer;
    private long mTimeDelta = 0;
    private Effect mEffects = new Effect();

    public Compositor(java.util.List<Source> sources, Rectangle outputSize, int fps) {
        sources.sort((a, b) -> Integer.compare(b.getZOrder(), a.getZOrder()));
        mSources = sources;
        mOutputSize = outputSize;
        mFPS = fps;
        for (Source s : mSources) {
            s.start();
            while (s.getImage() == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Compositor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        // Apply transitions...
        for (Source s : mSources) {
            if (!s.isRemoteDisplay()) {
                s.setDisplayTime(-1, -1);
            }
            if (s.getTransitionStart() != Transition.NAMES.None && s.getStartDisplayTime() == 0) {
                Transition t = Transition.getInstance(s.getTransitionStart(), s, fps, this.mOutputSize);
                new Thread(t).start();
            }
        }
        mImage = new BufferedImage(mOutputSize.width, mOutputSize.height, BufferedImage.TYPE_3BYTE_BGR);
        g = mImage.createGraphics();
        mData = ((DataBufferByte) mImage.getRaster().getDataBuffer()).getData();
        mStartTime = System.currentTimeMillis();
        mIsReady = true;
    }

    public long getTimeDelta() {
        return mTimeDelta;
    }

    public List<Source> getSources() {
        return mSources;
    }

    public byte[] getImage() {
        return mPreviewBuffer;
    }

    public boolean isReady() {
        return mIsReady;
    }

    public void RequestStop() {
        mRequestStop = true;
    }

    public byte[] getData() {
        java.util.Arrays.fill(mData, (byte) 0);
        mTimeDelta = (System.currentTimeMillis() - mStartTime) / 1000;
        for (Source s : mSources) {
            BufferedImage img = mEffects.apply(s.getEffect(), s.getImage());
            if ((s.getEndDisplayTime() == 0 || s.getEndDisplayTime() >= mTimeDelta)
                    && (s.getStartDisplayTime() <= mTimeDelta)) {
                //Showing for the first time???
                if (s.getTransitionStart() != Transition.NAMES.None && s.getStartDisplayTime() != 0) {
                    //Then we can trigger the start event...
                    Transition t = Transition.getInstance(s.getTransitionStart(), s, mFPS, mOutputSize);
                    new Thread(t).start();
                    s.setTransitionStart(Transition.NAMES.None);
                } else {
                    if (s.getTransitionStop() != Transition.NAMES.None && (mRequestStop || (s.getEndDisplayTime() - 1 == mTimeDelta))) {
                        Transition t = Transition.getInstance(s.getTransitionStop(), s, mFPS, mOutputSize);
                        new Thread(t).start();
                        s.setTransitionStop(Transition.NAMES.None);
                    }
                    g.setComposite(s.getAlpha());
                    g.drawImage(img, s.getBounds().x, s.getBounds().y, null);
                }
            }
        }
        mRequestStop = false;
        mPreviewBuffer = new byte[mData.length];
        System.arraycopy(mData, 0, mPreviewBuffer, 0, mPreviewBuffer.length);
        return mData;
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
        System.out.println("Compositor is stopping");
        for (Source s : mSources) {
            s.stop();
        }
    }

    public static List<Source> getSources(JTable sources, int fps) {
        java.util.ArrayList<Source> list = new java.util.ArrayList();
        for (int i = sources.getRowCount() - 1; i >= 0; i--) {
            int sx = (int) sources.getValueAt(i, 3);
            int sy = (int) sources.getValueAt(i, 4);
            int sw = (int) sources.getValueAt(i, 5);
            int sh = (int) sources.getValueAt(i, 6);
            float alpha = new Float(sources.getValueAt(i, 7).toString());
            long timestart = (Long) sources.getValueAt(i, 8);
            long timeend = (Long) sources.getValueAt(i, 9);
            String transIn = sources.getValueAt(i, 10).toString();
            String transOut = sources.getValueAt(i, 11).toString();
            String effect = sources.getValueAt(i, 12).toString();
            Object source = sources.getValueAt(i, 2);
            // Detect type of source...
            if (source instanceof Screen) {
                Screen screen = (Screen) source;
                SourceFFMpeg s = SourceFFMpeg.getDesktopInstance(screen, new Rectangle(sx, sy, sw, sh), fps);
                s.setAlpha(alpha);
                s.setZOrder(i);
                s.setFPS(fps);
                s.setDisplayTime(timestart, timeend);
                s.setTransitionStart(Transition.NAMES.valueOf(transIn));
                s.setTransitionStop(Transition.NAMES.valueOf(transOut));
                s.setRemoteDisplay((Boolean) sources.getValueAt(i, 0));
                s.setEffect(Effect.eEffects.valueOf(effect));
                list.add(s);
            } else if (source instanceof Webcam) {
                Webcam webcam = (Webcam) source;
                webcam.setWidth(sw);
                webcam.setHeight(sh);
                SourceFFMpeg s = SourceFFMpeg.getWebcamInstance(webcam, fps);
                s.getBounds().setBounds(new Rectangle(sx, sy, sw, sh));
                s.setAlpha(alpha);
                s.setZOrder(i);
                s.setFPS(fps);
                s.setDisplayTime(timestart, timeend);
                s.setTransitionStart(Transition.NAMES.valueOf(transIn));
                s.setTransitionStop(Transition.NAMES.valueOf(transOut));
                s.setRemoteDisplay((Boolean) sources.getValueAt(i, 0));
                s.setEffect(Effect.eEffects.valueOf(effect));
                list.add(s);
            } else if (source instanceof File) {
                switch ((Layout.SourceType) sources.getValueAt(i, 1)) {
                    case Image:
                        SourceImage s = new SourceImage(new Rectangle(sx, sy, sw, sh), i, alpha, (File) source);
                        s.setDisplayTime(timestart, timeend);
                        s.setTransitionStart(Transition.NAMES.valueOf(transIn));
                        s.setTransitionStop(Transition.NAMES.valueOf(transOut));
                        s.setRemoteDisplay((Boolean) sources.getValueAt(i, 0));
                        s.setEffect(Effect.eEffects.valueOf(effect));
                        list.add(s);
                        break;
                    case Video:
                        SourceFFMpeg sf = SourceFFMpeg.getFileInstance(new Rectangle(sx, sy, sw, sh), ((File) source), fps);
                        sf.setAlpha(alpha);
                        sf.setZOrder(i);
                        sf.setDisplayTime(timestart, timeend);
                        sf.setTransitionStart(Transition.NAMES.valueOf(transIn));
                        sf.setTransitionStop(Transition.NAMES.valueOf(transOut));
                        sf.setRemoteDisplay((Boolean) sources.getValueAt(i, 0));
                        sf.setEffect(Effect.eEffects.valueOf(effect));
                        list.add(sf);
                        break;
                }
            } else if (source instanceof LabelText) {
                SourceLabel s = new SourceLabel(new Rectangle(sx, sy, sw, sh), i, alpha, ((LabelText) source));
                s.setDisplayTime(timestart, timeend);
                s.setTransitionStart(Transition.NAMES.valueOf(transIn));
                s.setTransitionStop(Transition.NAMES.valueOf(transOut));
                s.setRemoteDisplay((Boolean) sources.getValueAt(i, 0));
                s.setEffect(Effect.eEffects.valueOf(effect));
                list.add(s);
            } else if (source instanceof Frames.eList) {
                try {
                    SourceImage s = new SourceImage(new Rectangle(sx, sy, sw, sh), i, alpha, (BufferedImage) Frames.getImage((Frames.eList) source), source.toString());
                    s.setDisplayTime(timestart, timeend);
                    s.setTransitionStart(Transition.NAMES.valueOf(transIn));
                    s.setTransitionStop(Transition.NAMES.valueOf(transOut));
                    s.setRemoteDisplay((Boolean) sources.getValueAt(i, 0));
                    s.setEffect(Effect.eEffects.valueOf(effect));
                    list.add(s);
                } catch (IOException ex) {
                }
            }
        }
        return list;
    }

}
