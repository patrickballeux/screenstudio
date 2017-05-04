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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.gui.LabelText;
import screenstudio.gui.images.frames.Frames;
import screenstudio.sources.effects.Effect;
import screenstudio.sources.transitions.Transition;

/**
 *
 * @author patrick
 */
public class Compositor implements Runnable {

    private java.util.List<Source> mSources;
    private final int mFPS;
    private final Rectangle mOutputSize;
    private byte[] mData;
    private boolean mIsReady = false;
    private final long mStartTime;
    private boolean mRequestStop = false;
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
            if (s.getTransitionStart() != Transition.NAMES.None && s.getStartDisplayTime() == 0) {
                Transition t = Transition.getInstance(s.getTransitionStart(), s, fps, this.mOutputSize);
                s.setTransitionStart(Transition.NAMES.None);
                new Thread(t).start();
            }
        }
        mData = new byte[0];
        mStartTime = System.currentTimeMillis();
        new Thread(this).start();
        mIsReady = true;
    }

    public void setCurrentView(int index) {
        ArrayList<Source> newList = new ArrayList<>();
        for (Source s : mSources) {
            s.setViewIndex(index);
        }
        newList.addAll(mSources);
        newList.sort((a, b) -> Integer.compare(b.getZOrder(), a.getZOrder()));
        if (mTimeDelta > 0) {
            //Skipping the background image...
            for (int i = 0; i < mSources.size(); i++) {
                Source s = mSources.get(index);
                if (s.isRemoteDisplay() && s.getTransitionStart() == Transition.NAMES.None) {
                    s.setTransitionStart(Transition.NAMES.FadeIn);
                }
            }
        }
        mSources = newList;
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

    public static List<Source> getSources(ArrayList<screenstudio.targets.Source> sources, int fps) {
        java.util.ArrayList<screenstudio.sources.Source> list = new java.util.ArrayList();
        for (int i = sources.size() - 1; i >= 0; i--) {
            long timestart = sources.get(i).getStartTime();
            long timeend = sources.get(i).getEndTime();
            Transition.NAMES transIn = sources.get(i).getTransitionStart();
            Transition.NAMES transOut = sources.get(i).getTransitionStop();
            Effect.eEffects effect = sources.get(i).getEffect();
            Object source = sources.get(i).getSourceObject();
            // Detect type of source...
            if (source instanceof Screen) {
                Screen screen = (Screen) source;
                SourceFFMpeg s = SourceFFMpeg.getDesktopInstance(screen, sources.get(i).getViews(), fps);
                s.setFPS(fps);
                s.setDisplayTime(timestart, timeend);
                s.setTransitionStart(transIn);
                s.setTransitionStop(transOut);
                s.setEffect(effect);
                s.setViewIndex(sources.get(i).getCurrentViewIndex());
                list.add(s);
            } else if (source instanceof Webcam) {
                Webcam webcam = (Webcam) source;
                webcam.setWidth(sources.get(i).getViews().get(0).Width);
                webcam.setHeight(sources.get(i).getViews().get(0).Height);
                SourceFFMpeg s = SourceFFMpeg.getWebcamInstance(webcam, sources.get(i).getViews(), fps);
                s.setFPS(fps);
                s.setDisplayTime(timestart, timeend);
                s.setTransitionStart(transIn);
                s.setTransitionStop(transOut);
                s.setEffect(effect);
                s.setViewIndex(sources.get(i).getCurrentViewIndex());
                list.add(s);
            } else if (source instanceof File) {
                switch (sources.get(i).getType()) {
                    case Image:
                        SourceImage s = new SourceImage(sources.get(i).getViews(), (File) source);
                        s.setDisplayTime(timestart, timeend);
                        s.setTransitionStart(transIn);
                        s.setTransitionStop(transOut);
                        s.setEffect(effect);
                        s.setViewIndex(sources.get(i).getCurrentViewIndex());
                        list.add(s);
                        break;
                }
            } else if (source instanceof SlideShow) {
                SourceImage s = new SourceImage(sources.get(i).getViews(), (SlideShow) source);
                s.setDisplayTime(timestart, timeend);
                s.setTransitionStart(transIn);
                s.setTransitionStop(transOut);
                s.setEffect(effect);
                s.setViewIndex(sources.get(i).getCurrentViewIndex());
                list.add(s);
            } else if (source instanceof LabelText) {
                SourceLabel s = new SourceLabel(sources.get(i).getViews(), ((LabelText) source));
                s.setDisplayTime(timestart, timeend);
                s.setTransitionStart(transIn);
                s.setTransitionStop(transOut);
                s.setEffect(effect);
                s.setViewIndex(sources.get(i).getCurrentViewIndex());
                list.add(s);
            } else if (source instanceof Frames.eList) {
                try {
                    SourceImage s = new SourceImage(sources.get(i).getViews(), (BufferedImage) Frames.getImage((Frames.eList) source), ((Frames.eList) source).name());
                    s.setDisplayTime(timestart, timeend);
                    s.setTransitionStart(transIn);
                    s.setTransitionStop(transOut);
                    s.setEffect(effect);
                    s.setViewIndex(sources.get(i).getCurrentViewIndex());
                    list.add(s);
                } catch (IOException ex) {
                }
            } else if (source instanceof String) {
                switch (sources.get(i).getType()) {
                    case Custom:
                        SourceFFMpeg s = SourceFFMpeg.getCustomInstance(sources.get(i), sources.get(i).getViews(), fps);
                        s.setFPS(fps);
                        s.setDisplayTime(timestart, timeend);
                        s.setTransitionStart(transIn);
                        s.setTransitionStop(transOut);
                        s.setEffect(effect);
                        s.setViewIndex(sources.get(i).getCurrentViewIndex());
                        list.add(s);
                        break;
                }
            }
        }
        return list;
    }

    @Override
    public void run() {
        BufferedImage img = new BufferedImage(mOutputSize.width, mOutputSize.height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = img.createGraphics();
        byte[] buffer = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        long frameDelay = 1000 / mFPS;
        long nextPTS = System.currentTimeMillis() + frameDelay;
        while (!mRequestStop) {
            java.util.Arrays.fill(buffer, (byte) 0);
            mTimeDelta = (System.currentTimeMillis() - mStartTime) / 1000;
            for (int i = 0; i < mSources.size(); i++) {
                Source s = mSources.get(i);
                if (s.isRemoteDisplay()) {
                    if ((s.getEndDisplayTime() == 0 || s.getEndDisplayTime() >= mTimeDelta)
                            && (s.getStartDisplayTime() <= mTimeDelta)) {
                        //Showing for the first time???
                        if (s.getTransitionStart() != Transition.NAMES.None) {
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
                            Rectangle r = s.getBounds();
                            BufferedImage source;
                            if (s.getEffect() == Effect.eEffects.None) {
                                source = s.getImage();
                            } else {
                                source = mEffects.apply(s.getEffect(), s.getImage());
                            }
                            g.drawImage(source, r.x, r.y, r.x + r.width, r.y + r.height, 0, 0, source.getWidth(), source.getHeight(), null);
                        }
                    }
                }
            }
            byte[] returnData = new byte[buffer.length];
            System.arraycopy(buffer, 0, returnData, 0, buffer.length);
            mData = returnData;
            mPreviewBuffer = returnData;
            long wait = nextPTS - System.currentTimeMillis();
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Compositor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            nextPTS += frameDelay;
        }
        mRequestStop = false;
    }

}
