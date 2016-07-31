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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author patrick
 */
public class Compositor implements Runnable {

    private final java.util.List<Source> mSources;
    private BufferedImage mImage;
    private boolean mStopMe = false;
    private final int mFPS;
    private final Rectangle mOutputSize;

    public Compositor(java.util.List<Source> sources, Rectangle outputSize, int fps) {
        sources.sort((a, b) -> Integer.compare(b.getZOrder(), a.getZOrder()));
        mSources = sources;
        mOutputSize = outputSize;
        mFPS = fps;
    }

    
    @Override
    public void run() {
        mStopMe = false;
        boolean flip = false;
        mImage = new BufferedImage(mOutputSize.width, mOutputSize.height, BufferedImage.TYPE_3BYTE_BGR);
        BufferedImage mImage1 = new BufferedImage(mOutputSize.width, mOutputSize.height, BufferedImage.TYPE_3BYTE_BGR);
        BufferedImage mImage2 = new BufferedImage(mOutputSize.width, mOutputSize.height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g1 = mImage1.createGraphics();
        Graphics2D g2 = mImage2.createGraphics();
        Graphics2D g = g1;
        for (Source s : mSources) {
            new Thread(s).start();
        }
        while (!mStopMe) {
            g.clearRect(0, 0, mImage.getWidth(), mImage.getHeight());
            for (Source s : mSources) {
                g.setComposite(s.getAlpha());
                g.drawImage(s.getImage(), s.getBounds().x, s.getBounds().y, null);
            }
            if (flip) {
                mImage = mImage2;
                g = g1;
            } else {
                mImage = mImage1;
                g = g2;
            }
            flip = !flip;
            try {
                Thread.sleep(1000 / mFPS);
            } catch (InterruptedException ex) {
                Logger.getLogger(Compositor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        for (Source s : mSources) {
            s.stop();
        }
        g.dispose();
    }

    public int getFPS(){
        return mFPS;
    }
    public BufferedImage getImage() {
        return mImage;
    }

    public int getWidth(){
        return mOutputSize.width;
    }
    public int getHeight(){
        return mOutputSize.height;
    }
    public void stop() {
        mStopMe = true;
    }
}
