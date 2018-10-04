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
package org.lz1aq.tuner;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.lz1aq.py.rig.I_EncodedTransaction;
import org.lz1aq.utils.DynamicByteArray;
import org.lz1aq.utils.Misc;


public class Tuner
{
  private static final int QUEUE_SIZE = 1;   // Max number of commands that queueWithTransactions can hold
  
  private final String              serialPortName;       
  private final int                 baudRate;             
  private       SerialPort          serialPort;           // Used for writing to serialPort
  private final Thread              threadPortWriter;     // Thread that writes transaction to the serial port
  private final DynamicByteArray    receiveBuffer;        // Where bytes received through the serial port will be put
  private final TunerController     tunerController;
  
  private final BlockingQueue<I_EncodedTransaction>  queueWithTransactions; // Transactions waiting to be sent to the ATU
  
  private static final Logger       logger = Logger.getLogger(Tuner.class.getName());
 
  
  
  private enum ConfirmationTypes{EMPTY,    // No confirmation has arrived yet
                                 POSITIVE, // Positive confirmation
                                 NEGATIVE} // Negative confirmation
  private ConfirmationTypes  confirmationStatus = ConfirmationTypes.EMPTY; // Holds type of last confirmation that came from ATU
  private boolean            isWaitingForConfirmation = false;             // If we are waiting from the ATU to send us positive or negative confirmation
  
  
  
  /**   
   * Constructor 
   * 
   * @param portName -  name of the serial port that will be used for communicating with the ATU
   * @param baudRate -  baud rate to be used for the serial port
   * @param tunerController - 
   */
  public Tuner(String portName, int baudRate, TunerController tunerController)
  {
    serialPortName        = portName;
    this.baudRate         = baudRate;
    this.tunerController  = tunerController;
    queueWithTransactions = new LinkedBlockingQueue<>(QUEUE_SIZE); 
    threadPortWriter      = new Thread(new PortWriter(), "threadPortWrite");    
    receiveBuffer         = new DynamicByteArray(200);  // Set the initial size to some reasonable value
    
    tunerController.addEventListener(new LocalTunerControllerListener());
  }
    
  
  //----------------------------------------------------------------------
  //                           Public methods
  //----------------------------------------------------------------------
  
  /**
   * Establishes communication with the ATU using the desired Com port
   * 
   * This must be the first method that we call before being able to use this
   * class
   * 
   * @throws SerialPortException 
   */
  public void connect() throws Exception
  {
    if(isConnected())
      logger.warning("ATU already disconnected!");
    if(threadPortWriter.getState() != Thread.State.NEW )
      throw new Exception("Please create a new ATU object");
    
    // 
    serialPort = new SerialPort(serialPortName);
    serialPort.openPort();
    setComPortParams(serialPort);
    
    // PortWriter  (for sending the data to the ATU)
    threadPortWriter.start();
    
    // PortReader  (for reading data coming from ATU)
    serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
    serialPort.addEventListener(new PortReader());
  }
  
  
  /**
   * Close the connection to the ATU
   * 
   * After an object has been disconnects it can not be started again by calling 
   * the connect() method. For this purpose a new object must be created.
   * 
   * @throws jssc.SerialPortException
   */
  public void disconnect() throws SerialPortException
  {
    if(!isConnected())
      logger.warning("ATU already disconnected!");
   
    // Wait a little to give a chance for the cleanup data to be sent to the ATU
    try{Thread.sleep(150);} catch (InterruptedException ex){logger.log(Level.SEVERE, null, ex);}
    
    threadPortWriter.interrupt();
    serialPort.removeEventListener();
    serialPort.closePort();
    
    serialPort = null;
  }
  
  
  /**
   * Checks if currently connected to the ATU
   * @return false if not connected
   */
  public boolean isConnected()
  {
    return !(serialPort==null || serialPort.isOpened()==false);   
  }
  
