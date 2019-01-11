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
package org.lz1aq.atu;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import static javax.swing.plaf.basic.BasicSliderUI.NEGATIVE_SCROLL;
import static javax.swing.plaf.basic.BasicSliderUI.POSITIVE_SCROLL;
import javax.swing.plaf.synth.SynthSliderUI;
import org.lz1aq.lzlog.MainWindow;
import org.lz1aq.radio.RadioController;
import org.lz1aq.radio.RadioModes;
import org.lz1aq.tuner.TunerController;
import org.lz1aq.utils.ComPortProperties;

/**
 *
 * @author levkov_cha_ext
 */
public class AtuApplication extends javax.swing.JFrame
{
  public final String appTitle = "ATU";
  public final String appVersion = "0.9";
  
  static final String ATU_COMPORT_PROPERTIES_FILE = "AtuSerialPortSettings.properties";
  static final String RADIO_COMPORT_PROPERTIES_FILE = "RadioSerialPortSettings.properties";
          
  private final AtuApplicationSettings  applicationSettings;
  private final ComPortProperties       atuSerialPortSettings;
  private final ComPortProperties       radioSerialPortSettings;
  private final TunerController         tunerController;
  private final TunerController.TunerControllerListener tunerControllerListener = new LocalTunerControllerListener();
  private final RadioController         radioController;
  
  private String                        pathToWorkingDir; // where the ATU jar file is located
  
  private final TuneSettings tuneSettings;
  private JToggleButton[] bandButtons;
  private JToggleButton[] antennaButtons;
  private JToggleButton[] modeButtons;
  private List<JToggleButton>   tuneBoxButtons; 
  private List<JSlider>  sliderButtons = new ArrayList<>(AtuApplicationSettings.NUMBER_OF_SLIDER_BUTTONS);  
  
  // Variables used for controlling the AUTO mode - i.e. where we search for the tune setting with best SWR
  private boolean isAutoTuneModeActive = false;
  private int     autoTuneCurrentTune;  // Index of the currently selected Tune; -1 if we are just starting
  private int     autoTuneBestSwrIndex; // Index of the tune setting that has the best SWR
  private float   autoTuneBestSwrValue; // The lowest value of the SWR 
  
  
  private static final Logger logger = Logger.getLogger(AtuApplication.class.getName());
  
  
  
  /**
   * Creates new form AtuApp
   */
  public AtuApplication()
  {
    applicationSettings = new AtuApplicationSettings(); // Load user settings for the application from a properties file
    atuSerialPortSettings = new ComPortProperties(ATU_COMPORT_PROPERTIES_FILE);
    radioSerialPortSettings = new ComPortProperties(RADIO_COMPORT_PROPERTIES_FILE);
    
    tuneSettings = new TuneSettings();  // Load tune settings from a file
    tunerController = new TunerController(atuSerialPortSettings);
    tunerController.addEventListener(tunerControllerListener);
    
    radioController = new RadioController();
    if(loadRadioProtocolParser())// Select the python file describing the radio protocol
      connectToRadio(); // now we can try to connect
    
    initComponents(); // GUI
    
    jSliderC1.setUI(new MySliderUI(jSliderC1)); // Custom UI for slider buttons (needed for PG UP/DWN scrolling)
    jSliderL.setUI(new MySliderUI(jSliderL));

    populateTuneBox(); // Add buttons to the TuneBox dialog

    packButtonsIntoStructure();
  }
  
  
  /**
   * Initialize the controls of the main windows
   */
  private void initMainWindow()
  {
    this.setTitle(appTitle+" v"+appVersion);
    
    jProgressBarSwr.setStringPainted(true); // So that text is displayed on the progress bar
    jProgressBarSwr.setBackground(Color.green);
    jProgressBarSwr.setValue(50);
    jProgressBarAntennaV.setStringPainted(true); // So that text is displayed on the progress bar
    
    setAntennaButtonsLabels();

    // Read last used JFrame dimensions and restore it
    if(applicationSettings.getMainWindowDimensions().isEmpty() == false)
    {
      this.setBounds(applicationSettings.getMainWindowDimensions());
    }

    // Init by starting with antenna - as this will switch all the relays to the proper position sending only one command to the Tuner.
    jToggleButtonAnt1.setSelected(true);
    jToggleButtonBand1.setSelected(true);
    jToggleButtonSsb.setSelected(true);
  }

  
  private void setAntennaButtonsLabels()
  {
    for(int i = 0; i < antennaButtons.length; i++)
    {
      antennaButtons[i].setText(applicationSettings.getAntennaLabel(i));
    }
  }

