/*
 * Copyright (C) 2017 patrick
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
package screenstudio.sources.transitions;

import java.awt.Rectangle;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.sources.Source;

/**
 *
 * @author patrick
 */
public class Transition implements Runnable {

    private final Source mSource;
    private boolean mIsRunning = false;
    private float mFromAlpha = -1f;
    private int mFromX = -1;
    private int mFromY = -1;
    private long mDuration = 1000;
    private float mToAlpha = -1f;
    private int mToX = -1;
    private int mToY = -1;
    private long mFPS = 10;

    public enum NAMES{
        None,
        FadeIn,
        FadeOut,
        EnterLeft,
        EnterRight,
        EnterTop,
        EnterBottom,
        ExitLeft,
        ExitRight,
        ExitTop,
        ExitBottom,
    }
    
    private Transition(Source s, long fps) {
        mSource = s;
        mFPS = fps;
    }

    public static Transition getInstance(NAMES n,Source s, long fps,Rectangle bounds){
        Transition t = new Transition(s,fps);
                t.setDuration(1000);
        switch (n){
            case FadeIn:
                t.setFromAlpha(0);
                t.setToAlpha(s.getAlpha().getAlpha());
                break;
            case FadeOut:
                t.setFromAlpha(s.getAlpha().getAlpha());
                t.setToAlpha(0);
                break;
            case EnterBottom:
                t.setFromX(bounds.height+1);
                t.setToX(s.getBounds().y);
                break;
            case EnterLeft:
                t.setFromX(0-s.getBounds().width-1);
                t.setToX(s.getBounds().x);
                break;
            case EnterRight:
                t.setFromX(bounds.width+1);
                t.setToX(s.getBounds().x);
                break;
            case EnterTop:
                t.setFromY(0-s.getBounds().height-1);
                t.setToY(s.getBounds().y);
                break;
            case ExitBottom:
                t.setFromY(s.getBounds().y);
                t.setToY(bounds.height+1);
                break;
            case ExitLeft:
                t.setFromX(s.getBounds().x);
                t.setToX(0-s.getBounds().width-1);
                break;
            case ExitRight:
                t.setFromY(s.getBounds().x);
                t.setToY(bounds.width+1);
                break;
            case ExitTop: 
                t.setFromY(s.getBounds().y);
                t.setToY(0-s.getBounds().width-1);
                break;
        }
        return t;
    }
    @Override
    public void run() {
        mIsRunning = true;
        if (mFromAlpha != -1) {
            mSource.setAlpha(mFromAlpha);
        } else {
            mFromAlpha = mSource.getAlpha().getAlpha();
        }
        if (mFromX != -1) {
            mSource.getBounds().x = mFromX;
        } else {
            mFromX = mSource.getBounds().x;
        }

        if (mFromY != -1) {
            mSource.getBounds().y = mFromY;
        } else {
            mFromY = mSource.getBounds().y;
        }

        if (mToAlpha == -1) {
            mToAlpha = mSource.getAlpha().getAlpha();
        }
        if (mToX == -1) {
            mToX = mSource.getBounds().x;
        }

        if (mToY == -1) {
            mToY = mSource.getBounds().y;
        }
        
        long end = System.currentTimeMillis() + mDuration;
        long frameDuration = 1000 / mFPS;
        float alphaSteps = (mToAlpha - mFromAlpha) / (float)(mDuration / frameDuration);
        int xSteps = (mToX - mFromX) / (int)(mDuration / frameDuration);
        int ySteps = (mToY - mFromY) / (int)(mDuration / frameDuration);
        while (end >= System.currentTimeMillis()) {
            mSource.setAlpha(mFromAlpha+=alphaSteps);
            mSource.getBounds().x = (mFromX+=xSteps);
            mSource.getBounds().y = (mFromY+=ySteps);
            try {
                Thread.sleep(frameDuration);
            } catch (InterruptedException ex) {
                Logger.getLogger(Transition.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        mSource.setAlpha(mToAlpha);
        mSource.getBounds().x = mToX;
        mSource.getBounds().y = mToY;
        mIsRunning = false;
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    /**
     * @return the mFromAlpha
     */
    public float getFromAlpha() {
        return mFromAlpha;
    }

    /**
     * @param mFromAlpha the mFromAlpha to set
     */
    public void setFromAlpha(float mFromAlpha) {
        this.mFromAlpha = mFromAlpha;
    }

    /**
     * @return the mFromX
     */
    public int getFromX() {
        return mFromX;
    }

    /**
     * @param mFromX the mFromX to set
     */
    public void setFromX(int mFromX) {
        this.mFromX = mFromX;
    }

    /**
     * @return the mFromY
     */
    public int getFromY() {
        return mFromY;
    }

    /**
     * @param mFromY the mFromY to set
     */
    public void setFromY(int mFromY) {
        this.mFromY = mFromY;
    }


    /**
     * @return the mDuration
     */
    public long getDuration() {
        return mDuration;
    }

    /**
     * @param mDuration the mDuration to set
     */
    public void setDuration(long mDuration) {
        this.mDuration = mDuration;
    }

    /**
     * @return the mToAlpha
     */
    public float getToAlpha() {
        return mToAlpha;
    }

    /**
     * @param mToAlpha the mToAlpha to set
     */
    public void setToAlpha(float mToAlpha) {
        this.mToAlpha = mToAlpha;
    }

    /**
     * @return the mToX
     */
    public int getToX() {
        return mToX;
    }

    /**
     * @param mToX the mToX to set
     */
    public void setToX(int mToX) {
        this.mToX = mToX;
    }

    /**
     * @return the mToY
     */
    public int getToY() {
        return mToY;
    }

    /**
     * @param mToY the mToY to set
     */
    public void setToY(int mToY) {
        this.mToY = mToY;
    }

}
