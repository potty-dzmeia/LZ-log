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

import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.table.AbstractTableModel;
import org.lz1aq.log.Log;
import org.lz1aq.log.Qso;
import org.lz1aq.radio.RadioModes;
import org.lz1aq.utils.Misc;
import org.lz1aq.utils.TimeUtils;

/**
 *
 * @author potty
 */
public class TimeToNextQsoTableModel extends AbstractTableModel
{
  private final static int NUMBER_OF_COLUMNS = 4;
  
  
  /** Reference to the Log */
  private final Log log;
  /** This will hold all the unique callsigns that have been worked */
  private ArrayList<IncomingQso> incomingQsoArrayList; 
  
  
  
  public TimeToNextQsoTableModel(Log log)
  {
    incomingQsoArrayList = new ArrayList<>(0);
    this.log = log;
  }


  @Override
  public int getRowCount()
  {
    return incomingQsoArrayList.size();
  }

  @Override
  public int getColumnCount()
  {
    return NUMBER_OF_COLUMNS;
  }
  
  
  @Override
  public String getColumnName(int col)
  {
    switch(col)
      {
        case 0:
          return "hisCall";    
        case 1:
          return "freq";
        case 2:
          return "type";
        case 3:
          return "time left";
        default:
          return "should not be used";
      }
  }
  

  @Override
  public Object getValueAt(int rowIndex, int columnIndex)
  {
    switch(columnIndex)
    {
      case 0:
        return incomingQsoArrayList.get(rowIndex).hisCall;
      case 1: 
        return Misc.toIncomingQsoFreq(incomingQsoArrayList.get(rowIndex).frequency);
      case 2:
        return incomingQsoArrayList.get(rowIndex).typeOfWork; // CQ or S&P
      case 3: 
        return TimeUtils.getTimeLeftFormatted(incomingQsoArrayList.get(rowIndex).getSecondsLeft());
      default:
          return "should not happen";
    }
    
  }
  
  @Override
  public boolean isCellEditable(int row, int col)
  {
    return false;
  }
  
  
  public int getFrequency(int row) throws Exception
  {
    return Integer.parseInt(incomingQsoArrayList.get(row).frequency);
  }
  
  public RadioModes getMode(int row) throws Exception
  {
    return incomingQsoArrayList.get(row).mode;
  }
   
  public String getCallsign(int row) throws Exception
  {
    return incomingQsoArrayList.get(row).hisCall;
  }
  
  public boolean  isSpQso(int row) throws Exception
  {
    return incomingQsoArrayList.get(row).getTypeOfWork().equalsIgnoreCase(Qso.TYPE_OF_WORK_SP);
  }
  
  
  /**
   * If we should go and work the callsign contained in this cell.
   * @param row
   * @param col
   * @return 
   */
  public boolean containsExpiredCallsign(int row, int col)
  {
    return incomingQsoArrayList.get(row).isExpired();
  }
  
  
  /**
   * Updates the content of the table.
   *
   * @param appsettings - settings used by the application
   */
  public synchronized void refresh(ApplicationSettings appsettings)
  {
    ArrayList<String> callsigns = log.getUniqueCallsigns();
    
    incomingQsoArrayList = new ArrayList<>(0);
    
    // For each callsign insert the last qso with this station.
    for(String callsign : callsigns)
    {
      Qso lastQso = log.getLastQso(callsign);
      if( log.getSecondsLeft(lastQso, appsettings.getQsoRepeatPeriod()) < appsettings.getIncomingQsoHiderAfter()*(-1) )
        continue;
      IncomingQso incoming = new IncomingQso(lastQso.getHisCallsign(),
                                            lastQso.getType(),
                                            lastQso.getFrequency(),
                                            lastQso.getMode(),
                                            lastQso.getElapsedSeconds(),
                                            log.getSecondsLeft(lastQso, appsettings.getQsoRepeatPeriod()));
      incomingQsoArrayList.add(incoming);
    }
    
    // Now order the array starting from the Qso with the highest value for elapsed time 
    Collections.sort(incomingQsoArrayList);
    
    this.fireTableDataChanged();
  }
  
  
  
  
  public class IncomingQso implements Comparable<IncomingQso>
  {
    private String  hisCall;
    private String  typeOfWork; // CQ or SP
    private String  frequency;
    private RadioModes  mode;
    //private String  timeLeftFormatted;
    private long    secondsLeft;
    private long    elapsedSeconds; // How many seconds before being able to work the station again

   
    
    public IncomingQso(String hisCall, 
                       String typeOfWork, 
                       String freq, 
                       RadioModes mode,
                       long secondsElapsed, 
                       long secondsLeft)
    {
      this.hisCall = hisCall;
      this.typeOfWork = typeOfWork;
      this.frequency = freq;
      this.elapsedSeconds = secondsElapsed;
      this.secondsLeft = secondsLeft;
      this.mode = mode;
    }
   
   
    
    public String getHisCall()
    {
      return hisCall;
    }

    public String getTypeOfWork()
    {
      return typeOfWork;
    }

    public String getFrequency()
    {
      return frequency;
    }

    

    public void setHisCall(String hisCall)
    {
      this.hisCall = hisCall;
    }

    public void setTypeOfWork(String typeOfWork)
    {
      this.typeOfWork = typeOfWork;
    }

    public void setFrequency(String frequency)
    {
      this.frequency = frequency;
    }

    public void setElapsedSeconds(long elapsedSeconds)
    {
      this.elapsedSeconds = elapsedSeconds;
    }

    public void setSecondsLeft(int left)
    {
      this.secondsLeft = left;
    }

    public long getElapsedSeconds()
    {
      return elapsedSeconds;
    }

    public long getSecondsLeft()
    {
      return secondsLeft;
    }
    
    public boolean isExpired()
    {
      return (secondsLeft<=0);
    }

    @Override
    public int compareTo(IncomingQso o)
    {
      if(this.elapsedSeconds < o.elapsedSeconds)
        return 1;
      else if(this.elapsedSeconds == o.elapsedSeconds)
        return 0;
      else
        return -1;
    }
  }

  
  
}
