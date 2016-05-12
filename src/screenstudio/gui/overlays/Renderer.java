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
package screenstudio.gui.overlays;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import screenstudio.sources.DesktopViewer;
import screenstudio.sources.NotificationListener;
import screenstudio.sources.Screen;
import screenstudio.sources.UDPNotifications;
import screenstudio.sources.WebcamViewer;

/**
 *
 * @author patrick
 */
public class Renderer implements NotificationListener{

    private WebcamViewer mViewer;
    private DesktopViewer mDesktop;
    private long startingTime;
    private long showEndTime;
    private JLabel lblText;
    private boolean mIsUpdating = false;
    private boolean mStopMe = false;
    private String mText = "";
    private long nextTextUpdate = 0;
    private PanelLocation panelLocation;
    private WebcamLocation webcamLocation;
    private final BufferedImage textBuffer;
    private UDPNotifications notifications;
    private long lastNotificationTime = 0;
    private JLabel notificationMessage = null;
    

    private int desktopX = 0;
    private int desktopY = 0;
    private int textX = 0;
    private int textY = 0;
    private int webcamX = 0;
    private int webcamY = 0;
    private int textSize = 0;

    @Override
    public void received(String message) {
        lastNotificationTime = System.currentTimeMillis();
        notificationMessage.setText("<HTML><BODY width="+notificationMessage.getWidth()+" height="+notificationMessage.getHeight()+">" + message + "</BODY></HTML>");
        notificationMessage.validate();
        System.out.println("Message received: " + message);
    }

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

    public int getWidth() {
        switch (panelLocation) {
            case Top:
            case Bottom:
                return mDesktop.getImage().getWidth();
            case Left:
            case Right:
                return mDesktop.getImage().getWidth() + lblText.getWidth();
        }
        return mDesktop.getImage().getWidth();
    }

    public int getHeight() {
        switch (panelLocation) {
            case Top:
            case Bottom:
                return mDesktop.getImage().getHeight() + lblText.getHeight();
            case Left:
            case Right:
                return mDesktop.getImage().getHeight();
        }
        return mDesktop.getImage().getHeight();
    }

    private void setPositions() {
        desktopX = 0;
        desktopY = 0;
        textX = 0;
        textY = 0;
        webcamX = 0;
        webcamY = 0;
        switch (panelLocation) {
            case Top:
                desktopY = textBuffer.getHeight();
                if (mViewer != null) {
                    switch (webcamLocation) {
                        case Top:
                        case Left:
                            textX = mViewer.getImage().getWidth();
                            break;
                        case Bottom:
                        case Right:
                            webcamX = textBuffer.getWidth();
                            break;
                    }
                }
                break;
            case Bottom:
                if (mViewer != null) {
                    switch (webcamLocation) {
                        case Top:
                        case Left:
                            textX = mViewer.getImage().getWidth();
                            textY = mViewer.getImage().getHeight();
                            webcamY = mDesktop.getImage().getHeight();
                            break;
                        case Bottom:
                        case Right:
                            textY = mDesktop.getImage().getHeight();
                            webcamX = textBuffer.getWidth();
                            webcamY = mDesktop.getImage().getHeight();
                            break;
                    }
                } else {
                    textY = mDesktop.getImage().getHeight();
                }
                break;
            case Left:
                desktopX = textBuffer.getWidth();

                if (mViewer != null) {
                    switch (webcamLocation) {
                        case Top:
                        case Left:
                            textX = mViewer.getImage().getHeight();
                            break;
                        case Bottom:
                        case Right:
                            webcamY = textBuffer.getHeight();

                            break;
                    }
                }
                break;
            case Right:
                if (mViewer != null) {
                    switch (webcamLocation) {
                        case Top:
                        case Left:
                            textX = mDesktop.getImage().getWidth();
                            textY = mViewer.getImage().getHeight();
                            webcamX = mDesktop.getImage().getWidth();
                            break;
                        case Bottom:
                        case Right:
                            textX = mDesktop.getImage().getWidth();
                            webcamX = mDesktop.getImage().getWidth();
                            webcamY = textBuffer.getHeight();
                            break;
                    }
                } else {
                    textX = mDesktop.getImage().getWidth();
                }
                break;
        }
//        System.out.println("Desktop X,Y: " + desktopX + "," + desktopY);
//        System.out.println("Text X,Y: " + textX + "," + textY);
//        System.out.println("Webcam X,Y: " + webcamX + "," + webcamY);

    }

