// ***************************************************************************
// *   Copyright (C) 2015 by Chavdar Levkov                              
// *   ch.levkov@gmail.com                                                   
// *                                                                         
// *   This program is free software; you can redistribute it and/or modify  
// *   it under the terms of the GNU General Public License as published by  
// *   the Free Software Foundation; either version 2 of the License, or     
// *   (at your option) any later version.                                   
// *                                                                         
// *   This program is distributed in the hope that it will be useful,       
// *   but WITHOUT ANY WARRANTY; without even the implied warranty of        
// *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
// *   GNU General Public License for more details.                          
// *                                                                         
// *   You should have received a copy of the GNU General Public License     
// *   along with this program; if not, write to the                         
// *   Free Software Foundation, Inc.,                                       
// *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             
// ***************************************************************************
package org.lz1aq.lzlog;

import org.lz1aq.keyer.KeyerTypes;
import java.awt.FontMetrics;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import jssc.SerialPortList;
import org.lz1aq.keyer.Keyer;
import org.lz1aq.keyer.KeyerFactory;
import org.lz1aq.keyer.VoiceKeyer;
import org.lz1aq.log.Log;
import org.lz1aq.log.LogDatabase;
import org.lz1aq.log.LogListener;
import org.lz1aq.log.LogTableModel;
import org.lz1aq.log.Qso;
import org.lz1aq.ptt.DtrRtsPtt;
import org.lz1aq.ptt.Ptt;
import org.lz1aq.ptt.PttTypes;
import org.lz1aq.utils.FontChooser;
import org.lz1aq.utils.Misc;
import org.lz1aq.radio.RadioModes;
import org.lz1aq.utils.CommUtils;
import org.lz1aq.utils.MorseCode;
import org.lz1aq.utils.RcvFormatter;
import org.lz1aq.utils.SerialportShare;
import org.lz1aq.utils.TimeUtils;

/**
 *
 * @author potty
 */
public class MainWindow extends javax.swing.JFrame
{

    static final String PROGRAM_VERSION = "1.91";
    static final String PROGRAM_NAME = "LZ-Log";
    static final String PROGRAM_ABOUT = "LZ-log is a program designed for Bulgarian hamradio contests including the lzhfqrp. \nIt is written in Java+Python and the source code is available at https://github.com/potty-dzmeia/LZ-log \n\n73 de LZ1ABC/Chav";

    static final int SERIAL_NUMBER_MAX_LENGTH = 15;

    private Log log;
    private LogTableModel jtablemodelLog;
    private TimeToNextQsoTableModel jtablemodelIncomingQso;
    private BandmapTableModel jtablemodelBandmap;
    private final ApplicationSettings settings;
    private final RadioController radioController;
    private Keyer keyer;
    private VoiceKeyer voiceKeyer;
    private Ptt ptt;
    private int cqFrequency = 3500000;
    private int keyerSpeed = 28;
    private final Timer timer1sec;
    private final Timer timer500ms;
    private Timer timerContinuousCq;
    private Timer timerRadioPolling;
    private FontChooser fontchooser = new FontChooser();
    private String logDbFile;
    private String pathToWorkingDir; // where the jar file is located
    private SerialportShare serialportShare = new SerialportShare();

    private DocumentFilter callsignFilter = new CallsignDocumentFilter();
    private DocumentFilter serialNumberFilter = new SerialNumberDocumentFilter();
    private DocumentFilter pttDelayFilter = new PttDelayDocumentFilter();
    private DocumentFilter qsoRepeatPeriodFilter = new DigitsOnlyFilter();
    private DocumentFilter dontShowAfterFilter = new DigitsOnlyFilter();

