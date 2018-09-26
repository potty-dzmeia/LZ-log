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
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPortException;
import org.lz1aq.py.rig.I_EncodedTransaction;


public class TunerController
{
  public static final int C1_MAX = 4000;
  public static final int L_MAX  = 4000;
  
  private Tuner   tunerSerialComm;
  
  // Controlled by the user
  private int     C1;
  private int     L;
  private boolean isTuneOn;
  
  // Controller by the Tuner
  private float   swr;
  private float   antennaVoltage;
  private float   powerSupplyVoltage;
  
  
  
  TunerController(String comport, int baudRate)
  {
    try
    {
      tunerSerialComm = new Tuner(comport, baudRate);
      tunerSerialComm.connect();
    }
    catch(Exception ex)
    {
      Logger.getLogger(TunerController.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
 
  
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
  
  
  public float getSwr()
  {
    return swr;
  }

   
  public float getAntennaVoltage()
  {
    return antennaVoltage;
  }

  
  public float getPowerSupplyVoltage()
  {
    return powerSupplyVoltage;
  }
  
   
  public void setC1(int value)
  {
    assert value<=C1_MAX;
       
    this.C1 = value;
    
    // encode the command and send it to serial through the serial connection
    I_EncodedTransaction transaction = new EncodedTransaction();
    _((EncodedTransaction)transaction).setTransaction(new byte[]{}); 
    tunerSerialComm.queueTransactions(transaction);
  }
  
  public void setL(int value)
  {
    assert value<=L_MAX;
       
    this.L = value;
    
    // encode the command and send it to serial through the serial connection
    I_EncodedTransaction transaction = new EncodedTransaction();
    _((EncodedTransaction)transaction).setTransaction(new byte[]{}); 
    tunerSerialComm.queueTransactions(transaction);
  }
   
  
  
  /**--------------------------------------------
   *    internal class
   * --------------------------------------------
   */
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
  
  
   /**--------------------------------------------
   *    internal class
   * --------------------------------------------
   */
  public interface TunerControllerListener extends EventListener
  {
    public void eventSwr();
    public void eventAntennaVoltage();
    public void eventPowerSupplyVoltage();
//    public void eventNotsupported();
//    public void eventPosConfirmation();
//    public void eventNegConfirmation();
  }

}