    public Renderer(PanelLocation location, int panelSize, screenstudio.sources.Webcam webcam, Screen screen, int showDuration) {
        startingTime = System.currentTimeMillis();
        showEndTime = System.currentTimeMillis() + (showDuration * 60000);
        textSize = panelSize;
        notifications = new UDPNotifications(this);
        notificationMessage = new JLabel("<HTML><BODY></BODY></HTML>");
        mDesktop = new DesktopViewer(screen);
        new Thread(mDesktop).start();
        mViewer = null;

        if (webcam != null) {
            mViewer = new WebcamViewer(screen, new File(webcam.getDevice()), webcam.getWidth(), webcam.getHeight(), webcam.getFps());
            new Thread(mViewer).start();
        }
        lblText = new JLabel();
        panelLocation = location;
        if (webcam != null) {
            webcamLocation = webcam.getLocation();
        }
        switch (location) {
            case Top:
            case Bottom:
                if (webcam != null) {
                    lblText.setPreferredSize(new Dimension(screen.getWidth() - webcam.getWidth(), webcam.getHeight()));
                } else {
                    lblText.setPreferredSize(new Dimension(screen.getWidth(), textSize));
                }
                break;
            case Left:
            case Right:
                if (webcam != null) {
                    lblText.setPreferredSize(new Dimension(webcam.getWidth(), screen.getHeight() - webcam.getHeight()));
                } else {
                    lblText.setPreferredSize(new Dimension(textSize, screen.getHeight()));
                }
                break;
        }
        lblText.setSize(lblText.getPreferredSize());
        lblText.setLocation(0, 0);
        textBuffer = new BufferedImage((int) lblText.getPreferredSize().getWidth(), (int) lblText.getPreferredSize().getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        notificationMessage.setPreferredSize(new Dimension(screen.getWidth(), 150));
        notificationMessage.setVisible(true);
        notificationMessage.setSize(notificationMessage.getPreferredSize());
        notificationMessage.setLocation(0,250);
        notificationMessage.setVerticalAlignment(JLabel.TOP);
        notificationMessage.setFont(new Font("Monospaced", Font.BOLD, 24));
    }

    public boolean IsUpdating() {
        return mIsUpdating;
    }

    public int getPort(){
        return notifications.getPort();
    }
    public void stop() {
        mStopMe = true;
        mDesktop.stop();
        if (mViewer != null) {
            mViewer.stop();
        }
    }

    
    public void paint(Graphics g) {
        if (System.currentTimeMillis() > nextTextUpdate) {
            setPositions();
            lblText.setText(replaceTags(mText));
            lblText.revalidate();
            nextTextUpdate = System.currentTimeMillis() + 1000;
        }
        lblText.paint(textBuffer.getGraphics());
        BufferedImage desktop = mDesktop.getImage();
        g.drawImage(desktop, desktopX, desktopY, null);
        g.drawImage(textBuffer, textX, textY, null);
        if (mViewer != null) {
            BufferedImage webcam = mViewer.getImage();
            g.drawImage(webcam, webcamX, webcamY, null);
        }
        if (System.currentTimeMillis() - lastNotificationTime <= 10000){
            ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(10000-(System.currentTimeMillis() - lastNotificationTime))/10000F));
            notificationMessage.paint(g);
            ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
    }

    public void setText(String text, String userTextContent, String command) {
        mIsUpdating = true;
        mText = text.replaceAll("@TEXT", userTextContent);
        if (command.length() > 0) {
            String commandContent = getCommandContent(command);
            if (mText.toUpperCase().contains("<HTML")) {
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
            Logger.getLogger(Renderer.class.getName()).log(Level.SEVERE, null, ex);
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

}
