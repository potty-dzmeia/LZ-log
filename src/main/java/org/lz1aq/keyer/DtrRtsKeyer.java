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

// Generator for CW messages on a serial port.
// Special chars:
// =: BT
// *: SK
// ^: AR
// Makes the following text substitutions:
// $n: serial number
// $x: complete outgoing exchange
// $f: (from) my callsign
// $t: (to) other station's callsign
// $?: whatever's in the main entry field (presumably a partial callsign) followed by "?"
// NB:  The property "keyer.cw.useCutZerosInQTCs" is actually consumed by QTCLine



import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.lz1aq.utils.MorseCode;


/**
 * Class for transmitting Morse code over the serial port DTR or RTS pins
 * @author potty
 */
public class DtrRtsKeyer implements Keyer 
{  
  /** One of the two ways generating CW - through the DRT or the RTS pin*/
  public static enum CONTROL_PIN{DTR, RTS};
  
  private final String serialPortName;
  private SerialPort   serialPort = null;
  private CONTROL_PIN  control_pin = CONTROL_PIN.DTR;
  
  private static final int QUEUE_SIZE = 30;   // Max number of texts that queueWithTexts can hold
  private final BlockingQueue<String>  queueWithTexts = new LinkedBlockingQueue<>(QUEUE_SIZE); 
  private Thread threadKeyer = new Thread(new TransmitThread(), "Keyer thread");  // Thread responsible for transmitting the messages 
  
  private static final double dashDotRatio = 3.0;
  private static final double markSpaceRatio = 1.0;
  private static final double charSpaceRatio = 3.0;
  private static final double wordSpaceRatio = 9.0;
  private double dotTime = 0.05; // Determines the speed with which CW will be send (dottime is in seconds)
  private long dotMillis = (long) (dotTime * 1000);
  private long dashMillis = (long) (dotTime * dashDotRatio * 1000);
  private long markSpaceMillis = (long) (dotTime * markSpaceRatio * 1000);
  private long charSpaceMillis = (long) (dotTime * charSpaceRatio * 1000);
  private long wordSpaceMillis = (long) (dotTime * wordSpaceRatio * 1000);

  private static final Logger logger = Logger.getLogger(DtrRtsKeyer.class.getName());

  public DtrRtsKeyer(String portName, CONTROL_PIN pin) 
  {
    this.serialPortName = portName;
    control_pin = pin;
  }

  
  @Override
  public void connect() throws Exception
  {
    if(isConnected())
    {
      logger.warning("Keyer already connected!.");
      return;
    }
    if(threadKeyer.getState() != Thread.State.NEW)
    {
      throw new Exception("For some reason the DTR thread is already running.");
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
      throw new Exception("Couldn't set manipulate DTR for comm port: "+serialPortName);
    }
  }
  
  
  /**
   * Disconnect the keyer from the comport and also kill the threadKeyer responsible for sending CW
   */
  @Override
  public void disconnect()
  {
    if(!isConnected())
    {
      logger.warning("Keyer already disconnected!");
      serialPort = null;
      return;
    }
    
    threadKeyer.interrupt();      // Stop the threadKeyer that is actually sending the morse
    while(threadKeyer.isAlive()); // wait till the threadKeyer is closed
    
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
  
  
  /**
   * Adds message to be send to the local queue
   * @param text 
   */
  @Override
  public void sendCw(String text)
  {  
    if(queueWithTexts.offer(text)== false)
    {
      logger.warning("Max queue sized reached!");
      return;
    }
    
    if(threadKeyer.getState() == Thread.State.NEW)
    {
      threadKeyer.start();
    }
    else if(threadKeyer.getState() == Thread.State.TERMINATED)
    {
      threadKeyer = new Thread(new TransmitThread(), "DTR thread");
      threadKeyer.start();
    }
  }
  
  
  /**
   * Interrupt CW sending
   */
  @Override
  public void stopSendingCw()
  {
    if(threadKeyer == null)
    {
      return;
    }
    
    queueWithTexts.clear();
    if(threadKeyer.isAlive())
    {
      threadKeyer.interrupt();
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
    
    // Speed (wpm) is 2.4/dottime (per formula in handbook, located by n2mg and other ycccers;
    // dottime in seconds).  This formula includes both
    // the dot and the following space, so the actual dot time is half this value.
    // Note that this is *independent* of other factors, like the various ratios.
    dotTime = 1.2 / (double) wpm;
    dotMillis = (long) (dotTime * 1000);
    dashMillis = (long) (dotTime * dashDotRatio * 1000);
    markSpaceMillis = (long) (dotTime * markSpaceRatio * 1000);
    charSpaceMillis = (long) (dotTime * charSpaceRatio * 1000);
    wordSpaceMillis = (long) (dotTime * wordSpaceRatio * 1000);
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
  
  
  private class TransmitThread extends Thread 
  {
    @Override
    public void run() 
    {  
      this.setPriority(Thread.MAX_PRIORITY -1); // highest prio needed so that the CW is not choppy
      
      while(true)
      { 
        try
        {
          try
          {
            transmitText(queueWithTexts.take()); // Send it over the serial
          }
          catch(InterruptedException ex)
          {
            logger.info("CW transmit interrupted.");
            setControlPin(false); // key up
            break;
          }
        }catch(SerialPortException ex)
        {
          logger.log(Level.SEVERE, "Error while trying to manipulate the com port");
        }
      }
       
    }
  
    
    /**
     * Sends the text over the serial
     * @param text  Message to be send
     * @throws InterruptedException 
     * @throws SerialPortException 
     */
    private void transmitText(String text) throws InterruptedException, SerialPortException
    {
      // 'all but the last' logic preserves the ratios between the different spaces by avoiding playing two spaces in a row.
      for(int i = 0; i < text.length(); i++)
      {
        Character c = text.charAt(i);
        if(c.equals(' '))
        {
          playWordSpace();
        }
        else
        {
          String dotsndashes = MorseCode.getCode(c); // c must be an Object to be a hashkey
          if(dotsndashes == null) return; // not a valid character - stop transmitting
          for(int j = 0; j < dotsndashes.length(); j++)
          {
            char ch = dotsndashes.charAt(j);
            if(ch == MorseCode.DOT)
            {
              playDot();
            }
            if(ch == MorseCode.DASH)
            {
              playDash();
            }
            if(!(j == dotsndashes.length() - 1))
            {
              playMarkSpace(); // all but the last mark
            }
          }
          if((i + 1 < text.length()) && text.charAt(i + 1) != ' ')
          {
            playCharSpace();  // lookahead for word space
          }
        }
      }
    }
    
    
    // port != null only useful for testing w/o a real port.  Normally, shouldn't
    // even get here.
    private void playDot() throws SerialPortException, InterruptedException 
    {
      setControlPin(true);
      sleep(dotMillis);  
      setControlPin(false);
    }
    

    private void playDash() throws SerialPortException, InterruptedException
    {
      setControlPin(true);
      sleep(dashMillis);  
      setControlPin(false);
    }
    

    private void playMarkSpace() throws InterruptedException
    {
      sleep(markSpaceMillis); 
    }

    
    private void playCharSpace() throws InterruptedException
    {   
      sleep(charSpaceMillis);
    }
    

    private void playWordSpace() throws InterruptedException
    {
      sleep(wordSpaceMillis);
    }
  }
  
}

