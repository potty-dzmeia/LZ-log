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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;
import static javax.swing.plaf.basic.BasicSliderUI.NEGATIVE_SCROLL;
import static javax.swing.plaf.basic.BasicSliderUI.POSITIVE_SCROLL;
import javax.swing.plaf.synth.SynthSliderUI;
import org.lz1aq.tuner.TunerController;

/**
 *
 * @author levkov_cha_ext
 */
public class AtuApplication extends javax.swing.JFrame
{
  private final AtuApplicationSettings applicationSettings;
  private final TunerController tunerController;
  
  private final TuneSettings tuneSettins;
  private JToggleButton[] bandButtons;
  private JToggleButton[] antennaButtons;
  private JToggleButton[] modeButtons;
  private List<JToggleButton>   tuneBoxButtons; 
  private List<JSlider>  sliderButtons = new ArrayList<>(AtuApplicationSettings.NUMBER_OF_SLIDER_BUTTONS);        
  /**
   * Creates new form AtuApp
   */
  public AtuApplication()
  {
    // Load user settings for the application from a properties file
    applicationSettings = new AtuApplicationSettings();
    // Load tune settings from a file
    tuneSettins = new TuneSettings(AtuApplicationSettings.NUMBER_OF_BAND_BUTTONS,
                                   AtuApplicationSettings.NUMBER_OF_ANT_BUTTONS,
                                   AtuApplicationSettings.NUMBER_OF_MODE_BUTTONS,
                                   AtuApplicationSettings.NUMBER_OF_TUNE_VALUES);
    
    tunerController = new TunerController(applicationSettings.getComPortAtu(), applicationSettings.getBaudRateAtu());
            
    initComponents();
    
    jSliderC1.setUI(new MySliderUI(jSliderC1));
    jSliderL.setUI(new MySliderUI(jSliderL));
    
    // Add buttons to the TuneBox dialog
    populateTuneBox();
    
    packButtonsIntoStructure();
  }
  
  
  /**
   * Initialize the controls of the main windows
   */
  private void initMainWindow()
  {
    setAntennaButtonsLabels();

    // Read last used JFrame dimensions and restore it
    if(applicationSettings.getMainWindowDimensions().isEmpty() == false)
    {
      this.setBounds(applicationSettings.getMainWindowDimensions());
    }

    //
    jToggleButtonBand1.setSelected(true);
    jToggleButtonAnt1.setSelected(true);
    jToggleButtonSsb.setSelected(true);

    //
    updateSliders();
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
    //sliderButtons.add(jSliderL);
  }

  private void updateSliders()
  {
    // Get the tune for the current combination of band, ant and mode
    TuneValue tune = tuneSettins.get(applicationSettings.getCurrentBandSelection(),
            applicationSettings.getCurrentAntSelection(),
            applicationSettings.getCurrentModeSelection(),
            applicationSettings.getCurrentTuneSelection());

    // update slider values to represent the current tune
    jSliderC1.setValue(tune.getC1());
    jSliderL.setValue(tune.getL());
    //jSliderL.setValue(tune.getL());

    sendTune();
  }

  void sendTune()
  {
    jProgressBar1.setValue(50);
  }

  private void onBandButtonPress(int bandButton)
  {
    applicationSettings.setCurrentBandSelection(bandButton);
    updateSliders();
    updateTuneBoxValues();
  }

  private void onAntennaButtonPress(int antennaButton)
  {
    applicationSettings.setCurrentAntSelection(antennaButton);
    updateSliders();
    updateTuneBoxValues();
  }

  private void onModeButtonPress(int modeButton)
  {
    applicationSettings.setCurrentModeSelection(modeButton);
    updateSliders();
    updateTuneBoxValues();
  }
  
