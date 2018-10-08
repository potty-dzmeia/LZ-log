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
package org.lz1aq.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import org.lz1aq.atu.AtuApplicationSettings;


public class ComPortProperties
{
  static final String PROPERTY_COMPORT = "ComPortName";
  static final String PROPERTY_BAUDE_RATE = "BaudRate";
  static final String PROPERTY_DATA_BITS = "DataBits";
  static final String PROPERTY_PARITY = "Parity";
  static final String PROPERTY_STOP_BITS = "Stopbits";
  static final String PROPERTY_DTR = "Dtr";
  static final String PROPERTY_RTS = "Rts";
  
  
  private String filename;
  private String comPortName;
  private String baudRate;
  private String stopbits;
  private String parity;
  private String dataBits;
  private String rts;
  private String dtr;
  
  private final Properties prop;
  
  public ComPortProperties(String fileName)
  {
    this.filename = fileName;
    
    this.prop = new Properties();
    
    this.Load();
  }
  
  private void Load()
  {
    try
    {
      prop.load(new FileInputStream(this.filename));

      comPortName = prop.getProperty(PROPERTY_COMPORT);
      if(comPortName == null)
        throwMissingPropertyException(PROPERTY_COMPORT);

      baudRate = prop.getProperty(PROPERTY_BAUDE_RATE);
      if(baudRate == null)
        throwMissingPropertyException(PROPERTY_BAUDE_RATE);

      this.dataBits = prop.getProperty(PROPERTY_DATA_BITS);
      if(dataBits == null)
        throwMissingPropertyException(PROPERTY_DATA_BITS);
      
      this.dtr = prop.getProperty(PROPERTY_DTR);
      if(dtr == null)
        throwMissingPropertyException(PROPERTY_DTR);
      
      this.parity = prop.getProperty(PROPERTY_PARITY);
      if(parity == null)
        throwMissingPropertyException(PROPERTY_PARITY);
      
      this.rts = prop.getProperty(PROPERTY_RTS);
      if(rts == null)
        throwMissingPropertyException(PROPERTY_RTS);
      
      this.stopbits = prop.getProperty(PROPERTY_STOP_BITS);
      if(stopbits == null)
        throwMissingPropertyException(PROPERTY_STOP_BITS);
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
  
  public void store()
  {
    
  }
  
  public String getComPortName()
  {
    return comPortName;
  }

  public void setComPortName(String comPort)
  {
    this.comPortName = comPort;
  }
  
  public int getBaudRate()
  {
    return Integer.parseInt(this.baudRate);
  }

  public void setBaudRate(int baudRate)
  {
    this.baudRate = Integer.toString(baudRate);
  }

  
  /**
   * 
   * @return See SerialPort.STOPBITS_ ...
   */
  public int getStopbits()
  {
    switch(this.stopbits)
    {
      case "1":
        return SerialPort.STOPBITS_1;
      case "1.5":
        return SerialPort.STOPBITS_1_5;
      case "2":
      default:
        return SerialPort.STOPBITS_2;
    }
  }

  
  /**
   * 
   * @param stopbits - See SerialPort.STOPBITS_ ...
   */
  public void setStopbits(int stopbits)
  {
    switch(stopbits)
    {
      case SerialPort.STOPBITS_1:
        this.stopbits = "1";
        break;
      case SerialPort.STOPBITS_1_5:
        this.stopbits = "1.5";
        break;
      case SerialPort.STOPBITS_2:
      default:
        this.stopbits = "2";
        break;
    }
  }

  public int getParity()
  {          
    String par = this.parity.toLowerCase();
    
    switch(par)
    {
      case "odd":
        return SerialPort.PARITY_ODD;
      case "even":
        return SerialPort.PARITY_EVEN;
      case "mark":
        return SerialPort.PARITY_MARK;
      case "space":  
        return SerialPort.PARITY_SPACE;
      default:
      case "none":
        return SerialPort.PARITY_NONE;
    }
  }

  public void setParity(int par)
  {
    switch(par)
    {
      case SerialPort.PARITY_ODD:
        this.parity = "odd";
        break;
      case SerialPort.PARITY_EVEN:
        this.parity = "even";
        break;
      case SerialPort.PARITY_MARK:
        this.parity = "mark";
        break;
      case SerialPort.PARITY_SPACE:  
        this.parity = "space" ;
        break;
      default:
      case SerialPort.PARITY_NONE:
        this.parity = "none";
        break;
    }
  }

  public int getDataBits()
  {
    switch(this.dataBits)
    {
      case "5":
        return SerialPort.DATABITS_5;
      case "6":
        return SerialPort.DATABITS_6;
      case "7":
        return SerialPort.DATABITS_7;
      default:
      case "8":  
        return SerialPort.DATABITS_8;
    }
  }

  public void setDataBits(int dBits)
  {
    this.dataBits = Integer.toString(dBits);
  }

  public boolean getRts()
  {
    
    return Boolean.parseBoolean(rts);
  }

  public void setRts(boolean rts)
  {
    this.rts = Boolean.toString(rts);
  }

  public boolean getDtr()
  {
     return Boolean.parseBoolean(rts);
  }

  public void setDtr(boolean dtr)
  {
    this.rts = Boolean.toString(dtr);
  }
  
  public void SaveSettingsToDisk()
  {
    // Store com port settings
    prop.setProperty(PROPERTY_COMPORT, comPortName);
    prop.setProperty(PROPERTY_BAUDE_RATE, baudRate);
    prop.setProperty(PROPERTY_STOP_BITS, stopbits);
    prop.setProperty(PROPERTY_PARITY, parity);
    prop.setProperty(PROPERTY_DATA_BITS, dataBits);
    prop.setProperty(PROPERTY_RTS, rts);
    prop.setProperty(PROPERTY_DTR, dtr);
    
    try
    {
      prop.store(new FileOutputStream(filename), null);
    }
    catch(IOException ex)
    {
      Logger.getLogger(AtuApplicationSettings.class.getName()).log(Level.SEVERE, null, ex);
      System.exit(0);
    }
  }
  
  private void SetSettingsToDefault()
  {
    this.comPortName = "Com1";
    this.baudRate = "9600";
    this.dataBits = "8";
    this.parity = "None";
    this.stopbits = "1"; 
    this.dtr = "true";
    this.rts = "false";
  }
   
  void throwMissingPropertyException(String propertyName) throws Exception
  {
    throw new Exception("Error when trying to read element " + propertyName + " from file " + filename);
  }
}
