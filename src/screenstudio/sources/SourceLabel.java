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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private long mStartTimeStamp = System.currentTimeMillis();
    private final String mText;

    public SourceLabel(Rectangle bounds, int zOrder, float alpha, LabelText text) {
        super(bounds, zOrder, alpha, 1000, text.getText());
        mText = text.getText();
        mLabel.setText(replaceTags(mText));
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
        mLabel.setText(replaceTags(value));
        Graphics2D g = mImage.createGraphics();
        mLabel.paint(g);
    }

    @Override
    protected void getData(byte[] buffer) throws IOException {
        mLabel.setText(replaceTags(mText));
        Graphics2D g = mImage.createGraphics();
        java.util.Arrays.fill(mData, (byte)0);
        mLabel.paint(g);
        System.arraycopy(mData, 0, buffer, 0, buffer.length);
    }

    @Override
    protected void initStream() throws IOException {
        mStartTimeStamp = System.currentTimeMillis();
        Graphics2D g = mImage.createGraphics();
        mLabel.paint(g);
    }

    @Override
    protected void disposeStream() throws IOException {

    }
    private final DateFormat formatDate = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    private final DateFormat formatTime = DateFormat.getTimeInstance(DateFormat.LONG, Locale.getDefault());

    private String replaceTags(String text) {
        String retValue = text + "";
        retValue = retValue.replaceAll("@CURRENTDATE", formatDate.format(new Date()));
        retValue = retValue.replaceAll("@CURRENTTIME", formatTime.format(new Date()));
        retValue = retValue.replaceAll("@RECORDINGTIME", ((System.currentTimeMillis() - mStartTimeStamp) / 1000 / 60) + " min");
        retValue = retValue.replaceAll("@STARTTIME", formatTime.format(new Date(mStartTimeStamp)));

        int index = retValue.indexOf("file:///");
        while (index != -1) {
            int toIndex = retValue.indexOf(";", index);
            if (toIndex == -1) {
                toIndex = retValue.indexOf(" ", index);
            }
            if (toIndex == -1) {
                toIndex = retValue.length() - 1;
            }
            if (toIndex != -1) {
                String file = retValue.substring(index, toIndex + 1);
                retValue = retValue.replaceAll(file, getFileContent(new File(retValue.substring(index + 6, toIndex))));
                index = retValue.indexOf("file:///", toIndex + 1);
            } else {
                index = retValue.indexOf("file:///", index + 1);
            }
        }

        index = retValue.indexOf("@UPDATE");
        if (index != -1) {
            int toIndex = retValue.indexOf("MIN@");
            if (toIndex == -1) {
                toIndex = retValue.indexOf("SEC@");
            }
            if (toIndex != -1) {
                String update = retValue.substring(index, toIndex + 4);
                try {
                    String intValue = update.replaceAll("@UPDATE", "").replaceAll(" ", "").replaceAll("MIN@", "").replaceAll("SEC@", "");
                    int value = new Integer(intValue.trim());
                    if (update.endsWith("MIN@")) {
                        value = value * 60000;
                    } else if (update.endsWith("SEC@")) {
                        value = value * 1000;
                    }
                    setDelayTime(value);
                    retValue = retValue.replaceAll(update, "");
                } catch (Exception ex) {
                    System.err.println("Parsing update value failed:" + ex.getMessage());
                }
            }
        }
        return retValue;
    }

    private String getFileContent(File file) {
        String text = "";
        if (file.exists()) {
            try {
                InputStream in = file.toURI().toURL().openStream();
                byte[] data = new byte[(int) file.length()];
                in.read(data);
                in.close();
                text = new String(data).trim();

            } catch (IOException ex) {
                Logger.getLogger(SourceLabel.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        return text;
    }

}
