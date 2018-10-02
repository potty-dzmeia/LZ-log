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

import java.util.EventListener;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPortException;
import org.lz1aq.py.rig.I_EncodedTransaction;


public class TunerController
{
  public static final int C1_MAX = 4000;
  public static final int L_MAX  = 4000;
  
  private Tuner   tunerSerialComm;
  private final CopyOnWriteArrayList<TunerControllerListener>  eventListeners = new CopyOnWriteArrayList<>();
  
  // Controlled by the user
  private int     c;
  private int     l;
  private boolean n;
  private int     antenna;
  private boolean isTuneOn;
  
  // Controller by the Tuner
  private float   swr;
  private float   antennaVoltage;
  private float   powerSupplyVoltage;
  
  public TunerController(String comport, String baudRate)
  {
    try
    {
      tunerSerialComm = new Tuner(comport, Integer.parseInt(baudRate), this);
      tunerSerialComm.connect();
    }
    catch(Exception ex)
    {
      Logger.getLogger(TunerController.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
 
  
  //----------------------------------------------------------------------
  //                           Public methods
  //----------------------------------------------------------------------
  void disconnect()
  {
    try
    {
      tunerSerialComm.disconnect();
    }
    catch(SerialPortException ex)
    {
      Logger.getLogger(TunerController.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  /**
   * Will send to the tuner request for ADC values.
   * When response is back ADC value can be read with readSWR(), readAntennaVoltage() and readPowerSupplyVoltage()
   * @return 
   */
  public boolean requestAdcValues()
  {
    if(isTuneOn == false)
    {
      System.err.println("SWR should not be requested if Tune Mode is not enabled!");
      return false;
    }
    
    return sendRequestADC();
  }
  
  
  public float readSWR()
  {
    return swr;
  }

   
  public float readAntennaVoltage()
  {
    return antennaVoltage;
  }

  
  public float readPowerSupplyVoltage()
  {
    return powerSupplyVoltage;
  }
  
  
  /**
   * Sends to the tuner a command to go into Tune Mode (i.e. where we can read ADC values)
   * @return 
   */
  public boolean enableTuneMode()
  {
    if(this.isTuneOn)
      return true;
    
    this.isTuneOn = true;
    
    return sendSetTuneMode();
  }
  
  /**
   * Sends to the tuner a command to go into Tune Mode (i.e. where we cannot read ADC values)
   * @return 
   */
  public boolean disableTuneMode()
  {
    if(isTuneOn == false)
      return true;
    
    isTuneOn = false;
    return sendSetTuneMode();
  }
  
  
  /**
   *  Sends a command to the tuner to select another antenna.
   * 
   * @param antenna
   * @return 
   */
  public boolean setAntenna(int antenna)
  {
    if(this.antenna == antenna)
      return true;
    
    System.out.println("setAnt");
    this.antenna = antenna;
    return sendSetRelays();
  }
  
  
  /**
   * Sets the antenna locally only in the TunerController class without sending a command to the Tuner.
   * Useful if we would like to avoid sending more than one setRelay command to the Tuner when
   * changing antenna which possibly also includes change of tune values.
   * 
   * @param antenna
   * @param sendCommand
   * @return 
   */
  public boolean setAntenna(int antenna, boolean sendCommand)
  {
    if(this.antenna == antenna)
      return true;
    
    
    this.antenna = antenna;
    
    if(sendCommand)
    {
      System.out.println("setAnt");
      return sendSetRelays();
    }
    else
      return true;
  }
  
  
  /**
   * This sends immediately command to the tuner. 
   * 
   * @param value 
   * @return  
   */
  public boolean setN(boolean value)
  {
    if(this.n == value)
      return true;
    
    System.out.println("setN");
    this.n = value;
    return sendSetRelays();
  }
  
 
  /**
   * This sends immediately command to the tuner. 
   * 
   * @param value 
   * @return  
   */
  public boolean setC1(int value)
  {
    assert value<=C1_MAX;
    if(this.c == value)
      return true;
    
    System.out.println("setC");
    this.c = value;
    return sendSetRelays();
  }
  
  /**
   * This sends immediately command to the tuner.
   * 
   * @param value 
   * @return  
   */
  public boolean setL(int value)
  {
    assert value<=L_MAX;
    if(this.l == value)
      return true;   
    
    System.out.println("setL");
    this.l = value;
    return sendSetRelays();
  }
  
  /**
   * This sends immediately command to the tuner
   * 
   * @param c
   * @param l
   * @param n 
   */
  public boolean setAll(int c, int l, boolean n)
  { 
    assert c<=C1_MAX;
    assert l<=L_MAX;
    
    if(this.c == c && this.l==l && this.n==n)
      return true;
    
    System.out.println("setAll");
    this.c = c;  
    this.l = l;
    this.n = n;
    
    return sendSetRelays();
  }
  
  
  public void addEventListener(TunerControllerListener listener)
  {
    this.eventListeners.add(listener);
  }

  public void removeEventListener(TunerControllerListener listener)
  {
    this.eventListeners.remove(listener);
  }
  
  
  /**
   * Extracts SWR, voltage and other parameters from the input data
   * @param data - incoming data
   * 
   * @return - number of bytes read from the the supplied data
   */
  public int decodeSerialData(byte[] data)
  {
    if(data.length==0)
      return 0;
    
    // Depending on the decoded data inform the interested parties
    // -----------------------------------------------------------
    // POS confirmation
    for(TunerControllerListener listener : eventListeners)
    {
      listener.eventPosConfirmation();
    }
    
    // NEG confirmation
    for(TunerControllerListener listener : eventListeners)
    {
      listener.eventNegConfirmation();
    }
    
    // SWR 
    for(TunerControllerListener listener : eventListeners)
    {
      listener.eventSwr();
    }

    // Ant voltage 
    for(TunerControllerListener listener : eventListeners)
    {
      listener.eventAntennaVoltage();
    }
    
    // Power supply voltage
    for(TunerControllerListener listener : eventListeners)
    {
      listener.eventPowerSupplyVoltage();
    }
    
    return 0;
  }
  
 public boolean isReadyToAcceptCommand()
  {
    return !tunerSerialComm.isQueueFull();
  }
          
  //----------------------------------------------------------------------
  //                           Private methods
  //----------------------------------------------------------------------
  
  /** Assembles and sends the data to the class responsible for the serial communication with the tuner.
   * 
   * @return Transaction contains the data that will be sent through the serial communication 
   */
  private boolean sendSetRelays()
  {
    // encode the command and send it to serial through the serial connection
    I_EncodedTransaction transaction = new EncodedTransaction();
    
    
    ((EncodedTransaction)transaction).setTransaction(new byte[]{}); 
    return tunerSerialComm.queueTransactions(transaction);
  }
  
  
  private boolean sendRequestADC()
  {
     // encode the command and send it to serial through the serial connection
    I_EncodedTransaction transaction = new EncodedTransaction();
    
    
    ((EncodedTransaction)transaction).setTransaction(new byte[]{}); 
    return tunerSerialComm.queueTransactions(transaction);
  }
  
  
  private boolean sendSetTuneMode()
  {
     // encode the command and send it to serial through the serial connection
    I_EncodedTransaction transaction = new EncodedTransaction();
 
    ((EncodedTransaction)transaction).setTransaction(new byte[]{}); 
    return tunerSerialComm.queueTransactions(transaction);
  }
  
  
  //----------------------------------------------------------------------
  //                           Internal Classes
  //----------------------------------------------------------------------
 
  private class EncodedTransaction implements I_EncodedTransaction
  {
    private byte[] transaction;
    
    public void setTransaction(byte[] buffer)
    {
      this.transaction = buffer;
    }
    
    @Override
    public byte[] getTransaction()
    {
      return this.transaction;
    }

    /**
     * If there should be a delay between each byte of the transaction being sent out
     *
     * @return The amount of delay in milliseconds
     */
    @Override
    public int getWriteDelay()
    {
      return 0;
    }

    /**
     * If there should be a delay between each transaction send out
     *
     * @return The amount of delay in millisecond
     */
    @Override
    public int getPostWriteDelay()
    {
      return 100;
    }
 
    /**
     * Timeout after which we should not wait for positive confirmation from the rig If
     * isConfirmationExpected is false there will be no waiting anyway.
     *
     * @return Timeout, in milliseconds
     */
    @Override
    public int getTimeout()
    {
      return 0;
    }

    /**
     * Maximum number of retries if command fails (0 for no retry)
     *
     * @return number of retries before abandoning the transaction
     */
    @Override
    public int getRetry()
    {
      return 0;
    }

    /**
     * If the program should expect confirmation after sending this transaction to the rig
     *
     * @return TRUE - if the rig will send confirmation after receiving this transaction
     */
    @Override
    public boolean isConfirmationExpected()
    {
      return false;
    }
  }
  
  
  public interface TunerControllerListener extends EventListener
  {
    public void eventSwr();
    public void eventAntennaVoltage();
    public void eventPowerSupplyVoltage();
    public void eventNotsupported();
    public void eventPosConfirmation();
    public void eventNegConfirmation();
  }

}
