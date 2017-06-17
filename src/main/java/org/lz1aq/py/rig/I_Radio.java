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
 * Interface of a radio protocol parser.
 * 
 * Encode - The protocol parser is responsible of encoding commands (e.g. set 
 *          frequency to 14.100.100) into a packet of bytes that is ready to be 
 *          send to the radio.
 * Decode - The protocol parser is responsible of decoding packets coming from
 *          the radio into meaningful data (e.g. Mode was changed to CW)
 */
public interface I_Radio extends I_Rig
{
   /**
   * The function returns a string with all the modes that it supports.
   * Example: "cw ssb lsb"
   * 
   * @return A string with the supported modes. Each mode is separated from 
   * the next with space.
   */
  public String getAvailableModes();
  
  
  /**
   * The function returns a string with all the bands that it supports.
   * Example: "3.5 7 14"
   * 
   * @return A string with the supported bands in MHz. Each band is separated from the
   *  next with space.
   */
  public String getAvailableBands();
  
  
  /**
   * Gets the command(s) with which we can tell a radio to change frequency
   *
   * @param freq - the frequency to which we would like to set the VFO
   * @param vfo - which VFO we would like to manipulate
   * @return array of Transactions which are to be sent to the radio.
   */
  public I_EncodedTransaction[] encodeSetFreq(long freq, int vfo);
  
  
  /**
   * Gets the command(s) with which we can tell the radio to send us the frequency
   *
   * @param vfo - for which VFO we want the frequency
   * @return array of Transactions which are to be sent to the radio.
   */
  public I_EncodedTransaction[] encodeGetFreq(int vfo);
  
  
  /**
   * Gets the command(s) that must be send to the radio in order to set mode (e.g.
   * CW)
   *
   * @param mode - the mode to which we would like to set the VFO (see RadioModes)
   * @param vfo - which VFO we would like to manipulate
   * @return array of Transactions which are to be sent to the radio.
   */
  public I_EncodedTransaction[] encodeSetMode(String mode, int vfo);
  
  
  /**
   * Gets the command(s) with which we can tell the radio to send us the current
   * mode
   *
   * @param vfo - for which VFO we want the mode  
   * @return array of Transactions which are to be sent to the radio.
   */
  public I_EncodedTransaction[] encodeGetMode(int vfo);
 
  
  /**
   * Gets the command with which we can the the radio to send us the currently active VFO
   * @return - Array of transaction which are to be sent to the radio
   */
  public I_EncodedTransaction[] encodeGetActiveVfo();
  
  /**
   * Gets the command(s) with which we can tell the radio to send morse code
   *
   * @param text - the text that we would like to transmit as morse code  
   * @return array of Transactions which are to be sent to the radio.
   */
  public I_EncodedTransaction[] encodeSendCW(String text);
  
  
  /**
   * Interrupt the previous command sendCW
   * @return - array of Transactions which are to be sent to the radio.
   */
  public I_EncodedTransaction[] encodeInterruptSendCW();
  
  
  /**
   * Gets the command(s) with which we can tell the radio to set the speed of the CW
   * transmission.
   *
   * @param keyerSpeed - the desired speed 
   * @return array of Transactions which are to be sent to the radio.
   */
  public I_EncodedTransaction[] encodeSetKeyerSpeed(int keyerSpeed);
}