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
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
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
import screenstudio.gui.images.frames.Frames;
import screenstudio.panel.editor.Editor;
import screenstudio.remote.HTTPServer;
import screenstudio.sources.Compositor;
import screenstudio.sources.Microphone;
import screenstudio.sources.Screen;
import screenstudio.sources.Source;
import screenstudio.sources.SystemCheck;
import screenstudio.sources.Webcam;
import screenstudio.sources.effects.Effect;
import screenstudio.sources.transitions.Transition;
import screenstudio.targets.Layout;
import screenstudio.targets.Layout.SourceType;
import screenstudio.targets.Source.View;

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
    private java.util.ResourceBundle LANGUAGES = java.util.ResourceBundle.getBundle("screenstudio/Languages"); // NOI18N
    private Microphone mCurrentAudioMonitor = null;
    private Layout mCurrentLayout = new Layout();
    private ArrayList<screenstudio.targets.Source> mSources = new ArrayList<>();

    /**
     * Creates new form MainVersion3
     */
    public ScreenStudio() {
        initComponents();

        this.setIconImage(new ImageIcon(ScreenStudio.class.getResource("/screenstudio/gui/images/icon.png")).getImage());
        initControls();
        updateColumnsLayout();
        mLayoutPreview = new SourceLayoutPreview(tableSources, mSources);
        mLayoutPreview.setOutputWidth((Integer) spinWidth.getValue());
        mLayoutPreview.setOutputHeight((Integer) spinHeight.getValue());
        panPreviewLayout.add(mLayoutPreview, BorderLayout.CENTER);
        this.setTitle("ScreenStudio " + screenstudio.Version.MAIN);
        this.setSize(700, 450);
        ToolTipManager.sharedInstance().setDismissDelay(8000);
        ToolTipManager.sharedInstance().setInitialDelay(2000);
        new Thread(() -> {
            if (Version.hasNewVersion()) {
                lblMessages.setText(LANGUAGES.getString("MSG_NEW_VERSION_AVAILABLE"));
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
        mRemote = new HTTPServer(null, null, mnuCapture);
        new Thread(mRemote).start();
        try {
            lblRemoteMessage.setText(LANGUAGES.getString("REMOTE_ACCESS") + ": http://" + Inet4Address.getLocalHost().getHostName() + ".local:" + mRemote.getPort());
        } catch (UnknownHostException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ArrayList<screenstudio.targets.Source> getSources() {
        return mSources;
    }

    private void initControls() {
        updateRemoteSources();
        cboTarget.setModel(new DefaultComboBoxModel<>(FFMpeg.FORMATS.values()));
        cboTarget.setSelectedIndex(0);
        cboVideoPresets.setModel(new DefaultComboBoxModel<>(FFMpeg.Presets.values()));
        cboAudioBitrate.setModel(new DefaultComboBoxModel<>(FFMpeg.AudioRate.values()));
        cboAudioBitrate.setSelectedItem(FFMpeg.AudioRate.Audio44K);
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
            trayIcon = new TrayIcon(this.getIconImage(), LANGUAGES.getString("TIP_SCREENSTUDIO_DOUBLE_CLICK_TO_ACTIVATE_RECORDING"));
            if (Screen.isOSX()) {
                trayIcon.setToolTip(LANGUAGES.getString("TIP_SCREENSTUDIO_DOUBLE_CLICK_TO_ACTIVATE_RECORDING"));
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
        for (Effect.eEffects e : Effect.eEffects.values()) {
            popMnuSourceEffect.add(e.name());
        }
        for (int i = 0; i < popMnuSourceEffect.getItemCount(); i++) {
            JMenuItem m = popMnuSourceEffect.getItem(i);
            m.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (tableSources.getSelectedRow() != -1) {
                        tableSources.setValueAt(e.getActionCommand(), tableSources.getSelectedRow(), 12);
                    }
                }
            });
        }
        for (Frames.eList f : Frames.eList.values()) {
            JMenuItem menu = new JMenuItem(f.toString());
            menu.setActionCommand(f.name());
            mnuMainFrames.add(menu);
            menu.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    screenstudio.targets.Source source = new screenstudio.targets.Source(cboSourceViews.getItemCount());
                    source.getViews().add(new screenstudio.targets.Source.View());
                    source.setCurrentViewIndex(cboSourceViews.getSelectedIndex());

                    source.Views.get(source.CurrentViewIndex).remoteDisplay = true;
                    source.setType(SourceType.Frame);
                    source.setSourceObject(Frames.eList.valueOf(e.getActionCommand()));
                    source.Views.get(source.CurrentViewIndex).X = 0;
                    source.Views.get(source.CurrentViewIndex).Y = 0;
                    source.Views.get(source.CurrentViewIndex).Width = (Integer) spinWidth.getValue();
                    source.Views.get(source.CurrentViewIndex).Height = (Integer) spinHeight.getValue();
                    source.Views.get(source.CurrentViewIndex).Alpha = 1f;
                    source.Views.get(source.CurrentViewIndex).Order = mSources.size();
                    source.setStartTime(0L);
                    source.setEndTime(0L);
                    source.setTransitionStart(Transition.NAMES.None);
                    source.setTransitionStop(Transition.NAMES.None);
                    source.setEffect(Effect.eEffects.None);
                    source.initOtherViews();
                    bindingGroup.getBinding("MySource").unbind();
                    mSources.add(source);
                    bindingGroup.getBinding("MySource").bind();
                    updateColumnsLayout();
                    updateRemoteSources();
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

    private void selectCurrentView(int index) {
        bindingGroup.getBinding("MySource").unbind();
        for (screenstudio.targets.Source source : mSources) {
            source.setCurrentViewIndex(index);
        }
        mSources.sort((b, a) -> Integer.compare(b.getViews().get(cboSourceViews.getSelectedIndex()).Order, a.getViews().get(cboSourceViews.getSelectedIndex()).Order));
        bindingGroup.getBinding("MySource").bind();
        updateColumnsLayout();
        mRemote.setCurrentView(index);
    }

    public void updateColumnsLayout() {
        tableSources.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        if (tableSources.getColumnModel().getColumnCount() > 0) {
            //Show
            tableSources.getColumnModel().getColumn(0).setResizable(false);
            tableSources.getColumnModel().getColumn(0).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(0).setHeaderValue(LANGUAGES.getString("SHOW_SOURCE")); // NOI18N

            //Type
            tableSources.getColumnModel().getColumn(1).setResizable(false);
            tableSources.getColumnModel().getColumn(1).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(1).setHeaderValue(LANGUAGES.getString("SOURCE_TYPE")); // NOI18N

            //Source
            tableSources.getColumnModel().getColumn(2).setMinWidth(150);
            tableSources.getColumnModel().getColumn(2).setPreferredWidth(200);
            tableSources.getColumnModel().getColumn(2).setHeaderValue(LANGUAGES.getString("SOURCE")); // NOI18N

            //X
            tableSources.getColumnModel().getColumn(3).setResizable(false);
            tableSources.getColumnModel().getColumn(3).setPreferredWidth(75);
            //Y
            tableSources.getColumnModel().getColumn(4).setResizable(false);
            tableSources.getColumnModel().getColumn(4).setPreferredWidth(75);
            //Width
            tableSources.getColumnModel().getColumn(5).setResizable(false);
            tableSources.getColumnModel().getColumn(5).setPreferredWidth(75);
            //Height
            tableSources.getColumnModel().getColumn(6).setResizable(false);
            tableSources.getColumnModel().getColumn(6).setPreferredWidth(75);
            //Alpha
            tableSources.getColumnModel().getColumn(7).setResizable(false);
            tableSources.getColumnModel().getColumn(7).setPreferredWidth(75);
            //Start
            tableSources.getColumnModel().getColumn(8).setResizable(false);
            tableSources.getColumnModel().getColumn(8).setPreferredWidth(75);
            tableSources.getColumnModel().getColumn(8).setHeaderValue(LANGUAGES.getString("START_TIME")); // NOI18N
            //End
            tableSources.getColumnModel().getColumn(9).setResizable(false);
            tableSources.getColumnModel().getColumn(9).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(9).setHeaderValue(LANGUAGES.getString("END_TIME")); // NOI18N
            //Transition In
            tableSources.getColumnModel().getColumn(10).setResizable(false);
            tableSources.getColumnModel().getColumn(10).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(10).setHeaderValue(LANGUAGES.getString("TRANSITION_IN")); // NOI18N
            ComboBoxCellEditor edti = new ComboBoxCellEditor(new JComboBox(Transition.NAMES.values()));
            tableSources.getColumnModel().getColumn(10).setCellEditor(edti);
            //Transition Out
            tableSources.getColumnModel().getColumn(11).setResizable(false);
            tableSources.getColumnModel().getColumn(11).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(11).setHeaderValue(LANGUAGES.getString("TRANSITION_OUT")); // NOI18N
            ComboBoxCellEditor edto = new ComboBoxCellEditor(new JComboBox(Transition.NAMES.values()));
            tableSources.getColumnModel().getColumn(11).setCellEditor(edto);
            //Effect
            tableSources.getColumnModel().getColumn(12).setResizable(false);
            tableSources.getColumnModel().getColumn(12).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(12).setHeaderValue(LANGUAGES.getString("EFFECT")); // NOI18N
            ComboBoxCellEditor edef = new ComboBoxCellEditor(new JComboBox(Effect.eEffects.values()));
            tableSources.getColumnModel().getColumn(12).setCellEditor(edef);

        }

    }

    private void loadLayout(File file) {
        mCurrentLayout = new Layout();
        bindingGroup.getBinding("MySource").unbind();
        try {
            mCurrentLayout.load(file);
            cboAudioBitrate.setSelectedItem(mCurrentLayout.getAudioBitrate());
            for (int i = 0; i < cboAudioMicrophones.getItemCount(); i++) {
                if (cboAudioMicrophones.getItemAt(i).getDescription().equals(mCurrentLayout.getAudioMicrophone())) {
                    cboAudioMicrophones.setSelectedIndex(i);
                    break;
                }
            }
            for (int i = 0; i < cboAudioSystems.getItemCount(); i++) {
                if (cboAudioSystems.getItemAt(i).getDescription().equals(mCurrentLayout.getAudioSystem())) {
                    cboAudioSystems.setSelectedIndex(i);
                    break;
                }
            }
            spinFPS.setValue(mCurrentLayout.getOutputFramerate());
            spinHeight.setValue(mCurrentLayout.getOutputHeight());
            cboVideoPresets.setSelectedItem(mCurrentLayout.getOutputPreset());
            cboTarget.setSelectedItem(mCurrentLayout.getOutputTarget());
            txtRTMPKey.setText(mCurrentLayout.getOutputRTMPKey());
            cboRTMPServers.setSelectedItem(mCurrentLayout.getOutputRTMPServer());
            mVideoOutputFolder = mCurrentLayout.getOutputVideoFolder();
            txtVideoFolder.setText(mVideoOutputFolder);
            txtVideoFolder.setToolTipText(mVideoOutputFolder);
            spinWidth.setValue(mCurrentLayout.getOutputWidth());
            numVideoBitrate.setValue(mCurrentLayout.getVideoBitrate());
            mBackgroundMusic = mCurrentLayout.getBackgroundMusic();
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
            mSources.clear();
            cboSourceViews.setSelectedIndex(0);
            for (screenstudio.targets.Source s : mCurrentLayout.getSources()) {
                while (s.getViews().size() < cboSourceViews.getItemCount()) {
                    screenstudio.targets.Source.View v = new screenstudio.targets.Source.View();
                    v.X = s.getViews().get(0).X;
                    v.Y = s.getViews().get(0).Y;
                    v.Width = s.getViews().get(0).Width;
                    v.Height = s.getViews().get(0).Height;
                    v.Order = s.getViews().get(0).Order;
                    v.Alpha = s.getViews().get(0).Alpha;
                    v.remoteDisplay = s.getViews().get(0).remoteDisplay;
                    s.getViews().add(v);
                }
                switch (s.getType()) {
                    case Desktop:
                        Screen[] screens = Screen.getSources();
                        for (Screen screen : screens) {
                            if (screen.getLabel().equals(s.getID())) {
                                s.setSourceObject(screen);
                                if (s.getCaptureX() != 0 || s.getCaptureY() != 0) {
                                    screen.getSize().width = s.getViews().get(s.getCurrentViewIndex()).Width;
                                    screen.getSize().height = s.getViews().get(s.getCurrentViewIndex()).Height;
                                    screen.getSize().x = s.getCaptureX();
                                    screen.getSize().y = s.getCaptureY();
                                }
                                break;
                            }
                        }
                        break;
                    case Image:
                        s.setSourceObject(new File(s.getID()));
                        break;
                    case LabelText:
                        LabelText t = new LabelText(s.getID());
                        t.setForegroundColor(s.getForegroundColor());
                        t.setBackgroundColor(s.getBackgroundColor());
                        t.setFontName(s.getFontName());
                        s.setSourceObject(t);
                        break;
                    case Webcam:
                        for (Webcam webcam : webcams) {
                            if (webcam.getDevice().equals(s.getID())) {
                                s.setSourceObject(webcam);
                                break;
                            }
                        }
                        break;
                    case Frame:
                        s.setSourceObject(Frames.eList.valueOf(s.getID()));
                        break;
                }
                mSources.add(s);
            }
            mSources.sort((b, a) -> Integer.compare(b.getViews().get(cboSourceViews.getSelectedIndex()).Order, a.getViews().get(cboSourceViews.getSelectedIndex()).Order));
        } catch (IOException | ParserConfigurationException | SAXException | InterruptedException ex) {
            Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
        }
        bindingGroup.getBinding("MySource").bind();
        updateColumnsLayout();
        updateRemoteSources();
        panPreviewLayout.repaint();
    }

    private void saveLayout(File file) {
        mCurrentLayout = new Layout();
        mCurrentLayout.setAudioBitrate(cboAudioBitrate.getItemAt(cboAudioBitrate.getSelectedIndex()));
        mCurrentLayout.setAudioMicrophone(cboAudioMicrophones.getItemAt(cboAudioMicrophones.getSelectedIndex()).getDescription());
        mCurrentLayout.setAudioSystem(cboAudioSystems.getItemAt(cboAudioSystems.getSelectedIndex()).getDescription());
        mCurrentLayout.setOutputFramerate((Integer) spinFPS.getValue());
        mCurrentLayout.setOutputHeight((Integer) spinHeight.getValue());
        mCurrentLayout.setOutputPreset(cboVideoPresets.getItemAt(cboVideoPresets.getSelectedIndex()));
        mCurrentLayout.setOutputRTMPKey(txtRTMPKey.getText());
        if (cboRTMPServers.getSelectedIndex() != -1) {
            mCurrentLayout.setOutputRTMPServer(cboRTMPServers.getSelectedItem().toString());
        } else {
            mCurrentLayout.setOutputRTMPServer("");
        }
        mCurrentLayout.setOutputTarget(cboTarget.getItemAt(cboTarget.getSelectedIndex()));
        mCurrentLayout.setOutputVideoFolder(mVideoOutputFolder);
        mCurrentLayout.setOutputWith((Integer) spinWidth.getValue());
        mCurrentLayout.setVideoBitrate((Integer) numVideoBitrate.getValue());
        mCurrentLayout.setBackgroundMusic(mBackgroundMusic);
        mSources.sort((b, a) -> Integer.compare(b.getViews().get(0).Order, a.getViews().get(0).Order));
        for (int i = 0; i < mSources.size(); i++) {
            screenstudio.targets.Source s = mSources.get(i);
            if (s.getSourceObject() instanceof File) {
                s.setID(((File) s.getSourceObject()).getAbsolutePath());
            } else if (s.getSourceObject() instanceof Screen) {
                s.setID(((Screen) s.getSourceObject()).getLabel());
            } else if (s.getSourceObject() instanceof Frames.eList) {
                s.setID(((Frames.eList) s.getSourceObject()).name());
            } else if (s.getSourceObject() instanceof Webcam) {
                s.setID(((Webcam) s.getSourceObject()).getDevice());
            } else if (s.getSourceObject() instanceof LabelText) {
                s.setID(((LabelText) s.getSourceObject()).getText());
            }
            mCurrentLayout.addSource(s);
        }
        try {
            mCurrentLayout.save(file);
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
                case TIMELAPSE:
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
                menu.setToolTipText(LANGUAGES.getString("DEVICE") + ": " + w.getDevice());
                menu.addActionListener((ActionEvent e) -> {
                    try {
                        for (Webcam webcam : Webcam.getSources()) {
                            if (webcam.getDevice().equals(e.getActionCommand())) {
                                screenstudio.targets.Source source = new screenstudio.targets.Source(cboSourceViews.getItemCount());
                                source.setCurrentViewIndex(cboSourceViews.getSelectedIndex());;
                                source.Views.get(source.CurrentViewIndex).remoteDisplay = true;
                                source.setType(SourceType.Webcam);
                                source.setSourceObject(webcam);
                                source.Views.get(source.CurrentViewIndex).X = 0;
                                source.Views.get(source.CurrentViewIndex).Y = 0;
                                source.Views.get(source.CurrentViewIndex).Width = webcam.getWidth();
                                source.Views.get(source.CurrentViewIndex).Height = webcam.getHeight();
                                source.Views.get(source.CurrentViewIndex).Alpha = 1f;
                                source.Views.get(source.CurrentViewIndex).Order = mSources.size();
                                source.setStartTime(0L);
                                source.setEndTime(0L);
                                source.setTransitionStart(Transition.NAMES.None);
                                source.setTransitionStop(Transition.NAMES.None);
                                source.setEffect(Effect.eEffects.None);
                                source.initOtherViews();
                                bindingGroup.getBinding("MySource").unbind();
                                mSources.add(source);
                                bindingGroup.getBinding("MySource").bind();
                                updateColumnsLayout();
                                updateRemoteSources();
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
        for (int i = 0; i < mSources.size(); i++) {
            sources.add(mSources.get(i).getID());
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
                menu.setToolTipText(LANGUAGES.getString("SIZE") + ": " + s.getDetailledLabel());
                menu.addActionListener((ActionEvent e) -> {
                    try {
                        for (Screen screen : Screen.getSources()) {
                            if (screen.getLabel().equals(e.getActionCommand())) {
                                screenstudio.targets.Source source = new screenstudio.targets.Source(cboSourceViews.getItemCount());
                                source.setCurrentViewIndex(cboSourceViews.getSelectedIndex());;
                                source.Views.get(source.CurrentViewIndex).remoteDisplay = true;
                                source.setType(SourceType.Desktop);
                                source.setSourceObject(screen);
                                source.Views.get(source.CurrentViewIndex).X = 0;
                                source.Views.get(source.CurrentViewIndex).Y = 0;
                                source.Views.get(source.CurrentViewIndex).Width = (int) spinWidth.getValue();
                                source.Views.get(source.CurrentViewIndex).Height = (int) spinHeight.getValue();
                                source.Views.get(source.CurrentViewIndex).Alpha = 1f;
                                source.Views.get(source.CurrentViewIndex).Order = mSources.size();
                                source.setStartTime(0L);
                                source.setEndTime(0L);
                                source.setTransitionStart(Transition.NAMES.None);
                                source.setTransitionStop(Transition.NAMES.None);
                                source.setEffect(Effect.eEffects.None);
                                source.initOtherViews();
                                bindingGroup.getBinding("MySource").unbind();
                                mSources.add(source);
                                bindingGroup.getBinding("MySource").bind();
                                updateColumnsLayout();
                                updateRemoteSources();
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
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        popSources = new javax.swing.JPopupMenu();
        popMnuSourceTransitionIn = new javax.swing.JMenu();
        popMnuSourceTransitionOut = new javax.swing.JMenu();
        popMnuSourceEffect = new javax.swing.JMenu();
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
        panSourcesViews = new javax.swing.JPanel();
        lblSourceViewsCount = new javax.swing.JLabel();
        cboSourceViews = new javax.swing.JComboBox<>();
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
        pgAudioLevels = new javax.swing.JProgressBar();
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
        mnuMainFrames = new javax.swing.JMenu();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        mnuMainMoveUp = new javax.swing.JMenuItem();
        mnuMainMoveDown = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        mnuMainRemove = new javax.swing.JMenuItem();

        popMnuSourceTransitionIn.setText("Transition In");
        popSources.add(popMnuSourceTransitionIn);

        popMnuSourceTransitionOut.setText("Transition Out");
        popSources.add(popMnuSourceTransitionOut);

        popMnuSourceEffect.setLabel("Effects");
        popSources.add(popMnuSourceEffect);

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

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("screenstudio/Languages"); // NOI18N
        jLabel1.setText(bundle.getString("OUTPUT_FORMAT")); // NOI18N

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

        jLabel3.setText(bundle.getString("FRAME_RATE")); // NOI18N

        spinFPS.setModel(new javax.swing.SpinnerNumberModel(10, 1, 60, 1));

        jLabel4.setText(bundle.getString("TARGET")); // NOI18N

        cboTarget.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboTargetActionPerformed(evt);
            }
        });

        panTargetSettings.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("SETTINGS"))); // NOI18N

        jLabel5.setText(bundle.getString("VIDEO_BITRATE")); // NOI18N

        jLabel6.setText(bundle.getString("VIDEO_PRESET")); // NOI18N

        jLabel7.setText(bundle.getString("AUDIO_BITRATE")); // NOI18N

        lblRTMPServer.setText(bundle.getString("RTMP_SERVER")); // NOI18N

        lblRTMPKey.setText(bundle.getString("RTMP_SECRET_KEY")); // NOI18N

        numVideoBitrate.setModel(new javax.swing.SpinnerNumberModel(1000, 1, 9000, 50));

        javax.swing.GroupLayout panTargetSettingsLayout = new javax.swing.GroupLayout(panTargetSettings);
        panTargetSettings.setLayout(panTargetSettingsLayout);
        panTargetSettingsLayout.setHorizontalGroup(
            panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panTargetSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panTargetSettingsLayout.createSequentialGroup()
                        .addComponent(lblRTMPKey, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRTMPKey, javax.swing.GroupLayout.PREFERRED_SIZE, 435, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panTargetSettingsLayout.createSequentialGroup()
                        .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)
                            .addComponent(lblRTMPServer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(cboAudioBitrate, 0, 162, Short.MAX_VALUE)
                            .addComponent(cboRTMPServers, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(panTargetSettingsLayout.createSequentialGroup()
                        .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panTargetSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(numVideoBitrate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cboVideoPresets, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE))))
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

        chkKeepScreenRatio.setText(bundle.getString("KEEP_SCREEN_RATIO")); // NOI18N

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

        tabs.addTab(bundle.getString("OUTPUT"), panOutput); // NOI18N

        panSources.setLayout(new java.awt.BorderLayout());

        panSourcesViews.setMaximumSize(new java.awt.Dimension(212, 52));

        lblSourceViewsCount.setText(bundle.getString("SELECTED_VIEW")); // NOI18N

        cboSourceViews.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Default", "View 1", "View 2", "View 3", "View 4" }));
        cboSourceViews.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboSourceViewsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panSourcesViewsLayout = new javax.swing.GroupLayout(panSourcesViews);
        panSourcesViews.setLayout(panSourcesViewsLayout);
        panSourcesViewsLayout.setHorizontalGroup(
            panSourcesViewsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panSourcesViewsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblSourceViewsCount)
                .addGap(18, 18, 18)
                .addComponent(cboSourceViews, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(370, Short.MAX_VALUE))
        );
        panSourcesViewsLayout.setVerticalGroup(
            panSourcesViewsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panSourcesViewsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panSourcesViewsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSourceViewsCount)
                    .addComponent(cboSourceViews, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        panSources.add(panSourcesViews, java.awt.BorderLayout.NORTH);

        splitterSources.setDividerLocation(150);
        splitterSources.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        panPreviewLayout.setBackground(new java.awt.Color(51, 51, 51));
        panPreviewLayout.setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("LAYOUT"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 12), new java.awt.Color(255, 255, 255))); // NOI18N
        panPreviewLayout.setLayout(new java.awt.BorderLayout());
        splitterSources.setRightComponent(panPreviewLayout);

        scrollSources.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("VIDEO_SOURCES"))); // NOI18N

        tableSources.setToolTipText("Double-click for more options...");
        tableSources.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        tableSources.setColumnSelectionAllowed(true);
        tableSources.setComponentPopupMenu(popSources);
        tableSources.setFillsViewportHeight(true);
        tableSources.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableSources.setSurrendersFocusOnKeystroke(true);
        tableSources.getTableHeader().setReorderingAllowed(false);

        org.jdesktop.beansbinding.ELProperty eLProperty = org.jdesktop.beansbinding.ELProperty.create("${sources}");
        org.jdesktop.swingbinding.JTableBinding jTableBinding = org.jdesktop.swingbinding.SwingBindings.createJTableBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, this, eLProperty, tableSources, "MySource");
        org.jdesktop.swingbinding.JTableBinding.ColumnBinding columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${display}"));
        columnBinding.setColumnName("Display");
        columnBinding.setColumnClass(Boolean.class);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${type}"));
        columnBinding.setColumnName("Type");
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${sourceObject}"));
        columnBinding.setColumnName("Source Object");
        columnBinding.setEditable(false);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${x}"));
        columnBinding.setColumnName("X");
        columnBinding.setColumnClass(Integer.class);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${y}"));
        columnBinding.setColumnName("Y");
        columnBinding.setColumnClass(Integer.class);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${width}"));
        columnBinding.setColumnName("Width");
        columnBinding.setColumnClass(Integer.class);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${height}"));
        columnBinding.setColumnName("Height");
        columnBinding.setColumnClass(Integer.class);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${alpha}"));
        columnBinding.setColumnName("Alpha");
        columnBinding.setColumnClass(Float.class);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${startTime}"));
        columnBinding.setColumnName("Start Time");
        columnBinding.setColumnClass(Long.class);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${endTime}"));
        columnBinding.setColumnName("End Time");
        columnBinding.setColumnClass(Long.class);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${transitionStart}"));
        columnBinding.setColumnName("Transition Start");
        columnBinding.setColumnClass(Transition.NAMES.class);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${transitionStop}"));
        columnBinding.setColumnName("Transition Stop");
        columnBinding.setColumnClass(Transition.NAMES.class);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${effect}"));
        columnBinding.setColumnName("Effect");
        columnBinding.setColumnClass(Effect.eEffects.class);
        bindingGroup.addBinding(jTableBinding);
        jTableBinding.bind();
        tableSources.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableSourcesMouseClicked(evt);
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
            tableSources.getColumnModel().getColumn(0).setPreferredWidth(60);
            tableSources.getColumnModel().getColumn(0).setHeaderValue(bundle.getString("SHOW_SOURCE")); // NOI18N
            tableSources.getColumnModel().getColumn(1).setResizable(false);
            tableSources.getColumnModel().getColumn(1).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("SOURCE_TYPE")); // NOI18N
            tableSources.getColumnModel().getColumn(2).setMinWidth(150);
            tableSources.getColumnModel().getColumn(2).setPreferredWidth(200);
            tableSources.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("SOURCE")); // NOI18N
            tableSources.getColumnModel().getColumn(3).setResizable(false);
            tableSources.getColumnModel().getColumn(3).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(4).setResizable(false);
            tableSources.getColumnModel().getColumn(4).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(5).setResizable(false);
            tableSources.getColumnModel().getColumn(5).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(6).setResizable(false);
            tableSources.getColumnModel().getColumn(6).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(7).setResizable(false);
            tableSources.getColumnModel().getColumn(7).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(8).setResizable(false);
            tableSources.getColumnModel().getColumn(8).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(8).setHeaderValue(bundle.getString("START_TIME")); // NOI18N
            tableSources.getColumnModel().getColumn(9).setResizable(false);
            tableSources.getColumnModel().getColumn(9).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(9).setHeaderValue(bundle.getString("END_TIME")); // NOI18N
            tableSources.getColumnModel().getColumn(10).setResizable(false);
            tableSources.getColumnModel().getColumn(10).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(10).setHeaderValue(bundle.getString("TRANSITION_IN")); // NOI18N
            tableSources.getColumnModel().getColumn(10).setCellEditor(null);
            tableSources.getColumnModel().getColumn(11).setResizable(false);
            tableSources.getColumnModel().getColumn(11).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(11).setHeaderValue(bundle.getString("TRANSITION_OUT")); // NOI18N
            tableSources.getColumnModel().getColumn(11).setCellEditor(null);
            tableSources.getColumnModel().getColumn(12).setResizable(false);
            tableSources.getColumnModel().getColumn(12).setPreferredWidth(100);
            tableSources.getColumnModel().getColumn(12).setHeaderValue(bundle.getString("EFFECT")); // NOI18N
        }

        splitterSources.setLeftComponent(scrollSources);

        panSources.add(splitterSources, java.awt.BorderLayout.CENTER);

        tabs.addTab(bundle.getString("SOURCES"), panSources); // NOI18N

        panSettingsAudios.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("AUDIO"))); // NOI18N

        jLabel8.setText(bundle.getString("MICROPHONE_INPUT")); // NOI18N

        jLabel9.setText(bundle.getString("AUDIO_SYSTEM_INPUT")); // NOI18N

        cboAudioMicrophones.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboAudioMicrophonesActionPerformed(evt);
            }
        });

        jLabel10.setText(bundle.getString("AUDIO_DELAY")); // NOI18N

        spinAudioDelay.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(-5.0f), Float.valueOf(5.0f), Float.valueOf(0.1f)));
        spinAudioDelay.setToolTipText("<HTML><BODY>\nApply a delay (in seconds) to the audio.\n<BR><I>If video is late, apply a positive value...</I>\n</BODY></HTML>");
        spinAudioDelay.setEditor(new javax.swing.JSpinner.NumberEditor(spinAudioDelay, "#.#"));
        spinAudioDelay.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinAudioDelayStateChanged(evt);
            }
        });

        pgAudioLevels.setMaximum(255);

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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pgAudioLevels, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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
                .addGroup(panSettingsAudiosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(panSettingsAudiosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel10)
                        .addComponent(spinAudioDelay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(pgAudioLevels, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panSettingsVideos.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("VIDEO_FOLDER"))); // NOI18N

        btnSetVideoFolder.setText(bundle.getString("BROWSE")); // NOI18N
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

        panSettingsMisc.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("MISCELANEOUS"))); // NOI18N

        jLabel11.setText(bundle.getString("ACTION_WHEN_RECORDING")); // NOI18N

        cboDefaultRecordingAction.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Hide", "Minimize", "Stay Visible" }));
        cboDefaultRecordingAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboDefaultRecordingActionActionPerformed(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(102, 102, 102));
        jLabel12.setText(bundle.getString("TIP_KEYBOARD_SHORTCUT_GLOBAL")); // NOI18N

        chkDoNotUseTrayIcon.setText(bundle.getString("ACTION_DO_NOT_USE_TRAY_ICON")); // NOI18N
        chkDoNotUseTrayIcon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkDoNotUseTrayIconActionPerformed(evt);
            }
        });

        jLabel13.setText(bundle.getString("ACTION_WHEN_STARTING")); // NOI18N

        jLabel14.setText(bundle.getString("BACKGROUND_MUSIC")); // NOI18N

        lblBGMusic.setText(" ");
        lblBGMusic.setToolTipText("<html>\n<body>\nSelect an audio file to play in the background<br>\nSet the proper audio volume and duration using a software like <b>Audacity</b><br>\n<i>Tip: Make the duration last a bit longer than your recording to have a background music for all the lenght of your video</i>\n</body>\n</html>");

        btnBGMusicBrowse.setText(bundle.getString("BROWSE")); // NOI18N
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

        tabs.addTab(bundle.getString("OPTIONS"), panOptions); // NOI18N

        getContentPane().add(tabs, java.awt.BorderLayout.CENTER);
        tabs.getAccessibleContext().setAccessibleName(bundle.getString("OUTPUT")); // NOI18N

        panStatus.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        panStatus.setPreferredSize(new java.awt.Dimension(767, 20));
        panStatus.setLayout(new java.awt.GridLayout(1, 0));

        lblMessages.setText(bundle.getString("WELCOME_SCREENSTUDIO")); // NOI18N
        panStatus.add(lblMessages);

        lblRemoteMessage.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblRemoteMessage.setText("...");
        panStatus.add(lblRemoteMessage);
        lblRemoteMessage.getAccessibleContext().setAccessibleName("");
        lblRemoteMessage.getAccessibleContext().setAccessibleDescription("");

        getContentPane().add(panStatus, java.awt.BorderLayout.SOUTH);

        mnuFile.setText(bundle.getString("MAIN_MENU_LAYOUT")); // NOI18N

        mnuCapture.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        mnuCapture.setText(bundle.getString("ACTION_RECORD")); // NOI18N
        mnuCapture.setToolTipText("<html><body>\nStart recording/streaming using CTRL-R.  \n<BR><B>ScreenStudio</B> will automatically hide in the taskbar of your system.  \n<BR>To stop the recording, simply restore the <B>ScreenStudio</B> window.\n</body></html>");
        mnuCapture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuCaptureActionPerformed(evt);
            }
        });
        mnuFile.add(mnuCapture);
        mnuFile.add(jSeparator1);

        mnuFileLoad.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileLoad.setText(bundle.getString("ACTION_OPEN")); // NOI18N
        mnuFileLoad.setToolTipText("<HTML><BODY>\nOpen a <B>ScreenStudio</B> XML layout file\n</BODY></HTML>");
        mnuFileLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileLoadActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileLoad);

        mnuFileSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileSave.setText(bundle.getString("ACTION_SAVE")); // NOI18N
        mnuFileSave.setToolTipText("<HTML><BODY>\nSave the current layour to a <B>ScreenStudio</B> XML layout file\n</BODY></HTML>");
        mnuFileSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileSaveActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileSave);

        menuBar.add(mnuFile);

        mnuEdit.setText(bundle.getString("MAIN_MENU_SOURCES")); // NOI18N

        mnuMainWebcams.setText(bundle.getString("WEBCAMS")); // NOI18N
        mnuEdit.add(mnuMainWebcams);

        mnuMainDestops.setText(bundle.getString("DESKTOPS")); // NOI18N
        mnuEdit.add(mnuMainDestops);

        mnuMainAddImage.setText(bundle.getString("MENU_ADD_IMAGES")); // NOI18N
        mnuMainAddImage.setToolTipText("Browse your hard disk to add a source image file");
        mnuMainAddImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainAddImageActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainAddImage);

        mnuMainAddLabel.setText(bundle.getString("MENU_ADD_LABEL")); // NOI18N
        mnuMainAddLabel.setToolTipText("<HTML><BODY>\nAdd a new text label.  \n<BR>Double-click on the source to edit the content.\n</BODY></HTML>");
        mnuMainAddLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainAddLabelActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainAddLabel);

        mnuMainFrames.setText(bundle.getString("ACTION_ADD_FRAMES")); // NOI18N
        mnuEdit.add(mnuMainFrames);
        mnuEdit.add(jSeparator3);

        mnuMainMoveUp.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.ALT_MASK));
        mnuMainMoveUp.setText(bundle.getString("MOVE_UP")); // NOI18N
        mnuMainMoveUp.setToolTipText("Move the currently selected source to a higher layer");
        mnuMainMoveUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainMoveUpActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainMoveUp);

        mnuMainMoveDown.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.ALT_MASK));
        mnuMainMoveDown.setText(bundle.getString("MOVE_DOWN")); // NOI18N
        mnuMainMoveDown.setToolTipText("Move the currently selected source to a lower layer");
        mnuMainMoveDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainMoveDownActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainMoveDown);
        mnuEdit.add(jSeparator2);

        mnuMainRemove.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        mnuMainRemove.setText(bundle.getString("REMOVE_SOURCE")); // NOI18N
        mnuMainRemove.setToolTipText("Remove the currently selected source");
        mnuMainRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuMainRemoveActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuMainRemove);

        menuBar.add(mnuEdit);

        setJMenuBar(menuBar);

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tableSourcesKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tableSourcesKeyPressed
        mLayoutPreview.repaint();
    }//GEN-LAST:event_tableSourcesKeyPressed

    private void tableSourcesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSourcesMouseClicked
        if (evt.getClickCount() == 2) {
            int rowIndex = tableSources.getSelectedRow();
            screenstudio.targets.Source source = mSources.get(rowIndex);
            if (source.Type == SourceType.LabelText) {
                LabelText t = ((LabelText) tableSources.getValueAt(rowIndex, 2));
                Editor ed = new Editor(t, this);
                ed.setModal(true);
                ed.setVisible(true);
                tableSources.setValueAt(ed.getLabelText(), rowIndex, 2);
                source.setFontName(t.getFontName());
                source.setForegroundColor(t.getForegroundColor());
                source.setBackgroundColor(t.getBackgroundColor());
                tableSources.repaint();
            } else if (source.Type == SourceType.Desktop) {
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
                lblMessages.setText(LANGUAGES.getString("STOPPED"));
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
                    mnuCapture.setText(LANGUAGES.getString("ACTION_RECORD"));
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
            if (mCurrentAudioMonitor != null) {
                mCurrentAudioMonitor.stopMonitoring();
                mCurrentAudioMonitor = null;
            }

            if (trayIcon != null) {
                trayIcon.setImage(new ImageIcon(ScreenStudio.class.getResource("/screenstudio/gui/images/iconStarting.png")).getImage());
            }
            if (txtRTMPKey.isVisible()) {
                txtRTMPKey.setVisible(false);
            }
            boolean abort = false;
            if (tableSources.getRowCount() == 0) {
                lblMessages.setText(LANGUAGES.getString("WARNING_NO_VIDEO_SOURCE"));
                abort = true;
            }
            if (cboTarget.getSelectedItem() != FFMpeg.FORMATS.GIF && cboTarget.getSelectedItem() != FFMpeg.FORMATS.TIMELAPSE && cboAudioMicrophones.getSelectedIndex() == 0 && cboAudioSystems.getSelectedIndex() == 0) {
                lblMessages.setText(LANGUAGES.getString("WARNING_NO_AUDIO_SOURCE"));
                abort = true;
            }
            if (!abort) {
                List<Source> sources = Compositor.getSources(mSources, (Integer) spinFPS.getValue());
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

                lblMessages.setText(LANGUAGES.getString("RECORDING"));

                mnuCapture.setText(LANGUAGES.getString("STOP"));

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
                            setTitle(LANGUAGES.getString("RECORDING") + "! (" + seconds + " sec)");
                        } else {
                            setTitle(LANGUAGES.getString("RECORDING") + "! (" + (seconds / 60) + " min " + (seconds % 60) + " sec)");
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
            screenstudio.targets.Source source = new screenstudio.targets.Source(cboSourceViews.getItemCount());
            source.setCurrentViewIndex(cboSourceViews.getSelectedIndex());
            source.Views.get(source.CurrentViewIndex).remoteDisplay = true;
            source.setType(SourceType.Image);
            source.setSourceObject(image);
            source.Views.get(source.CurrentViewIndex).X = 0;
            source.Views.get(source.CurrentViewIndex).Y = 0;
            source.Views.get(source.CurrentViewIndex).Width = 200;
            source.Views.get(source.CurrentViewIndex).Height = 200;
            source.Views.get(source.CurrentViewIndex).Alpha = 1f;
            source.Views.get(source.CurrentViewIndex).Order = mSources.size();
            source.setStartTime(0L);
            source.setEndTime(0L);
            source.setTransitionStart(Transition.NAMES.None);
            source.setTransitionStop(Transition.NAMES.None);
            source.setEffect(Effect.eEffects.None);
            source.initOtherViews();
            bindingGroup.getBinding("MySource").unbind();
            mSources.add(source);
            bindingGroup.getBinding("MySource").bind();
            updateColumnsLayout();
            updateRemoteSources();
        }
    }//GEN-LAST:event_mnuMainAddImageActionPerformed

    private void mnuMainAddLabelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuMainAddLabelActionPerformed

        screenstudio.targets.Source source = new screenstudio.targets.Source(cboSourceViews.getItemCount());
        source.setCurrentViewIndex(cboSourceViews.getSelectedIndex());
        source.Views.get(source.CurrentViewIndex).remoteDisplay = true;
        source.setType(SourceType.LabelText);
        source.setSourceObject(new LabelText("<HTML><BODY>New Label...</BODY><HTML>"));
        source.Views.get(source.CurrentViewIndex).X = 0;
        source.Views.get(source.CurrentViewIndex).Y = 0;
        source.Views.get(source.CurrentViewIndex).Width = 600;
        source.Views.get(source.CurrentViewIndex).Height = 100;
        source.Views.get(source.CurrentViewIndex).Alpha = 1f;
        source.Views.get(source.CurrentViewIndex).Order = mSources.size();
        source.setStartTime(0L);
        source.setEndTime(0L);
        source.setTransitionStart(Transition.NAMES.None);
        source.setTransitionStop(Transition.NAMES.None);
        source.setEffect(Effect.eEffects.None);
        source.initOtherViews();
        bindingGroup.getBinding("MySource").unbind();
        mSources.add(source);
        bindingGroup.getBinding("MySource").bind();
        updateColumnsLayout();
        updateRemoteSources();

    }//GEN-LAST:event_mnuMainAddLabelActionPerformed

    private void mnuMainMoveUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuMainMoveUpActionPerformed
        if (tableSources.getSelectedRow() != -1 && tableSources.getSelectedRow() > 0) {
            int index = tableSources.getSelectedRow();
            int indexCol = tableSources.getSelectedColumn();
            screenstudio.targets.Source source1 = mSources.get(index);
            screenstudio.targets.Source source2 = mSources.get(index - 1);
            source1.Views.get(source1.CurrentViewIndex).Order = index - 1;
            source2.Views.get(source2.CurrentViewIndex).Order = index;
            bindingGroup.getBinding("MySource").unbind();
            mSources.sort((b, a) -> Integer.compare(b.getViews().get(cboSourceViews.getSelectedIndex()).Order, a.getViews().get(cboSourceViews.getSelectedIndex()).Order));
            bindingGroup.getBinding("MySource").bind();
            updateColumnsLayout();
            tableSources.setRowSelectionInterval(index - 1, index - 1);
            tableSources.setColumnSelectionInterval(indexCol, indexCol);
            tableSources.requestFocus();
            mLayoutPreview.repaint();
            updateRemoteSources();
        }
    }//GEN-LAST:event_mnuMainMoveUpActionPerformed

    private void mnuMainMoveDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuMainMoveDownActionPerformed
        if (tableSources.getSelectedRow() != -1 && tableSources.getSelectedRow() < tableSources.getRowCount() - 1) {
            int index = tableSources.getSelectedRow();
            int indexCol = tableSources.getSelectedColumn();
            screenstudio.targets.Source source1 = mSources.get(index);
            screenstudio.targets.Source source2 = mSources.get(index + 1);
            source1.Views.get(source1.CurrentViewIndex).Order = index + 1;
            source2.Views.get(source2.CurrentViewIndex).Order = index;
            bindingGroup.getBinding("MySource").unbind();
            mSources.sort((b, a) -> Integer.compare(b.getViews().get(cboSourceViews.getSelectedIndex()).Order, a.getViews().get(cboSourceViews.getSelectedIndex()).Order));
            bindingGroup.getBinding("MySource").bind();
            updateColumnsLayout();
            tableSources.setRowSelectionInterval(index + 1, index + 1);
            tableSources.setColumnSelectionInterval(indexCol, indexCol);
            mLayoutPreview.repaint();
            updateRemoteSources();
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
            bindingGroup.getBinding("MySource").unbind();
            mSources.remove(index);
            for (int i = 0; i < mSources.size(); i++) {
                for (View v : mSources.get(i).Views) {
                    v.Order = i;
                }
            }
            mSources.sort((b, a) -> Integer.compare(b.getViews().get(cboSourceViews.getSelectedIndex()).Order, a.getViews().get(cboSourceViews.getSelectedIndex()).Order));
            bindingGroup.getBinding("MySource").bind();
            updateColumnsLayout();
            updateRemoteSources();
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
        if (mCurrentAudioMonitor != null) {
            mCurrentAudioMonitor.stopMonitoring();
            mCurrentAudioMonitor = null;
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

    private void cboAudioMicrophonesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboAudioMicrophonesActionPerformed

        if (cboAudioMicrophones.getSelectedItem() != null) {
            Microphone m = (Microphone) cboAudioMicrophones.getSelectedItem();
            if (mCurrentAudioMonitor != null) {
                mCurrentAudioMonitor.stopMonitoring();
                mCurrentAudioMonitor = null;
                pgAudioLevels.setValue(0);
            }
            if (m.getDevice() != null) {
                mCurrentAudioMonitor = m;
                m.startMonitoring();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (mCurrentAudioMonitor != null) {
                            pgAudioLevels.setValue(mCurrentAudioMonitor.getCurrentAudioLevel());
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(ScreenStudio.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }).start();

            }
        }
    }//GEN-LAST:event_cboAudioMicrophonesActionPerformed

    private void cboSourceViewsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboSourceViewsActionPerformed
        if (bindingGroup.getBinding("MySource").isBound()) {
            selectCurrentView(cboSourceViews.getSelectedIndex());
        }
        mLayoutPreview.repaint();
    }//GEN-LAST:event_cboSourceViewsActionPerformed

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
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ScreenStudio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

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
    private javax.swing.JComboBox<String> cboSourceViews;
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
    private javax.swing.JLabel lblSourceViewsCount;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem mnuCapture;
    private javax.swing.JMenu mnuEdit;
    private javax.swing.JMenu mnuFile;
    private javax.swing.JMenuItem mnuFileLoad;
    private javax.swing.JMenuItem mnuFileSave;
    private javax.swing.JMenuItem mnuMainAddImage;
    private javax.swing.JMenuItem mnuMainAddLabel;
    private javax.swing.JMenu mnuMainDestops;
    private javax.swing.JMenu mnuMainFrames;
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
    private javax.swing.JPanel panSourcesViews;
    private javax.swing.JPanel panStatus;
    private javax.swing.JPanel panTargetSettings;
    private javax.swing.JProgressBar pgAudioLevels;
    private javax.swing.JMenu popMnuSourceEffect;
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
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

}