  private void onSliderButtonPress(int index)
  {
    System.out.println("onSliderButtonPress()\n");
    TuneValue tune = tuneSettins.get(applicationSettings.getCurrentBandSelection(),
                                     applicationSettings.getCurrentAntSelection(),
                                     applicationSettings.getCurrentModeSelection(),
                                     applicationSettings.getCurrentTuneSelection());
    
    switch(index)
    {
      case 0:
        tune.setC1(jSliderC1.getValue());
        break;
      case 1:
        tune.setL(jSliderL.getValue());
        break;
//      case 2:
//        tune.setL(jSliderL.getValue());
//        break;
    }
   
    updateTuneBoxValues();
            
    sendTune();
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
          int index = tuneBoxButtons.indexOf(button);
          applicationSettings.setCurrentTuneSelection(index);
          updateSliders();
        }
      });
    }
  }
  
  
  private void updateTuneBoxValues()
  {
    System.out.println("updateTuneBoxValues()\n");
    TuneValue tune;
    for(int i=0; i<tuneBoxButtons.size(); i++)
    {
      tune = tuneSettins.get(applicationSettings.getCurrentBandSelection(), 
                             applicationSettings.getCurrentAntSelection(), 
                             applicationSettings.getCurrentModeSelection(), 
                             i);
      
      tuneBoxButtons.get(i).setText("c1="+tune.getC1()+"  l="+tune.getL());  
    }
    
    // Highlight the current selection
    tuneBoxButtons.get(applicationSettings.getCurrentTuneSelection()).setSelected(true);
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
    jProgressBar1 = new javax.swing.JProgressBar();
    jLabel3 = new javax.swing.JLabel();
    jPanelVoltage = new javax.swing.JPanel();
    jProgressBar2 = new javax.swing.JProgressBar();
    jLabel4 = new javax.swing.JLabel();
    jPanelMiscButtons = new javax.swing.JPanel();
    jToggleButtonSsb = new javax.swing.JToggleButton();
    jToggleButtonCw = new javax.swing.JToggleButton();
    jPanelSliderControls = new javax.swing.JPanel();
    jSliderC1 = new javax.swing.JSlider();
    jSliderL = new javax.swing.JSlider();
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

    jProgressBar1.setOrientation(1);
    jProgressBar1.setToolTipText("");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelSwr.add(jProgressBar1, gridBagConstraints);

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
    jPanelDisplay.add(jPanelSwr, gridBagConstraints);

    jPanelVoltage.setLayout(new java.awt.GridBagLayout());

    jProgressBar2.setOrientation(1);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelVoltage.add(jProgressBar2, gridBagConstraints);

    jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabel4.setText("Voltage");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.weighty = 0.1;
    jPanelVoltage.add(jLabel4, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelDisplay.add(jPanelVoltage, gridBagConstraints);

    getContentPane().add(jPanelDisplay);

    jPanelMiscButtons.setLayout(new java.awt.GridBagLayout());

    buttonGroupMode.add(jToggleButtonSsb);
    jToggleButtonSsb.setText("SSB");
    jToggleButtonSsb.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonSsbActionPerformed(evt);
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
    jToggleButtonCw.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonCwActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelMiscButtons.add(jToggleButtonCw, gridBagConstraints);

    jPanelSliderControls.setLayout(new java.awt.GridLayout(2, 1));

    jSliderC1.setMaximum(TunerController.C1_MAX);
    jSliderC1.setPaintLabels(true);
    jSliderC1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    jSliderC1.addChangeListener(new javax.swing.event.ChangeListener()
    {
      public void stateChanged(javax.swing.event.ChangeEvent evt)
      {
        jSliderC1StateChanged(evt);
      }
    });
    jPanelSliderControls.add(jSliderC1);

    jSliderL.setMaximum(TunerController.L_MAX);
    jSliderL.addChangeListener(new javax.swing.event.ChangeListener()
    {
      public void stateChanged(javax.swing.event.ChangeEvent evt)
      {
        jSliderLStateChanged(evt);
      }
    });
    jSliderL.addKeyListener(new java.awt.event.KeyAdapter()
    {
      public void keyReleased(java.awt.event.KeyEvent evt)
      {
        jSliderLKeyReleased(evt);
      }
    });
    jPanelSliderControls.add(jSliderL);

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
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    jPanelMiscButtons.add(jToggleButtonTune, gridBagConstraints);

    jToggleButtonAuto.setText("AUTO");
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
    jToggleButtonBand1.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonBand1ActionPerformed(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand1);

    buttonGroupBand.add(jToggleButtonBand2);
    jToggleButtonBand2.setText("3.5");
    jToggleButtonBand2.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonBand2ActionPerformed(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand2);

    buttonGroupBand.add(jToggleButtonBand3);
    jToggleButtonBand3.setSelected(true);
    jToggleButtonBand3.setText("7");
    jToggleButtonBand3.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonBand3ActionPerformed(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand3);

    buttonGroupBand.add(jToggleButtonBand4);
    jToggleButtonBand4.setText("10");
    jToggleButtonBand4.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonBand4ActionPerformed(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand4);

    buttonGroupBand.add(jToggleButtonBand5);
    jToggleButtonBand5.setText("14");
    jToggleButtonBand5.setToolTipText("");
    jToggleButtonBand5.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonBand5ActionPerformed(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand5);

    buttonGroupBand.add(jToggleButtonBand6);
    jToggleButtonBand6.setText("18");
    jToggleButtonBand6.setToolTipText("");
    jToggleButtonBand6.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonBand6ActionPerformed(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand6);

    buttonGroupBand.add(jToggleButtonBand7);
    jToggleButtonBand7.setText("21");
    jToggleButtonBand7.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonBand7ActionPerformed(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand7);

    buttonGroupBand.add(jToggleButtonBand8);
    jToggleButtonBand8.setText("24");
    jToggleButtonBand8.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonBand8ActionPerformed(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand8);

    buttonGroupBand.add(jToggleButtonBand9);
    jToggleButtonBand9.setText("28");
    jToggleButtonBand9.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonBand9ActionPerformed(evt);
      }
    });
    jPanelBands.add(jToggleButtonBand9);

    getContentPane().add(jPanelBands);

    jPanelAntennas.setLayout(new java.awt.GridLayout(3, 2));

    buttonGroupAnt.add(jToggleButtonAnt1);
    jToggleButtonAnt1.setText("ant 1");
    jToggleButtonAnt1.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonAnt1ActionPerformed(evt);
      }
    });
    jPanelAntennas.add(jToggleButtonAnt1);

    buttonGroupAnt.add(jToggleButtonAnt2);
    jToggleButtonAnt2.setText("ant 4");
    jToggleButtonAnt2.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonAnt2ActionPerformed(evt);
      }
    });
    jPanelAntennas.add(jToggleButtonAnt2);

    buttonGroupAnt.add(jToggleButtonAnt3);
    jToggleButtonAnt3.setText("ant 2");
    jToggleButtonAnt3.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonAnt3ActionPerformed(evt);
      }
    });
    jPanelAntennas.add(jToggleButtonAnt3);

    buttonGroupAnt.add(jToggleButtonAnt4);
    jToggleButtonAnt4.setText("ant 5");
    jToggleButtonAnt4.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonAnt4ActionPerformed(evt);
      }
    });
    jPanelAntennas.add(jToggleButtonAnt4);

    buttonGroupAnt.add(jToggleButtonAnt5);
    jToggleButtonAnt5.setText("ant 3");
    jToggleButtonAnt5.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonAnt5ActionPerformed(evt);
      }
    });
    jPanelAntennas.add(jToggleButtonAnt5);

    buttonGroupAnt.add(jToggleButtonAnt6);
    jToggleButtonAnt6.setText("ant 6");
    jToggleButtonAnt6.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        jToggleButtonAnt6ActionPerformed(evt);
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

    private void jToggleButtonBand1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonBand1ActionPerformed
      onBandButtonPress(0);
    }//GEN-LAST:event_jToggleButtonBand1ActionPerformed

  private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
  {//GEN-HEADEREND:event_formWindowClosing
    // If not maximized...
    if(this.getExtendedState() != MAXIMIZED_BOTH)
    {
      applicationSettings.setMainWindowDimensions(this.getBounds()); // save the dimensions of the JFrame
    }
    
    applicationSettings.SaveSettingsToDisk();
    tuneSettins.save();
  }//GEN-LAST:event_formWindowClosing

  private void formWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowOpened
  {//GEN-HEADEREND:event_formWindowOpened
    initMainWindow();
  }//GEN-LAST:event_formWindowOpened

  private void jToggleButtonBand2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonBand2ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonBand2ActionPerformed
    onBandButtonPress(1);
  }//GEN-LAST:event_jToggleButtonBand2ActionPerformed

  private void jToggleButtonBand4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonBand4ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonBand4ActionPerformed
    onBandButtonPress(3);
  }//GEN-LAST:event_jToggleButtonBand4ActionPerformed

  private void jToggleButtonBand5ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonBand5ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonBand5ActionPerformed
    onBandButtonPress(4);
  }//GEN-LAST:event_jToggleButtonBand5ActionPerformed

  private void jToggleButtonBand3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonBand3ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonBand3ActionPerformed
    onBandButtonPress(2);
  }//GEN-LAST:event_jToggleButtonBand3ActionPerformed

  private void jToggleButtonBand6ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonBand6ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonBand6ActionPerformed
    onBandButtonPress(5);
  }//GEN-LAST:event_jToggleButtonBand6ActionPerformed

  private void jToggleButtonBand7ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonBand7ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonBand7ActionPerformed
    onBandButtonPress(6);
  }//GEN-LAST:event_jToggleButtonBand7ActionPerformed

  private void jToggleButtonBand8ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonBand8ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonBand8ActionPerformed
    onBandButtonPress(7);
  }//GEN-LAST:event_jToggleButtonBand8ActionPerformed

  private void jToggleButtonBand9ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonBand9ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonBand9ActionPerformed
    onBandButtonPress(8);
  }//GEN-LAST:event_jToggleButtonBand9ActionPerformed

  private void jToggleButtonAnt1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonAnt1ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonAnt1ActionPerformed
    onAntennaButtonPress(0);
  }//GEN-LAST:event_jToggleButtonAnt1ActionPerformed

  private void jToggleButtonAnt2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonAnt2ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonAnt2ActionPerformed
    onAntennaButtonPress(1);
  }//GEN-LAST:event_jToggleButtonAnt2ActionPerformed

  private void jToggleButtonAnt3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonAnt3ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonAnt3ActionPerformed
    onAntennaButtonPress(2);
  }//GEN-LAST:event_jToggleButtonAnt3ActionPerformed

  private void jToggleButtonAnt4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonAnt4ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonAnt4ActionPerformed
    onAntennaButtonPress(3);
  }//GEN-LAST:event_jToggleButtonAnt4ActionPerformed

  private void jToggleButtonAnt5ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonAnt5ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonAnt5ActionPerformed
    onAntennaButtonPress(4);
  }//GEN-LAST:event_jToggleButtonAnt5ActionPerformed

  private void jToggleButtonAnt6ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonAnt6ActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonAnt6ActionPerformed
    onAntennaButtonPress(5);
  }//GEN-LAST:event_jToggleButtonAnt6ActionPerformed

  private void jToggleButtonSsbActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonSsbActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonSsbActionPerformed
    onModeButtonPress(0);
  }//GEN-LAST:event_jToggleButtonSsbActionPerformed

  private void jToggleButtonCwActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButtonCwActionPerformed
  {//GEN-HEADEREND:event_jToggleButtonCwActionPerformed
    onModeButtonPress(1);
  }//GEN-LAST:event_jToggleButtonCwActionPerformed

  private void jSliderC1StateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jSliderC1StateChanged
  {//GEN-HEADEREND:event_jSliderC1StateChanged
    JSlider source = (JSlider) evt.getSource();
    if(source.getValueIsAdjusting())
      return;
    
    onSliderButtonPress(sliderButtons.lastIndexOf(source));
  }//GEN-LAST:event_jSliderC1StateChanged

  private void jSliderLStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jSliderLStateChanged
  {//GEN-HEADEREND:event_jSliderLStateChanged
    JSlider source = (JSlider) evt.getSource();
    if(source.getValueIsAdjusting())
      return;
      
    onSliderButtonPress(sliderButtons.lastIndexOf(source));
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

  private void jSliderLKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jSliderLKeyReleased
  {//GEN-HEADEREND:event_jSliderLKeyReleased
    if(evt.getKeyCode() == KeyEvent.VK_TAB)
    {
      if(jSliderL.isFocusOwner())
      {
        jSliderC1.requestFocusInWindow();
        evt.consume();
      }
    }
  }//GEN-LAST:event_jSliderLKeyReleased

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
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JMenu jMenu1;
  private javax.swing.JMenu jMenu2;
  private javax.swing.JMenuBar jMenuBar1;
  private javax.swing.JPanel jPanelAntennas;
  private javax.swing.JPanel jPanelBands;
  private javax.swing.JPanel jPanelDisplay;
  private javax.swing.JPanel jPanelMiscButtons;
  private javax.swing.JPanel jPanelSliderControls;
  private javax.swing.JPanel jPanelSwr;
  private javax.swing.JPanel jPanelTuneBox;
  private javax.swing.JPanel jPanelVoltage;
  private javax.swing.JProgressBar jProgressBar1;
  private javax.swing.JProgressBar jProgressBar2;
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
  private javax.swing.JToggleButton jToggleButtonSsb;
  private javax.swing.JToggleButton jToggleButtonTune;
  // End of variables declaration//GEN-END:variables
}
