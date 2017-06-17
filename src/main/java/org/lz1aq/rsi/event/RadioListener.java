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
package org.lz1aq.rsi.event;


import java.util.EventListener;

/**
 *
 * @author potty
 */
public interface RadioListener extends EventListener
{
  /**
   * Called when the radio sends us data that we couldn't decode
   * 
   * @param e - holds information about the event
   */
  public void eventNotsupported(NotsupportedEvent e);
  
  /**
   * Called when the radio sends us positive or negative confirmation
   * 
   * @param e - holds information about the event
   */
  public void eventConfirmation(ConfirmationEvent e);
  
  /**
   * Called when the radio sends us the Frequency
   * 
   * @param e - holds information about the event
   */
  public void eventFrequency(FrequencyEvent e);

  /**
   * Called when the radio sends us the Mode (e.g. to CW)
   * 
   * @param e - holds information about the event
   */
  public void eventMode(ModeEvent e);
  
  
  /**
   * Called when the radio changes the active VFO (e.g. from VfoA to VfoB)
   * 
   * @param e - holds information about the event
   */
  public void eventActiveVfo(ActiveVfoEvent e);
  

  /**
   * Called when the radio sends us the Smeter value
   * 
   * @param e - holds information about the event
   */
  public void eventSmeter(SmeterEvent e);
}
