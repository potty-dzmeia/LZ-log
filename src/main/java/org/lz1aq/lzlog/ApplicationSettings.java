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
import java.awt.Font;
import java.awt.Rectangle;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Class used for writing/reading the application settings into a file.
 */
public final class ApplicationSettings
{

  static final String SETTINGS_FILE_NAME = "settings.properties";

  static final String PROPERTY_RADIO_COMPORT = "radio_com_port";
  static final String PROPERTY_RADIO_COMPORT_BAUDRATE = "radio_com_port_baudrate";
  static final String PROPERTY_KEYER_COMPORT = "keyer_com_port";
  //static final String PROPERTY_KEYER_COMPORT_BAUDRATE = "keyer_com_port_baudrate";
  static final String PROPERTY_KEYER_TYPE = "keyer_type";
  static final String PROPERTY_MY_CALL_SIGN = "my_callsign";
  static final String PROPERTY_QUICK_CALLSIGN_MODE = "quick_callsign_mode";
  static final String PROPERTY_DEFAULT_PREFIX = "default_prefix";
  static final String PROPERTY_QSO_REPEAT_PERIOD_SEC = "qso_repeat_period";
  static final String PROPERTY_INCOMING_QSO_MAX_ENTRIES = "incoming_qso_max_entries";
  static final String PROPERTY_INCOMING_QSO_HIDE_AFTER = "incoming_qso_hide_after";
  static final String PROPERTY_FUNCTION_KEYS = "function_keys";
  static final String PROPERTY_FRAMES_DIMENSIONS = "internal_frames_dimensions";
  static final String PROPERTY_BANDMAP_STEP = "bandmap_step_in_hz";
  static final String PROPERTY_BANDMAP_COLUMN_COUNT = "bandmap_column_count";
  static final String PROPERTY_BANDMAP_ROW_COUNT = "bandmap_row_count";
  static final String PROPERTY_FONTS = "fonts";
  static final String PROPERTY_ESM = "ems"; // enter sends message
  static final String PROPERTY_SEND_LEADING_ZERO_AS_T = "leading_zero_as_t";
  static final String PROPERTY_SEND_ZERO_AS_T = "all_zero_as_t";
  static final String PROPERTY_AUTO_CQ_FREQ_JUMP = "auto_cq_freq_jump";
  static final String PROPERTY_BANDMAP_SHOW_FREQ_COLUMNS = "bandmap_show_freq_columns";
  static final String PROPERTY_BANDMAP_AUTO_FREQ = "bandmap_auto_freq";
    
  public static final int FUNCTION_KEYS_COUNT = 12; // The number of function keys
  
  public enum FrameIndex
  {
    JFRAME(0),        
    ENTRY(1),         
    LOG(2),          
    INCOMING_QSO(3),  
    BANDMAP(4), 
    RADIO(5),
    SETTINGS(6);
    
    private final int code;
    FrameIndex(int code)  { this.code = code; }
    public int toInt() { return code; }
  }
  
   public enum FontIndex
  {
    CALLSIGN(0),
    SNT(1),       
    RCV(2),       
    INCOMING_QSO(3),    
    BANDMAP(4), 
    LOG(5);
    
    private final int code;
    FontIndex(int code)  { this.code = code; }
    public int toInt() { return code; }
  }
  

  private String radioComPort;
  private int    radioComPortBaudRate;
  private String keyerComPort;
  //private int    keyerComPortBaudRate;
  private KeyerTypes keyerType;
  private String myCallsign;
  private boolean isQuickCallsignModeEnabled;
  private boolean emsEnabled;
  private boolean autoCqJumpEnabled;
  private boolean isSendLeadingZeroAsT;
  private boolean isSendZeroAsT;
  private String defaultPrefix;
  private int qsoRepeatPeriodInSeconds;
  private final Rectangle[] framesDimensions; // Postition and size of all frames used by the program
  private final Font[] fonts; // Fonts used by the program
  private final String[] functionKeyTexts;  // texts for the function keys
  private int incomingQsoMaxEntries; // How many entries will be shown on the IncomingQsoPanel
  private int incomingQsoHiderAfter; // Specifies due time after which we hide the entry
  private int bandmapStepInHz;
  private int bandmapRowCount;
  private int bandmapColumnCount;
  private boolean isShowBandmapFreqColumnsEnabled;
  private boolean isBandmapAutoFreq;
  
  
  private final Properties prop;

