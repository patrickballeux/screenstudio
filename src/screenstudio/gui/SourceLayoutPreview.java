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
package screenstudio.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import screenstudio.sources.Compositer;
import screenstudio.sources.Screen;
import screenstudio.sources.Source;
import screenstudio.sources.SourceFFMpeg;
import screenstudio.sources.SourceImage;
import screenstudio.sources.Webcam;

/**
 *
 * @author patrick
 */
public class SourceLayoutPreview extends javax.swing.JPanel {

    private final JTable mSources;
    private final Rectangle outputSize = new Rectangle(0, 0, 720, 480);
    private Compositer compositer = null;

    /**
     * Creates new form SourceLayoutPreview
     *
     * @param sources
     */
    public SourceLayoutPreview(JTable sources) {
        initComponents();
        this.setDoubleBuffered(true);
        mSources = sources;
    }

    public void setOutputWidth(int value) {
        outputSize.setSize(value, outputSize.height);
    }

    public void setOutputHeight(int value) {
        outputSize.setSize(outputSize.width, value);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        // Draw Output borders...
        int x = 0;
        int y = 0;
        int w = getWidth();
        int h = getHeight();
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
            if (compositer != null) {
                BufferedImage img = compositer.getImage();
                if (img != null) {
                    g.drawImage(img.getScaledInstance(w, h, Image.SCALE_FAST), x, y, this);
                }
            } else {
                g.setColor(Color.BLACK);
                g.fillRect(x, y, w, h);
                if (mSources != null) {
                    for (int i = mSources.getRowCount() - 1; i >= 0; i--) {
                        if ((Boolean) mSources.getValueAt(i, 0)) {
                            int sx = (int) mSources.getValueAt(i, 3);
                            int sy = (int) mSources.getValueAt(i, 4);
                            int sw = (int) mSources.getValueAt(i, 5);
                            int sh = (int) mSources.getValueAt(i, 6);
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
                            switch (mSources.getValueAt(i, 1).toString()) {
                                case "Screen":
                                    g.setColor(Color.red);
                                    break;
                                case "Webcam":
                                    g.setColor(Color.blue);
                                    break;
                                case "Image":
                                    g.setColor(Color.ORANGE);
                                    break;
                                default:
                                    g.setColor(Color.gray);
                                    break;
                            }
                            g.fillRect(sx, sy, sw, sh);
                            if (i == mSources.getSelectedRow()) {
                                g.setColor(Color.green);
                                g.drawRect(sx, sy, sw, sh);
                            }
                            g.setColor(Color.white);
                            g.drawString(mSources.getValueAt(i, 2).toString(), sx + 5, sy + 15);
                        }
                    }
                }
                //draw output format that will be used...
                g.setColor(Color.WHITE);
                g.drawString("Output : " + outputSize.width + "X" + outputSize.height, x + 5, y + h - 10);
            }
        }
    }

    public List<Source> getSources() {
        java.util.ArrayList<Source> list = new java.util.ArrayList();
        for (int i = mSources.getRowCount() - 1; i >= 0; i--) {
            if ((Boolean) mSources.getValueAt(i, 0)) {
                int sx = (int) mSources.getValueAt(i, 3);
                int sy = (int) mSources.getValueAt(i, 4);
                int sw = (int) mSources.getValueAt(i, 5);
                int sh = (int) mSources.getValueAt(i, 6);
                float alpha = new Float(mSources.getValueAt(i, 7).toString());
                Object source = mSources.getValueAt(i, 2);
                // Detect type of source...
                if (source instanceof Screen) {
                    Screen screen = (Screen) source;
                    SourceFFMpeg s = SourceFFMpeg.getDesktopInstance(screen);
                    s.getBounds().setBounds(new Rectangle(sx, sy, sw, sh));
                    s.setAlpha(alpha);
                    s.setZOrder(i);
                    list.add(s);
                } else if (source instanceof Webcam) {
                    Webcam webcam = (Webcam) source;
                    SourceFFMpeg s = SourceFFMpeg.getWebcamInstance(webcam);
                    s.getBounds().setBounds(new Rectangle(sx, sy, sw, sh));
                    s.setAlpha(alpha);
                    s.setZOrder(i);
                    list.add(s);
                } else if (source instanceof File) {
                    if ("Image".equals(mSources.getValueAt(i, 1).toString())) {
                        SourceImage s = new SourceImage(new Rectangle(sx, sy, sw, sh), i, alpha, (File) source);
                        list.add(s);
                    }
                }
            }
        }
        return list;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popPreview = new javax.swing.JPopupMenu();
        popStartPreview = new javax.swing.JMenuItem();
        popStopPreview = new javax.swing.JMenuItem();

        popStartPreview.setText("Start Preview");
        popStartPreview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popStartPreviewActionPerformed(evt);
            }
        });
        popPreview.add(popStartPreview);

        popStopPreview.setText("Stop Preview");
        popStopPreview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popStopPreviewActionPerformed(evt);
            }
        });
        popPreview.add(popStopPreview);

        setComponentPopupMenu(popPreview);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void popStartPreviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popStartPreviewActionPerformed
        List<Source> list = getSources();
        compositer = new Compositer(list, outputSize, 10);
        new Thread(compositer).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (compositer != null) {
                    repaint();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SourceLayoutPreview.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                repaint();
            }
        }).start();
    }//GEN-LAST:event_popStartPreviewActionPerformed

    private void popStopPreviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popStopPreviewActionPerformed
        compositer.stop();
        compositer = null;
        repaint();
    }//GEN-LAST:event_popStopPreviewActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu popPreview;
    private javax.swing.JMenuItem popStartPreview;
    private javax.swing.JMenuItem popStopPreview;
    // End of variables declaration//GEN-END:variables
}
