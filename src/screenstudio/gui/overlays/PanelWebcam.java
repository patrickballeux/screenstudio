/*
 * Copyright (C) 2014 Patrick Balleux
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
package screenstudio.gui.overlays;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.sources.Screen;
import screenstudio.sources.WebcamViewer;

/**
 *
 * @author patrick
 */
public final class PanelWebcam extends javax.swing.JPanel {

    private final WebcamViewer mViewer;
    private final long startingTime;
    private final long showEndTime;
    private boolean mIsUpdating = false;
    private boolean mStopMe = false;
    private String mText = "";
    private long nextTextUpdate = 0;

    public enum WebcamLocation {
        Top,
        Bottom,
        Left,
        Right,
    }

    public enum PanelLocation {
        Top,
        Bottom,
        Left,
        Right,
    }

    /**
     * Creates new form PanelWebcam
     *
     * @param location
     * @param webcam
     * @param screen
     * @param showDuration
     * @param webcamTitle
     */
    public PanelWebcam(PanelLocation location, screenstudio.sources.Webcam webcam, Screen screen, int showDuration, String webcamTitle) {
        initComponents();
        startingTime = System.currentTimeMillis();
        showEndTime = System.currentTimeMillis() + (showDuration * 60000);
        if (webcam != null) {
            mViewer = new WebcamViewer(new File(webcam.getDevice()), webcam.getWidth(), webcam.getHeight(), webcamTitle, webcam.getFps());
            mViewer.setOpaque(true);
            panWebcam.setOpaque(true);
            panWebcam.add(mViewer, BorderLayout.CENTER);
            panWebcam.setPreferredSize(new Dimension(webcam.getWidth(), webcam.getHeight()));
            new Thread(mViewer).start();
            System.out.println("Started webcam viewer");
            switch (location) {
                case Top:
                case Bottom:
                    switch (webcam.getLocation()) {
                        case Bottom:
                            setWebcamLocation(WebcamLocation.Right);
                            break;
                        case Top:
                            setWebcamLocation(WebcamLocation.Left);
                            break;
                        default:
                            setWebcamLocation(webcam.getLocation());
                            break;
                    }
                    this.setSize((int) screen.getSize().getWidth(), webcam.getHeight());
                    this.setPreferredSize(new Dimension((int) screen.getSize().getWidth(), webcam.getHeight()));
                    lblText.setPreferredSize(new Dimension((int) screen.getSize().getWidth() - webcam.getWidth(), webcam.getHeight()));
                    break;
                case Left:
                case Right:
                    switch (webcam.getLocation()) {
                        case Left:
                            setWebcamLocation(WebcamLocation.Top);
                            break;
                        case Right:
                            setWebcamLocation(WebcamLocation.Bottom);
                            break;
                        default:
                            setWebcamLocation(webcam.getLocation());
                            break;
                    }
                    this.setSize(webcam.getWidth(), (int) screen.getSize().getHeight());
                    this.setPreferredSize(new Dimension(webcam.getWidth(), (int) screen.getSize().getHeight()));
                    lblText.setPreferredSize(new Dimension(webcam.getWidth(), (int) screen.getSize().getHeight() - webcam.getHeight()));
                    break;
            }
        } else {
            mViewer = null;
            this.remove(panWebcam);
            switch (location) {
                case Top:
                case Bottom:
                    this.setSize((int) screen.getSize().getWidth(), 120);
                    this.setPreferredSize(new Dimension((int) screen.getSize().getWidth(), 120));
                    break;
                case Left:
                case Right:
                    this.setSize(320, (int) screen.getSize().getHeight());
                    this.setPreferredSize(new Dimension(320, (int) screen.getSize().getHeight()));
                    break;
            }
            lblText.setPreferredSize(this.getPreferredSize());
        }
        lblText.setSize(lblText.getPreferredSize());
        String tips = "<H1>Supported tags</H1>";
        tips += "<ul>";
        tips += "<li>@CURRENTDATE (Current date)</li>";
        tips += "<li>@CURRENTTIME (Current time)</li>";
        tips += "<li>@RECORDINGTIME (Recording time in minutes)</li>";
        tips += "<li>@STARTTIME (Time when the recording started)</li>";
        tips += "<li>@REMAININGTIME (Time remaining in minutes)</li>";
        tips += "<li>@TEXT (Custom text from the text entry in the Panel tab...)</li>";
        tips += "<li>@COMMAND (Custom text from a command output...)</li>";
        tips += "</ul>";
        this.setToolTipText("<html>" + tips + "</html>");
        this.revalidate();
        this.doLayout();

    }