  /**
   * During creation we try to read the settings from the disk. 
   * If it fails default values are used.
   */
  public ApplicationSettings()
  {
    this.prop = new Properties();
    framesDimensions  = new Rectangle[FrameIndex.values().length];
    fonts             = new Font[FontIndex.values().length];
    functionKeyTexts  = new String[FUNCTION_KEYS_COUNT];

    this.LoadSettingsFromDisk();
  }
  
  public Font getFonts(FontIndex index)
  {
    return fonts[index.toInt()];
  }
  
  public void setFont(FontIndex index, Font font)
  {
    fonts[index.toInt()] = font;
  }
  
  public Rectangle getFrameDimensions(FrameIndex index)
  {
    return framesDimensions[index.toInt()];
  }

  public void setFrameDimensions(FrameIndex index, Rectangle rect)
  {
    this.framesDimensions[index.toInt()] = rect;
  }

  
  public void setBandmapStepInHz(int bandmapStepInHz)
  {
    this.bandmapStepInHz = bandmapStepInHz;
  }

  public void setBandmapRowCount(int bandmapRowCount)
  {
    this.bandmapRowCount = bandmapRowCount;
  }

  public void setBandmapColumnCount(int bandmapColumnCount)
  {
    this.bandmapColumnCount = bandmapColumnCount;
  }

  public int getBandmapStepInHz()
  {
    return bandmapStepInHz;
  }

  public int getBandmapRowCount()
  {
    return bandmapRowCount;
  }

  public int getBandmapColumnCount()
  {
    return bandmapColumnCount;
  }
  
  public boolean isShowBandmapFreqColumns()
  {
    return isShowBandmapFreqColumnsEnabled;
  }
  
  public void setShowBandmapFreqColumns(boolean isEnabled)
  {
    isShowBandmapFreqColumnsEnabled = isEnabled;
  }
  
  public boolean isBandmapAutoFreq()
  {
    return isBandmapAutoFreq;
  }
  
  public void setBandmapAutoFreq(boolean isEnabled)
  {
    isBandmapAutoFreq = isEnabled;
  }
  
  
  
  public String getRadioComPort()
  {
    return radioComPort;
  }

  public void setRadioComPort(String comPort)
  {
    this.radioComPort = comPort;
  }
  
  public String getKeyerComPort()
  {
    return keyerComPort;
  }

  public void setKeyerComPort(String comPort)
  {
    this.keyerComPort = comPort;
  }
  
  public KeyerTypes getKeyerType()
  {
    return this.keyerType;
  }
  
  public void setKeyerType(KeyerTypes keyerType)
  {
    this.keyerType = keyerType;
  }

  public int getRadioComPortBaudRate()
  {
    return radioComPortBaudRate;
  }

//  public int getKeyerComPortBaudRate()
//  {
//    return keyerComPortBaudRate;
//  }
//
//  public void setKeyerComPortBaudRate(int keyerComPortBaudRate)
//  {
//    this.keyerComPortBaudRate = keyerComPortBaudRate;
//  }

  public void setRadioComPortBaudRate(int radioComPortBaudRate)
  {
    this.radioComPortBaudRate = radioComPortBaudRate;
  }
  
  public String getMyCallsign()
  {
    return this.myCallsign;
  }

  public void setMyCallsign(String callsign)
  {
    this.myCallsign = callsign;
  }

  
  public void setEmsEnabled(boolean isEnabled)
  {
    this.emsEnabled = isEnabled;
  }
  
  /**
   * If "Enter Sends Message" is enabled:
   *  - sends "599 and report" when enter is pressed while focus is on Callsign text field.
   *  - sends "tu" when enter is pressed while focus in on RCV text field.
   * 
   * @return 
   */
  public boolean isEmsEnabled()
  {
    return this.emsEnabled;
  }
  
  public void setAutoCqJumpEnabled(boolean isEnabled)
  {
    this.autoCqJumpEnabled = isEnabled;
  }
  
