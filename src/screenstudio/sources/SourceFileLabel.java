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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import screenstudio.gui.LabelText;
import screenstudio.targets.Layout;

/**
 *
 * @author patrick
 */
public class SourceFileLabel extends Source {

    private final File mFile;
    private final JLabel mLabel = new JLabel();
    private final BufferedImage mImage;
    private final byte[] mData;

    public SourceFileLabel(Rectangle bounds, int zOrder, float alpha, int delayTime, LabelText text) {
        super(bounds, zOrder, alpha, delayTime, text.getText());
        mFile = new File(text.getText());
        mLabel.setForeground(new Color(text.getForegroundColor()));
        mLabel.setBackground(new Color(text.getBackgroundColor()));
        mLabel.setText("");
        mLabel.setHorizontalAlignment(JLabel.LEFT);
        mLabel.setVerticalAlignment(JLabel.TOP);
        mLabel.setFont(new Font(mLabel.getFont().getName(), mLabel.getFont().getStyle(), bounds.height - 20));
        this.mType = Layout.SourceType.LabelText;
        this.mImageType = BufferedImage.TYPE_4BYTE_ABGR;
        mImage = new BufferedImage(bounds.width, bounds.height, mImageType);
        mData = ((DataBufferByte) mImage.getRaster().getDataBuffer()).getData();
        mLabel.setSize(bounds.getSize());
        mLabel.setOpaque(false);
        mLabel.paint(mImage.createGraphics());
        new Thread(() -> {
            long fileSize = mFile.length();
            long lastModified = mFile.lastModified();
            while (!mStopMe) {
                if (mFile.lastModified() != lastModified || mFile.length() != fileSize) {
                    try (java.io.InputStream in = mFile.toURI().toURL().openStream()) {
                        byte[] buffer = new byte[65536];
                        int count = in.read(buffer);
                        mLabel.setText(new String(buffer, 0, count));
                        mLabel.paint(mImage.createGraphics());
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(SourceFileLabel.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(SourceFileLabel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    fileSize = mFile.length();
                    lastModified = mFile.lastModified();
                }
                try {
                    Thread.sleep(delayTime);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SourceFileLabel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();

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
