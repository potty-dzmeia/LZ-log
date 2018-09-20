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
  static final String PROPERTY_COMPORT_RADIO = "comPortRadio";
  static final String PROPERTY_BAUDE_RATE_RADIO = "baudRateRadio";
  static final String PROPERTY_COMPORT_ATU = "comPortAtu";
  static final String PROPERTY_BAUDE_RATE_ATU = "baudRateAtu";
  static final String PROPERTY_LABEL_ANT = "labelAnt";
  static final String PROPERTY_MAIN_WINDOW_X = "x";
  static final String PROPERTY_MAIN_WINDOW_Y = "y";
  static final String PROPERTY_MAIN_WINDOW_WIDTH = "w";
  static final String PROPERTY_MAIN_WINDOW_HEIGHT = "h";

  static final int NUMBER_OF_BAND_BUTTONS = 9;
  static final int NUMBER_OF_ANT_BUTTONS = 6;
  static final int NUMBER_OF_MODE_BUTTONS = 2;
  static final int NUMBER_OF_TUNE_VALUES = 10;
  
  static final int SLIDER_C1_MAX = 100;
  static final int SLIDER_C2_MAX = 100;
  static final int SLIDER_L_MAX = 100;

  private String comPortRadio;
  private String baudRateRadio;
  private String comPortAtu;
  private String baudRateAtu;
  private String[] antennaLabels;
  private Rectangle jFrameDimensions;
  private final Properties prop;

  // Values below are not saved in the file
  private int currentBandSelection = 0;
  private int currentAntSelection = 0;
  private int currentModeSelection = 0;
  private int currentTuneSelection = 0;
  private int[][][] lastUsedTuneSelection = new int[NUMBER_OF_BAND_BUTTONS][NUMBER_OF_ANT_BUTTONS][NUMBER_OF_MODE_BUTTONS];  // last used tune selection for given combination of Band, Ant and Mode

  
  
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
    jFrameDimensions = new Rectangle();
    antennaLabels = new String[NUMBER_OF_ANT_BUTTONS];
   
    this.LoadSettingsFromDisk();
  }
  
  public Rectangle getJFrameDimensions()
  {
    return jFrameDimensions;
  }

  public void setJFrameDimensions(Rectangle jFrameDimensions)
  {
    this.jFrameDimensions = jFrameDimensions;
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

  public String getBaudRate()
  {
    return baudRateRadio;
  }

  public void setBaudRate(String baudRate)
  {
    this.baudRateRadio = baudRate;
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

    // Now save the JFrame dimensions:
    prop.setProperty(PROPERTY_MAIN_WINDOW_X, Integer.toString(jFrameDimensions.x));
    prop.setProperty(PROPERTY_MAIN_WINDOW_Y, Integer.toString(jFrameDimensions.y));
    prop.setProperty(PROPERTY_MAIN_WINDOW_WIDTH, Integer.toString(jFrameDimensions.width));
    prop.setProperty(PROPERTY_MAIN_WINDOW_HEIGHT, Integer.toString(jFrameDimensions.height));

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

      // Read the JFrame dimensions:
      int x = Integer.parseInt(prop.getProperty(PROPERTY_MAIN_WINDOW_X));
      int y = Integer.parseInt(prop.getProperty(PROPERTY_MAIN_WINDOW_Y));
      int w = Integer.parseInt(prop.getProperty(PROPERTY_MAIN_WINDOW_WIDTH));
      int h = Integer.parseInt(prop.getProperty(PROPERTY_MAIN_WINDOW_HEIGHT));

      this.jFrameDimensions = new Rectangle(x, y, w, h);

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
    jFrameDimensions.height = 0;
    jFrameDimensions.width = 0;
    jFrameDimensions.x = 0;
    jFrameDimensions.y = 0;

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

  class jSliderC1
  {

    public jSliderC1()
    {
    }
  }
}
