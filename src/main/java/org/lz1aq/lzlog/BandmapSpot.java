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
package org.lz1aq.lzlog;

import java.util.Objects;
import org.lz1aq.radio.RadioModes;
import org.lz1aq.utils.Misc;

/**
 *
 * @author potty
 */
public class BandmapSpot
{

  private final String callsign;
  private int freq = 3500000;
  private RadioModes mode = RadioModes.CW;
  private String band = "80"; // Band (e.g. 160, 80, 40 and so on...)

  
  public BandmapSpot(String callsign)
  {
    this.callsign = callsign;
  }
  
  public BandmapSpot(String callsign, int freq, RadioModes mode)
  {
    this.callsign = callsign;
    this.freq = freq;
    this.mode = mode;
    
    this.band = Misc.freqToBand(freq);
  }
  

  @Override
  /**
   * Spot is considered equal if the Callsign, Band and Mode are the same
   */
  public boolean equals(Object obj)
  {
    if (obj == this) return true;
    
    if (!(obj instanceof BandmapSpot))
    {
      return false;
    }
    BandmapSpot sp = (BandmapSpot) obj;
    return Objects.equals(callsign, sp.callsign) && ;     
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(callsign, mode, band);
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
  
  public RadioModes getMode()
  {
      return this.mode;
  }
}
