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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import javax.swing.JLabel;
import screenstudio.gui.LabelText;
import screenstudio.targets.Layout;

/**
 *
 * @author patrick
 */
public class SourceLabel extends Source {

    private final JLabel mLabel = new JLabel();
    private final BufferedImage mImage;
    private final byte[] mData;

    public SourceLabel(Rectangle bounds, int zOrder, float alpha, LabelText text) {
        super(bounds, zOrder, alpha, 1000, text.getText());
        mLabel.setText(text.getText());
        mLabel.setForeground(new Color(text.getForegroundColor()));
        super.mBackground = text.getBackgroundColor();
        super.mForeground = text.getForegroundColor();
        mLabel.setOpaque((text.getBackgroundColor() & 0xFF000000) != 0);
        mLabel.setBackground(new Color(text.getBackgroundColor()));
        mLabel.setHorizontalAlignment(JLabel.CENTER);
        mLabel.setVerticalAlignment(JLabel.CENTER);
        mLabel.setFont(new Font(text.getFontName(), Font.BOLD, bounds.height - 20));
        super.setFontName(text.getFontName());
        this.mType = Layout.SourceType.LabelText;
        this.mImageType = BufferedImage.TYPE_4BYTE_ABGR;
        mImage = new BufferedImage(bounds.width, bounds.height, mImageType);
        mData = ((DataBufferByte) mImage.getRaster().getDataBuffer()).getData();
        mLabel.setSize(bounds.getSize());
        Graphics2D g = mImage.createGraphics();
        mLabel.paint(g);
        
    }

    public void setText(String value) {
        mLabel.setText(value);
        Graphics2D g = mImage.createGraphics();
        mLabel.paint(g);
    }

    @Override
    protected void getData(byte[] buffer) throws IOException {
        System.arraycopy(mData, 0, buffer, 0, buffer.length);
    }

    @Override
    protected void initStream() throws IOException {
        Graphics2D g = mImage.createGraphics();
        mLabel.paint(g);
    }

    @Override
    protected void disposeStream() throws IOException {

    }

}