  public SerialPort getSerialPort()
  {
    return serialPort;
  }
 
  
  /**
   * Inserts transaction(s) into the queueWithTransactions
   *
   * @param trans - array of I_EncodedTransaction
   * @return - if transaction was successfully queued
   */
  public boolean queueTransactions(I_EncodedTransaction trans)
  {
    if(!isConnected())
    {
      logger.warning("Not connected to ATU! Please call the connect() method!");
      return false;
    }
    if(threadPortWriter.getState() == Thread.State.NEW)
    {
      logger.warning("You need to call the start() method first!");
      return false;
    }

    if(queueWithTransactions.size() >= QUEUE_SIZE)
    {
      //logger.warning("Max queue sized reached!");
      return false;
    }
    
    queueWithTransactions.offer(trans);
    return true;
  }

  
  boolean isQueueFull()
  {
    return queueWithTransactions.remainingCapacity() < 1;
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
        
        //logger.log(Level.INFO, "Incoming bytes ("+b.length+") <------ " + Misc.toHexString(b) );
        
        // Read all there is and add it to our receive buffer
        receiveBuffer.write(b);
       
      } catch (SerialPortException | IOException ex)
      {
        logger.log(Level.WARNING, ex.toString(), ex);
      }
      
      // Do parsing till there is nothing to be parsed...
      while(true)
      {
        // Pass the received data to the protocol parser for decoding
        int bytesRead = tunerController.decodeSerialData(receiveBuffer.toByteArray());
            
        if(bytesRead > 0) 
        {
          receiveBuffer.remove(bytesRead);
        }
        else
        {
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
              logger.log(Level.INFO, "Com-> "+Misc.toHexString(trans.getTransaction()));
              serialPort.writeBytes(trans.getTransaction());
              serialPort.purgePort(SerialPort.PURGE_TXCLEAR);
            } catch (SerialPortException ex)
            {
              logger.log(Level.SEVERE, null, ex);
            }
                        
            // Wait for confirmation from the ATU (optional)
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
        logger.log(Level.WARNING, "\"confirmationStatus\" is not EMPTY!");
      if(isWaitingForConfirmation == true)
        logger.log(Level.WARNING, "\"isWaitingForConfirmation\" is not false!");
      
      synchronized(this)
      {
        long start = System.nanoTime();
        isWaitingForConfirmation = true;
        
        // The while loop is to protect from spurious wakeups
        while( isWaitingForConfirmation == true                         &&   // We loop till confirmation comes
              ((System.nanoTime()-start)/1000000 < trans.getTimeout())     ) // or till timeout expires
            wait(trans.getTimeout());
        
        if(confirmationStatus == ConfirmationTypes.EMPTY)
          logger.log(Level.SEVERE, "Timeout expired - no confirmation from the ATU!");
        
        
        if(confirmationStatus == ConfirmationTypes.POSITIVE)
          ret = true;
        
        confirmationStatus       = ConfirmationTypes.EMPTY; // reset to empty for the next operation
        isWaitingForConfirmation = false ;  
      }
      
      return ret;
    }
    
  }// class
            
  
  /**
   *  Stores the confirmation received from the ATU and notifies the threadPortWriter
   * 
   * @param cfm - Confirmation type (i.e. positive or negative)
   */
  private void updateConfirmation(boolean cfm)
  {
    // Inform portWriter that we have received confirmation for last command we have sent
    synchronized(threadPortWriter)
    {
      if(confirmationStatus != ConfirmationTypes.EMPTY)
        logger.log(Level.WARNING, "Upon receiving of confirmation from the ATU the \"confirmation\" var is not empty!");
      if(isWaitingForConfirmation == false)
        logger.log(Level.WARNING, "Upon receiving of confirmation from the ATU the \"isWaitingForConfirmation\" var is false!");
     
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
  
   /**
   * Sets Com port parameters
   * 
   * @param port The serial port which parameters will be adjusted
   * @param settings Source from which the values will be taken
   */
  private void setComPortParams(SerialPort port) throws SerialPortException
  {
    int parity = SerialPort.PARITY_NONE;
    int stopbits = SerialPort.STOPBITS_2;

    port.setParams(baudRate,
                   8,
                   stopbits,
                   parity);
    
//    switch (settings.getDtr().toLowerCase())
//    {
//      case "none":
//        break;
//      case "on":
//        port.setDTR(true);
//        break;
//      case "off":
//        port.setDTR(false);
//        break;
//    }
//
//    switch (settings.getRts().toLowerCase())
//    {
//      case "none":
//        break;
//      case "on":
//        port.setRTS(true);
//        break;
//      case "off":
//        port.setRTS(false);
//        break;
//    }
  }
  
  class LocalTunerControllerListener implements TunerController.TunerControllerListener
  {

    @Override
    public void eventAdc(){}

    @Override
    public void eventPosConfirmation()
    {
      updateConfirmation(true);
    }

    @Override
    public void eventNegConfirmation()
    {
      updateConfirmation(false);
    }
  }
  
  

}

