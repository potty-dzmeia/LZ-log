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
package org.lz1aq.log;

import java.util.HashSet;
import java.util.Set;
import javax.swing.table.AbstractTableModel;

/**
 * Class used for mapping a Log to a JTable.
 * 
 */
public class LogTableModel extends AbstractTableModel
{
  /** Reference to the Log that is to be mapped to the JTable*/
  private final Log log;
  /** This is holding indeces of the columns which shouldn't be visible on the table */
  private final Set<Integer> hiddenColumns;
  
  public LogTableModel(Log log)
  {
    this.hiddenColumns = new HashSet<>();
    this.log = log;
  }
  
  
  @Override
  public int getRowCount()
  {
    return log.getRowCount();
  }

  @Override
  public int getColumnCount()
  {
    return log.getColumnCount() - hiddenColumns.size();
  }

  @Override
  public Object getValueAt(int row, int col)
  {
    return log.getValueAt(row, toLogColumn(col));
  }
  
  @Override
  public String getColumnName(int col)
  {
    return log.getColumnName(toLogColumn(col));
  }
  
  
  @Override
  public boolean isCellEditable(int row, int col)
  {
    return true;
  }
   
  @Override
  public void setValueAt(Object value, int row, int col)
  {
    log.setValueAt((String)value, row, toLogColumn(col));
  }
  
  public void removeRow(int i)
  {
    log.remove(i);
    this.fireTableRowsInserted(log.getRowCount()-1, log.getRowCount()-1);
  }
  
  public void addRow(Qso qso)
  {
    log.add(qso);
    this.fireTableRowsInserted(log.getRowCount()-1, log.getRowCount()-1);
  }
  
  /**
   * Hides a desired column.
   * 
   * @param col - Index of the column to be hidden (starting from 0)
   */
  public void setInvisible(int col)
  {
    hiddenColumns.add(toLogColumn(col));
  }
  
  
  /**
   * If a column was hidden using the setInvisible() this method will make it 
   * again visible.
   * 
   * @param col - Index of the column that we whish to be again visible.
   */
  public void setAllVisible(int col)
  {
    hiddenColumns.clear();
  }
  
  
  /** 
   * Converts the index of the table column to the actual index inside the log
   * by taking into account any hidden columns.
   * 
   * @param tableColumn - the index of the column inside the table.
   * @return the index of the column inside the log
   */
  private int toLogColumn(int tableColumn)
  {
    int visibleColumns = -1;
    
    for(int i=0; i<log.getColumnCount(); i++)
    {
      // If log column is visible...
      if(hiddenColumns.contains(i) == false)
      {
        visibleColumns++; // 
      }
      
      if(visibleColumns == tableColumn)
      {
        return i;
      }
    }
    
    throw new ArrayIndexOutOfBoundsException("Invalid index for tableColumn!");
  }

  
}
