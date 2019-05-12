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


import javax.swing.DefaultComboBoxModel;
/**
 *
 * @author levkov_cha_ext
 */
public enum PttTypes
{
  NONE(0), DTR(1), RTS(2), RADIO_CONTROLLED(3);
  private final int value;

  PttTypes(int value)
  {
    this.value = value;
  }

  public int getValue()
  {
    return value;
  }

  static public String getName(int value)
  {
    for(PttTypes e : PttTypes.values())
    {
      if(e.value == value)
      {
        return e.toString();
      }
    }
    return null;// not found
  }
   
  static public DefaultComboBoxModel getComboxModel()
  {
    return new DefaultComboBoxModel(new String[]
    {
      NONE.toString(), DTR.toString(), RTS.toString(), /*RADIO_CONTROLLED.toString()*/
    });
  }
}