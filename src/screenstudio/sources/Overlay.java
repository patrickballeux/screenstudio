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
package screenstudio.sources;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.encoder.FFMpeg;
import screenstudio.gui.overlays.Renderer;

/**
 *
 * @author patrick
 */
public class Overlay implements Runnable {

    private File mContent;
    private String mUserTextContent;
    private final Renderer htmlRenderer;
    private final int mFPS;
    private boolean stopME = false;
    private OverlayUnix mOutput = null;
    private final String mCommand;
    private boolean mIsPrivateMode = false;

    public Overlay(File content, Renderer.PanelLocation location, int panelSize, Screen screen, screenstudio.sources.Webcam webcam, int showDurationTime, String userTextContent, String command) throws IOException, InterruptedException {
        mContent = content;
        mCommand = command;
        mUserTextContent = userTextContent;
        htmlRenderer = new Renderer(location, panelSize, webcam, screen, showDurationTime);
        mFPS = screen.getFps();
        new Thread(this).start();
        mOutput = new OverlayUnix(htmlRenderer, mFPS);
    }

    public int getNotificationPort() {
        return htmlRenderer.getPort();
    }

    public void setWebcamFocus(boolean focus){
        htmlRenderer.setWebcamFocus(focus);
    }
    public boolean isWebcamFocus(){
        return htmlRenderer.isWebcamFocus();
    }
    public boolean isPrivateMode() {
        return mIsPrivateMode;
    }

    public void setPrivateMode(boolean value) {
        mIsPrivateMode = value;
        mOutput.setPrivateMode(value);
    }

    public void start() {
        mOutput.start();
    }

    public boolean isRunning() {
        return mOutput.isRunning();
    }

    public void setUserTextContent(String text) {
        mUserTextContent = text;
    }

    public void setContent(File content) {
        mContent = content;
    }

    public void stop() {
        stopME = true;
    }

    public int getWidth() {
        return htmlRenderer.getWidth();
    }

    public int getHeight() {
        return htmlRenderer.getHeight();
    }

    public String OutputURL() {
        if (mOutput == null) {
            return "";
        } else {
            return mOutput.getOutput().getAbsolutePath();
        }
    }

    public static ArrayList<File> getOverlays() throws IOException {

        File home = new FFMpeg().getHome();
        File overlayFolder = new File(home, "Overlays");
        if (!overlayFolder.exists()) {
            overlayFolder.mkdir();
        }
        File[] list = overlayFolder.listFiles((File folder, String filename) -> filename.endsWith("html") || filename.endsWith("txt") || filename.endsWith("url"));
        ArrayList<File> newList = new ArrayList();
        newList.add(new ComboBoxFile("None"));

        for (File f : list) {
            newList.add(new ComboBoxFile(f.getAbsolutePath()));
        }
        if (list.length == 0) {
            //No template found, add a default one...
            byte[] buffer = new byte[65000];
            java.io.InputStream in = Overlay.class.getResource("/screenstudio/sources/Default.html").openStream();
            int count = in.read(buffer);
            in.close();
            FileWriter out = new FileWriter(new File(overlayFolder, "Default.html"));
            out.write(new String(buffer, 0, count));
            out.close();
            newList.add(new ComboBoxFile(new File(overlayFolder, "Default.html").getAbsolutePath()));
        }
        return newList;
    }

    public static ArrayList<File> getWaterMarks() throws IOException {

        File home = new FFMpeg().getHome();
        File overlayFolder = new File(home, "Overlays");
        if (!overlayFolder.exists()) {
            overlayFolder.mkdir();
        }
        File[] list = overlayFolder.listFiles((File folder, String filename) -> filename.toLowerCase().endsWith("png"));
        ArrayList<File> newList = new ArrayList();
        newList.add(new ComboBoxFile("None"));

        for (File f : list) {
            newList.add(new ComboBoxFile(f.getAbsolutePath()));
        }
        return newList;
    }

    @Override
    public void run() {
        stopME = false;
        try {
            htmlRenderer.setText("<html></html>", "", "","");
            while (!stopME) {
                if (mContent != null) {
                    // Read content into renderer...
                    InputStream in = mContent.toURI().toURL().openStream();
                    byte[] data = new byte[(int) mContent.length()];
                    in.read(data);
                    if (mContent.getName().endsWith("html")) {
                        //Reading content from a local html file
                        htmlRenderer.setText(new String(data), mUserTextContent, mCommand,mContent.getParentFile().getAbsolutePath());
                    } else if (mContent.getName().endsWith("url")) {
                        //Reading content from a webpage...
                        data = new byte[65536];
                        String addr = new String(data);
                        in.close();
                        in = new java.net.URI(addr).toURL().openStream();
                        StringBuilder html = new StringBuilder();
                        int count = in.read(data);
                        while (count > 0) {
                            html.append(new String(data, 0, count));
                            count = in.read(data);
                        }
                        htmlRenderer.setText(html.toString(), mUserTextContent, mCommand,addr);
                    } else {
                        //Reading raw content from a text file
                        htmlRenderer.setText("<html>" + new String(data).replaceAll("\n", "<br>") + "</html>", mUserTextContent, mCommand,"");
                    }
                    in.close();
                    //System.out.println(htmlRenderer.getText());
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Overlay.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(Overlay.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (mOutput != null) {
            mOutput.stop();
        }
        htmlRenderer.stop();
        System.out.println("Exiting Overlay rendering...");

    }
}

class ComboBoxFile extends File {

    public ComboBoxFile(String pathname) {
        super(pathname);
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
