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
 * Contains the decoded transaction together with some control information
 *
 */
public interface I_DecodedTransaction
{

  /**
   * Gets the transaction which was received from the rig.
   *
   * @return Transaction in the form of JSON formatted string. Might be null in
   * case the supplied buffer did not contain a complete transaction
   */
  public String getTransaction();

  /**
   * Returns the amount of bytes that were read from the supplied buffer in
   * order to decode the transaction.
   *
   *
   * Typical usage is: 1) decodedTransaction1 = I_Rig.decode(receiveBuffer); to
   * decode the transaction
   *
   * 2) receiveBuffer.removeBytes(decodedTransaction.getBytesRead()); to remove
   * the already decoded bytes from the receive buffer
   *
   * 3) decodedTransaction2 = I_Rig.decode(receiveBuffer); to decode next
   * transaction
   *
   * ...and so on...
   *
   * @return The amount of bytes that were read. 0 if no transaction was found.
   * If this function returns
   */
  public int getBytesRead();

}
