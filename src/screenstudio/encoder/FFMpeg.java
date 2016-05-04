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

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import screenstudio.gui.overlays.PanelWebcam;
import screenstudio.sources.Overlay;
import screenstudio.sources.Screen;
import screenstudio.targets.SIZES;
import screenstudio.targets.Targets;
import screenstudio.targets.Targets.FORMATS;

/**
 *
 * @author patrick
 */
public class FFMpeg {

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

    private String bin = "ffmpeg  ";
    private String nonVerboseMode = " -nostats -loglevel 0 ";
    //Main inpu
    private String captureWidth = "720";
    private String captureHeight = "480";
    private String captureX = "0";
    private String captureY = "0";
    private String mainInput = ":0.0";
    private String mainFormat = "x11grab";
    //Overlay
    private String overlayInput = "";
    private String overlayFormat = "rawvideo -pix_fmt bgr24";
    private Overlay runningOverlay = null;
    // Audio
    private String audioRate = "44100";
    private String audioInput = "default";
    private String audioFormat = "pulse";

    //Output
    private String framerate = "10";
    private String videoBitrate = "9000";
    private String audioBitrate = "128";
    private String videoEncoder = "libx264";
    private String audioEncoder = "aac";
    private String muxer = "mp4";
    private String preset = "ultrafast";
    private String strictSetting = "-2";
    private String outputWidth = "720";
    private String outputHeight = "480";
    private File defaultCaptureFolder = new File(".");
    private String output = "Capture/capture.mp4";
    private File mHome = new File(".");
    private String mThreading = "";
    private File mWatermarkFile = null;

    private Rectangle overlaySetting = new Rectangle(0, 0);

    /**
     * Main class wrapper for FFMpeg
     */
    public FFMpeg() {
        //Creating default folder for capturing videos...
        defaultCaptureFolder = new File("Capture");
        initDefaults();
        if (!defaultCaptureFolder.exists()) {
            defaultCaptureFolder.mkdir();
        }
        output = new File(defaultCaptureFolder, "capture.flv").getAbsolutePath();
    }

    public File getHome() {
        return mHome;
    }

    public File getWaterMark() {
        return mWatermarkFile;
    }

    public void setWaterMark(File image) {
        mWatermarkFile = image;
    }

    /**
     * Set the audio parameters
     *
     * @param rate : Audio rate for the output
     * @param input : device to use
     */
    public void setAudio(AudioRate rate, String input) {
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
    }

    /**
     * Set the Overlay to use
     *
     * @param overlay
     */
    public void setOverlay(Overlay overlay) {
        runningOverlay = overlay;
        if (overlay == null) {
            overlayInput = "";
        } else {
            overlayInput = overlay.OutputURL();
            overlaySetting = new Rectangle(0, 0, (int) overlay.getSize().getWidth(), (int) overlay.getSize().getHeight());
        }
    }

    public Overlay getOverlay() {
        return runningOverlay;
    }

    /**
     * Set the capture format to use
     *
     * @param device : The display
     * @param capX : The left location
     * @param capY : The top location
     */
    public void setCaptureFormat(String device, int capX, int capY) {
        mainInput = device;
        captureX = String.valueOf(capX);
        captureY = String.valueOf(capY);
    }

    /**
     * Set the output format of the video file/stream
     *
     * @param format
     * @param target
     */
    public void setOutputFormat(FORMATS format, Targets target) {
        switch (format) {
            case FLV:
                muxer = "flv";
                videoEncoder = "libx264";
                audioEncoder = "libmp3lame";
                output = new File(defaultCaptureFolder, generateRandomName() + ".flv").getAbsolutePath();
                break;
            case MP4:
                muxer = "mp4";
                videoEncoder = "libx264";
                audioEncoder = "mp3";
                output = new File(defaultCaptureFolder, generateRandomName() + ".mp4").getAbsolutePath();
                break;
            case MOV:
                muxer = "mov";
                videoEncoder = "libx264";
                audioEncoder = "mp3";
                output = new File(defaultCaptureFolder, generateRandomName() + ".mov").getAbsolutePath();
                break;
            case TS:
                muxer = "mpegts";
                videoEncoder = "mpeg2video";
                audioEncoder = "mp2";
                output = new File(defaultCaptureFolder, generateRandomName() + ".ts").getAbsolutePath();
                break;
            case RTMP:
            case HITBOX:
            case TWITCH:
            case USTREAM:
            case VAUGHNLIVE:
            case YOUTUBE:
                muxer = "flv";
                videoEncoder = "libx264";
                audioEncoder = "aac";
                if (target.server.length() == 0) {
                    output = target.getKey(format);
                } else {
                    output = target.server + "/" + target.getKey(format);
                }
                break;
            case BROADCAST:
                muxer = "mpegts";
                videoEncoder = "mpeg2video";
                audioEncoder = "mp2";
                output = "udp://255.255.255.255:8888?broadcast=1";
                break;
        }
        preset = target.outputPreset;
        videoBitrate = target.outputVideoBitrate;
    }

