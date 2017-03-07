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
package screenstudio.gui;

import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.Provider;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import screenstudio.Version;
import screenstudio.encoder.FFMpeg;
import screenstudio.panel.editor.Editor;
import screenstudio.remote.HTTPServer;
import screenstudio.sources.Compositor;
import screenstudio.sources.Microphone;
import screenstudio.sources.Screen;
import screenstudio.sources.Source;
import screenstudio.sources.SystemCheck;
import screenstudio.sources.Webcam;
import screenstudio.sources.transitions.Transition;
import screenstudio.targets.Layout;
import screenstudio.targets.Layout.SourceType;

/**
 *
 * @author patrick
 */
public class ScreenStudio extends javax.swing.JFrame {

    private final SourceLayoutPreview mLayoutPreview;
    private FFMpeg mFFMpeg = null;
    private String mVideoOutputFolder = System.getProperty("user.home");
    private long mRecordingTimestamp = 0;
    private java.awt.TrayIcon trayIcon;
    private final com.tulskiy.keymaster.common.Provider mShortcuts;
    private File mBackgroundMusic = null;
    private HTTPServer mRemote;

    /**
     * Creates new form MainVersion3
     */
    public ScreenStudio() {
        initComponents();
        this.setIconImage(new ImageIcon(ScreenStudio.class.getResource("/screenstudio/gui/images/icon.png")).getImage());
        initControls();
        mLayoutPreview = new SourceLayoutPreview(tableSources);
        mLayoutPreview.setOutputWidth((Integer) spinWidth.getValue());
        mLayoutPreview.setOutputHeight((Integer) spinHeight.getValue());
        panPreviewLayout.add(mLayoutPreview, BorderLayout.CENTER);
        this.setTitle("ScreenStudio " + screenstudio.Version.MAIN);
        this.setSize(700, 450);
        ToolTipManager.sharedInstance().setDismissDelay(8000);
        ToolTipManager.sharedInstance().setInitialDelay(2000);
        new Thread(() -> {
            if (Version.hasNewVersion()) {
                lblMessages.setText("A new version is available...");
            }
            String text = "";
            for (String msg : SystemCheck.getSystemCheck(false)) {
                text = text + msg + "\n ";
            }
            if (text.length() > 0) {
                lblMessages.setText(text);
                lblMessages.setForeground(Color.red);
                lblMessages.setToolTipText("<HTML><BODY>" + text.replaceAll("\n", "<BR>") + "</BODY></HTML>");
            }
        }).start();
        mShortcuts = Provider.getCurrentProvider(false);
        mShortcuts.register(KeyStroke.getKeyStroke("control shift R"), new HotKeyListener() {
            @Override
            public void onHotKey(HotKey hotkey) {
                System.out.println("Hotkey: " + hotkey.toString());
                switch (hotkey.keyStroke.getKeyCode()) {
                    case KeyEvent.VK_R:
                        mnuCapture.doClick();
                        break;
                }
            }
        });
        mRemote = new HTTPServer(null, null,mnuCapture);
        new Thread(mRemote).start();
        try {
            lblRemoteMessage.setText("Remote: http://" + Inet4Address.getLocalHost().getHostName() + ".local:" + mRemote.getPort());
        } catch (UnknownHostException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initControls() {
        DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
        updateRemoteSources();

        cboTarget.setModel(new DefaultComboBoxModel<>(FFMpeg.FORMATS.values()));
        cboTarget.setSelectedIndex(0);
        cboVideoPresets.setModel(new DefaultComboBoxModel<>(FFMpeg.Presets.values()));
        cboAudioBitrate.setModel(new DefaultComboBoxModel<>(FFMpeg.AudioRate.values()));
        cboRTMPServers.setModel(new DefaultComboBoxModel<>());
        txtRTMPKey.setText((""));
        try {
            cboAudioMicrophones.setModel(new DefaultComboBoxModel());
            cboAudioSystems.setModel(new DefaultComboBoxModel());
            cboAudioMicrophones.addItem(new Microphone());
            cboAudioSystems.addItem(new Microphone());
            for (Microphone o : Microphone.getSources()) {
                if (o.getDescription().toLowerCase().contains("monitor")) {
                    cboAudioSystems.addItem(o);
                } else {
                    cboAudioMicrophones.addItem(o);
                }
            }
        } catch (IOException | InterruptedException ex) {

        }
        setRTMPControls((FFMpeg.FORMATS) cboTarget.getSelectedItem());
        int defaultWidth = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth();
        int defaultHeight = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight();
        spinWidth.setValue(defaultWidth);
        spinHeight.setValue(defaultHeight);
        txtVideoFolder.setText(mVideoOutputFolder);
        txtVideoFolder.setToolTipText(mVideoOutputFolder);
        updateMenuWebcams();
        updateMenuDesktops();
        // get audio sync
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("screenstudio");
        spinAudioDelay.setValue(p.getFloat("audiodelay", 0));
        cboDefaultRecordingAction.setSelectedIndex(p.getInt("DefaultRecAction", 0));
        chkDoNotUseTrayIcon.setSelected(p.getBoolean("DoNotUseTrayIcon", chkDoNotUseTrayIcon.isSelected()));
        if (SystemTray.isSupported() && !chkDoNotUseTrayIcon.isSelected()) {
            trayIcon = new TrayIcon(this.getIconImage(), "ScreenStudio: Double-click to activate recording...");
            if (Screen.isOSX()) {
                trayIcon.setToolTip("ScreenStudio: CTRL-Click to activate recording...");
            }
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener((ActionEvent e) -> {
                mnuCapture.doClick();
            });
            try {
                SystemTray.getSystemTray().add(trayIcon);
            } catch (AWTException ex) {
                Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            trayIcon = null;
        }

        if (Screen.isOSX() || Screen.isWindows()) {
            cboAudioSystems.setEnabled(false);
        }

        for (Transition.NAMES t : Transition.NAMES.values()) {
            popMnuSourceTransitionIn.add(t.name());
            popMnuSourceTransitionOut.add(t.name());
        }
        for (int i = 0; i < popMnuSourceTransitionIn.getItemCount(); i++) {
            JMenuItem m = popMnuSourceTransitionIn.getItem(i);
            m.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (tableSources.getSelectedRow() != -1) {
                        tableSources.setValueAt(e.getActionCommand(), tableSources.getSelectedRow(), 10);
                    }
                }
            });
            m = popMnuSourceTransitionOut.getItem(i);
            m.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (tableSources.getSelectedRow() != -1) {
                        tableSources.setValueAt(e.getActionCommand(), tableSources.getSelectedRow(), 11);
                    }
                }
            });
        }
    }

    private void updateControls(boolean enabled) {

        cboTarget.setEnabled(enabled);
        cboVideoPresets.setEnabled(enabled);
        cboAudioBitrate.setEnabled(enabled);
        cboRTMPServers.setEnabled(enabled);
        txtRTMPKey.setEnabled(enabled);
        cboAudioMicrophones.setEnabled(enabled);
        cboAudioSystems.setEnabled(enabled);
        cboAudioMicrophones.setEnabled(enabled);
        cboAudioSystems.setEnabled(enabled);
        txtRTMPKey.setEnabled(enabled);
        cboRTMPServers.setEnabled(enabled);
        spinWidth.setEnabled(enabled);
        spinHeight.setEnabled(enabled);
        numVideoBitrate.setEnabled(enabled);
        spinFPS.setEnabled(enabled);
        btnSetVideoFolder.setEnabled(enabled);
        mnuFileLoad.setEnabled(enabled);
        mnuFileSave.setEnabled(enabled);
        spinAudioDelay.setEnabled(enabled);
        cboDefaultRecordingAction.setEnabled(enabled);
        if (Screen.isOSX() || Screen.isWindows()) {
            cboAudioSystems.setEnabled(false);
        }
        txtVideoFolder.setEnabled(enabled);
        txtVideoFolder.setVisible(enabled);
        btnBGMusicBrowse.setEnabled(enabled);
    }

    private void loadLayout(File file) {
        Layout layout = new Layout();
        try {
            layout.load(file);
            cboAudioBitrate.setSelectedItem(layout.getAudioBitrate());
            for (int i = 0; i < cboAudioMicrophones.getItemCount(); i++) {
                if (cboAudioMicrophones.getItemAt(i).getDescription().equals(layout.getAudioMicrophone())) {
                    cboAudioMicrophones.setSelectedIndex(i);
                    break;
                }
            }
            for (int i = 0; i < cboAudioSystems.getItemCount(); i++) {
                if (cboAudioSystems.getItemAt(i).getDescription().equals(layout.getAudioSystem())) {
                    cboAudioSystems.setSelectedIndex(i);
                    break;
                }
            }
            spinFPS.setValue(layout.getOutputFramerate());
            spinHeight.setValue(layout.getOutputHeight());
            cboVideoPresets.setSelectedItem(layout.getOutputPreset());
            cboTarget.setSelectedItem(layout.getOutputTarget());
            txtRTMPKey.setText(layout.getOutputRTMPKey());
            cboRTMPServers.setSelectedItem(layout.getOutputRTMPServer());
            mVideoOutputFolder = layout.getOutputVideoFolder();
            txtVideoFolder.setText(mVideoOutputFolder);
            txtVideoFolder.setToolTipText(mVideoOutputFolder);
            spinWidth.setValue(layout.getOutputWidth());
            numVideoBitrate.setValue(layout.getVideoBitrate());
            mBackgroundMusic = layout.getBackgroundMusic();
            if (mBackgroundMusic == null) {
                lblBGMusic.setText("");
            } else {
                lblBGMusic.setText(mBackgroundMusic.getAbsolutePath());
            }
            // load sources...
            DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
            while (model.getRowCount() > 0) {
                model.removeRow(0);
            }

            Webcam[] webcams = Webcam.getSources();
            for (screenstudio.targets.Source s : layout.getSources()) {
                Object[] row = new Object[model.getColumnCount()];
                row[0] = true;
                row[1] = s.Type;
                row[2] = "Not found!";
                row[3] = s.X;
                row[4] = s.Y;
                row[5] = s.Width;
                row[6] = s.Height;
                row[7] = s.Alpha;
                row[8] = s.startTime;
                row[9] = s.endTime;
                row[10] = s.transitionStart;
                row[11] = s.transitionStop;
                switch (s.Type) {
                    case Desktop:
                        row[0] = false;
                        Screen[] screens = Screen.getSources();
                        for (Screen screen : screens) {
                            if (screen.getLabel().equals(s.ID)) {
                                row[2] = screen;
                                row[0] = true;
                                if (s.CaptureX != 0 || s.CaptureY != 0) {
                                    screen.getSize().width = s.Width;
                                    screen.getSize().height = s.Height;
                                    screen.getSize().x = s.CaptureX;
                                    screen.getSize().y = s.CaptureY;
                                }
                                break;
                            }
                        }
                        break;
                    case Image:
                        row[2] = new File(s.ID);
                        row[0] = new File(s.ID).exists();
                        break;
                    case LabelText:
                        LabelText t = new LabelText(s.ID);
                        t.setForegroundColor(s.foregroundColor);
                        t.setBackgroundColor(s.backgroundColor);
                        t.setFontName(s.fontName);
                        System.out.println("Font: " + s.fontName);
                        row[2] = t;
                        break;
                    case Stream:
                        row[2] = s.ID;
                        break;
                    case Video:
                        row[2] = new File(s.ID);
                        row[0] = new File(s.ID).exists();
                        break;
                    case Webcam:
                        row[0] = false;
                        for (Webcam webcam : webcams) {
                            if (webcam.getDevice().equals(s.ID)) {
                                row[2] = webcam;
                                row[0] = true;
                                break;
                            }
                        }
                        break;
                }
                model.addRow(row);
            }
        } catch (IOException | ParserConfigurationException | SAXException | InterruptedException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }
        updateRemoteSources();
        panPreviewLayout.repaint();
    }

    private void saveLayout(File file) {
        Layout layout = new Layout();
        layout.setAudioBitrate(cboAudioBitrate.getItemAt(cboAudioBitrate.getSelectedIndex()));
        layout.setAudioMicrophone(cboAudioMicrophones.getItemAt(cboAudioMicrophones.getSelectedIndex()).getDescription());
        layout.setAudioSystem(cboAudioSystems.getItemAt(cboAudioSystems.getSelectedIndex()).getDescription());
        layout.setOutputFramerate((Integer) spinFPS.getValue());
        layout.setOutputHeight((Integer) spinHeight.getValue());
        layout.setOutputPreset(cboVideoPresets.getItemAt(cboVideoPresets.getSelectedIndex()));
        layout.setOutputRTMPKey(txtRTMPKey.getText());
        if (cboRTMPServers.getSelectedIndex() != -1) {
            layout.setOutputRTMPServer(cboRTMPServers.getSelectedItem().toString());
        } else {
            layout.setOutputRTMPServer("");
        }
        layout.setOutputTarget(cboTarget.getItemAt(cboTarget.getSelectedIndex()));
        layout.setOutputVideoFolder(mVideoOutputFolder);
        layout.setOutputWith((Integer) spinWidth.getValue());
        layout.setVideoBitrate((Integer) numVideoBitrate.getValue());
        layout.setBackgroundMusic(mBackgroundMusic);
        List<Source> sources = Compositor.getSources(tableSources, (Integer) spinFPS.getValue());
        for (Source s : sources) {
            layout.addSource(s.getType(), s.getID(), s.getBounds().x, s.getBounds().y, s.getBounds().width, s.getBounds().height, s.getAlpha().getAlpha(), s.getZOrder(), s.getForeground(), s.getBackground(), s.getFontName(), s.getCaptureX(), s.getCaptureY(), s.getStartDisplayTime(), s.getEndDisplayTime(), s.getTransitionStart().name(), s.getTransitionStop().name());
        }
        try {
            layout.save(file);
        } catch (Exception ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setRTMPControls(FFMpeg.FORMATS value) {
        if (value != null) {
            switch (value) {
                case BROADCAST:
                case MOV:
                case MP4:
                case FLV:
                case TS:
                case GIF:
                case HTTP:
                    cboRTMPServers.setModel(new DefaultComboBoxModel());
                    txtRTMPKey.setText((""));
                    cboRTMPServers.setVisible(false);
                    txtRTMPKey.setVisible(false);
                    lblRTMPKey.setVisible(false);
                    lblRTMPServer.setVisible(false);
                    break;
                case HITBOX:
                case RTMP:
                case TWITCH:
                case USTREAM:
                case VAUGHNLIVE:
                case YOUTUBE:
                case FACEBOOK:
                    cboRTMPServers.setModel(new DefaultComboBoxModel(FFMpeg.getServerList(value)));
                    txtRTMPKey.setText((""));
                    cboRTMPServers.setVisible(true);
                    txtRTMPKey.setVisible(true);
                    lblRTMPKey.setVisible(true);
                    lblRTMPServer.setVisible(true);
                    break;
            }
        }
    }

    private void updateMenuWebcams() {

        mnuMainWebcams.removeAll();
        try {
            for (Webcam w : Webcam.getSources()) {
                JMenuItem menu = new JMenuItem(w.getDescription());
                menu.setActionCommand(w.getDevice());
                menu.setToolTipText("Device: " + w.getDevice());
                menu.addActionListener((ActionEvent e) -> {
                    DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
                    Object[] row = new Object[model.getColumnCount()];
                    try {
                        for (Webcam webcam : Webcam.getSources()) {
                            if (webcam.getDevice().equals(e.getActionCommand())) {
                                row[0] = true;
                                row[1] = SourceType.Webcam;
                                row[2] = webcam;
                                row[3] = 0;
                                row[4] = 0;
                                row[5] = webcam.getWidth();
                                row[6] = webcam.getHeight();
                                row[7] = 1;
                                row[8] = 0L;
                                row[9] = 0L;
                                row[10] = Transition.NAMES.None.name();
                                row[11] = Transition.NAMES.None.name();
                                model.addRow(row);
                                updateRemoteSources();

                                mLayoutPreview.repaint();
                                tabs.setSelectedComponent(panSources);
                                break;

                            }
                        }
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });

                mnuMainWebcams.add(menu);
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateRemoteSources() {
        ArrayList<String> sources = new ArrayList<>();
        for (int i = tableSources.getRowCount()-1; i >=0; i--) {
            sources.add(tableSources.getValueAt(i, 2).toString());
        }
        if (mRemote != null) {
            mRemote.setSourceIDs(sources);
        }
    }

    private void updateMenuDesktops() {

        mnuMainDestops.removeAll();
        try {
            for (Screen s : Screen.getSources()) {
                JMenuItem menu = new JMenuItem(s.getDetailledLabel());
                menu.setActionCommand(s.getLabel());
                menu.setToolTipText("Size: " + s.getDetailledLabel());
                menu.addActionListener((ActionEvent e) -> {
                    DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
                    Object[] row = new Object[model.getColumnCount()];
                    try {
                        for (Screen screen : Screen.getSources()) {
                            if (screen.getLabel().equals(e.getActionCommand())) {
                                row[0] = true;
                                row[1] = SourceType.Desktop;
                                row[2] = screen;
                                row[3] = 0;
                                row[4] = 0;
                                row[5] = spinWidth.getValue();
                                row[6] = spinHeight.getValue();
                                row[7] = 1;
                                row[8] = 0L;
                                row[9] = 0L;
                                row[10] = Transition.NAMES.None.name();
                                row[11] = Transition.NAMES.None.name();
                                model.addRow(row);
                                updateRemoteSources();

                                mLayoutPreview.repaint();
                                tabs.setSelectedComponent(panSources);
                                break;
                            }

                        }
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });

                mnuMainDestops.add(menu);
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
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

        popSources = new javax.swing.JPopupMenu();
        popMnuSourceTransitionIn = new javax.swing.JMenu();
        popMnuSourceTransitionOut = new javax.swing.JMenu();
        tabs = new javax.swing.JTabbedPane();
        panOutput = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        spinWidth = new javax.swing.JSpinner();
        spinHeight = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        spinFPS = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        cboTarget = new javax.swing.JComboBox<>();
        panTargetSettings = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lblRTMPServer = new javax.swing.JLabel();
        lblRTMPKey = new javax.swing.JLabel();
        numVideoBitrate = new javax.swing.JSpinner();
        cboVideoPresets = new javax.swing.JComboBox<>();
        cboAudioBitrate = new javax.swing.JComboBox<>();
        cboRTMPServers = new javax.swing.JComboBox<>();
        txtRTMPKey = new javax.swing.JTextField();
        chkKeepScreenRatio = new javax.swing.JCheckBox();
        panSources = new javax.swing.JPanel();
        splitterSources = new javax.swing.JSplitPane();
        panPreviewLayout = new javax.swing.JPanel();
        scrollSources = new javax.swing.JScrollPane();
        tableSources = new javax.swing.JTable();
        panOptions = new javax.swing.JPanel();
        panSettingsAudios = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        cboAudioMicrophones = new javax.swing.JComboBox<>();
        cboAudioSystems = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        spinAudioDelay = new javax.swing.JSpinner();
        panSettingsVideos = new javax.swing.JPanel();
        btnSetVideoFolder = new javax.swing.JButton();
        txtVideoFolder = new javax.swing.JTextField();
        panSettingsMisc = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        cboDefaultRecordingAction = new javax.swing.JComboBox<>();
        jLabel12 = new javax.swing.JLabel();
        chkDoNotUseTrayIcon = new javax.swing.JCheckBox();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        lblBGMusic = new javax.swing.JLabel();
        btnBGMusicBrowse = new javax.swing.JButton();
        panStatus = new javax.swing.JPanel();
        lblMessages = new javax.swing.JLabel();
        lblRemoteMessage = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        mnuFile = new javax.swing.JMenu();
        mnuCapture = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mnuFileLoad = new javax.swing.JMenuItem();
        mnuFileSave = new javax.swing.JMenuItem();
        mnuEdit = new javax.swing.JMenu();
        mnuMainWebcams = new javax.swing.JMenu();
        mnuMainDestops = new javax.swing.JMenu();
        mnuMainAddImage = new javax.swing.JMenuItem();
        mnuMainAddLabel = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        mnuMainMoveUp = new javax.swing.JMenuItem();
        mnuMainMoveDown = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        mnuMainRemove = new javax.swing.JMenuItem();

        popMnuSourceTransitionIn.setText("Transition In");
        popSources.add(popMnuSourceTransitionIn);

        popMnuSourceTransitionOut.setText("Transition Out");
        popSources.add(popMnuSourceTransitionOut);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ScreenStudio");
        addWindowStateListener(new java.awt.event.WindowStateListener() {
            public void windowStateChanged(java.awt.event.WindowEvent evt) {
                formWindowStateChanged(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jLabel1.setText("Output Format");

        spinWidth.setModel(new javax.swing.SpinnerNumberModel(720, 640, 1920, 1));
        spinWidth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinWidthStateChanged(evt);
            }
        });

        spinHeight.setModel(new javax.swing.SpinnerNumberModel(480, 240, 1080, 1));
        spinHeight.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinHeightStateChanged(evt);
            }
        });

        jLabel2.setText("X");

        jLabel3.setText("Framerate");

        spinFPS.setModel(new javax.swing.SpinnerNumberModel(10, 1, 60, 1));

        jLabel4.setText("Target");

        cboTarget.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboTargetActionPerformed(evt);
            }
        });

        panTargetSettings.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));

        jLabel5.setText("Video Bitrate");

        jLabel6.setText("Video Preset");

        jLabel7.setText("Audio Bitrate");

        lblRTMPServer.setText("RTMP Server");

        lblRTMPKey.setText("RTMP Secret Key");

        numVideoBitrate.setModel(new javax.swing.SpinnerNumberModel(1000, 1, 9000, 50));

        javax.swing.GroupLayout panTargetSettingsLayout = new javax.swing.GroupLayout(panTargetSettings);
        panTargetSettings.setLayout(panTargetSettingsLayout);
        panTargetSettingsLayout.setHorizontalGroup(
            panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panTargetSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panTargetSettingsLayout.createSequentialGroup()
                        .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)
                            .addComponent(lblRTMPServer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cboAudioBitrate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cboRTMPServers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(panTargetSettingsLayout.createSequentialGroup()
                        .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(numVideoBitrate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cboVideoPresets, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(panTargetSettingsLayout.createSequentialGroup()
                        .addComponent(lblRTMPKey, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRTMPKey, javax.swing.GroupLayout.PREFERRED_SIZE, 435, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panTargetSettingsLayout.setVerticalGroup(
            panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panTargetSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(numVideoBitrate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(cboVideoPresets, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(cboAudioBitrate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRTMPServer)
                    .addComponent(cboRTMPServers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRTMPKey)
                    .addComponent(txtRTMPKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        chkKeepScreenRatio.setText("Keep Screen Ratio");

        javax.swing.GroupLayout panOutputLayout = new javax.swing.GroupLayout(panOutput);
        panOutput.setLayout(panOutputLayout);
        panOutputLayout.setHorizontalGroup(
            panOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panTargetSettings, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panOutputLayout.createSequentialGroup()
                        .addGroup(panOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(panOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(panOutputLayout.createSequentialGroup()
                                .addComponent(spinWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(spinFPS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cboTarget, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chkKeepScreenRatio)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panOutputLayout.setVerticalGroup(
            panOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panOutputLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(spinWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(chkKeepScreenRatio))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(spinFPS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(cboTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panTargetSettings, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabs.addTab("Output", panOutput);

        panSources.setLayout(new java.awt.BorderLayout());

        splitterSources.setDividerLocation(150);
        splitterSources.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        panPreviewLayout.setBackground(new java.awt.Color(51, 51, 51));
        panPreviewLayout.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Layout", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 12), new java.awt.Color(255, 255, 255))); // NOI18N
        panPreviewLayout.setLayout(new java.awt.BorderLayout());
        splitterSources.setRightComponent(panPreviewLayout);

        scrollSources.setBorder(javax.swing.BorderFactory.createTitledBorder("Video Sources"));

        tableSources.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                " ", "Source", "Description", "X", "Y", "Width", "Height", "Alpha", "Start", "End", "Trans-In", "Trans-Out"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Long.class, java.lang.Long.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, true, true, true, true, true, true, true, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableSources.setToolTipText("Double-click for more options...");
        tableSources.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        tableSources.setColumnSelectionAllowed(true);
        tableSources.setComponentPopupMenu(popSources);
        tableSources.setFillsViewportHeight(true);
        tableSources.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableSources.setSurrendersFocusOnKeystroke(true);
        tableSources.getTableHeader().setReorderingAllowed(false);
        tableSources.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableSourcesMouseClicked(evt);
            }
        });
        tableSources.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                tableSourcesPropertyChange(evt);
            }
        });
        tableSources.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tableSourcesKeyPressed(evt);
            }
        });
        scrollSources.setViewportView(tableSources);
        tableSources.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        if (tableSources.getColumnModel().getColumnCount() > 0) {
            tableSources.getColumnModel().getColumn(0).setResizable(false);
            tableSources.getColumnModel().getColumn(0).setPreferredWidth(25);
            tableSources.getColumnModel().getColumn(1).setResizable(false);
            tableSources.getColumnModel().getColumn(1).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(2).setMinWidth(150);
            tableSources.getColumnModel().getColumn(2).setPreferredWidth(150);
            tableSources.getColumnModel().getColumn(3).setResizable(false);
            tableSources.getColumnModel().getColumn(3).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(4).setResizable(false);
            tableSources.getColumnModel().getColumn(4).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(5).setResizable(false);
            tableSources.getColumnModel().getColumn(5).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(6).setResizable(false);
            tableSources.getColumnModel().getColumn(6).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(7).setResizable(false);
            tableSources.getColumnModel().getColumn(7).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(8).setResizable(false);
            tableSources.getColumnModel().getColumn(8).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(9).setResizable(false);
            tableSources.getColumnModel().getColumn(9).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(10).setResizable(false);
            tableSources.getColumnModel().getColumn(10).setPreferredWidth(95);
            tableSources.getColumnModel().getColumn(11).setResizable(false);
            tableSources.getColumnModel().getColumn(11).setPreferredWidth(95);
        }

        splitterSources.setLeftComponent(scrollSources);

        panSources.add(splitterSources, java.awt.BorderLayout.CENTER);

        tabs.addTab("Sources", panSources);

        panSettingsAudios.setBorder(javax.swing.BorderFactory.createTitledBorder("Audio"));

        jLabel8.setText("Microphone Input");

        jLabel9.setText("Audio System Input");

        jLabel10.setText("Audio Delay");

        spinAudioDelay.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(-5.0f), Float.valueOf(5.0f), Float.valueOf(0.1f)));
        spinAudioDelay.setToolTipText("<HTML><BODY>\nApply a delay (in seconds) to the audio.\n<BR><I>If video is late, apply a positive value...</I>\n</BODY></HTML>");
        spinAudioDelay.setEditor(new javax.swing.JSpinner.NumberEditor(spinAudioDelay, "#.#"));
        spinAudioDelay.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinAudioDelayStateChanged(evt);
            }
        });

        javax.swing.GroupLayout panSettingsAudiosLayout = new javax.swing.GroupLayout(panSettingsAudios);
        panSettingsAudios.setLayout(panSettingsAudiosLayout);
        panSettingsAudiosLayout.setHorizontalGroup(
            panSettingsAudiosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSettingsAudiosLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panSettingsAudiosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panSettingsAudiosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cboAudioMicrophones, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cboAudioSystems, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panSettingsAudiosLayout.createSequentialGroup()
                        .addComponent(spinAudioDelay, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panSettingsAudiosLayout.setVerticalGroup(
            panSettingsAudiosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSettingsAudiosLayout.createSequentialGroup()
                .addGroup(panSettingsAudiosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(cboAudioMicrophones, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panSettingsAudiosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(cboAudioSystems, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panSettingsAudiosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(spinAudioDelay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panSettingsVideos.setBorder(javax.swing.BorderFactory.createTitledBorder("Video Folders"));

        btnSetVideoFolder.setText("Browse");
        btnSetVideoFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetVideoFolderActionPerformed(evt);
            }
        });

        txtVideoFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtVideoFolderActionPerformed(evt);
            }
        });
        txtVideoFolder.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtVideoFolderKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout panSettingsVideosLayout = new javax.swing.GroupLayout(panSettingsVideos);
        panSettingsVideos.setLayout(panSettingsVideosLayout);
        panSettingsVideosLayout.setHorizontalGroup(
            panSettingsVideosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSettingsVideosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txtVideoFolder, javax.swing.GroupLayout.PREFERRED_SIZE, 497, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSetVideoFolder)
                .addContainerGap())
        );
        panSettingsVideosLayout.setVerticalGroup(
            panSettingsVideosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSettingsVideosLayout.createSequentialGroup()
                .addGroup(panSettingsVideosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSetVideoFolder)
                    .addComponent(txtVideoFolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panSettingsMisc.setBorder(javax.swing.BorderFactory.createTitledBorder("Misc"));

        jLabel11.setText("When recording");

        cboDefaultRecordingAction.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Hide", "Minimize", "Stay Visible" }));
        cboDefaultRecordingAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboDefaultRecordingActionActionPerformed(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(102, 102, 102));
        jLabel12.setText("Contrl Shift R can be used as a global shortcut");

        chkDoNotUseTrayIcon.setText("Do not use Tray Icon");
        chkDoNotUseTrayIcon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkDoNotUseTrayIconActionPerformed(evt);
            }
        });

        jLabel13.setText("When starting,");

        jLabel14.setText("Background Music");

        lblBGMusic.setText(" ");
        lblBGMusic.setToolTipText("<html>\n<body>\nSelect an audio file to play in the background<br>\nSet the proper audio volume and duration using a software like <b>Audacity</b><br>\n<i>Tip: Make the duration last a bit longer than your recording to have a background music for all the lenght of your video</i>\n</body>\n</html>");

        btnBGMusicBrowse.setText("Browse");
        btnBGMusicBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBGMusicBrowseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panSettingsMiscLayout = new javax.swing.GroupLayout(panSettingsMisc);
        panSettingsMisc.setLayout(panSettingsMiscLayout);
        panSettingsMiscLayout.setHorizontalGroup(
            panSettingsMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSettingsMiscLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panSettingsMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.LEADING))
                .addGap(18, 18, 18)
                .addGroup(panSettingsMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panSettingsMiscLayout.createSequentialGroup()
                        .addComponent(lblBGMusic, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBGMusicBrowse))
                    .addGroup(panSettingsMiscLayout.createSequentialGroup()
                        .addGroup(panSettingsMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chkDoNotUseTrayIcon)
                            .addGroup(panSettingsMiscLayout.createSequentialGroup()
                                .addComponent(cboDefaultRecordingAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel12)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panSettingsMiscLayout.setVerticalGroup(
            panSettingsMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSettingsMiscLayout.createSequentialGroup()
                .addGroup(panSettingsMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(cboDefaultRecordingAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panSettingsMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(chkDoNotUseTrayIcon))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panSettingsMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(lblBGMusic)
                    .addComponent(btnBGMusicBrowse))
                .addContainerGap())
        );

        javax.swing.GroupLayout panOptionsLayout = new javax.swing.GroupLayout(panOptions);
        panOptions.setLayout(panOptionsLayout);
        panOptionsLayout.setHorizontalGroup(
            panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panSettingsAudios, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panSettingsVideos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panSettingsMisc, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panOptionsLayout.setVerticalGroup(
            panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panSettingsAudios, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panSettingsVideos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panSettingsMisc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabs.addTab("Options", panOptions);

        getContentPane().add(tabs, java.awt.BorderLayout.CENTER);

        panStatus.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        panStatus.setPreferredSize(new java.awt.Dimension(767, 20));
        panStatus.setLayout(new java.awt.GridLayout());

        lblMessages.setText("Welcome to ScreenStudio");
        panStatus.add(lblMessages);

        lblRemoteMessage.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblRemoteMessage.setText("...");
        panStatus.add(lblRemoteMessage);
        lblRemoteMessage.getAccessibleContext().setAccessibleName("");
        lblRemoteMessage.getAccessibleContext().setAccessibleDescription("");

        getContentPane().add(panStatus, java.awt.BorderLayout.SOUTH);

        mnuFile.setText("Layout");

        mnuCapture.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        mnuCapture.setText("Record");
        mnuCapture.setToolTipText("<html><body>\nStart recording/streaming using CTRL-R.  \n<BR><B>ScreenStudio</B> will automatically hide in the taskbar of your system.  \n<BR>To stop the recording, simply restore the <B>ScreenStudio</B> window.\n</body></html>");
        mnuCapture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuCaptureActionPerformed(evt);
            }
        });
        mnuFile.add(mnuCapture);
        mnuFile.add(jSeparator1);

        mnuFileLoad.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileLoad.setText("Open");
        mnuFileLoad.setToolTipText("<HTML><BODY>\nOpen a <B>ScreenStudio</B> XML layout file\n</BODY></HTML>");
        mnuFileLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileLoadActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileLoad);

        mnuFileSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileSave.setText("Save");
        mnuFileSave.setToolTipText("<HTML><BODY>\nSave the current layour to a <B>ScreenStudio</B> XML layout file\n</BODY></HTML>");
        mnuFileSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileSaveActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileSave);

        menuBar.add(mnuFile);

        mnuEdit.setText("Sources");

        mnuMainWebcams.setText("Webcams");
        mnuEdit.add(mnuMainWebcams);

        mnuMainDestops.setText("Desktops");
        mnuEdit.add(mnuMainDestops);

        mnuMainAddImage.setText("Add Image...");
        mnuMainAddImage.setToolTipText("Browse your hard disk to add a source image file");
        mnuMainAddImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainAddImageActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainAddImage);

        mnuMainAddLabel.setText("Add Label");
        mnuMainAddLabel.setToolTipText("<HTML><BODY>\nAdd a new text label.  \n<BR>Double-click on the source to edit the content.\n</BODY></HTML>");
        mnuMainAddLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainAddLabelActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainAddLabel);
        mnuEdit.add(jSeparator3);

        mnuMainMoveUp.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.ALT_MASK));
        mnuMainMoveUp.setText("Move Up");
        mnuMainMoveUp.setToolTipText("Move the currently selected source to a higher layer");
        mnuMainMoveUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainMoveUpActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainMoveUp);

        mnuMainMoveDown.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.ALT_MASK));
        mnuMainMoveDown.setText("Move Down");
        mnuMainMoveDown.setToolTipText("Move the currently selected source to a lower layer");
        mnuMainMoveDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainMoveDownActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainMoveDown);
        mnuEdit.add(jSeparator2);

        mnuMainRemove.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        mnuMainRemove.setText("Remove Source");
        mnuMainRemove.setToolTipText("Remove the currently selected source");
        mnuMainRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainRemoveActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainRemove);

        menuBar.add(mnuEdit);

        setJMenuBar(menuBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tableSourcesPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_tableSourcesPropertyChange
        if (mLayoutPreview != null) {
            mLayoutPreview.repaint();
        }
    }//GEN-LAST:event_tableSourcesPropertyChange

    private void tableSourcesKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableSourcesKeyPressed
        mLayoutPreview.repaint();
    }//GEN-LAST:event_tableSourcesKeyPressed

    private void tableSourcesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSourcesMouseClicked
        if (evt.getClickCount() == 2) {
            int rowIndex = tableSources.getSelectedRow();
            if (tableSources.getValueAt(rowIndex, 1) == SourceType.LabelText) {
                Editor ed = new Editor(((LabelText) tableSources.getValueAt(rowIndex, 2)), this);
                ed.setModal(true);
                ed.setVisible(true);
                tableSources.setValueAt(ed.getLabelText(), rowIndex, 2);
                tableSources.repaint();
            } else if (tableSources.getValueAt(rowIndex, 1) == SourceType.Desktop) {
                Screen s = (Screen) tableSources.getValueAt(rowIndex, 2);
                ScreenStudioCaptureArea d = new ScreenStudioCaptureArea(this, true);
                d.setVisible(true);
                switch (d.getReturnStatus()) {
                    case 1:
                        s.setSize(d.getReturnBounds());
                        tableSources.setValueAt(s.getWidth(), rowIndex, 5);
                        tableSources.setValueAt(s.getHeight(), rowIndex, 6);
                        break;
                    case 2:
                        break;
                }
                tableSources.repaint();
            }
        }
        mLayoutPreview.repaint();
    }//GEN-LAST:event_tableSourcesMouseClicked

    boolean mAutoAction = false;
    private void spinWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinWidthStateChanged
        if (mLayoutPreview != null) {
            mLayoutPreview.setOutputWidth((Integer) spinWidth.getValue());
            if (!mAutoAction && chkKeepScreenRatio.isSelected()) {
                mAutoAction = true;
                Rectangle r = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
                int h = r.height;
                int w = r.width;
                int value = ((Integer) spinWidth.getValue()) * h / w;
                if (value % 2 != 0) {
                    value++;
                }
                spinHeight.setValue(value);
                mAutoAction = false;
            }
        }
    }//GEN-LAST:event_spinWidthStateChanged

    private void spinHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinHeightStateChanged
        if (mLayoutPreview != null) {
            mLayoutPreview.setOutputHeight((Integer) spinHeight.getValue());
            if (!mAutoAction && chkKeepScreenRatio.isSelected()) {
                mAutoAction = true;
                Rectangle r = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
                int h = r.height;
                int w = r.width;
                int value = ((Integer) spinHeight.getValue()) * w / h;
                if (value % 2 != 0) {
                    value++;
                }
                spinWidth.setValue(value);
                mAutoAction = false;
            }
        }
    }//GEN-LAST:event_spinHeightStateChanged

    private void cboTargetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboTargetActionPerformed
        setRTMPControls((FFMpeg.FORMATS) cboTarget.getSelectedItem());
    }//GEN-LAST:event_cboTargetActionPerformed


    private void mnuCaptureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuCaptureActionPerformed
        if (mFFMpeg != null) {
            if (mFFMpeg.getState() == FFMpeg.RunningState.Error) {
                lblMessages.setText(mFFMpeg.getLastErrorMessage());
            } else {
                lblMessages.setText("Stopped...");
            }
            mnuCapture.setEnabled(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mFFMpeg.stop();
                    while (mFFMpeg.getState() == FFMpeg.RunningState.Running) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    try {
                        Microphone.getVirtualAudio(null, null);
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    mFFMpeg = null;
                    mnuCapture.setText("Record");
                    switch (cboDefaultRecordingAction.getSelectedIndex()) {
                        case 0: // Hide
                            if (trayIcon != null) {
                                setVisible(true);
                            } else {
                                setExtendedState(JFrame.NORMAL);
                            }
                            break;
                        case 1: // Minimize
                            setExtendedState(JFrame.NORMAL);
                            break;
                        case 2: // Stay Visible
                            break;
                    }

                    if (trayIcon != null) {
                        trayIcon.setImage(getIconImage());
                    }
                    updateControls(true);
                    if (FFMpeg.isRTMP((FFMpeg.FORMATS) cboTarget.getSelectedItem())) {
                        txtRTMPKey.setVisible(true);
                    }
                    mnuCapture.setEnabled(true);
                    mRemote.setCompositor(null);

                }
            }).start();

        } else {
            if (trayIcon != null) {
                trayIcon.setImage(new ImageIcon(ScreenStudio.class.getResource("/screenstudio/gui/images/iconStarting.png")).getImage());
            }
            if (txtRTMPKey.isVisible()) {
                txtRTMPKey.setVisible(false);
            }
            boolean abort = false;
            if (tableSources.getRowCount() == 0) {
                lblMessages.setText("No video source to display...");
                abort = true;
            }
            if (cboTarget.getSelectedItem() != FFMpeg.FORMATS.GIF && cboAudioMicrophones.getSelectedIndex() == 0 && cboAudioSystems.getSelectedIndex() == 0) {
                lblMessages.setText("No audio source selected...");
                abort = true;
            }
            if (!abort) {
                List<Source> sources = Compositor.getSources(tableSources, (Integer) spinFPS.getValue());
                Compositor compositor = new Compositor(sources, new Rectangle(0, 0, (Integer) spinWidth.getValue(), (Integer) spinHeight.getValue()), (Integer) spinFPS.getValue());
                mFFMpeg = new FFMpeg(compositor);
                mRemote.setCompositor(compositor);
                String audio = "default";
                Microphone mic = null;
                Microphone sys = null;
                if (cboAudioMicrophones.getSelectedIndex() > 0) {
                    mic = (Microphone) cboAudioMicrophones.getSelectedItem();
                }
                if (cboAudioSystems.getSelectedIndex() > 0) {
                    sys = (Microphone) cboAudioSystems.getSelectedItem();
                }
                if (mic != null || sys != null) {
                    try {
                        audio = Microphone.getVirtualAudio(mic, sys);
                    } catch (IOException ex) {
                        Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    mFFMpeg.setAudio((FFMpeg.AudioRate) cboAudioBitrate.getSelectedItem(), audio, (Float) spinAudioDelay.getValue(), mBackgroundMusic);
                }
                String server = "";
                if (cboRTMPServers.getSelectedItem() != null) {
                    server = cboRTMPServers.getSelectedItem().toString();
                    server = server.split(";")[1];
                }
                mFFMpeg.setOutputFormat((FFMpeg.FORMATS) cboTarget.getSelectedItem(), (FFMpeg.Presets) cboVideoPresets.getSelectedItem(), (Integer) numVideoBitrate.getValue(), server, txtRTMPKey.getText(), mVideoOutputFolder);
                new Thread(mFFMpeg).start();
                lblMessages.setText("Recording...");
                mnuCapture.setText("Stop");

                updateControls(false);

                switch (cboDefaultRecordingAction.getSelectedIndex()) {
                    case 0: // Hide
                        if (trayIcon != null) {
                            this.setVisible(false);
                        } else {
                            this.setExtendedState(JFrame.ICONIFIED);
                        }
                        break;
                    case 1: // Minimize
                        this.setExtendedState(JFrame.ICONIFIED);
                        break;
                    case 2: // Stay Visible
                        break;
                }

                mRecordingTimestamp = System.currentTimeMillis();
                new Thread(() -> {
                    FFMpeg f = mFFMpeg;
                    FFMpeg.RunningState initState = FFMpeg.RunningState.Starting;
                    while (f != null) {
                        if (initState == FFMpeg.RunningState.Starting && f.getState() == FFMpeg.RunningState.Running) {
                            if (trayIcon != null) {
                                trayIcon.setImage(new ImageIcon(ScreenStudio.class.getResource("/screenstudio/gui/images/iconRunning.png")).getImage());
                            }
                            initState = FFMpeg.RunningState.Running;
                        }
                        long seconds = (System.currentTimeMillis() - mRecordingTimestamp) / 1000;
                        if (seconds < 60) {
                            setTitle("Recording! (" + seconds + " sec)");
                        } else {
                            setTitle("Recording! (" + (seconds / 60) + " min " + (seconds % 60) + " sec)");
                        }
                        if (f.getState() == FFMpeg.RunningState.Error) {
                            System.err.println("Encoder error detected...");
                            mnuCapture.doClick();
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        f = mFFMpeg;
                    }
                    setTitle("ScreenStudio " + screenstudio.Version.MAIN);
                }).start();
            }
        }

    }//GEN-LAST:event_mnuCaptureActionPerformed

    private void mnuFileSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuFileSaveActionPerformed
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("screenstudio");
        String lastFolder = p.get("lastfolder", ".");
        JFileChooser chooser = new JFileChooser(new File(lastFolder));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toUpperCase().endsWith(".XML");
            }

            @Override
            public String getDescription() {
                return "ScreenStudio XML Layout";
            }
        });
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile() != null) {
                File f = chooser.getSelectedFile();
                if (!f.getName().endsWith(".xml")) {
                    f = new File(f.getAbsolutePath() + ".xml");
                }
                saveLayout(f);
                p.put("lastfolder", f.getParent());
                try {
                    p.flush();
                } catch (BackingStoreException ex) {
                    Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }//GEN-LAST:event_mnuFileSaveActionPerformed

    private void mnuFileLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuFileLoadActionPerformed
        // load last folder...
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("screenstudio");
        String lastFolder = p.get("lastfolder", ".");
        JFileChooser chooser = new JFileChooser(new File(lastFolder));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toUpperCase().endsWith(".XML");
            }

            @Override
            public String getDescription() {
                return "ScreenStudio XML Layout";
            }
        });
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile() != null) {
                loadLayout(chooser.getSelectedFile());
                p.put("lastfolder", chooser.getSelectedFile().getParent());
                try {
                    p.flush();
                } catch (BackingStoreException ex) {
                    Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }//GEN-LAST:event_mnuFileLoadActionPerformed

    private void btnSetVideoFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetVideoFolderActionPerformed
        JFileChooser chooser = new JFileChooser(mVideoOutputFolder);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.showOpenDialog(this);
        if (chooser.getSelectedFile() != null) {
            mVideoOutputFolder = chooser.getSelectedFile().getAbsolutePath();
            txtVideoFolder.setText(mVideoOutputFolder);
            txtVideoFolder.setToolTipText(mVideoOutputFolder);
        }
    }//GEN-LAST:event_btnSetVideoFolderActionPerformed

    private void mnuMainAddImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuMainAddImageActionPerformed
        JFileChooser chooser = new JFileChooser(mVideoOutputFolder);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toUpperCase().endsWith(".PNG") || f.getName().toUpperCase().endsWith(".JPG") || f.getName().toUpperCase().endsWith(".GIF") || f.getName().toUpperCase().endsWith(".BMP");
            }

            @Override
            public String getDescription() {
                return "Image Files";
            }
        });
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.showOpenDialog(this);
        if (chooser.getSelectedFile() != null) {
            File image = chooser.getSelectedFile();
            //add new source...
            DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
            Object[] row = new Object[model.getColumnCount()];
            row[0] = true;
            row[1] = SourceType.Image;
            row[2] = image;
            row[3] = 0;
            row[4] = 0;
            row[5] = 200;
            row[6] = 200;
            row[7] = 1.0f;
            row[8] = 0L;
            row[9] = 0L;
            row[10] = Transition.NAMES.None.name();
            row[11] = Transition.NAMES.None.name();
            model.addRow(row);
            updateRemoteSources();
        }
        mLayoutPreview.repaint();
        tabs.setSelectedComponent(panSources);
    }//GEN-LAST:event_mnuMainAddImageActionPerformed

    private void mnuMainAddLabelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuMainAddLabelActionPerformed
        DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
        Object[] row = new Object[model.getColumnCount()];
        row[0] = true;
        row[1] = SourceType.LabelText;
        row[2] = new LabelText("<HTML><BODY>New Label...</BODY><HTML>");
        row[3] = 0;
        row[4] = 0;
        row[5] = 300;
        row[6] = 100;
        row[7] = 1.0f;
        row[8] = 0L;
        row[9] = 0L;
        row[10] = Transition.NAMES.None.name();
        row[11] = Transition.NAMES.None.name();
        model.addRow(row);
        updateRemoteSources();

        mLayoutPreview.repaint();
        tabs.setSelectedComponent(panSources);
    }//GEN-LAST:event_mnuMainAddLabelActionPerformed

    private void mnuMainMoveUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuMainMoveUpActionPerformed
        if (tableSources.getSelectedRow() != -1 && tableSources.getSelectedRow() > 0) {
            Object[] row = new Object[tableSources.getColumnCount()];
            for (int i = 0; i < row.length; i++) {
                row[i] = tableSources.getValueAt(tableSources.getSelectedRow(), i);
                tableSources.setValueAt(tableSources.getValueAt(tableSources.getSelectedRow() - 1, i), tableSources.getSelectedRow(), i);
                tableSources.setValueAt(row[i], tableSources.getSelectedRow() - 1, i);
            }
            int index = tableSources.getSelectedRow() - 1;
            tableSources.setRowSelectionInterval(index, index);
            mLayoutPreview.repaint();
            tabs.setSelectedComponent(panSources);
        }
    }//GEN-LAST:event_mnuMainMoveUpActionPerformed

    private void mnuMainMoveDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuMainMoveDownActionPerformed
        if (tableSources.getSelectedRow() != -1 && tableSources.getSelectedRow() < tableSources.getRowCount() - 1) {
            Object[] row = new Object[tableSources.getColumnCount()];
            for (int i = 0; i < row.length; i++) {
                row[i] = tableSources.getValueAt(tableSources.getSelectedRow(), i);
                tableSources.setValueAt(tableSources.getValueAt(tableSources.getSelectedRow() + 1, i), tableSources.getSelectedRow(), i);
                tableSources.setValueAt(row[i], tableSources.getSelectedRow() + 1, i);
            }
            int index = tableSources.getSelectedRow() + 1;
            tableSources.setRowSelectionInterval(index, index);
            mLayoutPreview.repaint();
            tabs.setSelectedComponent(panSources);
        }
    }//GEN-LAST:event_mnuMainMoveDownActionPerformed

    private void formWindowStateChanged(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowStateChanged
        if (mFFMpeg != null && evt.getOldState() == JFrame.ICONIFIED) {
            switch (cboDefaultRecordingAction.getSelectedIndex()) {
                case 0: // Hide
                    if (trayIcon == null) {
                        mnuCapture.doClick();
                    }
                    break;
                case 1: // Minimize
                    mnuCapture.doClick();
                    break;
                case 2: // Stay Visible
                    break;
            }
            //mnuCapture.doClick();
        }
    }//GEN-LAST:event_formWindowStateChanged

    private void mnuMainRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuMainRemoveActionPerformed
        if (tableSources.getSelectedRow() != -1) {
            int index = tableSources.getSelectedRow();
            DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
            model.removeRow(index);
            updateRemoteSources();
            mLayoutPreview.repaint();
        }
    }//GEN-LAST:event_mnuMainRemoveActionPerformed

    private void spinAudioDelayStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinAudioDelayStateChanged
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("screenstudio");
        p.putFloat("audiodelay", (Float) spinAudioDelay.getValue());
        try {
            p.flush();
        } catch (BackingStoreException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_spinAudioDelayStateChanged

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        if (mRemote != null) {
            mRemote.shutdown();
            mRemote = null;
        }
        if (mShortcuts != null) {
            mShortcuts.stop();
        }
    }//GEN-LAST:event_formWindowClosing

    private void cboDefaultRecordingActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboDefaultRecordingActionActionPerformed
        //Keep preferences...
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("screenstudio");
        p.putInt("DefaultRecAction", cboDefaultRecordingAction.getSelectedIndex());
        try {
            p.flush();
        } catch (BackingStoreException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_cboDefaultRecordingActionActionPerformed

    private void chkDoNotUseTrayIconActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkDoNotUseTrayIconActionPerformed
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("screenstudio");
        p.putBoolean("DoNotUseTrayIcon", chkDoNotUseTrayIcon.isSelected());
        try {
            p.flush();
        } catch (BackingStoreException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_chkDoNotUseTrayIconActionPerformed

    private void btnBGMusicBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBGMusicBrowseActionPerformed
        JFileChooser chooser = new JFileChooser(mVideoOutputFolder);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toUpperCase().endsWith(".MP3") || f.getName().toUpperCase().endsWith(".WAV") || f.getName().toUpperCase().endsWith(".MP4") || f.getName().toUpperCase().endsWith(".BMP");
            }

            @Override
            public String getDescription() {
                return "Music Files";
            }
        });
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile() != null) {
                File music = chooser.getSelectedFile();
                lblBGMusic.setText(music.getAbsolutePath());
                mBackgroundMusic = music;
            }
        } else {
            lblBGMusic.setText("");
            mBackgroundMusic = null;
        }
    }//GEN-LAST:event_btnBGMusicBrowseActionPerformed

    private void txtVideoFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtVideoFolderActionPerformed
        mVideoOutputFolder = txtVideoFolder.getText();
        txtVideoFolder.setToolTipText(mVideoOutputFolder);
    }//GEN-LAST:event_txtVideoFolderActionPerformed

    private void txtVideoFolderKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtVideoFolderKeyTyped
        mVideoOutputFolder = txtVideoFolder.getText();
        txtVideoFolder.setToolTipText(mVideoOutputFolder);
    }//GEN-LAST:event_txtVideoFolderKeyTyped

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        System.out.println("Running on " + System.getProperty("os.name").toLowerCase());
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ScreenStudio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ScreenStudio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ScreenStudio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ScreenStudio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ScreenStudio().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBGMusicBrowse;
    private javax.swing.JButton btnSetVideoFolder;
    private javax.swing.JComboBox<FFMpeg.AudioRate> cboAudioBitrate;
    private javax.swing.JComboBox<Microphone> cboAudioMicrophones;
    private javax.swing.JComboBox<Microphone> cboAudioSystems;
    private javax.swing.JComboBox<String> cboDefaultRecordingAction;
    private javax.swing.JComboBox<String> cboRTMPServers;
    private javax.swing.JComboBox<FFMpeg.FORMATS> cboTarget;
    private javax.swing.JComboBox<FFMpeg.Presets> cboVideoPresets;
    private javax.swing.JCheckBox chkDoNotUseTrayIcon;
    private javax.swing.JCheckBox chkKeepScreenRatio;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JLabel lblBGMusic;
    private javax.swing.JLabel lblMessages;
    private javax.swing.JLabel lblRTMPKey;
    private javax.swing.JLabel lblRTMPServer;
    private javax.swing.JLabel lblRemoteMessage;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem mnuCapture;
    private javax.swing.JMenu mnuEdit;
    private javax.swing.JMenu mnuFile;
    private javax.swing.JMenuItem mnuFileLoad;
    private javax.swing.JMenuItem mnuFileSave;
    private javax.swing.JMenuItem mnuMainAddImage;
    private javax.swing.JMenuItem mnuMainAddLabel;
    private javax.swing.JMenu mnuMainDestops;
    private javax.swing.JMenuItem mnuMainMoveDown;
    private javax.swing.JMenuItem mnuMainMoveUp;
    private javax.swing.JMenuItem mnuMainRemove;
    private javax.swing.JMenu mnuMainWebcams;
    private javax.swing.JSpinner numVideoBitrate;
    private javax.swing.JPanel panOptions;
    private javax.swing.JPanel panOutput;
    private javax.swing.JPanel panPreviewLayout;
    private javax.swing.JPanel panSettingsAudios;
    private javax.swing.JPanel panSettingsMisc;
    private javax.swing.JPanel panSettingsVideos;
    private javax.swing.JPanel panSources;
    private javax.swing.JPanel panStatus;
    private javax.swing.JPanel panTargetSettings;
    private javax.swing.JMenu popMnuSourceTransitionIn;
    private javax.swing.JMenu popMnuSourceTransitionOut;
    private javax.swing.JPopupMenu popSources;
    private javax.swing.JScrollPane scrollSources;
    private javax.swing.JSpinner spinAudioDelay;
    private javax.swing.JSpinner spinFPS;
    private javax.swing.JSpinner spinHeight;
    private javax.swing.JSpinner spinWidth;
    private javax.swing.JSplitPane splitterSources;
    private javax.swing.JTable tableSources;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JTextField txtRTMPKey;
    private javax.swing.JTextField txtVideoFolder;
    // End of variables declaration//GEN-END:variables
}
