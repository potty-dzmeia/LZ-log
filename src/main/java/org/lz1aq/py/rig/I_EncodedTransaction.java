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
package org.lz1aq.py.rig;




/**
 * Container for a "transaction".
 *
 * A "transaction" is a packet of bytes being sent to the rig. Usually it
 * contains some command (e.g. change frequency in case of a radio)
 */
public interface I_EncodedTransaction
{

  /**
   * Gets the transaction which can be send to the rig
   *
   * @return Transaction in the form of array of bytes
   */
  public byte[] getTransaction();

  /**
   * If there should be a delay between each byte of the transaction being sent
   * out
   *
   * @return The amount of delay in milliseconds
   */
  public int getWriteDelay();

  /**
   * If there should be a delay between each transaction send out
   *
   * @return The amount of delay in millisecond
   */
  public int getPostWriteDelay();

  /**
   * Timeout after which we should abandon sending the transaction to the rig
   *
   * @return Timeout, in milliseconds
   */
  public int getTimeout();

  /**
   * Maximum number of retries if command fails (0 for no retry)
   *
   * @return number of retries before abandoning the transaction
   */
  public int getRetry();

  /**
   * If the program should expect confirmation after sending this transaction to
   * the rig
   *
   * @return TRUE - if the rig will send confirmation after receiving this
   * transaction
   */
  public boolean isConfirmationExpected();
}