    /**
     * Set the audio bitate
     *
     * @param rate
     */
    public void setAudioBitrate(int rate) {
        audioBitrate = String.valueOf(rate);
    }

    /**
     * Set the video bitrate
     *
     * @param rate
     */
    public void setVideoBitrate(int rate) {
        videoBitrate = String.valueOf(rate);
    }

    /**
     * Set the capture framerate for the display
     *
     * @param rate
     */
    public void setFramerate(int rate) {
        framerate = String.valueOf(rate);
    }

    /**
     * Set the preset to use for the encording
     *
     * @param p
     */
    public void setPreset(Presets p) {
        preset = p.name();
    }

    /**
     * Set the output size of the video/stream encoding
     *
     * @param capWidth
     * @param capHeight
     * @param size
     */
    public void setOutputSize(int capWidth, int capHeight, SIZES size, PanelWebcam.PanelLocation panelLocation) {
        captureWidth = String.valueOf(capWidth);
        captureHeight = String.valueOf(capHeight);
        if (overlayInput.length() > 0) {
            switch (panelLocation) {
                case Top:
                case Bottom:
                    capHeight += overlaySetting.getSize().getHeight();
                    break;
                case Left:
                case Right:
                    capWidth += overlaySetting.getSize().getWidth();
                    break;
            }
        }
        int calculatedWidth;
        switch (size) {
            case SOURCE:
                outputHeight = String.valueOf(capHeight);
                outputWidth = String.valueOf(capWidth);
                break;
            case OUT_240P:
                outputHeight = "240";
                calculatedWidth = (capWidth * 240 / capHeight);
                calculatedWidth += calculatedWidth % 2;
                outputWidth = String.valueOf(calculatedWidth);
                break;
            case OUT_360P:
                outputHeight = "360";
                calculatedWidth = (capWidth * 360 / capHeight);
                calculatedWidth += calculatedWidth % 2;
                outputWidth = String.valueOf(calculatedWidth);
                break;
            case OUT_480P:
                outputHeight = "480";
                calculatedWidth = (capWidth * 480 / capHeight);
                calculatedWidth += calculatedWidth % 2;
                outputWidth = String.valueOf(calculatedWidth);
                break;
            case OUT_720P:
                outputHeight = "720";
                calculatedWidth = (capWidth * 720 / capHeight);
                calculatedWidth += calculatedWidth % 2;
                outputWidth = String.valueOf(calculatedWidth);
                break;
            case OUT_1080P:
                outputHeight = "1080";
                calculatedWidth = (capWidth * 1080 / capHeight);
                calculatedWidth += calculatedWidth % 2;
                outputWidth = String.valueOf(calculatedWidth);
                break;
        }

    }

