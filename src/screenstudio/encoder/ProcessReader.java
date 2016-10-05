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
        String line = "";
        int count = 0;
        while (true) {
            try {
                count = mIn.read(buffer);
                if (count > 0) {
                    line = new String(buffer, 0, count).trim();
                    if (line.length() > 0) {
                        System.out.println("FFMPEG: " + line);
                    }
                }
                Thread.sleep(100);
            } catch (IOException | InterruptedException ex) {
                break;
            }
        }
    }
}
