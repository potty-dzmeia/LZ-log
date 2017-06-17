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

import java.io.ByteArrayOutputStream;

/**
 * DynamicByteArray extends ByteArrayOutputStream and add one additional method
 * remove() which removes desired number of bytes starting from the beginning 
 * of the array.
 */
public class DynamicByteArray extends ByteArrayOutputStream 
{
  // private static final Logger logger = Logger.getLogger(DynamicByteArray.class.getName());
  
  public DynamicByteArray()
  {
    super();
  }
  public DynamicByteArray(int i)
  {
    super(i);
  }
  
  /**
   * Removes bytes starting from the beginning of the array.
   * 
   * @param bytesCount  - the number of bytes that we would like to remove.
   * If the number of bytes is more than the amount inside the array no error
   * will occur and all bytes from the array will be deleted
   * 
   */
  public void remove(int bytesCount)
  {
    
    if(bytesCount==0)
      return;
    
    // We can't remove more than there is in the array
    if(bytesCount > this.count)
      bytesCount = this.count;
            
    System.arraycopy(this.buf, bytesCount, buf, 0, this.count-bytesCount);
    
    this.count -=bytesCount;
    
    // logger.log(Level.INFO, "Removed: "+removedBytes+"; Remaining: "+this.count+";  Buffer size "+this.buf.length);
    
  }
  
}