    /**
     * Set the output file to use
     *
     * @param out
     */
    public void setOutput(File out) {
        output = out.getAbsolutePath();
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
     * @param panelLocation
     * @param debugMode : If enabled, verbose mode is activated
     * @return the full command for FFMpeg
     */
    public String getCommand(PanelWebcam.PanelLocation panelLocation, boolean debugMode) {
        StringBuilder c = new StringBuilder();
        // Add binary path
        c.append(bin);
        // Enable debug mode
        if (!debugMode) {
            c.append(nonVerboseMode);
        }
        // Capture Desktop
        if (!Screen.isOSX()) {
            c.append(" -video_size ").append(captureWidth).append("x").append(captureHeight);
        }
        c.append(" -framerate ").append(framerate);
        c.append(" ").append(mThreading).append(" -f ").append(mainFormat).append(" -i ").append(mainInput);

        if (!Screen.isOSX()) {
            if (captureX.length() > 0) {
                c.append("+").append(captureX).append(",").append(captureY);
            }
        }
        // Capture Audio
        c.append(" -f ").append(audioFormat).append(" -i ").append(audioInput);
        // watermark
        if (mWatermarkFile != null) {
            c.append(" -f image2 -i ").append(mWatermarkFile.getAbsolutePath());
        }
        // Capture Overlay Panel
        if (overlayInput.length() > 0) {
            int w = (int) overlaySetting.getWidth();
            int h = (int) overlaySetting.getHeight();
            c.append(" ").append(mThreading).append(" -f ").append(overlayFormat);
            c.append(" -framerate ").append(framerate);
            c.append(" -video_size ").append(w).append("x").append(h);
            c.append(" -i ").append(overlayInput);

            switch (panelLocation) {
                case Top:
                    if (mWatermarkFile != null) {
                        c.append(" -filter_complex [0:v][2:v]overlay=0:main_h-overlay_h[pre];[pre]pad=iw:ih+").append(h).append(":0:").append(h).append("[desk];[desk][3:v]overlay=0:0");
                    } else {
                        c.append(" -filter_complex [0:v]pad=iw:ih+").append(h).append(":0:").append(h).append("[desk];[desk][2:v]overlay=0:0");
                    }
                    break;
                case Bottom:
                    if (mWatermarkFile != null) {
                        c.append(" -filter_complex [0:v][2:v]overlay=0:main_h-overlay_h[pre];[pre]pad=iw:ih+").append(h).append("[desk];[desk][3:v]overlay=0:main_h-overlay_h");
                    } else {
                        c.append(" -filter_complex [0:v]pad=iw:ih+").append(h).append("[desk];[desk][2:v]overlay=0:main_h-overlay_h");
                    }
                case Left:
                    if (mWatermarkFile != null) {
                        c.append(" -filter_complex [0:v][2:v]overlay=0:main_h-overlay_h[pre];[pre]pad=iw+").append(w).append(":ih:").append(w).append("[desk];[desk][3:v]overlay=0:0");
                    } else {
                        c.append(" -filter_complex [0:v]pad=iw+").append(w).append(":ih:").append(w).append("[desk];[desk][2:v]overlay=0:0");
                    }
                case Right:
                    if (mWatermarkFile != null) {
                        c.append(" -filter_complex [0:v][2:v]overlay=0:main_h-overlay_h[pre];[pre]pad=iw+").append(w).append(":ih[desk];[desk][3:v]overlay=main_w-overlay_w:0");
                    } else {
                        c.append(" -filter_complex [0:v]pad=iw+").append(w).append(":ih[desk];[desk][2:v]overlay=main_w-overlay_w:0");
                    }
            }
        } else if (mWatermarkFile != null) {
            c.append(" -filter_complex [0:v][2:v]overlay=0:main_h-overlay_h");
        }
        // Enabled strict settings
        if (strictSetting.length() > 0) {
            c.append(" -strict ").append(strictSetting);
        }
        // Output
        c.append(" -r ").append(framerate);
        c.append(" -s ").append(outputWidth).append("x").append(outputHeight);
        c.append(" -vb ").append(videoBitrate).append("k");
        if (output.startsWith("rtmp://")) {
            c.append(" -minrate ").append(videoBitrate).append("k -maxrate ").append(videoBitrate).append("k ");
        }
        c.append(" -ab ").append(audioBitrate).append("k").append(" -ar ").append(audioRate);
        c.append(" -vcodec ").append(videoEncoder);
        c.append(" -acodec ").append(audioEncoder);
        if (preset.length() > 0) {
            c.append(" -preset ").append(preset);
        }
        String buffer = " -g " + (new Integer(framerate) * 2);
        c.append(buffer).append(" -f ").append(muxer).append(" ");
        c.append(output);
        // Set proper output
        if (debugMode) {
            System.out.println(c.toString());
        }
        return c.toString();
    }

    private void initDefaults() {
        File folder = new File("FFMPEG");
        if (folder.exists()) {
            File file;
            if (Screen.isOSX()) {
                file = new File(folder, "osx.properties");
            } else {
                file = new File(folder, "default.properties");
            }
            if (file.exists()) {
                try {
                    Properties p = new Properties();
                    try (InputStream in = file.toURI().toURL().openStream()) {
                        p.load(in);
                    }
                    bin = p.getProperty("BIN", "ffmpeg") + " ";
                    nonVerboseMode = p.getProperty("NONVERBOSEMODE", " -nostats -loglevel 0 ") + " ";
                    //Main inpu
                    mainFormat = p.getProperty("DESKTOPFORMAT", "x11grab") + " ";
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
                    defaultCaptureFolder = new File(mHome, "Capture");
                    mThreading = p.getProperty("THREADING", mThreading);

                } catch (MalformedURLException ex) {
                    Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(FFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
    }
}
