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
package org.lz1aq.ptt;

import jssc.SerialPort;

/**
 *
 * @author levkov_cha_ext
 */
public interface Ptt
{
  /**
   * Will initialize the PTT object so that is ready to be used (i.e. Open the serial port)
   * @return 
   * @throws java.lang.Exception 
   */
  public void init() throws Exception;
  
  /**
   * Will deinitialize the PTT object (i.e. Close the serial port)
   */
  public void terminate();
  
  /**
   * Will engage the PTT. Will introduce the required delay.
   */
  public void on();
  
  /**
   * Will disengage the PTT. 
   */
  public void off();
  
  public SerialPort getCommport();
}