  public boolean isAutoCqJumpEnabled()
  {
    return this.autoCqJumpEnabled;
  }
  
  public void setSendLeadingZeroAsT(boolean isEnabled)
  {
    this.isSendLeadingZeroAsT = isEnabled;
  }
  
  public boolean isSendLeadingZeroAsT()
  {
    return this.isSendLeadingZeroAsT;
  }
  
  public void setSendZeroAsT(boolean isEnabled)
  {
    this.isSendZeroAsT = isEnabled;
  }
  
  public boolean isSendZeroAsT()
  {
    return this.isSendZeroAsT;
  }
          
  /**
   * This setting is not saved to a file (also it is initialized to 0 on object creation)
   *
   * @param isEnabled True is single element mode is enabled
   */
  public void setQuickCallsignMode(boolean isEnabled)
  {
    this.isQuickCallsignModeEnabled = isEnabled;
  }

  /**
   * This setting is not saved to a file (also it is initialized to 0 on object creation)
   *
   * @return True is single element mode is enabled
   */
  public boolean isQuickCallsignModeEnabled()
  {
    return this.isQuickCallsignModeEnabled;
  }

  public String getDefaultPrefix()
  {
    return this.defaultPrefix;
  }

  public void setDefaultPrefix(String prefix)
  {
    this.defaultPrefix = prefix;
  }

  /**
   * Get the allowed repeat period for Qso in seconds
   *
   * @return
   */
  public int getQsoRepeatPeriod()
  {
    return this.qsoRepeatPeriodInSeconds;
  }

  /**
   * Set the allowed repeat period for Qso in seconds
   *
   * @param periodInSeconds
   */
  public void setQsoRepeatPeriod(int periodInSeconds)
  {
    this.qsoRepeatPeriodInSeconds = periodInSeconds;
  }

  /**
   * Returns the text for the desired function key
   *
   * @param keyIndex - Index 0 is for the F1 key, 1 for F2 and so on...
   * @return
   */
  public String getFunctionKeyMessage(int keyIndex)
  {
    return functionKeyTexts[keyIndex];
  }

  /**
   * Sets the text for the desired function key
   *
   * @param keyIndex - Index 0 is for the F1 key, 1 for F2 and so on...
   * @param text - the text that will be set for the desired function key
   */
  public void setFunctionKeyMessage(int keyIndex, String text)
  {
    functionKeyTexts[keyIndex] = text;
  }
  
  public void setIncomingQsoMaxEntries(int incomingQsoMaxEntries)
  {
    this.incomingQsoMaxEntries = incomingQsoMaxEntries;
  }

  public void setIncomingQsoHiderAfter(int incomingQsoHiderAfter)
  {
    this.incomingQsoHiderAfter = incomingQsoHiderAfter;
  }

  public int getIncomingQsoMaxEntries()
  {
    return incomingQsoMaxEntries;
  }

  public int getIncomingQsoHiderAfter()
  {
    return incomingQsoHiderAfter;
  }

  /**
   * Stores the array of values into properties which are named using key+index of the value
   *
   * @param key - property key that will be used for writing this property
   * @param values - array of values that will be written
   */
  private void setProperties(String key, String[] values)
  {
    for (int i = 0; i < values.length; i++)
    {
      prop.setProperty(key + i, values[i]);
    }
  }

  private void getProperties(String key, String[] values)
  {
    for (int i = 0; i < values.length; i++)
    {
      values[i] = prop.getProperty(key + i);
    }
  }
  
  
  private void setPropertiesFramesSizes()
  {
    for(int i=0; i<framesDimensions.length; i++)
    {
      prop.setProperty(PROPERTY_FRAMES_DIMENSIONS+i+"x", Integer.toString(framesDimensions[i].x));
      prop.setProperty(PROPERTY_FRAMES_DIMENSIONS+i+"y", Integer.toString(framesDimensions[i].y));
      prop.setProperty(PROPERTY_FRAMES_DIMENSIONS+i+"width", Integer.toString(framesDimensions[i].width));
      prop.setProperty(PROPERTY_FRAMES_DIMENSIONS+i+"height", Integer.toString(framesDimensions[i].height));
    } 
  }
  
