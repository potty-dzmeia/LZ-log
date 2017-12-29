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

/**
 *
 * @author potty
 */
public interface Keyer
{
  /**
   * 
   * @throws Exception 
   */
  public void connect() throws Exception;
  
  public void disconnect();
  
  public boolean isConnected();
  /**
   * Send the specified text as Morse code
   * @param text what we are about to send
   */
  void sendCw(String text);
  
  /**
   * Interrupt Morse sending
   */
  void stopSendingCw();
  
  /**
   * Set the speed with which we will transmit
   * @param wpm 
   */
  void setCwSpeed(int wpm);
}
