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
package org.lz1aq.radio;

import org.lz1aq.radio.event.NotsupportedEvent;
import org.lz1aq.radio.event.RadioListener;
import org.lz1aq.radio.event.ConfirmationEvent;
import org.lz1aq.py.rig.I_EncodedTransaction;
import org.lz1aq.py.rig.I_DecodedTransaction;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.lz1aq.utils.DynamicByteArray;
import org.lz1aq.py.rig.I_Radio;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.lz1aq.radio.event.EmptyRadioListener;
import org.lz1aq.utils.Misc;





/**   
 * Class for controlling a radio through the serial interface
 *
 * After creating the radio object the connect() method must be called!
 */
public class Radio
{
  private static final Logger       LOGGER = Logger.getLogger(Radio.class.getName());
  
  private static final int QUEUE_SIZE = 30;   // Max number of commands that queueWithTransactions can hold
  
  private final CopyOnWriteArrayList<RadioListener>  eventListeners;      
  private       SerialPort          serialPort;           // Used for writing to serialPort
  private final I_Radio             radioProtocolParser;  // Used for decoding/encoding msg from/to the radio (jython object)
  private final Thread              threadPortWriter;     // Thread that writes transaction to the serial port
  private final DynamicByteArray    receiveBuffer;        // Where bytes received through the serial port will be put
   
  private final BlockingQueue<I_EncodedTransaction>  queueWithTransactions; // Transactions waiting to be sent to the radio
  
  private enum ConfirmationTypes{EMPTY, POSITIVE, NEGATIVE} 
  private ConfirmationTypes  confirmationStatus = ConfirmationTypes.EMPTY; // Holds type of last confirmation that came from radio
  private boolean            isWaitingForConfirmation = false;             // If we are waiting from the radio to send us positive or negative confirmation
  
 
  /**   
   * Constructor 
   * 
   * @param protocolParser - provides the protocol for communicating with the radio
   * @param commport -  name of the serial port that will be used for communicating with the radio
   */
  public Radio(I_Radio protocolParser, SerialPort commport)
  {
    radioProtocolParser   = protocolParser;           // Store the reference to the jython object
    serialPort            = commport;
    queueWithTransactions = new LinkedBlockingQueue<>(); 
    threadPortWriter      = new Thread(new PortWriter(), "threadPortWrite");    
    receiveBuffer         = new DynamicByteArray(200);  // Set the initial size to some reasonable value
    eventListeners        = new CopyOnWriteArrayList<>();
    
    LOGGER.setLevel(Level.WARNING);
  }
  
 
  
  //----------------------------------------------------------------------
  //                           Public methods
  //----------------------------------------------------------------------
  
  /**
   * Establishes communication with the radio using the desired Com port
   * 
   * This must be the first method that we call before being able to use this
   * class
   * 
   * @throws SerialPortException 
   */
  public void connect() throws Exception
  {
    if(!isConnected())
      throw new Exception("Commport for the Radio is not open!");
     
    if(threadPortWriter.getState() != Thread.State.NEW )
      throw new Exception("Please create a new Radio object");
    
    // Register a local listener - this class is interested in the confirmation events
    eventListeners.add(new LocalRadioListener());
    
    // Start Writer thread responsible of sending the data to the radio
    threadPortWriter.start();
    
    // Register the serial port reader which is responsible for handling the incoming data
    serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
    serialPort.addEventListener(new PortReader());
    
    this.queueTransactions(radioProtocolParser.encodeInit());
  }
  
  
  /**
   * Close the connection to the radio
   * 
   * After an object has been disconnects it can not be started again by calling 
   * the connect() method. For this purpose a new object must be created.
   * 
   * @throws jssc.SerialPortException
   */
  public void disconnect() throws SerialPortException
  {
    if(!isConnected())
      LOGGER.warning("Radio already disconnected!");
    
    this.queueTransactions(radioProtocolParser.encodeCleanup());
    // Wait a little to give a chance for the cleanup data to be sent to the radio
    try{Thread.sleep(150);} catch (InterruptedException ex){LOGGER.log(Level.SEVERE, null, ex);}
    
    threadPortWriter.interrupt();
    serialPort.removeEventListener();    
  }
  
  
  /**
   * Checks if currently connected to the radio
   * @return false if not connected
   */
  public boolean isConnected()
  {
    return serialPort!=null && serialPort.isOpened();   
  }
  
  
  /**
   * Set the VFO frequency of the radio
   * 
   * @param freq - frequency value
   * @param vfo - VFO which frequency will be changed
   * @throws Exception 
   */
  public void setFrequency(long freq, RadioVfos vfo) throws Exception
  {
    this.queueTransactions(radioProtocolParser.encodeSetFreq(freq, vfo.getValue()));
  }
  
  
  /**
   * Asks the radio to send us the current VFO frequency.
   * 
   * If we would like to get the frequency event when it comes we have to
   * register an EventListener
   * 
   * @param vfo - for which VFO we would like to get the frequency
   * @throws Exception 
   */
  public void getFrequency(RadioVfos vfo) throws Exception
  {
    this.queueTransactions(radioProtocolParser.encodeGetFreq(vfo.getValue()));
  }
  
    
  /**
   * Set the working mode of the radio (e.g. to CW)
   * @param mode - mode value (see I_Radio.RadioModes)
   * @param vfo - VFO which mode will be changed
   * @throws Exception 
   */
  public void setMode(RadioModes mode, RadioVfos vfo) throws Exception
  {
    this.queueTransactions(radioProtocolParser.encodeSetMode(mode.toString(), vfo.getValue()));
  }
  
    
  /**
   * Get the VFO mode of the radio
   * If we would like to get the mode event when it comes from the radio we 
   * have to register an EventListener
   * 
   * @param vfo - VFO of which we want to read the mode
   * @throws Exception 
   */
  public void getMode(RadioVfos vfo) throws Exception
  {
    this.queueTransactions(radioProtocolParser.encodeGetMode(vfo.getValue()));
  }
  
  
  public void getActiveVfo() throws Exception
  {
    this.queueTransactions(radioProtocolParser.encodeGetActiveVfo());
  }
  
  
  public String getManufacturer() throws Exception
  {
    return radioProtocolParser.getManufacturer();
  }
  
