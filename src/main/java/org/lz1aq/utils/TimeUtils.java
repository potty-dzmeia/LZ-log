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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author chavdar
 */
public class TimeUtils
{
  private static DateTimeFormatter qsoTime = DateTimeFormat.forPattern("HHmm");
  private static DateTimeFormatter qsoDate = DateTimeFormat.forPattern("yyyy-MM-dd");
  
  
  static public DateTime getUTC()
  {
    return new DateTime(DateTimeZone.UTC);
  }
  
  static public String toQsoTime(DateTime dt)
  {
    return qsoTime.print(dt);
  }
  
  static public String toQsoDate(DateTime dt)
  {
    return qsoDate.print(dt);
  }
  
  
  /**
   * 
   * @param secondsleft
   * @return 
   */
  public static String getTimeLeftFormatted(long secondsleft)
  {
    long second = secondsleft % 60;
    long minute = (secondsleft / 60) % 60;
    
    if(secondsleft < 0)
      return String.format("-%02d:%02d", Math.abs(minute), Math.abs(second));
    else
      return String.format(" %02d:%02d", Math.abs(minute), Math.abs(second));
  }
}