  private boolean getPropertiesFramesSizes()
  {
    int x, y, w, h;
    
    
    try
    {
      for(int i=0; i<framesDimensions.length; i++)
      {
        // Read the JFrame dimensions:
        x = Integer.parseInt(prop.getProperty(PROPERTY_FRAMES_DIMENSIONS+i+"x"));
        y = Integer.parseInt(prop.getProperty(PROPERTY_FRAMES_DIMENSIONS+i+"y"));
        w = Integer.parseInt(prop.getProperty(PROPERTY_FRAMES_DIMENSIONS+i+"width"));
        h = Integer.parseInt(prop.getProperty(PROPERTY_FRAMES_DIMENSIONS+i+"height"));

        this.framesDimensions[i] = new Rectangle(x, y, w, h);
      }
    }catch(Exception exc)
    {
      return false;
    }
    
    
    return true;
  }

  
  private void setPropertiesFonts()
  {
    for(int i=0; i<fonts.length; i++)
    {
      prop.setProperty(PROPERTY_FONTS+i+"name", fonts[i].getName());
      prop.setProperty(PROPERTY_FONTS+i+"style", Integer.toString(fonts[i].getStyle()));
      prop.setProperty(PROPERTY_FONTS+i+"size", Integer.toString(fonts[i].getSize()));
    } 
  }
  
  
  private boolean getPropertiesFonts() throws Exception
  {
    String name;
    int style, size;
    
    try
    {
      for(int i=0; i<fonts.length; i++)
      {
        name = prop.getProperty(PROPERTY_FONTS+i+"name");
        style = Integer.parseInt(prop.getProperty(PROPERTY_FONTS+i+"style"));
        size = Integer.parseInt(prop.getProperty(PROPERTY_FONTS+i+"size"));

        fonts[i] = new Font(name, style, size);
      } 
    }catch(Exception exc)
    {
      return false;
    }
    
    return true;
  }
   
   
  /**
   * Saves the settings into a file called "DLineSettings.properties"
   */
  public void SaveSettingsToDisk()
  {
    prop.setProperty(PROPERTY_RADIO_COMPORT, radioComPort);
    prop.setProperty(PROPERTY_KEYER_COMPORT, keyerComPort);
    prop.setProperty(PROPERTY_RADIO_COMPORT_BAUDRATE, Integer.toString(radioComPortBaudRate));
    //prop.setProperty(PROPERTY_KEYER_COMPORT_BAUDRATE, Integer.toString(keyerComPortBaudRate));
    prop.setProperty(PROPERTY_KEYER_TYPE, Integer.toString(keyerType.toInt()));
    prop.setProperty(PROPERTY_MY_CALL_SIGN, myCallsign);
    prop.setProperty(PROPERTY_QUICK_CALLSIGN_MODE, Boolean.toString(isQuickCallsignModeEnabled));
    prop.setProperty(PROPERTY_AUTO_CQ_FREQ_JUMP, Boolean.toString(autoCqJumpEnabled));
    prop.setProperty(PROPERTY_ESM, Boolean.toString(emsEnabled));
    prop.setProperty(PROPERTY_SEND_LEADING_ZERO_AS_T, Boolean.toString(isSendLeadingZeroAsT));
    prop.setProperty(PROPERTY_SEND_ZERO_AS_T, Boolean.toString(isSendZeroAsT));
    prop.setProperty(PROPERTY_DEFAULT_PREFIX, defaultPrefix);
    prop.setProperty(PROPERTY_QSO_REPEAT_PERIOD_SEC, Integer.toString(qsoRepeatPeriodInSeconds));

    // Now save the texts for the function keys
    setProperties(PROPERTY_FUNCTION_KEYS, functionKeyTexts);

    prop.setProperty(PROPERTY_INCOMING_QSO_HIDE_AFTER, Integer.toString(incomingQsoHiderAfter));
    prop.setProperty(PROPERTY_INCOMING_QSO_MAX_ENTRIES, Integer.toString(incomingQsoMaxEntries));
    
    
    // Save the dimensions for the different frames
    setPropertiesFramesSizes();
    
    // Save the fonts
    setPropertiesFonts();
    
    // Save the bandmap settings
    prop.setProperty(PROPERTY_BANDMAP_COLUMN_COUNT, Integer.toString(bandmapColumnCount));
    prop.setProperty(PROPERTY_BANDMAP_ROW_COUNT, Integer.toString(bandmapRowCount));
    prop.setProperty(PROPERTY_BANDMAP_STEP, Integer.toString(bandmapStepInHz));
    prop.setProperty(PROPERTY_BANDMAP_SHOW_FREQ_COLUMNS, Boolean.toString(isShowBandmapFreqColumnsEnabled));
    prop.setProperty(PROPERTY_BANDMAP_AUTO_FREQ, Boolean.toString(isBandmapAutoFreq));
    
    try
    {
      prop.store(new FileOutputStream(SETTINGS_FILE_NAME), null);
    }
    catch (IOException ex)
    {
      Logger.getLogger(ApplicationSettings.class.getName()).log(Level.SEVERE, null, ex);
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

      // Read the dimensions for the different frames
      if(getPropertiesFramesSizes() == false)
        SetSettingToDefault(PROPERTY_FRAMES_DIMENSIONS);
          
      // Read the fonts
      if(getPropertiesFonts() == false)
        SetSettingToDefault(PROPERTY_FONTS);
      
      // Radio Comport
      radioComPort = prop.getProperty(PROPERTY_RADIO_COMPORT);
      if (radioComPort == null)
        SetSettingToDefault(PROPERTY_RADIO_COMPORT);
      
      // Radio Comport baud rate
      String temp = prop.getProperty(PROPERTY_RADIO_COMPORT_BAUDRATE);
      if (temp == null)
        SetSettingToDefault(PROPERTY_RADIO_COMPORT_BAUDRATE);
      else
        radioComPortBaudRate = Integer.parseInt(temp);
      
      // Keyer Comport
      keyerComPort = prop.getProperty(PROPERTY_KEYER_COMPORT);
      if (keyerComPort == null)
        SetSettingToDefault(PROPERTY_KEYER_COMPORT);
      
      // Keyer Comport baud rate
//      temp = prop.getProperty(PROPERTY_KEYER_COMPORT_BAUDRATE);
//      if (temp == null)
//        SetSettingToDefault(PROPERTY_KEYER_COMPORT_BAUDRATE);
//      else
//        keyerComPortBaudRate = Integer.parseInt(temp);
      
      // Keyer type
      temp = prop.getProperty(PROPERTY_KEYER_TYPE);
      if (temp == null)
        SetSettingToDefault(PROPERTY_KEYER_TYPE);
      else
        keyerType = KeyerTypes.values()[Integer.parseInt(temp)];  
      
      // My callsign
      myCallsign = prop.getProperty(PROPERTY_MY_CALL_SIGN);
      if (myCallsign == null)
        SetSettingToDefault(PROPERTY_MY_CALL_SIGN);

      // Now read the texts for the function keys
      getProperties(PROPERTY_FUNCTION_KEYS, functionKeyTexts);
      for (String str : functionKeyTexts)
      {
        if (str == null)
        {
          SetSettingToDefault(PROPERTY_FUNCTION_KEYS);
          break;
        }
      }

      
      // Misc settings
      temp = prop.getProperty(PROPERTY_QUICK_CALLSIGN_MODE);
      if (temp == null)
        SetSettingToDefault(PROPERTY_QUICK_CALLSIGN_MODE);
      else
        isQuickCallsignModeEnabled = Boolean.parseBoolean(temp);
      
      temp = prop.getProperty(PROPERTY_AUTO_CQ_FREQ_JUMP);
      if (temp == null)
        SetSettingToDefault(PROPERTY_AUTO_CQ_FREQ_JUMP);
      else
        autoCqJumpEnabled = Boolean.parseBoolean(temp);
      
      temp = prop.getProperty(PROPERTY_ESM);
      if (temp == null)
        SetSettingToDefault(PROPERTY_ESM);
      else
        emsEnabled = Boolean.parseBoolean(temp);
      
      temp = prop.getProperty(PROPERTY_SEND_LEADING_ZERO_AS_T);
      if (temp == null)
        SetSettingToDefault(PROPERTY_SEND_LEADING_ZERO_AS_T);
      else
        isSendLeadingZeroAsT = Boolean.parseBoolean(temp);
      
      temp = prop.getProperty(PROPERTY_SEND_ZERO_AS_T);
      if (temp == null)
        SetSettingToDefault(PROPERTY_SEND_ZERO_AS_T);
      else
        isSendZeroAsT = Boolean.parseBoolean(temp);

      
      // Default prefix
      defaultPrefix = prop.getProperty(PROPERTY_DEFAULT_PREFIX);
      if (defaultPrefix == null)
        SetSettingToDefault(PROPERTY_DEFAULT_PREFIX);

      // Repeat period for Qso
      temp = prop.getProperty(PROPERTY_QSO_REPEAT_PERIOD_SEC);
      if(temp == null)
        SetSettingToDefault(PROPERTY_QSO_REPEAT_PERIOD_SEC);
      else
        qsoRepeatPeriodInSeconds = Integer.parseInt(temp);
      
      // Incoming qso hide after
      temp = prop.getProperty(PROPERTY_INCOMING_QSO_HIDE_AFTER);
      if (temp == null)
        SetSettingToDefault(PROPERTY_QSO_REPEAT_PERIOD_SEC);
      else
        incomingQsoHiderAfter = Integer.parseInt(temp); 
      
      // Incoming qso max entries
      temp = prop.getProperty(PROPERTY_INCOMING_QSO_MAX_ENTRIES);
      if (temp == null)
        SetSettingToDefault(PROPERTY_INCOMING_QSO_MAX_ENTRIES);
      else
        incomingQsoMaxEntries = Integer.parseInt(temp);
      
      // Read the bandmap settings
      temp = prop.getProperty(PROPERTY_BANDMAP_COLUMN_COUNT);
      if (temp == null)
        SetSettingToDefault(PROPERTY_BANDMAP_COLUMN_COUNT);
      else
        bandmapColumnCount = Integer.parseInt(temp);
      
      temp = prop.getProperty(PROPERTY_BANDMAP_ROW_COUNT);
      if (temp == null)
        SetSettingToDefault(PROPERTY_BANDMAP_ROW_COUNT);
      else
        bandmapRowCount = Integer.parseInt(temp);
      
      temp = prop.getProperty(PROPERTY_BANDMAP_STEP);
      if (temp == null)
        SetSettingToDefault(PROPERTY_BANDMAP_STEP);
      else
        bandmapStepInHz = Integer.parseInt(temp);
    
      
      temp = prop.getProperty(PROPERTY_BANDMAP_SHOW_FREQ_COLUMNS);
      if(temp == null)
        SetSettingToDefault(PROPERTY_BANDMAP_SHOW_FREQ_COLUMNS);
      else
        isShowBandmapFreqColumnsEnabled = Boolean.parseBoolean(temp);
      
      temp = prop.getProperty(PROPERTY_BANDMAP_AUTO_FREQ);
      if(temp == null)
        SetSettingToDefault(PROPERTY_BANDMAP_AUTO_FREQ);
      else
        isBandmapAutoFreq = Boolean.parseBoolean(temp);
      
    }
    catch (Exception ex)
    {
      Logger.getLogger(ApplicationSettings.class.getName()).log(Level.SEVERE, null, ex);
      SetAllSettingsToDefault();
    }

  }

  
  private void SetAllSettingsToDefault()
  {
    radioComPort = "";

    keyerComPort = "";

    radioComPortBaudRate = 19200;

//    keyerComPortBaudRate = 1200;

    keyerType = KeyerTypes.DTR;

    myCallsign = "LZ1ABC";

    isQuickCallsignModeEnabled = false;

    defaultPrefix = "LZ";

    qsoRepeatPeriodInSeconds = 1800;

    incomingQsoMaxEntries = 20;  // Number of entries visible on the Incoming Qso panel

    incomingQsoHiderAfter = -14400; // If overtime is 4 hours don't show the entry

    // Set texts for the direction buttons
    functionKeyTexts[0] = "test {mycall}";       // F1
    functionKeyTexts[1] = "{#} {$}";             // F2
    functionKeyTexts[2] = "tu";                  // F3
    functionKeyTexts[3] = "not defined by user";
    functionKeyTexts[4] = "not defined by user";
    functionKeyTexts[5] = "nr?";
    functionKeyTexts[6] = "?";
    functionKeyTexts[7] = "qsob4";
    functionKeyTexts[8] = "hello";
    functionKeyTexts[9] = "";
    functionKeyTexts[10] = "not defined by user";
    functionKeyTexts[11] = "not defined by user";

    // Default positions for the different frames               x  y  width height
    framesDimensions[FrameIndex.JFRAME.toInt()] = new Rectangle(48, 1, 1374, 744);
    framesDimensions[FrameIndex.ENTRY.toInt()] = new Rectangle(9, 6, 383, 244);
    framesDimensions[FrameIndex.LOG.toInt()]  = new Rectangle(430, 473, 943, 164);
    framesDimensions[FrameIndex.INCOMING_QSO.toInt()] = new Rectangle(9, 254, 384, 375);
    framesDimensions[FrameIndex.BANDMAP.toInt()]  = new Rectangle(440, 4, 932, 471);
    framesDimensions[FrameIndex.RADIO.toInt()] = new Rectangle(236, 318, 394, 223);
    framesDimensions[FrameIndex.SETTINGS.toInt()] = new Rectangle(306, 32, 265, 277);
    
    bandmapStepInHz = 200;

    bandmapColumnCount = 18;

    bandmapRowCount = 24;

    // Fonts  
    fonts[FontIndex.BANDMAP.toInt()] = new Font("Dialog", Font.PLAIN, 12);
    fonts[FontIndex.CALLSIGN.toInt()] = new Font("Dialog", Font.PLAIN, 24);
    fonts[FontIndex.INCOMING_QSO.toInt()] = new Font("Dialog", 1, 18);
    fonts[FontIndex.LOG.toInt()] = new Font("Dialog", Font.PLAIN, 12);
    fonts[FontIndex.RCV.toInt()] = new Font("Dialog", Font.PLAIN, 24);
    fonts[FontIndex.SNT.toInt()] = new Font("Dialog", Font.PLAIN, 24);
    
    emsEnabled = false;

    isSendLeadingZeroAsT = false;
    
    isSendZeroAsT = false;

    autoCqJumpEnabled = false;

    isShowBandmapFreqColumnsEnabled = true;
    isBandmapAutoFreq = false;
  }
    
