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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    private final float mOriginalAlpha;
    private String mLastContent;
    private boolean mOnChangeOnly = false;
    private boolean mOneLiner = false;
    private int mOneLinerLastLine = 0;
    private long mOneLinerLastUpdate = System.currentTimeMillis();
    private long mReloadTime = 1000;
    private long mLastReloadTime = System.currentTimeMillis();
    private String mLastRenderedText = "";
    private Graphics2D g;

    public SourceLabel(List<screenstudio.targets.Source.View> views, LabelText text) {
        super(views, 100, text.getText(), BufferedImage.TYPE_4BYTE_ABGR);
        mOriginalAlpha = views.get(0).Alpha;
        mText = updateWithTextTags(text.getText());
        mLastRenderedText = replaceTags(mText);
        mLabel.setText(mLastRenderedText);
        mLastContent = mLabel.getText();
        mLabel.setForeground(new Color(text.getForegroundColor()));
        super.mBackground = text.getBackgroundColor();
        super.mForeground = text.getForegroundColor();
        mLabel.setOpaque((text.getBackgroundColor() & 0xFF000000) != 0);
        mLabel.setBackground(new Color(text.getBackgroundColor()));
        mLabel.setHorizontalAlignment(JLabel.CENTER);
        mLabel.setVerticalAlignment(JLabel.CENTER);
        mLabel.setFont(new Font(text.getFontName(), Font.BOLD, views.get(0).Height - 20));
        super.setFontName(text.getFontName());
        this.mType = Layout.SourceType.LabelText;
        mImage = new BufferedImage(views.get(0).Width, views.get(0).Height, mImageType);
        mData = ((DataBufferByte) mImage.getRaster().getDataBuffer()).getData();
        mLabel.setSize(new Dimension(views.get(0).Width, views.get(0).Height));
        g = mImage.createGraphics();
        mLabel.paint(g);
        mLastReloadTime = System.currentTimeMillis();
    }

    private String updateWithTextTags(String text) {
        String retValue = text + "";
        int index = retValue.indexOf("@UPDATE");
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
                    mReloadTime = value;
                    retValue = retValue.replaceAll(update, "");
                } catch (Exception ex) {
                    System.err.println("Parsing update value failed:" + ex.getMessage());
                }
            }
        }
        index = retValue.indexOf("@ONCHANGEONLY");
        if (index != -1) {
            setAlpha(0);
            mOnChangeOnly = true;
            retValue = retValue.replaceAll("@ONCHANGEONLY", "");
        }
        index = retValue.indexOf("@ONELINER");
        if (index != -1) {
            mOneLiner = true;
            retValue = retValue.replaceAll("@ONELINER", "");
        }
        return retValue;
    }

    @Override
    protected void getData(byte[] buffer) throws IOException {
        if (System.currentTimeMillis() - mLastReloadTime > mReloadTime) {
            mLastRenderedText = replaceTags(mText);
            if (!mOneLiner) {
                mLabel.setText(mLastRenderedText);
                java.util.Arrays.fill(mData, (byte) 0);
                mLabel.paint(g);
            }
            mLastReloadTime = System.currentTimeMillis();
        }
        if (mOneLiner) {
            if (System.currentTimeMillis() - mOneLinerLastUpdate >= 5000) {
                mOneLinerLastLine++;
                mOneLinerLastUpdate = System.currentTimeMillis();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int r, gr, b;
                        r = mLabel.getForeground().getRed();
                        gr = mLabel.getForeground().getGreen();
                        b = mLabel.getForeground().getBlue();
                        mLabel.setForeground(new Color(r, gr, b, 0));
                        String lines[] = mLastRenderedText.split("\n");
                        if (mOneLinerLastLine >= lines.length) {
                            mOneLinerLastLine = 0;
                        }
                        mLabel.setText(lines[mOneLinerLastLine]);
                        java.util.Arrays.fill(mData, (byte) 0);
                        mLabel.paint(g);
                        for (int alpha = 0; alpha < 255; alpha += 5) {
                            mLabel.setForeground(new Color(r, gr, b, alpha));
                            mLabel.paint(g);
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(SourceLabel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        mLabel.setForeground(new Color(r, gr, b, 255));
                        mLabel.paint(g);
                    }
                }).start();
            }
        }
        System.arraycopy(mData, 0, buffer, 0, buffer.length);
        if (mOnChangeOnly && !mLastContent.equals(mLabel.getText()) && getAlpha().getAlpha() == 0.0F) {
            setAlpha(mOriginalAlpha);
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SourceLabel.class.getName()).log(Level.SEVERE, null, ex);
                }
                for (float alpha = mOriginalAlpha; alpha >= 0.0F; alpha -= 0.05) {
                    setAlpha(alpha);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SourceLabel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                setAlpha(0);
            }).start();
            mLastContent = mLabel.getText();
        }
    }

    @Override
    protected void initStream() throws IOException {
        mStartTimeStamp = System.currentTimeMillis();
        mLastReloadTime = System.currentTimeMillis();
        g = mImage.createGraphics();
        mLabel.paint(g);
    }

    @Override
    protected void disposeStream() throws IOException {
        g.dispose();
    }
    private final DateFormat formatDate = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    private final DateFormat formatTime = DateFormat.getTimeInstance(DateFormat.LONG, Locale.getDefault());

    private String replaceTags(String text) {
        String retValue = text + "";

        int index = retValue.indexOf("file:///");
        while (index != -1) {
            int toIndex = retValue.indexOf(";", index);
            if (toIndex == -1) {
                toIndex = retValue.indexOf(" ", index);
            }
            if (toIndex == -1) {
                toIndex = retValue.indexOf("\n", index);
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
        retValue = retValue.replaceAll("@CURRENTDATE", formatDate.format(new Date()));
        retValue = retValue.replaceAll("@CURRENTTIME", formatTime.format(new Date()));
        retValue = retValue.replaceAll("@RECORDINGTIME", ((System.currentTimeMillis() - mStartTimeStamp) / 1000 / 60) + " min");
        retValue = retValue.replaceAll("@STARTTIME", formatTime.format(new Date(mStartTimeStamp)));
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
                Logger.getLogger(SourceLabel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return text;
    }

}
