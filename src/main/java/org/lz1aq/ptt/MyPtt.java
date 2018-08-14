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
import org.lz1aq.keyer.DtrRtsKeyer;
import org.lz1aq.radio.Radio;


/**
 *
 * @author potty
 */
public class MyPtt
{

  private String serialPortName;
  private boolean isUsingAlreadyOpenComPort; // If ptt has to use an already open com port (sharing a com port is useful when people have the same cable for controlling the radio and the keyer)
  private SerialPort serialPort = null;
  private DtrRtsKeyer.CONTROL_PIN control_pin = DtrRtsKeyer.CONTROL_PIN.DTR;
  Radio radio;

  private static final Logger logger = Logger.getLogger(DtrRtsKeyer.class.getName());


  public void MyPtt(String portName, DtrRtsKeyer.CONTROL_PIN pin)
  {
    this.serialPortName = portName;
    control_pin = pin;
    isUsingAlreadyOpenComPort = false;
  }


  public void MyPtt(SerialPort serialPort, DtrRtsKeyer.CONTROL_PIN pin) throws Exception
  {
    if(serialPort == null || !serialPort.isOpened())
    {
      throw new Exception("Trying to share the com port but it is still not open.");
    }
    this.serialPort = serialPort;
    this.serialPortName = serialPort.getPortName();
    isUsingAlreadyOpenComPort = true;
    control_pin = pin;
  }


  public void connect() throws Exception
  {
    // Open new com port (not shared with the radio)
    if(isUsingAlreadyOpenComPort == false)
    {
      if(isConnected())
      {
        logger.warning("Keyer already connected!.");
        return;
      }
      serialPort = new SerialPort(serialPortName);
      try
      {
        serialPort.openPort();
      }
      catch(SerialPortException ex)
      {
        serialPort = null;
        throw new Exception("Couldn't open com port: " + serialPortName);
      }

      try
      {
        // key up
        this.setControlPin(false);
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
        throw new Exception("Couldn't manipulate the DTR for comm port: " + serialPortName);
      }
    }
    // Shared com port
    else
    {
      try
      {
        // key up
        setControlPin(false);
      }
      catch(SerialPortException ex)
      {
        throw new Exception("Couldn't manipulate the DTR for comm port: " + serialPortName);
      }

    }
  }


  /**
   * Disconnect the ptt from the comport
   */
  public void disconnect()
  {
    // Close the port if not shared
    if(isUsingAlreadyOpenComPort == false)
    {
      if(!isConnected())
      {
        logger.warning("Keyer already disconnected!");
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
    // Com port is shared - do NOT close com port
    else
    {
      serialPort = null;
    }
  }


  public void set(boolean pttState)
  {
    try
    {
      setControlPin(pttState);
    }
    catch(SerialPortException ex)
    {
      logger.warning("Exception! For some reason the PTT cannot be set.");
    }
  }


  /**
   * Checks if connected
   *
   * @return false if not connected
   */
  public boolean isConnected()
  {
    return !(serialPort == null || serialPort.isOpened() == false);
  }


  /**
   * Set the control pin (DTR or RTS)
   *
   * @param state
   * @throws SerialPortException
   */
  private void setControlPin(boolean state) throws SerialPortException
  {
    if(control_pin == DtrRtsKeyer.CONTROL_PIN.DTR)
    {
      serialPort.setDTR(state);
    }
    else
    {
      serialPort.setRTS(state);
    }
  }
}
