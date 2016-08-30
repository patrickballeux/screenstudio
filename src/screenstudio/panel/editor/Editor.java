/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package screenstudio.panel.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import screenstudio.gui.LabelText;

/**
 *
 * @author patrick
 */
public class Editor extends javax.swing.JDialog {

    String mUserContent = "";
    File currentFile = null;
    LabelText returnContent;
    boolean isModified = false;
    private JColorChooser foreground;
    private JColorChooser background;

    /**
     * Creates new form Main
     *
     * @param file
     * @param userContent
     */
    public Editor(File file, String userContent, int panelWidth) {
        initComponents();
        currentFile = file;

        try {
            this.setIconImage(new ImageIcon(this.getClass().getResource("icon.png").toURI().toURL()).getImage());
        } catch (URISyntaxException | MalformedURLException ex) {
            Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
        }
        mUserContent = userContent;
        if (file != null) {
            txtEditor.setText(getFileContent(file));

        }
        txtEditor.setText(capitalizeKeyworkds(txtEditor.getText()));
        try {
            lblPreview.setText(replaceTags(txtEditor.getText()));
        } catch (RuntimeException ex) {
            //do nothing...
        }
        lblPreview.setPreferredSize(new Dimension(panelWidth + (lblPreview.getBorder().getBorderInsets(lblPreview).right * 2), 500));
        setSize(800, 500);
    }

