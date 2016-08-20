/*
 * Copyright (C) 2016 patrick
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

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import screenstudio.encoder.FFMpeg;
import screenstudio.panel.editor.Editor;
import screenstudio.sources.Compositor;
import screenstudio.sources.Microphone;
import screenstudio.sources.Screen;
import screenstudio.sources.Source;
import screenstudio.sources.Webcam;
import screenstudio.targets.Layout;
import screenstudio.targets.Layout.SourceType;

/**
 *
 * @author patrick
 */
public class MainVersion3 extends javax.swing.JFrame {

    private final SourceLayoutPreview mLayoutPreview;
    private FFMpeg mFFMpeg = null;
    private File mVideoOutputFolder = new File("");
    private long mRecordingTimestamp = 0;

    /**
     * Creates new form MainVersion3
     */
    public MainVersion3() {
        initComponents();
        this.setIconImage(new ImageIcon(MainVersion3.class.getResource("/screenstudio/gui/images/icon.png")).getImage());
        initControls();
        mLayoutPreview = new SourceLayoutPreview(tableSources);
        mLayoutPreview.setOutputWidth((Integer) spinWidth.getValue());
        mLayoutPreview.setOutputHeight((Integer) spinHeight.getValue());
        panPreviewLayout.add(mLayoutPreview, BorderLayout.CENTER);
        this.setTitle("ScreenStudio " + screenstudio.Version.MAIN);
        this.setSize(700, 400);
    }

