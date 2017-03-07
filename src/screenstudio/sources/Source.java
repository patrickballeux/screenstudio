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

import java.awt.AlphaComposite;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.sources.transitions.Transition;
import screenstudio.targets.Layout.SourceType;

/**
 *
 * @author patrick
 */
public abstract class Source {

    /**
     * @return the mTransitionStart
     */
    public Transition.NAMES getTransitionStart() {
        return mTransitionStart;
    }

    /**
     * @param mTransitionStart the mTransitionStart to set
     */
    public void setTransitionStart(Transition.NAMES mTransitionStart) {
        this.mTransitionStart = mTransitionStart;
    }

    /**
     * @return the mTransitionStop
     */
    public Transition.NAMES getTransitionStop() {
        return mTransitionStop;
    }

    /**
     * @param mTransitionStop the mTransitionStop to set
     */
    public void setTransitionStop(Transition.NAMES mTransitionStop) {
        this.mTransitionStop = mTransitionStop;
    }

    public enum EVENTS {
        START,
        STOP,
        DISPLAYED,
        HIDDEN,
    }
    protected Rectangle mBounds = new Rectangle(0, 0, 320, 240);
    protected int mCaptureX = 0;
    protected int mCaptureY = 0;
    protected int mForeground = 0;
    protected int mBackground = 0xFFFFFF;
    private int mZ = 0;
    private AlphaComposite mAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1);
    protected int mImageType = BufferedImage.TYPE_3BYTE_BGR;
    protected String mFontName = "Dialog";
    protected long mStartDisplayTime = 0;
    protected long mEndDisplayTime = 0;
    private final BufferedImage mImage;
    private final byte[] mBuffer;
    private Transition.NAMES mTransitionStart =Transition.NAMES.None; 
    private Transition.NAMES mTransitionStop =Transition.NAMES.None; 
    private ISourceEvents mListener = null;
    private boolean mRemoteDisplay = true;
    private String mID = "";

    protected SourceType mType = null;

    protected abstract void getData(byte[] buffer) throws IOException;

    protected abstract void initStream() throws IOException;

    protected abstract void disposeStream() throws IOException;

    public void setRemoteDisplay(boolean show){
        mRemoteDisplay = show;
    }
    public boolean isRemoteDisplay(){
        return mRemoteDisplay;
    }
    protected Source(Rectangle bounds, int zOrder, float alpha, int delayTime, String id, int imageType) {
        mBounds = bounds;
        mZ = zOrder;
        mAlpha = mAlpha.derive(alpha);
        mID = id;
        mImageType = imageType;
        mImage = new BufferedImage(mBounds.width, mBounds.height, mImageType);
        mBuffer = ((DataBufferByte) mImage.getRaster().getDataBuffer()).getData();
    }

    public int getCaptureX() {
        return mCaptureX;
    }

    public int getCaptureY() {
        return mCaptureY;
    }

    public String getFontName() {
        return mFontName;
    }

    public void setFontName(String value) {
        mFontName = value;
    }

    public int getForeground() {
        return mForeground;
    }

    public int getBackground() {
        return mBackground;
    }

    public String getID() {
        return mID;
    }

    public SourceType getType() {
        return mType;
    }

    public void setDisplayTime(long start, long end) {
        mStartDisplayTime = start;
        mEndDisplayTime = end;
    }

    public long getStartDisplayTime() {
        return mStartDisplayTime;
    }

    public long getEndDisplayTime() {
        return mEndDisplayTime;
    }

    public void start() {
        try {
            initStream();
            if (mListener != null) {
                mListener.event(EVENTS.START);
            }
        } catch (IOException ex) {
            Logger.getLogger(Source.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stop() {
        try {
            disposeStream();
            if (mListener != null) {
                mListener.event(EVENTS.STOP);
            }
        } catch (IOException ex) {
            Logger.getLogger(Source.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public BufferedImage getImage() {
        try {
            getData(mBuffer);
        } catch (IOException ex) {
            Logger.getLogger(Source.class.getName()).log(Level.SEVERE, null, ex);
        }
        return mImage;
    }

    public Rectangle getBounds() {
        return mBounds;
    }

    public void setAlpha(float value) {
        if (value > 1f) {
            value = 1f;
        } else if (value < 0f) {
            value = 0f;
        }
        if (mListener != null) {
            if (value == 0f && mAlpha.getAlpha() != 0f) {
                mListener.event(EVENTS.HIDDEN);
            } else if (value != 0f && mAlpha.getAlpha() == 0f) {
                mListener.event(EVENTS.DISPLAYED);
            }
        }
        mAlpha = mAlpha.derive(value);
    }

    public AlphaComposite getAlpha() {
        return mAlpha;
    }

    public void setZOrder(int value) {
        mZ = value;
    }

    public int getZOrder() {
        return mZ;
    }

    public void setListener(ISourceEvents l) {
        mListener = l;
    }
}
