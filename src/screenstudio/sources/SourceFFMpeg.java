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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
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
    private final Rectangle mCaptureSize;
    private boolean mStopMe = false;
    private byte[] dataBuffer;

    @Override
    public void run() {
        while (!mStopMe) {
            try {
                byte[] buffer = new byte[dataBuffer.length];
                mInputData.readFully(buffer);
                dataBuffer = buffer;
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

    public SourceFFMpeg(Rectangle captureSize, Rectangle outputSize, int fps, String input, SourceType type, String id) {
        super(outputSize, 1, 1, 0, id, BufferedImage.TYPE_3BYTE_BGR);
        mInput = input;
        mFPS = fps;
        mCaptureSize = captureSize;
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

    protected String getInput(String source, DEVICES type) {
        String input = "";

        switch (type) {
            case Desktop:
                input = " -f " + mFFMpeg.getDesktopFormat() + " -video_size " + mCaptureSize.width + "x" + mCaptureSize.height + " -i " + source;
                break;
            case File:
                input = " -i " + source;
                break;
            case Stream:
                input = " -i " + source;
                break;
            case Webcam:
                input = " -f " + mFFMpeg.getWebcamFormat() + " -i " + source;
                break;
        }
        return input;
    }

    public static SourceFFMpeg getDesktopInstance(Screen display, Rectangle outputSize, int fps) {
        String input = " -f " + new FFMpeg(null).getDesktopFormat() + " -video_size " + display.getWidth() + "x" + display.getHeight() + " -i " + display.getId();
        if (Screen.isWindows()) {
            input = " -f " + new FFMpeg(null).getDesktopFormat() + " -video_size " + display.getWidth() + "x" + display.getHeight() + " -offset_x " + display.getSize().x + " -offset_y " + display.getSize().y + " " + " -i " + display.getId();
        }
        SourceFFMpeg f = new SourceFFMpeg(display.getSize(), outputSize, fps, input, SourceType.Desktop, display.getLabel());
        f.mCaptureX = display.getSize().x;
        f.mCaptureY = display.getSize().y;
        return f;
    }

    public static SourceFFMpeg getWebcamInstance(Webcam webcam, int fps) {
        String input = " -f " + new FFMpeg(null).getWebcamFormat() + " -r " + fps + " -i " + webcam.getDevice();
        System.out.println(input);
        return new SourceFFMpeg(webcam.getSize(), new Rectangle(webcam.getSize()), fps, input, SourceType.Webcam, webcam.getDevice());
    }

    public static SourceFFMpeg getFileInstance(Rectangle bounds, java.io.File file, int fps) {
        return new SourceFFMpeg(bounds, bounds, fps, "-i " + file.getAbsolutePath(), SourceType.Video, file.getAbsolutePath());
    }

    public static SourceFFMpeg getStreamInstance(Rectangle bounds, String url, int fps) {
        return new SourceFFMpeg(bounds, bounds, fps, "-i " + url, SourceType.Stream, url);
    }
}