    public Editor(LabelText content, JFrame owner) {
        super(owner);
        initComponents();
        currentFile = null;
        returnContent = content;
        foreground = new JColorChooser(new Color(returnContent.getForegroundColor()));
        tabs.add("Foreground color", foreground);

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        cboFonts.setModel(model);
        lblPreview.setFont(new Font(content.getFontName(),Font.BOLD,16));
        cboFonts.setSelectedItem(content.getFontName());
        foreground.getSelectionModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                lblPreview.setForeground(((ColorSelectionModel) e.getSource()).getSelectedColor());
                returnContent.setForegroundColor(lblPreview.getForeground().getRGB());
            }
        });

        background = new JColorChooser(new Color(returnContent.getBackgroundColor()));
        tabs.add("Background color", background);
        background.getSelectionModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                lblPreview.setBackground(((ColorSelectionModel) e.getSource()).getSelectedColor());
                returnContent.setBackgroundColor(lblPreview.getBackground().getRGB());
            }
        });

        try {
            this.setIconImage(new ImageIcon(this.getClass().getResource("icon.png").toURI().toURL()).getImage());
        } catch (URISyntaxException | MalformedURLException ex) {
            Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
        }
        mUserContent = "";
        txtEditor.setText(content.getText());
        txtEditor.setText(capitalizeKeyworkds(txtEditor.getText()));
        try {
            lblPreview.setText(replaceTags(txtEditor.getText()));
        } catch (RuntimeException ex) {
            //do nothing...
        }
        lblPreview.setForeground(foreground.getColor());
        lblPreview.setBackground(background.getColor());
        lblPreview.setOpaque(true);
        lblPreview.setPreferredSize(new Dimension(200 + (lblPreview.getBorder().getBorderInsets(lblPreview).right * 2), 300));
        setSize(600, 600);

    }

    public LabelText getLabelText() {
        return returnContent;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblPreview = new javax.swing.JLabel();
        tabs = new javax.swing.JTabbedPane();
        scrollEditor = new javax.swing.JScrollPane();
        txtEditor = new javax.swing.JTextArea();
        cboFonts = new javax.swing.JComboBox<>();
        menu = new javax.swing.JMenuBar();
        mnuFile = new javax.swing.JMenu();
        mnuFileOpen = new javax.swing.JMenuItem();
        mnuFileSave = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mnuFileExit = new javax.swing.JMenuItem();
        mnuEdit = new javax.swing.JMenu();
        mnuEditCopy = new javax.swing.JMenuItem();
        mnuEditCut = new javax.swing.JMenuItem();
        mnuEditPaste = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Screen Studio Panel Editor");
        setModal(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        lblPreview.setFont(new java.awt.Font("Ubuntu", 0, 12)); // NOI18N
        lblPreview.setText("<html><body>... Template ...</body></html>");
        lblPreview.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblPreview.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));
        lblPreview.setMaximumSize(new java.awt.Dimension(320, 9999));
        lblPreview.setMinimumSize(new java.awt.Dimension(320, 500));
        lblPreview.setPreferredSize(new java.awt.Dimension(320, 36));
        getContentPane().add(lblPreview, java.awt.BorderLayout.EAST);

        scrollEditor.setBorder(javax.swing.BorderFactory.createTitledBorder("HTML Code"));

        txtEditor.setColumns(20);
        txtEditor.setFont(new java.awt.Font("Ubuntu", 0, 12)); // NOI18N
        txtEditor.setRows(5);
        txtEditor.setText("<html>\n<body width=300 height=768>\n... Template ...\n</body>\n</html>");
        txtEditor.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtEditorKeyReleased(evt);
            }
        });
        scrollEditor.setViewportView(txtEditor);

        tabs.addTab("Text", scrollEditor);

        getContentPane().add(tabs, java.awt.BorderLayout.CENTER);

        cboFonts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboFontsActionPerformed(evt);
            }
        });
        getContentPane().add(cboFonts, java.awt.BorderLayout.PAGE_END);

        mnuFile.setText("File");

        mnuFileOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileOpen.setText("Open");
        mnuFileOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileOpenActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileOpen);

        mnuFileSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileSave.setText("Save");
        mnuFileSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileSaveActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileSave);
        mnuFile.add(jSeparator1);

        mnuFileExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileExit.setText("Exit");
        mnuFileExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileExitActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileExit);

        menu.add(mnuFile);

        mnuEdit.setText("Edit");

        mnuEditCopy.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditCopy.setText("Copy");
        mnuEditCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditCopyActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditCopy);

        mnuEditCut.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditCut.setText("Cut");
        mnuEditCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditCutActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditCut);

        mnuEditPaste.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditPaste.setText("Paste");
        mnuEditPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditPasteActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditPaste);

        menu.add(mnuEdit);

        setJMenuBar(menu);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private String getFileContent(File file) {
        String text = "";
        try {
            InputStream in = file.toURI().toURL().openStream();
            byte[] data = new byte[(int) file.length()];
            in.read(data);
            in.close();
            text = new String(data).trim();
        } catch (IOException ex) {
            Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return text;
    }

    private DateFormat formatDate = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    private DateFormat formatTime = DateFormat.getTimeInstance(DateFormat.LONG, Locale.getDefault());

    private String replaceTags(String text) {
        String retValue = text + "";
        retValue = retValue.replaceAll("@TEXT", mUserContent);
        retValue = retValue.replaceAll("@CURRENTDATE", formatDate.format(new Date()));
        retValue = retValue.replaceAll("@CURRENTTIME", formatTime.format(new Date()));
        retValue = retValue.replaceAll("@RECORDINGTIME", "17 min");
        retValue = retValue.replaceAll("@STARTTIME", formatTime.format(new Date(System.currentTimeMillis() - 600000)));
        retValue = retValue.replaceAll("@REMAININGTIME", "23 min");
        return retValue;
    }
    private void mnuFileOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuFileOpenActionPerformed
        JFileChooser chooser = new JFileChooser(currentFile);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".html") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "HTML file";
            }
        });
        chooser.setDialogTitle("Select HTML file to open...");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.showOpenDialog(this);
        File selected = chooser.getSelectedFile();
        if (selected != null) {
            txtEditor.setText(getFileContent(selected));
            currentFile = selected;
        }
    }//GEN-LAST:event_mnuFileOpenActionPerformed

    private void mnuFileSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuFileSaveActionPerformed
        JFileChooser chooser = new JFileChooser(currentFile);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".html") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "HTML file";
            }
        });
        chooser.setDialogTitle("Select HTML file to save...");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.showSaveDialog(this);
        File selected = chooser.getSelectedFile();
        if (selected != null) {
            try {
                FileWriter writer = new FileWriter(selected);
                writer.write(txtEditor.getText());
                writer.close();
                currentFile = selected;
                isModified = false;
            } catch (IOException ex) {
                Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }//GEN-LAST:event_mnuFileSaveActionPerformed

    private void mnuFileExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuFileExitActionPerformed
        //Add validation if content was not saved...
        returnContent = new LabelText(txtEditor.getText());
        returnContent.setForegroundColor(foreground.getColor().getRGB());
        returnContent.setBackgroundColor(background.getColor().getRGB());
        this.dispose();
    }//GEN-LAST:event_mnuFileExitActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        returnContent = new LabelText(txtEditor.getText());
        returnContent.setForegroundColor(foreground.getColor().getRGB());
        returnContent.setBackgroundColor(background.getColor().getRGB());
        returnContent.setFontName(cboFonts.getSelectedItem().toString());
        if (this.currentFile != null && isModified) {
            // warn saving...
            EditorWarning confirm = new EditorWarning(null, true);
            confirm.setLocationRelativeTo(this);
            confirm.setVisible(true);
            if (confirm.getReturnStatus() == EditorWarning.RET_OK) {
                this.mnuFileSave.doClick();
            }
        }
    }//GEN-LAST:event_formWindowClosing

    private void mnuEditCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditCopyActionPerformed
        String textSelected = txtEditor.getSelectedText();
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(textSelected);
        clpbrd.setContents(stringSelection, null);
    }//GEN-LAST:event_mnuEditCopyActionPerformed

    private void mnuEditCutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditCutActionPerformed
        String textSelected = txtEditor.getSelectedText();
        int start = txtEditor.getSelectionStart();
        int end = txtEditor.getSelectionEnd();
        txtEditor.replaceRange("", start, end);
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(textSelected);
        clpbrd.setContents(stringSelection, null);
    }//GEN-LAST:event_mnuEditCutActionPerformed

    private void mnuEditPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditPasteActionPerformed
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable clipData = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(clipboard);
        if (clipData != null) {
            try {
                if (clipData.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String s = (String) (clipData.getTransferData(
                            DataFlavor.stringFlavor));
                    txtEditor.insert(s, txtEditor.getCaretPosition());
                }
            } catch (UnsupportedFlavorException ufe) {
                System.err.println("Flavor unsupported: " + ufe);
            } catch (IOException ioe) {
                System.err.println("Data not available: " + ioe);
            }
        }
    }//GEN-LAST:event_mnuEditPasteActionPerformed

    private void txtEditorKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtEditorKeyReleased
        try {
            // To prevent runtime exception when quotation is not closed...
            if (evt.getKeyCode() != KeyEvent.VK_QUOTE && evt.getKeyCode() != KeyEvent.VK_QUOTEDBL) {
                lblPreview.setText(replaceTags(txtEditor.getText()));
            }
        } catch (RuntimeException ex) {
            //do nothing...
        }
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_GREATER:
                isModified = true;
                int pos = txtEditor.getCaretPosition();
                String html = capitalizeKeyworkds(txtEditor.getText());
                txtEditor.setText(html);
                txtEditor.setCaretPosition(pos);
                break;
            default:
                isModified = isModified || (evt.getKeyChar() + "").trim().length() == 1;
                break;
        }
        lblPreview.repaint();
    }//GEN-LAST:event_txtEditorKeyReleased

    private void cboFontsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboFontsActionPerformed
        returnContent.setFontName(cboFonts.getSelectedItem().toString());
        lblPreview.setFont(new Font(cboFonts.getSelectedItem().toString(),lblPreview.getFont().getStyle(),lblPreview.getFont().getSize()));
    }//GEN-LAST:event_cboFontsActionPerformed

    private final static String[] TAGS = {
        "html", "body", "p", "hr", "br", "table", "th", "tr", "td", "span", "div", "a", "thead", "head", "h",
        "ul", "li", "div", "style", "img"
    };

    private String capitalizeKeyworkds(String html) {
        for (String t : TAGS) {
            html = html.replaceAll("<" + t, "<" + t.toUpperCase()).replaceAll("</" + t, "</" + t.toUpperCase());
        }
        return html;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> cboFonts;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JLabel lblPreview;
    private javax.swing.JMenuBar menu;
    private javax.swing.JMenu mnuEdit;
    private javax.swing.JMenuItem mnuEditCopy;
    private javax.swing.JMenuItem mnuEditCut;
    private javax.swing.JMenuItem mnuEditPaste;
    private javax.swing.JMenu mnuFile;
    private javax.swing.JMenuItem mnuFileExit;
    private javax.swing.JMenuItem mnuFileOpen;
    private javax.swing.JMenuItem mnuFileSave;
    private javax.swing.JScrollPane scrollEditor;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JTextArea txtEditor;
    // End of variables declaration//GEN-END:variables
}
