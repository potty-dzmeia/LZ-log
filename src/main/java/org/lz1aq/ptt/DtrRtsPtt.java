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
package org.lz1aq.ptt;

import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 *
 * @author levkov_cha_ext
 */
public class DtrRtsPtt implements Ptt
{
  private static final Logger logger = Logger.getLogger(DtrRtsPtt.class.getName());
  /** One of the two ways for controlling the PTT - through the DRT or the RTS pin*/
  public static enum CONTROL_PIN{DTR, RTS};
  
  private final String  serialPortName;
  private SerialPort    serialPort;
  private int           delayInMs = 100;
  private boolean       isSharingCommPort;
  private CONTROL_PIN   control_pin = CONTROL_PIN.DTR;
  
  /** 
   * Use in case PTT has to open the Commport
   * @param portName
   * @param pin
   * @param delayInMs 
   */
  public DtrRtsPtt(String portName, CONTROL_PIN pin, int delayInMs)
  {
    this.serialPortName = portName;
    this.control_pin = pin;
    this.isSharingCommPort = false;
    this.delayInMs = delayInMs;
  }
  
  /**
   * Use in case PTT has to share already open Commport
   * 
   * @param serialPort
   * @param pin
   * @param delayInMs 
   */
  public DtrRtsPtt(SerialPort serialPort, CONTROL_PIN pin, int delayInMs) throws Exception
  {
    if(serialPort==null || !serialPort.isOpened())
    {
      throw new Exception("PTT trying to share use Commport" + serialPort.getPortName()  + ", but it is still not open.");
    }
    this.serialPort = serialPort;
    this.serialPortName = serialPort.getPortName();
    isSharingCommPort= true;
    control_pin = pin;
  }
   

  @Override
  public void connect() throws Exception 
  {
    // Open new com port (not shared with the radio)
    if(isSharingCommPort == false)
    {
      if(isConnected())
        throw new Exception("PTT already connected to Commport: "+serialPortName); 
 
      serialPort = new SerialPort(serialPortName);
      try
      {
        serialPort.openPort();
      }
      catch(SerialPortException ex)
      {
        serialPort = null;
        throw new Exception("PTT couldn't open Commport: "+serialPortName);
      }
     
      try
      {
        // key up
        setControlPin(false);
      }
      catch(SerialPortException ex)
      {
        try
        {
          serialPort.closePort();
        }
        catch(SerialPortException ex1)
        {
          logger.log(Level.SEVERE, null, ex1);
        }
        serialPort = null;
        throw new Exception("PTT couldn't manipulate the DTR for Commport: "+serialPortName);
      }
    }
    // Shared com port
    else
    {
      try
      {
        setControlPin(false);
      }
      catch(SerialPortException ex)
      {
        throw new Exception("PTT couldn't manipulate the DTR for Commport: "+serialPortName);
      }
      
    }
  }

  @Override
  public void disconnect()
  {
    // Close the port if not shared
    if(!isSharingCommPort)
    {
      if(!isConnected())
      {
        logger.warning("PTT already disconnected!");
        serialPort = null;
        return;
      }

      try
      {
        serialPort.closePort();
      }
      catch(SerialPortException ex)
      {
        logger.log(Level.SEVERE, null, ex);
      }
      serialPort = null;
    }
    // Com port is shared - do close com port
    else    
      serialPort = null;
  }
             

  @Override
  public void on()
  {
    try
    {
      Thread.sleep(this.delayInMs);
      setControlPin(true);
    }
    catch(Exception ex)
    {
      logger.log(Level.SEVERE, null, ex);
    }
  }
  
  @Override
  public void off()
  {
    try
    {
      setControlPin(false);
    }
    catch(Exception ex)
    {
      logger.log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * Set the control pin (DTR or RTS)
   * @param state
   * @throws SerialPortException 
   */
  private void setControlPin(boolean state) throws SerialPortException
  {
    if(control_pin == CONTROL_PIN.DTR)
    {
      serialPort.setDTR(state);
    }
    else
    {
      serialPort.setRTS(state);
    }
  }
  
  
   /**
   * Checks if currently connected to the radio
   * @return false if not connected
   */
  public boolean isConnected()
  {
    return serialPort!=null && serialPort.isOpened();   
  }

}
