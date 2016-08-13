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
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import screenstudio.targets.Layout;

/**
 *
 * @author patrick
 */
public class SourceImage extends Source{

    private final File mFile;
    private byte[] data = null;
    public SourceImage(Rectangle bounds, int zOrder, float alpha,File image) {
        super(bounds, zOrder, alpha, 1000,image.getAbsolutePath());
        mFile = image;
        mImageType = BufferedImage.TYPE_4BYTE_ABGR;
        mType = Layout.SourceType.Image;
    }

    @Override
    protected void getData(byte[] buffer) throws IOException {
        System.arraycopy(data, 0, buffer, 0, buffer.length);
    }

    @Override
    protected void initStream() throws IOException {
        BufferedImage image = javax.imageio.ImageIO.read(mFile);
        BufferedImage buffer = new BufferedImage(mBounds.width,mBounds.height,mImageType);
        Graphics2D g = buffer.createGraphics();
        g.drawImage(image.getScaledInstance(mBounds.width, mBounds.height, Image.SCALE_SMOOTH),0,0,null);
        data = ((DataBufferByte) buffer.getRaster().getDataBuffer()).getData();
    }

    @Override
    protected void disposeStream() throws IOException {
        //nothing to do
    }
    
}
