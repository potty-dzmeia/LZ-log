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

import jssc.SerialPort;


/**
 *
 * @author potty
 */
public class KeyerFactory
{
  public static Keyer create(KeyerTypes type, String portName)
  {
    if(type==KeyerTypes.DTR)
    {
      return new DtrRtsKeyer(portName, DtrRtsKeyer.CONTROL_PIN.DTR);
    }
    else if(type==KeyerTypes.RTS)
    {
      return new DtrRtsKeyer(portName, DtrRtsKeyer.CONTROL_PIN.RTS);
    }
    else if(type == KeyerTypes.WINKEYER)
    {
      return new WinKeyer(portName);
    }
    else
    {
      return null;
    }  
  }
  
  
  public static Keyer create(KeyerTypes type, SerialPort serialPort) throws Exception
  {
    if(type==KeyerTypes.DTR)
    {
      return new DtrRtsKeyer(serialPort, DtrRtsKeyer.CONTROL_PIN.DTR);
    }
    else if(type==KeyerTypes.RTS)
    {
      return new DtrRtsKeyer(serialPort, DtrRtsKeyer.CONTROL_PIN.RTS);
    }
    else if(type == KeyerTypes.WINKEYER)
    {
      throw new Exception("Can't connect to Winkeyer using com port which is already in use.");
    }
    else
    {
      throw new Exception("Unknown Keyer type: "+type.toString());
    }
  }
}
