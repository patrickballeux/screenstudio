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
import java.io.IOException;
import javax.swing.JLabel;
import screenstudio.targets.Layout;

/**
 *
 * @author patrick
 */
public class SourceLabel extends Source{

    private final JLabel mLabel = new JLabel();
    private final BufferedImage mImage;
    private final byte[] mData;
    public SourceLabel(Rectangle bounds, int zOrder, float alpha, String text) {
        super(bounds, zOrder, alpha, 1000, text);
        mLabel.setText(text);
        mLabel.setHorizontalAlignment(JLabel.LEFT);
        mLabel.setVerticalAlignment(JLabel.TOP);
        this.mType = Layout.SourceType.LabelText;
        this.mImageType = BufferedImage.TYPE_4BYTE_ABGR;
        mImage = new BufferedImage(bounds.width, bounds.height, mImageType);
        mData = ((DataBufferByte) mImage.getRaster().getDataBuffer()).getData();
        mLabel.setSize(bounds.getSize());
        mLabel.paint(mImage.createGraphics());
    }

    public void setText(String value){
        mLabel.setText(value);
        Graphics2D g = mImage.createGraphics();
        g.clearRect(0,0,mImage.getWidth(),mImage.getHeight() );
        mLabel.paint(g);
    }
    @Override
    protected void getData(byte[] buffer) throws IOException {
        System.arraycopy(mData, 0, buffer, 0, buffer.length);
    }

    @Override
    protected void initStream() throws IOException {
        Graphics2D g = mImage.createGraphics();
        g.clearRect(0,0,mImage.getWidth(),mImage.getHeight() );
        mLabel.paint(g);
    }

    @Override
    protected void disposeStream() throws IOException {
        
    }
    
}
