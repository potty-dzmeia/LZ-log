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
 * Serial Port settings that must be used when connecting to a rig
 */
public interface I_SerialSettings
{

  public int getBauderateMin();

  public int getBauderateMax();

  public int getDataBits();

  public int getStopBits();

  /**
   * Returns parity parameter
   *
   * @return Possible values are: 'None', 'Even', 'Odd', 'Mark', 'Space'
   */
  public String getParity();

  /**
   * Returns handshake parameter
   *
   * @return Possible values are 'None', 'XonXoff', 'CtsRts'
   */
  public String getHandshake();

  /**
   * If the RTS line state should be changed.
   *
   * @return Possible values are "None", "On", "Off"
   */
  public String getRts();

  /**
   * If the DTR line state should be changed.
   *
   * @return Possible values are "None", "On", "Off"
   */
  public String getDtr();
}