  /**
   * Send morse code
   * 
   * @param text - text to be send as morse code
   * @throws Exception 
   */
  public void sendCW(String text) throws Exception
  {
    this.queueTransactions(radioProtocolParser.encodeSendCW(text));
  }
  
  
  public void setKeyerSpeed(int speed) throws Exception
  {
    this.queueTransactions(radioProtocolParser.encodeSetKeyerSpeed(speed));
  }
  
  /**
   * Interrupts the sending of morse code which was initiated by the SendCW() command
   * @throws Exception 
   */
  public void interruptSendCW() throws Exception
  {
    this.queueTransactions(radioProtocolParser.encodeInterruptSendCW());
  }
  
  
  /**
   * Tell the radio to send back status data(usually this is frequency, operating mode and possibly more)
   * @throws Exception 
   */
  public void poll() throws Exception
  {
    this.queueTransactions(radioProtocolParser.encodePoll());
  }
  
  
  /**
   * Tell the radio whether to send back status data(usually this is frequency, operating mode and possibly more)
   * @param isActive - is 
   * @throws Exception 
   */
  public void setAutomaticInfo(boolean isActive) throws Exception
  {
    if(isActive)
      this.queueTransactions(radioProtocolParser.encodeEnableAutomaticInfo());
    else
      this.queueTransactions(radioProtocolParser.encodeDisableAutomaticInfo());
  }
  
  
  
  public void addEventListener(RadioListener listener) throws Exception
  {
    this.eventListeners.add(listener);
  }
  
  
  public void removeEventListener(RadioListener listener)
  { 
    this.eventListeners.remove(listener);
  }
  
  public SerialPort getSerialPort()
  {
    return serialPort;
  }
 
  //----------------------------------------------------------------------
  //                           Private stuff
  //----------------------------------------------------------------------
  
  
  
  /**
   * Implements a Listener which is taking care of processing serial port
   * data
   */
  private class PortReader implements SerialPortEventListener
  {
    /**
     * Reads bytes from the serial port and tries to decode them. If decoding
     * is successful the decoded transaction is send to a dispatcher who is
     * responsible of notifying the interested parties.
     * 
     * @param event 
     */
    @Override
    public void serialEvent(SerialPortEvent event)
    {
      try
      {   
        byte b[] = serialPort.readBytes();
        if(b==null)
          return;
        
        LOGGER.log(Level.INFO, "Incoming bytes ("+b.length+") <------ " + Misc.toHexString(b) );
        
        // Read all there is and add it to our receive buffer
        receiveBuffer.write(b);
       
      } catch (SerialPortException | IOException ex)
      {
        LOGGER.log(Level.WARNING, ex.toString(), ex);
      }
      
      // Do parsing till there is nothing to be parsed...
      while(true)
      {
        // Pass the received data to the protocol parser for decoding
        I_DecodedTransaction trans = radioProtocolParser.decode(receiveBuffer.toByteArray());
      
        if(trans.getBytesRead() > 0) 
        {
          // This will parseAndNotify the JSON string and notify all the interested parties
          JsonMsgParser.parseAndNotify(trans.getTransaction(), eventListeners);
          // Remove the processed bytes from the received buffer
          receiveBuffer.remove(trans.getBytesRead());
        }
        else
        {
          if(receiveBuffer.size()>1000) // Lots of incoming garabge - clean the buffer
            receiveBuffer.reset();
          break;
        }
      }
    }
  }
  
  
  
  /**
   * Implements a Thread which is taking care of writing transactions to the
   * serial port.
   */
  private class PortWriter implements Runnable
  {
    
