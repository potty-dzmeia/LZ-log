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

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.lz1aq.log.Log;
import org.lz1aq.log.Qso;
import org.lz1aq.utils.Misc;

/**
 *
 * @author potty
 */
public class BandmapTableModel extends AbstractTableModel
{
  private int startFreqInHz;
  private ApplicationSettings appSettings;
  
  /** Reference to the Log */
  private final Log log;

  
  ArrayList<Qso> lastSpQsos;
  List<BandmapSpot> manualSpots= new ArrayList();
  
  
  public BandmapTableModel(Log log, int startFreq, ApplicationSettings appSettings)
  {
    this.log = log;
    this.startFreqInHz = startFreq;
    this.appSettings = appSettings;
    
    lastSpQsos = log.getLastSpContacts();
  }

  
  @Override
  public int getRowCount()
  {
    return appSettings.getBandmapRowCount();
  }

  @Override
  public int getColumnCount()
  {
    return appSettings.getBandmapColumnCount();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex)
  {
    CellBuilder cellBuilder = new CellBuilder();

   
    // If frequency cell ...
    // ---------------------
    if(columnIndex%2 == 0)
      return Misc.toBandmapFreq(cellToFreq(rowIndex, columnIndex));
    
    
    // If Callsign cell...
    // ---------------------
      
    // Last worked SP
    for (Qso qso : lastSpQsos)
    {
      if (isCurrentFreqInThisCell(rowIndex, columnIndex, qso.getFrequencyInt()))
      {
        cellBuilder.addWorkedOnSp(qso);
      }
    }
    
    
    // Manual Spots
    for (BandmapSpot spot : manualSpots)
    {
      if (isCurrentFreqInThisCell(rowIndex, columnIndex, spot.getFreq()))
      {
        cellBuilder.addSpot(spot.getCallsign());
      }
    }
     
    return cellBuilder.getResult();
  }
  
  
  private class CellBuilder
  {
    StringBuilder cellText = new StringBuilder();
    boolean isIsHtml = false;
  
    
    /**
     * Adds callsign which has '*' in front (all Manually spotted callsigns have '*' in front)
     * @param callsign 
     */
    void addSpot(String callsign)
    {  
      String call;
      
      if(appSettings.isQuickCallsignModeEnabled())
      {
        call = "*"+Misc.toShortCallsign(callsign, appSettings.getDefaultPrefix());
      }
      else
      {
        call = "*"+callsign;
      }
      
    
      // If not a Dupe the callsign must be in BLUE
      if(!log.isDupe(callsign, appSettings.getQsoRepeatPeriod()))
      {
        isIsHtml = true; // If we add one blue callsign the whole cell must be HTML formatted
        cellText.append("<b><font color=blue>");
        cellText.append(call);
        cellText.append("</b></font>");
      }
      else
      {
        cellText.append(call);
      }
      
      cellText.append(" ");
    }
    
    
    /**
     * Add a callsign 
     * @param callsign 
     */
    public void addWorkedOnSp(Qso qso)
    {
      String call;
      
      if(appSettings.isQuickCallsignModeEnabled())
      {
        call = Misc.toShortCallsign(qso.getHisCallsign(), appSettings.getDefaultPrefix());
      }
      else
      {
        call = qso.getHisCallsign();
      }
      
      // If not a Dupe the callsign must be in BLUE
      if(!qso.isDupe(appSettings.getQsoRepeatPeriod()))
      {
        isIsHtml = true; // If we add one blue callsign the whole cell must be HTML formatted
        cellText.append("<b><font color=blue>");
        cellText.append(call);
        cellText.append("</b></font>");
      }
      else
      {
        cellText.append(call);
      }
      
      cellText.append(" ");
    }
    
    public String getResult()
    {
      if(isIsHtml)
      {
        cellText.insert(0, "<html>");
        cellText.append("</html>");
      }
      return cellText.toString();
    }         
  }
  
  
  public void addSpot(String callsign, int freq)
  {
     
    for(BandmapSpot spot : manualSpots)
    {
      // If a manual sport is available update the frequency
      if(spot.getCallsign().equals(callsign))
      {
        spot.setFreq(freq);
        return;
      }   
    }
    
    manualSpots.add(new BandmapSpot(callsign, freq));
  }
  
  /**
   * Updates the content of the table.
   * @param appSettings
   * @param startFreq
   */
  public synchronized void refresh(ApplicationSettings appSettings, int startFreq)
  {
    startFreqInHz = startFreq;
    this.appSettings = appSettings;
    
    lastSpQsos = log.getLastSpContacts();
    
    // Check if an SP contact is also in the manualSpots. 
    // If yes and frequency is within 1Khz remove the manual spot
    for(Qso spQso : lastSpQsos)
    {
      if(isManuallySpotOnSameFreq(spQso))
      {
        manualSpots.remove(new BandmapSpot(spQso.getHisCallsign(),3500000));
      }
    }
    
    
    this.fireTableDataChanged();
    //this.fireTableStructureChanged();
  }
  
  /**
   * Checks if there is a manual spot on the same frequency (+-500KHz)
   * @param qso
   * @return 
   */
  private boolean isManuallySpotOnSameFreq(Qso qso)
  {  
    for(int i=0; i<manualSpots.size(); i++)
    {
      if(manualSpots.get(i).getCallsign().equals(qso.getHisCallsign()) && // if same callsign
         Math.abs(manualSpots.get(i).getFreq() - qso.getFrequencyInt()) < 500 ) // if same freq (+-2KHz)
      {
        manualSpots.remove(i);
      }
    }    
    return false;
  }
  
  /**
   * Used for getting the frequency represented by the cell. Could be e frequency cell or a callsign
   * cell.
   * 
   * @param row
   * @param column
   * @return - frequency in Hz
   */
  public int cellToFreq(int row, int column)
  {
    // Odd column - means a callsign is hold in the cell
    if(column%2 == 1)
    {
      return startFreqInHz+((row)*appSettings.getBandmapStepInHz())+((appSettings.getBandmapRowCount()/2)*(column-1)*appSettings.getBandmapStepInHz());
    }
    // Even - a frequency is hold in this cell
    else
    {
      return startFreqInHz+((row)*appSettings.getBandmapStepInHz())+((appSettings.getBandmapRowCount()/2)*column*appSettings.getBandmapStepInHz());
    }
  }
 
  
  /**
   * Checks if the supplied "freq" fits the cell frequency
   * @param row - cell row
   * @param col - cell column
   * @param freq - the frequency that we want to check if it is within the cell frequency
   * @return 
   */
  public boolean isCurrentFreqInThisCell(int row, int col, int freq)
  {
    int cellFreq = cellToFreq(row, col);
    int lowRange = cellFreq - (appSettings.getBandmapStepInHz()/2);
    int highTange = cellFreq + (appSettings.getBandmapStepInHz()/2);
    
    return freq >= lowRange && freq < highTange;
  }
  
  
  /**
   * Check is the frequency of the Qso fits the cell frequency
   * 
   * @param row
   * @param col
   * @param qso
   * @return 
   */
  private boolean isQsoInThisCell(int row, int col, Qso qso)
  {
    return isCurrentFreqInThisCell(row, col, qso.getFrequencyInt());
  }
}
