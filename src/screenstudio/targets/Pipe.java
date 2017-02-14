/*
 * Copyright (C) 2016 Patrick Balleux (@patrickballeux)
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
package screenstudio.targets;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author patri
 */
public class Pipe implements Runnable{
    private int mPort = 0;
    private OutputStream mOutput = null;
    private Socket mSocket = null;
    private ServerSocket mServer = null;
    public Pipe() throws IOException{
        mServer = new ServerSocket(0);
        mPort = mServer.getLocalPort();
    }

    public String getServer(){
        return "tcp://127.0.0.1:" + mPort;
    }
    public void write(byte[] data) throws IOException{
        if (mOutput != null){
            mOutput.write(data);
        }
    }
    
    public void close() throws IOException{
        if (mOutput != null){
            mOutput.close();
            mSocket.close();
        }
        if (mServer != null){
            mServer.close();
            mServer = null;
        }
    }
    @Override
    public void run() {
        try {
            mServer.setSoTimeout(30000);
            mPort = mServer.getLocalPort();
            mSocket = mServer.accept();
            mOutput = mSocket.getOutputStream();
            mServer.close();
            mServer = null;
        } catch (IOException ex) {
            Logger.getLogger(Pipe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
