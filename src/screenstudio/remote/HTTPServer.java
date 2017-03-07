/*
 * Copyright (C) 2017 patrick
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
package screenstudio.remote;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import screenstudio.sources.Compositor;
import screenstudio.sources.Source;

/**
 *
 * @author patrick
 */
public class HTTPServer implements Runnable {

    private boolean mStopMe = false;
    private ServerSocket mServer;
    private Compositor mCompositor;
    private int mPort = 8080;
    private ArrayList<String> mSourcesIDs;
    private final JMenuItem mMenuAction;

    public HTTPServer(Compositor comp, ArrayList<String> sourceIDs, JMenuItem recordAction) {
        mCompositor = comp;
        mSourcesIDs = sourceIDs;
        mMenuAction = recordAction;
    }

    public int getPort() {
        return mPort;
    }

    public void setSourceIDs(ArrayList<String> sourceIDs) {
        mSourcesIDs = sourceIDs;
    }

    public void setCompositor(Compositor c) {
        mCompositor = c;
    }

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) //Trying 10 times...
        {
            try {
                mServer = new ServerSocket(mPort);
                mServer.setSoTimeout(1000);
                break;
            } catch (IOException ex) {
                //Port is busy, trying another one...
                mPort++;
            }
        }
        System.out.println("Remote is listening on port " + mPort);
        while (!mStopMe) {
            try {
                Socket connection = mServer.accept();
                //Receiving connection
                new Thread(() -> {
                    try {
                        handleRequest(connection);
                    } catch (Exception ex) {
                        //Logger.getLogger(HTTPServer.class.getName()).log(Level.SEVERE, null, ex);
                        //do nothing
                    }
                }).start();

            } catch (java.net.SocketTimeoutException ex) {
                continue;
            } catch (IOException ex) {
                Logger.getLogger(HTTPServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            mServer.close();
        } catch (IOException ex) {
            Logger.getLogger(HTTPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleRequest(Socket conn) throws IOException {
        String fileToServe = "";
        String[] parameters = new String[0];

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
        String request = in.readLine();
        if (request != null) {
            request = java.net.URLDecoder.decode(request, "UTF-8");
            while (true) {
                String misc = in.readLine();
                if (misc == null || misc.length() == 0) {
                    break;
                }
            }
            if (!request.startsWith("GET") || request.length() < 14 || !(request.endsWith("HTTP/1.0") || request.endsWith("HTTP/1.1"))) {
                throw new IOException("Bad request");
            }
            String req = request.substring(4, request.length() - 9).trim();
            if (req.indexOf('?') != -1) {
                fileToServe = req.substring(0, req.indexOf('?'));
                parameters = req.substring(req.indexOf('?') + 1).split("&");
            } else {
                fileToServe = req;
                parameters = new String[0];
            }
            System.out.println("Handling: " + req);
            for (String p : parameters) {
                if (p.equals("action=record")) {
                    //start stop recording...
                    mMenuAction.doClick();
                } else if (p.startsWith("source")) {
                    if (mCompositor != null) {
                        int index = Integer.parseInt(p.split("=")[0].replace("source", ""));
                        Source s = mCompositor.getSources().get(index);
                        s.setRemoteDisplay(p.split("=")[1].equals("on"));
                    }
                }
            }
            switch (fileToServe) {
                case "/":
                    System.out.println("Writing homepage");
                    out.write("HTTP/1.0 200 OK\r\n" + "Content-Type: " + "text/html" + "\r\n" + "Date: " + new Date() + "\r\nServer: ScreenStudio Remote\r\n\r\n");
                    sendHomeScreen(out);
                    out.flush();
                    break;
                case "/preview.png":
                    out.write("HTTP/1.0 200 OK\r\n" + "Content-Type: " + "image/png" + "\r\n" + "Date: " + new Date() + "\r\nServer: ScreenStudio Remote\r\n\r\n");
                    out.flush();
                    sendPreview(conn.getOutputStream());
                    break;
                case "/logo.png":
                case "/apple-touch-icon.png":
                case "/apple-touch-icon-120x120-precomposed.png":
                case "/apple-touch-icon-120x120.png":
                    out.write("HTTP/1.0 200 OK\r\n" + "Content-Type: " + "image/png" + "\r\n" + "Date: " + new Date() + "\r\nServer: ScreenStudio Remote\r\n\r\n");
                    out.flush();
                    sendResources("/screenstudio/remote/logo128.png",conn.getOutputStream());
                    break;
                case "/favicon.ico":
                    out.write("HTTP/1.0 200 OK\r\n" + "Content-Type: " + "image/png" + "\r\n" + "Date: " + new Date() + "\r\nServer: ScreenStudio Remote\r\n\r\n");
                    out.flush();
                     sendResources("/screenstudio/remote/favicon.ico",conn.getOutputStream());
                    break;
            }
        }
        conn.close();
    }

    private void sendResources(String name, OutputStream out) throws IOException{
        try (InputStream in = this.getClass().getResource(name).openStream()) {
            byte[] buffer = new byte[in.available()];
            int count = in.read(buffer);
            out.write(buffer, 0, count);
        }
        out.flush();
    }
    private void sendHomeScreen(BufferedWriter out) throws IOException {
        String html;
        try (InputStream in = this.getClass().getResource("/screenstudio/remote/index.html").openStream()) {
            byte[] buffer = new byte[65536];
            int count = in.read(buffer);
            html = new String(buffer, 0, count);
        }
        if (mCompositor != null){
            html = html.replace(">Capture<", ">Stop<");
        }
        String sources = "";
        if (mSourcesIDs != null) {
            int index = mSourcesIDs.size() - 1;
            for (int i = 0; i < mSourcesIDs.size(); i++) {
                String s = mSourcesIDs.get(index);
                boolean checkboxValue = true;
                if (mCompositor != null) {
                    Source source = mCompositor.getSources().get(index);
                    checkboxValue = source.isRemoteDisplay();
                }
                if (checkboxValue) {
                    sources += "\r\n<form class=source name='source" + index + "'><input type=hidden value='off' name='source" + index + "'><input type=checkbox checked onchange='document.forms.source" + index + ".submit();'>" + s + "</form>";
                } else {
                    sources += "\r\n<form class=source name='source" + index + "'><input type=hidden value='on' name='source" + index + "'><input type=checkbox onchange='document.forms.source" + index + ".submit();'>" + s + "</form>";
                }
                index--;
            }
        }
        html = html.replace("@SOURCES", sources);
        out.write(html);
    }

    private void sendPreview(OutputStream out) throws IOException,IndexOutOfBoundsException {

        if (mCompositor != null) {
            int newW = mCompositor.getWidth() / 3;
            int newH = mCompositor.getHeight() / 3;
            BufferedImage img = new BufferedImage(mCompositor.getWidth(), mCompositor.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            System.arraycopy(mCompositor.getImage(), 0, data, 0, data.length);
            BufferedImage smallImg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = smallImg.createGraphics();
            g.drawImage(img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH), 0, 0, null);
            javax.imageio.ImageIO.write(smallImg, "png", out);
            g.dispose();
            out.flush();
        } else {
            BufferedImage img = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.black);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            javax.imageio.ImageIO.write(img, "png", out);
            g.dispose();
        }
    }

    public void shutdown() {
        mStopMe = true;
    }

}
