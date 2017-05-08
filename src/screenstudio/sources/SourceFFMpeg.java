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

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import screenstudio.encoder.FFMpeg;
import screenstudio.encoder.ProcessReader;
import screenstudio.targets.Layout.SourceType;

/**
 *
 * @author patrick
 */
public class SourceFFMpeg extends Source implements Runnable {

    private FFMpeg mFFMpeg;
    private Process mProcess;
    private DataInputStream mInputData;
    private final String mInput;
    private int mFPS;
    private boolean mStopMe = false;
    private byte[] dataBuffer;

    @Override
    public void run() {
        while (!mStopMe) {
            try {
                byte[] buffer = new byte[dataBuffer.length];
                mInputData.readFully(buffer);
                dataBuffer = buffer;
                //Thread.sleep(10);
            } catch (IOException ex) {
                //Logger.getLogger(SourceFFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                mStopMe = true;
            } 
        }
    }

    protected enum DEVICES {
        Desktop,
        Webcam,
        File,
        Stream
    }

    public SourceFFMpeg(List<screenstudio.targets.Source.View> views, int fps, String input, SourceType type, String id) {
        super( views, 0, id, BufferedImage.TYPE_3BYTE_BGR);
        mInput = input;
        mFPS = fps;
        mType = type;
    }

    public void setFPS(int fps) {
        mFPS = fps;
    }

    @Override
    protected void getData(byte[] buffer) throws IOException {
        System.arraycopy(dataBuffer, 0, buffer, 0, buffer.length);
    }

    @Override
    protected void initStream() throws IOException {
        mStopMe = false;
        mFFMpeg = new FFMpeg(null);
        String command = mFFMpeg.getBin() + " " + mInput + " " + "-s " + mBounds.width + "x" + mBounds.height + " -r " + mFPS + " -f rawvideo -pix_fmt bgr24 -";
        mProcess = Runtime.getRuntime().exec(command);
        new Thread(new ProcessReader(mProcess.getErrorStream())).start();
        System.out.println(command);
        mInputData = new DataInputStream(mProcess.getInputStream());
        dataBuffer = new byte[mBounds.width * mBounds.height * 3];
        mInputData.readFully(dataBuffer);
        new Thread(this).start();
    }

    @Override
    protected void disposeStream() throws IOException {
        mStopMe = true;
        mProcess.getOutputStream().write("q\n".getBytes());
        try {
            mProcess.getOutputStream().flush();
            mProcess.getOutputStream().close();
        } catch (IOException ex) {
            //just in case the stream is already closed...
        }
        mInputData.close();
        mProcess.destroy();
        mProcess.destroyForcibly();
        mProcess = null;
        mInputData = null;
    }

    public static SourceFFMpeg getDesktopInstance(Screen display,List<screenstudio.targets.Source.View> views , int fps) {
        String input = " -f " + new FFMpeg(null).getDesktopFormat() + " -video_size " + display.getWidth() + "x" + display.getHeight() + " -i " + display.getId();
        if (Screen.isWindows()) {
            input = " -f " + new FFMpeg(null).getDesktopFormat() + " -video_size " + display.getWidth() + "x" + display.getHeight() + " -offset_x " + display.getSize().x + " -offset_y " + display.getSize().y + " " + " -i " + display.getId();
        }
        SourceFFMpeg f = new SourceFFMpeg(views, fps, input, SourceType.Desktop, display.getLabel());
        f.mCaptureX = display.getSize().x;
        f.mCaptureY = display.getSize().y;
        return f;
    }

    public static SourceFFMpeg getCustomInstance(screenstudio.targets.Source source,List<screenstudio.targets.Source.View> views , int fps) {
        String input = source.getSourceObject().toString();
        SourceFFMpeg f = new SourceFFMpeg(views, fps, input, SourceType.Custom, source.getID());
        return f;
    }
    public static SourceFFMpeg getWebcamInstance(Webcam webcam,List<screenstudio.targets.Source.View> views , int fps) {
        String inputFormat = " -video_size " + webcam.getWidth() + "x" + webcam.getHeight() + " -framerate " + fps;
        if (Screen.isWindows()){
            inputFormat = "";
        }
        String input = " -f " + new FFMpeg(null).getWebcamFormat() + inputFormat  + " -i " + webcam.getDevice();
        System.out.println(input);
        return new SourceFFMpeg(views, fps, input, SourceType.Webcam, webcam.getDevice());
    }
}
