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

import org.lz1aq.utils.RadioModes;
import org.lz1aq.utils.RadioVfos;


public class ModeEvent
{

  private final RadioModes mode;  // The new mode
  private final RadioVfos vfo;    // Which VFO changed its mode

  public ModeEvent(RadioModes mode, RadioVfos vfo)
  {
    this.mode = mode;
    this.vfo = vfo;
  }

  public RadioModes getMode()
  {
    return mode;
  }

  public RadioVfos getVfo()
  {
    return this.vfo;
  }
}
