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
package org.lz1aq.utils;

/**
 *
 * @author levkov_cha_ext
 */
public class RcvFormatter
{
  
  /**
   * Will set all spaces between words to be  "space"
   * @param serial
   * @return 
   */
  public static String removeExtraSpaces(String serial)
  {
    return serial.replaceAll("\\s+"," ");
  }
  
  /**
   * All the numbers that are separated by spaces will have a minimum length of 3 digits (i.e. "%03d")
   * @param serial
   * @return 
   */
  public static String padNumbersWithZeros(String serial)
  {
    String arr[] = serial.split(" ", 2);
    StringBuilder result = new StringBuilder();
    for(int i=0; i<arr.length; i++)
    {
      if(Misc.isInteger(arr[i]))
      {
        arr[i] = String.format("%03d", Integer.parseInt(arr[i]));   
      }
      
      if(i>0)
        result.append(" ");
      
      result.append(arr[i]);
    }
    
    return result.toString();
  }
  
  
  public static String leadingZerosToT(String serial)
  {
    String arr[] = serial.split(" ", 2);
    StringBuilder result = new StringBuilder();
    for(int i=0; i<arr.length; i++)
    {
      if(Misc.isInteger(arr[i]))
      {
        arr[i] = arr[i].replaceFirst("^0{5}", "TTTTT");
        arr[i] = arr[i].replaceFirst("^0{4}", "TTTT");
        arr[i] = arr[i].replaceFirst("^0{3}", "TTT");
        arr[i] = arr[i].replaceFirst("^0{2}", "TT");
        arr[i] = arr[i].replaceFirst("^0{1}", "T");   
      }
      
      if(i>0)
        result.append(" ");
      
      result.append(arr[i]);
    }
    
    return result.toString();
  }
}
