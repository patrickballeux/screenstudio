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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a class used to receive notification over the UDP Protocol
 * To test it:
 * echo -n "<H1><FONT BGCOLOR=BLACK COLOR=RED>Welcome to the new user</FONT></H1>" | nc -4u -w1 127.0.0.1 8899
 * @author patrick
 */
public class UDPNotifications implements Runnable {

    private int mPort = 8899;
    private boolean mStopMe = false;
    private NotificationListener mListener;

    public UDPNotifications(NotificationListener listener) {
        mListener = listener;
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            mStopMe = false;
            byte[] receiveData = new byte[1024];
            DatagramSocket serverSocket = null;
            while (!mStopMe && serverSocket == null) {
                try {

                    serverSocket = new DatagramSocket(mPort);
                    System.out.println(serverSocket.getLocalAddress().toString());
                    serverSocket.setSoTimeout(1000);
                } catch (SocketException ex) {
                    //Trying another port
                    System.err.println("Port UDP:" + mPort + " could not be used...");
                    mPort++;
                }
            }
            System.out.println("Listening on port UDP:" + mPort);
            while (!mStopMe && serverSocket != null) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    String message = new String(receivePacket.getData()).trim();
                    mListener.received(message);
                } catch (SocketTimeoutException ex) {
                    //continue...
                }
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (SocketException ex) {
            Logger.getLogger(UDPNotifications.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UDPNotifications.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stop() {
        mStopMe = true;
        mListener = null;
    }

    public int getPort() {
        return mPort;
    }
}
