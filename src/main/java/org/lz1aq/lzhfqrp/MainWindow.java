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
package org.lz1aq.lzhfqrp;

import java.awt.Color;
import java.awt.Component;
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
import org.lz1aq.log.Log;
import org.lz1aq.log.LogDatabase;
import org.lz1aq.log.LogTableModel;
import org.lz1aq.log.Qso;
import org.lz1aq.rsi.Radio;
import org.lz1aq.utils.FontChooser;
import org.lz1aq.utils.Misc;
import org.lz1aq.utils.RadioModes;
import org.lz1aq.utils.TimeUtils;

/**
 *
 * @author potty
 */
public class MainWindow extends javax.swing.JFrame
{
  static final String PROGRAM_VERSION = "1.0.0";
  static final String PROGRAM_NAME    = "LZ-Log";
          
  static final String TYPE_OF_WORK_SP = "SP";
  static final String TYPE_OF_WORK_CQ = "CQ";
  static final int    SERIAL_NUMBER_LENGTH = 6;
  
  private Log                           log;
  private LogTableModel                 jtablemodelLog;
  private TimeToNextQsoTableModel         jtablemodelIncomingQso;
  private BandmapTableModel             jtablemodelBandmap;
  private final ApplicationSettings     applicationSettings;
  private final RadioController         radioController;
  private int                           cqFrequency =3500000;
  private int                           keyerSpeed = 28;
  private final Timer                   timer1sec;
  private final Timer                   timer500ms;
  private Timer                         timerContinuousCq;
  private FontChooser                   fontchooser = new FontChooser();
  private String                        logDbFile;
  private String                        pathToWorkingDir; // where the jar file is located
  private boolean                       isFreqColumnHidden = false;
          
  private DocumentFilter                callsignFilter = new UppercaseDocumentFilter();
  private DocumentFilter                serialNumberFilter = new SerialNumberDocumentFilter();
  
  
  
  private static final Logger logger = Logger.getLogger(Radio.class.getName());
  /**
   * Creates new form MainWindow
   */
  

  public MainWindow()
  { 
    determineWorkingDir();
    
    // Create directory for logs if not existing
    File directory = new File(Paths.get(pathToWorkingDir, "/logs").toString());
    if (! directory.exists())
    {
        directory.mkdir();
    }
    
    // Load user settings from the properties file
    this.applicationSettings = new ApplicationSettings();
    
    // Init GUI
    initComponents();
    
    // Show dialog for opening New/Existing log - result will be kept in logDbFile
    if(((LogSelectionDialog)jdialogLogSelection).showDialog())
    {
      System.exit(0); // Close program if Showdialog tells us to do so
    }
    
    // Open log database
    try
    {
      Qso example = new Qso(14190000, "cw", "lz1abc", "lz0fs", "200 091", "200 091", "cq"); // We need to supply an example QSO whwn creating/opening new
      log = new Log(new LogDatabase(logDbFile), example);
    }
    catch (Exception ex)
    {
      logger.log(Level.SEVERE, "Couldn't open the log database!", ex);
    }
    
    // Init TableModels
    jtablemodelLog = new LogTableModel(log);
    jtablemodelLog.setInvisible(4); // Hide myCall
    jtableLog.setModel(jtablemodelLog);
    
    jtablemodelIncomingQso = new TimeToNextQsoTableModel(log);
    jtableIncomingQso.setModel(jtablemodelIncomingQso);
    
    jtablemodelBandmap = new BandmapTableModel(log, 3500000, applicationSettings);
    jtableBandmap.setModel(jtablemodelBandmap);
    
    
    // renderer for the bandmap
    jtableBandmap.setDefaultRenderer(Object.class, new BandmapTableCellRender());
    jtableIncomingQso.setDefaultRenderer(Object.class, new IncomingQsoTableCellRender());
    
    // For communicating with the radio
    radioController = new RadioController();
    
    //This is used for catching global key presses (i.e. needed for F1-F12 presses)
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new MyDispatcher());
    
    // Prepare the entry fields to have the necessary data
    initEntryFields();

    // Callsign text field should show capital letters only
    ((AbstractDocument) jtextfieldCallsign.getDocument()).setDocumentFilter(callsignFilter);
    // Serial number should be 6 digits long
    ((AbstractDocument) jtextfieldSnt.getDocument()).setDocumentFilter(serialNumberFilter);
    ((AbstractDocument) jtextfieldRcv.getDocument()).setDocumentFilter(serialNumberFilter);
    
    
    // Needed so that jTable to scroll automatically upon entering a new Qso
    jtableLog.addComponentListener(new ComponentAdapter()
    {
      @Override
      public void componentResized(ComponentEvent e)
      {
        jtableLog.scrollRectToVisible(jtableLog.getCellRect(jtableLog.getRowCount() - 1, 0, true));
      }
    });
    
    
    // Hihglighting for active text fields 
    jtextfieldCallsign.addFocusListener(highlighter);
    jtextfieldSnt.addFocusListener(highlighter);
    jtextfieldRcv.addFocusListener(highlighter);

    // Start a 1sec timer
    timer1sec = new Timer(1000, timer1secListener);
    timer1sec.setRepeats(true);
    timer1sec.start();
    
