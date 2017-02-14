/*
 * Copyright (C) 2014 Patrick Balleux (Twitter: @patrickballeux
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
 * ffmpeg -f x11grab -framerate 5 -video_size 1280x800 -i :0.0  -c:v libx264 -crf 18 -profile:v baseline -maxrate 400k -bufsize 1835k -pix_fmt yuv420p -flags -global_header ftp://someserver/www/screenstudio.m3u8
 */
package screenstudio.encoder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.sources.Compositor;
import screenstudio.sources.Screen;
import screenstudio.targets.Pipe;

/**
 *
 * @author patrick
 */
public class FFMpeg implements Runnable {

    /**
     * List of supported presets for FFMPEG
     */
    public enum Presets {

        ultrafast,
        superfast,
        veryfast,
        faster,
        fast,
        medium,
        slow,
        slower,
        veryslow
    }

    /**
     * Supported audio rate for output
     */
    public enum AudioRate {

        Audio11K,
        Audio22K,
        Audio44K,
        Audio48K
    }

    public enum RunningState {
        Starting,
        Running,
        Stopped,
        Error

    }

    private RunningState state = RunningState.Stopped;
    private String lastErrorMessage = "";

    private String bin = "ffmpeg  ";
    private String nonVerboseMode = " -nostats -loglevel 0 ";
    private String desktopFormat = "x11grab";
    private String webcamFormat = "v4l2";
    private String audioInput = "default";
    private String audioFormat = "pulse";
    private File mHome = new File(".");
    private String mThreading = "";
    private String output = "capture.mp4";
    private boolean mStopMe = false;
    private boolean mDebugMode = false;
    private String mITSOffset = "";

    private final String compositorFormat = "rawvideo -pix_fmt bgr24";
    private final Compositor compositor;
    // Audio
    private String audioRate = "44100";

    //Output
    private String videoBitrate = "9000";
    private final String audioBitrate = "128";
    private String videoEncoder = "libx264";
    private String audioEncoder = "aac";
    private String muxer = "mp4";
    private String preset = "ultrafast";
    private String strictSetting = "-2";

    private String backgroundMusic = "";

    /**
     * Main class wrapper for FFMpeg
     *
     * @param c
     */
    public FFMpeg(Compositor c) {
        //Creating default folder for capturing videos...
        initDefaults();
        compositor = c;
    }

    public File getHome() {
        return mHome;
    }

    public void stop() {
        System.out.println("Stopping requested...");
        mStopMe = true;
    }

    public void setDebugMode(boolean value) {
        mDebugMode = value;
    }

    /**
     * Set the audio parameters
     *
     * @param rate : Audio rate for the output
     * @param input : device to use
     */
    public void setAudio(AudioRate rate, String input, Float offset, File bgMusic) {
        switch (rate) {
            case Audio44K:
                audioRate = "44100";
                break;
            case Audio48K:
                audioRate = "48000";
                break;
            case Audio22K:
                audioRate = "22050";
                break;
            case Audio11K:
                audioRate = "11025";
                break;
        }
        audioInput = input;
        if (offset != 0) {
            mITSOffset = offset.toString();
        } else {
            mITSOffset = "";
        }
        if (bgMusic != null) {
            backgroundMusic = bgMusic.getAbsolutePath();
        }
    }

