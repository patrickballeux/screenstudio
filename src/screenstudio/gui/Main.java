/*
 * Copyright (C) 2016 Patrick Balleux
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
package screenstudio.gui;

import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.Provider;
import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.UnsupportedLookAndFeelException;
import screenstudio.Version;
import screenstudio.encoder.FFMpeg;
import screenstudio.gui.overlays.Renderer;
import screenstudio.sources.Microphone;
import screenstudio.sources.Overlay;
import screenstudio.sources.Screen;
import screenstudio.sources.TwitchAlerts;
import screenstudio.sources.Webcam;
import screenstudio.targets.SIZES;
import screenstudio.targets.Targets;
import screenstudio.targets.Targets.FORMATS;

/**
 *
 * @author patrick
 */
public class Main extends javax.swing.JFrame implements ItemListener, HotKeyListener {

    private Targets target = new Targets();
    private Overlay runningOverlay = null;
    private TrayIcon trayIcon = null;
    private long recordingTimestamp = 0;
    //Shortcut key handler
    private Provider keyShortcuts = null;
    private boolean isLoading = false;
    private File mConfig = null;
    private FFMpeg.AudioRate audioRate = FFMpeg.AudioRate.Audio22K;
    private TwitchAlerts mTwitchAlerts = null;

    /**
     * Creates new form Main
     *
     * @param config
     */
    public Main(File config) {
        initComponents();
        mConfig = config;
        isLoading = true;
        lblMessages.setText("Loading...");
        try {
            target.loadDefault(mConfig);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        initControls();
        updateCurrentConfigurationStatus();
        isLoading = false;
        lblMessages.setText("Welcome!");

        new Thread(() -> {
            //Check for a new version...
            if (Version.hasNewVersion()) {
                lblNotice.setText("A new version is available");
            }
        }).start();
        this.setLocationByPlatform(true);
        this.pack();
    }

    private void initControls() {
        this.setTitle("ScreenStudio " + Version.MAIN);
        try {
            this.setIconImage(javax.imageio.ImageIO.read(this.getClass().getResource("images/logo.png")));
        } catch (IOException ex) {
            lblMessages.setText("Error when loading icons: " + ex.getMessage());
        }

        cboTargets.removeAllItems();
        for (FORMATS f : FORMATS.values()) {
            cboTargets.addItem(f);
            if (f.name().equals(target.format)) {
                cboTargets.setSelectedItem(f);
            }
        }
        cboProfiles.removeAllItems();
        for (SIZES s : SIZES.values()) {
            cboProfiles.addItem(s);
            if (s.name().equals(target.size)) {
                cboProfiles.setSelectedItem(s);
            }
        }
        try {
            cboDisplays.removeAllItems();
            for (Screen s : Screen.getSources()) {
                cboDisplays.addItem(s);
                if (s.toString().equals(target.mainSource)) {
                    cboDisplays.setSelectedItem(s);
                    int x, y, w, h;
                    if (target.captureX.length() > 0) {
                        x = Integer.parseInt(target.captureX);
                        y = Integer.parseInt(target.captureY);
                        w = Integer.parseInt(target.captureWidth);
                        h = Integer.parseInt(target.captureHeight);
                        s.setFps(Integer.parseInt(target.framerate));
                        //s.setSize(new Rectangle(x, y, w, h));
                    }
                }
            }
        } catch (IOException | InterruptedException ex) {
            lblMessages.setText("Error when loading displays: " + ex.getMessage());
        }
        try {
            cboWebcams.removeAllItems();
            for (Webcam o : Webcam.getSources()) {
                cboWebcams.addItem(o);
                if (o.toString().equals(target.webcamDevice)) {
                    cboWebcams.setSelectedItem(o);
                    o.setWidth(Integer.parseInt(target.webcamWidth));
                    o.setHeight(Integer.parseInt(target.webcamHeight));
                    o.setOffset(Double.parseDouble(target.webcamOffset));
                }
                o.setLocation(Renderer.WebcamLocation.valueOf(target.webcamLocation));
            }

        } catch (IOException | InterruptedException ex) {
            lblMessages.setText("Error when loading webcams: " + ex.getMessage());
        }
        try {
            cboAudiosMicrophone.removeAllItems();
            cboAudiosInternal.removeAllItems();
            cboAudiosMicrophone.addItem(new Microphone());
            cboAudiosInternal.addItem(new Microphone());
            for (Microphone o : Microphone.getSources()) {
                if (o.getDescription().toLowerCase().contains("monitor")) {
                    cboAudiosInternal.addItem(o);
                } else {
                    cboAudiosMicrophone.addItem(o);
                }
                if (o.toString().equals(target.mainAudio)) {
                    cboAudiosMicrophone.setSelectedItem(o);
                }
                if (o.toString().equals(target.secondAudio)) {
                    cboAudiosInternal.setSelectedItem(o);
                }
            }
        } catch (IOException | InterruptedException ex) {
            lblMessages.setText("Error when loading audios: " + ex.getMessage());
        }
        cboOverlays.removeAllItems();
        popTrayIconPanelContent.removeAll();
        try {
            for (Object o : Overlay.getOverlays()) {
                cboOverlays.addItem(o);
                java.awt.CheckboxMenuItem menu = new CheckboxMenuItem(o.toString());
                menu.setActionCommand(((File) o).getAbsolutePath());
                menu.addItemListener(this);
                popTrayIconPanelContent.add(menu);
                if (o.toString().equals(target.mainOverlay)) {
                    cboOverlays.setSelectedItem(o);
                    menu.setState(true);
                }
            }
        } catch (IOException | HeadlessException ex) {
            lblMessages.setText("Error when loading overlays: " + ex.getMessage());
        }
        cboWaterMarks.removeAllItems();
        try {
            for (File o : Overlay.getWaterMarks()) {
                cboWaterMarks.addItem(o);
                if (o.toString().equals(target.waterMarkFile)) {
                    cboWaterMarks.setSelectedItem(o);
                }
            }
        } catch (IOException | HeadlessException ex) {
            lblMessages.setText("Error when loading watermarks: " + ex.getMessage());
        }

        if (SystemTray.isSupported() && trayIcon == null) {
            SystemTray tray = SystemTray.getSystemTray();
            try {
                BufferedImage img = new BufferedImage((int) tray.getTrayIconSize().getWidth(), (int) tray.getTrayIconSize().getHeight(), BufferedImage.OPAQUE);
                trayIcon = new TrayIcon(img, this.getTitle(), popTrayIcon);
                trayIcon.setImageAutoSize(false);
                tray.add(trayIcon);

                updateTrayIcon();
            } catch (AWTException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (trayIcon == null) {
            this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        }
        if (target.showDuration.length() > 0) {
            try {
                spinShowDurationTime.setValue(new Integer(target.showDuration));
            } catch (Exception ex) {
                lblMessages.setText("Error when parsing duration time: " + ex.getMessage());
            }
        }
        txtPanelContentText.setText(target.panelTextContent);
        if (target.shortcutCaptureKey.length() == 0) {
            target.shortcutCaptureKey = "control shift R";
        }
        chkShortcutCaptureControl.setSelected(target.shortcutCaptureKey.contains("control"));
        chkShortcutCaptureSHIFT.setSelected(target.shortcutCaptureKey.contains("shift"));
        cboShortcutCaptureKey.setSelectedItem(target.shortcutCaptureKey.toUpperCase().trim().substring(target.shortcutCaptureKey.length() - 1, target.shortcutCaptureKey.length()));
        chkShortcutPrivacyControl.setSelected(target.shortcutPrivacyKey.contains("control"));
        chkShortcutPrivacySHIFT.setSelected(target.shortcutPrivacyKey.contains("shift"));
        cboShortcutPrivacyKey.setSelectedItem(target.shortcutPrivacyKey.toUpperCase().trim().substring(target.shortcutPrivacyKey.length() - 1, target.shortcutPrivacyKey.length()));
        System.out.println("Shortcut Key in use: " + target.shortcutCaptureKey.trim());
        System.out.println("Shortcut Key in use: " + target.shortcutPrivacyKey.trim());
        initializeShortCuts();

        chkDoNotHide.setSelected(target.doNotHide.equals("true"));

        cboAudioRate.removeAllItems();
        for (FFMpeg.AudioRate o : FFMpeg.AudioRate.values()) {
            cboAudioRate.addItem(o);
            if (o.name().equals(target.outputAudioRate)) {
                cboAudioRate.setSelectedItem(o);
                audioRate = o;
            }
        }
        for (int i = 0; i < cboPanelOrientation.getItemCount(); i++) {
            if (cboPanelOrientation.getItemAt(i).toString().equals(target.mainOverlayLocation)) {
                cboPanelOrientation.setSelectedIndex(i);
                break;
            }
        }
        txtCommand.setText(target.command);
        spinPanelSize.setValue(new Integer(target.mainOverlaySize));
        txtTwitchAlertFolder.setText(target.twitchalertsfolder);
    }

    private void initializeShortCuts() {
        final Main instance = this;
        new Thread(() -> {
            try {
                if (keyShortcuts == null) {
                    keyShortcuts = Provider.getCurrentProvider(false);
                }
                keyShortcuts.reset();
                keyShortcuts.register(KeyStroke.getKeyStroke(target.shortcutCaptureKey), instance);
                keyShortcuts.register(KeyStroke.getKeyStroke(target.shortcutPrivacyKey), instance);
            } catch (Exception ex) {
                keyShortcuts = null;
            }
        }).start();

    }

    private void stopShortcuts() {
        new Thread(() -> {
            try {
                if (keyShortcuts != null) {
                    keyShortcuts.reset();
                    keyShortcuts.stop();
                }

            } catch (Exception ex) {
                keyShortcuts = null;
            }
        }).start();
    }

    private void setPanelContent(File file) {
        if (runningOverlay != null) {
            runningOverlay.setUserTextContent(txtPanelContentText.getText());
            runningOverlay.setContent(file);
        }
    }

    private void updateTrayIcon() {
        if (trayIcon != null) {
            long delta = (System.currentTimeMillis() - recordingTimestamp) / 60000; //In minutes
            Image img = trayIcon.getImage();
            Graphics2D g = (Graphics2D) img.getGraphics();
            if (this.processRunning) {
                if (runningOverlay != null && runningOverlay.isPrivateMode()) {
                    g.setBackground(Color.RED);
                } else {
                    g.setBackground(Color.GREEN);
                }
                g.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(
                        RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                g.clearRect(0, 0, img.getWidth(null), img.getHeight(null));
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(2));
                g.drawRect(1, 1, img.getWidth(null) - 2, img.getHeight(null) - 2);
                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, img.getHeight(null) / 3));
                String time = (delta) + "";
                int x = (img.getWidth(null) / 2) - (g.getFontMetrics(g.getFont()).stringWidth(time) / 2);
                g.drawString(time, x, (img.getHeight(null) / 2));
                time = "MIN";
                x = (img.getWidth(null) / 2) - (g.getFontMetrics(g.getFont()).stringWidth(time) / 2);
                g.drawString(time, x, img.getHeight(null) - 4);
                g.dispose();
                trayIcon.setImage(img);
                if (runningOverlay == null) {
                    this.trayIcon.setToolTip("Recording Time: " + delta + " minutes...");
                } else {
                    this.trayIcon.setToolTip("Recording Time: " + delta + " minutes...\nNotifications on port UDP:" + runningOverlay.getNotificationPort());
                }
            } else {
                g.drawImage(this.getIconImage().getScaledInstance(img.getWidth(null), img.getHeight(null), Image.SCALE_FAST), 0, 0, null);
                g.dispose();
                trayIcon.setImage(img);
                this.trayIcon.setToolTip("Stopped");
            }
        }
    }

    private void updateCurrentConfigurationStatus() {
        String text = "<HTML>";
        if (cboTargets.getSelectedItem() != null && cboTargets.getSelectedItem() instanceof FORMATS) {
            btnSetTarget.setEnabled(Targets.isRTMP((FORMATS) cboTargets.getSelectedItem()));
            text += "<B>Format:</B> " + cboTargets.getSelectedItem().toString() + "<BR>";
            if (Targets.isRTMP((FORMATS) cboTargets.getSelectedItem())) {
                text += "<B>RTMP Server:</B> " + target.server + "<BR>";
                if (target.getKey((FORMATS) cboTargets.getSelectedItem()).length() == 0) {
                    text += "<font color=red><B>Warning:</B> Secret key not set</font><BR>";
                }
            }
            if ((FORMATS) cboTargets.getSelectedItem() == FORMATS.BROADCAST) {
                text += "<font color=red><B>Multicast:</B> udp://255.255.255.255:8888</font><BR>";
            }
        }
        if (cboProfiles.getSelectedItem() != null) {
            text += "<B>Size:</B> " + cboProfiles.getSelectedItem().toString() + " (bitrate: " + target.outputVideoBitrate + ")<BR>";
        }
        if (cboDisplays.getSelectedItem() != null && cboDisplays.getSelectedItem() instanceof Screen) {
            text += "<B>Display:</B> " + ((Screen) cboDisplays.getSelectedItem()).getDetailledLabel() + "<BR>";
        }
        if (cboWebcams.getSelectedItem() != null) {
            text += "<B>Webcam:</B> " + cboWebcams.getSelectedItem().toString() + "<BR>";
        }
        if (cboAudiosMicrophone.getSelectedItem() != null) {
            text += "<B>Microphone:</B> " + cboAudiosMicrophone.getSelectedItem().toString() + "<BR>";
        }
        if (cboAudiosInternal.getSelectedItem() != null) {
            text += "<B>Internal:</B> " + cboAudiosInternal.getSelectedItem().toString() + "<BR>";
        }
        if (cboOverlays.getSelectedItem() != null) {
            text += "<B>Panel:</B> " + cboOverlays.getSelectedItem().toString() + "<BR>";
        }
        if (new File(new FFMpeg().getHome() + "/Overlays", "privacy.png").exists()) {
            text += "<B>Privacy file found...</B> <BR>";
        }
        text += "</HTML>";
        lblCurrentTargetConfiguration.setText(text);

    }

    private FFMpeg getCommand() throws IOException, InterruptedException {
        FFMpeg command = new FFMpeg();
        Microphone m = (Microphone) cboAudiosMicrophone.getSelectedItem();
        Microphone i = (Microphone) cboAudiosInternal.getSelectedItem();
        if (m.getDescription().equals("None")) {
            m = null;
        }
        if (i.getDescription().equals("None")) {
            i = null;
        }
        command.setAudio(audioRate, Microphone.getVirtualAudio(m, i));
        Screen s = (Screen) cboDisplays.getSelectedItem();
        command.setCaptureFormat(s.getId(), (int) s.getSize().getX(), (int) s.getSize().getY());
        command.setFramerate(s.getFps());
        command.setOutputFormat((FORMATS) cboTargets.getSelectedItem(), target);
        command.setPreset(FFMpeg.Presets.ultrafast);
        if (cboOverlays.getSelectedIndex() > 0) {
            File content = (File) cboOverlays.getSelectedItem();
            if (cboWebcams.getSelectedIndex() > 0) {
                Webcam w = (Webcam) cboWebcams.getSelectedItem();
                w.setFps(s.getFps());
                runningOverlay = new Overlay(content, (Renderer.PanelLocation) cboPanelOrientation.getSelectedItem(), (Integer) spinPanelSize.getValue(), s, w, (Integer) spinShowDurationTime.getValue(), txtPanelContentText.getText(), txtCommand.getText());
            } else {
                runningOverlay = new Overlay(content, (Renderer.PanelLocation) cboPanelOrientation.getSelectedItem(), (Integer) spinPanelSize.getValue(), s, null, (Integer) spinShowDurationTime.getValue(), txtPanelContentText.getText(), txtCommand.getText());
            }
            command.setOverlay(runningOverlay);
            command.setOutputSize((int) runningOverlay.getWidth(), (int) runningOverlay.getHeight(), (SIZES) cboProfiles.getSelectedItem());
        } else {
            command.setOutputSize((int) s.getSize().getWidth(), (int) s.getSize().getHeight(), (SIZES) cboProfiles.getSelectedItem());
        }
        if (cboWaterMarks.getSelectedIndex() > 0) {
            command.setWaterMark((File) cboWaterMarks.getSelectedItem());
        }
        return command;
    }

    private Process streamProcess = null;
    private boolean processRunning = false;

    private void startProcess(String command) {
        btnCapture.setText("Stop");
        popTrayIconRecord.setLabel("Stop recording");
        processRunning = true;
        System.out.println("Screen Studio 2");
        System.out.println("-----------------------");
        System.out.println("Started");
        recordingTimestamp = System.currentTimeMillis();
        try {
            //updateControls(true);
            streamProcess = Runtime.getRuntime().exec(command);
            if (runningOverlay != null && cboTargets.getSelectedItem().equals(Targets.FORMATS.TWITCH)) {
                File tFolder = new File(txtTwitchAlertFolder.getText());
                if (tFolder.exists()) {
                    mTwitchAlerts = new TwitchAlerts(tFolder, runningOverlay.getNotificationPort());
                }
            }
            new Thread(this::updateStatus).start();
            new Thread(this::monitorProcess).start();
        } catch (IOException ex) {
            lblMessages.setText(ex.getMessage());
        }

    }

    private void stopStream(String message) {
        btnCapture.setText("Capture");
        popTrayIconRecord.setLabel(btnCapture.getText());
        if (streamProcess != null) {
            try {
                try (OutputStream out = streamProcess.getOutputStream()) {
                    out.write("q".getBytes());
                    out.flush();
                }
                streamProcess.waitFor(30, TimeUnit.SECONDS);
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            streamProcess.destroy();
            streamProcess = null;

        }
        if (runningOverlay != null) {
            runningOverlay.stop();
            runningOverlay = null;
        }
        if (mTwitchAlerts != null) {
            mTwitchAlerts.stop();
            mTwitchAlerts = null;
        }
        while (processRunning) {
            //Waiting for the process to completly stop...
            try {
                Thread.sleep(100);
                Thread.yield();
            } catch (InterruptedException ex) {
            }
        }
        try {
            //unloading any virtual audio...
            Microphone.getVirtualAudio(null, null);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(message);
        lblMessages.setText(message);
        if (trayIcon == null || target.doNotHide.equals("true")) {
            this.setExtendedState(JFrame.NORMAL);
        } else {
            this.setVisible(true);
        }

    }

    private void updateStatus() {
        if (streamProcess != null) {
            BufferedReader reader = null;
            try {
                String line;
                reader = new BufferedReader(new InputStreamReader(streamProcess.getErrorStream()));
                line = reader.readLine();
                recordingTimestamp = System.currentTimeMillis();
                while (line != null) {
                    lblMessages.setText(line);
                    System.out.println(line);
                    if (streamProcess != null) {
                        line = reader.readLine();
                    } else {
                        line = null;
                    }
                }
                recordingTimestamp = 0;
            } catch (IOException ex) {

            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                        //
                    }
                }
            }

        }
        processRunning = false;
        recordingTimestamp = 0;
        updateTrayIcon();
    }

    private void monitorProcess() {
        while (streamProcess != null) {
            try {
                lblMessages.setText("Recording...");
                updateTrayIcon();
                System.out.println("Exit Code: " + streamProcess.exitValue());
                stopStream("An error occured...");
            } catch (Exception ex) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex1) {
                    //Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popTrayIcon = new java.awt.PopupMenu();
        popTrayIconPrivacyMode = new java.awt.CheckboxMenuItem();
        popTrayIconPanelContent = new java.awt.Menu();
        popTrayIconRecord = new java.awt.MenuItem();
        popTrayIconExit = new java.awt.MenuItem();
        tabs = new javax.swing.JTabbedPane();
        panCapture = new javax.swing.JPanel();
        cboTargets = new javax.swing.JComboBox();
        lblTargets = new javax.swing.JLabel();
        lblProfiles = new javax.swing.JLabel();
        cboProfiles = new javax.swing.JComboBox();
        btnCapture = new javax.swing.JButton();
        btnSetTarget = new javax.swing.JButton();
        btnSetProfile = new javax.swing.JButton();
        lblCurrentTargetConfiguration = new javax.swing.JLabel();
        chkDoNotHide = new javax.swing.JCheckBox();
        chkDebugMode = new javax.swing.JCheckBox();
        panSources = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        cboDisplays = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        cboWebcams = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        cboAudiosMicrophone = new javax.swing.JComboBox();
        btnSetDisplay = new javax.swing.JButton();
        btnSetWebcam = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        cboAudiosInternal = new javax.swing.JComboBox();
        jLabel11 = new javax.swing.JLabel();
        cboAudioRate = new javax.swing.JComboBox();
        jLabel13 = new javax.swing.JLabel();
        cboWaterMarks = new javax.swing.JComboBox<>();
        panPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        cboOverlays = new javax.swing.JComboBox();
        spinShowDurationTime = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        scrollPanelContentText = new javax.swing.JScrollPane();
        txtPanelContentText = new javax.swing.JTextArea();
        btnEditor = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        cboPanelOrientation = new javax.swing.JComboBox<>();
        jLabel12 = new javax.swing.JLabel();
        txtCommand = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        spinPanelSize = new javax.swing.JSpinner();
        panShortcuts = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        chkShortcutCaptureControl = new javax.swing.JCheckBox();
        chkShortcutCaptureSHIFT = new javax.swing.JCheckBox();
        cboShortcutCaptureKey = new javax.swing.JComboBox();
        btnShortcutCaptureApply = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        chkShortcutPrivacyControl = new javax.swing.JCheckBox();
        chkShortcutPrivacySHIFT = new javax.swing.JCheckBox();
        cboShortcutPrivacyKey = new javax.swing.JComboBox();
        btnShortcutPrivacyApply = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        txtTwitchAlertFolder = new javax.swing.JTextField();
        panStatusBar = new javax.swing.JPanel();
        lblMessages = new javax.swing.JLabel();
        lblNotice = new javax.swing.JLabel();

        popTrayIcon.setLabel("ScreenStudio");

        popTrayIconPrivacyMode.setLabel("Privacy Mode");
        popTrayIconPrivacyMode.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                popTrayIconPrivacyModeItemStateChanged(evt);
            }
        });
        popTrayIcon.add(popTrayIconPrivacyMode);

