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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.targets.Layout.SourceType;

/**
 *
 * @author patrick
 */
public abstract class Source implements Runnable {

    protected Rectangle mBounds = new Rectangle(0, 0, 320, 240);
    private int mZ = 0;
    private AlphaComposite mAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1);
    protected int mImageType = BufferedImage.TYPE_3BYTE_BGR;
    // Time to wait before fetching next image in milliseconds
    private int mDelayTime = 10;
    
    private BufferedImage mImage = null;
    private boolean mImageFlip = false;

    private boolean mStopMe = false;

    protected SourceType mType = null;
    
    protected abstract void getData(byte[] buffer) throws IOException;

    protected abstract void initStream() throws IOException;

    protected abstract void disposeStream() throws IOException;

    protected Source(Rectangle bounds, int zOrder, float alpha, int delayTime) {
        mBounds = bounds;
        mZ = zOrder;
        mAlpha = mAlpha.derive(alpha);
        mDelayTime = delayTime;
    }

    public SourceType getType(){
        return mType;
    }
    @Override
    public void run() {
        mStopMe = false;
        BufferedImage mImage3ByteBRG = new BufferedImage(mBounds.width, mBounds.height, mImageType);
        BufferedImage mImageBuffer1 = new BufferedImage(mBounds.width, mBounds.height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage mImageBuffer2 = new BufferedImage(mBounds.width, mBounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = mImageBuffer1.createGraphics();
        Graphics2D g2 = mImageBuffer2.createGraphics();
        byte[] buffer = ((DataBufferByte) mImage3ByteBRG.getRaster().getDataBuffer()).getData();
        try {
            initStream();
            while (!mStopMe) {

                getData(buffer);
                if (mImageFlip) {
                    g1.drawImage(mImage3ByteBRG, 0, 0, null);
                    mImage = mImageBuffer1;
                } else {
                    g2.drawImage(mImage3ByteBRG, 0, 0, null);
                    mImage = mImageBuffer2;
                }
                mImageFlip = !mImageFlip;
                if (mDelayTime > 0) {
                    try {
                        Thread.sleep(mDelayTime);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Source.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Source.class.getName()).log(Level.SEVERE, null, ex);
            mStopMe = true;
        }
        try {
            disposeStream();
        } catch (IOException ex) {
            Logger.getLogger(Source.class.getName()).log(Level.SEVERE, null, ex);
        }
        g1.dispose();
        g2.dispose();
    }

    public void stop() {
        mStopMe = true;
    }

    public BufferedImage getImage() {
        return mImage;
    }

    public Rectangle getBounds() {
        return mBounds;
    }

    public void setAlpha(float value) {
        mAlpha = mAlpha.derive(value);
    }
    public AlphaComposite getAlpha(){
        return mAlpha;
    }
    public void setZOrder (int value){
        mZ = value;
    }
    public int getZOrder (){
        return mZ;
    }
}