  private void packButtonsIntoStructure()
  {
    // order of inclusion is important
    
    bandButtons = new JToggleButton[]
    {
      jToggleButtonBand1,
      jToggleButtonBand2,
      jToggleButtonBand3,
      jToggleButtonBand4,
      jToggleButtonBand5,
      jToggleButtonBand6,
      jToggleButtonBand7,
      jToggleButtonBand8,
      jToggleButtonBand9
    };

    antennaButtons = new JToggleButton[]
    {
      jToggleButtonAnt1,
      jToggleButtonAnt2,
      jToggleButtonAnt3,
      jToggleButtonAnt4,
      jToggleButtonAnt5,
      jToggleButtonAnt6
    };

    modeButtons = new JToggleButton[]
    {
      jToggleButtonCw,
      jToggleButtonSsb
    };
    
    
    sliderButtons.add(jSliderC1);
    sliderButtons.add(jSliderL);
  }

  
  /**
   * Sets the Sliders and C1/C2 to the appropriate current values
   */  
  private void updateTuneControls()
  {
    TuneValue tune = getCurrentTune();
    
    // update slider values to represent the current tune
    jSliderC1.setValue(tune.getC());
    jSliderL.setValue(tune.getL());
    jToggleButtonN.setSelected(tune.isN());
  }

  
  private boolean loadRadioProtocolParser()
  { 
    boolean result = radioController.loadProtocolParser(applicationSettings.getRadioProtocolParserLocation());
    if (result == false)
    {
      JOptionPane.showMessageDialog(null, "Error when trying to load the radio protocol parser file!", "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
     
    // Show the serial settings that we are going to use when connecting to this radio
    JOptionPane.showMessageDialog(null, radioController.getInfo());
    return true;
  }
  
  
  private boolean connectToRadio()
  {
    boolean result =   radioController.connect(radioSerialPortSettings.getComPortName(), 
                                                radioSerialPortSettings.getBaudRate(), 
                                                null);
    if (!result)
    {
      JOptionPane.showMessageDialog(null, "Could not connect to radio!", "Serial connection error...", JOptionPane.ERROR_MESSAGE);
    }
    
    return result;
  }
  
  
  TuneValue getCurrentTune()
  {
     return tuneSettings.get(applicationSettings.getCurrentBandSelection(),
                             applicationSettings.getCurrentAntSelection(),
                             applicationSettings.getCurrentModeSelection(),
                             applicationSettings.getCurrentTuneSelection() );
  }

  
  private void onBandButtonPress(int bandButton)
  {
    if(jToggleButtonTune.isSelected())
      return; // do nothing in case tune mode is active
    
    applicationSettings.setCurrentBandSelection(bandButton);
    TuneValue tune = getCurrentTune(); // Get the tune for the current combination of band, ant and mode
  
    boolean ret = tunerController.setTuneControls(tune.getC(), tune.getL(), tune.isN()); // Take care of sending the command to the Tuner now - so that later when sliders state change we won't need to send for each slider a separate command to the tunner.
    if(!ret)
      System.err.println("setTuneControls() failed!");
    
    updateTuneControls(); // Set the tuning controls to respective values
    updateTuneBoxValues(); 
    
    changeRadioBand(bandButton);
    changeRadioMode(applicationSettings.getCurrentModeSelection());
  }

  
  private void changeRadioBand(int bandButtonPressed)
  {
    boolean isCw = false;
    if(applicationSettings.getCurrentModeSelection() == 1)
      isCw = true;
   
    
    switch(bandButtonPressed)
    {
      case 0: // 1.8
        if(isCw)
          radioController.setFrequency(1820000);
        else
          radioController.setFrequency(1850000);
        break;
        
      case 1: // 3.5
          if(isCw)
          radioController.setFrequency(3510000);
        else
          radioController.setFrequency(3700000);
        break;
        
      case 2: // 7
        if(isCw)
          radioController.setFrequency(7010000);
        else
          radioController.setFrequency(7120000);
        break;
        
      case 3: // 10
        if(isCw)
          radioController.setFrequency(10110000);
        else
          radioController.setFrequency(10110000);
        break;
        
      case 4: // 14
        if(isCw)
          radioController.setFrequency(14015000);
        else
          radioController.setFrequency(14200000);
        break;
      case 5: // 18
        if(isCw)
          radioController.setFrequency(18070000);
        else
          radioController.setFrequency(18130000);
        break;
      case 6: // 21
        if(isCw)
          radioController.setFrequency(21010000);
        else
          radioController.setFrequency(21200000);
        break;
      case 7: // 24
        if(isCw)
          radioController.setFrequency(24900000);
        else
          radioController.setFrequency(24940000);
        break;
      case 8: // 28
        if(isCw)
          radioController.setFrequency(28010000);
        else
          radioController.setFrequency(28450000);
    }
  }
  
  
  private void onAntennaButtonPress(int antennaButton)
  {
    if(jToggleButtonTune.isSelected())
      return; // do nothing in case tune mode is active
     
    applicationSettings.setCurrentAntSelection(antennaButton);
    TuneValue tune = getCurrentTune();
    
    boolean ret = tunerController.setAll(antennaButton, tune.getC(), tune.getL(), tune.isN()); 
    if(!ret)
      System.err.println("setTuneControls() failed!");
    
    updateTuneControls(); // Set the tuning controls to respective values
    updateTuneBoxValues(); 
  }

  
  private void onModeButtonPress(int modeButton)
  {
    // TODO: use instead radioController.isTransmiiting()
    if(jToggleButtonTune.isSelected())
      return; // do nothing in case tune mode is active
     
    applicationSettings.setCurrentModeSelection(modeButton);
    TuneValue tune = getCurrentTune();
    
    boolean ret = tunerController.setTuneControls(tune.getC(), tune.getL(), tune.isN()); // Take care of sending the command to the Tuner now - so that later when sliders state change we won't need to send for each slider a separate command to the tunner.
    if(!ret)
      System.err.println("setTuneControls() failed!");
    
    updateTuneControls(); // Set the tuning controls to the respective values
    updateTuneBoxValues(); 
    
    changeRadioBand(applicationSettings.getCurrentBandSelection());
    changeRadioMode(modeButton);
  }
  
  
  private void changeRadioMode(int modeButton)
  {
    if(modeButton == 1)
      radioController.setMode(RadioModes.CW);
    else
    {
       // If higher than 10MHz
      if(applicationSettings.getCurrentBandSelection()>3)
        radioController.setMode(RadioModes.USB);
      else
        radioController.setMode(RadioModes.LSB);
    }
  }
  
  
  void onTuneBoxButtonPress(int buttonIndex)
  {
    applicationSettings.setCurrentTuneSelection(buttonIndex);
    TuneValue tune = getCurrentTune();
    
    boolean ret = tunerController.setTuneControls(tune.getC(), tune.getL(), tune.isN());
    if(!ret)
      System.err.println("setTuneControls() failed!");
    
    updateTuneControls();
  }
  
   
  private void populateTuneBox()
  { 
    tuneBoxButtons = new ArrayList<>(AtuApplicationSettings.NUMBER_OF_TUNE_VALUES);
    
    for(int i=0; i<AtuApplicationSettings.NUMBER_OF_TUNE_VALUES; i++)
    {
      JToggleButton jb = new JToggleButton("tune "+i);
      tuneBoxButtons.add(jb);
      jPanelTuneBox.add(jb);
      buttonGroupTuneBox.add(jb);
      jb.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          JToggleButton button = (JToggleButton)e.getSource();
          onTuneBoxButtonPress(tuneBoxButtons.indexOf(button));
        }
      });
    }
  }
  
  
  private void updateTuneBoxText(int index, TuneValue tune)
  { 
    String c;
    if(tune.isN())
      c = "C_ANT";
    else
      c = "C_TX";
    tuneBoxButtons.get(index).setText("c1= "+tune.getC()+"  l= "+tune.getL()+"   C="+c);  
  }
   
  
  /**
   * Update the text of the buttons and select the button that needs to be active
   */
  private void updateTuneBoxValues()
  {
    TuneValue tune;
    
    for(int i=0; i<tuneBoxButtons.size(); i++)
    {
      tune = tuneSettings.get(applicationSettings.getCurrentBandSelection(), 
                             applicationSettings.getCurrentAntSelection(), 
                             applicationSettings.getCurrentModeSelection(), 
                             i);
      
      
      updateTuneBoxText(i, tune);
      //tuneBoxButtons.get(i).setText("c1="+tune.getC()+"  l="+tune.getL()+" >C="+tune.isN());  
    }
    
    // Highlight the current selection
    tuneBoxButtons.get(applicationSettings.getCurrentTuneSelection()).setSelected(true);
  }
  
  
 /**
  * A button from the TuneBox dialog is considered valid if at least one tune value is different from 0
  * @return true - if next Tune was selected; false - there are no more valid Tunes nothing has been selected
  */
  private boolean selectNextValidTuneBoxButton()
  {
    autoTuneCurrentTune++;
    
    // find the first Tune setting that has at least one value different than 0;
    for(int i=autoTuneCurrentTune; i<AtuApplicationSettings.NUMBER_OF_TUNE_VALUES; i++)
    {
      TuneValue tune= tuneSettings.get(applicationSettings.getCurrentBandSelection(),
                                       applicationSettings.getCurrentAntSelection(), 
                                       applicationSettings.getCurrentModeSelection(), 
                                       i);
      if(tune.getC()!=0 || tune.getL()!=0 )
      {
        tuneBoxButtons.get(i).setSelected(true);
        return true;
      }     
    }
    
    return false;
  }
  
   // Converts SWR to progress bar value 
  private static int swrToProgressBarValue(float swr)
  {
    int value;

    if(swr < 1)
    {
      swr = 1;
    }

    // SWR 1 will be 0 on the progress bar
    swr -= 1;

    // Different scaling for different ranges of the SWR
    if(swr <= 10)
    {
      value = Math.round(swr * 10);
    }
    else
    {
      value = Math.round(swr);
    }

    return value;
  }

  // Converts Antenna Voltage to progress bar value
  private static int antVotageToProgressBarValue(float voltage)
  {
    voltage = (voltage / 26214) * 100;

    return Math.round(voltage);
  }

  
  // All needed actions in order to start measuring SWR for each tune setting and selecting the best one
  void initAutoTuneMode()
  {
    if(jToggleButtonTune.isSelected())
      jToggleButtonTune.setSelected(false);
      
    isAutoTuneModeActive = true;
    autoTuneBestSwrIndex = 0;
    autoTuneCurrentTune = -1;
    autoTuneBestSwrValue = 0;
    
    tunerController.enableTuneMode();
  }
  
  // Stop autoTune mode
  void clearAutoTuneMode()
  {
    isAutoTuneModeActive = false;
 
    tunerController.disableTuneMode();
  }
 
 
  
  //----------------------------------------------------------------------
  //                           Internal Classes
  //----------------------------------------------------------------------
  class LocalTunerControllerListener implements TunerController.TunerControllerListener
  {
    @Override
    // Event coming from the TunnerController
    public void eventAdc()
    {
      SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() 
            {
              // SWR
              // ----
              jProgressBarSwr.setValue(swrToProgressBarValue(tunerController.getSwr())); 
              System.out.println("SWR------------> "+Float.toString(tunerController.getSwr()));
              jProgressBarSwr.setString(String.format("%.1f", tunerController.getSwr()));

              // Antenna Voltage
              // ----
              jProgressBarAntennaV.setValue(antVotageToProgressBarValue(tunerController.getAntennaV()/(float)1000));
              jProgressBarAntennaV.setString(String.format ("%.1f", tunerController.getAntennaV()/(float)1000)); 
            }
        });

      
    }

    @Override
    public void eventPosConfirmation()
    {
    }

    @Override
    public void eventNegConfirmation()
    {
    } 
  }
  
  
  /**
   *  We need a class where PG UP and PG DWN have different than the default scroll steps
   */
  class MySliderUI extends SynthSliderUI
  {
    protected MySliderUI(JSlider c) 
    {
        super(c);
    }

    private static final int SLIDER_FRACTION = TunerController.C1_MAX/80;

    
    @Override
    public void scrollByBlock(final int direction)
    {
      synchronized (slider)
      {
        int blockIncrement = (slider.getMaximum() - slider.getMinimum()) / SLIDER_FRACTION;
        if(blockIncrement == 0)
          blockIncrement = 1;

        if(slider.getSnapToTicks())
        {
          int tickSpacing = getTickSpacing();

          if(blockIncrement < tickSpacing)
          {
            blockIncrement = tickSpacing;
          }
        }

        int delta = blockIncrement * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);
        slider.setValue(slider.getValue() + delta);
      }
    }
    
    private int getTickSpacing() {
        int majorTickSpacing = slider.getMajorTickSpacing();
        int minorTickSpacing = slider.getMinorTickSpacing();

        int result;

        if (minorTickSpacing > 0) {
            result = minorTickSpacing;
        } else if (majorTickSpacing > 0) {
            result = majorTickSpacing;
        } else {
            result = 0;
        }

        return result;
    }
  }
  
  
  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    java.awt.GridBagConstraints gridBagConstraints;

    buttonGroupBand = new javax.swing.ButtonGroup();
    buttonGroupAnt = new javax.swing.ButtonGroup();
    buttonGroupMode = new javax.swing.ButtonGroup();
    jDialogTuneBox = new javax.swing.JDialog(this);
    jPanelTuneBox = new javax.swing.JPanel();
    jButton4 = new javax.swing.JButton();
    buttonGroupTuneBox = new javax.swing.ButtonGroup();
    jPanelDisplay = new javax.swing.JPanel();
    jPanelSwr = new javax.swing.JPanel();
    jProgressBarSwr = new javax.swing.JProgressBar();
    jLabel3 = new javax.swing.JLabel();
    jPanelAntVoltage = new javax.swing.JPanel();
    jProgressBarAntennaV = new javax.swing.JProgressBar();
    jLabel4 = new javax.swing.JLabel();
    jPanelRefVoltage = new javax.swing.JPanel();
    jLabel1 = new javax.swing.JLabel();
    jProgressBarRefV = new javax.swing.JProgressBar();
    jPanelMiscButtons = new javax.swing.JPanel();
    jToggleButtonSsb = new javax.swing.JToggleButton();
    jToggleButtonCw = new javax.swing.JToggleButton();
    jPanelSliderControls = new javax.swing.JPanel();
    jSliderC1 = new javax.swing.JSlider();
    jSliderL = new javax.swing.JSlider();
    jToggleButtonN = new javax.swing.JToggleButton();
    jLabel2 = new javax.swing.JLabel();
    jLabel5 = new javax.swing.JLabel();
    jButton3 = new javax.swing.JButton();
    jToggleButtonTune = new javax.swing.JToggleButton();
    jToggleButtonAuto = new javax.swing.JToggleButton();
    jPanelBands = new javax.swing.JPanel();
    jToggleButtonBand1 = new javax.swing.JToggleButton();
    jToggleButtonBand2 = new javax.swing.JToggleButton();
    jToggleButtonBand3 = new javax.swing.JToggleButton();
    jToggleButtonBand4 = new javax.swing.JToggleButton();
    jToggleButtonBand5 = new javax.swing.JToggleButton();
    jToggleButtonBand6 = new javax.swing.JToggleButton();
    jToggleButtonBand7 = new javax.swing.JToggleButton();
    jToggleButtonBand8 = new javax.swing.JToggleButton();
    jToggleButtonBand9 = new javax.swing.JToggleButton();
    jPanelAntennas = new javax.swing.JPanel();
    jToggleButtonAnt1 = new javax.swing.JToggleButton();
    jToggleButtonAnt2 = new javax.swing.JToggleButton();
    jToggleButtonAnt3 = new javax.swing.JToggleButton();
    jToggleButtonAnt4 = new javax.swing.JToggleButton();
    jToggleButtonAnt5 = new javax.swing.JToggleButton();
    jToggleButtonAnt6 = new javax.swing.JToggleButton();
    jMenuBar1 = new javax.swing.JMenuBar();
    jMenu1 = new javax.swing.JMenu();
    jMenu2 = new javax.swing.JMenu();

    jDialogTuneBox.setMinimumSize(new java.awt.Dimension(50, 200));
    jDialogTuneBox.addWindowListener(new java.awt.event.WindowAdapter()
    {
      public void windowDeactivated(java.awt.event.WindowEvent evt)
      {
        jDialogTuneBoxWindowDeactivated(evt);
      }
      public void windowOpened(java.awt.event.WindowEvent evt)
      {
        jDialogTuneBoxWindowOpened(evt);
      }
    });
    jDialogTuneBox.getContentPane().setLayout(new java.awt.GridBagLayout());

    jPanelTuneBox.setLayout(new java.awt.GridLayout(0, 1));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jDialogTuneBox.getContentPane().add(jPanelTuneBox, gridBagConstraints);

    jButton4.setFont(new java.awt.Font("Tahoma", 1, 16)); // NOI18N
    jButton4.setText("CLOSE");
    jButton4.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButton4ActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.weighty = 0.1;
    jDialogTuneBox.getContentPane().add(jButton4, gridBagConstraints);

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
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
    getContentPane().setLayout(new java.awt.GridLayout(2, 3));

    jPanelDisplay.setLayout(new java.awt.GridBagLayout());

    jPanelSwr.setLayout(new java.awt.GridBagLayout());

    jProgressBarSwr.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
    jProgressBarSwr.setOrientation(1);
    jProgressBarSwr.setToolTipText("");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelSwr.add(jProgressBarSwr, gridBagConstraints);

    jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabel3.setText("SWR");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.weighty = 0.1;
    jPanelSwr.add(jLabel3, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    jPanelDisplay.add(jPanelSwr, gridBagConstraints);

    jPanelAntVoltage.setLayout(new java.awt.GridBagLayout());

    jProgressBarAntennaV.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
    jProgressBarAntennaV.setOrientation(1);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelAntVoltage.add(jProgressBarAntennaV, gridBagConstraints);

    jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabel4.setText("Ant V");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.weighty = 0.1;
    jPanelAntVoltage.add(jLabel4, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    jPanelDisplay.add(jPanelAntVoltage, gridBagConstraints);

    jPanelRefVoltage.setLayout(new java.awt.GridBagLayout());

    jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabel1.setText("Ref V");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.weighty = 0.1;
    jPanelRefVoltage.add(jLabel1, gridBagConstraints);

    jProgressBarRefV.setOrientation(1);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelRefVoltage.add(jProgressBarRefV, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    jPanelDisplay.add(jPanelRefVoltage, gridBagConstraints);

    getContentPane().add(jPanelDisplay);

    jPanelMiscButtons.setLayout(new java.awt.GridBagLayout());

    buttonGroupMode.add(jToggleButtonSsb);
    jToggleButtonSsb.setText("SSB");
    jToggleButtonSsb.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonSsbItemStateChanged(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelMiscButtons.add(jToggleButtonSsb, gridBagConstraints);

    buttonGroupMode.add(jToggleButtonCw);
    jToggleButtonCw.setText("CW");
    jToggleButtonCw.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonCwItemStateChanged(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelMiscButtons.add(jToggleButtonCw, gridBagConstraints);

    jPanelSliderControls.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Tune Relays", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12))); // NOI18N
    jPanelSliderControls.setLayout(new java.awt.GridBagLayout());

    jSliderC1.setMaximum(TunerController.C1_MAX);
    jSliderC1.setPaintLabels(true);
    jSliderC1.setToolTipText("C");
    jSliderC1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    jSliderC1.addChangeListener(new javax.swing.event.ChangeListener()
    {
      public void stateChanged(javax.swing.event.ChangeEvent evt)
      {
        jSliderC1StateChanged(evt);
      }
    });
    jSliderC1.addKeyListener(new java.awt.event.KeyAdapter()
    {
      public void keyPressed(java.awt.event.KeyEvent evt)
      {
        jSliderC1KeyPressed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelSliderControls.add(jSliderC1, gridBagConstraints);

    jSliderL.setMaximum(TunerController.L_MAX);
    jSliderL.setToolTipText("L");
    jSliderL.addChangeListener(new javax.swing.event.ChangeListener()
    {
      public void stateChanged(javax.swing.event.ChangeEvent evt)
      {
        jSliderLStateChanged(evt);
      }
    });
    jSliderL.addKeyListener(new java.awt.event.KeyAdapter()
    {
      public void keyPressed(java.awt.event.KeyEvent evt)
      {
        jSliderLKeyPressed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelSliderControls.add(jSliderL, gridBagConstraints);

    jToggleButtonN.setText("C_TX");
    jToggleButtonN.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonNItemStateChanged(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridheight = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    jPanelSliderControls.add(jToggleButtonN, gridBagConstraints);

    jLabel2.setText("L");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.01;
    gridBagConstraints.weighty = 1.0;
    jPanelSliderControls.add(jLabel2, gridBagConstraints);

    jLabel5.setText("C");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.01;
    gridBagConstraints.weighty = 1.0;
    jPanelSliderControls.add(jLabel5, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelMiscButtons.add(jPanelSliderControls, gridBagConstraints);

    jButton3.setText("TuneBox");
    jButton3.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jButton3ActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelMiscButtons.add(jButton3, gridBagConstraints);

    jToggleButtonTune.setText("Tune");
    jToggleButtonTune.setToolTipText("");
    jToggleButtonTune.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonTuneItemStateChanged(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelMiscButtons.add(jToggleButtonTune, gridBagConstraints);

    jToggleButtonAuto.setText("AUTO");
    jToggleButtonAuto.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonAutoItemStateChanged(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelMiscButtons.add(jToggleButtonAuto, gridBagConstraints);

    getContentPane().add(jPanelMiscButtons);

    jPanelBands.setLayout(new java.awt.GridLayout(3, 3));

    buttonGroupBand.add(jToggleButtonBand1);
    jToggleButtonBand1.setText("1.8");
    jToggleButtonBand1.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonBand1ItemStateChanged(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand1);

    buttonGroupBand.add(jToggleButtonBand2);
    jToggleButtonBand2.setText("3.5");
    jToggleButtonBand2.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonBand2ItemStateChanged(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand2);

    buttonGroupBand.add(jToggleButtonBand3);
    jToggleButtonBand3.setSelected(true);
    jToggleButtonBand3.setText("7");
    jToggleButtonBand3.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonBand3ItemStateChanged(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand3);

    buttonGroupBand.add(jToggleButtonBand4);
    jToggleButtonBand4.setText("10");
    jToggleButtonBand4.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonBand4ItemStateChanged(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand4);

    buttonGroupBand.add(jToggleButtonBand5);
    jToggleButtonBand5.setText("14");
    jToggleButtonBand5.setToolTipText("");
    jToggleButtonBand5.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonBand5ItemStateChanged(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand5);

    buttonGroupBand.add(jToggleButtonBand6);
    jToggleButtonBand6.setText("18");
    jToggleButtonBand6.setToolTipText("");
    jToggleButtonBand6.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonBand6ItemStateChanged(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand6);

    buttonGroupBand.add(jToggleButtonBand7);
    jToggleButtonBand7.setText("21");
    jToggleButtonBand7.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonBand7ItemStateChanged(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand7);

    buttonGroupBand.add(jToggleButtonBand8);
    jToggleButtonBand8.setText("24");
    jToggleButtonBand8.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonBand8ItemStateChanged(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand8);

    buttonGroupBand.add(jToggleButtonBand9);
    jToggleButtonBand9.setText("28");
    jToggleButtonBand9.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonBand9ItemStateChanged(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand9);

    getContentPane().add(jPanelBands);

    jPanelAntennas.setLayout(new java.awt.GridLayout(3, 2));

    buttonGroupAnt.add(jToggleButtonAnt1);
    jToggleButtonAnt1.setText("ant 1");
    jToggleButtonAnt1.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonAnt1ItemStateChanged(evt);
      }
    });
    jPanelAntennas.add(jToggleButtonAnt1);

    buttonGroupAnt.add(jToggleButtonAnt2);
    jToggleButtonAnt2.setText("ant 2");
    jToggleButtonAnt2.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonAnt2ItemStateChanged(evt);
      }
    });
    jPanelAntennas.add(jToggleButtonAnt2);

    buttonGroupAnt.add(jToggleButtonAnt3);
    jToggleButtonAnt3.setText("ant 3");
    jToggleButtonAnt3.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonAnt3ItemStateChanged(evt);
      }
    });
    jPanelAntennas.add(jToggleButtonAnt3);

    buttonGroupAnt.add(jToggleButtonAnt4);
    jToggleButtonAnt4.setText("ant 4");
    jToggleButtonAnt4.setEnabled(false);
    jToggleButtonAnt4.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonAnt4ItemStateChanged(evt);
      }
    });
    jPanelAntennas.add(jToggleButtonAnt4);

    buttonGroupAnt.add(jToggleButtonAnt5);
    jToggleButtonAnt5.setText("ant 5");
    jToggleButtonAnt5.setEnabled(false);
    jToggleButtonAnt5.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonAnt5ItemStateChanged(evt);
      }
    });
    jPanelAntennas.add(jToggleButtonAnt5);

    buttonGroupAnt.add(jToggleButtonAnt6);
    jToggleButtonAnt6.setText("ant 6");
    jToggleButtonAnt6.setEnabled(false);
    jToggleButtonAnt6.addItemListener(new java.awt.event.ItemListener()
    {
      public void itemStateChanged(java.awt.event.ItemEvent evt)
      {
        jToggleButtonAnt6ItemStateChanged(evt);
      }
    });
    jPanelAntennas.add(jToggleButtonAnt6);

    getContentPane().add(jPanelAntennas);

    jMenu1.setText("File");
    jMenuBar1.add(jMenu1);

    jMenu2.setText("Edit");
    jMenuBar1.add(jMenu2);

    setJMenuBar(jMenuBar1);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
  {//GEN-HEADEREND:event_formWindowClosing
    // If not maximized...
    if(this.getExtendedState() != MAXIMIZED_BOTH)
    {
      applicationSettings.setMainWindowDimensions(this.getBounds()); // save the dimensions of the JFrame
    }
    
    applicationSettings.SaveSettingsToDisk();
    atuSerialPortSettings.SaveSettingsToDisk();
    radioSerialPortSettings.SaveSettingsToDisk();
    tuneSettings.save();
  }//GEN-LAST:event_formWindowClosing

  private void formWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowOpened
  {//GEN-HEADEREND:event_formWindowOpened
    initMainWindow();
  }//GEN-LAST:event_formWindowOpened

  private void jSliderC1StateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jSliderC1StateChanged
  {//GEN-HEADEREND:event_jSliderC1StateChanged
    JSlider source = (JSlider) evt.getSource();
    if(source.getValueIsAdjusting())
      return;
    
    TuneValue tune = getCurrentTune();
    
    tune.setC(jSliderC1.getValue());     // Update the Tune with the new value
    tunerController.setC1(tune.getC());  // Tell the Tuner to set relays
    updateTuneBoxText(applicationSettings.getCurrentTuneSelection(), tune);
  }//GEN-LAST:event_jSliderC1StateChanged

  private void jSliderLStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jSliderLStateChanged
  {//GEN-HEADEREND:event_jSliderLStateChanged
    JSlider source = (JSlider) evt.getSource();
    if(source.getValueIsAdjusting())
      return;
 
    TuneValue tune = getCurrentTune();
    
    tune.setL(jSliderL.getValue());    // Update the Tune with the new value
    tunerController.setL(tune.getL());  // Tell the Tuner to set relays
    updateTuneBoxText(applicationSettings.getCurrentTuneSelection(), tune);
  }//GEN-LAST:event_jSliderLStateChanged

  private void jButton3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton3ActionPerformed
  {//GEN-HEADEREND:event_jButton3ActionPerformed
   jDialogTuneBox.setVisible(!jDialogTuneBox.isVisible());
  }//GEN-LAST:event_jButton3ActionPerformed

  private void jButton4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton4ActionPerformed
  {//GEN-HEADEREND:event_jButton4ActionPerformed
    jDialogTuneBox.setVisible(false);
  }//GEN-LAST:event_jButton4ActionPerformed

  private void jDialogTuneBoxWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_jDialogTuneBoxWindowOpened
  {//GEN-HEADEREND:event_jDialogTuneBoxWindowOpened
    jDialogTuneBox.setBounds(applicationSettings.getTuneBoxDimensions());
    updateTuneBoxValues();
  }//GEN-LAST:event_jDialogTuneBoxWindowOpened

  private void jDialogTuneBoxWindowDeactivated(java.awt.event.WindowEvent evt)//GEN-FIRST:event_jDialogTuneBoxWindowDeactivated
  {//GEN-HEADEREND:event_jDialogTuneBoxWindowDeactivated
   applicationSettings.setTuneBoxDimensions(jDialogTuneBox.getBounds());
  }//GEN-LAST:event_jDialogTuneBoxWindowDeactivated

  private void jSliderC1KeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jSliderC1KeyPressed
  {//GEN-HEADEREND:event_jSliderC1KeyPressed
   
    switch(evt.getKeyCode())
    {
      case KeyEvent.VK_DOWN:
      case KeyEvent.VK_UP:
         // UP/DOWN moves focus to next slider
        jSliderL.requestFocusInWindow();
        evt.consume();
        break;
        
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_PAGE_UP:
      case KeyEvent.VK_PAGE_DOWN:
        // We we are still waiting for the previous setRelay() command to be exectuded do nothing.
        if(!tunerController.isReadyToAcceptCommand())
        {
          evt.consume();
          System.out.println("-");
        }
        else
        {
          System.out.println("+");
        }
        break;  
    }
  }//GEN-LAST:event_jSliderC1KeyPressed

  private void jSliderLKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jSliderLKeyPressed
  {//GEN-HEADEREND:event_jSliderLKeyPressed
    switch(evt.getKeyCode())
    {
      case KeyEvent.VK_DOWN:
      case KeyEvent.VK_UP:
         // UP/DOWN moves focus to next slider
        jSliderC1.requestFocusInWindow();
        evt.consume();
        break;
        
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_PAGE_UP:
      case KeyEvent.VK_PAGE_DOWN:
        // We we are still waiting for the previous setRelay() command to be exectuded do nothing.
        if(!tunerController.isReadyToAcceptCommand())
        {
          evt.consume();
          System.out.println("-");
        }
        else
        {
          System.out.println("+");
        }
        break;  
    }
  }//GEN-LAST:event_jSliderLKeyPressed

  private void jToggleButtonBand1ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonBand1ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonBand1ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      onBandButtonPress(0);
    }
  }//GEN-LAST:event_jToggleButtonBand1ItemStateChanged

  private void jToggleButtonBand2ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonBand2ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonBand2ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      onBandButtonPress(1);
    }
  }//GEN-LAST:event_jToggleButtonBand2ItemStateChanged

  private void jToggleButtonBand4ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonBand4ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonBand4ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      onBandButtonPress(3);
    }
  }//GEN-LAST:event_jToggleButtonBand4ItemStateChanged

  private void jToggleButtonBand3ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonBand3ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonBand3ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      onBandButtonPress(2);
    }
  }//GEN-LAST:event_jToggleButtonBand3ItemStateChanged

  private void jToggleButtonBand5ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonBand5ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonBand5ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      onBandButtonPress(4);
    }
  }//GEN-LAST:event_jToggleButtonBand5ItemStateChanged

  private void jToggleButtonBand6ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonBand6ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonBand6ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      onBandButtonPress(5);
    }
  }//GEN-LAST:event_jToggleButtonBand6ItemStateChanged

  private void jToggleButtonBand7ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonBand7ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonBand7ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      onBandButtonPress(6);
    }
  }//GEN-LAST:event_jToggleButtonBand7ItemStateChanged

  private void jToggleButtonBand8ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonBand8ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonBand8ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      onBandButtonPress(7);
    }
  }//GEN-LAST:event_jToggleButtonBand8ItemStateChanged

  private void jToggleButtonBand9ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonBand9ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonBand9ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      onBandButtonPress(8);
    }
  }//GEN-LAST:event_jToggleButtonBand9ItemStateChanged

  private void jToggleButtonSsbItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonSsbItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonSsbItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      onModeButtonPress(0);
    }
  }//GEN-LAST:event_jToggleButtonSsbItemStateChanged

  private void jToggleButtonCwItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonCwItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonCwItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      onModeButtonPress(1);
    }
  }//GEN-LAST:event_jToggleButtonCwItemStateChanged

  private void jToggleButtonAnt1ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonAnt1ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonAnt1ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
       onAntennaButtonPress(0);
    }
  }//GEN-LAST:event_jToggleButtonAnt1ItemStateChanged

  private void jToggleButtonAnt2ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonAnt2ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonAnt2ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
       onAntennaButtonPress(1);
    }
  }//GEN-LAST:event_jToggleButtonAnt2ItemStateChanged

  private void jToggleButtonAnt3ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonAnt3ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonAnt3ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
       onAntennaButtonPress(2);
    }
  }//GEN-LAST:event_jToggleButtonAnt3ItemStateChanged

  private void jToggleButtonAnt4ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonAnt4ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonAnt4ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
       onAntennaButtonPress(3);
    }
  }//GEN-LAST:event_jToggleButtonAnt4ItemStateChanged

  private void jToggleButtonAnt5ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonAnt5ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonAnt5ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
       onAntennaButtonPress(4);
    }
  }//GEN-LAST:event_jToggleButtonAnt5ItemStateChanged

  private void jToggleButtonAnt6ItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonAnt6ItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonAnt6ItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
       onAntennaButtonPress(5);
    }
  }//GEN-LAST:event_jToggleButtonAnt6ItemStateChanged

  private void jToggleButtonTuneItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonTuneItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonTuneItemStateChanged
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      java.awt.EventQueue.invokeLater(new Runnable()
      {
        public void run()
        {
          Object[] options = {"Yes", "No"};
          int dialogResult = JOptionPane.showOptionDialog(null,
                                                          "Did you set PA to STBY ?!?",
                                                          "Warning !!!",
                                                          JOptionPane.YES_NO_OPTION,
                                                          JOptionPane.WARNING_MESSAGE,
                                                          null,
                                                          options,
                                                          options[1]);

          if(dialogResult == JOptionPane.NO_OPTION || dialogResult == JOptionPane.CLOSED_OPTION)
          {
            jToggleButtonTune.setSelected(false);
            return;
          }

          boolean ret = tunerController.enableTuneMode();
          if(!ret) // unable to send command to the Tuner
          {
            jToggleButtonTune.setSelected(false);
            return;
          }

          // Set the transmitter into: FM; Low power; Transmit mode
          radioController.setMode(RadioModes.FM);
          radioController.setTxPower(applicationSettings.getRadioTunePower());
          radioController.setMicPtt(true);
        }
      });
    }
    else if(evt.getStateChange() == ItemEvent.DESELECTED)  
    {
      java.awt.EventQueue.invokeLater(new Runnable()
      {
        public void run()
        {
          System.out.println("DESELECTED");
          boolean ret = tunerController.disableTuneMode();
          if(!ret) // unable to send command to the Tuner
          {
            jToggleButtonTune.setSelected(true);
            return;
          }

          // Set the transmitter into: FM; Low power; Transmit mode
          changeRadioMode(applicationSettings.getCurrentModeSelection());
          radioController.setTxPower(applicationSettings.getRadioAttackPower());
          radioController.setMicPtt(false);
        }
      });
    }
  }//GEN-LAST:event_jToggleButtonTuneItemStateChanged

  private void jToggleButtonAutoItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonAutoItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonAutoItemStateChanged
//    if(evt.getStateChange() == ItemEvent.SELECTED)
//    {   
//      initAutoTuneMode();
//     
//      if(selectNextValidTuneBoxButton() == false)
//      {
//        clearAutoTuneMode();
//      }
//    }
//    else if(evt.getStateChange() == ItemEvent.DESELECTED)
//    {
//      clearAutoTuneMode();
//    }
  }//GEN-LAST:event_jToggleButtonAutoItemStateChanged

  private void jToggleButtonNItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jToggleButtonNItemStateChanged
  {//GEN-HEADEREND:event_jToggleButtonNItemStateChanged
    boolean isSelected;
    
    if(evt.getStateChange() == ItemEvent.SELECTED)
    {
      isSelected = true;
      jToggleButtonN.setText("C_ANT");
    }
    else
    {
      isSelected = false;
      jToggleButtonN.setText("C_TX");
    }
    
    TuneValue tune = getCurrentTune();
    
    tune.setIsC2Selected(isSelected);             // Update the Tune with the new value
    tunerController.setIsC2Selected(isSelected);  // Tell the Tuner to set relays
    updateTuneBoxText(applicationSettings.getCurrentTuneSelection(), tune);
  }//GEN-LAST:event_jToggleButtonNItemStateChanged

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
    }
    catch(ClassNotFoundException ex)
    {
      java.util.logging.Logger.getLogger(AtuApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
    catch(InstantiationException ex)
    {
      java.util.logging.Logger.getLogger(AtuApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
    catch(IllegalAccessException ex)
    {
      java.util.logging.Logger.getLogger(AtuApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
    catch(javax.swing.UnsupportedLookAndFeelException ex)
    {
      java.util.logging.Logger.getLogger(AtuApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
    //</editor-fold>
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable()
    {
      public void run()
      {
        new AtuApplication().setVisible(true);
      }
    });
  }


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.ButtonGroup buttonGroupAnt;
  private javax.swing.ButtonGroup buttonGroupBand;
  private javax.swing.ButtonGroup buttonGroupMode;
  private javax.swing.ButtonGroup buttonGroupTuneBox;
  private javax.swing.JButton jButton3;
  private javax.swing.JButton jButton4;
  private javax.swing.JDialog jDialogTuneBox;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JMenu jMenu1;
  private javax.swing.JMenu jMenu2;
  private javax.swing.JMenuBar jMenuBar1;
  private javax.swing.JPanel jPanelAntVoltage;
  private javax.swing.JPanel jPanelAntennas;
  private javax.swing.JPanel jPanelBands;
  private javax.swing.JPanel jPanelDisplay;
  private javax.swing.JPanel jPanelMiscButtons;
  private javax.swing.JPanel jPanelRefVoltage;
  private javax.swing.JPanel jPanelSliderControls;
  private javax.swing.JPanel jPanelSwr;
  private javax.swing.JPanel jPanelTuneBox;
  private javax.swing.JProgressBar jProgressBarAntennaV;
  private javax.swing.JProgressBar jProgressBarRefV;
  private javax.swing.JProgressBar jProgressBarSwr;
  private javax.swing.JSlider jSliderC1;
  private javax.swing.JSlider jSliderL;
  private javax.swing.JToggleButton jToggleButtonAnt1;
  private javax.swing.JToggleButton jToggleButtonAnt2;
  private javax.swing.JToggleButton jToggleButtonAnt3;
  private javax.swing.JToggleButton jToggleButtonAnt4;
  private javax.swing.JToggleButton jToggleButtonAnt5;
  private javax.swing.JToggleButton jToggleButtonAnt6;
  private javax.swing.JToggleButton jToggleButtonAuto;
  private javax.swing.JToggleButton jToggleButtonBand1;
  private javax.swing.JToggleButton jToggleButtonBand2;
  private javax.swing.JToggleButton jToggleButtonBand3;
  private javax.swing.JToggleButton jToggleButtonBand4;
  private javax.swing.JToggleButton jToggleButtonBand5;
  private javax.swing.JToggleButton jToggleButtonBand6;
  private javax.swing.JToggleButton jToggleButtonBand7;
  private javax.swing.JToggleButton jToggleButtonBand8;
  private javax.swing.JToggleButton jToggleButtonBand9;
  private javax.swing.JToggleButton jToggleButtonCw;
  private javax.swing.JToggleButton jToggleButtonN;
  private javax.swing.JToggleButton jToggleButtonSsb;
  private javax.swing.JToggleButton jToggleButtonTune;
  // End of variables declaration//GEN-END:variables
}
