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
import java.awt.Image;
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
public class Compositor {

    private java.util.List<Source> mSources;
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

    public void setCurrentView(int index){
        ArrayList<Source> newList = new ArrayList<>();
        for (Source s : mSources){
            s.setViewIndex(index);
        }
        newList.addAll(mSources);
        newList.sort((a, b) -> Integer.compare(b.getZOrder(), a.getZOrder()));
        mSources=newList;
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
                    Rectangle r = s.getBounds();
                    if (img.getWidth() != r.width || img.getHeight() != r.height){
                        g.drawImage(img.getScaledInstance(r.width, r.height, Image.SCALE_DEFAULT), r.x, r.y, null);
                    } else {
                        g.drawImage(img, r.x, r.y, null);
                    }
                    
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

    public static List<Source> getSources(ArrayList<screenstudio.targets.Source> sources, int fps) {
        java.util.ArrayList<screenstudio.sources.Source> list = new java.util.ArrayList();
        for (int i = sources.size()- 1; i >= 0; i--) {
            long timestart = sources.get(i).getStartTime();
            long timeend = sources.get(i).getEndTime();
            Transition.NAMES transIn = sources.get(i).getTransitionStart();
            Transition.NAMES transOut =sources.get(i).getTransitionStop();
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
                list.add(s);
            } else if (source instanceof File) {
                switch (sources.get(i).getType()) {
                    case Image:
                        SourceImage s = new SourceImage(sources.get(i).getViews(), (File) source);
                        s.setDisplayTime(timestart, timeend);
                        s.setTransitionStart(transIn);
                        s.setTransitionStop(transOut);
                        s.setEffect(effect);
                        list.add(s);
                        break;
                }
            } else if (source instanceof LabelText) {
                SourceLabel s = new SourceLabel(sources.get(i).getViews(), ((LabelText) source));
                s.setDisplayTime(timestart, timeend);
                s.setTransitionStart(transIn);
                s.setTransitionStop(transOut);
                s.setEffect(effect);
                list.add(s);
            } else if (source instanceof Frames.eList) {
                try {
                    SourceImage s = new SourceImage(sources.get(i).getViews(), (BufferedImage) Frames.getImage((Frames.eList) source), ((Frames.eList) source).name());
                    s.setDisplayTime(timestart, timeend);
                    s.setTransitionStart(transIn);
                    s.setTransitionStop(transOut);
                    s.setEffect(effect);
                    list.add(s);
                } catch (IOException ex) {
                }
            }
        }
        return list;
    }

}