        popTrayIconPanelContent.setLabel("Panel Content");
        popTrayIcon.add(popTrayIconPanelContent);
        popTrayIcon.addSeparator();
        popTrayIconRecord.setActionCommand("Capture");
        popTrayIconRecord.setLabel("Capture");
        popTrayIconRecord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popTrayIconRecordActionPerformed(evt);
            }
        });
        popTrayIcon.add(popTrayIconRecord);

        popTrayIconExit.setLabel("Exit");
        popTrayIconExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popTrayIconExitActionPerformed(evt);
            }
        });
        popTrayIcon.add(popTrayIconExit);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setFont(new java.awt.Font("Nimbus Roman No9 L", 0, 10)); // NOI18N
        setMinimumSize(new java.awt.Dimension(450, 425));
        setPreferredSize(new java.awt.Dimension(450, 425));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        cboTargets.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboTargetsActionPerformed(evt);
            }
        });

        lblTargets.setText("Target");

        lblProfiles.setText("Profile");

        cboProfiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboProfilesActionPerformed(evt);
            }
        });

        btnCapture.setText("Capture");
        btnCapture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCaptureActionPerformed(evt);
            }
        });

        btnSetTarget.setText("...");
        btnSetTarget.setToolTipText("Set RTMP server and secret key");
        btnSetTarget.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetTargetActionPerformed(evt);
            }
        });

        btnSetProfile.setText("...");
        btnSetProfile.setToolTipText("Set bitrate and encording preset");
        btnSetProfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetProfileActionPerformed(evt);
            }
        });

        lblCurrentTargetConfiguration.setText("Loading...");
        lblCurrentTargetConfiguration.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblCurrentTargetConfiguration.setAutoscrolls(true);
        lblCurrentTargetConfiguration.setBorder(javax.swing.BorderFactory.createTitledBorder("Configuration"));

        chkDoNotHide.setText("Do not hide");
        chkDoNotHide.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkDoNotHideActionPerformed(evt);
            }
        });

        chkDebugMode.setText("Debug Mode");

        javax.swing.GroupLayout panCaptureLayout = new javax.swing.GroupLayout(panCapture);
        panCapture.setLayout(panCaptureLayout);
        panCaptureLayout.setHorizontalGroup(
            panCaptureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panCaptureLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panCaptureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblCurrentTargetConfiguration, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panCaptureLayout.createSequentialGroup()
                        .addComponent(lblTargets)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cboTargets, 0, 266, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSetTarget))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panCaptureLayout.createSequentialGroup()
                        .addComponent(chkDoNotHide)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkDebugMode)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnCapture))
                    .addGroup(panCaptureLayout.createSequentialGroup()
                        .addComponent(lblProfiles)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cboProfiles, 0, 266, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSetProfile)
                        .addGap(1, 1, 1)))
                .addContainerGap())
        );
        panCaptureLayout.setVerticalGroup(
            panCaptureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panCaptureLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panCaptureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(panCaptureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cboTargets)
                        .addComponent(lblTargets))
                    .addComponent(btnSetTarget, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panCaptureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblProfiles)
                    .addComponent(cboProfiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSetProfile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblCurrentTargetConfiguration, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panCaptureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnCapture)
                    .addGroup(panCaptureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(chkDoNotHide)
                        .addComponent(chkDebugMode)))
                .addContainerGap())
        );

        tabs.addTab("Targets", panCapture);

        jLabel2.setText("Display");

        cboDisplays.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboDisplaysActionPerformed(evt);
            }
        });

        jLabel3.setText("Webcam");

        cboWebcams.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboWebcamsActionPerformed(evt);
            }
        });

        jLabel4.setText("Microphone");

        cboAudiosMicrophone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboAudiosMicrophoneActionPerformed(evt);
            }
        });

        btnSetDisplay.setText("...");
        btnSetDisplay.setToolTipText("Set framerate capture");
        btnSetDisplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetDisplayActionPerformed(evt);
            }
        });

        btnSetWebcam.setText("...");
        btnSetWebcam.setToolTipText("Set webcam capture size");
        btnSetWebcam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetWebcamActionPerformed(evt);
            }
        });

        jLabel7.setText("Internal");

        cboAudiosInternal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboAudiosInternalActionPerformed(evt);
            }
        });

        jLabel11.setText("Audio Rate");

        cboAudioRate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboAudioRateActionPerformed(evt);
            }
        });

        jLabel13.setText("Watermark");

        javax.swing.GroupLayout panSourcesLayout = new javax.swing.GroupLayout(panSources);
        panSources.setLayout(panSourcesLayout);
        panSourcesLayout.setHorizontalGroup(
            panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSourcesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel13))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panSourcesLayout.createSequentialGroup()
                        .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(cboWaterMarks, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cboAudiosMicrophone, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cboAudiosInternal, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cboDisplays, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cboWebcams, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnSetWebcam, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnSetDisplay, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(panSourcesLayout.createSequentialGroup()
                        .addComponent(cboAudioRate, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 127, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panSourcesLayout.setVerticalGroup(
            panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSourcesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(cboDisplays, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSetDisplay))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(cboWebcams, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSetWebcam))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(cboAudiosMicrophone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(cboAudiosInternal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(cboAudioRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panSourcesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(cboWaterMarks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(144, Short.MAX_VALUE))
        );

        tabs.addTab("Sources", panSources);

        jLabel5.setText("Panel");

        cboOverlays.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboOverlaysActionPerformed(evt);
            }
        });

        spinShowDurationTime.setModel(new javax.swing.SpinnerNumberModel(30, 0, null, 15));

        jLabel1.setText("Duration");

        jLabel8.setText("minutes");

        scrollPanelContentText.setBorder(javax.swing.BorderFactory.createTitledBorder("@TEXT"));

        txtPanelContentText.setColumns(20);
        txtPanelContentText.setFont(new java.awt.Font("Ubuntu", 0, 12)); // NOI18N
        txtPanelContentText.setLineWrap(true);
        txtPanelContentText.setRows(5);
        txtPanelContentText.setWrapStyleWord(true);
        txtPanelContentText.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        txtPanelContentText.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtPanelContentTextKeyTyped(evt);
            }
        });
        scrollPanelContentText.setViewportView(txtPanelContentText);

        btnEditor.setText("Editor");
        btnEditor.setToolTipText("Launch internal HTML editor");
        btnEditor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditorActionPerformed(evt);
            }
        });

        jLabel6.setText("Orientation");

        cboPanelOrientation.setModel(new javax.swing.DefaultComboBoxModel<Renderer.PanelLocation>(Renderer.PanelLocation.values())
        );

        jLabel12.setText("@COMMAND");

        jLabel14.setText("Size");

        spinPanelSize.setModel(new javax.swing.SpinnerNumberModel(320, 0, null, 10));
        spinPanelSize.setToolTipText("Size of the panel when no webcam is selected...");

        javax.swing.GroupLayout panPanelLayout = new javax.swing.GroupLayout(panPanel);
        panPanel.setLayout(panPanelLayout);
        panPanelLayout.setHorizontalGroup(
            panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPanelContentText)
                    .addGroup(panPanelLayout.createSequentialGroup()
                        .addGroup(panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE)
                            .addGroup(panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE))
                                .addComponent(jLabel6)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panPanelLayout.createSequentialGroup()
                                .addGroup(panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panPanelLayout.createSequentialGroup()
                                        .addComponent(cboOverlays, 0, 181, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnEditor))
                                    .addGroup(panPanelLayout.createSequentialGroup()
                                        .addComponent(cboPanelOrientation, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel14)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinPanelSize, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(3, 3, 3))
                            .addGroup(panPanelLayout.createSequentialGroup()
                                .addComponent(spinShowDurationTime, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel8)
                                .addGap(0, 98, Short.MAX_VALUE))
                            .addComponent(txtCommand))))
                .addContainerGap())
        );
        panPanelLayout.setVerticalGroup(
            panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(cboOverlays, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEditor))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(cboPanelOrientation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14)
                    .addComponent(spinPanelSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(spinShowDurationTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(txtCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollPanelContentText, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabs.addTab("Panel", panPanel);

        jLabel9.setText("Capture");

        chkShortcutCaptureControl.setText("CTRL");

        chkShortcutCaptureSHIFT.setText("SHIFT");

        cboShortcutCaptureKey.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" }));

        btnShortcutCaptureApply.setText("Apply");
        btnShortcutCaptureApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShortcutCaptureApplyActionPerformed(evt);
            }
        });

        jLabel10.setText("Privacy");

        chkShortcutPrivacyControl.setText("CTRL");

        chkShortcutPrivacySHIFT.setText("SHIFT");

        cboShortcutPrivacyKey.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" }));

        btnShortcutPrivacyApply.setText("Apply");
        btnShortcutPrivacyApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShortcutPrivacyApplyActionPerformed(evt);
            }
        });

        jLabel15.setText("Twitch Alert Folder");

        txtTwitchAlertFolder.setText("~/twitchalerts");

        javax.swing.GroupLayout panShortcutsLayout = new javax.swing.GroupLayout(panShortcuts);
        panShortcuts.setLayout(panShortcutsLayout);
        panShortcutsLayout.setHorizontalGroup(
            panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panShortcutsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panShortcutsLayout.createSequentialGroup()
                            .addGroup(panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE)
                                .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(chkShortcutPrivacyControl)
                                .addComponent(chkShortcutCaptureControl, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(chkShortcutPrivacySHIFT)
                                .addComponent(chkShortcutCaptureSHIFT, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(cboShortcutPrivacyKey, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(cboShortcutCaptureKey, 0, 60, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(btnShortcutPrivacyApply, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnShortcutCaptureApply, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(panShortcutsLayout.createSequentialGroup()
                            .addComponent(jLabel15)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtTwitchAlertFolder))))
                .addContainerGap())
        );
        panShortcutsLayout.setVerticalGroup(
            panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panShortcutsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(chkShortcutCaptureSHIFT)
                    .addComponent(cboShortcutCaptureKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnShortcutCaptureApply)
                    .addComponent(chkShortcutCaptureControl))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(chkShortcutPrivacyControl)
                    .addComponent(chkShortcutPrivacySHIFT)
                    .addComponent(cboShortcutPrivacyKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnShortcutPrivacyApply))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panShortcutsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(txtTwitchAlertFolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(217, Short.MAX_VALUE))
        );

        tabs.addTab("Shortcuts", panShortcuts);

        getContentPane().add(tabs, java.awt.BorderLayout.CENTER);

        panStatusBar.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        panStatusBar.setLayout(new java.awt.GridLayout(1, 2, 0, 3));

        lblMessages.setText("Welcome");
        panStatusBar.add(lblMessages);

        lblNotice.setForeground(java.awt.Color.red);
        lblNotice.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        lblNotice.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblNoticeMouseClicked(evt);
            }
        });
        panStatusBar.add(lblNotice);

        getContentPane().add(panStatusBar, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCaptureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCaptureActionPerformed

        if (processRunning) {
            stopStream("Stopped...");
        } else {
            try {
                if (trayIcon == null || target.doNotHide.equals("true")) {
                    this.setExtendedState(JFrame.ICONIFIED);
                } else {
                    this.setVisible(false);
                }
                FFMpeg command = getCommand();
                if (command.getOverlay() != null) {
                    command.getOverlay().start();
                }
                startProcess(command.getCommand((Renderer.PanelLocation) cboPanelOrientation.getSelectedItem(), chkDebugMode.isSelected()));
            } catch (IOException | InterruptedException ex) {
                lblMessages.setText("An error occured: " + ex.getMessage());
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_btnCaptureActionPerformed

    private void cboTargetsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboTargetsActionPerformed
        if (!isLoading && cboTargets.getSelectedItem() != null && Targets.isRTMP((FORMATS) cboTargets.getSelectedItem())) {
            btnSetTargetActionPerformed(evt);
        }
        updateCurrentConfigurationStatus();
    }//GEN-LAST:event_cboTargetsActionPerformed

    private void cboProfilesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboProfilesActionPerformed
        updateCurrentConfigurationStatus();
    }//GEN-LAST:event_cboProfilesActionPerformed

    private void cboDisplaysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboDisplaysActionPerformed
        updateCurrentConfigurationStatus();
    }//GEN-LAST:event_cboDisplaysActionPerformed

    private void cboWebcamsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboWebcamsActionPerformed
        updateCurrentConfigurationStatus();
    }//GEN-LAST:event_cboWebcamsActionPerformed

    private void btnSetWebcamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetWebcamActionPerformed
        SetupWebcam dlg = new SetupWebcam((Webcam) cboWebcams.getSelectedItem(), this, true);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        dlg.dispose();
        target.webcamLocation = ((Webcam) cboWebcams.getSelectedItem()).getLocation().name();
        updateCurrentConfigurationStatus();
    }//GEN-LAST:event_btnSetWebcamActionPerformed

    private void btnSetDisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetDisplayActionPerformed
        SetupDisplay frm = new SetupDisplay((Screen) cboDisplays.getSelectedItem(), this, true);
        frm.setLocationRelativeTo(this);
        frm.setVisible(true);
        frm.dispose();
        updateCurrentConfigurationStatus();
    }//GEN-LAST:event_btnSetDisplayActionPerformed

    private void popTrayIconExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popTrayIconExitActionPerformed
        stopStream("Exiting");
        this.dispose();
        System.exit(0);
    }//GEN-LAST:event_popTrayIconExitActionPerformed

    private void popTrayIconRecordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popTrayIconRecordActionPerformed
        btnCapture.doClick();
    }//GEN-LAST:event_popTrayIconRecordActionPerformed

    private void btnSetTargetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetTargetActionPerformed
        if (cboTargets.getSelectedItem() != null) {
            FORMATS f = (FORMATS) cboTargets.getSelectedItem();
            SetupRTMP frm = new SetupRTMP(f, target, this, true);
            frm.setLocationRelativeTo(this);
            frm.setVisible(true);
            frm.dispose();
        }
        updateCurrentConfigurationStatus();
    }//GEN-LAST:event_btnSetTargetActionPerformed

    private void btnSetProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetProfileActionPerformed
        if (cboTargets.getSelectedItem() != null) {
            FORMATS f = (FORMATS) cboTargets.getSelectedItem();
            SetupProfile frm = new SetupProfile(target, this, true);
            frm.setLocationRelativeTo(this);
            frm.setVisible(true);
            frm.dispose();
        }
        updateCurrentConfigurationStatus();
    }//GEN-LAST:event_btnSetProfileActionPerformed

    private void cboAudiosInternalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboAudiosInternalActionPerformed
        updateCurrentConfigurationStatus();
    }//GEN-LAST:event_cboAudiosInternalActionPerformed

    private void cboOverlaysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboOverlaysActionPerformed
        updateCurrentConfigurationStatus();
    }//GEN-LAST:event_cboOverlaysActionPerformed

    private void cboAudiosMicrophoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboAudiosMicrophoneActionPerformed
        updateCurrentConfigurationStatus();
    }//GEN-LAST:event_cboAudiosMicrophoneActionPerformed

    private void txtPanelContentTextKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPanelContentTextKeyTyped
        if (runningOverlay != null) {
            runningOverlay.setUserTextContent(txtPanelContentText.getText());
        }
    }//GEN-LAST:event_txtPanelContentTextKeyTyped

    private void btnShortcutCaptureApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShortcutCaptureApplyActionPerformed
        if (target != null) {
            target.shortcutCaptureKey = "";
            if (chkShortcutCaptureControl.isSelected()) {
                target.shortcutCaptureKey += "control ";
            }
            if (chkShortcutCaptureSHIFT.isSelected()) {
                target.shortcutCaptureKey += "shift ";
            }
            target.shortcutCaptureKey += cboShortcutCaptureKey.getSelectedItem().toString();
            initializeShortCuts();
        }
    }//GEN-LAST:event_btnShortcutCaptureApplyActionPerformed

    private void lblNoticeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblNoticeMouseClicked
        if (evt.getClickCount() == 2) {
            if (lblNotice.getText().length() > 0 && Desktop.isDesktopSupported()) {
                try {
                    java.net.URL url = new java.net.URL("http://screenstudio.crombz.com");
                    Desktop.getDesktop().browse(url.toURI());
                } catch (MalformedURLException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException | URISyntaxException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }//GEN-LAST:event_lblNoticeMouseClicked

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        try {
            target.format = cboTargets.getSelectedItem().toString();
            Screen s = (Screen) cboDisplays.getSelectedItem();
            target.captureX = "" + (int) s.getSize().getX();
            target.captureY = "" + (int) s.getSize().getY();
            target.captureWidth = "" + (int) s.getSize().getWidth();
            target.captureHeight = "" + (int) s.getSize().getHeight();
            target.framerate = "" + s.getFps();
            target.size = ((SIZES) cboProfiles.getSelectedItem()).name();
            target.mainSource = cboDisplays.getSelectedItem().toString();
            target.mainAudio = cboAudiosMicrophone.getSelectedItem().toString();
            target.secondAudio = cboAudiosInternal.getSelectedItem().toString();
            target.showDuration = spinShowDurationTime.getValue().toString();
            if (cboWebcams.getSelectedIndex() > 0) {
                Webcam w = (Webcam) cboWebcams.getSelectedItem();
                target.webcamDevice = w.toString();
                target.webcamWidth = "" + w.getWidth();
                target.webcamHeight = "" + w.getHeight();
                target.webcamOffset = "" + w.getOffset();
            } else {
                target.webcamDevice = "";
            }
            if (cboOverlays.getSelectedItem() != null) {
                target.mainOverlay = cboOverlays.getSelectedItem().toString();
            }
            target.panelTextContent = txtPanelContentText.getText();
            target.outputAudioRate = audioRate.name();
            target.mainOverlayLocation = cboPanelOrientation.getSelectedItem().toString();
            target.command = txtCommand.getText();
            target.waterMarkFile = cboWaterMarks.getSelectedItem().toString();
            target.mainOverlaySize = spinPanelSize.getValue().toString();
            target.twitchalertsfolder = txtTwitchAlertFolder.getText();
            target.saveDefault(mConfig);
            stopShortcuts();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_formWindowClosing

    private void chkDoNotHideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkDoNotHideActionPerformed
        if (chkDoNotHide.isSelected()) {
            target.doNotHide = "true";
        } else {
            target.doNotHide = "false";
        }
    }//GEN-LAST:event_chkDoNotHideActionPerformed

    private void cboAudioRateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboAudioRateActionPerformed
        audioRate = (FFMpeg.AudioRate) cboAudioRate.getSelectedItem();
    }//GEN-LAST:event_cboAudioRateActionPerformed

    private void btnEditorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditorActionPerformed
        if (cboOverlays.getSelectedIndex() > 0) {
            new screenstudio.panel.editor.Editor((File) cboOverlays.getSelectedItem(), txtPanelContentText.getText(), 320).setVisible(true);
        } else {
            new screenstudio.panel.editor.Editor((File) null, txtPanelContentText.getText(), 320).setVisible(true);
        }
    }//GEN-LAST:event_btnEditorActionPerformed

    private void btnShortcutPrivacyApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShortcutPrivacyApplyActionPerformed
        if (target != null) {
            target.shortcutPrivacyKey = "";
            if (chkShortcutPrivacyControl.isSelected()) {
                target.shortcutPrivacyKey += "control ";
            }
            if (chkShortcutPrivacySHIFT.isSelected()) {
                target.shortcutPrivacyKey += "shift ";
            }
            target.shortcutPrivacyKey += cboShortcutPrivacyKey.getSelectedItem().toString();
            initializeShortCuts();
        }
    }//GEN-LAST:event_btnShortcutPrivacyApplyActionPerformed

    private void popTrayIconPrivacyModeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_popTrayIconPrivacyModeItemStateChanged
        if (!isLoading && runningOverlay != null) {
            runningOverlay.setPrivateMode(ItemEvent.SELECTED == evt.getStateChange());
        }
    }//GEN-LAST:event_popTrayIconPrivacyModeItemStateChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            /* Set the Nimbus look and feel */
            //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
            /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
             * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
             */
