/*
 * Copyright (C) 2016 patri
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
package screenstudio.encoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author patri
 */
public class ProcessReader implements Runnable {

    private InputStream mIn;

    public ProcessReader(InputStream in) {
        mIn = in;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[64000];
        while (true) {
            try {
                mIn.read(buffer);
                Thread.sleep(100);
            } catch (IOException ex) {
                break;
            } catch (InterruptedException ex) {
                break;
            }
        }
    }
}
