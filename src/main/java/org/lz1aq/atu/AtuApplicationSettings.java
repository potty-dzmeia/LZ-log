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

import java.awt.Rectangle;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AtuApplicationSettings
{

  static final String SETTINGS_FILE_NAME = "AtuSettings.properties";
  static final String PROPERTY_COMPORT_RADIO = "ComPortRadio";
  static final String PROPERTY_BAUDE_RATE_RADIO = "BaudRateRadio";
  static final String PROPERTY_COMPORT_ATU = "ComPortAtu";
  static final String PROPERTY_BAUDE_RATE_ATU = "BaudRateAtu";
  static final String PROPERTY_LABEL_ANT = "LabelAnt";
  static final String PROPERTY_MAIN_WINDOW_X = "MainWindow_x";
  static final String PROPERTY_MAIN_WINDOW_Y = "MainWindow_y";
  static final String PROPERTY_MAIN_WINDOW_WIDTH = "MainWindow_w";
  static final String PROPERTY_MAIN_WINDOW_HEIGHT = "MainWindow_h"; 
  static final String PROPERTY_TUNEBOX_WINDOW_X = "TuneboxWindow_x";
  static final String PROPERTY_TUNEBOX_WINDOW_Y = "TuneboxWindow_y";
  static final String PROPERTY_TUNEBOX_WINDOW_WIDTH = "TuneboxWindow_w";
  static final String PROPERTY_TUNEBOX_WINDOW_HEIGHT = "TuneboxWindow_h";
  static final String PROPERTY_RADIO_ATTACK_POWER = "radioAttackPower";
  static final String PROPERTY_RADIO_TUNE_POWER = "radioTunePower";
  
  static final int NUMBER_OF_BAND_BUTTONS = 9;
  static final int NUMBER_OF_ANT_BUTTONS = 6;
  static final int NUMBER_OF_MODE_BUTTONS = 2;
  static final int NUMBER_OF_TUNE_VALUES = 10;
  static final int NUMBER_OF_SLIDER_BUTTONS = 3;

  private String comPortRadio;
  private String baudRateRadio;
  private String comPortAtu;
  private String baudRateAtu;
  private final String[] antennaLabels;
  private Rectangle mainWindowDimensions;
  private Rectangle tuneBoxDimensions;
  private final Properties prop;
  private int  radioAttackPower;
  private int  radioTunePower;

  // Values below are not saved in the file
  private int currentBandSelection = 0;
  private int currentAntSelection = 0;
  private int currentModeSelection = 0;
  private int currentTuneSelection = 0;
  private int[][][] lastUsedTuneSelection = new int[NUMBER_OF_BAND_BUTTONS][NUMBER_OF_ANT_BUTTONS][NUMBER_OF_MODE_BUTTONS];  // last used tune selection for given combination of Band, Ant and Mode

  
  // TODO implement support
  public int getRadioAttackPower()
  {
    return radioAttackPower;
  }

  // TODO implement support
  public int getRadioTunePower()
  {
    return radioTunePower;
  }

  
  public int getCurrentBandSelection()
  {
    return currentBandSelection;
  }

  public void setCurrentBandSelection(int currentBandSelection)
  {
    this.currentBandSelection = currentBandSelection;
    // current tune selection should be the last one used for the particular combination of band ant and mode
    currentTuneSelection = lastUsedTuneSelection[currentBandSelection][currentAntSelection][currentModeSelection];
  }

  public int getCurrentAntSelection()
  {
    return currentAntSelection;
  }

  public void setCurrentAntSelection(int currentAntSelection)
  {
    this.currentAntSelection = currentAntSelection;
    // current tune selection should be the last one used for the particular combination of band ant and mode
    currentTuneSelection = lastUsedTuneSelection[currentBandSelection][currentAntSelection][currentModeSelection];
  }

  public int getCurrentModeSelection()
  {
    return currentModeSelection;
  }

  public void setCurrentModeSelection(int currentModeSelection)
  {
    this.currentModeSelection = currentModeSelection;
    // current tune selection should be the last one used for the particular combination of band ant and mode
    currentTuneSelection = lastUsedTuneSelection[currentBandSelection][currentAntSelection][currentModeSelection];
  }

  public int getCurrentTuneSelection()
  {
    return currentTuneSelection;
  }

  public void setCurrentTuneSelection(int currentTuneSelection)
  {
    this.currentTuneSelection = currentTuneSelection;
    lastUsedTuneSelection[currentBandSelection][currentAntSelection][currentModeSelection] = currentTuneSelection;
  }

  /**
   * Tries to read the settings from the disk. If it fails default values are used.
   */
  public AtuApplicationSettings()
  {
    this.prop = new Properties();
    mainWindowDimensions = new Rectangle();
    tuneBoxDimensions = new Rectangle();
    antennaLabels = new String[NUMBER_OF_ANT_BUTTONS];
   
    this.LoadSettingsFromDisk();
  }
  
   public Rectangle getTuneBoxDimensions()
  {
    return tuneBoxDimensions;
  }

  public void setTuneBoxDimensions(Rectangle tuneBoxDimensions)
  {
    this.tuneBoxDimensions = tuneBoxDimensions;
    System.out.println("TuneBox:"+tuneBoxDimensions.toString());
  }
  
  public Rectangle getMainWindowDimensions()
  {
    return mainWindowDimensions;
  }

  public void setMainWindowDimensions(Rectangle jFrameDimensions)
  {
    this.mainWindowDimensions = jFrameDimensions;
  }

  public String getComPortRadio()
  {
    return comPortRadio;
  }

  public void setComPortRadio(String comPort)
  {
    this.comPortRadio = comPort;
  }

  public String getComPortAtu()
  {
    return comPortAtu;
  }

  public void setComPortAtu(String comPort)
  {
    this.comPortAtu = comPort;
  }

  public String getBaudRateRadio()
  {
    return baudRateRadio;
  }

  public void setBaudRateRadio(String baudRate)
  {
    this.baudRateRadio = baudRate;
  }
  
   public String getBaudRateAtu()
  {
    return this.baudRateAtu;
  }

  public void setBaudRateAtu(String baudRate)
  {
    this.baudRateAtu = baudRate;
  }

  /**
   * Get the name for the antenna button
   *
   * @param antennaIndex - Number from 0 to ANTENNA_COUNT.
   * @return The label for the selected antenna
   */
  public String getAntennaLabel(int antennaIndex)
  {
    return antennaLabels[antennaIndex];
  }

  /**
   * Sets he name for the antenna button
   *
   * @param antennaIndex - Number from 0 to ANTENNA_COUNT.
   * @param label - name for the antenna button
   */
  public void setAntennaLabel(int antennaIndex, String label)
  {
    antennaLabels[antennaIndex] = label;
  }

  /**
   * Stores the array of values into properties which are named using key+index of the value
   *
   * @param key - property key that will be used for writing this property
   * @param values - array of values that will be written
   */
  private void setProperties(String key, String[] values)
  {
    for(int i = 0; i < values.length; i++)
    {
      prop.setProperty(key + i, values[i]);
    }
  }

  private void getProperties(String key, String[] values)
  {
    for(int i = 0; i < values.length; i++)
    {
      values[i] = prop.getProperty(key + i);
    }
  }

  /**
   * Saves the settings into a file called "DLineSettings.properties"
   */
  public void SaveSettingsToDisk()
  {
    // Store com port settings
    prop.setProperty(PROPERTY_COMPORT_RADIO, comPortRadio);
    prop.setProperty(PROPERTY_BAUDE_RATE_RADIO, baudRateRadio);
    prop.setProperty(PROPERTY_COMPORT_ATU, comPortAtu);
    prop.setProperty(PROPERTY_BAUDE_RATE_ATU, baudRateAtu);

    // Now save the dimensions:
    prop.setProperty(PROPERTY_MAIN_WINDOW_X, Integer.toString(mainWindowDimensions.x));
    prop.setProperty(PROPERTY_MAIN_WINDOW_Y, Integer.toString(mainWindowDimensions.y));
    prop.setProperty(PROPERTY_MAIN_WINDOW_WIDTH, Integer.toString(mainWindowDimensions.width));
    prop.setProperty(PROPERTY_MAIN_WINDOW_HEIGHT, Integer.toString(mainWindowDimensions.height));
    
    prop.setProperty(PROPERTY_TUNEBOX_WINDOW_X, Integer.toString(tuneBoxDimensions.x));
    prop.setProperty(PROPERTY_TUNEBOX_WINDOW_Y, Integer.toString(tuneBoxDimensions.y));
    prop.setProperty(PROPERTY_TUNEBOX_WINDOW_WIDTH, Integer.toString(tuneBoxDimensions.width));
    prop.setProperty(PROPERTY_TUNEBOX_WINDOW_HEIGHT, Integer.toString(tuneBoxDimensions.height));

    prop.setProperty(PROPERTY_RADIO_ATTACK_POWER, Integer.toString(radioAttackPower));
    prop.setProperty(PROPERTY_RADIO_TUNE_POWER, Integer.toString(radioTunePower));
    
    // Now save the texts for the Direction Buttons
    setProperties(PROPERTY_LABEL_ANT, antennaLabels);

    try
    {
      prop.store(new FileOutputStream(SETTINGS_FILE_NAME), null);
    }
    catch(IOException ex)
    {
      Logger.getLogger(AtuApplicationSettings.class.getName()).log(Level.SEVERE, null, ex);
      System.exit(0);
    }
  }

  /**
   * Loads the settings from a settings file
   */
  public void LoadSettingsFromDisk()
  {
    try
    {

      prop.load(new FileInputStream(SETTINGS_FILE_NAME));

      comPortRadio = prop.getProperty(PROPERTY_COMPORT_RADIO);
      if(comPortRadio == null)
      {
        throwMissingPropertyException(PROPERTY_COMPORT_RADIO);
      }

      baudRateRadio = prop.getProperty(PROPERTY_BAUDE_RATE_RADIO);
      if(baudRateRadio == null)
      {
        throwMissingPropertyException(PROPERTY_BAUDE_RATE_RADIO);
      }

      comPortAtu = prop.getProperty(PROPERTY_COMPORT_ATU);
      if(comPortAtu == null)
      {
        throwMissingPropertyException(PROPERTY_COMPORT_ATU);
      }

      baudRateAtu = prop.getProperty(PROPERTY_BAUDE_RATE_ATU);
      if(baudRateAtu == null)
      {
        throwMissingPropertyException(PROPERTY_BAUDE_RATE_ATU);
      }

      // Read the Main window dimensions:
      int x = Integer.parseInt(prop.getProperty(PROPERTY_MAIN_WINDOW_X));
      int y = Integer.parseInt(prop.getProperty(PROPERTY_MAIN_WINDOW_Y));
      int w = Integer.parseInt(prop.getProperty(PROPERTY_MAIN_WINDOW_WIDTH));
      int h = Integer.parseInt(prop.getProperty(PROPERTY_MAIN_WINDOW_HEIGHT));

      this.mainWindowDimensions = new Rectangle(x, y, w, h);
      
      // Read the TuneBox window dimensions:
      x = Integer.parseInt(prop.getProperty(PROPERTY_TUNEBOX_WINDOW_X));
      y = Integer.parseInt(prop.getProperty(PROPERTY_TUNEBOX_WINDOW_Y));
      w = Integer.parseInt(prop.getProperty(PROPERTY_TUNEBOX_WINDOW_WIDTH));
      h = Integer.parseInt(prop.getProperty(PROPERTY_TUNEBOX_WINDOW_HEIGHT));

      radioAttackPower = Integer.parseInt(prop.getProperty(PROPERTY_RADIO_ATTACK_POWER));
      radioTunePower = Integer.parseInt(prop.getProperty(PROPERTY_RADIO_TUNE_POWER));
      
      this.tuneBoxDimensions = new Rectangle(x, y, w, h);

      // Now read the texts for the Direction Buttons
      getProperties(PROPERTY_LABEL_ANT, antennaLabels);
    }
    catch(IOException ex)
    {
      // If some error we will set to default values
      this.SetSettingsToDefault();
      Logger.getLogger(AtuApplicationSettings.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch(NumberFormatException ex)
    {
      // If some error we will set to default values
      this.SetSettingsToDefault();
      Logger.getLogger(AtuApplicationSettings.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch(Exception ex)
    {
      this.SetSettingsToDefault();
      Logger.getLogger(AtuApplicationSettings.class.getName()).log(Level.SEVERE, null, ex);
    }

  }

  /**
   * Set all settings to default
   */
  private void SetSettingsToDefault()
  {
    comPortRadio = "Com1";
    baudRateRadio = "9600";
    comPortAtu = "Com2";
    baudRateAtu = "9600";

    // We have minimum size so we don't have to worry about the values:
    mainWindowDimensions.height = 0;
    mainWindowDimensions.width = 0;
    mainWindowDimensions.x = 0;
    mainWindowDimensions.y = 0;
    
    tuneBoxDimensions.height = 0;
    tuneBoxDimensions.width = 0;
    tuneBoxDimensions.x = 0;
    tuneBoxDimensions.y = 0;

    radioAttackPower = 10;
    radioTunePower   = 10;
    
    // Set texts for the antenna buttons
    for(int i = 0; i < NUMBER_OF_ANT_BUTTONS; i++)
    {
      antennaLabels[i] = "ant" + (i + 1);
    }
  }

  void throwMissingPropertyException(String propertyName) throws Exception
  {
    throw new Exception("Error when trying to read element " + propertyName + " from file " + SETTINGS_FILE_NAME);
  }
}