    public void setWebcamLocation(WebcamLocation location) {
        if (mViewer != null) {
            this.remove(panWebcam);
            switch (location) {
                case Top:
                    this.add(panWebcam, BorderLayout.NORTH);
                    break;
                case Bottom:
                    this.add(panWebcam, BorderLayout.SOUTH);
                    break;
                case Left:
                    this.add(panWebcam, BorderLayout.WEST);
                    break;
                case Right:
                    this.add(panWebcam, BorderLayout.EAST);
                    break;
                default:
                    this.add(panWebcam, BorderLayout.NORTH);
                    break;
            }
        }
    }

    public boolean IsUpdating() {
        return mIsUpdating;
    }

    public void stop() {
        mStopMe = true;
        if (mViewer != null) {
            mViewer.stop();
        }
    }

    public void startPreview() {
        new Thread(new Runnable() {

            @Override
            public void run() {

                mStopMe = false;
                while (!mStopMe) {
                    repaint();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(PanelWebcam.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }
        }).start();
    }

    @Override
    public void paint(Graphics g) {
        if (System.currentTimeMillis() > nextTextUpdate) {
            lblText.setText(replaceTags(mText));
            lblText.revalidate();
            nextTextUpdate = System.currentTimeMillis() + 1000;
        }
        super.paint(g);
    }

    public void setText(String text, String userTextContent, String command) {
        mIsUpdating = true;
        mText = text.replaceAll("@TEXT", userTextContent);
        if (command.length() > 0) {
            String commandContent = getCommandContent(command);
            if (mText.toUpperCase().contains("<HTML")){
                commandContent = commandContent.replaceAll("\n", "<BR>");
            }
            mText = mText.replaceAll("@COMMAND", commandContent);
        }
        if (mText.toUpperCase().contains("<HTML>")) {
            mText = mText.replaceFirst("<BODY", "<BODY width=" + (int) lblText.getPreferredSize().getWidth() + " height=" + (int) lblText.getPreferredSize().getHeight());
            mText = mText.replaceFirst("<body", "<body width=" + (int) lblText.getPreferredSize().getWidth() + " height=" + (int) lblText.getPreferredSize().getHeight());
        }
        mIsUpdating = false;
    }

    private String getCommandContent(String command) {
        String retValue = "";
        try {
            Process p = Runtime.getRuntime().exec(command);
            InputStream in = p.getInputStream();
            byte[] data = new byte[65000];
            int count = in.read(data);
            while (count > 0 && !mStopMe) {
                retValue += new String(data, 0, count);
                count = in.read(data);
            }
            retValue = retValue.trim();
        } catch (IOException ex) {
            Logger.getLogger(PanelWebcam.class.getName()).log(Level.SEVERE, null, ex);
        }
        return retValue;
    }

    private final DateFormat formatDate = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    private final DateFormat formatTime = DateFormat.getTimeInstance(DateFormat.LONG, Locale.getDefault());

    private String replaceTags(String text) {
        String retValue = text + "";
        retValue = retValue.replaceAll("@CURRENTDATE", formatDate.format(new Date()));
        retValue = retValue.replaceAll("@CURRENTTIME", formatTime.format(new Date()));
        retValue = retValue.replaceAll("@RECORDINGTIME", (System.currentTimeMillis() - startingTime) / 60000 + " min");
        retValue = retValue.replaceAll("@STARTTIME", formatTime.format(new Date(startingTime)));
        retValue = retValue.replaceAll("@REMAININGTIME", (((showEndTime - System.currentTimeMillis()) / 60000) + 1) + " min");
        return retValue;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panWebcam = new javax.swing.JPanel();
        lblText = new javax.swing.JLabel();

        setBackground(java.awt.Color.black);
        setFont(new java.awt.Font("Ubuntu", 0, 12)); // NOI18N
        setLayout(new java.awt.BorderLayout());

        panWebcam.setBackground(new java.awt.Color(102, 102, 102));
        panWebcam.setBorder(null);
        panWebcam.setForeground(java.awt.Color.white);
        panWebcam.setPreferredSize(new java.awt.Dimension(160, 120));
        panWebcam.setLayout(new java.awt.BorderLayout());
        add(panWebcam, java.awt.BorderLayout.WEST);

        lblText.setBackground(java.awt.Color.black);
        lblText.setForeground(new java.awt.Color(19, 219, 19));
        lblText.setText("<html>ScreenStudio</html>");
        lblText.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblText.setBorder(null);
        lblText.setOpaque(true);
        add(lblText, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblText;
    private javax.swing.JPanel panWebcam;
    // End of variables declaration//GEN-END:variables
}
