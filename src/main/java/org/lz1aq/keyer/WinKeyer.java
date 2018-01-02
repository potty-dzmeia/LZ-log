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
package org.lz1aq.keyer;

import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 *
 * @author potty
 */
public class WinKeyer implements Keyer
{
 
  private final String              serialPortName;        
  private final int                 baudRate = 1200;             
  private       SerialPort          serialPort;           
  private static final Logger       logger = Logger.getLogger(WinKeyer.class.getName());

  public WinKeyer(String portName)
  {
    serialPortName        = portName;
  }
  
  
  @Override
  public void connect() throws Exception
  {
    
    if(isConnected())
    {
      logger.warning("Keyer already connected! Will disconnect and connect again.");
      disconnect();
    }

    serialPort = new SerialPort(serialPortName);
    try
    {
      serialPort.openPort();
    }
    catch(SerialPortException ex)
    {
      serialPort = null;
      throw new Exception("Couldn't open com port: "+serialPortName);
    }
    
    try
    {
      serialPort.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_2, SerialPort.PARITY_NONE);
      serialPort.setDTR(true);
      serialPort.setRTS(false);
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
      throw new Exception("Couldn't set setting for "+serialPortName);
    }

    try
    {
      initKeyer();
    }
    catch(Exception ex)
    {
      disconnect();
      throw new Exception("Com port opened successfuly but couldn't communicate with WinKeyer");
    }
  }
  
  
  @Override
  public void disconnect()
  {
    if(!isConnected())
    {
      logger.warning("Keyer already disconnected!");
      serialPort = null;
      return;
    }
    
    // Send the "Host Close" command
    try
    {
      serialPort.writeByte((byte)0x00);
      serialPort.writeByte((byte)0x03);
      serialPort.purgePort(SerialPort.PURGE_TXCLEAR);
    }
    catch(SerialPortException ex)
    {
      logger.log(Level.SEVERE, null, ex);
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
  
  
  /**
   * Checks if currently connected to the radio
   * @return false if not connected
   */
  @Override
  public boolean isConnected()
  {
    return !(serialPort==null || serialPort.isOpened()==false);   
  }
  
  @Override
  public void sendCw(String textToSend)
  {
    textToSend = textToSend.toUpperCase();
    try
    {
      logger.log(Level.INFO, "Outgoing bytes (" + textToSend.getBytes().length + ") ------> " + textToSend);
      serialPort.writeBytes(textToSend.getBytes());
      serialPort.purgePort(SerialPort.PURGE_TXCLEAR);
    }
    catch(SerialPortException ex)
    {
      logger.log(Level.SEVERE, null, ex);
    }
  }
  
  
  /**
   * Interrupt CW sending
   */
  @Override
  public void stopSendingCw()
  {
    try
    {
      serialPort.writeByte((byte)0x0a);
      serialPort.purgePort(SerialPort.PURGE_TXCLEAR);
    }
    catch(SerialPortException ex)
    {
      logger.log(Level.SEVERE, null, ex);
    }
    
  }
  
  /**
   *  Set WPM Speed
   * @param wpm  in the range of 5-99 WPM
   */
  @Override
  public void setCwSpeed(int wpm)
  {
    if(wpm<10 && wpm>60)
      return;
    
    try
    {
      logger.log(Level.INFO, "Set speed to:" + wpm);
      serialPort.writeByte((byte)0x02);
      serialPort.writeByte((byte)wpm);
      serialPort.purgePort(SerialPort.PURGE_TXCLEAR);
    }
    catch(SerialPortException ex)
    {
      logger.log(Level.SEVERE, null, ex);
    }
    
  }
  
  
  /**
   * Send the "Host Open" command to the WinKeyer.
   * 
   */
  private void initKeyer() throws Exception
  {
    // This delay is important - giveWK time to power up
    Thread.sleep(400);
    
    serialPort.purgePort(SerialPort.PURGE_RXCLEAR);

    // Send three "NOP" commands
    serialPort.writeByte((byte)0x13);
    serialPort.purgePort(SerialPort.PURGE_TXCLEAR);
    
    // Send "Echo" command to make sure WK is online
    serialPort.writeByte((byte)0x00);
    serialPort.writeByte((byte)0x04);
    serialPort.writeByte((byte)0x65);
    serialPort.purgePort(SerialPort.PURGE_TXCLEAR);
    Thread.sleep(200); // wait for response
    byte b[] = serialPort.readBytes();
    if(b == null || b[0]!=0x65)
    {
      logger.log(Level.SEVERE, "No response from WK3 on the echo command!");
      throw new Exception("No response from WK3!");
    }
    
    // Send the "Host Open" command
    // From WK3 docu: Upon power-up, the host interface is closed. To enable host mode, the PC host must
    // issue the Admin:open <00><02> command. Upon open, WK3 will respond by sending the
    // revision code back to the host. The host must wait for this return code before any other
    // commands or data can be sent to WK3. Upon open, WK1 mode is set.
    serialPort.writeByte((byte) 0x00);
    serialPort.writeByte((byte) 0x02);
    serialPort.purgePort(SerialPort.PURGE_TXCLEAR);

    try
    {
      Thread.sleep(500); // delay to make sure WK is good to go
    }
    catch(InterruptedException ex)
    {
      logger.getLogger(WinKeyer.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}
