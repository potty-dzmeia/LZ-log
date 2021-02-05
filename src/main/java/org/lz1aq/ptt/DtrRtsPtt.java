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
  private static final Logger LOGGER = Logger.getLogger(DtrRtsPtt.class.getName());

  private SerialPort    serialPort;
  private final int     delayInMs;
  private final int     tailInMs;
  private PttTypes      control_pin;

  
  /**
   * Use in case PTT has to share already open Commport
   * 
   * @param serialPort
   * @param pin
   * @param delayInMs 
   * @throws java.lang.Exception 
   */
  public DtrRtsPtt(SerialPort serialPort, PttTypes pin, int delayInMs, int tailInMs) throws Exception
  {
    if(!serialPort.isOpened())
    {
      throw new Exception("Serial port is not open: " + serialPort.getPortName());
    }
    this.serialPort = serialPort;
    control_pin     = pin;
    this.delayInMs  = delayInMs;
    this.tailInMs   = tailInMs;
    
    LOGGER.setLevel(Level.WARNING);
  }
   

  @Override
  public void init() throws Exception 
  {
    try
    {
      setControlPin(false);
    }
    catch(SerialPortException ex)
    {
      throw new Exception("PTT couldn't manipulate the Commport: " + serialPort.getPortName());
    }
  }

  @Override
  public void terminate()
  {
    try
    {
      setControlPin(false);
    }
    catch(SerialPortException ex)
    {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
             

  @Override
  public void on()
  {
    try
    {
      setControlPin(true);
      Thread.sleep(this.delayInMs);
      LOGGER.info("ptt.ON");
    }
    catch(Exception ex)
    {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
  
  @Override
  public void off()
  {
    try
    {
      Thread.sleep(tailInMs);
      setControlPin(false);
      LOGGER.info("ptt.OFF");
    }
    catch(Exception ex)
    {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
  
  
   @Override
  public SerialPort getCommport()
  {
    return serialPort;
  }
  
  
  /**
   * Set the control pin (DTR or RTS)
   * @param state
   * @throws SerialPortException 
   */
  private void setControlPin(boolean state) throws SerialPortException
  {
    if(control_pin == PttTypes.DTR)
      serialPort.setDTR(state);
    else if(control_pin == PttTypes.RTS)
      serialPort.setRTS(state);
    
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