    public RunningState getState() {
        return state;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    /**
     * Set the output format of the video file/stream
     *
     * @param format
     * @param p
     * @param videoBitrate
     * @param server
     * @param key
     * @param outputFolder
     */
    public void setOutputFormat(FORMATS format, Presets p, int videoBitrate, String server, String key, String outputFolder) {
        preset = p.name();
        if (outputFolder.contains("\\")) {
            // Probably a path on Windows...
            if (!outputFolder.endsWith("\\")) {
                outputFolder += "\\";
            }
        } else if (!outputFolder.endsWith("/")) {
            outputFolder += "/";
        }
        switch (format) {
            case FLV:
                muxer = "flv";
                videoEncoder = "libx264";
                audioEncoder = "libmp3lame";
                output = outputFolder + generateRandomName() + ".flv";
                break;
            case MP4:
                muxer = "mp4";
                videoEncoder = "libx264";
                audioEncoder = "libmp3lame";
                output = outputFolder + generateRandomName() + ".mp4";
                break;
            case MOV:
                muxer = "mov";
                videoEncoder = "libx264";
                audioEncoder = "libmp3lame";
                output = outputFolder + generateRandomName() + ".mov";
                break;
            case TS:
                preset = "";
                muxer = "mpegts";
                videoEncoder = "mpeg2video";
                audioEncoder = "mp2";
                output = outputFolder + generateRandomName() + ".ts";
                break;
            case GIF:
                muxer = "gif";
                videoEncoder = "gif";
                audioEncoder = "";
                preset = "";
                String randomName = generateRandomName();
                output = outputFolder + randomName + ".gif";
                break;
            case RTMP:
            case HITBOX:
            case TWITCH:
            case USTREAM:
            case VAUGHNLIVE:
            case YOUTUBE:
            case FACEBOOK:
                muxer = "flv";
                videoEncoder = "libx264";
                audioEncoder = "aac";
                if (server.length() == 0) {
                    output = key;
                } else {
                    output = server + "/" + key;
                }
                break;
            case BROADCAST:
                preset = "";
                muxer = "mpegts";
                videoEncoder = "mpeg2video";
                audioEncoder = "mp2";
                output = "udp://255.255.255.255:8888?broadcast=1";
                break;
            case HTTP:
                muxer = "hls";
                videoEncoder = "libx264";
                audioEncoder = "libmp3lame";
                output = outputFolder + "stream.m3u8";
                break;
        }

        this.videoBitrate = videoBitrate + "";
    }

    /**
     * Get the output file being used
     *
     * @return
     */
    public String getOutput() {
        return output;
    }

    /**
     * Generate a random name for the video file
     *
     * @return
     */
    public String generateRandomName() {
        String name = "capture-" + System.currentTimeMillis();
        return name;
    }

    private String[] getCommands(String tcpServer) {
        ArrayList<String> c = new ArrayList<>();
        // Add binary path
        c.add(bin);
        // Enable debug mode
        if (!mDebugMode) {
            c.add("-nostats");
            c.add("-loglevel");
            c.add("0");
        }
        //compositor
        c.add("-f");
        c.add("rawvideo");
        c.add("-pix_fmt");
        c.add("bgr24");
        c.add("-framerate");
        c.add("" + compositor.getFPS());
        c.add("-video_size");
        c.add(compositor.getWidth() + "x" + compositor.getHeight());
        c.add("-i");
        c.add(tcpServer);

        // Capture Audio
        if (!videoEncoder.equals("gif")) {
            c.add("-f");
            c.add(audioFormat);
            if (mITSOffset.length() > 0) {
                c.add("-itsoffset");
                c.add(mITSOffset);
            }
            c.add("-i");
            c.add(audioInput);

            if (backgroundMusic.length() > 0) {
                c.add("-i");
                c.add(backgroundMusic);
                c.add("-filter_complex");
                c.add("amix");
            }
        }
        // Enabled strict settings
        if (strictSetting.length() > 0) {
            c.add("-strict");
            c.add(strictSetting);
        }
        // Output
        c.add("-r");
        c.add("" + compositor.getFPS());
        if (videoEncoder.equals("gif")) {
            if (compositor.getWidth() <= 600) {
                c.add("-vf");
                c.add("flags=lanczos");
            } else {
                c.add("-vf");
                c.add("scale=600:-1:flags=lanczos");
            }
        }
        c.add("-vb");
        c.add(videoBitrate + "k");
        c.add("-pix_fmt");
        c.add("yuv420p");
        if (output.startsWith("rtmp://")) {
            c.add("-minrate");
            c.add("" + videoBitrate + "k");
            c.add("-maxrate");
            c.add("" + videoBitrate + "k ");
        }
        if (!videoEncoder.equals("gif")) {
            c.add("-ab");
            c.add("" + audioBitrate + "k");
            c.add("-ar");
            c.add("" + audioRate);
            c.add("-acodec");
            c.add("" + audioEncoder);
        }
        c.add("-vcodec");
        c.add(videoEncoder);

        if (preset.length() > 0) {
            c.add("-preset");
            c.add(preset);
        }
        if (output.endsWith(".m3u8")) {
            c.add("-flags");
            c.add("-global_header");
            c.add("-hls_time");
            c.add("10");
            c.add("-hls_wrap");
            c.add("6");
        }
        c.add("-g");
        c.add("" + (compositor.getFPS() * 2));
        c.add("-y");
        c.add("-f");
        c.add(muxer);
        c.add(output);
        return c.toArray(new String[c.size()]);
    }

    public String getBin() {
        return bin + " " + nonVerboseMode;
    }

    public String getDesktopFormat() {
        return desktopFormat;
    }

    public String getWebcamFormat() {
        return webcamFormat;
    }

    private void initDefaults() {

        InputStream in = null;
        if (Screen.isOSX()) {
            in = FFMpeg.class.getResourceAsStream("/screenstudio/encoder/osx.properties");
        } else if (Screen.isWindows()) {
            in = FFMpeg.class.getResourceAsStream("/screenstudio/encoder/windows.properties");
        } else {
            in = FFMpeg.class.getResourceAsStream("/screenstudio/encoder/default.properties");
        }

        try {
            Properties p = new Properties();
            p.load(in);
            bin = p.getProperty("BIN", "ffmpeg");
            nonVerboseMode = p.getProperty("NONVERBOSEMODE", " -nostats -loglevel 0 ");
            //Main input
            desktopFormat = p.getProperty("DESKTOPFORMAT", "x11grab");
            webcamFormat = p.getProperty("WEBCAMFORMAT", "v4l2");
            // Audio
            audioInput = p.getProperty("DEFAULTAUDIO", "default");
            audioFormat = p.getProperty("AUDIOFORMAT", "pulse");
            //Output
            strictSetting = p.getProperty("STRICTSETTINGS", "-2");
            //HOME
            mHome = new File(System.getProperty("user.home"));
            mThreading = p.getProperty("THREADING", mThreading);
            in.close();

        } catch (MalformedURLException ex) {
            this.state = RunningState.Error;
            this.lastErrorMessage = ex.getMessage();
            Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            this.state = RunningState.Error;
            this.lastErrorMessage = ex.getMessage();
            Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void run() {
        mStopMe = false;
        state = RunningState.Starting;
        mDebugMode = true;
        try {
            System.out.println("Starting encoder...");
            while (!compositor.isReady()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            Pipe pipe = new Pipe();
            new Thread(pipe).start();
            String[] commands = getCommands(pipe.getServer());
            for (String c : commands) {
                System.out.print(c + " ");
            }
            System.out.println();
            Process p = Runtime.getRuntime().exec(commands);
            OutputStream out = p.getOutputStream();
            InputStream in = p.getErrorStream();
            new Thread(new ProcessReader(in)).start();
            long frameTime = (1000000000 / compositor.getFPS());
            long nextPTS = System.nanoTime() + frameTime;
            state = RunningState.Running;
            System.out.println("Starting encoding...");
            while (!mStopMe) {
                try {
                    pipe.write(compositor.getData());
                } catch (Exception exWrite) {
                    System.err.println("Exception while writing...  " + exWrite.getMessage());
                    this.lastErrorMessage = exWrite.getMessage();
                    state = RunningState.Error;
                    mStopMe = true;
                }
                long wait = nextPTS - System.nanoTime();
                nextPTS += frameTime;
                if (wait > 0) {
                    try {
                        Thread.sleep(wait / 1000000, (int) (wait % 1000000));
                    } catch (Exception ex) {
                        System.err.println("Error: Thread.sleep(" + (wait / 1000000) + "," + ((int) (wait % 1000000)) + ")");
                    }
                }
            }
            System.out.println("Exiting encoder...");
            System.out.println("Status : " + state.toString());
            in.close();
            pipe.close();
            out.write("q\n".getBytes());
            out.close();
            out = null;
            try {
                p.waitFor(15, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
            }
            compositor.stop();
            p.destroy();
            p = null;
            if (state == RunningState.Running) {
                state = RunningState.Stopped;
            }

        } catch (IOException ex) {
            compositor.stop();
            state = RunningState.Error;
            lastErrorMessage = ex.getMessage();
            Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public enum FORMATS {
        TS,
        FLV,
        MOV,
        MP4,
        GIF,
        HITBOX,
        TWITCH,
        USTREAM,
        VAUGHNLIVE,
        YOUTUBE,
        FACEBOOK,
        RTMP,
        HTTP,
        BROADCAST,
    }

    public static boolean isRTMP(FORMATS f) {
        switch (f) {
            case HITBOX:
            case RTMP:
            case TWITCH:
            case USTREAM:
            case VAUGHNLIVE:
            case YOUTUBE:
            case FACEBOOK:
                return true;
            default:
                return false;
        }
    }

    public static String[] getServerList(FORMATS format) {
        String[] list = new String[0];
        switch (format) {
            case HITBOX:
            case TWITCH:
            case VAUGHNLIVE:
            case YOUTUBE:
            case FACEBOOK:
                Properties p = new Properties();
                InputStream in;
                try {
                    java.util.ArrayList<String> l = new java.util.ArrayList<>();
                    in = FFMpeg.class.getResourceAsStream("/screenstudio/targets/rtmp/" + format.name() + ".properties");
                    p.load(in);
                    in.close();
                    p.values().stream().forEach((server) -> {
                        l.add(server.toString());
                    });
                    list = l.toArray(list);
                    java.util.Arrays.sort(list);
                } catch (MalformedURLException ex) {
                    Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;

        }
        return list;
    }

}
