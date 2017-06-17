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
package org.lz1aq.lzhfqrp;

import java.util.Objects;

/**
 *
 * @author potty
 */
public class BandmapSpot
{

  private final String callsign;
  private int freq;

  
  public BandmapSpot(String callsign, int freq)
  {
    this.callsign = callsign;
    this.freq = freq;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this) return true;
    
    if (!(obj instanceof BandmapSpot))
    {
      return false;
    }
    BandmapSpot user = (BandmapSpot) obj;
    return Objects.equals(callsign, user.callsign);     
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(callsign);
  }
  
  
  public int getFreq()
  {
    return freq;
  }
  
  public String getCallsign()
  {
    return callsign;
  }
  
  public void setFreq(int freq)
  {
    this.freq = freq;
  }
}
