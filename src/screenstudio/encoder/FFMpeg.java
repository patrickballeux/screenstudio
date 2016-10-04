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
package screenstudio.encoder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.sources.Compositor;
import screenstudio.sources.Screen;

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
    public void setAudio(AudioRate rate, String input, Float offset) {
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
            mITSOffset = " -itsoffset " + offset.toString() + " ";
        } else {
            mITSOffset = "";
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
    public void setOutputFormat(FORMATS format, Presets p, int videoBitrate, String server, String key, File outputFolder) {
        preset = p.name();
        switch (format) {
            case FLV:
                muxer = "flv";
                videoEncoder = "libx264";
                audioEncoder = "libmp3lame";
                output = new File(outputFolder, generateRandomName() + ".flv").getAbsolutePath();
                break;
            case MP4:
                muxer = "mp4";
                videoEncoder = "libx264";
                audioEncoder = "libmp3lame";
                output = new File(outputFolder, generateRandomName() + ".mp4").getAbsolutePath();
                break;
            case MOV:
                muxer = "mov";
                videoEncoder = "libx264";
                audioEncoder = "libmp3lame";
                output = new File(outputFolder, generateRandomName() + ".mov").getAbsolutePath();
                break;
            case TS:
                preset = "";
                muxer = "mpegts";
                videoEncoder = "mpeg2video";
                audioEncoder = "mp2";
                output = new File(outputFolder, generateRandomName() + ".ts").getAbsolutePath();
                break;
            case GIF:
                muxer = "gif";
                videoEncoder = "gif";
                audioEncoder = "libmp3lame";
                preset = "";
                String randomName = generateRandomName();
                output = new File(outputFolder, randomName + ".gif").getAbsolutePath() + " -f mp3 " + new File(outputFolder, randomName + ".mp3").getAbsolutePath();
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

    /**
     * Build the complete FFMpeg command from this object instance
     *
     * @param debugMode : If enabled, verbose mode is activated
     * @return the full command for FFMpeg
     */
    private String getCommand() {
        StringBuilder c = new StringBuilder();
        // Add binary path
        c.append(bin);
        // Enable debug mode
        if (!mDebugMode) {
            c.append(nonVerboseMode);
        }
        //compositor
        c.append(" ").append(mThreading).append(" -f ").append(compositorFormat);
        c.append(" -framerate ").append(compositor.getFPS());
        c.append(" -video_size ").append(compositor.getWidth()).append("x").append(compositor.getHeight());
        c.append(" -i - ");

        // Capture Audio
        c.append(" -f ").append(audioFormat).append(mITSOffset).append(" -i ").append(audioInput);
        // Enabled strict settings
        if (strictSetting.length() > 0) {
            c.append(" -strict ").append(strictSetting);
        }
        // Output
        c.append(" -r ").append(compositor.getFPS());
        if (videoEncoder.equals("gif")) {
            if (compositor.getWidth() < 800){
                c.append(" -vf flags=lanczos ");
            } else {
                c.append(" -vf scale=800:-1:flags=lanczos ");
            }
        } 
//        else {
//            c.append(" -s ").append(compositor.getWidth()).append("x").append(compositor.getHeight());
//        }
        c.append(" -vb ").append(videoBitrate).append("k");
        c.append(" -pix_fmt yuv420p ");
        if (output.startsWith("rtmp://")) {
            c.append(" -minrate ").append(videoBitrate).append("k -maxrate ").append(videoBitrate).append("k ");
        }
        c.append(" -ab ").append(audioBitrate).append("k").append(" -ar ").append(audioRate);
        c.append(" -vcodec ").append(videoEncoder);
        c.append(" -acodec ").append(audioEncoder);
        if (preset.length() > 0) {
            c.append(" -preset ").append(preset);
        }
        String buffer = " -g " + (compositor.getFPS() * 2);
        c.append(buffer).append(" -f ").append(muxer).append(" ");
        c.append(output);
        // Set proper output
        //if (mDebugMode) {
        System.out.println(c.toString());
        //}
        return c.toString();
    }

    public String getBin() {
        return bin + nonVerboseMode;
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
        }else {
            in = FFMpeg.class.getResourceAsStream("/screenstudio/encoder/default.properties");
        }

        try {
            Properties p = new Properties();
            p.load(in);
            bin = p.getProperty("BIN", "ffmpeg") + " ";
            nonVerboseMode = p.getProperty("NONVERBOSEMODE", " -nostats -loglevel 0 ") + " ";
            //Main input
            desktopFormat = p.getProperty("DESKTOPFORMAT", "x11grab") + " ";
            webcamFormat = p.getProperty("WEBCAMFORMAT", "v4l2");
            // Audio
            audioInput = p.getProperty("DEFAULTAUDIO", "default") + " ";
            audioFormat = p.getProperty("AUDIOFORMAT", "pulse") + " ";
            //Output
            strictSetting = p.getProperty("STRICTSETTINGS", "-2") + " ";
            //HOME
            mHome = new File(p.getProperty("HOME", ".").replaceAll("~", System.getProperty("user.home")));
            if (!mHome.exists()) {
                mHome.mkdirs();
            }
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
        new Thread(compositor).start();
        mDebugMode = true;
        try {
            String command = getCommand();
            System.out.println("Starting encoder...");
            while (!compositor.isReady()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            Process p = Runtime.getRuntime().exec(command);
            OutputStream out = p.getOutputStream();
            InputStream in = p.getErrorStream();
            new Thread(new ProcessReader(in)).start();
            long frameTime = (1000000000 / compositor.getFPS());
            long nextPTS = System.nanoTime() + frameTime;
            state = RunningState.Running;
            System.out.println("Starting encoding...");
            while (!mStopMe) {
                try {
                    out.write(compositor.getData());
//                    System.out.println("Data written " + System.currentTimeMillis());
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
            out.close();
            p.destroy();     
            p.destroyForcibly();
            p=null;
            if (state == RunningState.Running) {
                state = RunningState.Stopped;
            }

        } catch (IOException ex) {
            state = RunningState.Error;
            lastErrorMessage = ex.getMessage();
            Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
        }
        compositor.stop();
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