//        try {
//            //javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
//</editor-fold>
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        //</editor-fold>
        if (args.length >= 1) {
            /* Create and display the form */
            java.awt.EventQueue.invokeLater(() -> {
                new Main(new File(args[0])).setVisible(true);
            });
        } else {
            /* Create and display the form */
            java.awt.EventQueue.invokeLater(() -> {
                new Main(null).setVisible(true);
            });
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCapture;
    private javax.swing.JButton btnEditor;
    private javax.swing.JButton btnSetDisplay;
    private javax.swing.JButton btnSetProfile;
    private javax.swing.JButton btnSetTarget;
    private javax.swing.JButton btnSetWebcam;
    private javax.swing.JButton btnShortcutCaptureApply;
    private javax.swing.JButton btnShortcutPrivacyApply;
    private javax.swing.JComboBox cboAudioRate;
    private javax.swing.JComboBox cboAudiosInternal;
    private javax.swing.JComboBox cboAudiosMicrophone;
    private javax.swing.JComboBox cboDisplays;
    private javax.swing.JComboBox cboOverlays;
    private javax.swing.JComboBox<Renderer.PanelLocation> cboPanelOrientation;
    private javax.swing.JComboBox cboProfiles;
    private javax.swing.JComboBox cboShortcutCaptureKey;
    private javax.swing.JComboBox cboShortcutPrivacyKey;
    private javax.swing.JComboBox cboTargets;
    private javax.swing.JComboBox<java.io.File> cboWaterMarks;
    private javax.swing.JComboBox cboWebcams;
    private javax.swing.JCheckBox chkDebugMode;
    private javax.swing.JCheckBox chkDoNotHide;
    private javax.swing.JCheckBox chkShortcutCaptureControl;
    private javax.swing.JCheckBox chkShortcutCaptureSHIFT;
    private javax.swing.JCheckBox chkShortcutPrivacyControl;
    private javax.swing.JCheckBox chkShortcutPrivacySHIFT;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblCurrentTargetConfiguration;
    private javax.swing.JLabel lblMessages;
    private javax.swing.JLabel lblNotice;
    private javax.swing.JLabel lblProfiles;
    private javax.swing.JLabel lblTargets;
    private javax.swing.JPanel panCapture;
    private javax.swing.JPanel panPanel;
    private javax.swing.JPanel panShortcuts;
    private javax.swing.JPanel panSources;
    private javax.swing.JPanel panStatusBar;
    private java.awt.PopupMenu popTrayIcon;
    private java.awt.MenuItem popTrayIconExit;
    private java.awt.Menu popTrayIconPanelContent;
    private java.awt.CheckboxMenuItem popTrayIconPrivacyMode;
    private java.awt.MenuItem popTrayIconRecord;
    private javax.swing.JScrollPane scrollPanelContentText;
    private javax.swing.JSpinner spinPanelSize;
    private javax.swing.JSpinner spinShowDurationTime;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JTextField txtCommand;
    private javax.swing.JTextArea txtPanelContentText;
    private javax.swing.JTextField txtTwitchAlertFolder;
    // End of variables declaration//GEN-END:variables

    @Override
    public void itemStateChanged(ItemEvent e) {

        if (e.getStateChange() == ItemEvent.SELECTED) {
            setPanelContent(new File(((CheckboxMenuItem) e.getSource()).getActionCommand()));
            for (int i = 0; i < popTrayIconPanelContent.getItemCount(); i++) {
                CheckboxMenuItem item = (CheckboxMenuItem) popTrayIconPanelContent.getItem(i);
                if (!item.equals(e.getSource())) {
                    item.setState(false);
                }
            }
        }
    }

    @Override
    public void onHotKey(HotKey hotkey) {
        String shortcut = "";

        if (hotkey.toString().contains("ctrl")) {
            shortcut += " control";
        }
        if (hotkey.toString().contains("alt")) {
            shortcut += " alt";
        }
        if (hotkey.toString().contains("shift")) {
            shortcut += " shift";
        }
        shortcut += " " + KeyEvent.getKeyText(hotkey.keyStroke.getKeyCode());

        shortcut = shortcut.trim().toUpperCase().replaceAll("  ", " ");
        if (shortcut.equals(target.shortcutCaptureKey.toUpperCase())) {
            btnCapture.doClick();
        } else if (shortcut.equals(target.shortcutPrivacyKey.toUpperCase())) {
            //Activate or deactivate privacy mode...
            if (runningOverlay != null) {
                runningOverlay.setPrivateMode(!runningOverlay.isPrivateMode());
                isLoading = true;
                CheckboxMenuItem item = (CheckboxMenuItem) popTrayIconPrivacyMode;
                item.setState(runningOverlay.isPrivateMode());
                isLoading = false;
            }
        }

    }
}