    @Override
    public void run()
    {
      boolean isSent = true;
      I_EncodedTransaction trans;
      
      
      
      try
      {
        while(true)
        {
          // Get the next transaction to be send (waits if the queue is empty)
          trans = queueWithTransactions.take();
          
          // Retry - Try to send it the specified amount of times
          for(int i = 0; i < trans.getRetry()+1; i++)
          {
            // Write to serial port
            try
            {
              LOGGER.log(Level.INFO, "Outgoing bytes ("+trans.getTransaction().length+") ------> " + new String(trans.getTransaction(),"UTF-8" ));
              serialPort.writeBytes(trans.getTransaction());
              serialPort.purgePort(SerialPort.PURGE_TXCLEAR);
            } catch (SerialPortException | UnsupportedEncodingException ex)
            {
              LOGGER.log(Level.SEVERE, null, ex);
            }
                        
            // Wait for confirmation from the radio (optional)
            if(trans.isConfirmationExpected())
            {
              isSent = waitForPositiveConfirmation(trans);
            }

            // Delay between transactions (optional)
            if(trans.getPostWriteDelay() > 0)
            {
              Thread.sleep(trans.getPostWriteDelay());
            }
            
            // Transaction sent successfully 
            if(isSent)
            {
              break; 
            }        
          }//for(retry count) 
          
        }//while(true)
      }catch(InterruptedException e)
      {
        System.out.println("PortWriter was terminated!");
      }
    }// run()

    /**
     * Blocks the thread until the confirmation flag is updated or until the
     * timeout specified in trans expires.
     * 
     * @param trans - holds the timeout value for the confirmation
     * @return true if we received positive confirmation within the timeout
     * @throws InterruptedException 
     */
    private boolean waitForPositiveConfirmation(I_EncodedTransaction trans) throws InterruptedException
    { 
      boolean ret = false;
      
      if(confirmationStatus != ConfirmationTypes.EMPTY)
        LOGGER.log(Level.WARNING, "\"confirmationStatus\" is not EMPTY!");
      if(isWaitingForConfirmation == true)
        LOGGER.log(Level.WARNING, "\"isWaitingForConfirmation\" is not false!");
      
      synchronized(this)
      {
        long start = System.nanoTime();
        isWaitingForConfirmation = true;
        
        // The while loop is to protect from spurious wakeups
        while( isWaitingForConfirmation == true                         &&   // We loop till confirmation comes
              ((System.nanoTime()-start)/1000000 < trans.getTimeout())     ) // or till timeout expires
            wait(trans.getTimeout());
        
        if(confirmationStatus == ConfirmationTypes.EMPTY)
          LOGGER.log(Level.SEVERE, "Timeout expired - no confirmation from the radio!");
        
        
        if(confirmationStatus == ConfirmationTypes.POSITIVE)
          ret = true;
        
        confirmationStatus       = ConfirmationTypes.EMPTY; // reset to empty for the next operation
        isWaitingForConfirmation = false ;  
      }
      
      return ret;
    }
    
  }// class
  
  
  /**
   * Listener for processing confirmation responses  (positive or negative) from 
   * the radio
   */
  private class LocalRadioListener extends EmptyRadioListener
  {
    @Override
    public void eventConfirmation(ConfirmationEvent e)
    {
      updateConfirmation(e.getConfirmation());
    }
    
    @Override
    public void eventNotsupported(NotsupportedEvent e)
    {
      LOGGER.log(Level.INFO, "The following transaction couldn't be decoded: " + e.getData());
    }
  }
          
            
  /**
   * Inserts transaction(s) into the queueWithTransactions
   * @param trans - array of I_EncodedTransaction
   * @throws Exception 
   */
  private void queueTransactions(I_EncodedTransaction[] trans)
  {
    if(trans.length == 0)
    {
      LOGGER.log(Level.WARNING, "I_EncodedTransaction[] is empty"); 
      return;
    }
    if(!isConnected()) 
    {
      LOGGER.warning("Not connected to radio! Please call the connect() method!");
      return;
    }
    if(threadPortWriter.getState() == Thread.State.NEW )
    {
      LOGGER.warning("You need to call the start() method first!");
      return;
    }
   
    
    for(I_EncodedTransaction tr: trans)
    {
      if(queueWithTransactions.size() >= QUEUE_SIZE)
      {
        LOGGER.warning("Max queue sized reached!");
        return;
      }
      queueWithTransactions.offer(tr);
    }
    
  }
  
 
  /**
   *  Stores the confirmation received from the radio and notifies the threadPortWriter
   * 
   * @param cfm - Confirmation type (i.e. positive or negative)
   */
  private void updateConfirmation(boolean cfm)
  {
    // Inform portWriter that we have received confirmation for last command we have sent
    synchronized(threadPortWriter)
    {
      if(confirmationStatus != ConfirmationTypes.EMPTY)
        LOGGER.log(Level.WARNING, "Upon receiving of confirmation from the radio the \"confirmation\" var is not empty!");
      if(isWaitingForConfirmation == false)
        LOGGER.log(Level.WARNING, "Upon receiving of confirmation from the radio the \"isWaitingForConfirmation\" var is false!");
      
      
      // signal that confirmation has arrived  
      isWaitingForConfirmation = false; 
      
      // signal the type of confirmation
      if(cfm)
        confirmationStatus = ConfirmationTypes.POSITIVE;
      else
        confirmationStatus = ConfirmationTypes.NEGATIVE;
      
      threadPortWriter.notify();  // wake up the thread so that it can continue sending transactions
    }
  }
 
}
