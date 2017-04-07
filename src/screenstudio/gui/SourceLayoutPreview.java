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
package screenstudio.gui;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTable;
import screenstudio.gui.images.frames.Frames;
import screenstudio.sources.Compositor;
import screenstudio.sources.Screen;
import screenstudio.targets.Layout.SourceType;

/**
 *
 * @author patrick
 */
public class SourceLayoutPreview extends javax.swing.JPanel {

    private final JTable mSources;
    private final Rectangle outputSize = new Rectangle(0, 0, 720, 480);
    private Compositor compositer = null;
    private Robot mRobot = null;

    /**
     * Creates new form SourceLayoutPreview
     *
     * @param sources
     */
    public SourceLayoutPreview(JTable sources) {
        initComponents();
        this.setDoubleBuffered(true);
        mSources = sources;
        try {
            mRobot = new Robot();
        } catch (AWTException ex) {
            Logger.getLogger(SourceLayoutPreview.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setOutputWidth(int value) {
        outputSize.setSize(value, outputSize.height);
    }

    public void setOutputHeight(int value) {
        outputSize.setSize(outputSize.width, value);
    }

    @Override
    public void paint(Graphics g) {
        // Draw Output borders...
        int x = 0;
        int y = 0;
        int w = getWidth();
        int h = getHeight();
        g.setColor(this.getBackground());
        g.fillRect(0, 0, w, h);
        if (outputSize != null) {
            w = outputSize.width;
            h = outputSize.height;
            double ratio = outputSize.getWidth() / outputSize.getHeight();
            if (h > getHeight() - 1) {
                h = getHeight() - 1;
                w = (int) (h * ratio);
            }
            if (w > getWidth()) {
                w = getWidth();
                h = (int) (w / ratio) - 1;
            }
            x = (getWidth() - w) / 2;
            g.setClip(x, y, w, h);

            int fontSize = h / 25;
            Font font = new Font(getFont().getFontName(), getFont().getStyle(), fontSize);
            g.setFont(font);
            g.setColor(Color.BLACK);
            g.fillRect(x, y, w, h);
            if (mSources != null) {
                for (int i = mSources.getRowCount() - 1; i >= 0; i--) {
                    if ((Boolean) mSources.getValueAt(i, 0)) {
                        //Draw Preview...
                        g.setFont(font);
                        int sx = (int) mSources.getValueAt(i, 3);
                        int sy = (int) mSources.getValueAt(i, 4);
                        int sw = (int) mSources.getValueAt(i, 5);
                        int sh = (int) mSources.getValueAt(i, 6);
                        float salpha = (float) mSources.getValueAt(i, 7);
                        if (salpha < 0f) salpha = 0f;
                        if (salpha > 1f) salpha = 1f;
                        sx = (int) (x + (sx * w / outputSize.getWidth()));
                        sy = (int) (y + (sy * h / outputSize.getHeight()));
                        sw = (int) ((sw * w / outputSize.getWidth()));
                        sh = (int) ((sh * h / outputSize.getHeight()));
                        if (sw + sx > x + w) {
                            sw = (x + w - sx);
                        }
                        if (sy + sh > y + h) {
                            sh = y + h - sy;
                        }
                        BufferedImage img = null;
                        ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1).derive(salpha));

                        switch ((SourceType) mSources.getValueAt(i, 1)) {
                            case Desktop:
                                Screen s = (Screen) mSources.getValueAt(i, 2);
                                img = mRobot.createScreenCapture(s.getSize());
                                g.drawImage(img.getScaledInstance((int) (img.getWidth() * w / outputSize.getWidth()), (int) (img.getHeight() * h / outputSize.getHeight()), Image.SCALE_FAST), sx, sy, null);
                                break;
                            case Webcam:
                                g.setColor(Color.blue);
                                g.fillRect(sx, sy, sw, sh);
                                break;
                            case Image:
                                File sourceImg = (File) mSources.getValueAt(i, 2);
                                 {
                                    try {
                                        img = javax.imageio.ImageIO.read(sourceImg);
                                        g.drawImage(img.getScaledInstance((int) ((int) mSources.getValueAt(i, 5) * w / outputSize.getWidth()), (int) ((int) mSources.getValueAt(i, 6) * h / outputSize.getHeight()), Image.SCALE_FAST), sx, sy, null);
                                    } catch (IOException ex) {
                                        Logger.getLogger(SourceLayoutPreview.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                                break;
                            case LabelText:
                                img = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
                                LabelText text = (LabelText) mSources.getValueAt(i, 2);
                                JLabel label = new JLabel(text.getText());
                                label.setFont(new Font(text.getFontName(), Font.PLAIN, sh));
                                label.setSize((int) (((int) mSources.getValueAt(i, 5)) * w / outputSize.getWidth()), (int) (((int) mSources.getValueAt(i, 6)) * h / outputSize.getHeight()));
                                label.setLocation(0, 0);
                                label.setOpaque(true);
                                label.setForeground(new Color(text.getForegroundColor()));
                                label.setBackground(new Color(text.getBackgroundColor()));
                                label.paint(img.createGraphics());
                                g.drawImage(img, sx, sy, null);
                                break;
                            case Frame:
                                Frames.eList frameName = (Frames.eList) mSources.getValueAt(i, 2);
                                 {
                                    try {
                                        img = Frames.getImage(frameName);
                                        g.drawImage(img.getScaledInstance((int) ((int) mSources.getValueAt(i, 5) * w / outputSize.getWidth()), (int) ((int) mSources.getValueAt(i, 6) * h / outputSize.getHeight()), Image.SCALE_FAST), sx, sy, null);
                                    } catch (IOException ex) {
                                        Logger.getLogger(SourceLayoutPreview.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                                break;
                            default:
                                g.setColor(Color.gray);
                                g.fillRect(sx, sy, sw, sh);
                                break;
                        }
                        if (i == mSources.getSelectedRow()) {
                            g.setColor(Color.green);
                            g.drawRect(sx, sy, sw, sh);
                        }
                    }
                }
            }
            g.setFont(font);
            //draw output format that will be used...
            g.setColor(Color.WHITE);
            g.drawString("Output : " + outputSize.width + "X" + outputSize.height, x + 5, y + 20);
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBackground(new java.awt.Color(51, 51, 51));
        setPreferredSize(new java.awt.Dimension(320, 240));
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                formMouseDragged(evt);
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                formMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                formMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 320, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 240, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseDragged
        if (mSources.getSelectedRow() != -1) {
            int rowIndex = mSources.getSelectedRow();
            //int currentX = (Integer) mSources.getValueAt(rowIndex, 3);
            //int currentY = (Integer) mSources.getValueAt(rowIndex, 4);
            Point pos = getTranslatedPosition(evt.getX(), evt.getY());
            mSources.setValueAt(pos.x, rowIndex, 3);
            mSources.setValueAt(pos.y, rowIndex, 4);
            //System.out.println("Dragging " + mSources.getValueAt(rowIndex, 2).toString());
            mSources.repaint();
            repaint();
        }
    }//GEN-LAST:event_formMouseDragged

    private void formMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMousePressed
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }//GEN-LAST:event_formMousePressed

    private void formMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseReleased
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_formMouseReleased

    private Point getTranslatedPosition(int mouseX, int mouseY) {
        int w = outputSize.width;
        int h = outputSize.height;
        int newX = 0;
        int newY = 0;

        double ratio = outputSize.getWidth() / outputSize.getHeight();
        if (h > getHeight() - 1) {
            h = getHeight() - 1;
            w = (int) (h * ratio);
        }
        if (w > getWidth()) {
            w = getWidth();
            h = (int) (w / ratio) - 1;
        }
        int x = (getWidth() - w) / 2;
        int y = 0;
        // inside the area...
        if (mouseX > x && mouseY > y && mouseX < x + w && mouseY < y + h) {
            newX = (int) ((mouseX - x) * (outputSize.getWidth() / (double) w));
            newY = (int) ((mouseY - y) * (outputSize.getHeight() / (double) h));
        }
        return new Point(newX - 10, newY - 10);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