    private void initControls() {
        DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
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
        lblVideoFolder.setText(mVideoOutputFolder.getAbsolutePath());
        lblVideoFolder.setToolTipText(mVideoOutputFolder.getAbsolutePath());
        updateMenuWebcams();
        updateMenuDesktops();
        // get audio sync
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("screenstudio");
        spinAudioDelay.setValue(p.getFloat("audiodelay", 0));
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
            txtRTMPKey.setText(layout.getOutputRTMPKey());
            cboRTMPServers.setSelectedItem(layout.getOutputRTMPServer());
            cboTarget.setSelectedItem(layout.getOutputTarget());
            mVideoOutputFolder = layout.getOutputVideoFolder();
            lblVideoFolder.setText(mVideoOutputFolder.getAbsolutePath());
            lblVideoFolder.setToolTipText(mVideoOutputFolder.getAbsolutePath());
            spinWidth.setValue(layout.getOutputWidth());
            numVideoBitrate.setValue(layout.getVideoBitrate());

            // load sources...
            DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
            while (model.getRowCount() > 0) {
                model.removeRow(0);
            }
            Screen[] screens = Screen.getSources();
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
                switch (s.Type) {
                    case Desktop:
                        row[0] = false;
                        for (Screen screen : screens) {
                            if (screen.getId().equals(s.ID)) {
                                row[2] = screen;
                                row[0] = true;
                                break;
                            }
                        }
                        break;
                    case Image:
                        row[2] = new File(s.ID);
                        row[0] = new File(s.ID).exists();
                        break;
                    case LabelFile:
                        row[2] = new File(s.ID);
                        row[0] = new File(s.ID).exists();
                        break;
                    case LabelText:
                        row[2] = s.ID;
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
            Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
        }

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

        List<Source> sources = Compositor.getSources(tableSources, (Integer) spinFPS.getValue());
        for (Source s : sources) {
            layout.addSource(s.getType(), s.getID(), s.getBounds().x, s.getBounds().y, s.getBounds().width, s.getBounds().height, s.getAlpha().getAlpha(), s.getZOrder());
        }
        try {
            layout.save(file);
        } catch (Exception ex) {
            Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
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
                                model.addRow(row);
                                mLayoutPreview.repaint();
                                tabs.setSelectedComponent(panSources);
                                break;

                            }
                        }
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });

                mnuMainWebcams.add(menu);
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateMenuDesktops() {

        mnuMainDestops.removeAll();
        try {
            for (Screen s : Screen.getSources()) {
                JMenuItem menu = new JMenuItem(s.getDetailledLabel());
                menu.setActionCommand(s.getId());
                menu.addActionListener((ActionEvent e) -> {
                    DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
                    Object[] row = new Object[model.getColumnCount()];
                    try {
                        for (Screen screen : Screen.getSources()) {
                            if (screen.getId().equals(e.getActionCommand())) {
                                row[0] = true;
                                row[1] = SourceType.Desktop;
                                row[2] = screen;
                                row[3] = 0;
                                row[4] = 0;
                                row[5] = screen.getWidth();
                                row[6] = screen.getHeight();
                                row[7] = 1;
                                model.addRow(row);
                                mLayoutPreview.repaint();
                                tabs.setSelectedComponent(panSources);
                                break;
                            }

                        }
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });

                mnuMainDestops.add(menu);
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
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
        panSources = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
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
        lblVideoFolder = new javax.swing.JLabel();
        btnSetVideoFolder = new javax.swing.JButton();
        panStatus = new javax.swing.JPanel();
        lblMessages = new javax.swing.JLabel();
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

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ScreenStudio");
        addWindowStateListener(new java.awt.event.WindowStateListener() {
            public void windowStateChanged(java.awt.event.WindowEvent evt) {
                formWindowStateChanged(evt);
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

        spinFPS.setModel(new javax.swing.SpinnerNumberModel(10, 5, 30, 1));

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

        numVideoBitrate.setModel(new javax.swing.SpinnerNumberModel(1000, 300, 9000, 100));

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
                    .addComponent(jLabel2))
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

        jSplitPane1.setDividerLocation(150);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        panPreviewLayout.setBackground(new java.awt.Color(51, 51, 51));
        panPreviewLayout.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Layout", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 12), new java.awt.Color(255, 255, 255))); // NOI18N
        panPreviewLayout.setLayout(new java.awt.BorderLayout());
        jSplitPane1.setRightComponent(panPreviewLayout);

        scrollSources.setBackground(new java.awt.Color(255, 255, 255));
        scrollSources.setBorder(javax.swing.BorderFactory.createTitledBorder("Video Sources"));

        tableSources.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                " ", "Source", "Description", "X", "Y", "Width", "Height", "Alpha"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, true, true, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tableSources.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        tableSources.setColumnSelectionAllowed(true);
        tableSources.setFillsViewportHeight(true);
        tableSources.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableSources.getTableHeader().setResizingAllowed(false);
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
            tableSources.getColumnModel().getColumn(0).setMinWidth(25);
            tableSources.getColumnModel().getColumn(0).setPreferredWidth(25);
            tableSources.getColumnModel().getColumn(0).setMaxWidth(25);
            tableSources.getColumnModel().getColumn(1).setMinWidth(100);
            tableSources.getColumnModel().getColumn(1).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(1).setMaxWidth(100);
            tableSources.getColumnModel().getColumn(2).setMinWidth(150);
            tableSources.getColumnModel().getColumn(2).setPreferredWidth(150);
            tableSources.getColumnModel().getColumn(2).setMaxWidth(2000);
            tableSources.getColumnModel().getColumn(3).setMinWidth(60);
            tableSources.getColumnModel().getColumn(3).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(3).setMaxWidth(60);
            tableSources.getColumnModel().getColumn(4).setMinWidth(60);
            tableSources.getColumnModel().getColumn(4).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(4).setMaxWidth(60);
            tableSources.getColumnModel().getColumn(5).setMinWidth(60);
            tableSources.getColumnModel().getColumn(5).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(5).setMaxWidth(60);
            tableSources.getColumnModel().getColumn(6).setMinWidth(60);
            tableSources.getColumnModel().getColumn(6).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(6).setMaxWidth(60);
            tableSources.getColumnModel().getColumn(7).setMinWidth(60);
            tableSources.getColumnModel().getColumn(7).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(7).setMaxWidth(60);
        }

        jSplitPane1.setLeftComponent(scrollSources);

        panSources.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        tabs.addTab("Sources", panSources);

        panSettingsAudios.setBorder(javax.swing.BorderFactory.createTitledBorder("Audio"));

        jLabel8.setText("Microphone Input");

        jLabel9.setText("Audio System Input");

        jLabel10.setText("Audio Delay");

        spinAudioDelay.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(-5.0f), Float.valueOf(5.0f), Float.valueOf(0.1f)));
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
                    .addComponent(cboAudioMicrophones, 0, 549, Short.MAX_VALUE)
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

        lblVideoFolder.setText(" ");

        btnSetVideoFolder.setText("Browse");
        btnSetVideoFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetVideoFolderActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panSettingsVideosLayout = new javax.swing.GroupLayout(panSettingsVideos);
        panSettingsVideos.setLayout(panSettingsVideosLayout);
        panSettingsVideosLayout.setHorizontalGroup(
            panSettingsVideosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSettingsVideosLayout.createSequentialGroup()
                .addComponent(lblVideoFolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSetVideoFolder)
                .addContainerGap())
        );
        panSettingsVideosLayout.setVerticalGroup(
            panSettingsVideosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSettingsVideosLayout.createSequentialGroup()
                .addGroup(panSettingsVideosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblVideoFolder)
                    .addComponent(btnSetVideoFolder))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout panOptionsLayout = new javax.swing.GroupLayout(panOptions);
        panOptions.setLayout(panOptionsLayout);
        panOptionsLayout.setHorizontalGroup(
            panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panSettingsAudios, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panSettingsVideos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panOptionsLayout.setVerticalGroup(
            panOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panSettingsAudios, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panSettingsVideos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(284, Short.MAX_VALUE))
        );

        tabs.addTab("Options", panOptions);

        getContentPane().add(tabs, java.awt.BorderLayout.CENTER);

        panStatus.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        panStatus.setPreferredSize(new java.awt.Dimension(767, 20));
        panStatus.setLayout(new javax.swing.BoxLayout(panStatus, javax.swing.BoxLayout.LINE_AXIS));

        lblMessages.setText("Welcome to ScreenStudio");
        panStatus.add(lblMessages);

        getContentPane().add(panStatus, java.awt.BorderLayout.SOUTH);

        mnuFile.setText("Layout");

        mnuCapture.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        mnuCapture.setText("Record");
        mnuCapture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuCaptureActionPerformed(evt);
            }
        });
        mnuFile.add(mnuCapture);
        mnuFile.add(jSeparator1);

        mnuFileLoad.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileLoad.setText("Open");
        mnuFileLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileLoadActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileLoad);

        mnuFileSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileSave.setText("Save");
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
        mnuMainAddImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainAddImageActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainAddImage);

        mnuMainAddLabel.setText("Add Label");
        mnuMainAddLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainAddLabelActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainAddLabel);
        mnuEdit.add(jSeparator3);

        mnuMainMoveUp.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.ALT_MASK));
        mnuMainMoveUp.setText("Move Up");
        mnuMainMoveUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainMoveUpActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainMoveUp);

        mnuMainMoveDown.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.ALT_MASK));
        mnuMainMoveDown.setText("Move Down");
        mnuMainMoveDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainMoveDownActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainMoveDown);
        mnuEdit.add(jSeparator2);

        mnuMainRemove.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        mnuMainRemove.setText("Remove Source");
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
                Editor ed = new Editor(tableSources.getValueAt(rowIndex, 2).toString(), this);
                ed.setModal(true);
                ed.setVisible(true);
                tableSources.setValueAt(ed.getText(), rowIndex, 2);
                tableSources.repaint();
            }
        }
        mLayoutPreview.repaint();
    }//GEN-LAST:event_tableSourcesMouseClicked

    private void spinWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinWidthStateChanged
        if (mLayoutPreview != null) {
            mLayoutPreview.setOutputWidth((Integer) spinWidth.getValue());
        }
    }//GEN-LAST:event_spinWidthStateChanged

    private void spinHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinHeightStateChanged
        if (mLayoutPreview != null) {
            mLayoutPreview.setOutputHeight((Integer) spinHeight.getValue());
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
            mFFMpeg.stop();
            try {
                Microphone.getVirtualAudio(null, null);
            } catch (IOException ex) {
                Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
            }
            mFFMpeg = null;
            mnuCapture.setText("Record");
            updateControls(true);
        } else {
            List<Source> sources = Compositor.getSources(tableSources, (Integer) spinFPS.getValue());
            Compositor compositor = new Compositor(sources, new Rectangle(0, 0, (Integer) spinWidth.getValue(), (Integer) spinHeight.getValue()), (Integer) spinFPS.getValue());
            mFFMpeg = new FFMpeg(compositor);
            String audio = "default";
            Microphone mic = null;
            Microphone sys = null;
            if (cboAudioMicrophones.getSelectedIndex() > 0) {
                mic = (Microphone) cboAudioMicrophones.getSelectedItem();
            }
            if (cboAudioSystems.getSelectedIndex() > 0) {
                sys = (Microphone) cboAudioSystems.getSelectedItem();
            }
            try {
                audio = Microphone.getVirtualAudio(mic, sys);
            } catch (IOException ex) {
                Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
            }
            mFFMpeg.setAudio((FFMpeg.AudioRate) cboAudioBitrate.getSelectedItem(), audio, (Float) spinAudioDelay.getValue());
            mFFMpeg.setPreset((FFMpeg.Presets) cboVideoPresets.getSelectedItem());
            mFFMpeg.setOutputFormat((FFMpeg.FORMATS) cboTarget.getSelectedItem(), (FFMpeg.Presets) cboVideoPresets.getSelectedItem(), (Integer) numVideoBitrate.getValue(), "", txtRTMPKey.getText(), mVideoOutputFolder);
            new Thread(mFFMpeg).start();
            lblMessages.setText("Recording...");
            mnuCapture.setText("Stop");
            updateControls(false);
            this.setExtendedState(JFrame.ICONIFIED);
            mRecordingTimestamp = System.currentTimeMillis();
            new Thread(() -> {
                while (mFFMpeg != null) {
                    long seconds = (System.currentTimeMillis() - mRecordingTimestamp) / 1000;
                    if (seconds < 60) {
                        setTitle("Recording! (" + seconds + " sec)");
                    } else {
                        setTitle("Recording! (" + (seconds / 60) + " min " + (seconds % 60) + " sec)");
                    }
                    if (mFFMpeg.getState() == FFMpeg.RunningState.Error) {
                        setExtendedState(JFrame.NORMAL);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                setTitle("ScreenStudio " + screenstudio.Version.MAIN);
            }).start();
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
                saveLayout(chooser.getSelectedFile());
                p.put("lastfolder", chooser.getSelectedFile().getParent());
                try {
                    p.flush();
                } catch (BackingStoreException ex) {
                    Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
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
                    Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
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
            mVideoOutputFolder = chooser.getSelectedFile();
            lblVideoFolder.setText(mVideoOutputFolder.getAbsolutePath());
            lblVideoFolder.setToolTipText(mVideoOutputFolder.getAbsolutePath());
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
            model.addRow(row);
        }
        mLayoutPreview.repaint();
        tabs.setSelectedComponent(panSources);
    }//GEN-LAST:event_mnuMainAddImageActionPerformed

    private void mnuMainAddLabelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuMainAddLabelActionPerformed
        DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
        Object[] row = new Object[model.getColumnCount()];
        row[0] = true;
        row[1] = SourceType.LabelText;
        row[2] = "<HTML><BODY>New Label...</BODY><HTML>";
        row[3] = 0;
        row[4] = 0;
        row[5] = 300;
        row[6] = 100;
        row[7] = 1.0f;
        model.addRow(row);
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
            mnuCapture.doClick();
        }
    }//GEN-LAST:event_formWindowStateChanged

    private void mnuMainRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuMainRemoveActionPerformed
        if (tableSources.getSelectedRow() != -1) {
            int index = tableSources.getSelectedRow();
            DefaultTableModel model = (DefaultTableModel) tableSources.getModel();
            model.removeRow(index);
            mLayoutPreview.repaint();
        }
    }//GEN-LAST:event_mnuMainRemoveActionPerformed

    private void spinAudioDelayStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinAudioDelayStateChanged
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("screenstudio");
        p.putFloat("audiodelay", (Float) spinAudioDelay.getValue());
        try {
            p.flush();
        } catch (BackingStoreException ex) {
            Logger.getLogger(MainVersion3.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_spinAudioDelayStateChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
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
            java.util.logging.Logger.getLogger(MainVersion3.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainVersion3.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainVersion3.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainVersion3.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainVersion3().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSetVideoFolder;
    private javax.swing.JComboBox<FFMpeg.AudioRate> cboAudioBitrate;
    private javax.swing.JComboBox<Microphone> cboAudioMicrophones;
    private javax.swing.JComboBox<Microphone> cboAudioSystems;
    private javax.swing.JComboBox<String> cboRTMPServers;
    private javax.swing.JComboBox<FFMpeg.FORMATS> cboTarget;
    private javax.swing.JComboBox<FFMpeg.Presets> cboVideoPresets;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
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
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel lblMessages;
    private javax.swing.JLabel lblRTMPKey;
    private javax.swing.JLabel lblRTMPServer;
    private javax.swing.JLabel lblVideoFolder;
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
    private javax.swing.JPanel panSettingsVideos;
    private javax.swing.JPanel panSources;
    private javax.swing.JPanel panStatus;
    private javax.swing.JPanel panTargetSettings;
    private javax.swing.JScrollPane scrollSources;
    private javax.swing.JSpinner spinAudioDelay;
    private javax.swing.JSpinner spinFPS;
    private javax.swing.JSpinner spinHeight;
    private javax.swing.JSpinner spinWidth;
    private javax.swing.JTable tableSources;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JTextField txtRTMPKey;
    // End of variables declaration//GEN-END:variables
}