    private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());
    
    private final ActionListener timer1secListener = new ActionListener()
    {
        @Override
        public void actionPerformed(ActionEvent evt)
        {
            jtablemodelIncomingQso.refresh();
            jtablemodelBandmap.refresh(getBandmapStartFreq());
        }
    };

    private final ActionListener timer500msListener = new ActionListener()
    {
        @Override
        public void actionPerformed(ActionEvent evt)
        {
            // On every second update the callsign status
            String status = getCallsignStatusText(getCallsignFromTextField());
            jlabelCallsignStatus.setText(status);
        }
    };

    private final ActionListener timerContinuousCqListener = new ActionListener()
    {
        @Override
        public void actionPerformed(ActionEvent evt)
        {
            pressedF1();
        }
    };

    private final ActionListener timerRadioPollingListener = new ActionListener()
    {
        @Override
        public void actionPerformed(ActionEvent evt)
        {
            radioController.poll();
        }
    };

    public MainWindow()
    {
        LOGGER.setLevel(Level.WARNING);
        
        determineWorkingDir();

        // Create directory for logs if not existing
        File directory = new File(Paths.get(pathToWorkingDir, "/logs").toString());
        if(!directory.exists())
        {
            directory.mkdir();
        }

        // Load user settings from the properties file
        this.settings = new ApplicationSettings();

        // Init GUI
        initComponents();

        // Show dialog for opening New/Existing log - result will be kept in logDbFile
        if(((LogSelectionDialog) jdialogLogSelection).showDialog())
        {
            System.exit(0); // Close program if Showdialog tells us to do so
        }

        // Open log database
        try
        {
            Qso example = new Qso(14190000, RadioModes.CW, "lz1abc", "lz0fs", "200091", "200091", "cq"); // We need to supply an example QSO whwn creating/opening new
            log = new Log(new LogDatabase(logDbFile), example);
        } catch(Exception ex)
        {
            LOGGER.log(Level.SEVERE, "Couldn't open the log database!", ex);
        }

        log.addEventListener(new LogListener()
        {
            @Override
            public void eventInit()
            {
            }

            @Override
            public void eventQsoAdded(Qso qso)
            { 
            }

            @Override
            public void eventQsoRemoved(Qso qso)
            {
                initEntryFields(); // We need to update the Snt field in case we deleted the last contact
            }

            @Override
            public void eventQsoModified(Qso qso)
            {
                initEntryFields(); // We need to update the Snt field in case we deleted the last contact
            }
        });
        
        // Init TableModels
        jtablemodelLog = new LogTableModel(log);
        jtablemodelLog.setInvisible(4); // Hide myCall
        jtableLog.setModel(jtablemodelLog);

        jtablemodelIncomingQso = new TimeToNextQsoTableModel(log, settings);
        jtableIncomingQso.setModel(jtablemodelIncomingQso);

        jtablemodelBandmap = new BandmapTableModel(log, getBandmapStartFreq(), settings);
        jtableBandmap.setModel(jtablemodelBandmap);

        // Renderer for the bandmap
        jtableBandmap.setDefaultRenderer(Object.class, new BandmapTableCellRender());
        jtableIncomingQso.setDefaultRenderer(Object.class, new IncomingQsoTableCellRender());
        jtableLog.setDefaultRenderer(Object.class, new LogTableCellRender());

        // Communicating with the radio
        radioController = new RadioController();
        keyer = radioController.getKeyer(); // Radio will be the default keyer

        // This is used for catching global key presses (i.e. needed for F1-F12 presses)
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new MyDispatcher());

        // Prepare the entry fields to have the necessary data
        initEntryFields();

        // Callsign text field should show capital letters only
        ((AbstractDocument) jtextfieldCallsign.getDocument()).setDocumentFilter(callsignFilter);
        // Serial number should be 6 digits long
        ((AbstractDocument) jtextfieldSnt.getDocument()).setDocumentFilter(serialNumberFilter);
        ((AbstractDocument) jtextfieldRcv.getDocument()).setDocumentFilter(serialNumberFilter);
        ((AbstractDocument) jTextFieldPttDelay.getDocument()).setDocumentFilter(pttDelayFilter);
        ((AbstractDocument) jTextFieldPttTailDelay.getDocument()).setDocumentFilter(pttDelayFilter);
        ((AbstractDocument) jtextfieldQsoRepeatPeriod.getDocument()).setDocumentFilter(qsoRepeatPeriodFilter);
        ((AbstractDocument) jTextFieldTimeToNextQso.getDocument()).setDocumentFilter(dontShowAfterFilter);

        // Needed so that jTable to scroll automatically upon entering a new Qso
        jtableLog.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                jtableLog.scrollRectToVisible(jtableLog.getCellRect(jtableLog.getRowCount() - 1, 0, true));
            }
        });

        jtableBandmap.setShowGrid(true);
        // Hihglighting for active text fields 
        jtextfieldCallsign.addFocusListener(highlighter);
        jtextfieldSnt.addFocusListener(highlighter);
        jtextfieldRcv.addFocusListener(highlighter);

        // Timer for refreshing Bandmap and TimeToNextQso windows
        timer1sec = new Timer(1000, timer1secListener);
        timer1sec.setRepeats(true);
        timer1sec.start();

        // Timer for updating the status of the callsign (Dupe, new etc..)
        timer500ms = new Timer(300, timer500msListener);
        timer500ms.setRepeats(true);
        timer500ms.start();

        //
        timerContinuousCq = new Timer(6000, timerContinuousCqListener);
        timerContinuousCq.setRepeats(false);

        voiceKeyer = new VoiceKeyer();
    }

    public void resizeFreqColumnWidth(JTable table, int size)
    {
        final TableColumnModel columnModel = table.getColumnModel();

        for(int column = 0; column < table.getColumnCount(); column++)
        {
            // If frequency column
            if(settings.isShowBandmapFreqColumns() && (column % 2 == 0))
            {
                columnModel.getColumn(column).setPreferredWidth(size);
            }
        }
    }

    private DefaultComboBoxModel getBaudRates()
    {
        return CommUtils.BaudRate.getComboxModel();
    }

    private DefaultComboBoxModel getBandsComboboxModel()
    {
        return new DefaultComboBoxModel(new String[]
        {
            "1.8", "3.5", "7", "14", "21", "28"
        });
    }

    private DefaultComboBoxModel getModeComboboxModel()
    {
        return RadioModes.getComboxModel();
    }

    private DefaultComboBoxModel getBandmapStepInHzComboboxModel()
    {
        return new DefaultComboBoxModel(new String[]
        {
            "100", "200", "500"
        });
    }

    private DefaultComboBoxModel getBandmapColumnCountComboboxModel()
    {
        return new DefaultComboBoxModel(new String[]
        {
            "8", "10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "32", "34", "36", "38", "40", "42", "44", "46", "48", "50", "52", "54", "56", "58", "60", "62", "64", "66", "68", "70", "72", "74", "76", "78", "80", "82", "84", "86", "88"
        });
    }

    private DefaultComboBoxModel getBandmapRowCountComboboxModel()
    {
        return new DefaultComboBoxModel(new String[]
        {
            "8", "10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "32", "34", "36", "38", "40", "42", "44", "46", "48", "50", "52", "54", "56", "58", "60", "62", "64", "66", "68", "70", "72", "74", "76", "78", "80", "82", "84", "86", "88"
        });
    }

    private class LogSelectionDialog extends JDialog
    {

        public boolean isProgramTerminated = false;

        public boolean showDialog()
        {
            this.setVisible(true);
            return isProgramTerminated;
        }
    }

    FocusListener highlighter = new FocusListener()
    {

        @Override
        public void focusGained(FocusEvent e)
        {
            e.getComponent().setBackground(Color.white);
        }

        @Override
        public void focusLost(FocusEvent e)
        {
            e.getComponent().setBackground(Color.lightGray);
        }

    };

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroupTypeOfWork = new javax.swing.ButtonGroup();
        buttonGroupRadioDtr = new javax.swing.ButtonGroup();
        buttonGroupRadioRts = new javax.swing.ButtonGroup();
        jDialogSettings = new javax.swing.JDialog();
        jPanel1 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jButtonCancel = new javax.swing.JButton();
        jButtonSave = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelCallSign = new javax.swing.JPanel();
        textfieldSettingsMyCallsign = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jPanelRadio = new javax.swing.JPanel();
        jComboBoxRadioComPort = new javax.swing.JComboBox();
        jLabel12 = new javax.swing.JLabel();
        jComboBoxRadioComPortBaudRate = new javax.swing.JComboBox();
        jLabel20 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jRadioButtonRadioDtrOn = new javax.swing.JRadioButton();
        jRadioButtonRadioDtrOff = new javax.swing.JRadioButton();
        jRadioButtonRadioRtsOn = new javax.swing.JRadioButton();
        jRadioButtonRadioRtsOff = new javax.swing.JRadioButton();
        jLabel27 = new javax.swing.JLabel();
        jPanelKeyerAndPtt = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jComboBoxKeyerComPort = new javax.swing.JComboBox();
        jLabel25 = new javax.swing.JLabel();
        jComboBoxKeyerType = new javax.swing.JComboBox<>();
        jPanel4 = new javax.swing.JPanel();
        jLabel26 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jComboBoxPttCommport = new javax.swing.JComboBox<>();
        jComboBoxPttType = new javax.swing.JComboBox<>();
        jTextFieldPttDelay = new javax.swing.JTextField();
        jLabel35 = new javax.swing.JLabel();
        jTextFieldPttTailDelay = new javax.swing.JTextField();
        jPanelFunctionKeys = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jtextfieldfF1 = new javax.swing.JTextField();
        jLabel31 = new javax.swing.JLabel();
        jTextFieldF2 = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jtextfieldfF3 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jtextfieldfF6 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jtextfieldfF7 = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jtextfieldF8 = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jtextfieldF9 = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jtextfieldF10 = new javax.swing.JTextField();
        jPanelContestRules = new javax.swing.JPanel();
        jLabel34 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        jtextfieldQsoRepeatPeriod = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldContestExchange = new javax.swing.JTextField();
        jLabel32 = new javax.swing.JLabel();
        jPanelOther = new javax.swing.JPanel();
        checkboxSettingsQuickMode = new javax.swing.JCheckBox();
        textfieldSettingsDefaultPrefix = new javax.swing.JTextField();
        checkboxSendLeadingZeroAsT = new javax.swing.JCheckBox();
        checkboxSendZeroAsT = new javax.swing.JCheckBox();
        checkboxESM = new javax.swing.JCheckBox();
        jLabel10 = new javax.swing.JLabel();
        jTextFieldTimeToNextQso = new javax.swing.JTextField();
        jCheckBoxAutoBandmapStartFreq = new javax.swing.JCheckBox();
        jDialogFontChooser = new javax.swing.JDialog();
        jPanel10 = new javax.swing.JPanel();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jButton15 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jButton19 = new javax.swing.JButton();
        jdialogLogSelection = new LogSelectionDialog();
        jbuttonCreateNewLog = new javax.swing.JButton();
        jbuttonOpenExistingLog = new javax.swing.JButton();
        jDialogAbout = new javax.swing.JDialog();
        jPanel13 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton20 = new javax.swing.JButton();
        jDesktopPane1 = new javax.swing.JDesktopPane();
        intframeTimeToNextQso = new javax.swing.JInternalFrame();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtableIncomingQso = new javax.swing.JTable();
        intframeBandmap = new javax.swing.JInternalFrame();
        jScrollPane5 = new javax.swing.JScrollPane();
        jtableBandmap = new javax.swing.JTable();
        jPanel8 = new javax.swing.JPanel();
        jcomboboxStepInHz = new javax.swing.JComboBox<>();
        jcomboboxColumnCount = new javax.swing.JComboBox<>();
        jcomboboxRowCount = new javax.swing.JComboBox<>();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jtextfieldFreqWidth = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jtextfieldBandmapStartFreq = new javax.swing.JTextField();
        jlabelBandmapFreeSpace = new javax.swing.JLabel();
        jCheckBoxShowFreq = new javax.swing.JCheckBox();
        intframeLog = new javax.swing.JInternalFrame();
        jpanelCompleteLog = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtableLog = new javax.swing.JTable();
        jbuttonDeleteEntry = new javax.swing.JButton();
        intframeRadioConnection = new javax.swing.JInternalFrame();
        jpanelRadioConnection = new javax.swing.JPanel();
        jtogglebuttonConnectToRadio = new javax.swing.JToggleButton();
        jtextfieldFrequency = new javax.swing.JTextField();
        jtextfieldMode = new javax.swing.JTextField();
        jcheckboxRadioPolling = new javax.swing.JCheckBox();
        jLabel17 = new javax.swing.JLabel();
        jtextfieldPollingTime = new javax.swing.JTextField();
        jpanelKeyerConnection = new javax.swing.JPanel();
        jtogglebuttonConnectToKeyer = new javax.swing.JToggleButton();
        intframeEntryWindow = new javax.swing.JInternalFrame();
        jpanelCallsign = new javax.swing.JPanel();
        jtextfieldCallsign = new javax.swing.JTextField();
        jtextfieldSnt = new javax.swing.JTextField();
        jtextfieldRcv = new javax.swing.JTextField();
        jpanelTypeOfWork = new javax.swing.JPanel();
        jradiobuttonCQ = new javax.swing.JRadioButton();
        jradiobuttonSP = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jcomboboxMode = new javax.swing.JComboBox();
        jcomboboxBand = new javax.swing.JComboBox();
        jpanelFunctionKeys = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jPanelStatusBar = new javax.swing.JPanel();
        jlabelCallsignStatus = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabelStatus = new javax.swing.JLabel();
        intframeMisc = new javax.swing.JInternalFrame();
        jScrollPane3 = new javax.swing.JScrollPane();
        jPanel11 = new javax.swing.JPanel();
        jpanelCqSettings = new javax.swing.JPanel();
        jbuttonJumpToCqFreq = new javax.swing.JButton();
        jcheckboxF1jumpsToCq = new javax.swing.JCheckBox();
        jlabelCqFreq = new javax.swing.JLabel();
        jbuttonSetCqFreq = new javax.swing.JButton();
        jcheckboxContinuousCq = new javax.swing.JCheckBox();
        jLabel16 = new javax.swing.JLabel();
        jtextfieldContinuousCqPeriod = new javax.swing.JTextField();
        jpanelKeyerSettings = new javax.swing.JPanel();
        jbuttonKeyerUP = new javax.swing.JButton();
        jbuttonKeyerDown = new javax.swing.JButton();
        jlabelKeyerSpeed = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jmenuGenerateCabrillo = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jmenuSettings = new javax.swing.JMenuItem();
        jmenuFonts = new javax.swing.JMenuItem();
        jmenuWindows = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItemHelp = new javax.swing.JMenuItem();
        jMenuItemAbout = new javax.swing.JMenuItem();

        jDialogSettings.setTitle("Settings");
        jDialogSettings.setAlwaysOnTop(true);
        jDialogSettings.setModal(true);
        jDialogSettings.setType(java.awt.Window.Type.UTILITY);
        jDialogSettings.addComponentListener(new java.awt.event.ComponentAdapter()
        {
            public void componentShown(java.awt.event.ComponentEvent evt)
            {
                jDialogSettingsComponentShown(evt);
            }
        });
        jDialogSettings.getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel5.setMinimumSize(new java.awt.Dimension(0, 0));
        jPanel5.setLayout(new java.awt.GridBagLayout());

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonCancelActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 2, 5, 5);
        jPanel5.add(jButtonCancel, gridBagConstraints);

        jButtonSave.setText("Save");
        jButtonSave.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonSaveActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 2);
        jPanel5.add(jButtonSave, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(jPanel5, gridBagConstraints);

        jTabbedPane1.setToolTipText("");
        jTabbedPane1.setMinimumSize(new java.awt.Dimension(0, 0));

        jPanelCallSign.setLayout(new java.awt.GridBagLayout());

        textfieldSettingsMyCallsign.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        textfieldSettingsMyCallsign.setText("Your callsign here");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 20);
        jPanelCallSign.add(textfieldSettingsMyCallsign, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelCallSign.add(jLabel21, gridBagConstraints);

        jLabel22.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelCallSign.add(jLabel22, gridBagConstraints);

        jTabbedPane1.addTab("Callsign", jPanelCallSign);

        jPanelRadio.setLayout(new java.awt.GridBagLayout());

        jComboBoxRadioComPort.setModel(getComportsComboboxModel());
        jComboBoxRadioComPort.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jComboBoxRadioComPortActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanelRadio.add(jComboBoxRadioComPort, gridBagConstraints);

        jLabel12.setText("CommPort");
        jLabel12.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelRadio.add(jLabel12, gridBagConstraints);

        jComboBoxRadioComPortBaudRate.setModel(getBaudRates());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanelRadio.add(jComboBoxRadioComPortBaudRate, gridBagConstraints);

        jLabel20.setText("Baud rate");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 1;
        gridBagConstraints.ipady = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelRadio.add(jLabel20, gridBagConstraints);

        jLabel23.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelRadio.add(jLabel23, gridBagConstraints);

        jLabel24.setText("DTR");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelRadio.add(jLabel24, gridBagConstraints);

        jLabel28.setText("RTS");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanelRadio.add(jLabel28, gridBagConstraints);

        buttonGroupRadioDtr.add(jRadioButtonRadioDtrOn);
        jRadioButtonRadioDtrOn.setSelected(true);
        jRadioButtonRadioDtrOn.setText("On");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        jPanelRadio.add(jRadioButtonRadioDtrOn, gridBagConstraints);

        buttonGroupRadioDtr.add(jRadioButtonRadioDtrOff);
        jRadioButtonRadioDtrOff.setText("Off");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        jPanelRadio.add(jRadioButtonRadioDtrOff, gridBagConstraints);

        buttonGroupRadioRts.add(jRadioButtonRadioRtsOn);
        jRadioButtonRadioRtsOn.setText("On");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        jPanelRadio.add(jRadioButtonRadioRtsOn, gridBagConstraints);

        buttonGroupRadioRts.add(jRadioButtonRadioRtsOff);
        jRadioButtonRadioRtsOff.setText("Off");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        jPanelRadio.add(jRadioButtonRadioRtsOff, gridBagConstraints);

        jLabel27.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelRadio.add(jLabel27, gridBagConstraints);

        jTabbedPane1.addTab("Radio", jPanelRadio);

        jPanelKeyerAndPtt.setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Keyer"));
        jPanel3.setLayout(new java.awt.GridBagLayout());

        jLabel19.setText("CommPort");
        jLabel19.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.8;
        jPanel3.add(jLabel19, gridBagConstraints);

        jComboBoxKeyerComPort.setModel(getComportsComboboxModel());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.8;
        jPanel3.add(jComboBoxKeyerComPort, gridBagConstraints);

        jLabel25.setText("Type");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel3.add(jLabel25, gridBagConstraints);

        jComboBoxKeyerType.setModel(KeyerTypes.getComboxModel());
        jComboBoxKeyerType.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                jComboBoxKeyerTypeItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel3.add(jComboBoxKeyerType, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelKeyerAndPtt.add(jPanel3, gridBagConstraints);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("PTT"));
        jPanel4.setLayout(new java.awt.GridBagLayout());

        jLabel26.setText("Type");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel4.add(jLabel26, gridBagConstraints);

        jLabel30.setText("CommPort");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel4.add(jLabel30, gridBagConstraints);

        jLabel33.setText("Front delay [msec]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel4.add(jLabel33, gridBagConstraints);

        jComboBoxPttCommport.setModel(getComportsComboboxModel());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel4.add(jComboBoxPttCommport, gridBagConstraints);

        jComboBoxPttType.setModel(PttTypes.getComboxModel());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel4.add(jComboBoxPttType, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel4.add(jTextFieldPttDelay, gridBagConstraints);

        jLabel35.setText("Tail delay [msec]");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel4.add(jLabel35, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel4.add(jTextFieldPttTailDelay, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelKeyerAndPtt.add(jPanel4, gridBagConstraints);

        jTabbedPane1.addTab("Keyer/PTT", jPanelKeyerAndPtt);

        jPanelFunctionKeys.setLayout(new java.awt.GridLayout(0, 2));

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel3.setText("F1 Cq");
        jPanelFunctionKeys.add(jLabel3);

        jtextfieldfF1.setText("jTextField1");
        jtextfieldfF1.setToolTipText("Available macro: {mycall}");
        jPanelFunctionKeys.add(jtextfieldfF1);

        jLabel31.setText("F2 Exchange");
        jPanelFunctionKeys.add(jLabel31);

        jTextFieldF2.setText("jTextField2");
        jTextFieldF2.setToolTipText("Available macros: {exch} and {mycall}");
        jPanelFunctionKeys.add(jTextFieldF2);

        jLabel9.setText("F3 Tu");
        jPanelFunctionKeys.add(jLabel9);

        jtextfieldfF3.setText("jTextField2");
        jPanelFunctionKeys.add(jtextfieldfF3);

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel4.setText("F6");
        jPanelFunctionKeys.add(jLabel4);

        jtextfieldfF6.setText("jTextField3");
        jPanelFunctionKeys.add(jtextfieldfF6);

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel5.setText("F7");
        jPanelFunctionKeys.add(jLabel5);

        jtextfieldfF7.setText("jTextField4");
        jPanelFunctionKeys.add(jtextfieldfF7);

        jLabel8.setText("F8");
        jPanelFunctionKeys.add(jLabel8);

        jtextfieldF8.setText("jTextField5");
        jPanelFunctionKeys.add(jtextfieldF8);

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel6.setText("F9");
        jPanelFunctionKeys.add(jLabel6);

        jtextfieldF9.setText("jTextField6");
        jPanelFunctionKeys.add(jtextfieldF9);

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel7.setText("F10 ");
        jPanelFunctionKeys.add(jLabel7);

        jtextfieldF10.setEditable(false);
        jtextfieldF10.setText("jTextField7");
        jtextfieldF10.setEnabled(false);
        jPanelFunctionKeys.add(jtextfieldF10);

        jTabbedPane1.addTab("Function Keys", jPanelFunctionKeys);

        jPanelContestRules.setToolTipText("");
        jPanelContestRules.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelContestRules.add(jLabel34, gridBagConstraints);

        jLabel29.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        jLabel29.setText("QSO repeat period [min]:");
        jLabel29.setToolTipText("After what period we can work the same station again.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        jPanelContestRules.add(jLabel29, gridBagConstraints);

        jtextfieldQsoRepeatPeriod.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        jtextfieldQsoRepeatPeriod.setText("30");
        jtextfieldQsoRepeatPeriod.setToolTipText("Usually is 30 minutes.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        jPanelContestRules.add(jtextfieldQsoRepeatPeriod, gridBagConstraints);

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        jLabel2.setText("Exchange rule:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanelContestRules.add(jLabel2, gridBagConstraints);

        jTextFieldContestExchange.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        jTextFieldContestExchange.setText("{#} {$}");
        jTextFieldContestExchange.setToolTipText("Available macros: {#} is serial number; {$} is first part of last Rcv; ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanelContestRules.add(jTextFieldContestExchange, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelContestRules.add(jLabel32, gridBagConstraints);

        jTabbedPane1.addTab("Contest Rules", jPanelContestRules);

        jPanelOther.setLayout(new java.awt.GridBagLayout());

        checkboxSettingsQuickMode.setText("Enable quick callsign entry");
        checkboxSettingsQuickMode.setToolTipText("If enabled will allow to enter callsign by using only the sufix");
        checkboxSettingsQuickMode.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                checkboxSettingsQuickModeStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelOther.add(checkboxSettingsQuickMode, gridBagConstraints);

        textfieldSettingsDefaultPrefix.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        textfieldSettingsDefaultPrefix.setText("LZ0");
        textfieldSettingsDefaultPrefix.setToolTipText("The default prefix which will be added");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.weighty = 1.0;
        jPanelOther.add(textfieldSettingsDefaultPrefix, gridBagConstraints);

        checkboxSendLeadingZeroAsT.setText("Send leading zeros as 'T'");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelOther.add(checkboxSendLeadingZeroAsT, gridBagConstraints);

        checkboxSendZeroAsT.setText("Send all zeros as 'T'");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelOther.add(checkboxSendZeroAsT, gridBagConstraints);

        checkboxESM.setText("\"Enter\" sends mesage");
        checkboxESM.setToolTipText("When Enter is pressed in the EntryWindow exchange will be transmitted");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelOther.add(checkboxESM, gridBagConstraints);

        jLabel10.setText("TimeToNextQso - Do not show after [sec]");
        jLabel10.setToolTipText("Set how long should be a callsing kept in the TimeToNextQso window after expiration of the repeat period.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelOther.add(jLabel10, gridBagConstraints);

        jTextFieldTimeToNextQso.setText("jTextField1");
        jTextFieldTimeToNextQso.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelOther.add(jTextFieldTimeToNextQso, gridBagConstraints);

        jCheckBoxAutoBandmapStartFreq.setText("Automatic bandmap frequency change");
        jCheckBoxAutoBandmapStartFreq.setToolTipText("If enabled your bandmap start frequency will change depending on the Selected Frequency and Band.");
        jCheckBoxAutoBandmapStartFreq.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                jCheckBoxAutoBandmapStartFreqItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelOther.add(jCheckBoxAutoBandmapStartFreq, gridBagConstraints);

        jTabbedPane1.addTab("Other", jPanelOther);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        jPanel1.add(jTabbedPane1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jDialogSettings.getContentPane().add(jPanel1, gridBagConstraints);

        jDialogFontChooser.setTitle("Choose fonts...");
        jDialogFontChooser.setAlwaysOnTop(true);
        jDialogFontChooser.setMinimumSize(new java.awt.Dimension(200, 300));
        jDialogFontChooser.getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel10.setLayout(new java.awt.GridLayout(7, 1));

        jButton13.setText("Callsign");
        jButton13.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton13ActionPerformed(evt);
            }
        });
        jPanel10.add(jButton13);

        jButton14.setText("Snt");
        jButton14.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton14ActionPerformed(evt);
            }
        });
        jPanel10.add(jButton14);

        jButton15.setText("Rcv");
        jButton15.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton15ActionPerformed(evt);
            }
        });
        jPanel10.add(jButton15);

        jButton16.setText("TimeToNextQso");
        jButton16.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton16ActionPerformed(evt);
            }
        });
        jPanel10.add(jButton16);

        jButton17.setText("Log");
        jButton17.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton17ActionPerformed(evt);
            }
        });
        jPanel10.add(jButton17);

        jButton18.setText("Bandmap");
        jButton18.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton18ActionPerformed(evt);
            }
        });
        jPanel10.add(jButton18);

        jButton19.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jButton19.setText("OK");
        jButton19.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jButton19.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton19ActionPerformed(evt);
            }
        });
        jPanel10.add(jButton19);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jDialogFontChooser.getContentPane().add(jPanel10, gridBagConstraints);

        jdialogLogSelection.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        jdialogLogSelection.setTitle("Choose action");
        jdialogLogSelection.setAlwaysOnTop(true);
        jdialogLogSelection.setMinimumSize(new java.awt.Dimension(200, 150));
        jdialogLogSelection.setModal(true);
        jdialogLogSelection.setResizable(false);
        jdialogLogSelection.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                jdialogLogSelectionWindowClosing(evt);
            }
        });
        jdialogLogSelection.getContentPane().setLayout(new java.awt.GridBagLayout());

        jbuttonCreateNewLog.setText("New log");
        jbuttonCreateNewLog.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jbuttonCreateNewLogActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        jdialogLogSelection.getContentPane().add(jbuttonCreateNewLog, gridBagConstraints);

        jbuttonOpenExistingLog.setText("Existing log");
        jbuttonOpenExistingLog.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jbuttonOpenExistingLogActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        jdialogLogSelection.getContentPane().add(jbuttonOpenExistingLog, gridBagConstraints);

        jDialogAbout.setTitle("About "+PROGRAM_NAME+" "+PROGRAM_VERSION);
        jDialogAbout.setMinimumSize(new java.awt.Dimension(450, 250));
        jDialogAbout.setModal(true);

        jPanel13.setLayout(new java.awt.GridBagLayout());

        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText(PROGRAM_ABOUT);
        jTextArea1.setWrapStyleWord(true);
        jScrollPane6.setViewportView(jTextArea1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel13.add(jScrollPane6, gridBagConstraints);

        jButton20.setText("Close");
        jButton20.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton20ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        jPanel13.add(jButton20, gridBagConstraints);

        jDialogAbout.getContentPane().add(jPanel13, java.awt.BorderLayout.CENTER);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(PROGRAM_NAME+" by LZ1ABC");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowOpened(java.awt.event.WindowEvent evt)
            {
                formWindowOpened(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                formWindowClosing(evt);
            }
        });

        jDesktopPane1.setMinimumSize(new java.awt.Dimension(600, 400));

        intframeTimeToNextQso.setIconifiable(true);
        intframeTimeToNextQso.setResizable(true);
        intframeTimeToNextQso.setTitle("Time to next QSO");
        intframeTimeToNextQso.setVisible(true);

        jtableIncomingQso.setFont(new java.awt.Font("Liberation Mono", 0, 18)); // NOI18N
        jtableIncomingQso.setRowHeight(30);
        jtableIncomingQso.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mousePressed(java.awt.event.MouseEvent evt)
            {
                jtableIncomingQsoMousePressed(evt);
            }
        });
        jScrollPane2.setViewportView(jtableIncomingQso);

        intframeTimeToNextQso.getContentPane().add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jDesktopPane1.add(intframeTimeToNextQso);
        intframeTimeToNextQso.setBounds(490, 10, 468, 436);

        intframeBandmap.setIconifiable(true);
        intframeBandmap.setResizable(true);
        intframeBandmap.setTitle("Bandmap");
        intframeBandmap.setToolTipText("If enabled your bandmap frequency will follow the frequency and the mode of the radio.");
        intframeBandmap.setVisible(true);
        intframeBandmap.getContentPane().setLayout(new java.awt.GridBagLayout());

        jtableBandmap.setCellSelectionEnabled(true);
        jtableBandmap.setGridColor(new java.awt.Color(233, 233, 233));
        jtableBandmap.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mousePressed(java.awt.event.MouseEvent evt)
            {
                jtableBandmapMousePressed(evt);
            }
        });
        jScrollPane5.setViewportView(jtableBandmap);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        intframeBandmap.getContentPane().add(jScrollPane5, gridBagConstraints);

        jPanel8.setLayout(new java.awt.GridBagLayout());

        jcomboboxStepInHz.setModel(getBandmapStepInHzComboboxModel());
        jcomboboxStepInHz.setToolTipText("Bandmap resolution");
        jcomboboxStepInHz.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                jcomboboxStepInHzItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel8.add(jcomboboxStepInHz, gridBagConstraints);

        jcomboboxColumnCount.setModel(getBandmapColumnCountComboboxModel());
        jcomboboxColumnCount.setToolTipText("Number of columns");
        jcomboboxColumnCount.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                jcomboboxColumnCountItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel8.add(jcomboboxColumnCount, gridBagConstraints);

        jcomboboxRowCount.setModel(getBandmapRowCountComboboxModel());
        jcomboboxRowCount.setToolTipText("Number of rows");
        jcomboboxRowCount.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                jcomboboxRowCountItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel8.add(jcomboboxRowCount, gridBagConstraints);

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel13.setText("Step [Hz]:");
        jLabel13.setToolTipText("Resolution in Hz");
        jLabel13.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(jLabel13, gridBagConstraints);

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel14.setText("Row:");
        jLabel14.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(jLabel14, gridBagConstraints);

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel15.setText("Col:");
        jLabel15.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(jLabel15, gridBagConstraints);

        jtextfieldFreqWidth.setText("20");
        jtextfieldFreqWidth.setToolTipText("Width of the frequency columns ");
        jtextfieldFreqWidth.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jtextfieldFreqWidthActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(jtextfieldFreqWidth, gridBagConstraints);

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel11.setText("Width of freq cols:");
        jLabel11.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(jLabel11, gridBagConstraints);

        jLabel18.setText("Start at [KHz]:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(jLabel18, gridBagConstraints);

        jtextfieldBandmapStartFreq.setText("3500");
        jtextfieldBandmapStartFreq.setToolTipText("The start freq of the bandmap (in KHz)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(jtextfieldBandmapStartFreq, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 12;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(jlabelBandmapFreeSpace, gridBagConstraints);

        jCheckBoxShowFreq.setText("Show freq. ");
        jCheckBoxShowFreq.setToolTipText("If the frequency columns should be visible");
        jCheckBoxShowFreq.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                jCheckBoxShowFreqItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(jCheckBoxShowFreq, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.001;
        intframeBandmap.getContentPane().add(jPanel8, gridBagConstraints);

        jDesktopPane1.add(intframeBandmap);
        intframeBandmap.setBounds(500, 520, 511, 459);

        intframeLog.setIconifiable(true);
        intframeLog.setResizable(true);
        intframeLog.setTitle("Log");
        intframeLog.setToolTipText("");
        intframeLog.setVisible(true);
        intframeLog.getContentPane().setLayout(new java.awt.GridBagLayout());

        jpanelCompleteLog.setLayout(new java.awt.GridBagLayout());

        jScrollPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        jtableLog.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jtableLog.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jtableLog);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jpanelCompleteLog.add(jScrollPane1, gridBagConstraints);

        jbuttonDeleteEntry.setToolTipText("Deletes Qso from the Log.");
        jbuttonDeleteEntry.setLabel("Delete entry");
        jbuttonDeleteEntry.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jbuttonDeleteEntryActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.01;
        gridBagConstraints.insets = new java.awt.Insets(1, 25, 1, 25);
        jpanelCompleteLog.add(jbuttonDeleteEntry, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        intframeLog.getContentPane().add(jpanelCompleteLog, gridBagConstraints);

        jDesktopPane1.add(intframeLog);
        intframeLog.setBounds(30, 130, 410, 590);

        intframeRadioConnection.setIconifiable(true);
        intframeRadioConnection.setResizable(true);
        intframeRadioConnection.setTitle("Radio/Keyer connection");
        intframeRadioConnection.setToolTipText("");
        intframeRadioConnection.setMinimumSize(new java.awt.Dimension(130, 90));
        intframeRadioConnection.setPreferredSize(new java.awt.Dimension(402, 150));
        intframeRadioConnection.setVisible(true);
        intframeRadioConnection.getContentPane().setLayout(new java.awt.GridBagLayout());

        jpanelRadioConnection.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder(""), "Radio"));
        jpanelRadioConnection.setPreferredSize(new java.awt.Dimension(353, 87));
        jpanelRadioConnection.setLayout(new java.awt.GridBagLayout());

        jtogglebuttonConnectToRadio.setText("Connect");
        jtogglebuttonConnectToRadio.setToolTipText("");
        jtogglebuttonConnectToRadio.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jtogglebuttonConnectToRadioActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 20);
        jpanelRadioConnection.add(jtogglebuttonConnectToRadio, gridBagConstraints);

        jtextfieldFrequency.setEditable(false);
        jtextfieldFrequency.setBackground(new java.awt.Color(0, 0, 0));
        jtextfieldFrequency.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jtextfieldFrequency.setForeground(new java.awt.Color(255, 255, 255));
        jtextfieldFrequency.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jtextfieldFrequency.setText("frequency");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jpanelRadioConnection.add(jtextfieldFrequency, gridBagConstraints);

        jtextfieldMode.setEditable(false);
        jtextfieldMode.setBackground(new java.awt.Color(0, 0, 0));
        jtextfieldMode.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jtextfieldMode.setForeground(new java.awt.Color(255, 255, 255));
        jtextfieldMode.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jtextfieldMode.setText("mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        jpanelRadioConnection.add(jtextfieldMode, gridBagConstraints);

        jcheckboxRadioPolling.setText("Enable polling");
        jcheckboxRadioPolling.setEnabled(false);
        jcheckboxRadioPolling.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jcheckboxRadioPolling.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                jcheckboxRadioPollingItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jpanelRadioConnection.add(jcheckboxRadioPolling, gridBagConstraints);

        jLabel17.setText("Every [msec]:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jpanelRadioConnection.add(jLabel17, gridBagConstraints);

        jtextfieldPollingTime.setEditable(false);
        jtextfieldPollingTime.setText("200");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jpanelRadioConnection.add(jtextfieldPollingTime, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        intframeRadioConnection.getContentPane().add(jpanelRadioConnection, gridBagConstraints);

        jpanelKeyerConnection.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder(""), "Keyer/PTT"));
        jpanelKeyerConnection.setMinimumSize(new java.awt.Dimension(235, 85));
        jpanelKeyerConnection.setName(""); // NOI18N
        jpanelKeyerConnection.setPreferredSize(new java.awt.Dimension(353, 87));
        jpanelKeyerConnection.setLayout(new java.awt.GridBagLayout());

        jtogglebuttonConnectToKeyer.setText("Connect");
        jtogglebuttonConnectToKeyer.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jtogglebuttonConnectToKeyerActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 20);
        jpanelKeyerConnection.add(jtogglebuttonConnectToKeyer, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        intframeRadioConnection.getContentPane().add(jpanelKeyerConnection, gridBagConstraints);

        jDesktopPane1.add(intframeRadioConnection);
        intframeRadioConnection.setBounds(30, 10, 470, 160);

        intframeEntryWindow.setIconifiable(true);
        intframeEntryWindow.setResizable(true);
        intframeEntryWindow.setTitle("Entry window");
        intframeEntryWindow.setVisible(true);
        intframeEntryWindow.getContentPane().setLayout(new java.awt.GridBagLayout());

        jpanelCallsign.setFocusCycleRoot(true);
        jpanelCallsign.setLayout(new java.awt.GridLayout(1, 0));

        jtextfieldCallsign.setBackground(java.awt.Color.lightGray);
        jtextfieldCallsign.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jtextfieldCallsign.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jtextfieldCallsign.setBorder(javax.swing.BorderFactory.createTitledBorder("Callsign"));
        jtextfieldCallsign.setMinimumSize(new java.awt.Dimension(0, 80));
        jtextfieldCallsign.setPreferredSize(new java.awt.Dimension(30, 58));
        jtextfieldCallsign.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyReleased(java.awt.event.KeyEvent evt)
            {
                jtextfieldCallsignKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt)
            {
                jtextfieldCallsignKeyTyped(evt);
            }
        });
        jpanelCallsign.add(jtextfieldCallsign);

        jtextfieldSnt.setBackground(java.awt.Color.lightGray);
        jtextfieldSnt.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jtextfieldSnt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jtextfieldSnt.setBorder(javax.swing.BorderFactory.createTitledBorder("Snt"));
        jtextfieldSnt.setMinimumSize(new java.awt.Dimension(0, 80));
        jpanelCallsign.add(jtextfieldSnt);

        jtextfieldRcv.setBackground(java.awt.Color.lightGray);
        jtextfieldRcv.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jtextfieldRcv.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jtextfieldRcv.setBorder(javax.swing.BorderFactory.createTitledBorder("Rcv"));
        jtextfieldRcv.setMinimumSize(new java.awt.Dimension(0, 80));
        jtextfieldRcv.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyTyped(java.awt.event.KeyEvent evt)
            {
                jtextfieldRcvKeyTyped(evt);
            }
        });
        jpanelCallsign.add(jtextfieldRcv);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        intframeEntryWindow.getContentPane().add(jpanelCallsign, gridBagConstraints);

        jpanelTypeOfWork.setMinimumSize(new java.awt.Dimension(0, 25));
        jpanelTypeOfWork.setLayout(new java.awt.GridBagLayout());

        buttonGroupTypeOfWork.add(jradiobuttonCQ);
        jradiobuttonCQ.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jradiobuttonCQ.setText("CQ");
        jradiobuttonCQ.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jradiobuttonCQ.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                jradiobuttonCQItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 1.0;
        jpanelTypeOfWork.add(jradiobuttonCQ, gridBagConstraints);

        buttonGroupTypeOfWork.add(jradiobuttonSP);
        jradiobuttonSP.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jradiobuttonSP.setSelected(true);
        jradiobuttonSP.setText("S&P");
        jradiobuttonSP.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 1.0;
        jpanelTypeOfWork.add(jradiobuttonSP, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jpanelTypeOfWork.add(jLabel1, gridBagConstraints);

        jcomboboxMode.setModel(getModeComboboxModel());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 1.0;
        jpanelTypeOfWork.add(jcomboboxMode, gridBagConstraints);

        jcomboboxBand.setModel(getBandsComboboxModel());
        jcomboboxBand.setSelectedItem(getBandsComboboxModel().getElementAt(1)
        );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 1.0;
        jpanelTypeOfWork.add(jcomboboxBand, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        intframeEntryWindow.getContentPane().add(jpanelTypeOfWork, gridBagConstraints);

        jpanelFunctionKeys.setMinimumSize(new java.awt.Dimension(0, 80));
        jpanelFunctionKeys.setName(""); // NOI18N
        jpanelFunctionKeys.setPreferredSize(new java.awt.Dimension(100, 75));
        jpanelFunctionKeys.setLayout(new java.awt.GridLayout(3, 0));

        jButton1.setText("F1 CQ");
        jButton1.setFocusable(false);
        jButton1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton1ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton1);

        jButton2.setText("F2 Exch");
        jButton2.setFocusable(false);
        jButton2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton2ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton2);

        jButton3.setText("F3 Tu");
        jButton3.setFocusable(false);
        jButton3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton3ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton3);

        jButton4.setText("F4 "+settings.getMyCallsign());
        jButton4.setFocusable(false);
        jButton4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton4.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton4ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton4);

        jButton5.setText("F5 His Call");
        jButton5.setFocusable(false);
        jButton5.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton5.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton5ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton5);

        jButton6.setText("F6 Agn");
        jButton6.setFocusable(false);
        jButton6.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton6.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton6ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton6);

        jButton7.setText("F7 ?");
        jButton7.setFocusable(false);
        jButton7.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton7.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton7ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton7);

        jButton8.setText("F8 Dupe");
        jButton8.setFocusable(false);
        jButton8.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton8.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton8ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton8);

        jButton9.setText("F9 Spare");
        jButton9.setFocusable(false);
        jButton9.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton9.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton9ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton9);

        jButton10.setText("F10 Not used");
        jButton10.setToolTipText("");
        jButton10.setEnabled(false);
        jButton10.setFocusable(false);
        jButton10.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton10.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton10ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton10);

        jButton11.setText("F11 Spot");
        jButton11.setFocusable(false);
        jButton11.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton11.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton11ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton11);

        jButton12.setText("F12 Wipe");
        jButton12.setFocusable(false);
        jButton12.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton12.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton12ActionPerformed(evt);
            }
        });
        jpanelFunctionKeys.add(jButton12);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.4;
        intframeEntryWindow.getContentPane().add(jpanelFunctionKeys, gridBagConstraints);

        jPanelStatusBar.setMinimumSize(new java.awt.Dimension(0, 22));
        jPanelStatusBar.setLayout(new java.awt.GridBagLayout());

        jlabelCallsignStatus.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jlabelCallsignStatus.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jlabelCallsignStatus.setText("status text here");
        jlabelCallsignStatus.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelStatusBar.add(jlabelCallsignStatus, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 10, 0);
        intframeEntryWindow.getContentPane().add(jPanelStatusBar, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jLabelStatus.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabelStatus.setText("status");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(jLabelStatus, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.1;
        intframeEntryWindow.getContentPane().add(jPanel2, gridBagConstraints);

        jDesktopPane1.add(intframeEntryWindow);
        intframeEntryWindow.setBounds(280, 20, 453, 247);

        intframeMisc.setIconifiable(true);
        intframeMisc.setResizable(true);
        intframeMisc.setTitle("Misc");
        intframeMisc.setVisible(true);
        intframeMisc.getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel11.setLayout(new java.awt.GridBagLayout());

        jpanelCqSettings.setBorder(javax.swing.BorderFactory.createTitledBorder("CQ settings"));
        jpanelCqSettings.setLayout(new java.awt.GridBagLayout());

        jbuttonJumpToCqFreq.setText("Jump to CQ freq");
        jbuttonJumpToCqFreq.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jbuttonJumpToCqFreqActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
        jpanelCqSettings.add(jbuttonJumpToCqFreq, gridBagConstraints);

        jcheckboxF1jumpsToCq.setText("F1 jumps to CQ freq");
        jcheckboxF1jumpsToCq.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                jcheckboxF1jumpsToCqStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
        jpanelCqSettings.add(jcheckboxF1jumpsToCq, gridBagConstraints);

        jlabelCqFreq.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jlabelCqFreq.setText("N.A.");
        jlabelCqFreq.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
        jpanelCqSettings.add(jlabelCqFreq, gridBagConstraints);

        jbuttonSetCqFreq.setText("Set CQ freq (Ctrl+F1)");
        jbuttonSetCqFreq.setEnabled(false);
        jbuttonSetCqFreq.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jbuttonSetCqFreqActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
        jpanelCqSettings.add(jbuttonSetCqFreq, gridBagConstraints);

        jcheckboxContinuousCq.setText("Continuous CQ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
        jpanelCqSettings.add(jcheckboxContinuousCq, gridBagConstraints);

        jLabel16.setText("CQ interval [msec]:   ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 0);
        jpanelCqSettings.add(jLabel16, gridBagConstraints);

        jtextfieldContinuousCqPeriod.setText("2000");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 1, 0);
        jpanelCqSettings.add(jtextfieldContinuousCqPeriod, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel11.add(jpanelCqSettings, gridBagConstraints);

        jpanelKeyerSettings.setBorder(javax.swing.BorderFactory.createTitledBorder("Keyer settings"));
        jpanelKeyerSettings.setMinimumSize(new java.awt.Dimension(188, 70));
        jpanelKeyerSettings.setLayout(new java.awt.GridLayout(1, 0));

        jbuttonKeyerUP.setText("UP");
        jbuttonKeyerUP.setToolTipText("Also the PAGE_UP key");
        jbuttonKeyerUP.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jbuttonKeyerUPActionPerformed(evt);
            }
        });
        jpanelKeyerSettings.add(jbuttonKeyerUP);

        jbuttonKeyerDown.setText("DOWN");
        jbuttonKeyerDown.setToolTipText("Also the PAGE_DOWN key");
        jbuttonKeyerDown.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jbuttonKeyerDownActionPerformed(evt);
            }
        });
        jpanelKeyerSettings.add(jbuttonKeyerDown);

        jlabelKeyerSpeed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jlabelKeyerSpeed.setText(Integer.toString(keyerSpeed)+" WPM");
        jpanelKeyerSettings.add(jlabelKeyerSpeed);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.1;
        jPanel11.add(jpanelKeyerSettings, gridBagConstraints);

        jScrollPane3.setViewportView(jPanel11);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        intframeMisc.getContentPane().add(jScrollPane3, gridBagConstraints);

        jDesktopPane1.add(intframeMisc);
        intframeMisc.setBounds(400, 340, 230, 170);

        getContentPane().add(jDesktopPane1, java.awt.BorderLayout.CENTER);

        jMenu1.setText("File");

        jmenuGenerateCabrillo.setText("Generate Cabrillo File");
        jmenuGenerateCabrillo.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jmenuGenerateCabrilloActionPerformed(evt);
            }
        });
        jMenu1.add(jmenuGenerateCabrillo);

        jMenuItem7.setText("Generate Adif File");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem7);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Tools");

        jmenuSettings.setText("Settings");
        jmenuSettings.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jmenuSettingsActionPerformed(evt);
            }
        });
        jMenu2.add(jmenuSettings);

        jmenuFonts.setText("Fonts");
        jmenuFonts.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jmenuFontsActionPerformed(evt);
            }
        });
        jMenu2.add(jmenuFonts);

        jMenuBar1.add(jMenu2);

        jmenuWindows.setText("Windows");

        jMenuItem1.setText("Entry window");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jmenuWindows.add(jMenuItem1);

        jMenuItem2.setText("Time to next QSO");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jmenuWindows.add(jMenuItem2);

        jMenuItem3.setText("Bandmap");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jmenuWindows.add(jMenuItem3);

        jMenuItem4.setText("Radio/Keyer connection");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jmenuWindows.add(jMenuItem4);

        jMenuItem5.setText("Misc");
        jMenuItem5.setToolTipText("");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jmenuWindows.add(jMenuItem5);

        jMenuItem6.setText("Log");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jmenuWindows.add(jMenuItem6);

        jMenuBar1.add(jmenuWindows);

        jMenu3.setText("Help");

        jMenuItemHelp.setText("Help");
        jMenuItemHelp.setToolTipText("");
        jMenuItemHelp.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItemHelpActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItemHelp);

        jMenuItemAbout.setText("About");
        jMenuItemAbout.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jMenuItemAboutActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItemAbout);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void formWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowOpened
  {//GEN-HEADEREND:event_formWindowOpened
      initMainWindow(true);
  }//GEN-LAST:event_formWindowOpened

  private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
  {//GEN-HEADEREND:event_formWindowClosing
      // Read the dimensions of the different frames
      settings.setFrameDimensions(ApplicationSettings.FrameIndex.ENTRY, intframeEntryWindow.getBounds());
      settings.setFrameDimensions(ApplicationSettings.FrameIndex.BANDMAP, intframeBandmap.getBounds());
      settings.setFrameDimensions(ApplicationSettings.FrameIndex.INCOMING_QSO, intframeTimeToNextQso.getBounds());
      settings.setFrameDimensions(ApplicationSettings.FrameIndex.JFRAME, this.getBounds());
      settings.setFrameDimensions(ApplicationSettings.FrameIndex.LOG, intframeLog.getBounds());
      settings.setFrameDimensions(ApplicationSettings.FrameIndex.RADIO, intframeRadioConnection.getBounds());
      settings.setFrameDimensions(ApplicationSettings.FrameIndex.SETTINGS, intframeMisc.getBounds());

      storeFonts();

      settings.SaveSettingsToDisk(); // Save all settings to disk
  }//GEN-LAST:event_formWindowClosing

  private void jmenuSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmenuSettingsActionPerformed
  {//GEN-HEADEREND:event_jmenuSettingsActionPerformed
      jDialogSettings.pack();
      jDialogSettings.setVisible(true);
  }//GEN-LAST:event_jmenuSettingsActionPerformed

  private void jButton12ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton12ActionPerformed
  {//GEN-HEADEREND:event_jButton12ActionPerformed
      pressedF12();
  }//GEN-LAST:event_jButton12ActionPerformed

  private void jButton11ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton11ActionPerformed
  {//GEN-HEADEREND:event_jButton11ActionPerformed
      pressedF11();
  }//GEN-LAST:event_jButton11ActionPerformed

  private void jtextfieldCallsignKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jtextfieldCallsignKeyReleased
  {//GEN-HEADEREND:event_jtextfieldCallsignKeyReleased
      // On every key press update the callsign status
      String status = getCallsignStatusText(getCallsignFromTextField());
      jlabelCallsignStatus.setText(status);
  }//GEN-LAST:event_jtextfieldCallsignKeyReleased

  private void jtextfieldCallsignKeyTyped(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jtextfieldCallsignKeyTyped
  {//GEN-HEADEREND:event_jtextfieldCallsignKeyTyped
      if(timerContinuousCq.isRunning()) // Any key press stops the automatic CQ
      {
          timerContinuousCq.stop();
          keyer.stopSendingCw();
      }

      switch(evt.getKeyChar())
      {
          case KeyEvent.VK_SPACE: // Move to Rcv field    
              jtextfieldRcv.requestFocusInWindow();
              evt.consume();
              break;

          case KeyEvent.VK_ENTER: // Move to Rcv field      
              if(settings.isEmsEnabled())
              {
                  if(sendEnterSendsMessage())
                  {
                      jtextfieldRcv.requestFocusInWindow();
                  }
              }
              else
              {
                  jtextfieldRcv.requestFocusInWindow();
              }
              evt.consume();
              break;
      }
  }//GEN-LAST:event_jtextfieldCallsignKeyTyped

  private void jbuttonDeleteEntryActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jbuttonDeleteEntryActionPerformed
  {//GEN-HEADEREND:event_jbuttonDeleteEntryActionPerformed
      // Ask for confirmation
      int response = JOptionPane.showConfirmDialog(null, "Delete the selected Qso entry?", "Confirm",
              JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      if(response == JOptionPane.NO_OPTION || response == JOptionPane.CLOSED_OPTION)
      {
          return; // do nothing
      }

      // Get the selected row
      int selection = jtableLog.getSelectedRow();
      if(selection >= 0)
      {
          selection = jtableLog.convertRowIndexToModel(selection);
          jtablemodelLog.removeRow(selection);
      }
      else
      {
          JOptionPane.showMessageDialog(null, "Pease select entry!");
      }
  }//GEN-LAST:event_jbuttonDeleteEntryActionPerformed

  private void jtogglebuttonConnectToRadioActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jtogglebuttonConnectToRadioActionPerformed
  {//GEN-HEADEREND:event_jtogglebuttonConnectToRadioActionPerformed
      JToggleButton tBtn = (JToggleButton) evt.getSource();

      if(settings.getRadioCommportName().isEmpty())
      {
          JOptionPane.showMessageDialog(null, "Commport not set!");
      } // Connect
      // --------------------
      else if(tBtn.isSelected())
      {
          // Select the python file describing the radio protocol
          if(loadRadioProtocolParser())
          {
              connectRadio(); // now we can try to connect
          }
      } // Disconnect
      // --------------------
      else
      {
          disconnectRadio();
      }

      if(radioController.isConnected())
      {
          jtogglebuttonConnectToRadio.setSelected(true);
          radioController.getKeyer().setCwSpeed(keyerSpeed);
          jcomboboxBand.setEnabled(false);
          jcomboboxMode.setEnabled(false);
          jcheckboxRadioPolling.setEnabled(true);
          jtextfieldPollingTime.setEditable(true);
      }
      else
      {
          jtogglebuttonConnectToRadio.setSelected(false);
          jcomboboxBand.setEnabled(true);
          jcomboboxMode.setEnabled(true);
          jcheckboxRadioPolling.setEnabled(false);
          jtextfieldPollingTime.setEditable(false);
      }
  }//GEN-LAST:event_jtogglebuttonConnectToRadioActionPerformed

  private void jButton3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton3ActionPerformed
  {//GEN-HEADEREND:event_jButton3ActionPerformed
      pressedF3();
  }//GEN-LAST:event_jButton3ActionPerformed

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton1ActionPerformed
  {//GEN-HEADEREND:event_jButton1ActionPerformed
      pressedF1();
  }//GEN-LAST:event_jButton1ActionPerformed

  private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton2ActionPerformed
  {//GEN-HEADEREND:event_jButton2ActionPerformed
      pressedF2();
  }//GEN-LAST:event_jButton2ActionPerformed

  private void jButton4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton4ActionPerformed
  {//GEN-HEADEREND:event_jButton4ActionPerformed
      pressedF4();
  }//GEN-LAST:event_jButton4ActionPerformed

  private void jButton5ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton5ActionPerformed
  {//GEN-HEADEREND:event_jButton5ActionPerformed
      pressedF5();
  }//GEN-LAST:event_jButton5ActionPerformed

  private void jButton6ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton6ActionPerformed
  {//GEN-HEADEREND:event_jButton6ActionPerformed
      pressedF6();
  }//GEN-LAST:event_jButton6ActionPerformed

  private void jButton7ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton7ActionPerformed
  {//GEN-HEADEREND:event_jButton7ActionPerformed
      pressedF7();
  }//GEN-LAST:event_jButton7ActionPerformed

  private void jButton8ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton8ActionPerformed
  {//GEN-HEADEREND:event_jButton8ActionPerformed
      pressedF8();
  }//GEN-LAST:event_jButton8ActionPerformed

  private void jButton9ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton9ActionPerformed
  {//GEN-HEADEREND:event_jButton9ActionPerformed
      pressedF9();
  }//GEN-LAST:event_jButton9ActionPerformed

  private void jButton10ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton10ActionPerformed
  {//GEN-HEADEREND:event_jButton10ActionPerformed
      pressedF10();
  }//GEN-LAST:event_jButton10ActionPerformed

  private void jradiobuttonCQItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jradiobuttonCQItemStateChanged
  {//GEN-HEADEREND:event_jradiobuttonCQItemStateChanged

  }//GEN-LAST:event_jradiobuttonCQItemStateChanged

  private void jcomboboxColumnCountItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jcomboboxColumnCountItemStateChanged
  {//GEN-HEADEREND:event_jcomboboxColumnCountItemStateChanged
      if(evt.getStateChange() == ItemEvent.SELECTED)
      {
          settings.setBandmapColumnCount(Integer.parseInt((String) jcomboboxColumnCount.getSelectedItem()));
          jtablemodelBandmap.fireTableStructureChanged();
      }
  }//GEN-LAST:event_jcomboboxColumnCountItemStateChanged

  private void jcomboboxRowCountItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jcomboboxRowCountItemStateChanged
  {//GEN-HEADEREND:event_jcomboboxRowCountItemStateChanged
      if(evt.getStateChange() == ItemEvent.SELECTED)
      {
          settings.setBandmapRowCount(Integer.parseInt((String) jcomboboxRowCount.getSelectedItem()));
          jtablemodelBandmap.fireTableStructureChanged();
      }
  }//GEN-LAST:event_jcomboboxRowCountItemStateChanged

  private void jcomboboxStepInHzItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jcomboboxStepInHzItemStateChanged
  {//GEN-HEADEREND:event_jcomboboxStepInHzItemStateChanged
      if(evt.getStateChange() == ItemEvent.SELECTED)
      {
          settings.setBandmapStepInHz(Integer.parseInt((String) jcomboboxStepInHz.getSelectedItem()));
      }
  }//GEN-LAST:event_jcomboboxStepInHzItemStateChanged

  private void jmenuFontsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmenuFontsActionPerformed
  {//GEN-HEADEREND:event_jmenuFontsActionPerformed
      jDialogFontChooser.setVisible(true);
  }//GEN-LAST:event_jmenuFontsActionPerformed

  private void jButton13ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton13ActionPerformed
  {//GEN-HEADEREND:event_jButton13ActionPerformed
      fontchooser.setSelectedFont(jtextfieldCallsign.getFont());
      if(fontchooser.showDialog(jtextfieldCallsign) == FontChooser.OK_OPTION)
      {
          jtextfieldCallsign.setFont(fontchooser.getSelectedFont());
          storeFonts();
      }
  }//GEN-LAST:event_jButton13ActionPerformed

  private void jButton19ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton19ActionPerformed
  {//GEN-HEADEREND:event_jButton19ActionPerformed
      jDialogFontChooser.setVisible(false);
  }//GEN-LAST:event_jButton19ActionPerformed

  private void jButton14ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton14ActionPerformed
  {//GEN-HEADEREND:event_jButton14ActionPerformed
      fontchooser.setSelectedFont(jtextfieldSnt.getFont());
      if(fontchooser.showDialog(jtextfieldSnt) == FontChooser.OK_OPTION)
      {
          jtextfieldSnt.setFont(fontchooser.getSelectedFont());
          storeFonts();
      }
  }//GEN-LAST:event_jButton14ActionPerformed

  private void jButton15ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton15ActionPerformed
  {//GEN-HEADEREND:event_jButton15ActionPerformed
      fontchooser.setSelectedFont(jtextfieldRcv.getFont());
      if(fontchooser.showDialog(jtextfieldRcv) == FontChooser.OK_OPTION)
      {
          jtextfieldRcv.setFont(fontchooser.getSelectedFont());
          storeFonts();
      }
  }//GEN-LAST:event_jButton15ActionPerformed

  private void jButton16ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton16ActionPerformed
  {//GEN-HEADEREND:event_jButton16ActionPerformed
      fontchooser.setSelectedFont(jtableIncomingQso.getFont());
      if(fontchooser.showDialog(jtableIncomingQso) == FontChooser.OK_OPTION)
      {
          jtableIncomingQso.setFont(fontchooser.getSelectedFont());
          storeFonts();
      }
  }//GEN-LAST:event_jButton16ActionPerformed

  private void jButton17ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton17ActionPerformed
  {//GEN-HEADEREND:event_jButton17ActionPerformed
      fontchooser.setSelectedFont(jtableLog.getFont());
      if(fontchooser.showDialog(jtableLog) == FontChooser.OK_OPTION)
      {
          jtableLog.setFont(fontchooser.getSelectedFont());
          storeFonts();
      }
  }//GEN-LAST:event_jButton17ActionPerformed

  private void jButton18ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton18ActionPerformed
  {//GEN-HEADEREND:event_jButton18ActionPerformed
      fontchooser.setSelectedFont(jtableBandmap.getFont());
      if(fontchooser.showDialog(jtableBandmap) == FontChooser.OK_OPTION)
      {
          jtableBandmap.setFont(fontchooser.getSelectedFont());
          storeFonts();
      }
  }//GEN-LAST:event_jButton18ActionPerformed

  private void jtextfieldRcvKeyTyped(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jtextfieldRcvKeyTyped
  {//GEN-HEADEREND:event_jtextfieldRcvKeyTyped
      switch(evt.getKeyChar())
      {
          case KeyEvent.VK_SPACE: // Move to Rcv field    
              jtextfieldCallsign.requestFocusInWindow();
              evt.consume();
              break;
          case KeyEvent.VK_ENTER: // Move to Rcv field    
              // Log Qso
              if(addEntryToLog())
              {
                  if(settings.isEmsEnabled() && jradiobuttonCQ.isSelected())
                  {
                      pressedF3();
                  }
                  else if(settings.isEmsEnabled() && jradiobuttonSP.isSelected())
                  {
                      pressedF2();
                  }

                  initEntryFields();
              }
              evt.consume();
              break;
      }
  }//GEN-LAST:event_jtextfieldRcvKeyTyped

  private void jbuttonSetCqFreqActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jbuttonSetCqFreqActionPerformed
  {//GEN-HEADEREND:event_jbuttonSetCqFreqActionPerformed
      cqFrequency = getFreq();
      jlabelCqFreq.setText(Misc.formatFrequency(Integer.toString(cqFrequency)));
  }//GEN-LAST:event_jbuttonSetCqFreqActionPerformed

  private void jbuttonJumpToCqFreqActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jbuttonJumpToCqFreqActionPerformed
  {//GEN-HEADEREND:event_jbuttonJumpToCqFreqActionPerformed
      radioController.setFrequency(cqFrequency);
  }//GEN-LAST:event_jbuttonJumpToCqFreqActionPerformed

  private void jcheckboxF1jumpsToCqStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jcheckboxF1jumpsToCqStateChanged
  {//GEN-HEADEREND:event_jcheckboxF1jumpsToCqStateChanged
      if(jcheckboxF1jumpsToCq.isSelected())
      {
          jbuttonSetCqFreq.setEnabled(true);
      }
      else
      {
          jbuttonSetCqFreq.setEnabled(false);
      }
  }//GEN-LAST:event_jcheckboxF1jumpsToCqStateChanged

  private void jbuttonCreateNewLogActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jbuttonCreateNewLogActionPerformed
  {//GEN-HEADEREND:event_jbuttonCreateNewLogActionPerformed
      if(createNewLog())
      {
          jdialogLogSelection.dispose();
      }
  }//GEN-LAST:event_jbuttonCreateNewLogActionPerformed

  private void jdialogLogSelectionWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_jdialogLogSelectionWindowClosing
  {//GEN-HEADEREND:event_jdialogLogSelectionWindowClosing
      ((LogSelectionDialog) jdialogLogSelection).isProgramTerminated = true;
  }//GEN-LAST:event_jdialogLogSelectionWindowClosing

  private void jbuttonOpenExistingLogActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jbuttonOpenExistingLogActionPerformed
  {//GEN-HEADEREND:event_jbuttonOpenExistingLogActionPerformed
      if(findExistingLog())
      {
          jdialogLogSelection.dispose();
      }
  }//GEN-LAST:event_jbuttonOpenExistingLogActionPerformed

  private void jmenuGenerateCabrilloActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmenuGenerateCabrilloActionPerformed
  {//GEN-HEADEREND:event_jmenuGenerateCabrilloActionPerformed
      JFileChooser fc = new JFileChooser();
      fc.setFileFilter(new FileNameExtensionFilter("Cabrillo files (*.log)", "log"));
      fc.setCurrentDirectory(Paths.get(pathToWorkingDir, "/logs/").toFile());
      try
      {
          int returnVal = fc.showSaveDialog(this.getParent());
          if(returnVal != JFileChooser.APPROVE_OPTION)
          {
              return;
          }
      } catch(Exception exc)
      {
          JOptionPane.showMessageDialog(null, "Error when trying to acquire cabrillo file.", "Error", JOptionPane.ERROR_MESSAGE);
          return;
      }

      String absPath = fc.getSelectedFile().getAbsolutePath();
      if(!absPath.endsWith(".log"))
      {
          absPath = absPath + ".log";
      }

      File file = new File(absPath);

      if(file.exists())
      {
          JOptionPane.showMessageDialog(null, "File already exists: " + file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
          return;
      }

      // Write the cabrillo file
      try(PrintWriter printWriter = new PrintWriter(absPath))
      {
          printWriter.println("START-OF-LOG: 2.0");
          printWriter.println("CALLSIGN: " + settings.getMyCallsign());
          printWriter.println("CONTEST: ");
          printWriter.println("CATEGORY: ");
          printWriter.println("CLAIMED-SCORE: ");
          printWriter.println("OPERATORS: ");
          printWriter.println("NAME: ");
          printWriter.println("ADDRESS: ");
          printWriter.println("ADDRESS: ");
          printWriter.println("CREATED-BY: " + PROGRAM_NAME + " " + PROGRAM_VERSION);
          for(int i = 0; i < log.getQsoCount(); i++)
          {
              printWriter.println(log.get(i).toStringCabrillo());
          }
          printWriter.println("END-OF-LOG:");
      } catch(FileNotFoundException ex)
      {
          JOptionPane.showMessageDialog(null, "Couldn't generate the Cabrillo file", "Error", JOptionPane.ERROR_MESSAGE);
      }

      JOptionPane.showMessageDialog(null, "Cabrilo file created successfully.", "Success...", JOptionPane.INFORMATION_MESSAGE);
  }//GEN-LAST:event_jmenuGenerateCabrilloActionPerformed

  private void jbuttonKeyerUPActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jbuttonKeyerUPActionPerformed
  {//GEN-HEADEREND:event_jbuttonKeyerUPActionPerformed
      increaseKeyerSpeed();
  }//GEN-LAST:event_jbuttonKeyerUPActionPerformed

  private void jbuttonKeyerDownActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jbuttonKeyerDownActionPerformed
  {//GEN-HEADEREND:event_jbuttonKeyerDownActionPerformed
      decreaseKeyerSpeed();
  }//GEN-LAST:event_jbuttonKeyerDownActionPerformed

  private void jtextfieldFreqWidthActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jtextfieldFreqWidthActionPerformed
  {//GEN-HEADEREND:event_jtextfieldFreqWidthActionPerformed
      resizeFreqColumnWidth(jtableBandmap, Integer.parseInt(jtextfieldFreqWidth.getText()));
  }//GEN-LAST:event_jtextfieldFreqWidthActionPerformed

  private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem1ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem1ActionPerformed
      intframeEntryWindow.toFront();
  }//GEN-LAST:event_jMenuItem1ActionPerformed

  private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem2ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem2ActionPerformed
      intframeTimeToNextQso.toFront();
  }//GEN-LAST:event_jMenuItem2ActionPerformed

  private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem3ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem3ActionPerformed
      intframeBandmap.toFront();
  }//GEN-LAST:event_jMenuItem3ActionPerformed

  private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem4ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem4ActionPerformed
      intframeRadioConnection.moveToFront();
      jpanelRadioConnection.requestFocusInWindow();
  }//GEN-LAST:event_jMenuItem4ActionPerformed

  private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem5ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem5ActionPerformed
      intframeMisc.toFront();
  }//GEN-LAST:event_jMenuItem5ActionPerformed

  private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem6ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem6ActionPerformed
      intframeLog.toFront();
  }//GEN-LAST:event_jMenuItem6ActionPerformed

  private void jcheckboxRadioPollingItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jcheckboxRadioPollingItemStateChanged
  {//GEN-HEADEREND:event_jcheckboxRadioPollingItemStateChanged
      if(jcheckboxRadioPolling.isSelected())
      {
          try
          {
              int period = Integer.parseInt(jtextfieldPollingTime.getText());
              timerRadioPolling = new Timer(period, timerRadioPollingListener);
              timerRadioPolling.setRepeats(true);
          } catch(Exception exc)
          {
              JOptionPane.showMessageDialog(null, "Enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
              jcheckboxRadioPolling.setSelected(false);
              return;
          }
          radioController.setAutomaticInfo(false); // polling is started - we don't need the auto info from the radio
          timerRadioPolling.start();
      }
      else
      {
          if(timerRadioPolling != null)
          {
              timerRadioPolling.stop();
              radioController.setAutomaticInfo(true); // polling is stopped - enable auto info
          }
      }
  }//GEN-LAST:event_jcheckboxRadioPollingItemStateChanged

  private void jtableBandmapMousePressed(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jtableBandmapMousePressed
  {//GEN-HEADEREND:event_jtableBandmapMousePressed
      JTable target = (JTable) evt.getSource();
      int row = target.getSelectedRow();
      int col = target.getSelectedColumn();

      if(row > -1 && col > -1)
      {
          try
          {
              int freq = jtablemodelBandmap.cellToFreq(row, col);
              radioController.setFrequency(freq);
              initEntryFields();
          } catch(Exception ex)
          {
              Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
          }
      }
      else
      {
          Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, "Invalid row or col");
      }

      // Return focus to callsign field
      jtextfieldCallsign.requestFocusInWindow();
  }//GEN-LAST:event_jtableBandmapMousePressed

  private void jtableIncomingQsoMousePressed(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jtableIncomingQsoMousePressed
  {//GEN-HEADEREND:event_jtableIncomingQsoMousePressed
      JTable target = (JTable) evt.getSource();
      int row = target.getSelectedRow();

      try
      {
          if(getMode() != jtablemodelIncomingQso.getMode(row))
          {
              radioController.setMode(jtablemodelIncomingQso.getMode(row)); //set the mode used for this contact
          }
          radioController.setFrequency(jtablemodelIncomingQso.getFrequency(row)); // jump to the freq

          initEntryFields();  // clear the fields

          // Add the callsign into the Entry field if this is a S&P contact
          if(jtablemodelIncomingQso.isSpQso(row))
          {
              String callsign;

              callsign = jtablemodelIncomingQso.getCallsign(row);

              jtextfieldCallsign.setText(callsign);// set the callsign inside the callsign field
          }

      } catch(Exception ex)
      {
          Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
      }

      // Return focus to callsign field
      jtextfieldCallsign.requestFocusInWindow();
  }//GEN-LAST:event_jtableIncomingQsoMousePressed

  private void jtogglebuttonConnectToKeyerActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jtogglebuttonConnectToKeyerActionPerformed
  {//GEN-HEADEREND:event_jtogglebuttonConnectToKeyerActionPerformed
      JToggleButton connectButton = (JToggleButton) evt.getSource();

      if(connectButton.isSelected())
      {
          if(connectKeyer() == false)
          {
              jtogglebuttonConnectToKeyer.setSelected(false);
              return;
          }

          if(settings.getPttType() != PttTypes.NONE)
          {
              if(connectPtt() == false)
              {
                  disconnectKeyer();
                  jtogglebuttonConnectToKeyer.setSelected(false);
                  return;
              }
          }

          jtogglebuttonConnectToKeyer.setSelected(true);
      }
      else
      {
          disconnectKeyer();
          disconnectPtt();
          jtogglebuttonConnectToKeyer.setSelected(false);
      }

  }//GEN-LAST:event_jtogglebuttonConnectToKeyerActionPerformed

  private void jDialogSettingsComponentShown(java.awt.event.ComponentEvent evt)//GEN-FIRST:event_jDialogSettingsComponentShown
  {//GEN-HEADEREND:event_jDialogSettingsComponentShown
      // Settings dialog is shown and we need to set the states of the controls
      initSettingsDialog();
  }//GEN-LAST:event_jDialogSettingsComponentShown

  private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonSaveActionPerformed
  {//GEN-HEADEREND:event_jButtonSaveActionPerformed
      jDialogSettings.setVisible(false); // Hide the SettingsDialog
      storeSettings();       // Read the state of the controls and save them

      initMainWindow(false);
  }//GEN-LAST:event_jButtonSaveActionPerformed

  private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonCancelActionPerformed
  {//GEN-HEADEREND:event_jButtonCancelActionPerformed
      jDialogSettings.setVisible(false);
  }//GEN-LAST:event_jButtonCancelActionPerformed

  private void checkboxSettingsQuickModeStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_checkboxSettingsQuickModeStateChanged
  {//GEN-HEADEREND:event_checkboxSettingsQuickModeStateChanged
      if(checkboxSettingsQuickMode.isSelected())
      {
          textfieldSettingsDefaultPrefix.setEnabled(true);
      }
      else
      {
          textfieldSettingsDefaultPrefix.setEnabled(false);
      }
  }//GEN-LAST:event_checkboxSettingsQuickModeStateChanged

  private void jComboBoxRadioComPortActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jComboBoxRadioComPortActionPerformed
  {//GEN-HEADEREND:event_jComboBoxRadioComPortActionPerformed
      // TODO add your handling code here:
  }//GEN-LAST:event_jComboBoxRadioComPortActionPerformed

  private void jMenuItemHelpActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemHelpActionPerformed
  {//GEN-HEADEREND:event_jMenuItemHelpActionPerformed
      File htmlFile = new File("help.html");
      try
      {
          Desktop.getDesktop().browse(htmlFile.toURI());
      } catch(IOException ex)
      {
          Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
      }
  }//GEN-LAST:event_jMenuItemHelpActionPerformed

  private void jMenuItemAboutActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemAboutActionPerformed
  {//GEN-HEADEREND:event_jMenuItemAboutActionPerformed
      jDialogAbout.setVisible(true);
  }//GEN-LAST:event_jMenuItemAboutActionPerformed

  private void jButton20ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton20ActionPerformed
  {//GEN-HEADEREND:event_jButton20ActionPerformed
      jDialogAbout.setVisible(false);
  }//GEN-LAST:event_jButton20ActionPerformed

  private void jCheckBoxShowFreqItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jCheckBoxShowFreqItemStateChanged
  {//GEN-HEADEREND:event_jCheckBoxShowFreqItemStateChanged
      if(jCheckBoxShowFreq.isSelected())
      {
          settings.setShowBandmapFreqColumns(true);
      }
      else
      {
          settings.setShowBandmapFreqColumns(false);
      }

      jtablemodelBandmap.refresh(getBandmapStartFreq());
  }//GEN-LAST:event_jCheckBoxShowFreqItemStateChanged

  private void jCheckBoxAutoBandmapStartFreqItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jCheckBoxAutoBandmapStartFreqItemStateChanged
  {//GEN-HEADEREND:event_jCheckBoxAutoBandmapStartFreqItemStateChanged
      if(jCheckBoxAutoBandmapStartFreq.isSelected())
      {
          settings.setBandmapAutoFreq(true);
      }
      else
      {
          settings.setBandmapAutoFreq(false);
      }
  }//GEN-LAST:event_jCheckBoxAutoBandmapStartFreqItemStateChanged

  private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem7ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem7ActionPerformed
      JFileChooser fc = new JFileChooser();
      fc.setFileFilter(new FileNameExtensionFilter("ADIF files (*.adif)", "adif"));
      fc.setCurrentDirectory(Paths.get(pathToWorkingDir, "/logs/").toFile());
      try
      {
          int returnVal = fc.showSaveDialog(this.getParent());
          if(returnVal != JFileChooser.APPROVE_OPTION)
          {
              return;
          }
      } catch(Exception exc)
      {
          JOptionPane.showMessageDialog(null, "Error when trying to acquire adif file.", "Error", JOptionPane.ERROR_MESSAGE);
          return;
      }

      String absPath = fc.getSelectedFile().getAbsolutePath();
      if(!absPath.endsWith(".adif"))
      {
          absPath = absPath + ".adif";
      }

      File file = new File(absPath);

      if(file.exists())
      {
          JOptionPane.showMessageDialog(null, "File already exists: " + file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
          return;
      }

      // Write the cabrillo file
      try(PrintWriter printWriter = new PrintWriter(absPath))
      {
          printWriter.println("<programid>" + PROGRAM_NAME);
          printWriter.println("<programversion>" + PROGRAM_VERSION);
          printWriter.println("<eoh>");
          for(int i = 0; i < log.getQsoCount(); i++)
          {
              printWriter.println(log.get(i).toStringAdif());
          }
      } catch(FileNotFoundException ex)
      {
          JOptionPane.showMessageDialog(null, "Couldn't generate the adif file", "Error", JOptionPane.ERROR_MESSAGE);
      }

      JOptionPane.showMessageDialog(null, "Adif file created successfully.", "Success...", JOptionPane.INFORMATION_MESSAGE);
  }//GEN-LAST:event_jMenuItem7ActionPerformed

  private void jComboBoxKeyerTypeItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jComboBoxKeyerTypeItemStateChanged
  {//GEN-HEADEREND:event_jComboBoxKeyerTypeItemStateChanged

//    String selected = jComboBoxKeyerType.getSelectedItem().toString();
//    if(selected.compareTo(KeyerTypes.WINKEYER.toString()) == 0)
//    {
//      jComboBoxPttType.setSelectedItem(PttTypes.NONE.toString());
//      jComboBoxPttType.setEnabled(false);
//    }
//    else
//    {
//      jComboBoxPttType.setEnabled(true);
//    }
  }//GEN-LAST:event_jComboBoxKeyerTypeItemStateChanged

    private void increaseKeyerSpeed()
    {
        if(keyerSpeed > 45)
        {
            return;
        }

        keyerSpeed++;
        keyer.setCwSpeed(keyerSpeed);
        jlabelKeyerSpeed.setText(Integer.toString(keyerSpeed) + " WPM");
    }

    private void decreaseKeyerSpeed()
    {
        if(keyerSpeed < 10)
        {
            return;
        }

        keyerSpeed--;
        keyer.setCwSpeed(keyerSpeed);
        jlabelKeyerSpeed.setText(Integer.toString(keyerSpeed) + " WPM");
    }

    /**
     * Sends CW message when we press the enter button inside the callsign
     * textfield
     *
     * @return True if focus should move to Snt field
     */
    private boolean sendEnterSendsMessage()
    {
        // CQ mode
        if(getTypeOfWork().equalsIgnoreCase(Qso.TYPE_OF_WORK_CQ))
        {
            if(isCallsignFieldEmpty())
            {
                pressedF1(); // If callsign field is empty - send CQ
                return false; // do not move focus to Snt field
            }
            else
            {
                pressedF5(); // Send his callsign
                pressedF2(); // and Snt serial number
                return true; // move focus to Snt field
            }

        } // S&P mode
        else
        {
            pressedF4(); // Send my callsign
            return false; // do not move focus to Snt field
        }

    }

    /**
     * Will connect serial port (if needed) and init the keyer
     *
     * @return true if connection was successful
     */
    private boolean connectKeyer()
    {
        try
        {
            keyer = KeyerFactory.create(settings.getKeyerType(),
                    serialportShare.getPort(settings.getKeyerCommportName()));
        } catch(Exception ex)
        {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try
        {
            keyer.init();
        } catch(Exception ex)
        {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            serialportShare.releasePort(keyer.getCommport());
            return false;
        }

        keyer.setCwSpeed(keyerSpeed);
        return true;
    }

    private void disconnectKeyer()
    {
        keyer.terminate();
        serialportShare.releasePort(keyer.getCommport());

        // When disconnected keying will be redirected to the radio (hopefully it will be supported)
        keyer = radioController.getKeyer();
        keyer.setCwSpeed(keyerSpeed);
    }

    private boolean connectPtt()
    {
        if(settings.getKeyerType() == KeyerTypes.WINKEYER)
        {
            JOptionPane.showMessageDialog(null, "PTT will not work when sending CW with WinKeyer. To toggle your transceiver, use the pin provided by the WinKeyer.", "For your information...", JOptionPane.INFORMATION_MESSAGE);
            //return false;
        }

        try
        {
            ptt = new DtrRtsPtt(serialportShare.getPort(settings.getPttCommportName()),
                    settings.getPttType(),
                    settings.getPttDelayInMilliseconds(),
                    settings.getPttTailInMilliseconds());
            ptt.init();

            keyer.usePtt(ptt);
            voiceKeyer.usePtt(ptt);
        } catch(Exception ex)
        {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private void disconnectPtt()
    {
        if(ptt != null)
        {
            ptt.terminate();
            serialportShare.releasePort(ptt.getCommport());
        }
        ptt = null;

        keyer.usePtt(null);
        voiceKeyer.usePtt(null);
    }

    private boolean connectRadio()
    {
        boolean result;

        try
        {
            result = radioController.connect(new LocalRadioControllerListener(),
                    serialportShare.getPort(settings.getRadioCommportName()),
                    settings.getRadioCommportBaudRate());
        } catch(Exception ex)
        {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if(!result)
        {
            JOptionPane.showMessageDialog(null, "Could not connect to radio!", "Unknown Error...", JOptionPane.ERROR_MESSAGE);
        }

        return result;
    }

    private void disconnectRadio()
    {
        radioController.disconnect();
        serialportShare.releasePort(radioController.getSerialPort());
    }

    /**
     * Finds the current working dir and saves into the variable:
     * pathToWorkingDir
     */
    private void determineWorkingDir()
    {
        File file = new File(".");
        String currentDirectory = file.getAbsolutePath();
        currentDirectory = currentDirectory.substring(0, currentDirectory.length() - 1);
        pathToWorkingDir = Paths.get(currentDirectory).toString();
        System.out.println("Current working directory is: " + pathToWorkingDir);
    }

    /**
     * Opens a file chooser which lets the user select the appropriate radio
     * protocol parser.
     *
     * @return true - if the loading was successful
     */
    private boolean loadRadioProtocolParser()
    {
        JFileChooser chooser;

        try
        {
            // Configure the FileChooser for python files
            chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Python files (*.py)", "py"));
            chooser.setCurrentDirectory(Paths.get(pathToWorkingDir, "/pyrig").toFile());

            int returnVal = chooser.showOpenDialog(this.getParent());
            if(returnVal != JFileChooser.APPROVE_OPTION)
            {
                return false;
            }
        } catch(Exception exc)
        {
            LOGGER.log(Level.SEVERE, "Coudln't start file chooser", exc);
            return false;
        }

        boolean result = radioController.loadProtocolParser(chooser.getCurrentDirectory().getName(), // Module name
                chooser.getSelectedFile().getName());     // Class name
        if(result == false)
        {
            JOptionPane.showMessageDialog(null, "Error when trying to load the radio protocol parser file!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Show the serial settings that we are going to use when connecting to this radio
        JOptionPane.showMessageDialog(null, radioController.getInfo());
        return true;
    }

    /**
     * Reads the info from the entry window and if all data is valid it saves it
     * into the Log
     *
     * @return true - if the QSO was successfully logged false -
     */
    boolean addEntryToLog()
    {
        // Do some validation of the data
        // ------------------------------
        if(isCallsignFieldEmpty() || !Qso.isValidCallsign(getCallsignFromTextField()))
        {
            JOptionPane.showMessageDialog(null, "Invalid callsign!");
            jtextfieldCallsign.requestFocusInWindow();
            return false;
        }

        if(!Qso.isValidSerial(jtextfieldSnt.getText()))
        {
            JOptionPane.showMessageDialog(null, "Invalid Snt!");
            jtextfieldSnt.requestFocusInWindow();
            return false;
        }

        if(!Qso.isValidSerial(jtextfieldRcv.getText()))
        {
            JOptionPane.showMessageDialog(null, "Invalid Rcv!");
            jtextfieldRcv.requestFocusInWindow();
            return false;
        }

        // Format
        String Rcv = jtextfieldRcv.getText().replaceAll("\\s", ""); // Remove any empty spaces
        Rcv = jtextfieldRcv.getText().substring(0, 3) + " " + jtextfieldRcv.getText().substring(3); // Add blank space between the two parts of the SNT

        String Snt = jtextfieldSnt.getText().replaceAll("\\s", ""); // Remove any empty spaces
        Snt = Snt.substring(0, 3) + " " + Snt.substring(3);  // Add blank space between the two parts of the RCV

        // Add qso to log
        // ------------------------------
        Qso qso;
        try
        {
            qso = new Qso(getFreq(),
                    getMode(),
                    settings.getMyCallsign(),
                    getCallsignFromTextField(),
                    Snt,
                    Rcv,
                    getTypeOfWork());
        } catch(Exception exc)
        {
            return false;
        }

        jtablemodelLog.addRow(qso);
        return true;
    }

    /**
     * Determines if the current Type of work is SP or CQ
     *
     * @return
     */
    private String getTypeOfWork()
    {
        if(jradiobuttonSP.isSelected())
        {
            return Qso.TYPE_OF_WORK_SP;
        }
        else
        {
            return Qso.TYPE_OF_WORK_CQ;
        }
    }

    private KeyerTypes getSelectedKeyerType()
    {
        return KeyerTypes.valueOf(jComboBoxKeyerType.getSelectedItem().toString());
    }

    private PttTypes getSelectedPttType()
    {
        return PttTypes.valueOf(jComboBoxPttType.getSelectedItem().toString());
    }

    /**
     * Determines the current working mode. Takes into account if the connected
     * to the radio or not.
     *
     * @return - String describing the mode (e.g. "cw", "ssb")
     */
    private RadioModes getMode()
    {
        // If radio is connected get the frequency from there
        if(radioController.isConnected())
        {
            return radioController.getMode();
        } // If no radio is connected - read the mode from the combobox model
        else
        {
            String md = jcomboboxMode.getSelectedItem().toString();
            return RadioModes.valueOf(md);
        }
    }

    /**
     * Determines the current working frequency. Takes into account if the
     * connected to the radio or not.
     *
     * @return - frequency in Hz
     */
    private int getFreq()
    {
        int freq;

        // If radio is connected get the frequency from there
        if(radioController.isConnected())
        {
            freq = radioController.getFrequency();
        } // If no radio is connected - read the freq from the dropdown box
        else
        {
            String temp = jcomboboxBand.getSelectedItem().toString();
            // convert to Hz
            freq = Math.round(Float.parseFloat(temp) * 1000000);
        }

        return freq;
    }

    /**
     * Gets the callsign from the jtextfieldCallsign. If the callsign was
     * inserted in the short form (e.g. HH) this function will return the full
     * form (i.e. LZ2HH)
     *
     * @return - the callsign in its full form (e.g. LZ6HH)
     */
    private String getCallsignFromTextField()
    {
        String callsign = jtextfieldCallsign.getText();

//    if(settings.isQuickCallsignModeEnabled())
//    {
//      callsign = settings.getDefaultPrefix()+callsign;
//    }
        return callsign;
    }

    /**
     * Prints info concerning the callsign: NEW - If no qso before OK - Qso
     * before but the required time has elapsed DUPE time left... - Qso before
     * and the required time has not elapsed
     *
     * @param callsign
     * @return
     */
    private String getCallsignStatusText(String callsign)
    {
        String statusText = "";

        Qso qso = log.getLatestQso(callsign, getMode());

        // Unknown callsign - OK to work
        if(qso == null)
        {
            statusText = "NEW";
        }
        else
        {
            // DUPE
            if(Misc.getSecondsLeft(qso.getElapsedSeconds(), settings.getQsoRepeatPeriod()) > 0)
            {
                // Print DUPE
                statusText = statusText.concat("DUPE   ");

                //Print the time left till next possible contact
                statusText = statusText.concat("time left "
                        + TimeUtils.getTimeLeftFormatted(Misc.getSecondsLeft(qso.getElapsedSeconds(), settings.getQsoRepeatPeriod())));
                // Make it red
                statusText = "<html><font color=red>" + statusText + "</font></html>";
            }
            else
            {
                statusText = "OK";
            }
        }

        return statusText;
    }

    /**
     * Cleans/prepares the entry fields for the next QSO
     */
    private void initEntryFields()
    {
        // Clean the callsign field
        if(settings.isQuickCallsignModeEnabled())
        {
            jtextfieldCallsign.setText(settings.getDefaultPrefix());
        }
        else
        {
            jtextfieldCallsign.setText("");
        }

        // Set the new Snt 
        setSntField();
        //jtextfieldSnt.setText(log.getNextSentReport());
        // Cean the Rcv field
        jtextfieldRcv.setText("");
        // Clean the callsign status
        jlabelCallsignStatus.setText("NEW");
        // Set focus to callsign field
        jtextfieldCallsign.requestFocusInWindow();

        jtablemodelIncomingQso.init();
    }

    private void setSntField()
    {
        String snt = settings.getContestExchange();

        // {#} - is [serial number]
        String serial = String.format("%03d", log.getQsoCount() + 1);
        snt = snt.replaceAll("\\{#\\}", serial);

        // {$} - is [first part of last Rcv] 
        String lastRcv = log.getFirstPartOfLastRcv();
        snt = snt.replaceAll("\\{\\$\\}", lastRcv);

        jtextfieldSnt.setText(snt);
    }

    /**
     * @return Returns a new DefaultComboBoxModel containing all available COM
     * ports
     */
    private DefaultComboBoxModel getComportsComboboxModel()
    {
        String[] portNames = SerialPortList.getPortNames();
        return new DefaultComboBoxModel(portNames);
    }

    private void storeFonts()
    {
        // Store the fonts being in use
        settings.setFont(ApplicationSettings.FontIndex.BANDMAP, jtableBandmap.getFont());
        settings.setFont(ApplicationSettings.FontIndex.CALLSIGN, jtextfieldCallsign.getFont());
        settings.setFont(ApplicationSettings.FontIndex.INCOMING_QSO, jtableIncomingQso.getFont());
        settings.setFont(ApplicationSettings.FontIndex.LOG, jtableLog.getFont());
        settings.setFont(ApplicationSettings.FontIndex.RCV, jtextfieldRcv.getFont());
        settings.setFont(ApplicationSettings.FontIndex.SNT, jtextfieldSnt.getFont());
    }

    private void restoreFonts()
    {
        // Restore the fonts
        jtextfieldCallsign.setFont(settings.getFonts(ApplicationSettings.FontIndex.CALLSIGN));
        jtextfieldSnt.setFont(settings.getFonts(ApplicationSettings.FontIndex.SNT));
        jtextfieldRcv.setFont(settings.getFonts(ApplicationSettings.FontIndex.RCV));
        jtableBandmap.setFont(settings.getFonts(ApplicationSettings.FontIndex.BANDMAP));
        jtableIncomingQso.setFont(settings.getFonts(ApplicationSettings.FontIndex.INCOMING_QSO));
        jtableLog.setFont(settings.getFonts(ApplicationSettings.FontIndex.LOG));
    }

    /**
     * Initialize the controls of the main windows
     */
    private void initMainWindow(boolean isStartup)
    {
        if(isStartup)
        {
            // Restore the last used dimensions for the different frames
            this.setBounds(settings.getFrameDimensions(ApplicationSettings.FrameIndex.JFRAME));
            intframeBandmap.setBounds(settings.getFrameDimensions(ApplicationSettings.FrameIndex.BANDMAP));
            intframeEntryWindow.setBounds(settings.getFrameDimensions(ApplicationSettings.FrameIndex.ENTRY));
            intframeTimeToNextQso.setBounds(settings.getFrameDimensions(ApplicationSettings.FrameIndex.INCOMING_QSO));
            intframeLog.setBounds(settings.getFrameDimensions(ApplicationSettings.FrameIndex.LOG));
            intframeRadioConnection.setBounds(settings.getFrameDimensions(ApplicationSettings.FrameIndex.RADIO));
            intframeMisc.setBounds(settings.getFrameDimensions(ApplicationSettings.FrameIndex.SETTINGS));

            // Restore the bandmap settings
            jcomboboxStepInHz.setSelectedItem(Integer.toString(settings.getBandmapStepInHz()));
            jcomboboxColumnCount.setSelectedItem(Integer.toString(settings.getBandmapColumnCount()));
            jcomboboxRowCount.setSelectedItem(Integer.toString(settings.getBandmapRowCount()));
            jCheckBoxShowFreq.setSelected(settings.isShowBandmapFreqColumns());
            //jtextfieldFreqWidth.setText(settings.get);

            restoreFonts();
        }

        // Update the Function keys button text
        jButton4.setText("F4 " + settings.getMyCallsign());
        jButton6.setText("F6 " + settings.getFunctionKeyMessage(5));
        jButton7.setText("F7 " + settings.getFunctionKeyMessage(6));
        jButton8.setText("F8 " + settings.getFunctionKeyMessage(7));
        jButton9.setText("F9 " + settings.getFunctionKeyMessage(8));
        //jButton10.setText("F10 "+settings.getFunctionKeyText(9));

        // Set the CQ frequency
        jlabelCqFreq.setText(Misc.formatFrequency(Integer.toString(cqFrequency)));

        initEntryFields();
    }

    /**
     * User has opened the setting dialog and we need to load the state of the
     * controls
     */
    private void initSettingsDialog()
    {
        //--------------------------------
        // Radio
        //--------------------------------
        // Commport
        if(((DefaultComboBoxModel) jComboBoxRadioComPort.getModel()).getIndexOf(settings.getRadioCommportName()) < 0)
        {
            ((DefaultComboBoxModel) jComboBoxRadioComPort.getModel()).addElement(settings.getRadioCommportName()); // Add last used commport if not in the list
        }
        jComboBoxRadioComPort.setSelectedItem(settings.getRadioCommportName());
        // Baud rate
        jComboBoxRadioComPortBaudRate.setSelectedItem(CommUtils.BaudRate.getName(settings.getRadioCommportBaudRate()));
        // DTR
        if(settings.isRadioCommportDtrOn())
        {
            jRadioButtonRadioDtrOn.setSelected(true);
        }
        else
        {
            jRadioButtonRadioDtrOff.setSelected(true);
        }
        // RTS
        if(settings.isRadioCommportRtsOn())
        {
            jRadioButtonRadioRtsOn.setSelected(true);
        }
        else
        {
            jRadioButtonRadioRtsOff.setSelected(true);
        }

        //--------------------------------
        // Keyer
        //--------------------------------
        // Type
        jComboBoxKeyerType.setSelectedItem(settings.getKeyerType().toString());
        // Commport
        if(((DefaultComboBoxModel) jComboBoxKeyerComPort.getModel()).getIndexOf(settings.getKeyerCommportName()) < 0)
        {
            // Add last used commport if not in the list
            ((DefaultComboBoxModel) jComboBoxKeyerComPort.getModel()).addElement(settings.getKeyerCommportName());
        }
        jComboBoxKeyerComPort.setSelectedItem(settings.getKeyerCommportName());

        //--------------------------------
        // PTT
        //--------------------------------
        // Type
        jComboBoxPttType.setSelectedItem(settings.getPttType().toString());
//    // Disable editing if Winkey is selected
//    if(settings.getKeyerType() == KeyerTypes.WINKEYER)
//      jComboBoxPttType.setEnabled(false);
        // Commport
        if(((DefaultComboBoxModel) jComboBoxPttCommport.getModel()).getIndexOf(settings.getPttCommportName()) < 0)
        {
            ((DefaultComboBoxModel) jComboBoxPttCommport.getModel()).addElement(settings.getPttCommportName()); // Add last used commport if not in the list
        }
        jComboBoxPttCommport.setSelectedItem(settings.getPttCommportName());
        // Delay
        jTextFieldPttDelay.setText(Integer.toString(settings.getPttDelayInMilliseconds()));
        jTextFieldPttTailDelay.setText(Integer.toString(settings.getPttTailInMilliseconds()));
        //--------------------------------
        // Callsign
        //--------------------------------
        textfieldSettingsMyCallsign.setText(settings.getMyCallsign().toUpperCase());

        //--------------------------------
        // Other 
        //--------------------------------
        // Default prefix
        checkboxSettingsQuickMode.setSelected(settings.isQuickCallsignModeEnabled());
        textfieldSettingsDefaultPrefix.setText(settings.getDefaultPrefix());
        if(settings.isQuickCallsignModeEnabled() == false)
        {
            textfieldSettingsDefaultPrefix.setEnabled(false); // Disable the "default prefix" text field if the "Quick callsign mode" is disabled
        }    // T as 0
        checkboxSendLeadingZeroAsT.setSelected(settings.isSendLeadingZeroAsT());
        checkboxSendZeroAsT.setSelected(settings.isSendZeroAsT());
        // Bandmap auto set start frequncy
        jCheckBoxAutoBandmapStartFreq.setSelected(settings.isBandmapAutoFreq());
        // ESM 
        checkboxESM.setSelected(settings.isEmsEnabled());
        //TODO checkboxF1JumpsToCq.setSelected(settings.isAutoCqJump()); 
        // Incoming qso hide after
        jTextFieldTimeToNextQso.setText(Integer.toString(settings.getIncomingQsoHiderAfter()));

        //--------------------------------
        // Function keys 
        //--------------------------------
        jtextfieldfF1.setText(settings.getFunctionKeyMessage(0));
        jTextFieldF2.setText(settings.getFunctionKeyMessage(1));
        jtextfieldfF3.setText(settings.getFunctionKeyMessage(2));
        jtextfieldfF6.setText(settings.getFunctionKeyMessage(5));
        jtextfieldfF7.setText(settings.getFunctionKeyMessage(6));
        jtextfieldF8.setText(settings.getFunctionKeyMessage(7));
        jtextfieldF9.setText(settings.getFunctionKeyMessage(8));
        jtextfieldF10.setText(settings.getFunctionKeyMessage(9));

        //--------------------------------
        // Contest rules
        //--------------------------------
        // Repeat period in mins
        jtextfieldQsoRepeatPeriod.setText(Integer.toString(settings.getQsoRepeatPeriod()));
        // Exchange rule
        jTextFieldContestExchange.setText(settings.getContestExchange());
    }

    /**
     * User has closed the setting dialog and we need to save the state of the
     * controls
     */
    private boolean storeSettings()
    {
        //--------------------------------
        // Radio
        //--------------------------------
        // Commport
        if(jComboBoxRadioComPort.getSelectedItem() != null)
        {
            settings.setRadioCommportName(jComboBoxRadioComPort.getSelectedItem().toString());
        }
        // Baud rate
        String comport = jComboBoxRadioComPortBaudRate.getSelectedItem().toString();
        settings.setRadioCommportBaudRate(CommUtils.BaudRate.getBaudRate(comport));
        // DTR
        settings.setRadioCommportDtr(jRadioButtonRadioDtrOn.isSelected());
        // RTS
        settings.setRadioCommportRts(jRadioButtonRadioRtsOn.isSelected());

        //--------------------------------
        // Keyer
        //--------------------------------
        // Type
        settings.setKeyerType(getSelectedKeyerType());
        // Commport
        if(jComboBoxKeyerComPort.getSelectedItem() != null)
        {
            settings.setKeyerCommPortName(jComboBoxKeyerComPort.getSelectedItem().toString());
        }

        //--------------------------------
        // PTT
        //--------------------------------
        // Type
        settings.setPttType(getSelectedPttType());
        // Commport
        if(jComboBoxPttCommport.getSelectedItem() != null)
        {
            settings.setPttCommportName(jComboBoxPttCommport.getSelectedItem().toString());
        }
        // Delay
        settings.setPttDelayInMilliseconds(Integer.parseInt(jTextFieldPttDelay.getText()));
        settings.setPttTailInMilliseconds(Integer.parseInt(jTextFieldPttTailDelay.getText()));

        //--------------------------------
        // Callsign
        //--------------------------------
        if(!Qso.isValidCallsign(textfieldSettingsMyCallsign.getText()))
        {
            JOptionPane.showMessageDialog(null, "Invalid callsign!"); // Validate myCallsign
            return false;
        }
        settings.setMyCallsign(textfieldSettingsMyCallsign.getText());

        //--------------------------------
        // Other 
        //--------------------------------
        // Default prefix
        settings.setQuickCallsignMode(checkboxSettingsQuickMode.isSelected());
        settings.setDefaultPrefix(textfieldSettingsDefaultPrefix.getText());
        //TODO settings.setAutoCqJump(checkboxF1JumpsToCq.isSelected());
        settings.setEmsEnabled(checkboxESM.isSelected());
        settings.setSendLeadingZeroAsT(checkboxSendLeadingZeroAsT.isSelected());
        settings.setSendZeroAsT(checkboxSendZeroAsT.isSelected());
        // Hide after
        settings.setIncomingQsoHiderAfter(Integer.parseInt(jTextFieldTimeToNextQso.getText()));

        //--------------------------------
        // Function keys 
        //--------------------------------
        settings.setFunctionKeyMessage(0, jtextfieldfF1.getText());
        settings.setFunctionKeyMessage(1, jTextFieldF2.getText());
        settings.setFunctionKeyMessage(2, jtextfieldfF3.getText());
        settings.setFunctionKeyMessage(5, jtextfieldfF6.getText());
        settings.setFunctionKeyMessage(6, jtextfieldfF7.getText());
        settings.setFunctionKeyMessage(7, jtextfieldF8.getText());
        settings.setFunctionKeyMessage(8, jtextfieldF9.getText());
        settings.setFunctionKeyMessage(9, jtextfieldF10.getText());

        //--------------------------------
        // Contest rules
        //--------------------------------
        // Repeat period 
        settings.setQsoRepeatPeriod(Integer.parseInt(jtextfieldQsoRepeatPeriod.getText()));
        // Exchange rule
        settings.setContestExchange(jTextFieldContestExchange.getText());

        settings.SaveSettingsToDisk(); // Save all settings to disk

        initEntryFields();
        return true;
    }

    /**
     *
     * @return Frequency in Hz
     */
    private int getBandmapStartFreq()
    {
        int freq = 3500000;

        try
        {
            freq = Integer.parseInt(jtextfieldBandmapStartFreq.getText());
            freq *= 1000;
        } catch(Exception exc)
        {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, exc);
        }

        if(freq < 1800000)
        {
            return 1800000;
        }
        if(freq > 28000000)
        {
            return 28000000;
        }

        return freq;
    }

    /**
     * Sets the jtextfiled with the bandmap frequency
     *
     * @param freqInHz
     */
    private void setBandmapStartFreq(int freqInHz)
    {
        if(freqInHz < 1800000)
        {
            freqInHz = 1800000;
        }
        if(freqInHz > 28000000)
        {
            freqInHz = 28000000;
        }

        freqInHz = freqInHz / 1000;

        String frequency = Integer.toString(freqInHz);
        if(!jtextfieldBandmapStartFreq.getText().equals(frequency))
        {
            jtextfieldBandmapStartFreq.setText(frequency);
        }
    }

    private void pressedF1()
    {
        // if "jump to cq freq" is enabled we will jump to the cq frequency (cq freq can be set through the button "set cq freq"
        if(jcheckboxF1jumpsToCq.isSelected())
        {
            if(getFreq() < (cqFrequency - 50) || getFreq() > (cqFrequency + 50))
            {
                try
                {
                    radioController.setFrequency(cqFrequency);
                    Thread.sleep(300); // w8 for radio to jump to frequency
                } catch(InterruptedException ex)
                {
                    Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } // if not enabled - remember the cq frequency
        else
        {
            cqFrequency = getFreq();
            jlabelCqFreq.setText(Misc.formatFrequency(Integer.toString(cqFrequency)));
        }

        String text = settings.getFunctionKeyMessage(0);  // Get the text for the F1 key
        text = text.replaceAll("\\{mycall\\}", settings.getMyCallsign()); // Substitute {mycall} with my callsign
        text = text + " ";
        sendCw(text); // Send to keyer
        playVoiceKeyer(VoiceKeyer.Keys.F1);

        // Select the CQ radio button
        jradiobuttonCQ.setSelected(true);

        int period = 0;
        // Continious CQ is enabled ...
        if(jcheckboxContinuousCq.isSelected())
        {
            try
            {
                period += Integer.parseInt(jtextfieldContinuousCqPeriod.getText());
            } catch(NumberFormatException numberFormatException)
            {
                period += 3000; // default length if crap is entered by the user
            }

            if(getMode() == RadioModes.CW || getMode() == RadioModes.CWR)
            {
                period += MorseCode.getDurationOfMessage(text, keyerSpeed);
            }
            else if(getMode() == RadioModes.LSB || getMode() == RadioModes.USB)
            {
                period += VoiceKeyer.getF1LengthInSeconds();
            }

            if(timerContinuousCq.isRunning())
            {
                timerContinuousCq.stop();
            }
            timerContinuousCq.setInitialDelay(period);
            timerContinuousCq.start();
        }
    }

    private void pressedF2()
    {
        String toSend = settings.getFunctionKeyMessage(1).toLowerCase();

        // Check if "{exch}" is present 
        if(toSend.contains("{exch}"))
        {
            String exchange;

            // If jtextfieldCallsign is empty we should get exchange from last QSO
            if(isCallsignFieldEmpty() && log.getSize() > 0)
            {
                exchange = log.get(log.getSize()-1).getSnt();
            }
            else
            {
                exchange = jtextfieldSnt.getText();
            }

            if(settings.isSendLeadingZeroAsT())
            {
                exchange = RcvFormatter.leadingZerosToT(exchange);
            }

            if(settings.isSendZeroAsT())
            {
                exchange = exchange.replace('0', 't');
            }

            toSend = toSend.replaceAll("\\{exch\\}", exchange);  // Substitute all "{exch}"
        }

        toSend = toSend.replaceAll("\\{mycall\\}", settings.getMyCallsign());  // Substitute all "{mycall}"

        sendCw(toSend + " ");
    }

    private void pressedF3()
    {
        sendCw(settings.getFunctionKeyMessage(2) + " ");
        playVoiceKeyer(VoiceKeyer.Keys.F3);
    }

    private void pressedF4()
    {
        sendCw(settings.getMyCallsign() + " ");
        playVoiceKeyer(VoiceKeyer.Keys.F4);
    }

    private void pressedF5()
    {
        sendCw(getCallsignFromTextField() + " ");
    }

    private void pressedF6()
    {
        sendCw(settings.getFunctionKeyMessage(5) + " ");
    }

    private void pressedF7()
    {
        sendCw(settings.getFunctionKeyMessage(6) + " ");
    }

    private void pressedF8()
    {
        sendCw(settings.getFunctionKeyMessage(7) + " ");
    }

    private void pressedF9()
    {
        sendCw(settings.getFunctionKeyMessage(8) + " ");
    }

    private void pressedF10()
    {
        sendCw(settings.getFunctionKeyMessage(9) + " ");
    }

    private void pressedF11()
    {
        if(Qso.isValidCallsign(getCallsignFromTextField()))
        {
            jtablemodelBandmap.addSpot(getCallsignFromTextField(), getFreq(), getMode());
        }
        initEntryFields();
    }

    boolean isCallsignFieldEmpty()
    {
        if(settings.isQuickCallsignModeEnabled())
        {
            return settings.getDefaultPrefix().compareTo(jtextfieldCallsign.getText()) == 0;
        }

        return jtextfieldCallsign.getText().isEmpty();
    }

    private void pressedEsc()
    {
        keyer.stopSendingCw();
        voiceKeyer.stopVoice();
        if(timerContinuousCq.isRunning())
        {
            timerContinuousCq.stop();
        }
    }

    private void pressedF12()
    {
        initEntryFields();
    }

    private int calculateFrequencyChange()
    {
        return Math.abs(cqFrequency - getFreq());
    }

    /**
     * Will try to transmit CW is mode is CW or CWR
     */
    void sendCw(String msg)
    {
        if(getMode() == RadioModes.CW || getMode() == RadioModes.CWR)
        {
            keyer.sendCw(msg);
            jLabelStatus.setText(msg);
        }

    }

    void playVoiceKeyer(VoiceKeyer.Keys key)
    {
        if(getMode() == RadioModes.LSB || getMode() == RadioModes.USB)
        {
            voiceKeyer.play(key);
            jLabelStatus.setText("Playing file:"+key.getFileName());
        }
    }
    

    class LocalRadioControllerListener implements RadioController.RadioControllerListener
    {

        private void updateBandmapStartFreq(int freq, RadioModes mode)
        {
            switch(mode)
            {
                case CW:
                case CWR:
                    if(Misc.freqToBand(freq).equals("160"))
                    {
                        setBandmapStartFreq(1800000);
                    }
                    else if(Misc.freqToBand(freq).equals("80"))
                    {
                        setBandmapStartFreq(3500000);
                    }
                    else if(Misc.freqToBand(freq).equals("40"))
                    {
                        setBandmapStartFreq(7000000);
                    }
                    else if(Misc.freqToBand(freq).equals("20"))
                    {
                        setBandmapStartFreq(14000000);
                    }
                    else if(Misc.freqToBand(freq).equals("15"))
                    {
                        setBandmapStartFreq(21000000);
                    }
                    else if(Misc.freqToBand(freq).equals("10"))
                    {
                        setBandmapStartFreq(28000000);
                    }
                    break;

                case LSB:
                case USB:
                    if(Misc.freqToBand(freq).equals("160"))
                    {
                        setBandmapStartFreq(1800000);
                    }
                    else if(Misc.freqToBand(freq).equals("80"))
                    {
                        setBandmapStartFreq(3700000);
                    }
                    else if(Misc.freqToBand(freq).equals("40"))
                    {
                        setBandmapStartFreq(7060000);
                    }
                    else if(Misc.freqToBand(freq).equals("20"))
                    {
                        setBandmapStartFreq(14100000);
                    }
                    else if(Misc.freqToBand(freq).equals("15"))
                    {
                        setBandmapStartFreq(21150000);
                    }
                    else if(Misc.freqToBand(freq).equals("10"))
                    {
                        setBandmapStartFreq(28400000);
                    }
                    break;
            }
        }

        @Override
        public void frequency()
        {
            /* Create and display the form */
            java.awt.EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    // Update the Radio panel
                    jtextfieldFrequency.setText(radioController.getActiveVfo().toString() + " " + Integer.toString(radioController.getFrequency()));

                    // Set to S&P if in CQ mode and CQ frequency has changed with 500Hz
                    if(jradiobuttonCQ.isSelected() && calculateFrequencyChange() > 500)
                    {
                        jradiobuttonSP.setSelected(true);
                        pressedEsc();
                    }

                    // We need to repaint the bandmap table so that the fequency marker is updated
                    jtableBandmap.repaint();

                    if(settings.isBandmapAutoFreq())
                    {
                        updateBandmapStartFreq(radioController.getFrequency(), radioController.getMode());
                    }
                }
            });

        }

        @Override
        public void mode()
        {
            /* Create and display the form */
            java.awt.EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    jtextfieldMode.setText(radioController.getMode().toString());
                }
            });
        }

        @Override
        public void vfo()
        {
            /* Create and display the form */
            java.awt.EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    jtextfieldMode.setText(radioController.getMode().toString());
                    jtextfieldFrequency.setText(radioController.getActiveVfo().toString() + " " + Integer.toString(radioController.getFrequency()));

                    // Set to S&P if in CQ mode and CQ frequency has changed with 500Hz
                    if(jradiobuttonCQ.isSelected() && calculateFrequencyChange() > 500)
                    {
                        jradiobuttonSP.setSelected(true);
                    }

                    // We need to repaint the bandmap table so that the fequency marker is updated
                    jtableBandmap.repaint();
                }
            });
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[])
    {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
     * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try
        {
            for(javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
            {
                if("Nimbus".equals(info.getName()))
                {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch(ClassNotFoundException ex)
        {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch(InstantiationException ex)
        {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch(IllegalAccessException ex)
        {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch(javax.swing.UnsupportedLookAndFeelException ex)
        {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                new MainWindow().setVisible(true);
            }
        });
    }

    /**
     * Will return true if new db4o log file can be created. Theabsolute path
     * for the log file will be written in logDbFile.
     *
     * @return true in case the db4o file can be created
     */
    private boolean createNewLog()
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Log database files (*.db4o)", "db4o"));
        fc.setCurrentDirectory(Paths.get(pathToWorkingDir, "/logs/").toFile());
        try
        {
            int returnVal = fc.showSaveDialog(this.getParent());
            if(returnVal != JFileChooser.APPROVE_OPTION)
            {
                return false;
            }
        } catch(Exception exc)
        {
            JOptionPane.showMessageDialog(null, "Error when trying to acquire log database file.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        logDbFile = fc.getSelectedFile().getAbsolutePath();
        if(!logDbFile.endsWith(".db4o"))
        {
            logDbFile = logDbFile + ".db4o";
        }

        File file = new File(logDbFile);

        if(file.exists())
        {
            JOptionPane.showMessageDialog(null, "File already exists: " + file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Will return true if existing .db4o log file was found. The log file
     * absolute path will be written in logDbFile.
     *
     * @return true in case the db4o file was found.
     */
    private boolean findExistingLog()
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Log database files (*.db4o)", "db4o"));
        fc.setCurrentDirectory(Paths.get(pathToWorkingDir, "/logs/").toFile());
        try
        {
            int returnVal = fc.showOpenDialog(this.getParent());
            if(returnVal != JFileChooser.APPROVE_OPTION)
            {
                return false;
            }
        } catch(Exception exc)
        {
            JOptionPane.showMessageDialog(null, "Error when trying to acquire log database file.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        logDbFile = fc.getSelectedFile().getAbsolutePath();
        File file = new File(logDbFile);

        if(!file.exists())
        {
            JOptionPane.showMessageDialog(null, "Log file not found: " + file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private class MyDispatcher implements KeyEventDispatcher
    {

        long lastCtrlAltPressed = 0;

        @Override
        public boolean dispatchKeyEvent(KeyEvent evt)
        {
            if(evt.getID() != KeyEvent.KEY_RELEASED)
            {
                return false;
            }

            // Function keys events
            switch(evt.getKeyCode())
            {
                case KeyEvent.VK_CONTROL:
                case KeyEvent.VK_ALT:
                    lastCtrlAltPressed = System.currentTimeMillis();
                    break;

                case KeyEvent.VK_F1:
                    if(evt.isControlDown() || evt.isAltDown()
                            || System.currentTimeMillis() - lastCtrlAltPressed < 100)  // sometimes people release the Alt key earlier
                    {
                        cqFrequency = getFreq();
                        jlabelCqFreq.setText(Misc.formatFrequency(Integer.toString(cqFrequency)));
                        evt.consume();
                    }
                    else
                    {
                        pressedF1();
                        evt.consume();
                    }
                    break;

                case KeyEvent.VK_F2:
                    pressedF2();
                    evt.consume();
                    break;

                case KeyEvent.VK_F3:
                    pressedF3();
                    evt.consume();
                    break;

                case KeyEvent.VK_F4:
                    pressedF4();
                    evt.consume();
                    break;

                case KeyEvent.VK_F5:
                    pressedF5();
                    evt.consume();
                    break;

                case KeyEvent.VK_F6:
                    pressedF6();
                    evt.consume();
                    break;

                case KeyEvent.VK_F7:
                    pressedF7();
                    evt.consume();
                    break;

                case KeyEvent.VK_F8:
                    pressedF8();
                    evt.consume();
                    break;

                case KeyEvent.VK_F9:
                    pressedF9();
                    evt.consume();
                    break;

                case KeyEvent.VK_QUOTE: // ' = Sends TU message and enter in log
                    if(addEntryToLog())
                    {
                        initEntryFields();
                        jtextfieldCallsign.requestFocusInWindow(); // Move focus to Callsign field
                        pressedF3();
                    }
                    evt.consume();
                    break;

                case KeyEvent.VK_SEMICOLON: // ;  = Sends call and exchange
                    pressedF5();
                    pressedF2();
                    evt.consume();
                    break;

//        case KeyEvent.VK_F10:
//          pressedF10();
//          evt.consume();
//          break;
                case KeyEvent.VK_F11:
                    pressedF11();
                    evt.consume();
                    break;

                case KeyEvent.VK_W:
                    if(evt.isControlDown() || evt.isAltDown()
                            || System.currentTimeMillis() - lastCtrlAltPressed < 100)  // sometimes people release the Alt key earlier
                    {
                        pressedF12();
                        evt.consume();
                        break;
                    }
                    break;

                case KeyEvent.VK_F12:
                    pressedF12();
                    evt.consume();
                    break;

                case KeyEvent.VK_PAGE_UP:
                    increaseKeyerSpeed();
                    evt.consume();
                    break;

                case KeyEvent.VK_PAGE_DOWN:
                    decreaseKeyerSpeed();
                    evt.consume();
                    break;

                case KeyEvent.VK_ESCAPE:
                    pressedEsc();
                    break;
            }
            return false;
        }
    }

    /**
     * Used for coloring the cells within the IncomingQso table
     */
    class IncomingQsoTableCellRender extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            //SJComponent jc = (JComponent) comp;

            if(jtablemodelIncomingQso.containsExpiredCallsign(row, column))
            {
                setForeground(Color.BLUE);
            }
            else
            {
                setForeground(Color.black);
            }

            // Set row height
            FontMetrics fm = comp.getFontMetrics(comp.getFont());

            if(table.getRowHeight() != fm.getHeight())
            {
                table.setRowHeight(fm.getHeight());
            }

            return this;
        }
    }

    /**
     * Used for coloring the cells within the Bandmap table
     */
    class BandmapTableCellRender extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            //SJComponent jc = (JComponent) comp;

            //((JLabel)comp).setToolTipText(Integer.toString(jtablemodelBandmap.cellToFreq(row, column))); find a better way to set it checking all the time
            // Show the current freq of the radio by highlighting the appropriate cell
            if(jtablemodelBandmap.isCurrentFreqInThisCell(row, column, getFreq()))
            {
                setBackground(Color.LIGHT_GRAY);
            } // Show CQ freq in green
            else if(jtablemodelBandmap.isCurrentFreqInThisCell(row, column, cqFrequency))
            {
                setBackground(Color.GREEN);
            }
            else
            {
                setBackground(Color.white);
                setForeground(Color.black);
            }

            // Set row height
            FontMetrics fm = comp.getFontMetrics(comp.getFont());

            if(table.getRowHeight() != fm.getHeight())
            {
                table.setRowHeight(fm.getHeight());
            }

            return this;
        }
    }

    /**
     * Used for coloring the cells within the IncomingQso table
     */
    class LogTableCellRender extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            //SJComponent jc = (JComponent) comp;

            // Set row height
            FontMetrics fm = comp.getFontMetrics(comp.getFont());

            if(table.getRowHeight() != fm.getHeight())
            {
                table.setRowHeight(fm.getHeight());
            }

            return this;
        }
    }

    class DigitsOnlyFilter extends DocumentFilter
    {

        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException
        {
            fb.insertString(offset, text.toUpperCase().replaceAll("[^0-9]+", ""), attrs);
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
        {
            fb.replace(offset, length, text.toUpperCase().replaceAll("[^0-9]+", ""), attrs);
        }
    }

    class PttDelayDocumentFilter extends DocumentFilter
    {

        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException
        {
            int currentLength = fb.getDocument().getLength();
            int overlimit = (currentLength + text.length()) - 3/*max length*/;
            if(overlimit > 0)
            {
                fb.insertString(offset, text.substring(0, text.length() - overlimit), attrs);
                return;
            }
            fb.insertString(offset, text.toUpperCase().replaceAll("[^0-9]+", ""), attrs);
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
        {
            int currentLength = fb.getDocument().getLength();
            int overlimit = (currentLength + text.length()) - 3/*max length*/ - length;
            if(overlimit > 0)
            {
                fb.insertString(offset, text.substring(0, text.length() - overlimit), attrs);
                return;
            }
            fb.replace(offset, length, text.toUpperCase().replaceAll("[^0-9]+", ""), attrs);
        }
    }

    class CallsignDocumentFilter extends DocumentFilter
    {

        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException
        {
            fb.insertString(offset, text.toUpperCase().replaceAll("[^A-Z0-9/]*$", ""), attrs);
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
        {
            fb.replace(offset, length, text.toUpperCase().replaceAll("[^A-Z0-9/]*$", ""), attrs);
        }
    }

    class SerialNumberDocumentFilter extends DocumentFilter
    {

        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException
        {
            text = text.replaceAll("[^A-Za-z0-9]*$", "");
            int overlimit = fb.getDocument().getLength() + text.length() - SERIAL_NUMBER_MAX_LENGTH;
            if(overlimit > 0)
            {
                fb.insertString(offset, text.substring(0, text.length() - overlimit), attr);
                return;
            }
            fb.insertString(offset, text.toUpperCase(), attr);
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fb, // FilterBypass that can be used to mutate Document
                int offset, // Location in Document where we are inserting text
                int length, // Length of text to delete (in case we delete and insert at the same time)
                String text, // Text to insert, null indicates no text to insert
                AttributeSet attrs) throws BadLocationException // the attributes to associate with the inserted content. This may be null if there are no attributes.
        {
            int currentLength = fb.getDocument().getLength();
            int overLimit = (currentLength + text.length()) - SERIAL_NUMBER_MAX_LENGTH - length;
            if(overLimit > 0)
            {
                text = text.substring(0, text.length() - overLimit);
            }

            text = text.replaceAll("[^A-Za-z0-9]*$", "");
            fb.replace(offset, length, text.toUpperCase(), attrs);
        }

        @Override
        public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException
        {
            fb.remove(offset, length);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupRadioDtr;
    private javax.swing.ButtonGroup buttonGroupRadioRts;
    private javax.swing.ButtonGroup buttonGroupTypeOfWork;
    private javax.swing.JCheckBox checkboxESM;
    private javax.swing.JCheckBox checkboxSendLeadingZeroAsT;
    private javax.swing.JCheckBox checkboxSendZeroAsT;
    private javax.swing.JCheckBox checkboxSettingsQuickMode;
    private javax.swing.JInternalFrame intframeBandmap;
    private javax.swing.JInternalFrame intframeEntryWindow;
    private javax.swing.JInternalFrame intframeLog;
    private javax.swing.JInternalFrame intframeMisc;
    private javax.swing.JInternalFrame intframeRadioConnection;
    private javax.swing.JInternalFrame intframeTimeToNextQso;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton19;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton20;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JCheckBox jCheckBoxAutoBandmapStartFreq;
    private javax.swing.JCheckBox jCheckBoxShowFreq;
    private javax.swing.JComboBox jComboBoxKeyerComPort;
    private javax.swing.JComboBox<String> jComboBoxKeyerType;
    private javax.swing.JComboBox<String> jComboBoxPttCommport;
    private javax.swing.JComboBox<String> jComboBoxPttType;
    private javax.swing.JComboBox jComboBoxRadioComPort;
    private javax.swing.JComboBox jComboBoxRadioComPortBaudRate;
    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JDialog jDialogAbout;
    private javax.swing.JDialog jDialogFontChooser;
    private javax.swing.JDialog jDialogSettings;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelStatus;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItemAbout;
    private javax.swing.JMenuItem jMenuItemHelp;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanelCallSign;
    private javax.swing.JPanel jPanelContestRules;
    private javax.swing.JPanel jPanelFunctionKeys;
    private javax.swing.JPanel jPanelKeyerAndPtt;
    private javax.swing.JPanel jPanelOther;
    private javax.swing.JPanel jPanelRadio;
    private javax.swing.JPanel jPanelStatusBar;
    private javax.swing.JRadioButton jRadioButtonRadioDtrOff;
    private javax.swing.JRadioButton jRadioButtonRadioDtrOn;
    private javax.swing.JRadioButton jRadioButtonRadioRtsOff;
    private javax.swing.JRadioButton jRadioButtonRadioRtsOn;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextFieldContestExchange;
    private javax.swing.JTextField jTextFieldF2;
    private javax.swing.JTextField jTextFieldPttDelay;
    private javax.swing.JTextField jTextFieldPttTailDelay;
    private javax.swing.JTextField jTextFieldTimeToNextQso;
    private javax.swing.JButton jbuttonCreateNewLog;
    private javax.swing.JButton jbuttonDeleteEntry;
    private javax.swing.JButton jbuttonJumpToCqFreq;
    private javax.swing.JButton jbuttonKeyerDown;
    private javax.swing.JButton jbuttonKeyerUP;
    private javax.swing.JButton jbuttonOpenExistingLog;
    private javax.swing.JButton jbuttonSetCqFreq;
    private javax.swing.JCheckBox jcheckboxContinuousCq;
    private javax.swing.JCheckBox jcheckboxF1jumpsToCq;
    private javax.swing.JCheckBox jcheckboxRadioPolling;
    private javax.swing.JComboBox jcomboboxBand;
    private javax.swing.JComboBox<String> jcomboboxColumnCount;
    private javax.swing.JComboBox jcomboboxMode;
    private javax.swing.JComboBox<String> jcomboboxRowCount;
    private javax.swing.JComboBox<String> jcomboboxStepInHz;
    private javax.swing.JDialog jdialogLogSelection;
    private javax.swing.JLabel jlabelBandmapFreeSpace;
    private javax.swing.JLabel jlabelCallsignStatus;
    private javax.swing.JLabel jlabelCqFreq;
    private javax.swing.JLabel jlabelKeyerSpeed;
    private javax.swing.JMenuItem jmenuFonts;
    private javax.swing.JMenuItem jmenuGenerateCabrillo;
    private javax.swing.JMenuItem jmenuSettings;
    private javax.swing.JMenu jmenuWindows;
    private javax.swing.JPanel jpanelCallsign;
    private javax.swing.JPanel jpanelCompleteLog;
    private javax.swing.JPanel jpanelCqSettings;
    private javax.swing.JPanel jpanelFunctionKeys;
    private javax.swing.JPanel jpanelKeyerConnection;
    private javax.swing.JPanel jpanelKeyerSettings;
    private javax.swing.JPanel jpanelRadioConnection;
    private javax.swing.JPanel jpanelTypeOfWork;
    private javax.swing.JRadioButton jradiobuttonCQ;
    private javax.swing.JRadioButton jradiobuttonSP;
    private javax.swing.JTable jtableBandmap;
    private javax.swing.JTable jtableIncomingQso;
    private javax.swing.JTable jtableLog;
    private javax.swing.JTextField jtextfieldBandmapStartFreq;
    private javax.swing.JTextField jtextfieldCallsign;
    private javax.swing.JTextField jtextfieldContinuousCqPeriod;
    private javax.swing.JTextField jtextfieldF10;
    private javax.swing.JTextField jtextfieldF8;
    private javax.swing.JTextField jtextfieldF9;
    private javax.swing.JTextField jtextfieldFreqWidth;
    private javax.swing.JTextField jtextfieldFrequency;
    private javax.swing.JTextField jtextfieldMode;
    private javax.swing.JTextField jtextfieldPollingTime;
    private javax.swing.JTextField jtextfieldQsoRepeatPeriod;
    private javax.swing.JTextField jtextfieldRcv;
    private javax.swing.JTextField jtextfieldSnt;
    private javax.swing.JTextField jtextfieldfF1;
    private javax.swing.JTextField jtextfieldfF3;
    private javax.swing.JTextField jtextfieldfF6;
    private javax.swing.JTextField jtextfieldfF7;
    private javax.swing.JToggleButton jtogglebuttonConnectToKeyer;
    private javax.swing.JToggleButton jtogglebuttonConnectToRadio;
    private javax.swing.JTextField textfieldSettingsDefaultPrefix;
    private javax.swing.JTextField textfieldSettingsMyCallsign;
    // End of variables declaration//GEN-END:variables
}