  /**
   * Set setting to default
   */
  private void SetSettingToDefault(String propertyname)
  {
    switch (propertyname)
    {
      case PROPERTY_RADIO_COMPORT:
        radioComPort = "";
        break;
        
      case PROPERTY_KEYER_COMPORT:
        keyerComPort = "";
        break;
      
      case PROPERTY_RADIO_COMPORT_BAUDRATE:
        radioComPortBaudRate = 19200;
        break;
        
//      case PROPERTY_KEYER_COMPORT_BAUDRATE:
//        keyerComPortBaudRate = 1200;
//        break;
      
      case PROPERTY_KEYER_TYPE:
        keyerType = KeyerTypes.DTR;
        break;
        
      case PROPERTY_MY_CALL_SIGN:
        myCallsign = "LZ1ABC";
        break;
        
      case PROPERTY_QUICK_CALLSIGN_MODE:
        isQuickCallsignModeEnabled = false;
        break;
        
      case PROPERTY_DEFAULT_PREFIX:
        defaultPrefix = "LZ";
        break;
        
      case PROPERTY_QSO_REPEAT_PERIOD_SEC:
        qsoRepeatPeriodInSeconds = 1800;
        break;
        
      case PROPERTY_INCOMING_QSO_MAX_ENTRIES:
        incomingQsoMaxEntries = 20;  // Number of entries visible on the Incoming Qso panel
        break;
        
      case PROPERTY_INCOMING_QSO_HIDE_AFTER:
        incomingQsoHiderAfter = -14400; // If overtime is 4 hours don't show the entry
        break;
        
      case PROPERTY_FUNCTION_KEYS:
        // Set texts for the direction buttons
        functionKeyTexts[0] = "test {mycall}";       // F1
        functionKeyTexts[1] = "not defined by user"; // F2
        functionKeyTexts[2] = "tu";                  // F3
        functionKeyTexts[3] = "not defined by user";
        functionKeyTexts[4] = "not defined by user";
        functionKeyTexts[5] = "nr?";
        functionKeyTexts[6] = "?";
        functionKeyTexts[7] = "qsob4";
        functionKeyTexts[8] = "hello";
        functionKeyTexts[9] = "";
        functionKeyTexts[10] = "not defined by user";
        functionKeyTexts[11] = "not defined by user";
        break;
        
      case PROPERTY_FRAMES_DIMENSIONS:  
        // Default positions for the different frames               x  y  width height
        framesDimensions[FrameIndex.JFRAME.toInt()] = new Rectangle(48, 1, 1374, 744);
        framesDimensions[FrameIndex.ENTRY.toInt()] = new Rectangle(9, 6, 383, 244);
        framesDimensions[FrameIndex.LOG.toInt()]  = new Rectangle(430, 473, 943, 164);
        framesDimensions[FrameIndex.INCOMING_QSO.toInt()] = new Rectangle(9, 254, 384, 375);
        framesDimensions[FrameIndex.BANDMAP.toInt()]  = new Rectangle(440, 4, 932, 471);
        framesDimensions[FrameIndex.RADIO.toInt()] = new Rectangle(236, 318, 394, 223);
        framesDimensions[FrameIndex.SETTINGS.toInt()] = new Rectangle(306, 32, 265, 277);
        break;
        
      case PROPERTY_BANDMAP_STEP:
        bandmapStepInHz = 200;
        break;
        
      case PROPERTY_BANDMAP_COLUMN_COUNT:
        bandmapColumnCount = 18;
        break;
        
      case PROPERTY_BANDMAP_ROW_COUNT:
        bandmapRowCount = 24;
        break;
        
      case PROPERTY_FONTS:
        // Fonts
        if(fonts[FontIndex.BANDMAP.toInt()]==null)
          fonts[FontIndex.BANDMAP.toInt()] = new Font("Dialog", Font.PLAIN, 12);
        if(fonts[FontIndex.CALLSIGN.toInt()]==null)
          fonts[FontIndex.CALLSIGN.toInt()] = new Font("Dialog", Font.PLAIN, 24);
        if(fonts[FontIndex.INCOMING_QSO.toInt()]==null)
          fonts[FontIndex.INCOMING_QSO.toInt()] = new Font("Dialog", 1, 18);
        if(fonts[FontIndex.LOG.toInt()]==null)
          fonts[FontIndex.LOG.toInt()] = new Font("Dialog", Font.PLAIN, 12);
        if(fonts[FontIndex.RCV.toInt()]==null)
          fonts[FontIndex.RCV.toInt()] = new Font("Dialog", Font.PLAIN, 24);
        if(fonts[FontIndex.SNT.toInt()]==null)
          fonts[FontIndex.SNT.toInt()] = new Font("Dialog", Font.PLAIN, 24);
        break;
        
      case PROPERTY_ESM:
        emsEnabled = false;
        break;
        
      case PROPERTY_SEND_LEADING_ZERO_AS_T:
        isSendLeadingZeroAsT = false; 
        break;
        
      case PROPERTY_SEND_ZERO_AS_T:
        isSendZeroAsT = false; 
        break;
        
      case PROPERTY_AUTO_CQ_FREQ_JUMP:
        autoCqJumpEnabled = false;
        break;
        
      case PROPERTY_BANDMAP_SHOW_FREQ_COLUMNS:
        isShowBandmapFreqColumnsEnabled = true;
        break;
      
      case PROPERTY_BANDMAP_AUTO_FREQ:
        isBandmapAutoFreq = true;
        break;
        
      default:
        Logger.getLogger(ApplicationSettings.class.getName()).log(Level.SEVERE, null, "Property has no default settings");
        break;
    }
  }
  
}
