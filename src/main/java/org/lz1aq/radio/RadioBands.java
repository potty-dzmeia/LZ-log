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
package org.lz1aq.radio;

/**
 *
 * @author levkov_cha_ext
 */
public enum RadioBands
{
   BAND_160M(0),
   BAND_80M(1), 
   BAND_40M(2),
   BAND_30M(3),
   BAND_20M(4),
   BAND_17M(5),
   BAND_15M(6),
   BAND_12M(7),
   BAND_10M(8);

   private final int code;
   RadioBands(int code)  { this.code = code; }
   public int getCode() { return code; }
}
