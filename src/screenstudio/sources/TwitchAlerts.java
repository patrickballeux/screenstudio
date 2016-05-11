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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.encoder.FFMpeg;

/**
 *
 * @author patrick
 */
public class TwitchAlerts implements Runnable {

    private final TreeMap<File, String> values = new TreeMap();
    private final File mFolder;
    private boolean mStopMe = false;
    private final int mPort;
    private final Properties templates;

    public TwitchAlerts(File alertsFolder, int notificationPort) throws IOException {
        mFolder = alertsFolder;
        mPort = notificationPort;
        templates = new Properties();
        File props = new File(new FFMpeg().getHome() + "/RTMP", "TWITCHALERTS.properties");
        try (InputStream in = props.toURI().toURL().openStream()) {
            templates.load(in);
        }
        new Thread(this).start();
    }

    @Override
    public void run() {
        mStopMe = false;
        //Load ititial data...
        values.clear();
        for (File f : mFolder.listFiles()) {
            if (f.getName().endsWith(".txt")) {
                values.put(f, getContent(f));
            }
        }
        //monitor changes...
        System.out.println("Monitoring Twitch Alerts...");
        long lastLoading = System.currentTimeMillis();
        while (!mStopMe) {
            //Reload data each 5 seconds...
            if (System.currentTimeMillis() - lastLoading >= 5000) {
                for (File f : mFolder.listFiles()) {
                    if (f.getName().endsWith(".txt")) {
                        String content = getContent(f);
                        if (!values.get(f).equals(content) && content.trim().length()>0) {
                            try {
                                notifyScreenStudio(content, f.getName());
                            } catch (IOException ex) {
                                Logger.getLogger(TwitchAlerts.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            values.put(f, content);
                            break;
                        }
                    }
                }
                lastLoading = System.currentTimeMillis();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(TwitchAlerts.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void stop() {
        mStopMe = true;
    }

    private void notifyScreenStudio(String value, String filename) throws SocketException, IOException {
        if (templates.containsKey(filename)) {
            String template = templates.getProperty(filename).replace("@VALUE", value.trim().replaceAll("\n", ", "));
            byte[] message = template.getBytes();
            System.out.println("NOTIFICATION: " + template);
            // Get the internet address of the specified host
            InetAddress address = InetAddress.getByName("127.0.0.1");
            // Initialize a datagram packet with data and address
            DatagramPacket packet = new DatagramPacket(message, message.length, address, mPort);
            try ( // Create a datagram socket, send the packet through it, close it.
                    DatagramSocket dsocket = new DatagramSocket()) {
                dsocket.send(packet);
            }
        }

    }

    private String getContent(File f) {
        InputStream in = null;
        String data = "";
        try {

            byte[] buffer = new byte[65536];
            in = f.toURI().toURL().openStream();
            int count = in.read(buffer);
            if (count > 1) {
                data = new String(buffer, 0, count);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(TwitchAlerts.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TwitchAlerts.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(TwitchAlerts.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return data;
    }

    public static void main(String[] args) {
        try {
            TwitchAlerts t = new TwitchAlerts(new File("/home/patrick/twitchalerts"), 8899);
        } catch (IOException ex) {
            Logger.getLogger(TwitchAlerts.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