    // Start a 500ms second timer
    timer500ms = new Timer(300, timer500msListener);
    timer500ms.setRepeats(true);
    timer500ms.start();
  }

  
  public void resizeFreqColumnWidth(JTable table, int size)
  {
    final TableColumnModel columnModel = table.getColumnModel();

    for (int column = 0; column < table.getColumnCount(); column++)
    {
      //int width = 15; // Min width
//      for (int row = 0; row < table.getRowCount(); row++)
//      {
//        TableCellRenderer renderer = table.getCellRenderer(row, column);
//        Component comp = table.prepareRenderer(renderer, row, column);
//        width = Math.max(comp.getPreferredSize().width + 1, width);
//      }
      
      // If frequency column
      if(column%2==0)
      {
        columnModel.getColumn(column).setPreferredWidth(size);
      }
    }
  }
  
  private DefaultComboBoxModel getBandsComboboxModel()
  {
    return new DefaultComboBoxModel(new String[] { "1.8", "3.5", "7", "14", "21", "28" });
  }
  
  
  private DefaultComboBoxModel getModeComboboxModel()
  {
    return new DefaultComboBoxModel(new String[] { "SSB", "CW" });
  }
  
  private DefaultComboBoxModel getBandmapStepInHzComboboxModel()
  {
    return new DefaultComboBoxModel(new String[] { "100", "200", "500"});
  }
  
  
  private DefaultComboBoxModel getBandmapColumnCountComboboxModel()
  {
    return new DefaultComboBoxModel(new String[] {"8", "10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "32", "34", "36", "38", "40", "42", "44", "46", "48", "50", "52", "54", "56", "58", "60", "62", "64", "66", "68", "70", "72", "74", "76", "78", "80", "82", "84", "86", "88"});
  }
  
  private DefaultComboBoxModel getBandmapRowCountComboboxModel()
  {
    return new DefaultComboBoxModel(new String[] {"8", "10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "32", "34", "36", "38", "40", "42", "44", "46", "48", "50", "52", "54", "56", "58", "60", "62", "64", "66", "68", "70", "72", "74", "76", "78", "80", "82", "84", "86", "88"});
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
  
  
  /**
   * this is called every second
   */
  private final ActionListener timer1secListener = new ActionListener()
  {
    @Override
    public void actionPerformed(ActionEvent evt)
    {
      jtablemodelIncomingQso.refresh(applicationSettings.getQsoRepeatPeriod(), // How often we can repeat qso
              applicationSettings.getIncomingQsoHiderAfter()); // Hide qso after certain overtime

      jtablemodelBandmap.refresh(applicationSettings);
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
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    java.awt.GridBagConstraints gridBagConstraints;

    buttonGroupTypeOfWork = new javax.swing.ButtonGroup();
    jDialogSettings = new javax.swing.JDialog();
    jScrollPane4 = new javax.swing.JScrollPane();
    jPanel1 = new javax.swing.JPanel();
    jPanel4 = new javax.swing.JPanel();
    jComboBoxComPort = new javax.swing.JComboBox();
    jLabel12 = new javax.swing.JLabel();
    jPanel3 = new javax.swing.JPanel();
    textfieldSettingsMyCallsign = new javax.swing.JTextField();
    jPanel7 = new javax.swing.JPanel();
    jLabel3 = new javax.swing.JLabel();
    jtextfieldf1 = new javax.swing.JTextField();
    jLabel9 = new javax.swing.JLabel();
    jtextfieldf3 = new javax.swing.JTextField();
    jLabel4 = new javax.swing.JLabel();
    jtextfieldf6 = new javax.swing.JTextField();
    jLabel5 = new javax.swing.JLabel();
    jtextfieldf7 = new javax.swing.JTextField();
    jLabel8 = new javax.swing.JLabel();
    jtextfieldf8 = new javax.swing.JTextField();
    jLabel6 = new javax.swing.JLabel();
    jtextfieldf9 = new javax.swing.JTextField();
    jLabel7 = new javax.swing.JLabel();
    jtextfieldf10 = new javax.swing.JTextField();
    jPanel6 = new javax.swing.JPanel();
    checkboxSettingsQuickMode = new javax.swing.JCheckBox();
    textfieldSettingsDefaultPrefix = new javax.swing.JTextField();
    checkboxSendLeadingZeroAsT = new javax.swing.JCheckBox();
    checkboxF1JumpsToCq = new javax.swing.JCheckBox();
    checkboxESM = new javax.swing.JCheckBox();
    jPanel2 = new javax.swing.JPanel();
    jtextfieldQsoRepeatPeriod = new javax.swing.JTextField();
    jLabel2 = new javax.swing.JLabel();
    jPanel9 = new javax.swing.JPanel();
    jLabel10 = new javax.swing.JLabel();
    jTextField1 = new javax.swing.JTextField();
    jPanel5 = new javax.swing.JPanel();
    jButtonCancel = new javax.swing.JButton();
    jButtonSave = new javax.swing.JButton();
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
    jDesktopPane1 = new javax.swing.JDesktopPane();
    intframeTimeToNextQso = new javax.swing.JInternalFrame();
    jScrollPane2 = new javax.swing.JScrollPane();
    jtableIncomingQso = new javax.swing.JTable();
    intframeBandmap = new javax.swing.JInternalFrame();
    jScrollPane5 = new javax.swing.JScrollPane();
    jtableBandmap = new javax.swing.JTable();
    jPanel8 = new javax.swing.JPanel();
    jcomboboxStepInHz = new javax.swing.JComboBox<String>();
    jcomboboxColumnCount = new javax.swing.JComboBox<String>();
    jcomboboxRowCount = new javax.swing.JComboBox<String>();
    jlabelBandmapFreeSpace = new javax.swing.JLabel();
    jLabel13 = new javax.swing.JLabel();
    jLabel14 = new javax.swing.JLabel();
    jLabel15 = new javax.swing.JLabel();
    jtextfieldFreqWidth = new javax.swing.JTextField();
    jLabel11 = new javax.swing.JLabel();
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

    jDialogSettings.setTitle("Settings");
    jDialogSettings.setAlwaysOnTop(true);
    jDialogSettings.setModal(true);
    jDialogSettings.setPreferredSize(new java.awt.Dimension(350, 500));
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

    jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("CommPort settings"));
    jPanel4.setLayout(new java.awt.GridBagLayout());

    jComboBoxComPort.setModel(getComportsComboboxModel());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
    jPanel4.add(jComboBoxComPort, gridBagConstraints);

    jLabel12.setText("CommPort");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    jPanel4.add(jLabel12, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    jPanel1.add(jPanel4, gridBagConstraints);

    jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("My callsign"));
    jPanel3.setLayout(new java.awt.GridBagLayout());

    textfieldSettingsMyCallsign.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
    textfieldSettingsMyCallsign.setText("Your callsign here");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 20);
    jPanel3.add(textfieldSettingsMyCallsign, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    jPanel1.add(jPanel3, gridBagConstraints);

    jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Function keys"));
    jPanel7.setLayout(new java.awt.GridLayout(0, 2));

    jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    jLabel3.setText("F1 Cq");
    jPanel7.add(jLabel3);

    jtextfieldf1.setText("jTextField1");
    jPanel7.add(jtextfieldf1);

    jLabel9.setText("F3 Tu");
    jPanel7.add(jLabel9);

    jtextfieldf3.setText("jTextField2");
    jPanel7.add(jtextfieldf3);

    jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    jLabel4.setText("F6");
    jPanel7.add(jLabel4);

    jtextfieldf6.setText("jTextField3");
    jPanel7.add(jtextfieldf6);

    jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    jLabel5.setText("F7");
    jPanel7.add(jLabel5);

    jtextfieldf7.setText("jTextField4");
    jPanel7.add(jtextfieldf7);

    jLabel8.setText("F8");
    jPanel7.add(jLabel8);

    jtextfieldf8.setText("jTextField5");
    jPanel7.add(jtextfieldf8);

    jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    jLabel6.setText("F9");
    jPanel7.add(jLabel6);

    jtextfieldf9.setText("jTextField6");
    jPanel7.add(jtextfieldf9);

    jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    jLabel7.setText("F10 ");
    jPanel7.add(jLabel7);

    jtextfieldf10.setEditable(false);
    jtextfieldf10.setText("jTextField7");
    jtextfieldf10.setEnabled(false);
    jPanel7.add(jtextfieldf10);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    jPanel1.add(jPanel7, gridBagConstraints);

    jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Misc"));
    jPanel6.setLayout(new java.awt.GridBagLayout());

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
    jPanel6.add(checkboxSettingsQuickMode, gridBagConstraints);

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
    jPanel6.add(textfieldSettingsDefaultPrefix, gridBagConstraints);

    checkboxSendLeadingZeroAsT.setText("Send leading zeros as 'T'");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanel6.add(checkboxSendLeadingZeroAsT, gridBagConstraints);

    checkboxF1JumpsToCq.setText("F1 jumps to last CQ freq");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanel6.add(checkboxF1JumpsToCq, gridBagConstraints);

    checkboxESM.setText("\"Enter\" sends message");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanel6.add(checkboxESM, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.weighty = 0.1;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    jPanel1.add(jPanel6, gridBagConstraints);

    jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Contest rules"));
    jPanel2.setToolTipText("");
    jPanel2.setLayout(new java.awt.GridBagLayout());

    jtextfieldQsoRepeatPeriod.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
    jtextfieldQsoRepeatPeriod.setText("1800");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.3;
    gridBagConstraints.weighty = 1.0;
    jPanel2.add(jtextfieldQsoRepeatPeriod, gridBagConstraints);

    jLabel2.setText("QSO repeat period in seconds:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanel2.add(jLabel2, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.weighty = 0.1;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    jPanel1.add(jPanel2, gridBagConstraints);

    jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder("Time to next Qso"));
    jPanel9.setLayout(new java.awt.GridBagLayout());

    jLabel10.setText("Do not show after [sec]");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanel9.add(jLabel10, gridBagConstraints);

    jTextField1.setText("jTextField1");
    jTextField1.setToolTipText("This should be a negative value!");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanel9.add(jTextField1, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    jPanel1.add(jPanel9, gridBagConstraints);

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
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    jPanel1.add(jPanel5, gridBagConstraints);

    jScrollPane4.setViewportView(jPanel1);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jDialogSettings.getContentPane().add(jScrollPane4, gridBagConstraints);

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

    jButton16.setText("IncomingQso");
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

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    setTitle(PROGRAM_NAME+" by LZ1ABC");
    addWindowListener(new java.awt.event.WindowAdapter()
    {
      public void windowClosing(java.awt.event.WindowEvent evt)
      {
        formWindowClosing(evt);
      }
      public void windowOpened(java.awt.event.WindowEvent evt)
      {
        formWindowOpened(evt);
      }
    });

    jDesktopPane1.setMinimumSize(new java.awt.Dimension(600, 400));

    intframeTimeToNextQso.setIconifiable(true);
    intframeTimeToNextQso.setMaximizable(true);
    intframeTimeToNextQso.setResizable(true);
    intframeTimeToNextQso.setTitle("Time to next Qso");
    intframeTimeToNextQso.setVisible(true);

    jtableIncomingQso.setFont(new java.awt.Font("Liberation Mono", 0, 18)); // NOI18N
    jtableIncomingQso.setRowHeight(30);
    jtableIncomingQso.addMouseListener(new java.awt.event.MouseAdapter()
    {
      public void mouseClicked(java.awt.event.MouseEvent evt)
      {
        jtableIncomingQsoMouseClicked(evt);
      }
    });
    jScrollPane2.setViewportView(jtableIncomingQso);

    intframeTimeToNextQso.getContentPane().add(jScrollPane2, java.awt.BorderLayout.CENTER);

    jDesktopPane1.add(intframeTimeToNextQso);
    intframeTimeToNextQso.setBounds(490, 10, 463, 435);

    intframeBandmap.setIconifiable(true);
    intframeBandmap.setMaximizable(true);
    intframeBandmap.setResizable(true);
    intframeBandmap.setTitle("Bandmap");
    intframeBandmap.setVisible(true);
    intframeBandmap.getContentPane().setLayout(new java.awt.GridBagLayout());

    jtableBandmap.setCellSelectionEnabled(true);
    jtableBandmap.addMouseListener(new java.awt.event.MouseAdapter()
    {
      public void mouseClicked(java.awt.event.MouseEvent evt)
      {
        jtableBandmapMouseClicked(evt);
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
    jcomboboxStepInHz.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jcomboboxStepInHzItemStateChanged(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.01;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
    jPanel8.add(jcomboboxStepInHz, gridBagConstraints);

    jcomboboxColumnCount.setModel(getBandmapColumnCountComboboxModel());
    jcomboboxColumnCount.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jcomboboxColumnCountItemStateChanged(evt);
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
    jPanel8.add(jcomboboxColumnCount, gridBagConstraints);

    jcomboboxRowCount.setModel(getBandmapRowCountComboboxModel());
    jcomboboxRowCount.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jcomboboxRowCountItemStateChanged(evt);
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
    jPanel8.add(jcomboboxRowCount, gridBagConstraints);

    jlabelBandmapFreeSpace.setText(" ");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanel8.add(jlabelBandmapFreeSpace, gridBagConstraints);

    jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel13.setText("Step in Hz:");
    jLabel13.setToolTipText("Step in Hz");
    jLabel13.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.01;
    gridBagConstraints.weighty = 1.0;
    jPanel8.add(jLabel13, gridBagConstraints);

    jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel14.setText("Rows:");
    jLabel14.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.01;
    gridBagConstraints.weighty = 1.0;
    jPanel8.add(jLabel14, gridBagConstraints);

    jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel15.setText("Columns:");
    jLabel15.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.01;
    gridBagConstraints.weighty = 1.0;
    jPanel8.add(jLabel15, gridBagConstraints);

    jtextfieldFreqWidth.setText("20");
    jtextfieldFreqWidth.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jtextfieldFreqWidthActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.01;
    gridBagConstraints.weighty = 1.0;
    jPanel8.add(jtextfieldFreqWidth, gridBagConstraints);

    jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
    jLabel11.setText("Width of freq columns");
    jLabel11.setToolTipText("");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.01;
    gridBagConstraints.weighty = 1.0;
    jPanel8.add(jLabel11, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 0.001;
    intframeBandmap.getContentPane().add(jPanel8, gridBagConstraints);

    jDesktopPane1.add(intframeBandmap);
    intframeBandmap.setBounds(500, 520, 500, 459);

    intframeLog.setIconifiable(true);
    intframeLog.setMaximizable(true);
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
    intframeRadioConnection.setMaximizable(true);
    intframeRadioConnection.setResizable(true);
    intframeRadioConnection.setTitle("Radio connection");
    intframeRadioConnection.setToolTipText("");
    intframeRadioConnection.setVisible(true);

    jpanelRadioConnection.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
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
    gridBagConstraints.insets = new java.awt.Insets(5, 20, 5, 20);
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

    intframeRadioConnection.getContentPane().add(jpanelRadioConnection, java.awt.BorderLayout.CENTER);

    jDesktopPane1.add(intframeRadioConnection);
    intframeRadioConnection.setBounds(30, 20, 277, 69);

    intframeEntryWindow.setIconifiable(true);
    intframeEntryWindow.setMaximizable(true);
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
      public void keyTyped(java.awt.event.KeyEvent evt)
      {
        jtextfieldCallsignKeyTyped(evt);
      }
      public void keyReleased(java.awt.event.KeyEvent evt)
      {
        jtextfieldCallsignKeyReleased(evt);
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
    jtextfieldRcv.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jtextfieldRcvActionPerformed(evt);
      }
    });
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

    jButton4.setText("F4 "+applicationSettings.getMyCallsign());
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

    jDesktopPane1.add(intframeEntryWindow);
    intframeEntryWindow.setBounds(280, 20, 453, 227);

    intframeMisc.setIconifiable(true);
    intframeMisc.setMaximizable(true);
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

    jbuttonSetCqFreq.setText("Set CQ freq");
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

    jMenuItem4.setText("Radio connection");
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

    setJMenuBar(jMenuBar1);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonCancelActionPerformed
  {//GEN-HEADEREND:event_jButtonCancelActionPerformed
    jDialogSettings.setVisible(false);
  }//GEN-LAST:event_jButtonCancelActionPerformed

  private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonSaveActionPerformed
  {//GEN-HEADEREND:event_jButtonSaveActionPerformed
    jDialogSettings.setVisible(false); // Hide the SettingsDialog
    storeSettingsDialogParams();       // Read the state of the controls and save them
    
    initMainWindow(false);
  }//GEN-LAST:event_jButtonSaveActionPerformed

  private void jDialogSettingsComponentShown(java.awt.event.ComponentEvent evt)//GEN-FIRST:event_jDialogSettingsComponentShown
  {//GEN-HEADEREND:event_jDialogSettingsComponentShown
    // Settings dialog is shown and we need to set the states of the controls
    initSettingsDialog();
  }//GEN-LAST:event_jDialogSettingsComponentShown

  private void formWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowOpened
  {//GEN-HEADEREND:event_formWindowOpened
    initMainWindow(true);
  }//GEN-LAST:event_formWindowOpened

  private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
  {//GEN-HEADEREND:event_formWindowClosing
    // Read the dimensions of the different frames
    applicationSettings.setFrameDimensions(ApplicationSettings.FrameIndex.ENTRY, intframeEntryWindow.getBounds());
    applicationSettings.setFrameDimensions(ApplicationSettings.FrameIndex.BANDMAP, intframeBandmap.getBounds());
    applicationSettings.setFrameDimensions(ApplicationSettings.FrameIndex.INCOMING_QSO, intframeTimeToNextQso.getBounds());
    applicationSettings.setFrameDimensions(ApplicationSettings.FrameIndex.JFRAME, this.getBounds());
    applicationSettings.setFrameDimensions(ApplicationSettings.FrameIndex.LOG, intframeLog.getBounds());
    applicationSettings.setFrameDimensions(ApplicationSettings.FrameIndex.RADIO, intframeRadioConnection.getBounds());
    applicationSettings.setFrameDimensions(ApplicationSettings.FrameIndex.SETTINGS, intframeMisc.getBounds());
            
    // Store the fonts being in use
    applicationSettings.setFont(ApplicationSettings.FontIndex.BANDMAP, jtableBandmap.getFont());
    applicationSettings.setFont(ApplicationSettings.FontIndex.CALLSIGN, jtextfieldCallsign.getFont());
    applicationSettings.setFont(ApplicationSettings.FontIndex.INCOMING_QSO, jtableIncomingQso.getFont());
    applicationSettings.setFont(ApplicationSettings.FontIndex.LOG, jtableLog.getFont());
    applicationSettings.setFont(ApplicationSettings.FontIndex.RCV, jtextfieldRcv.getFont());
    applicationSettings.setFont(ApplicationSettings.FontIndex.SNT, jtextfieldSnt.getFont());
    
    applicationSettings.SaveSettingsToDisk(); // Save all settings to disk
  }//GEN-LAST:event_formWindowClosing

  private void jmenuSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmenuSettingsActionPerformed
  {//GEN-HEADEREND:event_jmenuSettingsActionPerformed
    jDialogSettings.pack();
    jDialogSettings.setVisible(true);
  }//GEN-LAST:event_jmenuSettingsActionPerformed

  private void checkboxSettingsQuickModeStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_checkboxSettingsQuickModeStateChanged
  {//GEN-HEADEREND:event_checkboxSettingsQuickModeStateChanged
    if(checkboxSettingsQuickMode.isSelected())
      textfieldSettingsDefaultPrefix.setEnabled(true);
    else
      textfieldSettingsDefaultPrefix.setEnabled(false);
  }//GEN-LAST:event_checkboxSettingsQuickModeStateChanged

  private void jButton12ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton12ActionPerformed
  {//GEN-HEADEREND:event_jButton12ActionPerformed
    pressedF12();
  }//GEN-LAST:event_jButton12ActionPerformed

  private void jButton11ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton11ActionPerformed
  {//GEN-HEADEREND:event_jButton11ActionPerformed
    pressedF11();
  }//GEN-LAST:event_jButton11ActionPerformed

  private void jtextfieldRcvActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jtextfieldRcvActionPerformed
  {//GEN-HEADEREND:event_jtextfieldRcvActionPerformed
    // Log Qso
    if(addEntryToLog())
    {
      initEntryFields();
      // Move focus to Callsign field
      jtextfieldCallsign.requestFocus();
    }
  }//GEN-LAST:event_jtextfieldRcvActionPerformed

  private void jtextfieldCallsignKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jtextfieldCallsignKeyReleased
  {//GEN-HEADEREND:event_jtextfieldCallsignKeyReleased
    // On every key press update the callsign status
    String status = getCallsignStatusText(getCallsignFromTextField());
    jlabelCallsignStatus.setText(status);
  }//GEN-LAST:event_jtextfieldCallsignKeyReleased

  private void jtextfieldCallsignKeyTyped(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jtextfieldCallsignKeyTyped
  {//GEN-HEADEREND:event_jtextfieldCallsignKeyTyped
    switch(evt.getKeyChar())
    {
      case KeyEvent.VK_SPACE: // Move to Rcv field    
        jtextfieldRcv.requestFocus();
        evt.consume();
        break;

      case KeyEvent.VK_ENTER: // Move to Rcv field      
        if(applicationSettings.isEms())
        {
          if(sendEnterSendsMessage())
          {
             jtextfieldRcv.requestFocus();
          }
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
    if (response == JOptionPane.NO_OPTION || response == JOptionPane.CLOSED_OPTION)
    {
      return; // do nothing
    }

    // Get the selected row
    int selection = jtableLog.getSelectedRow();
    if(selection >= 0)
    {
      selection = jtableLog.convertRowIndexToModel(selection);
      jtablemodelLog.removeRow(selection);
      initEntryFields(); // We need to update the Snt field in case we deleted the last contact
    }
    else
    {
      JOptionPane.showMessageDialog(null, "Pease select entry!");
    }
  }//GEN-LAST:event_jbuttonDeleteEntryActionPerformed

  private void jtogglebuttonConnectToRadioActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jtogglebuttonConnectToRadioActionPerformed
  {//GEN-HEADEREND:event_jtogglebuttonConnectToRadioActionPerformed
    JToggleButton tBtn = (JToggleButton) evt.getSource();
    
    // Connect
    // --------------------
    if (tBtn.isSelected())
    {
      // Select the python file describing the radio protocol
      if(loadRadioProtocolParser())
      {
        connectToRadio(); // now we can try to connect
      } 
    }
    // Disconnect
    // --------------------
    else
    {
      if (radioController != null)
      {
        radioController.disconnect();
      }
    }
    
    
    if(radioController.isConnected())
    {
      jtogglebuttonConnectToRadio.setSelected(true);
      jcomboboxBand.setEnabled(false);
      jcomboboxMode.setEnabled(false);
    }
    else
    {
      jtogglebuttonConnectToRadio.setSelected(false);
      jcomboboxBand.setEnabled(true);
      jcomboboxMode.setEnabled(true);
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

  private void jtableIncomingQsoMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jtableIncomingQsoMouseClicked
  {//GEN-HEADEREND:event_jtableIncomingQsoMouseClicked
    //if (evt.getClickCount() == 2)
    //{
      JTable target = (JTable) evt.getSource();
      int row = target.getSelectedRow();
      String callsign;
      
      try
      {
        
        // Jump to freq
        radioController.setFrequency(jtablemodelIncomingQso.getFrequency(row)); // jump to freq
        
        // Add the callsign into the Entry field
        initEntryFields();  // clear the fields
        if(applicationSettings.isQuickCallsignModeEnabled()) // If quick mode is enabled add only the suffix
        {
          callsign = Misc.toShortCallsign(jtablemodelIncomingQso.getCallsign(row), applicationSettings.getDefaultPrefix());
        }
        else
        {
          callsign = jtablemodelIncomingQso.getCallsign(row);
        }
  
        jtextfieldCallsign.setText(callsign);// set the callsign inside the callsign field
      
      }
      catch (Exception ex)
      {
        Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
      }
    //}
    
    // Return focus to callsign field
    jtextfieldCallsign.requestFocus();
  }//GEN-LAST:event_jtableIncomingQsoMouseClicked

  private void jtableBandmapMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jtableBandmapMouseClicked
  {//GEN-HEADEREND:event_jtableBandmapMouseClicked
    //if (evt.getClickCount() == 2)
    //{
      JTable target = (JTable) evt.getSource();
      int row = target.getSelectedRow();
      int col = target.getSelectedColumn();
      
      if(row>-1 && col>-1)
      { 
        try
        {
          int freq = jtablemodelBandmap.cellToFreq(row, col);
          radioController.setFrequency(freq);
          initEntryFields();
        }
        catch (Exception ex)
        {
          Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    //}

    // Return focus to callsign field
    jtextfieldCallsign.requestFocus();
  }//GEN-LAST:event_jtableBandmapMouseClicked

  private void jcomboboxColumnCountItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jcomboboxColumnCountItemStateChanged
  {//GEN-HEADEREND:event_jcomboboxColumnCountItemStateChanged
     if (evt.getStateChange() == ItemEvent.SELECTED) 
     {
       applicationSettings.setBandmapColumnCount(Integer.parseInt((String)jcomboboxColumnCount.getSelectedItem()));
       jtablemodelBandmap.fireTableStructureChanged();
     }
  }//GEN-LAST:event_jcomboboxColumnCountItemStateChanged

  private void jcomboboxRowCountItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jcomboboxRowCountItemStateChanged
  {//GEN-HEADEREND:event_jcomboboxRowCountItemStateChanged
     if (evt.getStateChange() == ItemEvent.SELECTED) 
     {
       applicationSettings.setBandmapRowCount(Integer.parseInt((String)jcomboboxRowCount.getSelectedItem()));
       jtablemodelBandmap.fireTableStructureChanged();
     }
  }//GEN-LAST:event_jcomboboxRowCountItemStateChanged

  private void jcomboboxStepInHzItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jcomboboxStepInHzItemStateChanged
  {//GEN-HEADEREND:event_jcomboboxStepInHzItemStateChanged
     if (evt.getStateChange() == ItemEvent.SELECTED) 
     {
       applicationSettings.setBandmapStepInHz(Integer.parseInt((String)jcomboboxStepInHz.getSelectedItem()));
     }
  }//GEN-LAST:event_jcomboboxStepInHzItemStateChanged

  private void jmenuFontsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmenuFontsActionPerformed
  {//GEN-HEADEREND:event_jmenuFontsActionPerformed
    jDialogFontChooser.setVisible(true);
  }//GEN-LAST:event_jmenuFontsActionPerformed

  private void jButton13ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton13ActionPerformed
  {//GEN-HEADEREND:event_jButton13ActionPerformed
    fontchooser.setSelectedFont(jtextfieldCallsign.getFont());
    if(fontchooser.showDialog(jtextfieldCallsign)==FontChooser.OK_OPTION)
      jtextfieldCallsign.setFont(fontchooser.getSelectedFont());
  }//GEN-LAST:event_jButton13ActionPerformed

  private void jButton19ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton19ActionPerformed
  {//GEN-HEADEREND:event_jButton19ActionPerformed
    jDialogFontChooser.setVisible(false);
  }//GEN-LAST:event_jButton19ActionPerformed

  private void jButton14ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton14ActionPerformed
  {//GEN-HEADEREND:event_jButton14ActionPerformed
    fontchooser.setSelectedFont(jtextfieldSnt.getFont());
    if(fontchooser.showDialog(jtextfieldSnt)==FontChooser.OK_OPTION)
      jtextfieldSnt.setFont(fontchooser.getSelectedFont());
  }//GEN-LAST:event_jButton14ActionPerformed

  private void jButton15ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton15ActionPerformed
  {//GEN-HEADEREND:event_jButton15ActionPerformed
    fontchooser.setSelectedFont(jtextfieldRcv.getFont());
    if(fontchooser.showDialog(jtextfieldRcv)==FontChooser.OK_OPTION)
      jtextfieldRcv.setFont(fontchooser.getSelectedFont());
  }//GEN-LAST:event_jButton15ActionPerformed

  private void jButton16ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton16ActionPerformed
  {//GEN-HEADEREND:event_jButton16ActionPerformed
    fontchooser.setSelectedFont(jtableIncomingQso.getFont());
    if(fontchooser.showDialog(jtableIncomingQso)==FontChooser.OK_OPTION)
      jtableIncomingQso.setFont(fontchooser.getSelectedFont());
  }//GEN-LAST:event_jButton16ActionPerformed

  private void jButton17ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton17ActionPerformed
  {//GEN-HEADEREND:event_jButton17ActionPerformed
    fontchooser.setSelectedFont(jtableLog.getFont());
    if(fontchooser.showDialog(jtableLog)==FontChooser.OK_OPTION)
      jtableLog.setFont(fontchooser.getSelectedFont());
  }//GEN-LAST:event_jButton17ActionPerformed

  private void jButton18ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton18ActionPerformed
  {//GEN-HEADEREND:event_jButton18ActionPerformed
    fontchooser.setSelectedFont(jtableBandmap.getFont());
    if(fontchooser.showDialog(jtableBandmap)==FontChooser.OK_OPTION)
      jtableBandmap.setFont(fontchooser.getSelectedFont());
  }//GEN-LAST:event_jButton18ActionPerformed

  private void jtextfieldRcvKeyTyped(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jtextfieldRcvKeyTyped
  {//GEN-HEADEREND:event_jtextfieldRcvKeyTyped
    switch(evt.getKeyChar())
    {
      case KeyEvent.VK_SPACE: // Move to Rcv field    
        jtextfieldCallsign.requestFocus();
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
    if (jcheckboxF1jumpsToCq.isSelected())
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
      jdialogLogSelection.dispose();
  }//GEN-LAST:event_jbuttonCreateNewLogActionPerformed

  private void jdialogLogSelectionWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_jdialogLogSelectionWindowClosing
  {//GEN-HEADEREND:event_jdialogLogSelectionWindowClosing
    ((LogSelectionDialog)jdialogLogSelection).isProgramTerminated = true;
  }//GEN-LAST:event_jdialogLogSelectionWindowClosing

  private void jbuttonOpenExistingLogActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jbuttonOpenExistingLogActionPerformed
  {//GEN-HEADEREND:event_jbuttonOpenExistingLogActionPerformed
    if(findExistingLog())
      jdialogLogSelection.dispose();
  }//GEN-LAST:event_jbuttonOpenExistingLogActionPerformed

  private void jmenuGenerateCabrilloActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmenuGenerateCabrilloActionPerformed
  {//GEN-HEADEREND:event_jmenuGenerateCabrilloActionPerformed
    JFileChooser fc = new JFileChooser();
    fc.setFileFilter(new FileNameExtensionFilter("Cabrillo files (*.log)", "log"));
    fc.setCurrentDirectory(Paths.get(pathToWorkingDir, "/logs/").toFile());
    try
    {
      int returnVal = fc.showSaveDialog(this.getParent());
      if (returnVal != JFileChooser.APPROVE_OPTION)
      {
        return;
      } 
    }
    catch (Exception exc)
    {
      JOptionPane.showMessageDialog(null, "Error when trying to acquire cabrillo file.", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    
    String absPath = fc.getSelectedFile().getAbsolutePath();
    if(!absPath.endsWith(".log"))
    {
      absPath = absPath+".log";
    }
        
    File file = new File(absPath);
    
    if(file.exists())
    {
      JOptionPane.showMessageDialog(null, "File already exists: "+file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    
    // Write the cabrillo file
    try (PrintWriter printWriter = new PrintWriter(absPath))
    {
      printWriter.println("START-OF-LOG: 2.0");
      printWriter.println("CALLSIGN: " + applicationSettings.getMyCallsign());
      printWriter.println("CONTEST: ");
      printWriter.println("CATEGORY: ");
      printWriter.println("CLAIMED-SCORE: ");
      printWriter.println("OPERATORS: ");
      printWriter.println("NAME: ");
      printWriter.println("ADDRESS: ");
      printWriter.println("ADDRESS: ");
      printWriter.println("CREATED-BY: " + PROGRAM_NAME + " " + PROGRAM_VERSION);
      for (int i = 0; i < log.getQsoCount(); i++)
      {
        printWriter.println(log.get(i).toStringCabrillo());
      }
      printWriter.println("END-OF-LOG:");
    }
    catch (FileNotFoundException ex)
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
    jpanelRadioConnection.requestFocus();
  }//GEN-LAST:event_jMenuItem4ActionPerformed

  private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem5ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem5ActionPerformed
    intframeMisc.toFront();
  }//GEN-LAST:event_jMenuItem5ActionPerformed

  private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem6ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem6ActionPerformed
    intframeLog.toFront();
  }//GEN-LAST:event_jMenuItem6ActionPerformed
  
  private void changeBandmapTableModelStructure()
  {
    jtablemodelBandmap.fireTableStructureChanged();
    jtablemodelBandmap.fireTableStructureChanged(); 
  }
  
  
  private void increaseKeyerSpeed()
  {        
   if(keyerSpeed>45)
      return;
    
    keyerSpeed++;
    radioController.setKeyerSpeed(keyerSpeed);
    jlabelKeyerSpeed.setText(Integer.toString(keyerSpeed)+" WPM");
  }
  
  private void decreaseKeyerSpeed()
  {        
    if(keyerSpeed<10)
      return;
    
    keyerSpeed--;
    radioController.setKeyerSpeed(keyerSpeed);
    jlabelKeyerSpeed.setText(Integer.toString(keyerSpeed)+" WPM");
  }
   
  /**
   * Sends CW message when we press the enter button inside the callsign textfield
   * 
   * @return True if focus should move to Snt field
   */
  private boolean sendEnterSendsMessage()
  {
    // CQ mode
    if(getTypeOfWork().equalsIgnoreCase(TYPE_OF_WORK_CQ))
    {
      if(jtextfieldCallsign.getText().isEmpty())
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
      
    }
    // S&P mode
    else
    {
      pressedF4(); // Send my callsign
      return true; // do not move focis to Snt field
    }
    
  }
  
  
  private boolean connectToRadio()
  {
    boolean result = radioController.connect(applicationSettings.getComPort(), new LocalRadioControllerListener());
    if (!result)
    {
      JOptionPane.showMessageDialog(null, "Could not connect to radio!", "Serial connection error...", JOptionPane.ERROR_MESSAGE);
    }
    
    return result;
  }
  
  
  /**
   * Finds the current working dir and saves into the variable: pathToWorkingDir
   */
  private void determineWorkingDir()
  {
    File file = new File(".");
		String currentDirectory = file.getAbsolutePath();
    currentDirectory = currentDirectory.substring(0, currentDirectory.length()-1);
    pathToWorkingDir = Paths.get(currentDirectory).toString();
    System.out.println("Current working directory is: " + pathToWorkingDir); 
  }
    
    
  /**
   * Opens a file chooser which lets the user select the appropriate radio protocol parser.
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
      chooser.setCurrentDirectory(Paths.get(pathToWorkingDir,"/pyrig").toFile());

      int returnVal = chooser.showOpenDialog(this.getParent());
      if (returnVal != JFileChooser.APPROVE_OPTION)
        return false;
    }catch(Exception exc)
    {
        logger.log(Level.SEVERE, "Coudln't start file chooser", exc);
        return false;
    }
   
    boolean result = radioController.loadProtocolParser(chooser.getSelectedFile().getName());
    if (result == false)
    {
      JOptionPane.showMessageDialog(null, "Error when trying to load the radio protocol parser file!", "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
     
    // Show the serial settings that we are going to use when connecting to this radio
    JOptionPane.showMessageDialog(null, radioController.getInfo());
    return true;
  }
  
  
  /**
   * Reads the info from the entry window and if all data is valid it saves it into the Log
   *
   * @return true - if the QSO was successfully logged
   *         false - 
   */
  boolean addEntryToLog()
  {   
    // Do some validation of the data
    // ------------------------------
    if(jtextfieldCallsign.getText().isEmpty() || !Qso.isValidCallsign(getCallsignFromTextField()))
    {
      JOptionPane.showMessageDialog(null, "Invalid callsign!");
      jtextfieldCallsign.requestFocus();
      return false;
    }

    if(!Qso.isValidSerial(jtextfieldSnt.getText()))
    {
      JOptionPane.showMessageDialog(null, "Invalid Snt!");
      jtextfieldSnt.requestFocus();
      return false;
    }
    
    if(!Qso.isValidSerial(jtextfieldRcv.getText()))
    {
      JOptionPane.showMessageDialog(null, "Invalid Rcv!");
      jtextfieldRcv.requestFocus();
      return false;
    }
      
    
    // Add qso to log
    // ------------------------------
    Qso qso;
    try
    {
     
      qso = new Qso(getFreq(),
                    getMode(),     
                    applicationSettings.getMyCallsign(),
                    getCallsignFromTextField(),
                    jtextfieldSnt.getText(),
                    jtextfieldRcv.getText(),
                    getTypeOfWork());
    }
    catch (Exception exc)
    {
      return false;
    }

    jtablemodelLog.addRow(qso);
    return true;
  }

  
  /**
   * Determines if the current Type of work is SP or CQ
   * @return 
   */
  private String getTypeOfWork()
  {
    if(jradiobuttonSP.isSelected())
      return TYPE_OF_WORK_SP;
    else
      return TYPE_OF_WORK_CQ;
  }
  
  
  /**
   * Determines the current working mode. Takes into account if the connected
   * to the radio or not.
   * @return - String describing the mode (e.g. "cw", "ssb")
   */
  private String getMode()
  {
    // If radio is connected get the frequency from there
    if(radioController.isConnected())
    {
      RadioModes mode;
      mode = radioController.getMode();
      return mode.toString();
    }
    // If no radio is connected - read the mode from the combobox model
    else
    {
      String temp = jcomboboxMode.getSelectedItem().toString();
      return temp;
    }
  }
  
  
  /**
   * Determines the current working frequency. Takes into account if the connected
   * to the radio or not.
   * @return - frequency in Hz
   */
  private int getFreq()
  {
    int freq;
    
    // If radio is connected get the frequency from there
    if(radioController.isConnected())
    {
      freq = radioController.getFrequency();
    }
    // If no radio is connected - read the freq from the dropdown box
    else
    {
      String temp = jcomboboxBand.getSelectedItem().toString();
      // convert to Hz
      freq = Math.round(Float.parseFloat(temp)*1000000);
    }
    
    return freq;
  }
  
  
  /**
   * Gets the callsign from the jtextfieldCallsign.
   * If the callsign was inserted in the short form (e.g. HH) this function will return the full
   * form (i.e. LZ2HH)
   * @return - the callsign in its full form (e.g. LZ6HH)
   */
  private String getCallsignFromTextField()
  {
    String callsign = jtextfieldCallsign.getText();
    
    if(applicationSettings.isQuickCallsignModeEnabled())
    {
      callsign = applicationSettings.getDefaultPrefix()+callsign;
    }
    
    return callsign;
  }
  
  
  /**
   * Prints info concerning the callsign:
   * NEW - If no qso before
   * OK - Qso before but the required time has elapsed
   * DUPE time left... - Qso before and the required time has not elapsed
   * @param callsign
   * @return 
   */
  private String getCallsignStatusText(String callsign)
  {
    String statusText = "";

    Qso qso = log.getLastQso(callsign);

    // Unknown callsign - OK to work
    if (qso == null)
    {
      statusText = "NEW";
    }
    else
    {
      // DUPE
      if (log.getSecondsLeft(qso, applicationSettings.getQsoRepeatPeriod()) > 0)
      {
        // Print DUPE
        statusText = statusText.concat("DUPE   ");

        //Print the time left till next possible contact
        statusText = statusText.concat("time left " + 
                TimeUtils.getTimeLeftFormatted(log.getSecondsLeft(qso, applicationSettings.getQsoRepeatPeriod())));
        // Make it red
        statusText = "<html><font color=red>"+statusText+"</font></html>";
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
    jtextfieldCallsign.setText("");
    // Add the new Snt number
    jtextfieldSnt.setText(log.getNextSentReport());
    // Cean the Rcv field
    jtextfieldRcv.setText("");
    // Clean the callsign status
    jlabelCallsignStatus.setText("NEW");
    // Set focus to callsign field
    jtextfieldCallsign.requestFocus();
  }
  

  /**
   * @return Returns a new DefaultComboBoxModel containing all available COM ports
   */
  private DefaultComboBoxModel getComportsComboboxModel()
  {
    String[] portNames = SerialPortList.getPortNames();
    return new DefaultComboBoxModel(portNames);
  }
  
  
  /**
   * Initialize the controls of the main windows
   */
  private void initMainWindow(boolean isStartup)
  {
    if(isStartup)
    {
      // Restore the last used dimensions for the different frames
      this.setBounds(applicationSettings.getFrameDimensions(ApplicationSettings.FrameIndex.JFRAME));
      intframeBandmap.setBounds(applicationSettings.getFrameDimensions(ApplicationSettings.FrameIndex.BANDMAP));
      intframeEntryWindow.setBounds(applicationSettings.getFrameDimensions(ApplicationSettings.FrameIndex.ENTRY));
      intframeTimeToNextQso.setBounds(applicationSettings.getFrameDimensions(ApplicationSettings.FrameIndex.INCOMING_QSO));
      intframeLog.setBounds(applicationSettings.getFrameDimensions(ApplicationSettings.FrameIndex.LOG));
      intframeRadioConnection.setBounds(applicationSettings.getFrameDimensions(ApplicationSettings.FrameIndex.RADIO));
      intframeMisc.setBounds(applicationSettings.getFrameDimensions(ApplicationSettings.FrameIndex.SETTINGS));

      // Restore the bandmap settings
      jcomboboxStepInHz.setSelectedItem(Integer.toString(applicationSettings.getBandmapStepInHz()));
      jcomboboxColumnCount.setSelectedItem(Integer.toString(applicationSettings.getBandmapColumnCount()));
      jcomboboxRowCount.setSelectedItem(Integer.toString(applicationSettings.getBandmapRowCount()));

      // Restore the fonts
      jtextfieldCallsign.setFont(applicationSettings.getFonts(ApplicationSettings.FontIndex.CALLSIGN));
      jtextfieldSnt.setFont(applicationSettings.getFonts(ApplicationSettings.FontIndex.SNT));
      jtextfieldRcv.setFont(applicationSettings.getFonts(ApplicationSettings.FontIndex.RCV));
      jtableBandmap.setFont(applicationSettings.getFonts(ApplicationSettings.FontIndex.BANDMAP));
      jtableIncomingQso.setFont(applicationSettings.getFonts(ApplicationSettings.FontIndex.INCOMING_QSO));
      jtableLog.setFont(applicationSettings.getFonts(ApplicationSettings.FontIndex.LOG)); 
    }
    
    
    // Update the Function keys button text
    jButton4.setText("F4 "+applicationSettings.getMyCallsign());
    jButton6.setText("F6 "+applicationSettings.getFunctionKeyMessage(5));
    jButton7.setText("F7 "+applicationSettings.getFunctionKeyMessage(6));
    jButton8.setText("F8 "+applicationSettings.getFunctionKeyMessage(7));
    jButton9.setText("F9 "+applicationSettings.getFunctionKeyMessage(8));
    //jButton10.setText("F10 "+applicationSettings.getFunctionKeyText(9));
    
    // Set the CQ frequency
    jlabelCqFreq.setText(Misc.formatFrequency(Integer.toString(cqFrequency)));
  }
    
    
  /**
   * User has opened the setting dialog and we need to load the state of the controls
   */
  private void initSettingsDialog()
  {
    // Comport selection
    jComboBoxComPort.setSelectedItem(applicationSettings.getComPort());
  
    // my callsing texts
    textfieldSettingsMyCallsign.setText(applicationSettings.getMyCallsign());

    // Misc 
    checkboxSettingsQuickMode.setSelected(applicationSettings.isQuickCallsignModeEnabled());
    checkboxESM.setSelected(applicationSettings.isEms());
    checkboxF1JumpsToCq.setSelected(applicationSettings.isAutoCqJump());
    checkboxSendLeadingZeroAsT.setSelected(applicationSettings.isSendZeroAsT());
    
    // Set the text for the function keys
    jtextfieldf1.setText(applicationSettings.getFunctionKeyMessage(0));
    jtextfieldf3.setText(applicationSettings.getFunctionKeyMessage(2));
    jtextfieldf6.setText(applicationSettings.getFunctionKeyMessage(5));
    jtextfieldf7.setText(applicationSettings.getFunctionKeyMessage(6));
    jtextfieldf8.setText(applicationSettings.getFunctionKeyMessage(7));
    jtextfieldf9.setText(applicationSettings.getFunctionKeyMessage(8));
    jtextfieldf10.setText(applicationSettings.getFunctionKeyMessage(9));
    
    // Default prefix
    textfieldSettingsDefaultPrefix.setText(applicationSettings.getDefaultPrefix());
   
    if(applicationSettings.isQuickCallsignModeEnabled() == false) 
    {
      textfieldSettingsDefaultPrefix.setEnabled(false); // Disable the "default prefix" text field if the "Quick callsign mode" is disabled
    }
    
    // Repeat period in seconds
    jtextfieldQsoRepeatPeriod.setText(Integer.toString(applicationSettings.getQsoRepeatPeriod()));
    
    // Incoming qso hide after
    jTextField1.setText(Integer.toString(applicationSettings.getIncomingQsoHiderAfter()));
   
    // Incoming qso max entries
    //jTextField2.setText(Integer.toString(applicationSettings.getIncomingQsoMaxEntries()));
  }
    
    
  /**
   * User has closed the setting dialog and we need to save the state of the controls
   */
  private boolean storeSettingsDialogParams()
  { 
    // Commport
    if (jComboBoxComPort.getSelectedItem() != null)
    {
      applicationSettings.setComPort(jComboBoxComPort.getSelectedItem().toString());
    }
    
    // Callsign   
    if(!Qso.isValidCallsign(textfieldSettingsMyCallsign.getText()))
    {
      JOptionPane.showMessageDialog(null, "Invalid callsign!"); // Validate myCallsign
      return false;
    }
    applicationSettings.setMyCallsign(textfieldSettingsMyCallsign.getText());
    
    // Function keys texts
    applicationSettings.setFunctionKeyMessage(0, jtextfieldf1.getText());
    applicationSettings.setFunctionKeyMessage(2, jtextfieldf3.getText());
    applicationSettings.setFunctionKeyMessage(5, jtextfieldf6.getText());
    applicationSettings.setFunctionKeyMessage(6, jtextfieldf7.getText());
    applicationSettings.setFunctionKeyMessage(7, jtextfieldf8.getText());
    applicationSettings.setFunctionKeyMessage(8, jtextfieldf9.getText());
    applicationSettings.setFunctionKeyMessage(9, jtextfieldf10.getText());
    
   
    // Misc settings
    applicationSettings.setQuickCallsignMode(checkboxSettingsQuickMode.isSelected());
    applicationSettings.setAutoCqJump(checkboxF1JumpsToCq.isSelected());
    applicationSettings.setEms(checkboxESM.isSelected());
    applicationSettings.setSendZeroAsT(checkboxSendLeadingZeroAsT.isSelected());
    
    
    // Default prefix
    applicationSettings.setDefaultPrefix(textfieldSettingsDefaultPrefix.getText());
    
    
    // Qso repeat period
    try
    {
      int temp = Integer.parseInt(jtextfieldQsoRepeatPeriod.getText());
      if(temp<=0)
        throw new Exception("invalid Qso repeat period!");
        
      applicationSettings.setQsoRepeatPeriod(temp);
    }catch(Exception exc)
    {
      JOptionPane.showMessageDialog(null, "Invalid repeat Qso period! Must be a number.");
      return false;
    }
    
    // Incoming Qso "hideAfter" and "maxEntries"
    try
    {
      int temp = Integer.parseInt(jTextField1.getText());
      if(temp>=0)
        throw new Exception("Invalid hideAfter entry!");
      applicationSettings.setIncomingQsoHiderAfter(temp);
      //applicationSettings.setIncomingQsoMaxEntries(Integer.parseInt(jTextField2.getText()));
    }catch(Exception exc)
    {
      JOptionPane.showMessageDialog(null, "Incoming Qso panel - invalid entry");
      return false;
    }
    
    return true;
  }

  
  private void pressedF1()
  {
    // if "jump to cq freq" is enabled we will jump to the cq frequency (cq freq can be set through the button "set cq freq"
    if(jcheckboxF1jumpsToCq.isSelected())
    { 
      if(getFreq()<(cqFrequency-50) || getFreq()>(cqFrequency+50) )
      {
        try
        {
          radioController.setFrequency(cqFrequency);
          Thread.sleep(300);
        }
        catch (InterruptedException ex)
        {
          Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    // if not enabled - remember the cq frequency
    else
    {
      cqFrequency = getFreq();
      jlabelCqFreq.setText(Misc.formatFrequency(Integer.toString(cqFrequency)));
    }
    
    String text = applicationSettings.getFunctionKeyMessage(0);  // Get the text for the F1 key
    text = text.replaceAll("\\{mycall\\}", applicationSettings.getMyCallsign()); // Substitute {mycall} with my callsign
    radioController.sendMorse(text);                          // Send to radio
   
    // Select the CQ radio button
    jradiobuttonCQ.setSelected(true);
    
    
    // Continious CQ is enabled ...
    if(jcheckboxContinuousCq.isSelected())
    {
      int period = getCqDuration(text,keyerSpeed);
      period += Integer.parseInt(jtextfieldContinuousCqPeriod.getText());
      timerContinuousCq = new Timer(period, timerContinuousCqListener);
      timerContinuousCq.setRepeats(true);
      timerContinuousCq.start();
    }
  }
  
  
  /**
   * Calculates the duration of the CQ depending on the morse code speed and the text that needs to be sent
   * 
   * @return Duration of the CQ in milliseconds
   */
  private int getCqDuration(String text, int speed)
  {
    int duration = 2000;
    
    
    
    return duration;
  }
  
  
  private void pressedF2()
  {
    sendSerial();
  }
  
  private void pressedF3()
  {
    radioController.sendMorse(applicationSettings.getFunctionKeyMessage(2));
  }
  
  private void pressedF4()
  {
    radioController.sendMorse(applicationSettings.getMyCallsign());
  }
  
  private void pressedF5()
  {
    radioController.sendMorse(getCallsignFromTextField());
  }
  
  private void pressedF6()
  {
    radioController.sendMorse(applicationSettings.getFunctionKeyMessage(5));
  }
  
  private void pressedF7()
  {
    radioController.sendMorse(applicationSettings.getFunctionKeyMessage(6));
  }
  
  private void pressedF8()
  {
    radioController.sendMorse(applicationSettings.getFunctionKeyMessage(7));
  }
  
  private void pressedF9()
  {
    radioController.sendMorse(applicationSettings.getFunctionKeyMessage(8));
  }
  
  private void pressedF10()
  {
    radioController.sendMorse(applicationSettings.getFunctionKeyMessage(9));
  }
  
  private void pressedF11()
  {
    if(Qso.isValidCallsign(getCallsignFromTextField()))
      jtablemodelBandmap.addSpot(getCallsignFromTextField(), getFreq());
    initEntryFields();
  }
  
  private void pressedEsc()
  {
    radioController.interruptMorseSending();
  }
  
  private void pressedF12()
  {
    initEntryFields();
  }
  
  
  private int calculateFrequencyChange()
  {
    return Math.abs(cqFrequency-getFreq());
  }
  
  
  private void sendSerial()
  {
    String serial;
    // If F2 is pressed and jtextfieldCallsign is empty we should Snt from the last qso 
    if(jtextfieldCallsign.getText().isEmpty() && log.getSize()>0)
    {
      serial = log.getLastQso().getSnt();
    }
    else
    {
      serial = jtextfieldSnt.getText().replaceAll("\\s", ""); // Get the serial removing white spaces
    }
    

    // If needed substitute leading zeros with 'T'
    if(applicationSettings.isSendZeroAsT())
    {
      serial = Misc.leadingZerosToT(serial);
    }
   
    radioController.sendMorse(serial.substring(0, 3)+ " " +serial.substring(3, 6));
    
    
  }
  
  
  class LocalRadioControllerListener implements RadioController.RadioControllerListener
  {

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
          jtextfieldFrequency.setText(radioController.getActiveVfo().toString()+" " +Integer.toString(radioController.getFrequency()));
          
          // Set to S&P if in CQ mode and CQ frequency has changed with 500Hz
          if(jradiobuttonCQ.isSelected() && calculateFrequencyChange()>500)
          {
            jradiobuttonSP.setSelected(true);
          }
          
          // We need to repaint the bandmap table so that the fequency marker is updated
          jtableBandmap.repaint();
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
          jtextfieldFrequency.setText(radioController.getActiveVfo().toString()+" " +Integer.toString(radioController.getFrequency()));
        
          // Set to S&P if in CQ mode and CQ frequency has changed with 500Hz
          if(jradiobuttonCQ.isSelected() && calculateFrequencyChange()>500)
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
      for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
      {
        if ("Nimbus".equals(info.getName()))
        {
          javax.swing.UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    }
    catch (ClassNotFoundException ex)
    {
      java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
    catch (InstantiationException ex)
    {
      java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
    catch (IllegalAccessException ex)
    {
      java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
    catch (javax.swing.UnsupportedLookAndFeelException ex)
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
      if (returnVal != JFileChooser.APPROVE_OPTION)
      {
        return false;
      } 
    }
    catch (Exception exc)
    {
      JOptionPane.showMessageDialog(null, "Error when trying to acquire log database file.", "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
    
    logDbFile = fc.getSelectedFile().getAbsolutePath();
    if(!logDbFile.endsWith(".db4o"))
    {
      logDbFile = logDbFile+".db4o";
    }
        
    File file = new File(logDbFile);
    
    if(file.exists())
    {
      JOptionPane.showMessageDialog(null, "File already exists: "+file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
    return true;
  }
          
  /**
   * Will return true if existing .db4o log file was found. The log file absolute path will be 
   * written in logDbFile.
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
      if (returnVal != JFileChooser.APPROVE_OPTION)
      {
        return false;
      } 
    }
    catch (Exception exc)
    {
      JOptionPane.showMessageDialog(null, "Error when trying to acquire log database file.", "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
    
    logDbFile = fc.getSelectedFile().getAbsolutePath();
    File file = new File(logDbFile);
    
    if(!file.exists())
    {
      JOptionPane.showMessageDialog(null, "Log file not found: "+file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
    return true;
  }

  
  
  private class MyDispatcher implements KeyEventDispatcher
  {

    @Override
    public boolean dispatchKeyEvent(KeyEvent evt)
    {
      if (evt.getID() != KeyEvent.KEY_RELEASED) 
        return false;
      
      // Function keys events
      switch (evt.getKeyCode())
      {
        case KeyEvent.VK_F1:
          pressedF1();
          evt.consume();
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
          
//        case KeyEvent.VK_F10:
//          pressedF10();
//          evt.consume();
//          break;
          
        case KeyEvent.VK_F11:
          pressedF11();
          evt.consume();
          break;
          
        case KeyEvent.VK_W:
          if(evt.isControlDown() || evt.isAltDown())
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
      Component comp = super.getTableCellRendererComponent(table, value,isSelected, hasFocus, row, column);
      //SJComponent jc = (JComponent) comp;
            
      // 
      if(jtablemodelIncomingQso.containsExpiredCallsign(row, column))
      {
        setForeground(Color.BLUE);
      }
      else
      {
        setForeground(Color.black);    
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
      Component comp = super.getTableCellRendererComponent(table, value,isSelected, hasFocus, row, column);
      //SJComponent jc = (JComponent) comp;
            
            
      // Show the current freq of the radio by highlighting the appropriate cell
      if(jtablemodelBandmap.isCurrentFreqInThisCell(row, column, getFreq()))
      {
        setBackground(Color.LIGHT_GRAY);
      }
      else
      {
        setBackground(Color.white);    
        setForeground(Color.black);    
      }
      
//      // Show which callsigns should be worked by marking them in BLUE
//      if(bandmapQsoTableModel.containsExpiredCallsign(row, column))
//      {
//        setForeground(Color.BLUE);
//      }
//      else
//      {
//        setForeground(Color.BLACK);
//      }
      return this;
    }
  }
  
  
  
  class UppercaseDocumentFilter extends DocumentFilter
  {

    @Override
    public void insertString(DocumentFilter.FilterBypass fb, int offset,
            String text, AttributeSet attr) throws BadLocationException
    {

      fb.insertString(offset, text.toUpperCase(), attr);
    }

    @Override
    public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
            String text, AttributeSet attrs) throws BadLocationException
    {

      fb.replace(offset, length, text.toUpperCase(), attrs);
    }
  }
  
  
  class SerialNumberDocumentFilter extends DocumentFilter
  {
   
    @Override
    public void insertString(DocumentFilter.FilterBypass fb, int offset,
            String text, AttributeSet attr) throws BadLocationException
    {
      int overlimit = fb.getDocument().getLength()+text.length() - SERIAL_NUMBER_LENGTH;
      if(overlimit > 0)
      {
        fb.insertString(offset, text.substring(0, text.length()-overlimit), attr);
      }
      fb.insertString(offset, text, attr);
    }

    @Override
    public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
            String text, AttributeSet attrs) throws BadLocationException
    {
      int currentLength = fb.getDocument().getLength();
      int overLimit = (currentLength + text.length()) - SERIAL_NUMBER_LENGTH - length;
      if (overLimit > 0)
      {
        text = text.substring(0, text.length() - overLimit);
      }
      
      super.replace(fb, offset, length, text, attrs);
     
    }
  }
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.ButtonGroup buttonGroupTypeOfWork;
  private javax.swing.JCheckBox checkboxESM;
  private javax.swing.JCheckBox checkboxF1JumpsToCq;
  private javax.swing.JCheckBox checkboxSendLeadingZeroAsT;
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
  private javax.swing.JButton jButton3;
  private javax.swing.JButton jButton4;
  private javax.swing.JButton jButton5;
  private javax.swing.JButton jButton6;
  private javax.swing.JButton jButton7;
  private javax.swing.JButton jButton8;
  private javax.swing.JButton jButton9;
  private javax.swing.JButton jButtonCancel;
  private javax.swing.JButton jButtonSave;
  private javax.swing.JComboBox jComboBoxComPort;
  private javax.swing.JDesktopPane jDesktopPane1;
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
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JLabel jLabel6;
  private javax.swing.JLabel jLabel7;
  private javax.swing.JLabel jLabel8;
  private javax.swing.JLabel jLabel9;
  private javax.swing.JMenu jMenu1;
  private javax.swing.JMenu jMenu2;
  private javax.swing.JMenuBar jMenuBar1;
  private javax.swing.JMenuItem jMenuItem1;
  private javax.swing.JMenuItem jMenuItem2;
  private javax.swing.JMenuItem jMenuItem3;
  private javax.swing.JMenuItem jMenuItem4;
  private javax.swing.JMenuItem jMenuItem5;
  private javax.swing.JMenuItem jMenuItem6;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel10;
  private javax.swing.JPanel jPanel11;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JPanel jPanel5;
  private javax.swing.JPanel jPanel6;
  private javax.swing.JPanel jPanel7;
  private javax.swing.JPanel jPanel8;
  private javax.swing.JPanel jPanel9;
  private javax.swing.JPanel jPanelStatusBar;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JScrollPane jScrollPane3;
  private javax.swing.JScrollPane jScrollPane4;
  private javax.swing.JScrollPane jScrollPane5;
  private javax.swing.JTextField jTextField1;
  private javax.swing.JButton jbuttonCreateNewLog;
  private javax.swing.JButton jbuttonDeleteEntry;
  private javax.swing.JButton jbuttonJumpToCqFreq;
  private javax.swing.JButton jbuttonKeyerDown;
  private javax.swing.JButton jbuttonKeyerUP;
  private javax.swing.JButton jbuttonOpenExistingLog;
  private javax.swing.JButton jbuttonSetCqFreq;
  private javax.swing.JCheckBox jcheckboxContinuousCq;
  private javax.swing.JCheckBox jcheckboxF1jumpsToCq;
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
  private javax.swing.JPanel jpanelKeyerSettings;
  private javax.swing.JPanel jpanelRadioConnection;
  private javax.swing.JPanel jpanelTypeOfWork;
  private javax.swing.JRadioButton jradiobuttonCQ;
  private javax.swing.JRadioButton jradiobuttonSP;
  private javax.swing.JTable jtableBandmap;
  private javax.swing.JTable jtableIncomingQso;
  private javax.swing.JTable jtableLog;
  private javax.swing.JTextField jtextfieldCallsign;
  private javax.swing.JTextField jtextfieldContinuousCqPeriod;
  private javax.swing.JTextField jtextfieldFreqWidth;
  private javax.swing.JTextField jtextfieldFrequency;
  private javax.swing.JTextField jtextfieldMode;
  private javax.swing.JTextField jtextfieldQsoRepeatPeriod;
  private javax.swing.JTextField jtextfieldRcv;
  private javax.swing.JTextField jtextfieldSnt;
  private javax.swing.JTextField jtextfieldf1;
  private javax.swing.JTextField jtextfieldf10;
  private javax.swing.JTextField jtextfieldf3;
  private javax.swing.JTextField jtextfieldf6;
  private javax.swing.JTextField jtextfieldf7;
  private javax.swing.JTextField jtextfieldf8;
  private javax.swing.JTextField jtextfieldf9;
  private javax.swing.JToggleButton jtogglebuttonConnectToRadio;
  private javax.swing.JTextField textfieldSettingsDefaultPrefix;
  private javax.swing.JTextField textfieldSettingsMyCallsign;
  // End of variables declaration//GEN-END:variables
}
